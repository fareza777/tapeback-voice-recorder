package com.f7developer.tapeback.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.room.withTransaction
import com.f7developer.tapeback.BuildConfig
import com.f7developer.tapeback.core.AppFiles
import com.f7developer.tapeback.data.db.CassetteEntity
import com.f7developer.tapeback.data.db.RecordingEntity
import com.f7developer.tapeback.data.db.RecordingTagEntity
import com.f7developer.tapeback.data.db.TapeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Offline backup: one `.zip` with a JSON manifest, every audio file and every cover image, written to
 * a location the user picks. Restoring merges into the current library and never overwrites:
 * cassettes and recordings that are already present are skipped.
 */
class BackupManager(
    private val context: Context,
    private val db: TapeDatabase,
    private val files: AppFiles,
    private val scope: CoroutineScope,
    /** Called with (recordingId, unlockAt) for every restored capsule that is still sealed. */
    private val onCapsuleRestored: (Long, Long) -> Unit,
) {
    sealed interface State {
        data object Idle : State
        data class Running(val restoring: Boolean, val done: Int, val total: Int) : State
        data class Exported(val recordings: Int, val bytes: Long) : State
        data class Restored(val cassettes: Int, val recordings: Int, val skipped: Int) : State
        data class Failed(val restoring: Boolean, val notABackup: Boolean = false) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    val isRunning: Boolean get() = _state.value is State.Running

    fun acknowledge() {
        if (_state.value !is State.Running) _state.value = State.Idle
    }

    fun export(target: Uri) {
        if (isRunning) return
        _state.value = State.Running(restoring = false, done = 0, total = 0)
        scope.launch {
            _state.value = withContext(Dispatchers.IO) {
                runCatching { writeBackup(target) }
                    .onFailure { Log.w(TAG, "Backup failed", it) }
                    .getOrElse { State.Failed(restoring = false) }
            }
        }
    }

    fun restore(source: Uri) {
        if (isRunning) return
        _state.value = State.Running(restoring = true, done = 0, total = 0)
        scope.launch {
            _state.value = withContext(Dispatchers.IO) {
                runCatching { readBackup(source) }
                    .onFailure { Log.w(TAG, "Restore failed", it) }
                    .getOrElse { State.Failed(restoring = true, notABackup = it is NotABackupException) }
            }
        }
    }

    // ---- Export -------------------------------------------------------------------------------

    private suspend fun writeBackup(target: Uri): State {
        val cassettes = db.cassetteDao().all()
        val recordings = db.recordingDao().allWithTags()
        val audio = recordings.map { File(it.recording.filePath) }.filter { it.isFile }
        val covers = cassettes.mapNotNull { c -> c.coverPath?.let(::File)?.takeIf { it.isFile } }
        val total = audio.size + covers.size
        var done = 0
        var bytes = 0L

        val manifest = JSONObject()
            .put("format", FORMAT)
            .put("app", BuildConfig.VERSION_NAME)
            .put("exportedAt", System.currentTimeMillis())
            .put(
                "cassettes",
                JSONArray().apply {
                    cassettes.forEach { c ->
                        put(
                            JSONObject()
                                .put("id", c.id)
                                .put("title", c.title)
                                .put("category", c.category)
                                .put("labelColor", c.labelColor)
                                .put("skin", c.skin)
                                .put("cover", c.coverPath?.let { COVERS + File(it).name } ?: JSONObject.NULL)
                                .put("createdAt", c.createdAt)
                                .put("updatedAt", c.updatedAt),
                        )
                    }
                },
            )
            .put(
                "recordings",
                JSONArray().apply {
                    recordings.forEach { row ->
                        val r = row.recording
                        put(
                            JSONObject()
                                .put("cassetteId", r.cassetteId)
                                .put("side", r.side)
                                .put("title", r.title)
                                .put("file", AUDIO + File(r.filePath).name)
                                .put("durationMs", r.durationMs)
                                .put("sizeBytes", r.sizeBytes)
                                .put("createdAt", r.createdAt)
                                .put("favorite", r.favorite)
                                .put("waveform", r.waveform?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: JSONObject.NULL)
                                .put("capsuleUnlockAt", r.capsuleUnlockAt ?: JSONObject.NULL)
                                .put("capsuleSealedAt", r.capsuleSealedAt ?: JSONObject.NULL)
                                .put("capsuleNote", r.capsuleNote ?: JSONObject.NULL)
                                .put("capsuleNotified", r.capsuleNotified)
                                .put("tags", JSONArray(row.tags)),
                        )
                    }
                },
            )

        val out = context.contentResolver.openOutputStream(target, "w") ?: throw IOException("Cannot open $target")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.setLevel(Deflater.BEST_SPEED) // AAC and JPEG are already compressed
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            fun addFile(prefix: String, file: File) {
                zip.putNextEntry(ZipEntry(prefix + file.name).apply { time = file.lastModified() })
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                bytes += file.length()
                done++
                _state.value = State.Running(restoring = false, done = done, total = total)
            }
            audio.forEach { addFile(AUDIO, it) }
            covers.forEach { addFile(COVERS, it) }
        }
        return State.Exported(recordings = recordings.size, bytes = bytes)
    }

    // ---- Restore ------------------------------------------------------------------------------

    private class NotABackupException : IOException("Not a TapeBack backup")

    private suspend fun readBackup(source: Uri): State {
        val staging = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var manifest: JSONObject? = null
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("Cannot open $source")
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        entry.isDirectory -> Unit
                        name == MANIFEST -> manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                        isSafeMember(name) -> File(staging, name.replace('/', '_')).outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            val m = manifest ?: throw NotABackupException()
            if (m.optInt("format", -1) != FORMAT) throw NotABackupException()
            return merge(m, staging)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Only flat file names inside the two known folders: no "..", no absolute paths. */
    private fun isSafeMember(name: String): Boolean {
        val folder = when {
            name.startsWith(AUDIO) -> AUDIO
            name.startsWith(COVERS) -> COVERS
            else -> return false
        }
        val file = name.removePrefix(folder)
        return file.isNotEmpty() && SAFE_NAME.matches(file)
    }

    private fun staged(staging: File, member: String?): File? {
        if (member.isNullOrEmpty() || !isSafeMember(member)) return null
        return File(staging, member.replace('/', '_')).takeIf { it.isFile && it.length() > 0 }
    }

    private suspend fun merge(m: JSONObject, staging: File): State {
        val cassetteDao = db.cassetteDao()
        val recordingDao = db.recordingDao()
        val cassettes = m.optJSONArray("cassettes") ?: JSONArray()
        val recordings = m.optJSONArray("recordings") ?: JSONArray()
        val idMap = HashMap<Long, Long>()
        var addedCassettes = 0
        var addedRecordings = 0
        var skipped = 0
        val restoredCapsules = ArrayList<Pair<Long, Long>>()
        val total = recordings.length()

        for (i in 0 until cassettes.length()) {
            val c = cassettes.getJSONObject(i)
            val oldId = c.getLong("id")
            val title = c.optString("title").ifBlank { "Untitled Tape" }
            val createdAt = c.optLong("createdAt", System.currentTimeMillis())
            val existing = cassetteDao.findByTitleAndCreated(title, createdAt)
            if (existing != null) {
                idMap[oldId] = existing.id
                continue
            }
            val cover = staged(staging, c.optStringOrNull("cover"))?.let { src ->
                File(files.coversDir, "cover-${UUID.randomUUID()}.jpg").also { moveOrCopy(src, it) }.absolutePath
            }
            val restored = CassetteEntity(
                title = title.take(48),
                category = c.optString("category").take(24),
                labelColor = c.optInt("labelColor", 0xFFF3E6C8.toInt()),
                skin = c.optString("skin", "CLASSIC_BLACK"),
                coverPath = cover,
                createdAt = createdAt,
                updatedAt = c.optLong("updatedAt", createdAt),
            )
            // A fresh install already made an empty "My First Mixtape": fill that one instead of
            // adding a look-alike next to it.
            val emptyTwin = cassetteDao.findEmptyByTitle(restored.title)?.takeIf { it.id !in idMap.values }
            if (emptyTwin != null) {
                cassetteDao.update(restored.copy(id = emptyTwin.id))
                files.deleteQuietly(emptyTwin.coverPath)
                idMap[oldId] = emptyTwin.id
                continue
            }
            idMap[oldId] = cassetteDao.insert(restored)
            addedCassettes++
        }

        for (i in 0 until recordings.length()) {
            val r = recordings.getJSONObject(i)
            _state.value = State.Running(restoring = true, done = i, total = total)
            val cassetteId = idMap[r.optLong("cassetteId", -1L)]
            val createdAt = r.optLong("createdAt", 0L)
            val duration = r.optLong("durationMs", 0L)
            val audio = staged(staging, r.optStringOrNull("file"))
            if (cassetteId == null || audio == null || duration <= 0L) {
                skipped++
                continue
            }
            if (recordingDao.countMatching(createdAt, duration) > 0) {
                skipped++
                continue
            }
            val dest = files.newRecordingFile()
            moveOrCopy(audio, dest)
            val unlockAt = r.optLongOrNull("capsuleUnlockAt")
            val notified = r.optBoolean("capsuleNotified", false)
            val tags = r.optJSONArray("tags")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }.orEmpty()
            db.withTransaction {
                val id = recordingDao.insert(
                    RecordingEntity(
                        cassetteId = cassetteId,
                        side = if (r.optString("side") == "B") "B" else "A",
                        title = r.optString("title").ifBlank { "Recording" }.take(80),
                        filePath = dest.absolutePath,
                        durationMs = duration,
                        sizeBytes = dest.length(),
                        createdAt = createdAt,
                        favorite = r.optBoolean("favorite", false),
                        waveform = r.optStringOrNull("waveform")?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() },
                        capsuleUnlockAt = unlockAt,
                        capsuleSealedAt = r.optLongOrNull("capsuleSealedAt"),
                        capsuleNote = r.optStringOrNull("capsuleNote")?.take(500),
                        capsuleNotified = notified,
                    ),
                )
                if (tags.isNotEmpty()) recordingDao.insertTags(tags.map { RecordingTagEntity(id, it.take(32)) })
                if (unlockAt != null && !notified) restoredCapsules += id to unlockAt
            }
            addedRecordings++
        }
        restoredCapsules.forEach { (id, at) -> onCapsuleRestored(id, at) }
        return State.Restored(cassettes = addedCassettes, recordings = addedRecordings, skipped = skipped)
    }

    private fun moveOrCopy(src: File, dest: File) {
        if (!src.renameTo(dest)) {
            src.inputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
            src.delete()
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).ifEmpty { null }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)

    companion object {
        private const val TAG = "BackupManager"
        const val FORMAT = 1
        const val MANIFEST = "tapeback-backup.json"
        private const val AUDIO = "recordings/"
        private const val COVERS = "covers/"
        private val SAFE_NAME = Regex("[A-Za-z0-9._-]{1,128}")
        const val MIME = "application/zip"
    }
}

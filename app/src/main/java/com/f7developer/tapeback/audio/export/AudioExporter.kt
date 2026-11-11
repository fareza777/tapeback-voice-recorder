package com.f7developer.tapeback.audio.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Share and export without any storage permission: FileProvider for sharing, SAF for saving. */
class AudioExporter(private val context: Context) {

    fun suggestedFileName(recording: Recording): String {
        val base = recording.title
            .replace(Regex("[\\\\/:*?\"<>|·]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifEmpty { "TapeBack recording" }
            .take(60)
        return "$base.m4a"
    }

    /** Build a chooser intent sharing the audio through a temporary read grant. */
    fun shareIntent(recording: Recording): Intent? {
        val file = File(recording.filePath)
        if (!file.exists()) return null
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file, suggestedFileName(recording))
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, recording.title)
            clipData = ClipData.newRawUri(recording.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, context.getString(R.string.share_chooser_title))
    }

    /** Copy the recording to a user-chosen document URI (from ACTION_CREATE_DOCUMENT). */
    suspend fun saveTo(recording: Recording, destination: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(recording.filePath)
            context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                file.inputStream().use { it.copyTo(out, 64 * 1024) }
            } ?: error("No output stream")
            true
        }.onFailure { Log.w("AudioExporter", "Export failed", it) }.getOrDefault(false)
    }

    companion object {
        const val MIME = "audio/mp4"
    }
}

package com.f7developer.tapeback.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.StatFs
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/** All TapeBack files live in app-private storage. */
class AppFiles(private val context: Context) {

    val recordingsDir: File get() = File(context.filesDir, "recordings").apply { mkdirs() }
    val coversDir: File get() = File(context.filesDir, "covers").apply { mkdirs() }
    val soundsDir: File get() = File(context.cacheDir, "mechanics").apply { mkdirs() }

    fun newRecordingFile(): File = File(recordingsDir, "tape-${UUID.randomUUID()}.m4a")

    fun freeBytes(): Long = runCatching { StatFs(context.filesDir.absolutePath).availableBytes }.getOrDefault(Long.MAX_VALUE)

    fun deleteQuietly(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /**
     * Copy a user-picked image into private storage as a ≤ 1024 px JPEG, respecting EXIF rotation.
     * Returns the new absolute path, or null if the image could not be decoded.
     */
    suspend fun importCover(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_COVER_PX) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null
            val rotation = resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val scale = MAX_COVER_PX.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(MAX_COVER_PX)
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
            }
            val output = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val file = File(coversDir, "cover-${UUID.randomUUID()}.jpg")
            file.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (output !== decoded) decoded.recycle()
            output.recycle()
            file.absolutePath
        }.onFailure { Log.w(TAG, "Cover import failed", it) }.getOrNull()
    }

    companion object {
        private const val TAG = "AppFiles"
        private const val MAX_COVER_PX = 1024
        const val MIN_FREE_BYTES = 50L * 1024 * 1024
    }
}

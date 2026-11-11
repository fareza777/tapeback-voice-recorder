package com.f7developer.tapeback

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.f7developer.tapeback.core.sound.WavWriter
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.WaveformCodec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Seeds the debug app with realistic demo content for store screenshots and the Kiroween demo video.
 * It is skipped unless explicitly requested:
 *
 *   adb shell am instrument -w -e class com.f7developer.tapeback.DemoContentSeeder -e seed true \
 *       com.f7developer.tapeback.debug.test/androidx.test.runner.AndroidJUnitRunner
 *
 * Audio is synthesized "speech-like" material (formant-filtered pulses with syllable envelopes),
 * so the waveforms and VU meters show real movement. Nothing is downloaded.
 */
@RunWith(AndroidJUnit4::class)
class DemoContentSeeder {

    @Test
    fun seed(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Run with -e seed true", args.getString("seed") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TapeBackApp
        val c = app.container
        val day = 86_400_000L
        val now = System.currentTimeMillis()
        val rnd = Random(1985)

        // Replace the empty default tape created on first launch with the demo shelf.
        c.cassetteRepository.observeSummaries().first().filter { it.totalCount == 0 }
            .forEach { c.cassetteRepository.delete(it.cassette.id) }
        data class Tape(val title: String, val category: String, val color: Int, val skin: CassetteSkin, val cover: IntArray?)

        val tapes = listOf(
            Tape("Daily Diary", "Diary", 0xFFF3E6C8.toInt(), CassetteSkin.SMOKED_CLEAR, null),
            Tape("Road Trip '26", "Travel", 0xFFF7C948.toInt(), CassetteSkin.OCEAN_BLUE, intArrayOf(0xFFFF8A5B.toInt(), 0xFF5B2A86.toInt())),
            Tape("Song Ideas", "Music", 0xFFEF476F.toInt(), CassetteSkin.HOT_PINK, intArrayOf(0xFF2DE2E6.toInt(), 0xFFFF3EA5.toInt())),
            Tape("Family Voices", "Family", 0xFF7AD3C2.toInt(), CassetteSkin.CREAM_BEIGE, intArrayOf(0xFFFFD166.toInt(), 0xFFEF476F.toInt())),
            Tape("Dream Journal", "Dreams", 0xFFB794F6.toInt(), CassetteSkin.GHOST_GLOW, null),
            Tape("Meeting Notes", "Meetings", 0xFFFFFFFF.toInt(), CassetteSkin.CHROME_METAL, null),
        )
        val ids = tapes.map { t ->
            val cover = t.cover?.let { writeCover(c.files.coversDir, it) }
            c.cassetteRepository.create(t.title, t.category, t.color, t.skin, cover)
        }

        data class Entry(val tape: Int, val side: TapeSide, val title: String, val daysAgo: Long, val seconds: Int, val tags: List<String>, val fav: Boolean = false)

        val entries = listOf(
            Entry(0, TapeSide.A, "Morning pages", 0, 74, listOf("morning", "gratitude"), fav = true),
            Entry(0, TapeSide.A, "Walk by the river", 1, 52, listOf("outside")),
            Entry(0, TapeSide.A, "Long day, good day", 2, 96, listOf("work")),
            Entry(0, TapeSide.B, "Sunday reset", 3, 61, listOf("gratitude")),
            Entry(0, TapeSide.B, "First snow", 365, 40, listOf("winter"), fav = true),
            Entry(1, TapeSide.A, "Leaving the city", 6, 83, listOf("travel"), fav = true),
            Entry(1, TapeSide.A, "Gas station at 2 AM", 6, 37, listOf("travel", "night")),
            Entry(1, TapeSide.B, "Ocean, finally", 5, 58, listOf("travel")),
            Entry(2, TapeSide.A, "Chorus hum in D", 4, 28, listOf("melody", "ideas")),
            Entry(2, TapeSide.A, "Bridge lyrics", 4, 45, listOf("lyrics", "ideas"), fav = true),
            Entry(3, TapeSide.A, "Grandma's story", 9, 118, listOf("family"), fav = true),
            Entry(3, TapeSide.A, "Birthday song", 12, 33, listOf("family", "birthday")),
            Entry(4, TapeSide.A, "Flying over rooftops", 2, 49, listOf("dreams")),
            Entry(5, TapeSide.A, "Sprint planning", 1, 66, listOf("work")),
        )
        for (e in entries) {
            val created = now - e.daysAgo * day - rnd.nextLong(0, 6 * 3_600_000L)
            val (file, levels) = synthesizeSpeech(c.files.recordingsDir, e.seconds, rnd.nextInt())
            val id = c.recordingRepository.insert(ids[e.tape], e.side, e.title, file.absolutePath, e.seconds * 1000L, file.length(), created, levels)
            c.recordingRepository.setTags(id, e.tags)
            if (e.fav) c.recordingRepository.setFavorite(id, true)
        }

        // Time capsules: two sealed, one already opened.
        val capsules = listOf(
            Triple("Dear me, one year from now", 365L, "Did you finally learn the guitar solo?"),
            Triple("For Mia's 18th birthday", 365L * 5, "Open this with cake."),
        )
        for ((title, days, note) in capsules) {
            val (file, levels) = synthesizeSpeech(c.files.recordingsDir, 55, rnd.nextInt())
            val created = now - 2 * day
            val id = c.recordingRepository.insert(ids[0], TapeSide.B, title, file.absolutePath, 55_000, file.length(), created, levels)
            val unlockAt = now + days * day
            c.recordingRepository.seal(id, unlockAt, created, note)
            c.capsuleScheduler.schedule(id, unlockAt)
        }
        val (openedFile, openedLevels) = synthesizeSpeech(c.files.recordingsDir, 42, rnd.nextInt())
        val openedId = c.recordingRepository.insert(ids[4], TapeSide.B, "Message from last winter", openedFile.absolutePath, 42_000, openedFile.length(), now - 300 * day, openedLevels)
        c.recordingRepository.seal(openedId, now - day, now - 300 * day, "You made it through. Proud of you.")
        c.recordingRepository.markCapsuleNotified(openedId)

        c.settingsRepository.setRecordTarget(com.f7developer.tapeback.domain.model.RecordTarget(ids[0], TapeSide.A))
        c.settingsRepository.setOnboardingDone()
    }

    /** Voice-like test signal: glottal pulse train through two formant resonators, shaped into syllables. */
    private fun synthesizeSpeech(dir: File, seconds: Int, seed: Int): Pair<File, List<Float>> {
        val sr = 22_050
        val rnd = Random(seed)
        val n = sr * seconds
        val out = FloatArray(n)
        var phase = 0.0
        var f1y1 = 0.0; var f1y2 = 0.0; var f2y1 = 0.0; var f2y2 = 0.0
        var syllableEnd = 0
        var syllableStart = 0
        var pitch = 140.0
        var f1 = 600.0
        var f2 = 1_500.0
        var i = 0
        while (i < n) {
            if (i >= syllableEnd) {
                val pause = if (rnd.nextFloat() < 0.18f) rnd.nextInt(sr / 3, sr) else rnd.nextInt(sr / 30, sr / 10)
                syllableStart = i + pause
                syllableEnd = syllableStart + rnd.nextInt(sr / 9, sr / 3)
                pitch = 110.0 + rnd.nextDouble() * 90
                f1 = 350.0 + rnd.nextDouble() * 500
                f2 = 900.0 + rnd.nextDouble() * 1_500
            }
            val env = if (i < syllableStart) 0.0 else {
                val t = (i - syllableStart).toDouble() / (syllableEnd - syllableStart)
                sin(PI * t) * (0.55 + 0.45 * sin(PI * t * 3))
            }
            phase += pitch / sr
            val pulse = if (phase >= 1.0) { phase -= 1.0; 1.0 } else 0.0
            val src = pulse + (rnd.nextDouble() - 0.5) * 0.08
            val (a1, b1) = resonator(f1, 90.0, sr)
            val y1 = src + a1 * f1y1 - b1 * f1y2; f1y2 = f1y1; f1y1 = y1
            val (a2, b2) = resonator(f2, 140.0, sr)
            val y2 = src + a2 * f2y1 - b2 * f2y2; f2y2 = f2y1; f2y1 = y2
            out[i] = ((y1 * 0.06 + y2 * 0.035) * env).toFloat()
            i++
        }
        var peak = 0f
        for (v in out) peak = maxOf(peak, abs(v))
        if (peak > 0f) for (k in out.indices) out[k] = out[k] / peak * 0.7f
        val levels = ArrayList<Float>()
        val hop = sr / 20
        var s = 0
        while (s < n) {
            var m = 0f
            for (k in s until minOf(n, s + hop)) m = maxOf(m, abs(out[k]))
            levels += WaveformCodec.perceptual(m)
            s += hop
        }
        val file = File(dir, "demo-${UUID.randomUUID()}.wav")
        file.writeBytes(WavWriter.encode(out, sr))
        return file to levels
    }

    private fun resonator(freq: Double, bandwidth: Double, sr: Int): Pair<Double, Double> {
        val r = exp(-PI * bandwidth / sr)
        return (2 * r * kotlin.math.cos(2 * PI * freq / sr)) to (r * r)
    }

    private fun writeCover(dir: File, colors: IntArray): String {
        val bmp = Bitmap.createBitmap(768, 768, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 768f, 768f, colors[0], colors[1], Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 768f, 768f, paint)
        paint.shader = null
        paint.color = 0x55FFFFFF
        canvas.drawCircle(560f, 250f, 150f, paint)
        paint.color = 0x33000000
        for (k in 0 until 6) canvas.drawRect(0f, 480f + k * 46f, 768f, 500f + k * 46f, paint)
        val file = File(dir, "cover-${UUID.randomUUID()}.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file.absolutePath
    }
}

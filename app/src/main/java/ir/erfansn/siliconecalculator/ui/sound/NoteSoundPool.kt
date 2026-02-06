/*
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.erfansn.siliconecalculator.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * A lightweight SoundPool wrapper that generates note samples at runtime and plays
 * them with low latency. This avoids committing binary audio files into the repository.
 *
 * Mapping:
 * - 0 -> G3 (Low So)
 * - 1 -> C4 (Do)
 * - 2 -> D4 (Re)
 * - 3 -> E4 (Mi)
 * - 4 -> F4 (Fa)
 * - 5 -> G4 (So)
 * - 6 -> A4 (La)
 * - 7 -> B4 (Ti)
 * - 8 -> C5 (High Do)
 * - 9 -> D5 (High Re)
 */
class NoteSoundPool(
    context: Context,
    maxStreams: Int = 8,
) {
    private companion object {
        private const val TAG = "NoteSoundPool"
    }

    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(maxStreams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val pendingSoundIds = mutableSetOf<Int>()
    private val digitToSoundId = mutableMapOf<Char, Int>()
    private val digitToLoaded = mutableMapOf<Char, Boolean>()
    private val digitToFrequencyHz = mutableMapOf<Char, Double>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                pendingSoundIds.remove(sampleId)
                digitToSoundId.entries
                    .firstOrNull { it.value == sampleId }
                    ?.key
                    ?.let { digitToLoaded[it] = true }
            }
        }

        // Generate in cache dir so SoundPool can load from file path.
        val dir = File(appContext.cacheDir, "note_sounds").also { it.mkdirs() }

        // G3 + C4..B4 + C5 + D5
        val notes = listOf(
            '0' to 196.00, // G3
            '1' to 261.63, // C4
            '2' to 293.66, // D4
            '3' to 329.63, // E4
            '4' to 349.23, // F4
            '5' to 392.00, // G4
            '6' to 440.00, // A4
            '7' to 493.88, // B4
            '8' to 523.25, // C5
            '9' to 587.33, // D5
        )

        // Phone speakers often attenuate low frequencies and short pure sine notes can sound quiet
        // or "thin". We add harmonics to make notes more audible without changing the perceived
        // pitch (scheme A).
        val harmonicWeights = doubleArrayOf(1.0, 0.35, 0.20)

        for ((digit, freq) in notes) {
            digitToFrequencyHz[digit] = freq
            val wavFile = File(dir, "note_${digit}.wav")
            val wavBytes = generateToneWav(
                frequencyHz = freq,
                durationMs = 110,
                sampleRateHz = 44_100,
                amplitude = 0.44,
                harmonicWeights = harmonicWeights,
            )

            // Always refresh samples to avoid stale cached sounds after tuning.
            wavFile.writeBytes(wavBytes)

            val soundId = soundPool.load(wavFile.absolutePath, 1)
            digitToSoundId[digit] = soundId
            digitToLoaded[digit] = false
            pendingSoundIds.add(soundId)
        }

        // Print mapping once for debugging/verification.
        Log.i(TAG, "Digit note map:")
        for ((digit, freq) in notes) {
            Log.i(TAG, "  digit=$digit -> ${noteNameForDigit(digit)} (${String.format("%.2f", freq)}Hz)")
        }
    }

    fun playDigit(digit: Char, volume: Float = 1f) {
        if (digit !in ('0'..'9')) return
        if (digitToLoaded[digit] != true) return

        val soundId = digitToSoundId[digit] ?: return
        val freq = digitToFrequencyHz[digit]
        Log.d(
            TAG,
            "play digit=$digit -> ${noteNameForDigit(digit)}" +
                (freq?.let { " (${String.format("%.2f", it)}Hz)" } ?: "")
        )
        // Allow overlap by always using a new stream (SoundPool handles this).
        soundPool.play(
            soundId,
            /* leftVolume = */ volume,
            /* rightVolume = */ volume,
            /* priority = */ 1,
            /* loop = */ 0,
            /* rate = */ 1f,
        )
    }

    fun release() {
        soundPool.release()
    }
}

private fun noteNameForDigit(digit: Char): String = when (digit) {
    '0' -> "G3"
    '1' -> "C4"
    '2' -> "D4"
    '3' -> "E4"
    '4' -> "F4"
    '5' -> "G4"
    '6' -> "A4"
    '7' -> "B4"
    '8' -> "C5"
    '9' -> "D5"
    else -> "?"
}

private fun generateToneWav(
    frequencyHz: Double,
    durationMs: Int,
    sampleRateHz: Int,
    amplitude: Double,
    harmonicWeights: DoubleArray,
): ByteArray {
    val numSamples = (durationMs * sampleRateHz) / 1000
    val mixed = DoubleArray(numSamples)

    // Simple attack/release to reduce clicks.
    val attackSamples = (sampleRateHz * 0.004).toInt().coerceAtLeast(1) // ~4ms
    val releaseSamples = (sampleRateHz * 0.010).toInt().coerceAtLeast(1) // ~10ms

    var maxAbs = 0.0
    for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRateHz
        var raw = 0.0
        for (h in harmonicWeights.indices) {
            val harmonic = h + 1
            raw += harmonicWeights[h] * sin(2.0 * PI * frequencyHz * harmonic * t)
        }

        val env = when {
            i < attackSamples -> i.toDouble() / attackSamples
            i > numSamples - releaseSamples -> (numSamples - i).toDouble() / releaseSamples
            else -> 1.0
        }.coerceIn(0.0, 1.0)

        val v = raw * env
        mixed[i] = v
        val abs = kotlin.math.abs(v)
        if (abs > maxAbs) maxAbs = abs
    }

    val scale = if (maxAbs > 0.0) (amplitude / maxAbs) else 0.0
    val pcmBytes = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
        for (i in 0 until numSamples) {
            val v = (mixed[i] * scale * Short.MAX_VALUE).toInt()
            putShort(v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
    }.array()

    return wavWrapPcm16Mono(
        pcmData = pcmBytes,
        sampleRateHz = sampleRateHz,
    )
}

private fun wavWrapPcm16Mono(
    pcmData: ByteArray,
    sampleRateHz: Int,
): ByteArray {
    val numChannels = 1
    val bitsPerSample = 16
    val byteRate = sampleRateHz * numChannels * bitsPerSample / 8
    val blockAlign = (numChannels * bitsPerSample / 8).toShort()
    val subChunk2Size = pcmData.size
    val chunkSize = 36 + subChunk2Size

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put(byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte()))
        putInt(chunkSize)
        put(byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()))

        put(byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte()))
        putInt(16) // PCM fmt chunk size
        putShort(1) // AudioFormat PCM = 1
        putShort(numChannels.toShort())
        putInt(sampleRateHz)
        putInt(byteRate)
        putShort(blockAlign)
        putShort(bitsPerSample.toShort())

        put(byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()))
        putInt(subChunk2Size)
    }.array()

    return ByteArray(header.size + pcmData.size).also { out ->
        System.arraycopy(header, 0, out, 0, header.size)
        System.arraycopy(pcmData, 0, out, header.size, pcmData.size)
    }
}

package com.example.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Single-Shot Track & Field Starter Pistol Acoustic Engine ("1 Bunyi Tembakan Tunggal").
 *
 * Generates strictly ONE (1) crisp, instantaneous starter pistol "BANG!" with a single
 * monotonic decay envelope (~260 ms total). Zero multi-tap echoes or repeated bursts,
 * so it never sounds like multiple bullets firing.
 */
object ElectronicStarterGunAudio {

    @Volatile
    private var cachedSingleShotPcm: ShortArray? = null

    private const val SAMPLE_RATE = 48000
    private const val DURATION_MS = 280

    private fun buildSingleShotStarterPistolWaveform(): ShortArray {
        cachedSingleShotPcm?.let { return it }

        val numSamples = (SAMPLE_RATE * (DURATION_MS / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        val random = Random(1001)

        // Single-stage filters for a cohesive 1-shot gunpowder snap + body
        var lpSnap = 0.0
        var lpBody = 0.0
        var lpLow = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val white = random.nextDouble() * 2.0 - 1.0

            // Filter bands: high snap (~5 kHz), mid punch (~1.2 kHz), low chest thump (~180 Hz)
            lpSnap += 0.65 * (white - lpSnap)
            lpBody += 0.22 * (white - lpBody)
            lpLow += 0.055 * (white - lpLow)

            // 1. Instantaneous single primer cap snap (0 to 2.5 ms ONLY)
            val primerSnap = when {
                t < 0.0005 -> 1.0
                t < 0.0012 -> -0.88
                t < 0.0025 -> lpSnap * exp(-t * 900.0) * 1.2
                else -> 0.0
            }

            // 2. Single cohesive muzzle pressure thump (0 to 32 ms, downward pitch sweep 140Hz -> 48Hz)
            val pressureFreq = 48.0 + 95.0 * exp(-t * 130.0)
            val muzzlePunch = sin(2.0 * Math.PI * pressureFreq * t) * exp(-t * 85.0) * 1.25

            // 3. Single monotonic gunpowder burst decay (NO secondary peaks, NO delay taps!)
            val singleShotEnvelope = exp(-t * 32.0)
            val gunpowderBurst = (lpSnap * 0.45 + lpBody * 1.05 + lpLow * 1.15) * singleShotEnvelope

            // Smooth fade-out near the end of the 280ms window
            val windowFade = ((DURATION_MS / 1000.0 - t) / 0.04).coerceIn(0.0, 1.0)

            val combined = (primerSnap + muzzlePunch + gunpowderBurst) * windowFade
            val saturated = tanh(combined * 2.5)

            pcm[i] = (saturated.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }

        cachedSingleShotPcm = pcm
        return pcm
    }

    fun playExplosiveStarterGunShot(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                val audioData = buildSingleShotStarterPistolWaveform()
                val numSamples = audioData.size

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                        }
                    }
                    .build()

                audioTrack.write(audioData, 0, numSamples)
                audioTrack.setVolume(AudioTrack.getMaxVolume())
                audioTrack.play()

                delay(DURATION_MS.toLong() + 60L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

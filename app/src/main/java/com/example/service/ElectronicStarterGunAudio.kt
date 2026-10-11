package com.example.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * High-Powered Electronic Starter Gun Audio Synthesizer (Piezo Bang / Gun Shot Blast)
 * World Athletics style starter transducer: generates real explosive impulse wave
 * with sharp 0.8ms attack, multi-frequency acoustic pressure blast, and stadium resonance decay.
 * Does NOT require external sound files, 100% works offline on any Android speaker.
 */
object ElectronicStarterGunAudio {

    fun playExplosiveStarterGunShot(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationMs = 420 // 420 milliseconds full shot impact & stadium reverb
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val audioData = ShortArray(numSamples)

                val random = Random(System.currentTimeMillis())

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate

                    // Explosive Envelope: instantaneous punch attack with exponential decay
                    val envelope = kotlin.math.exp(-t * 22.0)

                    // 1. Initial High Pressure Acoustic Blast (White/Brown noise transient)
                    val noise = (random.nextDouble() * 2.0 - 1.0) * kotlin.math.exp(-t * 45.0)

                    // 2. Heavy Sub-Bass Gun Muzzle Boom (75 Hz - 140 Hz downward pitch glide)
                    val subFrequency = 140.0 * kotlin.math.exp(-t * 28.0) + 55.0
                    val subBass = kotlin.math.sin(2.0 * Math.PI * subFrequency * t) * 0.9

                    // 3. Crisp Metallic Piezo Click & Stadium Track Resonance (850 Hz & 420 Hz)
                    val piezoSnap = (kotlin.math.sin(2.0 * Math.PI * 850.0 * t) * 0.5 + 
                                     kotlin.math.sin(2.0 * Math.PI * 420.0 * t) * 0.35) * kotlin.math.exp(-t * 18.0)

                    // Composite synthetic bang signal
                    val signal = (noise * 0.65 + subBass * 0.45 + piezoSnap * 0.35) * envelope

                    // Soft clip to prevent harsh distortion while maximizing peak loudness
                    val clipped = signal.coerceIn(-1.0, 1.0)
                    audioData[i] = (clipped * Short.MAX_VALUE).toInt().toShort()
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(audioData, 0, numSamples)
                audioTrack.play()

                // Release AudioTrack after playback finishes
                kotlinx.coroutines.delay(durationMs.toLong() + 50L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

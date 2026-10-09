package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * AudioGunDetector:
 * Mengesan bunyi tembakan pistol permulaan (starter gun) atau tepukan kuat untuk memicu pemasa larian.
 *
 * Dilengkapi perlindungan ralat selamat (Defensive Fallback):
 * - Menyemak kebenaran android.permission.RECORD_AUDIO secara dinamik sebelum mengakses audio hardware.
 * - Menguji pelbagai konfigurasi sample rate standard (44100, 16000, 8000 Hz) dan AudioSource (MIC, DEFAULT, VOICE_RECOGNITION).
 * - Menangkap ralat AudioFlinger status -1 / -20 dengan selamat tanpa merosakkan aplikasi atau sistem crash.
 */
class AudioGunDetector(
    private val context: Context? = null,
    private val onGunFired: () -> Unit
) {
    companion object {
        private const val TAG = "AudioGunDetector"
        private const val THRESHOLD_AMPLITUDE = 16000
    }

    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private var listeningJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startListening(coroutineScope: CoroutineScope) {
        if (isListening) return

        // 1. Semak kebenaran runtime RECORD_AUDIO jika Context tersedia
        if (context != null) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Kebenaran RECORD_AUDIO belum diberikan. Audio Gun Detector dimatikan secara selamat.")
                return
            }
        }

        // 2. Cuba inisialisasi AudioRecord dengan fallback parameter yang serasi
        val record = tryCreateAudioRecord()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "Audio hardware/AudioFlinger tidak tersedia atau tidak menyokong rakaman audio. Audio Gun Detector dilangkau.")
            record?.release()
            return
        }

        audioRecord = record

        try {
            audioRecord?.startRecording()
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.w(TAG, "AudioRecord gagal memulakan rakaman.")
                stopListening()
                return
            }
            isListening = true

            val bufferSize = 2048
            listeningJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize)
                while (isListening && isActive) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        var maxPeak = 0
                        for (i in 0 until read) {
                            val amp = abs(buffer[i].toInt())
                            if (amp > maxPeak) {
                                maxPeak = amp
                            }
                        }

                        // Sharp impulse trigger (bunyi tembakan pistol / starter gun)
                        if (maxPeak > THRESHOLD_AMPLITUDE) {
                            withContext(Dispatchers.Main) {
                                onGunFired()
                            }
                            // Debounce pencegahan trigger berganda
                            delay(1500)
                        }
                    } else if (read < 0) {
                        // Ralat bacaan AudioRecord
                        delay(100)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ralat semasa mengendalikan rakaman AudioGun: ${e.message}")
            stopListening()
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryCreateAudioRecord(): AudioRecord? {
        val sampleRates = intArrayOf(44100, 16000, 8000)
        val audioSources = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        for (source in audioSources) {
            for (rate in sampleRates) {
                try {
                    val minBuf = AudioRecord.getMinBufferSize(
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minBuf <= 0) continue

                    val bufSize = minBuf.coerceAtLeast(2048)
                    val record = AudioRecord(
                        source,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufSize
                    )

                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        Log.i(TAG, "AudioRecord berjaya diinisialisasi (Source: $source, Rate: $rate, Buf: $bufSize)")
                        return record
                    } else {
                        record.release()
                    }
                } catch (t: Throwable) {
                    // Tangkap ralat AudioFlinger / hardware tidak tersedia
                    Log.d(TAG, "Percubaan AudioRecord (Source: $source, Rate: $rate) tidak berjaya: ${t.message}")
                }
            }
        }
        return null
    }

    fun stopListening() {
        isListening = false
        listeningJob?.cancel()
        listeningJob = null
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // Ignored
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored
        }
        audioRecord = null
    }
}

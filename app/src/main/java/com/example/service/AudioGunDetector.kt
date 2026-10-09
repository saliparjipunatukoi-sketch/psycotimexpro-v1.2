package com.example.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.abs

class AudioGunDetector(
    private val onGunFired: () -> Unit
) {
    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private var listeningJob: Job? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

    // Decibel / amplitude threshold for starter gun or loud clap
    private val thresholdAmplitude = 18000

    @SuppressLint("MissingPermission")
    fun startListening(coroutineScope: CoroutineScope) {
        if (isListening) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
            isListening = true

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

                        // Sharp impulse trigger
                        if (maxPeak > thresholdAmplitude) {
                            withContext(Dispatchers.Main) {
                                onGunFired()
                            }
                            // Debounce
                            delay(1500)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isListening = false
        }
    }

    fun stopListening() {
        isListening = false
        listeningJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }
}

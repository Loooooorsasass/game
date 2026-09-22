package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var bgmJob: Job? = null

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) startBgm() else stopBgm()
        }

    private val sampleRate = 22050

    fun startBgm() {
        if (!isMusicEnabled || bgmJob?.isActive == true) return
        bgmJob = scope.launch {
            val chords = listOf(
                listOf(261.63, 329.63, 392.00, 523.25), // C Maj
                listOf(220.00, 261.63, 329.63, 440.00), // A Min
                listOf(174.61, 220.00, 261.63, 349.23), // F Maj
                listOf(196.00, 246.94, 293.66, 392.00)  // G Maj
            )
            while (isActive && isMusicEnabled) {
                for (chord in chords) {
                    for (note in chord) {
                        if (!isActive || !isMusicEnabled) break
                        playTone(note, durationMs = 180, volume = 0.08f, decay = true)
                        delay(220)
                    }
                }
            }
        }
    }

    fun stopBgm() {
        bgmJob?.cancel()
        bgmJob = null
    }

    fun playStep() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 480.0, durationMs = 25, volume = 0.12f, decay = true)
        }
    }

    fun playBump() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 110.0, durationMs = 60, volume = 0.25f, decay = true)
        }
    }

    fun playWin() {
        if (!isSoundEnabled) return
        scope.launch {
            val melody = listOf(523.25, 659.25, 783.99, 1046.50)
            for (f in melody) {
                playTone(freq = f, durationMs = 100, volume = 0.35f, decay = false)
                delay(90)
            }
            playTone(freq = 1046.50, durationMs = 280, volume = 0.4f, decay = true)
        }
    }

    fun playLose() {
        if (!isSoundEnabled) return
        scope.launch {
            val melody = listOf(392.00, 329.63, 261.63, 196.00)
            for (f in melody) {
                playTone(freq = f, durationMs = 120, volume = 0.3f, decay = true)
                delay(110)
            }
        }
    }

    fun playStarChime() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 1318.51, durationMs = 80, volume = 0.28f, decay = true)
            delay(60)
            playTone(freq = 1760.00, durationMs = 180, volume = 0.32f, decay = true)
        }
    }

    fun playClick() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 880.0, durationMs = 15, volume = 0.15f, decay = true)
        }
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Float, decay: Boolean) {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val angle = 2.0 * PI * freq * time
            val env = if (decay) {
                (1.0 - (i.toDouble() / numSamples)).coerceIn(0.0, 1.0)
            } else {
                1.0
            }
            val sample = (sin(angle) * Short.MAX_VALUE * volume * env).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(numSamples * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        track.notificationMarkerPosition = numSamples
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
                t?.stop()
                t?.release()
            }
            override fun onPeriodicNotification(t: AudioTrack?) {}
        })
    }

    fun release() {
        stopBgm()
    }
}

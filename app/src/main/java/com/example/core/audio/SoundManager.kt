package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * SoundManager quản lý âm thanh cho trò chơi:
 * - Dùng SoundPool để phát tức thời các hiệu ứng âm thanh (SFX) độ trễ cực thấp:
 *   + Tiếng click nút (button click)
 *   + Tiếng bước chân di chuyển (move step)
 *   + Tiếng va vào tường (wall bump)
 *   + Tiếng hoàn thành màn chơi / chuông thưởng (level completion ping / win)
 *   + Tiếng thất bại (level loss)
 * - Dùng MediaPlayer để phát nhạc nền lặp vô tận (BGM) êm dịu, hỗ trợ loop, pause/resume theo vòng đời ứng dụng.
 */
class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                resumeBgm()
            } else {
                pauseBgm()
            }
        }

    // SoundPool cho các hiệu ứng SFX với độ trễ thấp nhất
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // ID các âm thanh trong SoundPool
    private var soundIdClick: Int = 0
    private var soundIdStep: Int = 0
    private var soundIdBump: Int = 0
    private var soundIdWin: Int = 0
    private var soundIdLose: Int = 0
    private var soundIdStarPing: Int = 0

    // MediaPlayer cho nhạc nền (Background Music - BGM)
    private var bgmPlayer: MediaPlayer? = null
    private var isBgmPrepared: Boolean = false

    init {
        loadSounds()
        prepareBgmPlayer()
    }

    /**
     * Tạo file WAV bộ đệm PCM tự sinh để nạp vào SoundPool một cách độc lập,
     * không phụ thuộc vào tài nguyên nhị phân bên ngoài và không bao giờ lỗi file thiếu.
     */
    private fun loadSounds() {
        try {
            val cacheDir = File(context.cacheDir, "audio_sfx").apply { mkdirs() }

            // 1. Button click: Âm click sắc gọn cao độ 960Hz
            val clickFile = File(cacheDir, "click.wav")
            writeWavFile(clickFile, generatePcmTone(freq = 960.0, durationMs = 20, volume = 0.4f, decay = true))
            soundIdClick = soundPool.load(clickFile.absolutePath, 1)

            // 2. Move sound: Âm bước chân gõ nhẹ 520Hz
            val stepFile = File(cacheDir, "step.wav")
            writeWavFile(stepFile, generatePcmTone(freq = 520.0, durationMs = 30, volume = 0.35f, decay = true))
            soundIdStep = soundPool.load(stepFile.absolutePath, 1)

            // 3. Wall bump: Âm trầm đục dội lại 120Hz
            val bumpFile = File(cacheDir, "bump.wav")
            writeWavFile(bumpFile, generatePcmTone(freq = 120.0, durationMs = 70, volume = 0.55f, decay = true))
            soundIdBump = soundPool.load(bumpFile.absolutePath, 1)

            // 4. Level completion ping: Chuỗi âm vang chuông hân hoan (Arpeggio C - E - G - C cao)
            val winFile = File(cacheDir, "win_ping.wav")
            writeWavFile(winFile, generateMelodyPcm(
                notes = listOf(
                    523.25 to 80,   // C5
                    659.25 to 80,   // E5
                    783.99 to 90,   // G5
                    1046.50 to 260  // C6
                ),
                volume = 0.6f
            ))
            soundIdWin = soundPool.load(winFile.absolutePath, 1)

            // 5. Lose sound
            val loseFile = File(cacheDir, "lose.wav")
            writeWavFile(loseFile, generateMelodyPcm(
                notes = listOf(
                    392.00 to 110,  // G4
                    329.63 to 110,  // E4
                    261.63 to 110,  // C4
                    196.00 to 220   // G3
                ),
                volume = 0.45f
            ))
            soundIdLose = soundPool.load(loseFile.absolutePath, 1)

            // 6. Star chime / Ping
            val starFile = File(cacheDir, "star_ping.wav")
            writeWavFile(starFile, generateMelodyPcm(
                notes = listOf(
                    1318.51 to 70,  // E6
                    1760.00 to 180  // A6
                ),
                volume = 0.5f
            ))
            soundIdStarPing = soundPool.load(starFile.absolutePath, 1)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error generating SFX files", e)
        }
    }

    /**
     * Chuẩn bị MediaPlayer phát nhạc nền (BGM) ambient êm dịu, có loop vô tận
     */
    private fun prepareBgmPlayer() {
        try {
            val cacheDir = File(context.cacheDir, "audio_bgm").apply { mkdirs() }
            val bgmFile = File(cacheDir, "ambient_bgm.wav")

            // Tạo đoạn nhạc lặp êm dịu 4 hợp âm (Cmaj -> Amin -> Fmaj -> Gmaj) dài ~7.5 giây
            val bgmChords = listOf(
                listOf(261.63, 329.63, 392.00), // C Maj
                listOf(220.00, 261.63, 329.63), // A Min
                listOf(174.61, 220.00, 261.63), // F Maj
                listOf(196.00, 246.94, 293.66)  // G Maj
            )
            val bgmPcm = generatePolyphonicChordsPcm(bgmChords, chordDurationMs = 1800, volume = 0.12f)
            writeWavFile(bgmFile, bgmPcm)

            bgmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(bgmFile.absolutePath)
                isLooping = true
                setVolume(0.35f, 0.35f)
                setOnPreparedListener {
                    isBgmPrepared = true
                    if (isMusicEnabled) {
                        start()
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error preparing MediaPlayer BGM", e)
        }
    }

    // ==========================================
    // PHÁT HIỆU ỨNG ÂM THANH (SFX) BẰNG SOUNDPOOL
    // ==========================================

    /**
     * Phát tiếng gõ nhẹ của bước di chuyển (Move sound)
     */
    fun playStep() {
        if (!isSoundEnabled || soundIdStep == 0) return
        soundPool.play(soundIdStep, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    /**
     * Phát tiếng va chạm tường khi bị chặn (Wall bump sound)
     */
    fun playBump() {
        if (!isSoundEnabled || soundIdBump == 0) return
        soundPool.play(soundIdBump, 0.8f, 0.8f, 2, 0, 1.0f)
    }

    /**
     * Phát âm thanh hoàn thành màn chơi (Level completion ping / Win)
     */
    fun playWin() {
        if (!isSoundEnabled || soundIdWin == 0) return
        soundPool.play(soundIdWin, 0.95f, 0.95f, 3, 0, 1.0f)
    }

    /**
     * Phát âm thanh thua cuộc
     */
    fun playLose() {
        if (!isSoundEnabled || soundIdLose == 0) return
        soundPool.play(soundIdLose, 0.8f, 0.8f, 2, 0, 1.0f)
    }

    /**
     * Phát âm thanh nhấp nút (Button click)
     */
    fun playClick() {
        if (!isSoundEnabled || soundIdClick == 0) return
        soundPool.play(soundIdClick, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    /**
     * Phát âm thanh chuông nhận sao/thưởng (Star chime ping)
     */
    fun playStarChime() {
        if (!isSoundEnabled || soundIdStarPing == 0) return
        soundPool.play(soundIdStarPing, 0.85f, 0.85f, 2, 0, 1.0f)
    }

    // ==========================================
    // QUẢN LÝ NHẠC NỀN (BGM) BẰNG MEDIAPLAYER
    // ==========================================

    /**
     * Khởi động phát nhạc nền
     */
    fun startBgm() {
        if (!isMusicEnabled) return
        try {
            bgmPlayer?.let { player ->
                if (isBgmPrepared && !player.isPlaying) {
                    player.start()
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error starting BGM", e)
        }
    }

    /**
     * Dừng phát nhạc nền
     */
    fun stopBgm() {
        pauseBgm()
    }

    /**
     * Tạm dừng nhạc nền (khi người chơi tắt nhạc hoặc app vào background)
     */
    fun pauseBgm() {
        try {
            bgmPlayer?.let { player ->
                if (isBgmPrepared && player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error pausing BGM", e)
        }
    }

    /**
     * Tiếp tục phát nhạc nền (khi người chơi bật lại nhạc hoặc app trở lại foreground)
     */
    fun resumeBgm() {
        if (isMusicEnabled) {
            startBgm()
        }
    }

    /**
     * Giải phóng toàn bộ tài nguyên SoundPool và MediaPlayer khi Activity hủy
     */
    fun release() {
        try {
            bgmPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            bgmPlayer = null
            isBgmPrepared = false
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing MediaPlayer", e)
        }

        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing SoundPool", e)
        }
    }

    // ==========================================
    // UTILITIES TỰ SINH DỮ LIỆU WAV & PCM
    // ==========================================

    companion object {
        private const val SAMPLE_RATE = 22050

        private fun generatePcmTone(freq: Double, durationMs: Int, volume: Float, decay: Boolean): ShortArray {
            val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt().coerceAtLeast(1)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val time = i.toDouble() / SAMPLE_RATE
                val angle = 2.0 * PI * freq * time
                val env = if (decay) (1.0 - (i.toDouble() / numSamples)).coerceIn(0.0, 1.0) else 1.0
                val sample = (sin(angle) * Short.MAX_VALUE * volume * env).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return buffer
        }

        private fun generateMelodyPcm(notes: List<Pair<Double, Int>>, volume: Float): ShortArray {
            val buffers = notes.map { (freq, durationMs) ->
                generatePcmTone(freq, durationMs, volume, decay = true)
            }
            val totalSamples = buffers.sumOf { it.size }
            val result = ShortArray(totalSamples)
            var offset = 0
            for (b in buffers) {
                System.arraycopy(b, 0, result, offset, b.size)
                offset += b.size
            }
            return result
        }

        private fun generatePolyphonicChordsPcm(
            chords: List<List<Double>>,
            chordDurationMs: Int,
            volume: Float
        ): ShortArray {
            val samplesPerChord = (SAMPLE_RATE * chordDurationMs / 1000.0).toInt()
            val totalSamples = samplesPerChord * chords.size
            val result = ShortArray(totalSamples)

            for ((cIdx, chord) in chords.withIndex()) {
                val startIdx = cIdx * samplesPerChord
                for (i in 0 until samplesPerChord) {
                    val time = i.toDouble() / SAMPLE_RATE
                    // Fade in và Fade out nhẹ ở 2 đầu mỗi hợp âm để không có tiếng lách cách (click)
                    val fadeDuration = 0.08 * samplesPerChord
                    val envelope = when {
                        i < fadeDuration -> (i / fadeDuration)
                        i > samplesPerChord - fadeDuration -> ((samplesPerChord - i) / fadeDuration)
                        else -> 1.0
                    }

                    var mixedSample = 0.0
                    for (freq in chord) {
                        mixedSample += sin(2.0 * PI * freq * time)
                    }
                    mixedSample /= chord.size.toDouble()

                    val sampleValue = (mixedSample * Short.MAX_VALUE * volume * envelope).toInt()
                    result[startIdx + i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            return result
        }

        private fun writeWavFile(file: File, pcmData: ShortArray) {
            val totalAudioLen = (pcmData.size * 2).toLong()
            val totalDataLen = totalAudioLen + 36
            val byteRate = (SAMPLE_RATE * 1 * 2).toLong()

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xffL).toByte()
            header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
            header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
            header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // PCM
            header[21] = 0
            header[22] = 1 // Mono (1 channel)
            header[23] = 0
            header[24] = (SAMPLE_RATE and 0xff).toByte()
            header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
            header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
            header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xffL).toByte()
            header[29] = ((byteRate shr 8) and 0xffL).toByte()
            header[30] = ((byteRate shr 16) and 0xffL).toByte()
            header[31] = ((byteRate shr 24) and 0xffL).toByte()
            header[32] = 2 // block align (1 channel * 16 bits/sample / 8)
            header[33] = 0
            header[34] = 16 // bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xffL).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmData) {
                byteBuffer.putShort(s)
            }

            FileOutputStream(file).use { out ->
                out.write(header)
                out.write(byteBuffer.array())
                out.flush()
            }
        }
    }
}

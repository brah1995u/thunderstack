package studio.cortex.thunderstack.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import studio.cortex.thunderstack.R
import studio.cortex.thunderstack.model.PlayerProgress

/** Plays the original Thunder Stack audio pack and coordinated haptics. */
class ThunderFeedbackController(context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(audioAttributes)
        .build()
    private val loadedSounds = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSounds += sampleId
        }
    }

    private val sounds = mapOf(
        FeedbackCue.SELECT to SoundSpec(soundPool.load(context, R.raw.sfx_select, 1), .34f),
        FeedbackCue.PLACE to SoundSpec(soundPool.load(context, R.raw.sfx_place, 1), .58f),
        FeedbackCue.PERFECT to SoundSpec(soundPool.load(context, R.raw.sfx_perfect, 1), .72f),
        FeedbackCue.CROOKED to SoundSpec(soundPool.load(context, R.raw.sfx_crooked, 1), .64f),
        FeedbackCue.COLLAPSE to SoundSpec(soundPool.load(context, R.raw.sfx_collapse, 1), .88f),
        FeedbackCue.REWARD to SoundSpec(soundPool.load(context, R.raw.sfx_reward, 1), .76f),
        FeedbackCue.ERROR to SoundSpec(soundPool.load(context, R.raw.sfx_error, 1), .58f),
        FeedbackCue.BOOSTER to SoundSpec(soundPool.load(context, R.raw.sfx_booster, 1), .78f),
        FeedbackCue.VICTORY to SoundSpec(soundPool.load(context, R.raw.sfx_victory, 1), .82f),
    )
    private val ambience = MediaPlayer.create(context, R.raw.music_olympus)?.apply {
        isLooping = true
        setVolume(.16f, .16f)
    }
    private var progress = PlayerProgress()
    private var foreground = true

    fun updateSettings(value: PlayerProgress) {
        val musicChanged = progress.musicEnabled != value.musicEnabled
        progress = value
        if (musicChanged || ambience?.isPlaying != true) updateAmbience()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        updateAmbience()
    }

    fun handle(event: FeedbackEvent) {
        if (!foreground) return
        if (progress.soundEnabled) {
            sounds[event.cue]?.let { sound ->
                if (sound.id in loadedSounds) {
                    soundPool.play(sound.id, sound.volume, sound.volume, 1, 0, 1f)
                }
            }
        }
        if (progress.hapticsEnabled && vibrator.hasVibrator()) {
            val duration = when (event.haptic) {
                HapticStrength.LIGHT -> 18L
                HapticStrength.MEDIUM -> 34L
                HapticStrength.STRONG -> 58L
            }
            val amplitude = when (event.haptic) {
                HapticStrength.LIGHT -> 45
                HapticStrength.MEDIUM -> 100
                HapticStrength.STRONG -> 180
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(duration)
            }
        }
    }

    fun release() {
        runCatching { ambience?.stop() }
        ambience?.release()
        soundPool.release()
    }

    private fun updateAmbience() {
        if (!foreground || !progress.musicEnabled) {
            if (ambience?.isPlaying == true) ambience.pause()
            return
        }
        runCatching { ambience?.start() }
    }

    private data class SoundSpec(val id: Int, val volume: Float)
}

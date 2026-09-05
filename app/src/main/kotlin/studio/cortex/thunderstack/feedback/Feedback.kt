package studio.cortex.thunderstack.feedback

enum class FeedbackCue { SELECT, PLACE, PERFECT, CROOKED, COLLAPSE, REWARD, ERROR, BOOSTER, VICTORY }
enum class HapticStrength { LIGHT, MEDIUM, STRONG }
data class FeedbackEvent(val cue: FeedbackCue, val haptic: HapticStrength = HapticStrength.LIGHT)

package studio.cortex.thunderstack.model

enum class CrystalRushPhase { READY, PLAYING, FINISHED }

enum class RushItemKind {
    CRYSTAL,
    COIN,
    GEM_RED,
    GEM_BLUE,
    GEM_PURPLE,
    GEM_GREEN,
    CRACKED,
    LIGHTNING,
}

data class RushItem(
    val id: Int,
    val kind: RushItemKind,
    val x: Float,
    val y: Float,
    val speed: Float,
    val rotation: Float,
)

data class CrystalRushState(
    val phase: CrystalRushPhase = CrystalRushPhase.READY,
    val rewardEligible: Boolean = true,
    val rewardCommitted: Boolean = false,
    val elapsedSeconds: Float = 0f,
    val durationSeconds: Float = 30f,
    val items: List<RushItem> = emptyList(),
    val magnetX: Float = 0.5f,
    val score: Int = 0,
    val hearts: Int = 3,
    val multiplier: Int = 1,
    val combo: Int = 0,
    val shieldSeconds: Float = 0f,
    val stunSeconds: Float = 0f,
    val nextSpawnAt: Float = 0.35f,
    val spawnSerial: Int = 0,
    val lastCatchKind: RushItemKind? = null,
    val feedbackToken: Long = 0L,
    val isPaused: Boolean = false,
) {
    val remainingSeconds: Float get() = (durationSeconds - elapsedSeconds).coerceAtLeast(0f)
}

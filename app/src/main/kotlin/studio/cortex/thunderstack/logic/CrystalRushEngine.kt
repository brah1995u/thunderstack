package studio.cortex.thunderstack.logic

import kotlin.math.abs
import studio.cortex.thunderstack.model.CrystalRushPhase
import studio.cortex.thunderstack.model.CrystalRushState
import studio.cortex.thunderstack.model.RushItem
import studio.cortex.thunderstack.model.RushItemKind

/** Deterministic, renderer-independent rules for the 30-second Crystal Rush mode. */
class CrystalRushEngine {
    fun create(rewardEligible: Boolean): CrystalRushState = CrystalRushState(rewardEligible = rewardEligible)

    fun start(state: CrystalRushState): CrystalRushState = state.copy(
        phase = CrystalRushPhase.PLAYING,
        elapsedSeconds = 0f,
        items = emptyList(),
        score = 0,
        hearts = 3,
        multiplier = 1,
        combo = 0,
        shieldSeconds = 0f,
        stunSeconds = 0f,
        nextSpawnAt = 0.25f,
        spawnSerial = 0,
        rewardCommitted = false,
        lastCatchKind = null,
        feedbackToken = 0L,
        isPaused = false,
    )

    fun moveMagnet(state: CrystalRushState, normalizedX: Float): CrystalRushState {
        if (state.phase != CrystalRushPhase.PLAYING || state.isPaused || state.stunSeconds > 0f) return state
        return state.copy(magnetX = normalizedX.coerceIn(0.11f, 0.89f))
    }

    fun togglePause(state: CrystalRushState): CrystalRushState {
        if (state.phase != CrystalRushPhase.PLAYING) return state
        return state.copy(isPaused = !state.isPaused)
    }

    fun advance(state: CrystalRushState, deltaSeconds: Float): CrystalRushState {
        if (state.phase != CrystalRushPhase.PLAYING || state.isPaused) return state
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        val elapsed = (state.elapsedSeconds + delta).coerceAtMost(state.durationSeconds)
        var items = state.items.map { it.copy(y = it.y + it.speed * delta) }
        var score = state.score
        var hearts = state.hearts
        var combo = state.combo
        var multiplier = state.multiplier
        var shield = (state.shieldSeconds - delta).coerceAtLeast(0f)
        val stun = (state.stunSeconds - delta).coerceAtLeast(0f)
        var lastCatch: RushItemKind? = null
        var hazardHit = false
        var token = state.feedbackToken
        val caught = mutableSetOf<Int>()

        items.sortedByDescending { it.y }.forEach { item ->
            if (item.y !in 0.82f..1.02f || abs(item.x - state.magnetX) > collisionRadius(item.kind)) return@forEach
            caught += item.id
            lastCatch = item.kind
            token++
            when (item.kind) {
                RushItemKind.CRYSTAL -> {
                    combo++
                    score += 15 * multiplier
                }
                RushItemKind.COIN -> {
                    combo++
                    score += 8 * multiplier
                }
                RushItemKind.GEM_RED,
                RushItemKind.GEM_BLUE,
                RushItemKind.GEM_PURPLE,
                RushItemKind.GEM_GREEN -> {
                    combo++
                    multiplier = (multiplier + 1).coerceAtMost(4)
                    score += 20 * multiplier
                }
                RushItemKind.CRACKED -> {
                    if (shield > 0f) {
                        score += 10
                        shield = (shield - 1.2f).coerceAtLeast(0f)
                    } else {
                        hearts = (hearts - 1).coerceAtLeast(0)
                        combo = 0
                        multiplier = 1
                        hazardHit = true
                    }
                }
                RushItemKind.LIGHTNING -> {
                    score += 30 * multiplier
                    shield = 3.5f
                    caught.addAll(items.filter { it.kind == RushItemKind.CRACKED }.map { it.id })
                }
            }
        }
        items = items.filter { it.id !in caught && it.y < 1.08f }

        var serial = state.spawnSerial
        var nextSpawn = state.nextSpawnAt
        while (elapsed >= nextSpawn && hearts > 0) {
            val spawned = spawn(serial, nextSpawn)
            val overshoot = (elapsed - nextSpawn).coerceAtLeast(0f)
            items = items + spawned.copy(y = spawned.y + spawned.speed * overshoot)
            serial++
            val pace = (0.62f - nextSpawn / state.durationSeconds * 0.29f).coerceAtLeast(0.30f)
            nextSpawn += pace
        }
        val finished = elapsed >= state.durationSeconds || hearts <= 0
        return state.copy(
            phase = if (finished) CrystalRushPhase.FINISHED else CrystalRushPhase.PLAYING,
            elapsedSeconds = elapsed,
            items = items,
            score = score,
            hearts = hearts,
            combo = combo,
            multiplier = multiplier,
            shieldSeconds = shield,
            stunSeconds = if (hazardHit) 0.55f else stun,
            nextSpawnAt = nextSpawn,
            spawnSerial = serial,
            lastCatchKind = lastCatch,
            feedbackToken = token,
        )
    }

    private fun spawn(serial: Int, elapsed: Float): RushItem {
        val raw = ((serial * 73 + 19) % 79) / 100f
        val x = 0.105f + raw.coerceIn(0f, 0.79f)
        val kind = when {
            serial > 0 && serial % 13 == 0 -> RushItemKind.LIGHTNING
            serial % 7 == 5 -> RushItemKind.CRACKED
            else -> when (serial % 6) {
                0 -> RushItemKind.CRYSTAL
                1 -> RushItemKind.COIN
                2 -> RushItemKind.GEM_RED
                3 -> RushItemKind.GEM_BLUE
                4 -> RushItemKind.GEM_PURPLE
                else -> RushItemKind.GEM_GREEN
            }
        }
        val speed = 0.25f + elapsed / 30f * 0.12f + (serial % 3) * 0.018f
        return RushItem(serial, kind, x, -0.10f, speed, ((serial * 47) % 80 - 40).toFloat())
    }

    private fun collisionRadius(kind: RushItemKind): Float = when (kind) {
        RushItemKind.CRACKED -> 0.145f
        RushItemKind.LIGHTNING -> 0.13f
        RushItemKind.GEM_RED,
        RushItemKind.GEM_BLUE,
        RushItemKind.GEM_PURPLE,
        RushItemKind.GEM_GREEN -> 0.13f
        else -> 0.125f
    }
}

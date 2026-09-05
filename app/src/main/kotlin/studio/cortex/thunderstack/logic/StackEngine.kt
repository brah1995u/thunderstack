package studio.cortex.thunderstack.logic

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import studio.cortex.thunderstack.model.BlockKind
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.LevelDefinition
import studio.cortex.thunderstack.model.MovingBlock
import studio.cortex.thunderstack.model.PlacedBlock
import studio.cortex.thunderstack.model.PlacementGrade
import studio.cortex.thunderstack.model.RunPhase
import studio.cortex.thunderstack.model.StackGameState
import studio.cortex.thunderstack.model.TempleCourse
import studio.cortex.thunderstack.model.WorldRule

/** Renderer-independent deterministic tower rules. Coordinates are normalized to the 0..1 arena. */
class StackEngine {
    fun create(level: LevelDefinition, endless: Boolean = false): StackGameState {
        val baseWidth = baseWidth(level)
        val base = PlacedBlock(0, 0.5f, baseWidth, PlacementGrade.STABLE, BlockKind.STONE, TempleCourse.MARBLE_COURSE)
        val firstKind = nextKind(level, 1)
        val firstCourse = courseFor(level, 1, endless)
        val firstWidth = nominalWidth(level, firstKind, firstCourse)
        return StackGameState(
            level = level,
            endless = endless,
            blocks = listOf(base),
            mover = MovingBlock(-firstWidth / 2f + EDGE_PEEK, firstWidth, 1f, firstKind, firstCourse),
        )
    }

    fun advance(state: StackGameState, deltaSeconds: Float): StackGameState {
        if (state.isPaused || state.isFinished) return state
        val delta = deltaSeconds.coerceIn(0f, 0.05f)
        if (state.phase == RunPhase.COLLAPSING) {
            val collapseElapsed = state.collapseElapsed + delta
            val finished = collapseElapsed >= COLLAPSE_DURATION
            return state.copy(
                elapsedSeconds = state.elapsedSeconds + delta,
                collapseElapsed = collapseElapsed.coerceAtMost(COLLAPSE_DURATION),
                phase = if (finished) RunPhase.LOST else RunPhase.COLLAPSING,
                isFinished = finished,
                impactStrength = (state.impactStrength - delta * 1.8f).coerceAtLeast(0f),
            )
        }
        if (state.phase != RunPhase.PLAYING) return state

        val slowFactor = if (state.slowDrops > 0) 0.52f else 1f
        val heightPace = 1f + (state.height / 8) * 0.055f
        val stormPace = if (state.mover.kind == BlockKind.STORM) 1.18f else 1f
        val speed = state.level.speed * 1.65f * slowFactor * heightPace * stormPace
        val windAmplitude = when (state.level.rule) {
            WorldRule.AEGEAN_WIND -> 0.014f
            WorldRule.OLYMPUS_MASTERY -> 0.018f
            else -> 0f
        }
        val wind = sin((state.elapsedSeconds + delta) * (0.75f + state.level.world * 0.08f)) * windAmplitude
        val half = state.mover.width / 2f
        // Full blocks enter from beyond the arena rails. This preserves their visual size while
        // still allowing a genuine miss instead of constraining a wide block over the base.
        val minimumCenter = -half + EDGE_PEEK
        val maximumCenter = 1f + half - EDGE_PEEK
        var direction = state.mover.direction
        var center = state.mover.center + direction * speed * delta + wind * delta
        if (center > maximumCenter) {
            center = maximumCenter - (center - maximumCenter)
            direction = -1f
        } else if (center < minimumCenter) {
            center = minimumCenter + (minimumCenter - center)
            direction = 1f
        }
        return state.copy(
            elapsedSeconds = state.elapsedSeconds + delta,
            mover = state.mover.copy(center = center.coerceIn(minimumCenter, maximumCenter), direction = direction),
            impactStrength = (state.impactStrength - delta * 3.2f).coerceAtLeast(0f),
        )
    }

    fun activate(state: StackGameState, booster: BoosterType): StackGameState {
        if (state.phase != RunPhase.PLAYING) return state
        return when (booster) {
            BoosterType.THUNDER -> state.copy(mover = state.mover.copy(center = state.top.center))
            BoosterType.SHIELD -> state.copy(shieldCharges = 1, feedback = "AEGIS ARMED", feedbackToken = state.feedbackToken + 1)
            BoosterType.SLOW_TIME -> state.copy(slowDrops = max(state.slowDrops, 3), feedback = "TIME BENDS", feedbackToken = state.feedbackToken + 1)
            BoosterType.CRYSTAL_MAGNET -> state.copy(magnetDrops = max(state.magnetDrops, 4), feedback = "AMBROSIA CALLS", feedbackToken = state.feedbackToken + 1)
        }
    }

    fun drop(state: StackGameState): StackGameState {
        if (state.isPaused || state.isFinished || state.phase != RunPhase.PLAYING) return state
        val top = state.top
        val mover = state.mover
        val moverLeft = mover.center - mover.width / 2f
        val moverRight = mover.center + mover.width / 2f
        val topLeft = top.center - top.width / 2f
        val topRight = top.center + top.width / 2f
        val overlap = (min(moverRight, topRight) - max(moverLeft, topLeft)).coerceAtLeast(0f)

        if (overlap <= 0.006f) {
            return if (state.shieldCharges > 0) shieldSave(state) else beginCollapse(state, mover, "THE TEMPLE FELL")
        }

        val supportRatio = (overlap / min(mover.width, top.width).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        val offset = mover.center - top.center
        val normalizedOffset = offset / ((mover.width + top.width) / 2f).coerceAtLeast(0.001f)
        val error = abs(offset)
        val grade = when {
            error <= state.level.perfectTolerance && supportRatio >= 0.96f -> PlacementGrade.PERFECT
            supportRatio >= 0.62f -> PlacementGrade.STABLE
            else -> PlacementGrade.CROOKED
        }
        val perfect = grade == PlacementGrade.PERFECT
        val combo = if (perfect) state.combo + 1 else 0
        val materialPressure = when (mover.kind) {
            BlockKind.CRACKED -> 5f
            BlockKind.STORM -> 2f
            else -> 0f
        }
        val supportPenalty = (1f - supportRatio) * if (grade == PlacementGrade.CROOKED) 42f else 20f
        val baseStabilityDelta = when (grade) {
            PlacementGrade.PERFECT -> -10f
            PlacementGrade.STABLE -> 3f
            PlacementGrade.CROOKED -> 13f
            else -> 0f
        } + supportPenalty + materialPressure
        val rulePressure = when (state.level.rule) {
            WorldRule.PURE_TIMING -> 0.90f
            WorldRule.AEGEAN_WIND -> 1f
            WorldRule.GOLDEN_AGE -> 0.96f
            WorldRule.NARROW_FOUNDATIONS -> 1.05f
            WorldRule.STORM_RUINS -> 1.12f
            WorldRule.OLYMPUS_MASTERY -> 1.18f
        }
        val stabilityDelta = baseStabilityDelta * rulePressure
        val stability = (state.stability + stabilityDelta).coerceIn(0f, 100f)
        val rawMoment = state.balanceMoment * 0.62f + normalizedOffset * (0.82f + state.height * 0.025f)
        val balanceMoment = (if (perfect) rawMoment * 0.42f else rawMoment).coerceIn(-1.4f, 1.4f)
        val center = if (perfect) top.center else mover.center
        val tilt = if (perfect) 0f else (normalizedOffset * 7.5f).coerceIn(-8f, 8f)
        val placed = PlacedBlock(
            index = state.blocks.size,
            center = center,
            width = mover.width,
            grade = grade,
            kind = mover.kind,
            course = mover.course,
            tilt = tilt,
            supportRatio = supportRatio,
            overhang = offset,
            lean = balanceMoment,
        )
        val blocks = state.blocks + placed
        val height = blocks.size - 1
        val scoreAward = scoreAward(state, grade, combo, perfect)
        val coinsAward = (if (perfect) 3 else 1) + if (mover.kind == BlockKind.GOLD) 12 else 0
        val crystalsAward = if (perfect && state.magnetDrops > 0) 1 else 0
        val shouldCollapse = stability >= 100f || abs(balanceMoment) >= 0.92f || supportRatio < 0.12f

        if (shouldCollapse) {
            if (state.shieldCharges > 0) return shieldSave(state)
            return beginCollapse(
                state.copy(
                    blocks = blocks,
                    score = state.score + scoreAward,
                    runCoins = state.runCoins + coinsAward,
                    runCrystals = state.runCrystals + crystalsAward,
                    stability = stability,
                    balanceMoment = balanceMoment,
                    perfects = state.perfects + if (perfect) 1 else 0,
                    slowDrops = (state.slowDrops - 1).coerceAtLeast(0),
                    magnetDrops = (state.magnetDrops - 1).coerceAtLeast(0),
                ),
                null,
                "THE TEMPLE SHOOK APART",
            )
        }

        val won = !state.endless && height >= state.level.targetHeight
        val nextKind = nextKind(state.level, height + 1)
        val nextCourse = courseFor(state.level, height + 1, state.endless)
        val nextWidth = nominalWidth(state.level, nextKind, nextCourse)
        val direction = if ((height + state.level.number) % 2 == 0) 1f else -1f
        val nextCenter = if (direction > 0f) -nextWidth / 2f + EDGE_PEEK else 1f + nextWidth / 2f - EDGE_PEEK
        return state.copy(
            blocks = blocks,
            mover = MovingBlock(nextCenter, nextWidth, direction, nextKind, nextCourse),
            score = state.score + scoreAward,
            runCoins = state.runCoins + coinsAward,
            runCrystals = state.runCrystals + crystalsAward,
            stability = stability,
            balanceMoment = balanceMoment,
            combo = combo,
            perfects = state.perfects + if (perfect) 1 else 0,
            slowDrops = (state.slowDrops - 1).coerceAtLeast(0),
            magnetDrops = (state.magnetDrops - 1).coerceAtLeast(0),
            isFinished = won,
            won = won,
            phase = if (won) RunPhase.WON else RunPhase.PLAYING,
            lastGrade = grade,
            feedback = if (won) "OLYMPUS RISES" else gradeLabel(grade, combo),
            feedbackToken = state.feedbackToken + 1,
            missedMover = null,
            impactStrength = when (grade) {
                PlacementGrade.PERFECT -> 0.42f
                PlacementGrade.STABLE -> 0.72f
                else -> 1f
            },
        )
    }

    fun stars(state: StackGameState): Int {
        if (!state.won || state.endless) return 0
        var stars = 1
        if (state.stability <= state.level.twoStarStability) stars++
        if (stars == 2 && state.perfects >= state.level.threeStarPerfects) stars++
        return stars
    }

    private fun shieldSave(state: StackGameState): StackGameState {
        val top = state.top
        val kind = state.mover.kind
        val width = state.mover.width
        val block = PlacedBlock(
            state.blocks.size,
            top.center,
            width,
            PlacementGrade.SHIELDED,
            kind,
            state.mover.course,
            supportRatio = 1f,
        )
        val blocks = state.blocks + block
        val height = blocks.size - 1
        val won = !state.endless && height >= state.level.targetHeight
        val nextKind = nextKind(state.level, height + 1)
        val nextCourse = courseFor(state.level, height + 1, state.endless)
        val nextWidth = nominalWidth(state.level, nextKind, nextCourse)
        return state.copy(
            blocks = blocks,
            mover = MovingBlock(-nextWidth / 2f + EDGE_PEEK, nextWidth, 1f, nextKind, nextCourse),
            stability = (state.stability * 0.72f).coerceAtMost(92f),
            balanceMoment = state.balanceMoment * 0.35f,
            combo = 0,
            shieldCharges = 0,
            slowDrops = (state.slowDrops - 1).coerceAtLeast(0),
            magnetDrops = (state.magnetDrops - 1).coerceAtLeast(0),
            score = state.score + 25,
            isFinished = won,
            won = won,
            phase = if (won) RunPhase.WON else RunPhase.PLAYING,
            lastGrade = PlacementGrade.SHIELDED,
            feedback = if (won) "OLYMPUS RISES" else "AEGIS SAVED THE TEMPLE",
            feedbackToken = state.feedbackToken + 1,
            missedMover = null,
            impactStrength = 0.65f,
        )
    }

    private fun beginCollapse(state: StackGameState, missed: MovingBlock?, message: String): StackGameState {
        val direction = when {
            abs(state.balanceMoment) > 0.08f -> if (state.balanceMoment >= 0f) 1f else -1f
            missed != null -> if (missed.center >= state.top.center) 1f else -1f
            else -> 1f
        }
        return state.copy(
            phase = RunPhase.COLLAPSING,
            collapseElapsed = 0f,
            collapseDirection = direction,
            missedMover = missed,
            isFinished = false,
            won = false,
            combo = 0,
            stability = 100f,
            lastGrade = PlacementGrade.COLLAPSE,
            feedback = message,
            feedbackToken = state.feedbackToken + 1,
            impactStrength = 1f,
        )
    }

    private fun scoreAward(state: StackGameState, grade: PlacementGrade, combo: Int, perfect: Boolean): Int {
        val keystone = !state.endless && state.height + 1 == state.level.targetHeight && state.level.number % 10 == 0
        return when (grade) {
            PlacementGrade.PERFECT -> 100 + combo * 25
            PlacementGrade.STABLE -> 45
            PlacementGrade.CROOKED -> 20
            else -> 0
        } +
            (if (state.mover.kind == BlockKind.GOLD) 80 else 0) +
            (if (state.mover.kind == BlockKind.STORM && perfect) 60 else 0) +
            (if (keystone && perfect) 500 else 0)
    }

    private fun baseWidth(level: LevelDefinition): Float = when (level.rule) {
        WorldRule.NARROW_FOUNDATIONS -> 0.70f
        WorldRule.STORM_RUINS -> 0.71f
        WorldRule.OLYMPUS_MASTERY -> 0.69f
        else -> 0.75f
    }

    private fun nominalWidth(level: LevelDefinition, kind: BlockKind, course: TempleCourse): Float {
        val materialMultiplier = when (kind) {
            BlockKind.STONE -> 1f
            BlockKind.MOSS -> 0.98f
            BlockKind.GOLD -> 1.03f
            BlockKind.STORM -> 0.94f
            BlockKind.CRACKED -> 0.92f
        }
        val courseMultiplier = when (course) {
            TempleCourse.STEP -> 1.05f
            TempleCourse.MARBLE_COURSE -> 1f
            TempleCourse.COLUMN_COURSE -> .93f
            TempleCourse.ENTABLATURE -> 1.02f
            TempleCourse.PEDIMENT -> .88f
        }
        return (baseWidth(level) * materialMultiplier * courseMultiplier).coerceIn(0.54f, 0.82f)
    }

    /** Deterministic architecture sequence; the final campaign drop always completes the roof. */
    fun courseFor(level: LevelDefinition, index: Int, endless: Boolean = false): TempleCourse {
        if (!endless && index >= level.targetHeight) return TempleCourse.PEDIMENT
        val cycle = listOf(
            TempleCourse.STEP,
            TempleCourse.MARBLE_COURSE,
            TempleCourse.COLUMN_COURSE,
            TempleCourse.MARBLE_COURSE,
            TempleCourse.ENTABLATURE,
        )
        return cycle[(index - 1).coerceAtLeast(0).mod(cycle.size)]
    }

    private fun nextKind(level: LevelDefinition, index: Int): BlockKind {
        val cycle = when (level.rule) {
            WorldRule.PURE_TIMING -> listOf(BlockKind.STONE, BlockKind.MOSS, BlockKind.GOLD, BlockKind.STONE, BlockKind.CRACKED, BlockKind.MOSS)
            WorldRule.AEGEAN_WIND -> listOf(BlockKind.STONE, BlockKind.MOSS, BlockKind.GOLD, BlockKind.STORM, BlockKind.CRACKED, BlockKind.MOSS)
            WorldRule.GOLDEN_AGE -> listOf(BlockKind.GOLD, BlockKind.STONE, BlockKind.GOLD, BlockKind.MOSS, BlockKind.CRACKED, BlockKind.GOLD)
            WorldRule.NARROW_FOUNDATIONS -> listOf(BlockKind.STONE, BlockKind.GOLD, BlockKind.MOSS, BlockKind.STORM, BlockKind.CRACKED, BlockKind.STONE)
            WorldRule.STORM_RUINS -> listOf(BlockKind.STORM, BlockKind.CRACKED, BlockKind.STONE, BlockKind.STORM, BlockKind.MOSS, BlockKind.CRACKED)
            WorldRule.OLYMPUS_MASTERY -> listOf(BlockKind.STONE, BlockKind.GOLD, BlockKind.MOSS, BlockKind.STORM, BlockKind.CRACKED, BlockKind.GOLD)
        }
        return cycle[(index - 1).mod(cycle.size)]
    }

    private fun gradeLabel(grade: PlacementGrade, combo: Int): String = when (grade) {
        PlacementGrade.PERFECT -> if (combo >= 2) "PERFECT ×$combo" else "PERFECT"
        PlacementGrade.STABLE -> "STABLE"
        PlacementGrade.CROOKED -> "CROOKED"
        PlacementGrade.SHIELDED -> "AEGIS"
        PlacementGrade.COLLAPSE -> "COLLAPSE"
    }

    companion object {
        const val COLLAPSE_DURATION = 1.35f
        private const val EDGE_PEEK = 0.08f
    }
}

package studio.cortex.thunderstack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.cortex.thunderstack.logic.StackEngine
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.PlacementGrade
import studio.cortex.thunderstack.model.RunPhase
import studio.cortex.thunderstack.model.TempleCourse
import studio.cortex.thunderstack.model.WorldRule
import studio.cortex.thunderstack.model.campaignLevels

class StackEngineTest {
    private val engine = StackEngine()
    private val level = campaignLevels.first()

    @Test fun campaignHeightCurveRunsSmoothlyFromThreeToTwentyTwo() {
        assertEquals(3, campaignLevels.first().targetHeight)
        assertEquals(22, campaignLevels.last().targetHeight)
        assertTrue(campaignLevels.zipWithNext().all { (a, b) -> b.targetHeight >= a.targetHeight })
    }

    @Test fun campaignWorldsExposeTheirActualRules() {
        val rules = campaignLevels.chunked(10).map { it.first().rule }
        assertEquals(WorldRule.entries.toList(), rules)
        assertTrue(campaignLevels.all { it.modifier == it.rule.label })
    }

    @Test fun aegeanWindChangesMovementWhilePureTimingDoesNot() {
        val pureLevel = campaignLevels.first()
        val windLevel = pureLevel.copy(world = 2, rule = WorldRule.AEGEAN_WIND, modifier = WorldRule.AEGEAN_WIND.label)
        val pure = engine.advance(engine.create(pureLevel), .05f)
        val windy = engine.advance(engine.create(windLevel), .05f)
        assertTrue(kotlin.math.abs(windy.mover.center - pure.mover.center) > 0.000001f)
    }

    @Test fun narrowFoundationRuleReducesAllCourseWidths() {
        val normalLevel = campaignLevels.first()
        val narrowLevel = normalLevel.copy(world = 4, rule = WorldRule.NARROW_FOUNDATIONS, modifier = WorldRule.NARROW_FOUNDATIONS.label)
        assertTrue(engine.create(narrowLevel).mover.width < engine.create(normalLevel).mover.width)
    }

    @Test fun stormRuinsFavorDangerousMaterials() {
        var state = engine.create(campaignLevels[40])
        val kinds = mutableListOf(state.mover.kind)
        repeat(5) {
            state = engine.drop(state.copy(mover = state.mover.copy(center = state.top.center)))
            kinds += state.mover.kind
        }
        assertTrue(kinds.count { it == studio.cortex.thunderstack.model.BlockKind.STORM || it == studio.cortex.thunderstack.model.BlockKind.CRACKED } >= 4)
    }

    @Test fun finalCampaignPieceCompletesThePediment() {
        var state = engine.create(level)
        repeat(level.targetHeight) {
            state = engine.drop(state.copy(mover = state.mover.copy(center = state.top.center)))
        }
        assertEquals(TempleCourse.PEDIMENT, state.top.course)
    }

    @Test fun earlyTempleUsesDistinctCoursesAndWidths() {
        val step = engine.create(level)
        assertEquals(TempleCourse.STEP, step.mover.course)
        val marble = engine.drop(step.copy(mover = step.mover.copy(center = step.top.center)))
        assertEquals(TempleCourse.MARBLE_COURSE, marble.mover.course)
        val roof = engine.drop(marble.copy(mover = marble.mover.copy(center = marble.top.center)))
        assertEquals(TempleCourse.PEDIMENT, roof.mover.course)
        assertTrue(step.mover.width != marble.mover.width)
        assertTrue(roof.mover.width < marble.mover.width)
    }

    @Test fun centeredDropIsPerfectAndRetainsWidth() {
        val start = engine.create(level)
        val dropped = engine.drop(start.copy(mover = start.mover.copy(center = start.top.center)))
        assertEquals(PlacementGrade.PERFECT, dropped.lastGrade)
        assertEquals(start.mover.width, dropped.top.width)
        assertEquals(1, dropped.height)
        assertTrue(dropped.score > 100)
    }

    @Test fun partialDropKeepsFullBlockAndRecordsSupport() {
        val start = engine.create(level)
        val dropped = engine.drop(start.copy(mover = start.mover.copy(center = 0.70f)))
        assertEquals(start.mover.width, dropped.top.width)
        assertTrue(dropped.top.supportRatio < 1f)
        assertTrue(dropped.top.overhang > 0f)
        assertTrue(dropped.top.lean > 0f)
        assertFalse(dropped.isFinished)
    }

    @Test fun oppositeOverhangCounterbalancesTower() {
        val start = engine.create(level)
        val right = engine.drop(start.copy(mover = start.mover.copy(center = .63f)))
        val corrected = engine.drop(right.copy(mover = right.mover.copy(center = .50f)))
        assertTrue(kotlin.math.abs(corrected.balanceMoment) < kotlin.math.abs(right.balanceMoment))
    }

    @Test fun completeMissAnimatesBeforeResult() {
        val start = engine.create(level)
        val missed = engine.drop(start.copy(mover = start.mover.copy(center = 0.99f, width = 0.10f)))
        assertEquals(RunPhase.COLLAPSING, missed.phase)
        assertFalse(missed.isFinished)
        val falling = engine.advance(missed, .3f)
        assertFalse(falling.isFinished)
        var finished = falling
        repeat(30) { finished = engine.advance(finished, .05f) }
        assertTrue(finished.isFinished)
        assertEquals(RunPhase.LOST, finished.phase)
        assertFalse(missed.won)
        assertEquals(missed, engine.drop(missed))
    }

    @Test fun shieldPreventsOneFatalMiss() {
        val start = engine.activate(engine.create(level), BoosterType.SHIELD)
        val saved = engine.drop(start.copy(mover = start.mover.copy(center = 0.99f, width = 0.10f)))
        assertFalse(saved.isFinished)
        assertEquals(PlacementGrade.SHIELDED, saved.lastGrade)
        assertEquals(0, saved.shieldCharges)
    }

    @Test fun thunderCentersCurrentMover() {
        val start = engine.create(level)
        val aligned = engine.activate(start, BoosterType.THUNDER)
        assertEquals(start.top.center, aligned.mover.center)
        assertEquals(PlacementGrade.PERFECT, engine.drop(aligned).lastGrade)
    }

    @Test fun perfectTowerCompletesCampaignTrial() {
        var state = engine.create(level)
        repeat(level.targetHeight) {
            state = engine.drop(state.copy(mover = state.mover.copy(center = state.top.center)))
        }
        assertTrue(state.isFinished)
        assertTrue(state.won)
        assertEquals(level.targetHeight, state.height)
        assertEquals(3, engine.stars(state))
    }

    @Test fun firstRealmUsesDistinctMaterials() {
        var state = engine.create(campaignLevels[9])
        val kinds = mutableSetOf(state.mover.kind)
        repeat(5) {
            state = engine.drop(state.copy(mover = state.mover.copy(center = state.top.center)))
            kinds += state.mover.kind
        }
        assertTrue(kinds.size >= 4)
    }
}

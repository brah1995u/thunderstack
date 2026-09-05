package studio.cortex.thunderstack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.cortex.thunderstack.logic.CrystalRushEngine
import studio.cortex.thunderstack.model.RushItem
import studio.cortex.thunderstack.model.RushItemKind

class CrystalRushEngineTest {
    private val engine = CrystalRushEngine()

    @Test fun dragIsClampedToArena() {
        val state = engine.start(engine.create(rewardEligible = true))
        assertEquals(.11f, engine.moveMagnet(state, -4f).magnetX)
        assertEquals(.89f, engine.moveMagnet(state, 4f).magnetX)
    }

    @Test fun collectibleCollidesOnlyOnce() {
        val state = engine.start(engine.create(rewardEligible = true)).copy(
            magnetX = .5f,
            items = listOf(RushItem(99, RushItemKind.CRYSTAL, .5f, .9f, 0f, 0f)),
            nextSpawnAt = 20f,
        )
        val caught = engine.advance(state, .016f)
        val advanced = engine.advance(caught, .016f)
        assertEquals(15, caught.score)
        assertEquals(caught.score, advanced.score)
        assertTrue(advanced.items.none { it.id == 99 })
    }

    @Test fun spawnScheduleDoesNotDependOnFrameRate() {
        fun simulate(step: Float): studio.cortex.thunderstack.model.CrystalRushState {
            var state = engine.start(engine.create(false)).copy(magnetX = 1.2f)
            var elapsed = 0f
            while (elapsed < 5f - .0001f) {
                val delta = minOf(step, 5f - elapsed)
                state = engine.advance(state, delta)
                elapsed += delta
            }
            return state
        }
        val at60 = simulate(1f / 60f)
        val at20 = simulate(1f / 20f)
        assertEquals(at60.spawnSerial, at20.spawnSerial)
        assertEquals(at60.items.map { it.kind }, at20.items.map { it.kind })
        at60.items.zip(at20.items).forEach { (a, b) -> assertTrue(kotlin.math.abs(a.y - b.y) < .01f) }
    }
}

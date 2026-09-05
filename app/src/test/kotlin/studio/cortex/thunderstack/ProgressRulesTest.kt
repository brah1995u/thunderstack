package studio.cortex.thunderstack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.cortex.thunderstack.logic.ProgressRules
import studio.cortex.thunderstack.model.AchievementMetric
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.PlayerProgress
import studio.cortex.thunderstack.model.achievementDefinitions

class ProgressRulesTest {
    @Test fun purchaseCannotMakeWalletNegative() {
        val poor = PlayerProgress(coins = 0, crystals = 0)
        assertNull(ProgressRules.buyBooster(poor, BoosterType.THUNDER))
        assertEquals(0, poor.coins)
    }

    @Test fun dailyRewardIsClaimableOncePerDay() {
        val first = ProgressRules.claimDaily(PlayerProgress(), 100L)!!
        assertNull(ProgressRules.claimDaily(first.progress, 100L))
        assertTrue(first.progress.coins > PlayerProgress().coins)
    }

    @Test fun achievementClaimIsIdempotent() {
        val definition = achievementDefinitions.first()
        val ready = PlayerProgress(achievementProgress = mapOf(AchievementMetric.LEVELS_WON to 1))
        val claimed = ProgressRules.claimAchievement(ready, definition)!!
        assertNull(ProgressRules.claimAchievement(claimed.progress, definition))
    }

    @Test fun crystalRushDailyRewardIsIdempotent() {
        val initial = PlayerProgress(coins = 0, crystals = 0)
        val first = ProgressRules.applyCrystalRushResult(initial, 360, 500L)
        val practice = ProgressRules.applyCrystalRushResult(first.progress, 900, 500L)
        assertEquals(3, first.crystals)
        assertEquals(0, practice.coins)
        assertEquals(0, practice.crystals)
        assertEquals(900, practice.progress.bestCrystalRushScore)
        assertEquals(first.progress.coins, practice.progress.coins)
    }
}

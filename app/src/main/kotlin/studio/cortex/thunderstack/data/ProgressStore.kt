package studio.cortex.thunderstack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import studio.cortex.thunderstack.logic.ProgressRules
import studio.cortex.thunderstack.model.AchievementMetric
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.PlayerProgress

private val Context.thunderStackStore by preferencesDataStore("thunder_stack_progress_v1")

class ProgressStore(private val context: Context) {
    private object Keys {
        val level = intPreferencesKey("highest_unlocked")
        val coins = intPreferencesKey("coins")
        val crystals = intPreferencesKey("crystals")
        val stars = stringSetPreferencesKey("stars")
        val scores = stringSetPreferencesKey("scores")
        val boosters = stringSetPreferencesKey("boosters")
        val metrics = stringSetPreferencesKey("metrics")
        val claimed = stringSetPreferencesKey("claimed_achievements")
        val sound = booleanPreferencesKey("sound")
        val music = booleanPreferencesKey("music")
        val haptics = booleanPreferencesKey("haptics")
        val reducedFlashes = booleanPreferencesKey("reduced_flashes")
        val highContrast = booleanPreferencesKey("high_contrast")
        val dailyDay = longPreferencesKey("daily_day")
        val dailyStreak = intPreferencesKey("daily_streak")
        val endlessHeight = intPreferencesKey("endless_height")
        val endlessScore = intPreferencesKey("endless_score")
        val rushScore = intPreferencesKey("crystal_rush_best_score")
        val rushRewardDay = longPreferencesKey("crystal_rush_reward_day")
    }

    suspend fun load(): PlayerProgress {
        val p = context.thunderStackStore.data.first()
        val stars = parseMap(p[Keys.stars].orEmpty()).mapNotNull { (key, value) -> key.toIntOrNull()?.let { it to value } }.toMap()
        val scores = parseMap(p[Keys.scores].orEmpty()).mapNotNull { (key, value) -> key.toIntOrNull()?.let { it to value } }.toMap()
        val boosters = parseMap(p[Keys.boosters].orEmpty()).mapNotNull { (key, value) ->
            runCatching { BoosterType.valueOf(key) to value }.getOrNull()
        }.toMap()
        val metrics = parseMap(p[Keys.metrics].orEmpty()).mapNotNull { (key, value) ->
            runCatching { AchievementMetric.valueOf(key) to value }.getOrNull()
        }.toMap()
        return ProgressRules.syncDerivedMetrics(PlayerProgress(
            highestUnlocked = p[Keys.level] ?: 1,
            coins = p[Keys.coins] ?: 420,
            crystals = p[Keys.crystals] ?: 6,
            stars = stars,
            bestScores = scores,
            boosters = if (p[Keys.boosters] == null) BoosterType.entries.associateWith { 1 } else boosters,
            achievementProgress = metrics,
            claimedAchievementIds = p[Keys.claimed].orEmpty(),
            soundEnabled = p[Keys.sound] ?: true,
            musicEnabled = p[Keys.music] ?: true,
            hapticsEnabled = p[Keys.haptics] ?: true,
            reducedFlashes = p[Keys.reducedFlashes] ?: false,
            highContrast = p[Keys.highContrast] ?: false,
            lastDailyClaimEpochDay = p[Keys.dailyDay] ?: -1L,
            dailyStreak = p[Keys.dailyStreak] ?: 0,
            bestEndlessHeight = p[Keys.endlessHeight] ?: 0,
            bestEndlessScore = p[Keys.endlessScore] ?: 0,
            bestCrystalRushScore = p[Keys.rushScore] ?: 0,
            lastCrystalRushRewardEpochDay = p[Keys.rushRewardDay] ?: -1L,
        ))
    }

    suspend fun save(value: PlayerProgress) {
        val progress = ProgressRules.sanitize(value)
        context.thunderStackStore.edit { p ->
            p[Keys.level] = progress.highestUnlocked
            p[Keys.coins] = progress.coins
            p[Keys.crystals] = progress.crystals
            p[Keys.stars] = progress.stars.encode()
            p[Keys.scores] = progress.bestScores.encode()
            p[Keys.boosters] = progress.boosters.mapKeys { it.key.name }.encode()
            p[Keys.metrics] = progress.achievementProgress.mapKeys { it.key.name }.encode()
            p[Keys.claimed] = progress.claimedAchievementIds
            p[Keys.sound] = progress.soundEnabled
            p[Keys.music] = progress.musicEnabled
            p[Keys.haptics] = progress.hapticsEnabled
            p[Keys.reducedFlashes] = progress.reducedFlashes
            p[Keys.highContrast] = progress.highContrast
            p[Keys.dailyDay] = progress.lastDailyClaimEpochDay
            p[Keys.dailyStreak] = progress.dailyStreak
            p[Keys.endlessHeight] = progress.bestEndlessHeight
            p[Keys.endlessScore] = progress.bestEndlessScore
            p[Keys.rushScore] = progress.bestCrystalRushScore
            p[Keys.rushRewardDay] = progress.lastCrystalRushRewardEpochDay
        }
    }

    private fun parseMap(values: Set<String>): Map<String, Int> = values.mapNotNull { entry ->
        val split = entry.split(':', limit = 2)
        if (split.size != 2) null else split[1].toIntOrNull()?.let { split[0] to it }
    }.toMap()

    private fun <K> Map<K, Int>.encode(): Set<String> = entries.map { (key, value) -> "$key:$value" }.toSet()
}

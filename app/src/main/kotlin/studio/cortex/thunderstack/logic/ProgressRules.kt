package studio.cortex.thunderstack.logic

import studio.cortex.thunderstack.model.AchievementDefinition
import studio.cortex.thunderstack.model.AchievementMetric
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.PlayerProgress
import studio.cortex.thunderstack.model.StackGameState
import studio.cortex.thunderstack.model.achievementDefinitions
import studio.cortex.thunderstack.model.campaignLevels
import studio.cortex.thunderstack.model.dailyRewards
import studio.cortex.thunderstack.model.totalStars

data class ClaimResult(val progress: PlayerProgress, val message: String)

data class CrystalRushReward(val progress: PlayerProgress, val coins: Int, val crystals: Int, val rewarded: Boolean)

object ProgressRules {
    fun sanitize(value: PlayerProgress): PlayerProgress {
        val safeStars = value.stars.filterKeys { it in 1..campaignLevels.size }.mapValues { it.value.coerceIn(0, 3) }
        val safeScores = value.bestScores.filterKeys { it in 1..campaignLevels.size }.mapValues { it.value.coerceAtLeast(0) }
        val safeBoosters = BoosterType.entries.associateWith { (value.boosters[it] ?: 0).coerceIn(0, 999) }
        val safeMetrics = AchievementMetric.entries.associateWith { (value.achievementProgress[it] ?: 0).coerceAtLeast(0) }
        return value.copy(
            schema = 2,
            highestUnlocked = value.highestUnlocked.coerceIn(1, campaignLevels.size),
            coins = value.coins.coerceIn(0, 99_999_999),
            crystals = value.crystals.coerceIn(0, 99_999),
            stars = safeStars,
            bestScores = safeScores,
            boosters = safeBoosters,
            achievementProgress = safeMetrics,
            dailyStreak = value.dailyStreak.coerceIn(0, 999),
            bestEndlessHeight = value.bestEndlessHeight.coerceAtLeast(0),
            bestEndlessScore = value.bestEndlessScore.coerceAtLeast(0),
            bestCrystalRushScore = value.bestCrystalRushScore.coerceAtLeast(0),
        )
    }

    fun applyGameResult(progress: PlayerProgress, state: StackGameState, stars: Int): PlayerProgress {
        val coinReward = state.runCoins + if (state.won && !state.endless) state.level.rewardCoins else 0
        var next = progress.copy(
            coins = progress.coins + coinReward,
            crystals = progress.crystals + state.runCrystals,
            bestEndlessHeight = if (state.endless) maxOf(progress.bestEndlessHeight, state.height) else progress.bestEndlessHeight,
            bestEndlessScore = if (state.endless) maxOf(progress.bestEndlessScore, state.score) else progress.bestEndlessScore,
        )
        next = record(next, AchievementMetric.COINS_EARNED, coinReward)
        next = record(next, AchievementMetric.CRYSTALS_EARNED, state.runCrystals)
        next = record(next, AchievementMetric.PERFECTS, state.perfects)
        next = recordMax(next, AchievementMetric.BEST_HEIGHT, state.height)
        if (state.won && !state.endless) {
            val oldStars = progress.stars[state.level.number] ?: 0
            val newStars = maxOf(oldStars, stars.coerceIn(1, 3))
            val firstClear = oldStars == 0
            next = next.copy(
                highestUnlocked = maxOf(progress.highestUnlocked, (state.level.number + 1).coerceAtMost(campaignLevels.size)),
                stars = progress.stars + (state.level.number to newStars),
                bestScores = progress.bestScores + (state.level.number to maxOf(progress.bestScores[state.level.number] ?: 0, state.score)),
            )
            if (firstClear) next = record(next, AchievementMetric.LEVELS_WON, 1)
            if (newStars > oldStars) next = record(next, AchievementMetric.STARS, newStars - oldStars)
        }
        return sanitize(next)
    }

    fun buyBooster(progress: PlayerProgress, booster: BoosterType): PlayerProgress? {
        if (booster.crystalPrice > 0) {
            if (progress.crystals < booster.crystalPrice) return null
            return sanitize(progress.copy(
                crystals = progress.crystals - booster.crystalPrice,
                boosters = progress.boosters + (booster to ((progress.boosters[booster] ?: 0) + 1)),
            ))
        }
        if (progress.coins < booster.coinPrice) return null
        return sanitize(progress.copy(
            coins = progress.coins - booster.coinPrice,
            boosters = progress.boosters + (booster to ((progress.boosters[booster] ?: 0) + 1)),
        ))
    }

    fun consumeBooster(progress: PlayerProgress, booster: BoosterType): PlayerProgress? {
        val count = progress.boosters[booster] ?: 0
        if (count <= 0) return null
        return sanitize(record(
            progress.copy(boosters = progress.boosters + (booster to count - 1)),
            AchievementMetric.BOOSTERS_USED,
            1,
        ))
    }

    fun claimAchievement(progress: PlayerProgress, definition: AchievementDefinition): ClaimResult? {
        if (definition.id in progress.claimedAchievementIds) return null
        if ((progress.achievementProgress[definition.metric] ?: 0) < definition.target) return null
        var next = progress.copy(
            coins = progress.coins + definition.rewardCoins,
            crystals = progress.crystals + definition.rewardCrystals,
            claimedAchievementIds = progress.claimedAchievementIds + definition.id,
        )
        definition.rewardBooster?.let { booster ->
            next = next.copy(boosters = next.boosters + (booster to ((next.boosters[booster] ?: 0) + 1)))
        }
        next = record(next, AchievementMetric.COINS_EARNED, definition.rewardCoins)
        next = record(next, AchievementMetric.CRYSTALS_EARNED, definition.rewardCrystals)
        val reward = buildList {
            if (definition.rewardCoins > 0) add("${definition.rewardCoins} coins")
            if (definition.rewardCrystals > 0) add("${definition.rewardCrystals} crystals")
            definition.rewardBooster?.let { add(it.title) }
        }.joinToString(" + ")
        return ClaimResult(sanitize(next), "Claimed $reward")
    }

    fun claimDaily(progress: PlayerProgress, epochDay: Long): ClaimResult? {
        if (progress.lastDailyClaimEpochDay == epochDay) return null
        val streak = if (progress.lastDailyClaimEpochDay == epochDay - 1) progress.dailyStreak + 1 else 1
        val reward = dailyRewards[(streak - 1) % dailyRewards.size]
        var next = progress.copy(
            lastDailyClaimEpochDay = epochDay,
            dailyStreak = streak,
            coins = progress.coins + reward.coins,
            crystals = progress.crystals + reward.crystals,
        )
        reward.booster?.let { booster ->
            next = next.copy(boosters = next.boosters + (booster to ((next.boosters[booster] ?: 0) + 1)))
        }
        next = record(next, AchievementMetric.DAILY_CLAIMS, 1)
        next = record(next, AchievementMetric.COINS_EARNED, reward.coins)
        next = record(next, AchievementMetric.CRYSTALS_EARNED, reward.crystals)
        return ClaimResult(sanitize(next), "Day $streak: ${reward.label}")
    }

    fun applyCrystalRushResult(progress: PlayerProgress, score: Int, epochDay: Long): CrystalRushReward {
        val safeScore = score.coerceAtLeast(0)
        val alreadyRewarded = progress.lastCrystalRushRewardEpochDay == epochDay
        val coins = if (alreadyRewarded) 0 else 40 + (safeScore / 5).coerceAtMost(160)
        val crystals = if (alreadyRewarded) 0 else when {
            safeScore >= 360 -> 3
            safeScore >= 240 -> 2
            safeScore >= 120 -> 1
            else -> 0
        }
        var next = progress.copy(
            bestCrystalRushScore = maxOf(progress.bestCrystalRushScore, safeScore),
            coins = progress.coins + coins,
            crystals = progress.crystals + crystals,
            lastCrystalRushRewardEpochDay = if (alreadyRewarded) progress.lastCrystalRushRewardEpochDay else epochDay,
        )
        next = record(next, AchievementMetric.COINS_EARNED, coins)
        next = record(next, AchievementMetric.CRYSTALS_EARNED, crystals)
        return CrystalRushReward(sanitize(next), coins, crystals, !alreadyRewarded)
    }

    fun record(progress: PlayerProgress, metric: AchievementMetric, amount: Int): PlayerProgress {
        if (amount <= 0) return progress
        val current = progress.achievementProgress[metric] ?: 0
        return progress.copy(achievementProgress = progress.achievementProgress + (metric to (current + amount)))
    }

    fun recordMax(progress: PlayerProgress, metric: AchievementMetric, value: Int): PlayerProgress {
        val current = progress.achievementProgress[metric] ?: 0
        if (value <= current) return progress
        return progress.copy(achievementProgress = progress.achievementProgress + (metric to value))
    }

    fun syncDerivedMetrics(progress: PlayerProgress): PlayerProgress {
        var next = recordMax(progress, AchievementMetric.STARS, progress.totalStars())
        next = recordMax(next, AchievementMetric.LEVELS_WON, progress.stars.count { it.value > 0 })
        return sanitize(next)
    }

    fun claimableCount(progress: PlayerProgress): Int = achievementDefinitions.count { definition ->
        definition.id !in progress.claimedAchievementIds &&
            (progress.achievementProgress[definition.metric] ?: 0) >= definition.target
    }
}

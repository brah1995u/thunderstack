package studio.cortex.thunderstack.model

import kotlin.math.min

enum class AppScreen { HOME, LEVELS, PRELEVEL, SHOP, ACHIEVEMENTS, LEADERBOARD, DAILY, SETTINGS, GAMEPLAY, CRYSTAL_RUSH }

enum class PlacementGrade { PERFECT, STABLE, CROOKED, SHIELDED, COLLAPSE }

enum class RunPhase { PLAYING, COLLAPSING, WON, LOST }

enum class BlockKind { STONE, MOSS, GOLD, STORM, CRACKED }

/** Architectural silhouette. Material remains controlled independently by [BlockKind]. */
enum class TempleCourse { STEP, MARBLE_COURSE, COLUMN_COURSE, ENTABLATURE, PEDIMENT }

enum class WorldRule(val label: String) {
    PURE_TIMING("Pure timing"),
    AEGEAN_WIND("Announced Aegean wind"),
    GOLDEN_AGE("Golden blocks appear"),
    NARROW_FOUNDATIONS("Narrower foundations"),
    STORM_RUINS("Storm and cracked blocks"),
    OLYMPUS_MASTERY("Mastery mix"),
}

enum class BoosterType(
    val title: String,
    val description: String,
    val coinPrice: Int,
    val crystalPrice: Int = 0,
) {
    THUNDER("Thunder Strike", "Centers and drops the current block with divine precision.", 220),
    SHIELD("Aegis Shield", "Prevents one fatal miss and restores a safe support.", 180),
    SLOW_TIME("Slow Time", "Halves block speed for the next three drops.", 145),
    CRYSTAL_MAGNET("Crystal Magnet", "Perfect drops attract bonus crystals for four drops.", 0, 8),
}

enum class AchievementMetric {
    LEVELS_WON,
    PERFECTS,
    STARS,
    COINS_EARNED,
    CRYSTALS_EARNED,
    BOOSTERS_USED,
    BEST_HEIGHT,
    DAILY_CLAIMS,
}

data class LevelDefinition(
    val number: Int,
    val world: Int,
    val worldName: String,
    val title: String,
    val targetHeight: Int,
    val speed: Float,
    val perfectTolerance: Float,
    val twoStarStability: Float,
    val threeStarPerfects: Int,
    val rewardCoins: Int,
    val rule: WorldRule,
    val modifier: String,
)

data class PlacedBlock(
    val index: Int,
    val center: Float,
    val width: Float,
    val grade: PlacementGrade,
    val kind: BlockKind,
    val course: TempleCourse = TempleCourse.MARBLE_COURSE,
    val tilt: Float = 0f,
    val supportRatio: Float = 1f,
    val overhang: Float = 0f,
    val lean: Float = 0f,
)

data class MovingBlock(
    val center: Float,
    val width: Float,
    val direction: Float,
    val kind: BlockKind,
    val course: TempleCourse = TempleCourse.MARBLE_COURSE,
)

data class StackGameState(
    val level: LevelDefinition,
    val endless: Boolean,
    val blocks: List<PlacedBlock>,
    val mover: MovingBlock,
    val elapsedSeconds: Float = 0f,
    val score: Int = 0,
    val runCoins: Int = 0,
    val runCrystals: Int = 0,
    val stability: Float = 0f,
    val combo: Int = 0,
    val perfects: Int = 0,
    val shieldCharges: Int = 0,
    val slowDrops: Int = 0,
    val magnetDrops: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val won: Boolean = false,
    val resultCommitted: Boolean = false,
    val lastGrade: PlacementGrade? = null,
    val feedback: String = "TAP TO PLACE",
    val feedbackToken: Long = 0L,
    val phase: RunPhase = RunPhase.PLAYING,
    val balanceMoment: Float = 0f,
    val collapseElapsed: Float = 0f,
    val collapseDirection: Float = 1f,
    val missedMover: MovingBlock? = null,
    val impactStrength: Float = 0f,
) {
    val height: Int get() = blocks.size - 1
    val top: PlacedBlock get() = blocks.last()
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val target: Int,
    val rewardCoins: Int = 0,
    val rewardCrystals: Int = 0,
    val rewardBooster: BoosterType? = null,
)

data class PlayerProgress(
    val schema: Int = 2,
    val highestUnlocked: Int = 1,
    val coins: Int = 420,
    val crystals: Int = 6,
    val stars: Map<Int, Int> = emptyMap(),
    val bestScores: Map<Int, Int> = emptyMap(),
    val boosters: Map<BoosterType, Int> = BoosterType.entries.associateWith { 1 },
    val achievementProgress: Map<AchievementMetric, Int> = emptyMap(),
    val claimedAchievementIds: Set<String> = emptySet(),
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedFlashes: Boolean = false,
    val highContrast: Boolean = false,
    val lastDailyClaimEpochDay: Long = -1L,
    val dailyStreak: Int = 0,
    val bestEndlessHeight: Int = 0,
    val bestEndlessScore: Int = 0,
    val bestCrystalRushScore: Int = 0,
    val lastCrystalRushRewardEpochDay: Long = -1L,
)

data class DailyReward(
    val label: String,
    val coins: Int = 0,
    val crystals: Int = 0,
    val booster: BoosterType? = null,
)

data class LeaderboardEntry(val name: String, val value: Int, val isPlayer: Boolean = false)

private val worldNames = listOf(
    "Dawn Ruins",
    "Aegean Wind",
    "Forge of Hephaestus",
    "Oracle Heights",
    "Titan Storm",
    "Crown of Olympus",
)

private val levelTitles = listOf(
    "First Foundation", "Marble Rhythm", "Golden Measure", "Cloudline", "Builder's Oath",
    "Temple Echo", "Sunlit Steps", "Divine Balance", "Thunder Lesson", "Dawn Keystone",
)

private val worldRules = listOf(
    WorldRule.PURE_TIMING,
    WorldRule.AEGEAN_WIND,
    WorldRule.GOLDEN_AGE,
    WorldRule.NARROW_FOUNDATIONS,
    WorldRule.STORM_RUINS,
    WorldRule.OLYMPUS_MASTERY,
)

val campaignLevels: List<LevelDefinition> = (1..60).map { number ->
    val world = (number - 1) / 10
    val local = (number - 1) % 10
    // Short early temples and a smooth campaign-long rise: level 1 = 3, level 60 = 22.
    val target = kotlin.math.round(3f + 19f * (number - 1) / 59f).toInt()
    val rule = worldRules[world]
    LevelDefinition(
        number = number,
        world = world + 1,
        worldName = worldNames[world],
        title = if (local == 9) "${worldNames[world]} Keystone" else levelTitles[local],
        targetHeight = target,
        speed = 0.27f + number * 0.0045f,
        perfectTolerance = (0.026f - world * 0.0015f).coerceAtLeast(0.017f),
        twoStarStability = (66f - world * 4f).coerceAtLeast(42f),
        threeStarPerfects = min(target, 2 + world + local / 3),
        rewardCoins = 55 + number * 7,
        rule = rule,
        modifier = rule.label,
    )
}

val achievementDefinitions = listOf(
    AchievementDefinition("first_tower", "First Tower", "Complete one level.", AchievementMetric.LEVELS_WON, 1, rewardCoins = 100),
    AchievementDefinition("olympus_builder", "Olympus Builder", "Complete 10 levels.", AchievementMetric.LEVELS_WON, 10, rewardCoins = 260),
    AchievementDefinition("path_master", "Path Master", "Complete 30 levels.", AchievementMetric.LEVELS_WON, 30, rewardCrystals = 8),
    AchievementDefinition("perfect_hand", "Perfect Hand", "Land 25 Perfect blocks.", AchievementMetric.PERFECTS, 25, rewardCoins = 180),
    AchievementDefinition("thunder_precision", "Thunder Precision", "Land 150 Perfect blocks.", AchievementMetric.PERFECTS, 150, rewardCrystals = 12),
    AchievementDefinition("star_architect", "Star Architect", "Earn 45 campaign stars.", AchievementMetric.STARS, 45, rewardCoins = 320),
    AchievementDefinition("crown_collector", "Crown Collector", "Earn 120 campaign stars.", AchievementMetric.STARS, 120, rewardCrystals = 15),
    AchievementDefinition("divine_height", "Divine Height", "Reach a 25-block tower.", AchievementMetric.BEST_HEIGHT, 25, rewardBooster = BoosterType.SHIELD),
    AchievementDefinition("storm_caller", "Storm Caller", "Use 12 boosters.", AchievementMetric.BOOSTERS_USED, 12, rewardBooster = BoosterType.THUNDER),
    AchievementDefinition("temple_treasury", "Temple Treasury", "Earn 4,000 coins.", AchievementMetric.COINS_EARNED, 4_000, rewardCrystals = 10),
    AchievementDefinition("ambrosia_seeker", "Ambrosia Seeker", "Collect 30 crystals.", AchievementMetric.CRYSTALS_EARNED, 30, rewardCoins = 400),
    AchievementDefinition("daily_oracle", "Daily Oracle", "Claim 7 daily rewards.", AchievementMetric.DAILY_CLAIMS, 7, rewardBooster = BoosterType.CRYSTAL_MAGNET),
)

val dailyRewards = listOf(
    DailyReward("100 COINS", coins = 100),
    DailyReward("THUNDER", booster = BoosterType.THUNDER),
    DailyReward("180 COINS", coins = 180),
    DailyReward("3 CRYSTALS", crystals = 3),
    DailyReward("260 COINS", coins = 260),
    DailyReward("AEGIS", booster = BoosterType.SHIELD),
    DailyReward("AMBROSIA", coins = 350, crystals = 6),
)

fun PlayerProgress.totalStars(): Int = stars.values.sum()

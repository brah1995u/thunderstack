package studio.cortex.thunderstack.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.cortex.thunderstack.R
import studio.cortex.thunderstack.logic.ProgressRules
import studio.cortex.thunderstack.model.AchievementDefinition
import studio.cortex.thunderstack.model.AppScreen
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.LeaderboardEntry
import studio.cortex.thunderstack.model.PlayerProgress
import studio.cortex.thunderstack.model.achievementDefinitions
import studio.cortex.thunderstack.model.campaignLevels
import studio.cortex.thunderstack.model.dailyRewards
import studio.cortex.thunderstack.model.totalStars

@Composable
fun LoadingScreen() {
    SceneBackground(R.drawable.bg_loading_zeus, shade = .08f) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 34.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(1.dp))
            Image(painterResource(R.drawable.logo_thunder_stack), "Thunder Stack", Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FORGING OLYMPUS", color = Marble, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                ApprovedProgress(.82f, Modifier.fillMaxWidth(.72f))
            }
        }
    }
}

@Composable
fun HomeScreen(progress: PlayerProgress, model: ThunderStackViewModel) {
    SceneBackground(R.drawable.bg_home_olympus, shade = .18f) {
        Box(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 7.dp),
        ) {
            Row(Modifier.align(Alignment.TopStart)) {
                CurrencyPill(R.drawable.ic_coin, progress.coins, Modifier.width(106.dp))
                Spacer(Modifier.width(7.dp))
                CurrencyPill(R.drawable.ic_crystal, progress.crystals, Modifier.width(90.dp))
            }

            val claimable = ProgressRules.claimableCount(progress)
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = 52.dp).width(62.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                HomeRailButton(R.drawable.ic_menu_shop, "SHOP") { model.navigate(AppScreen.SHOP) }
                HomeRailButton(R.drawable.ic_menu_feats, "FEATS", if (claimable > 0) claimable.toString() else null) { model.navigate(AppScreen.ACHIEVEMENTS) }
                HomeRailButton(R.drawable.ic_menu_legends, "LEGENDS") { model.navigate(AppScreen.LEADERBOARD) }
                HomeRailButton(R.drawable.ic_menu_daily, "DAILY", if (!model.isDailyClaimedToday()) "!" else null) { model.navigate(AppScreen.DAILY) }
                HomeRailButton(R.drawable.ic_menu_settings, "SETTINGS") { model.navigate(AppScreen.SETTINGS) }
            }

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(38.dp))
                Image(
                    painterResource(R.drawable.logo_thunder_stack),
                    "Thunder Stack",
                    Modifier.fillMaxWidth(.69f).weight(.48f),
                    contentScale = ContentScale.Fit,
                )
                PrimaryButton(
                    "PLAY",
                    Modifier.fillMaxWidth(.94f).height(80.dp),
                    backgroundRes = R.drawable.btn_play_hero,
                ) { model.navigate(AppScreen.LEVELS) }
                Spacer(Modifier.height(5.dp))
                PrimaryButton("ENDLESS TEMPLE", Modifier.fillMaxWidth(.84f).height(60.dp)) { model.startEndless() }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(.84f).height(60.dp)) {
                    PrimaryButton("CRYSTAL RUSH", Modifier.fillMaxSize(), onClick = model::openCrystalRush)
                    if (model.isCrystalRushRewardAvailable()) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 12.dp)
                                .size(17.dp).clip(RoundedCornerShape(9.dp)).background(Danger)
                                .border(1.dp, Marble, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("!", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(Modifier.weight(.18f))
                Text(
                    "${progress.totalStars()} / ${campaignLevels.size * 3} STARS  •  LEVEL ${progress.highestUnlocked}",
                    color = Marble, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = .7.sp,
                    maxLines = 1, modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeRailButton(
    @DrawableRes icon: Int,
    label: String,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(49.dp).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(icon),
                label,
                Modifier.size(45.dp),
                contentScale = ContentScale.Fit,
            )
            HomeBadge(badge)
        }
        Text(
            label, color = Marble, fontSize = 6.5.sp, fontWeight = FontWeight.Black,
            letterSpacing = .1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(StormNavy.copy(alpha = .72f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun BoxScope.HomeBadge(badge: String?) {
    if (!badge.isNullOrBlank()) {
        Box(
            Modifier.align(Alignment.TopEnd).size(16.dp).clip(RoundedCornerShape(8.dp))
                .background(Danger).border(1.dp, Marble, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
fun LevelSelectScreen(progress: PlayerProgress, model: ThunderStackViewModel) {
    SceneBackground(R.drawable.bg_level_select_path, shade = .22f) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp)) {
            ScreenHeader("Path to Olympus", onBack = { model.navigate(AppScreen.HOME) })
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("60 TRIALS • 6 REALMS", color = Marble, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("★ ${progress.totalStars()}", color = AntiqueGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
            ) {
                items(campaignLevels, key = { it.number }) { level ->
                    val unlocked = level.number <= progress.highestUnlocked
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (level.number % 2 == 0) Alignment.CenterEnd else Alignment.CenterStart) {
                        Row(
                            Modifier.fillMaxWidth(.91f).alpha(if (unlocked) 1f else .52f)
                                .clickable(enabled = unlocked) { model.selectLevel(level.number) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                                Image(painterResource(R.drawable.level_node), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                Text(if (unlocked) level.number.toString() else "—", color = Marble, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                            FramedSurface(
                                Modifier.weight(1f).heightIn(min = 70.dp), contentPaddingHorizontal = 13, contentPaddingVertical = 8,
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(level.title.uppercase(), color = Marble, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(level.worldName, color = MutedText, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Stars(progress.stars[level.number] ?: 0, compact = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreLevelScreen(number: Int, progress: PlayerProgress, model: ThunderStackViewModel) {
    val level = campaignLevels[(number - 1).coerceIn(0, campaignLevels.lastIndex)]
    SceneBackground(R.drawable.bg_level_select_path, shade = .48f) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(14.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScreenHeader("Trial ${level.number}", onBack = { model.navigate(AppScreen.LEVELS) }, onHome = { model.navigate(AppScreen.HOME) })
            Spacer(Modifier.height(18.dp))
            PopupPanel(Modifier.fillMaxWidth(.94f).heightIn(min = 550.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.level_node), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        Text(level.number.toString(), color = Marble, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        level.worldName.uppercase(), color = ElectricCyan, fontSize = 9.sp,
                        fontWeight = FontWeight.Black, letterSpacing = .8.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        level.title.uppercase(), color = Marble, fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 22.sp,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Stars(progress.stars[level.number] ?: 0)
                    TrialInfoRow("TARGET", "${level.targetHeight} TEMPLE PIECES")
                    TrialInfoRow("REWARD", "${level.rewardCoins} + RUN COINS")
                    TrialInfoRow("CONDITION", level.modifier.uppercase())
                    Text(
                        "Full blocks keep their overhang. Counterbalance the next floor before the temple leans too far.",
                        color = MutedText, fontSize = 8.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton("BEGIN TRIAL", Modifier.fillMaxWidth().height(54.dp)) { model.startSelectedLevel() }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TrialInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(0.84f).heightIn(min = 42.dp).padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = AntiqueGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.width(62.dp))
        Text(
            value, color = Marble, fontSize = 9.sp, fontWeight = FontWeight.Black,
            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ShopScreen(progress: PlayerProgress, model: ThunderStackViewModel) {
    CommonMetaScaffold("Divine Arsenal", model) {
        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), horizontalArrangement = Arrangement.End) {
            CurrencyPill(R.drawable.ic_coin, progress.coins, Modifier.width(106.dp))
            Spacer(Modifier.width(7.dp))
            CurrencyPill(R.drawable.ic_crystal, progress.crystals, Modifier.width(90.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
            items(BoosterType.entries) { type ->
                UniversalCard(Modifier.fillMaxWidth().heightIn(min = 190.dp)) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BoosterArt(type, Modifier.size(62.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(type.title.uppercase(), color = Marble, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(type.description, color = MutedText, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("OWNED  ${progress.boosters[type] ?: 0}", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            val price = if (type.crystalPrice > 0) "${type.crystalPrice} CRYSTALS" else "${type.coinPrice} COINS"
                            Text(price, color = AntiqueGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        PrimaryButton("BUY", Modifier.fillMaxWidth().height(49.dp)) { model.buyBooster(type) }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(progress: PlayerProgress, model: ThunderStackViewModel) {
    CommonMetaScaffold("Heroic Feats", model) {
        Text(
            "${progress.claimedAchievementIds.size} / ${achievementDefinitions.size} REWARDS CLAIMED",
            color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(7.dp),
        )
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
            items(achievementDefinitions, key = { it.id }) { definition ->
                AchievementRow(definition, progress) { model.claimAchievement(definition) }
            }
        }
    }
}

@Composable
private fun AchievementRow(definition: AchievementDefinition, progress: PlayerProgress, onClaim: () -> Unit) {
    val value = progress.achievementProgress[definition.metric] ?: 0
    val claimed = definition.id in progress.claimedAchievementIds
    val ready = value >= definition.target && !claimed
    UniversalCard(Modifier.fillMaxWidth().heightIn(min = 174.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(definition.title.uppercase(), color = Marble, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (claimed) "DONE" else "$value/${definition.target}", color = if (claimed) Success else ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Text(definition.description, color = MutedText, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ApprovedProgress(value.toFloat() / definition.target.toFloat(), Modifier.fillMaxWidth())
            val reward = buildList {
                if (definition.rewardCoins > 0) add("${definition.rewardCoins} COINS")
                if (definition.rewardCrystals > 0) add("${definition.rewardCrystals} CRYSTALS")
                definition.rewardBooster?.let { add(it.title.uppercase()) }
            }.joinToString(" + ")
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("REWARD  $reward", color = AntiqueGold, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 2, modifier = Modifier.weight(1f))
                if (ready) PrimaryButton("CLAIM", Modifier.width(108.dp).height(48.dp), onClick = onClaim)
            }
        }
    }
}

@Composable
fun DailyScreen(progress: PlayerProgress, claimedToday: Boolean, model: ThunderStackViewModel) {
    CommonMetaScaffold("Oracle's Gift", model) {
        FramedSurface(Modifier.fillMaxWidth().heightIn(min = 112.dp), accent = ElectricCyan) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.booster_crystal_magnet), null, Modifier.size(54.dp), contentScale = ContentScale.Fit)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("CRYSTAL RUSH", color = Marble, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(if (model.isCrystalRushRewardAvailable()) "Daily reward ready • 30 sec" else "Practice mode • Best ${progress.bestCrystalRushScore}", color = ElectricCyan, fontSize = 8.sp, maxLines = 2)
                }
                PrimaryButton("PLAY", Modifier.width(92.dp).height(48.dp)) { model.openCrystalRush() }
            }
        }
        Text("STREAK  ${progress.dailyStreak} DAYS", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(7.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            itemsIndexed(dailyRewards) { index, reward ->
                val current = (progress.dailyStreak % dailyRewards.size).coerceAtLeast(0)
                val completedThisCycle = if (claimedToday) ((progress.dailyStreak - 1).mod(dailyRewards.size) + 1) else current
                val status = when {
                    index < completedThisCycle -> "CLAIMED"
                    !claimedToday && index == current -> "CLAIM"
                    else -> "LOCKED"
                }
                FramedSurface(Modifier.fillMaxWidth().heightIn(min = 78.dp).alpha(if (status == "LOCKED") .67f else 1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("DAY ${index + 1}", color = AntiqueGold, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.width(50.dp))
                        Text(
                            reward.label, color = Marble, fontWeight = FontWeight.Black, fontSize = 10.sp,
                            modifier = Modifier.weight(1f).padding(end = 7.dp), maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        PrimaryButton(
                            status, Modifier.width(112.dp).height(42.dp), enabled = status == "CLAIM",
                            onClick = model::claimDaily,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun LeaderboardScreen(progress: PlayerProgress, model: ThunderStackViewModel) {
    val playerValue = progress.totalStars()
    val base = listOf(146, 121, 97, 74, 55, 31, 18)
    val names = listOf("ATLAS", "NYX", "HELIOS", "THALIA", "ORION", "IRIS", "AEON")
    val entries = (names.zip(base).map { LeaderboardEntry(it.first, it.second) } +
        LeaderboardEntry("YOU", playerValue, true)).sortedByDescending { it.value }
    val playerRank = entries.indexOfFirst { it.isPlayer } + 1

    CommonMetaScaffold("Local Legends", model) {
        Text(
            "ON-DEVICE HALL OF FAME", color = MutedText, fontSize = 8.5.sp,
            letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
        FramedSurface(
            Modifier.fillMaxWidth(.92f).align(Alignment.CenterHorizontally).height(72.dp),
            accent = AntiqueGold,
            contentPaddingHorizontal = 10,
            contentPaddingVertical = 4,
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "YOUR BEST", color = AntiqueGold, fontSize = 8.sp, lineHeight = 10.sp,
                    fontWeight = FontWeight.Black, letterSpacing = .75.sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier.fillMaxWidth(.9f).height(34.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendMetric("RANK", "#$playerRank", Modifier.weight(1f))
                    LegendMetric("STARS", playerValue.toString(), Modifier.weight(1f))
                    LegendMetric("LEVEL", progress.highestUnlocked.toString(), Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 10.dp),
        ) {
            itemsIndexed(entries) { index, entry ->
                LegendRow(index, entry)
            }
        }
    }
}

@Composable
private fun LegendMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label, color = MutedText, fontSize = 7.sp, lineHeight = 8.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(value, color = Marble, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun LegendRow(index: Int, entry: LeaderboardEntry) {
    val accent = when (index) {
        0 -> AntiqueGold
        1 -> Color(0xFFC9D2DE)
        2 -> Color(0xFFCF8250)
        else -> Color(0xFF8A91AF)
    }
    Box(Modifier.fillMaxWidth().height(70.dp).clipToBounds(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.panel_leaderboard_row),
            null,
            Modifier.fillMaxSize().graphicsLayer { scaleY = 1.42f },
            contentScale = ContentScale.FillBounds,
        )
        Row(
            Modifier.fillMaxSize().padding(start = 50.dp, end = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${index + 1}", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.width(31.dp), maxLines = 1,
            )
            Column(Modifier.weight(1f).padding(start = 24.dp, end = 6.dp)) {
                Text(
                    entry.name, color = if (entry.isPlayer) ElectricCyan else Marble,
                    fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.isPlayer) Text("YOUR RECORD", color = ElectricCyan, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                entry.value.toString(), color = Marble, fontWeight = FontWeight.Black, fontSize = 12.sp,
                maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.width(54.dp),
            )
        }
    }
}

@Composable
fun SettingsScreen(progress: PlayerProgress, model: ThunderStackViewModel) {
    CommonMetaScaffold("Settings", model, deepShade = true) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingRow("MUSIC", "Procedural Olympus ambience", progress.musicEnabled, model::setMusic)
            SettingRow("SOUND EFFECTS", "Placement and reward cues", progress.soundEnabled, model::setSound)
            SettingRow("HAPTICS", "Responsive impact vibrations", progress.hapticsEnabled, model::setHaptics)
            SettingRow("REDUCED FLASHES", "Softens lightning and perfect effects", progress.reducedFlashes, model::setReducedFlashes)
            SettingRow("HIGH CONTRAST", "Stronger arena guides and labels", progress.highContrast, model::setHighContrast)
            FramedSurface(Modifier.fillMaxWidth().heightIn(min = 128.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("THUNDER STACK", color = Marble, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                    Text("Cortex Studio • Local save • Offline play", color = MutedText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("No ads. No fake online services. Your temple progress stays on this device.", color = ElectricCyan, fontSize = 9.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, maxLines = 3)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String, enabled: Boolean, set: (Boolean) -> Unit) {
    FramedSurface(Modifier.fillMaxWidth().heightIn(min = 102.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, color = Marble, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(description, color = MutedText, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            SettingToggle(enabled, set)
        }
    }
}

@Composable
private fun CommonMetaScaffold(
    title: String,
    model: ThunderStackViewModel,
    deepShade: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    SceneBackground(R.drawable.bg_home_olympus, shade = if (deepShade) .72f else .42f) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp)) {
            ScreenHeader(title, onBack = { model.navigate(AppScreen.HOME) }, onHome = { model.navigate(AppScreen.HOME) })
            content()
        }
    }
}

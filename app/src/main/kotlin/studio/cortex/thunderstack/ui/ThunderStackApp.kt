package studio.cortex.thunderstack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import studio.cortex.thunderstack.model.AppScreen

@Composable
fun ThunderStackApp(model: ThunderStackViewModel) {
    val progress by model.progress.collectAsStateWithLifecycle()
    val ready by model.progressReady.collectAsStateWithLifecycle()
    val screen by model.screen.collectAsStateWithLifecycle()
    val selectedLevel by model.selectedLevel.collectAsStateWithLifecycle()
    val game by model.game.collectAsStateWithLifecycle()
    val rush by model.rush.collectAsStateWithLifecycle()
    val toast by model.toast.collectAsStateWithLifecycle()
    var loadingHoldComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1_250)
        loadingHoldComplete = true
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_200)
            model.dismissToast()
        }
    }

    BackHandler(enabled = screen != AppScreen.HOME) { model.handleBack() }

    Box(Modifier.fillMaxSize().background(StormNavy)) {
        if (!ready || !loadingHoldComplete) {
            LoadingScreen()
        } else {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screenTransition",
            ) { target ->
                when (target) {
                    AppScreen.HOME -> HomeScreen(progress, model)
                    AppScreen.LEVELS -> LevelSelectScreen(progress, model)
                    AppScreen.PRELEVEL -> PreLevelScreen(selectedLevel, progress, model)
                    AppScreen.SHOP -> ShopScreen(progress, model)
                    AppScreen.ACHIEVEMENTS -> AchievementsScreen(progress, model)
                    AppScreen.LEADERBOARD -> LeaderboardScreen(progress, model)
                    AppScreen.DAILY -> DailyScreen(progress, model.isDailyClaimedToday(), model)
                    AppScreen.SETTINGS -> SettingsScreen(progress, model)
                    AppScreen.GAMEPLAY -> game?.let { GameplayScreen(it, progress, model) } ?: HomeScreen(progress, model)
                    AppScreen.CRYSTAL_RUSH -> rush?.let { CrystalRushScreen(it, progress, model) } ?: HomeScreen(progress, model)
                }
            }
        }

        toast?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 112.dp)
                    .background(StormNavy.copy(alpha = .96f), RoundedCornerShape(13.dp))
                    .border(1.dp, ElectricCyan, RoundedCornerShape(13.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

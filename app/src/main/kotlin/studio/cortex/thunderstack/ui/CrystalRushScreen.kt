package studio.cortex.thunderstack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlinx.coroutines.isActive
import studio.cortex.thunderstack.R
import studio.cortex.thunderstack.model.CrystalRushPhase
import studio.cortex.thunderstack.model.CrystalRushState
import studio.cortex.thunderstack.model.PlayerProgress
import studio.cortex.thunderstack.model.RushItemKind

@Composable
fun CrystalRushScreen(state: CrystalRushState, progress: PlayerProgress, model: ThunderStackViewModel) {
    var exitTarget by remember { mutableStateOf<ExitTarget?>(null) }
    fun requestExit(target: ExitTarget) {
        if (state.phase == CrystalRushPhase.PLAYING) {
            if (!state.isPaused) model.toggleCrystalRushPause()
            exitTarget = target
        } else {
            model.leaveCrystalRush()
        }
    }
    fun cancelExit() {
        exitTarget = null
        if (state.isPaused) model.toggleCrystalRushPause()
    }
    BackHandler {
        if (exitTarget != null) cancelExit() else requestExit(ExitTarget.PREVIOUS)
    }
    LaunchedEffect(state.phase, state.isPaused) {
        if (state.phase != CrystalRushPhase.PLAYING || state.isPaused) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { frame ->
                if (lastFrame != 0L) model.advanceCrystalRush((frame - lastFrame) / 1_000_000_000f)
                lastFrame = frame
            }
        }
    }

    SceneBackground(R.drawable.bg_gameplay_olympus_sky, shade = .24f) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painterResource(R.drawable.bg_reference_column), null,
                Modifier.align(Alignment.CenterStart).fillMaxHeight().width(46.dp),
                contentScale = ContentScale.FillBounds,
            )
            Image(
                painterResource(R.drawable.bg_reference_column), null,
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(46.dp),
                contentScale = ContentScale.FillBounds,
            )
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                CrystalRushHud(
                    state, model,
                    onBack = { requestExit(ExitTarget.PREVIOUS) },
                    onHome = { requestExit(ExitTarget.HOME) },
                )
                RushArena(state, model, Modifier.weight(1f).fillMaxWidth())
                Text(
                    if (state.stunSeconds > 0f) "STUNNED" else "DRAG THE MAGNET • DODGE CRACKED STONE",
                    color = if (state.stunSeconds > 0f) Danger else Marble,
                    fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 7.dp),
                )
            }
            when {
                exitTarget != null -> ExitConfirmationOverlay(
                    target = exitTarget!!,
                    previousLabel = "Home",
                    onCancel = ::cancelExit,
                    onConfirm = model::leaveCrystalRush,
                )
                state.phase == CrystalRushPhase.READY -> RushReadyOverlay(state.rewardEligible, progress.bestCrystalRushScore, model)
                state.phase == CrystalRushPhase.FINISHED -> RushResultOverlay(state, progress, model)
                state.isPaused -> RushPauseOverlay(model)
            }
        }
    }
}

@Composable
private fun CrystalRushHud(state: CrystalRushState, model: ThunderStackViewModel, onBack: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(StormNavy.copy(alpha = .9f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AssetButton(R.drawable.ic_nav_back, "Back", Modifier.size(44.dp), onClick = onBack)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text("CRYSTAL RUSH", color = Marble, fontFamily = FontFamily.Serif, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                ApprovedProgress(state.remainingSeconds / state.durationSeconds, Modifier.fillMaxWidth())
            }
            Text("${ceil(state.remainingSeconds).toInt()}s", color = AntiqueGold, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(4.dp))
            AssetButton(R.drawable.ic_nav_home, "Home", Modifier.size(44.dp), onClick = onHome)
            Spacer(Modifier.width(3.dp))
            AssetButton(
                R.drawable.ic_nav_pause, "Pause", Modifier.size(44.dp),
                enabled = state.phase == CrystalRushPhase.PLAYING, onClick = model::toggleCrystalRushPause,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("♥".repeat(state.hearts) + "♡".repeat(3 - state.hearts), color = Danger, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("SCORE  ${state.score}", color = Marble, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("×${state.multiplier}  COMBO ${state.combo}", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun RushArena(state: CrystalRushState, model: ThunderStackViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val images = remember(context) {
        mapOf(
            RushItemKind.CRYSTAL to ImageBitmap.imageResource(context.resources, R.drawable.collectible_crystal),
            RushItemKind.COIN to ImageBitmap.imageResource(context.resources, R.drawable.ic_coin),
            RushItemKind.GEM_RED to ImageBitmap.imageResource(context.resources, R.drawable.rush_gem_red_v2),
            RushItemKind.GEM_BLUE to ImageBitmap.imageResource(context.resources, R.drawable.rush_gem_blue_v2),
            RushItemKind.GEM_PURPLE to ImageBitmap.imageResource(context.resources, R.drawable.rush_gem_purple_v2),
            RushItemKind.GEM_GREEN to ImageBitmap.imageResource(context.resources, R.drawable.rush_gem_green_v2),
            RushItemKind.CRACKED to ImageBitmap.imageResource(context.resources, R.drawable.block_cracked),
            RushItemKind.LIGHTNING to ImageBitmap.imageResource(context.resources, R.drawable.booster_thunder),
        )
    }
    val magnet = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.booster_crystal_magnet) }
    Canvas(
        modifier.pointerInput(state.phase, state.isPaused) {
            detectDragGestures(
                onDragStart = { point -> model.moveCrystalRushMagnet(point.x / size.width.toFloat()) },
                onDrag = { change, _ ->
                    change.consume()
                    model.moveCrystalRushMagnet(change.position.x / size.width.toFloat())
                },
            )
        }
    ) {
        val safeLeft = size.width * .09f
        val safeWidth = size.width * .82f
        drawRect(StormNavy.copy(alpha = .15f), Offset(safeLeft, 0f), androidx.compose.ui.geometry.Size(safeWidth, size.height))
        state.items.forEach { item ->
            val image = images.getValue(item.kind)
            val width = when (item.kind) {
                RushItemKind.CRACKED -> size.width * .15f
                RushItemKind.LIGHTNING -> size.width * .12f
                RushItemKind.GEM_RED, RushItemKind.GEM_BLUE, RushItemKind.GEM_PURPLE, RushItemKind.GEM_GREEN -> size.width * .10f
                else -> size.width * .105f
            }
            val height = if (item.kind == RushItemKind.CRACKED) width * .38f else width
            drawRushImage(image, item.x * size.width - width / 2f, item.y * size.height, width, height)
        }
        val magnetWidth = size.width * .17f
        val magnetHeight = magnetWidth * .62f
        val magnetY = size.height * .86f
        drawRushImage(magnet, state.magnetX * size.width - magnetWidth / 2f, magnetY, magnetWidth, magnetHeight, if (state.stunSeconds > 0f) .48f else 1f)
        if (state.shieldSeconds > 0f) {
            drawCircle(ElectricCyan.copy(alpha = .20f), magnetWidth * .65f, Offset(state.magnetX * size.width, magnetY + magnetHeight / 2f))
            drawCircle(ElectricCyan.copy(alpha = .78f), magnetWidth * .65f, Offset(state.magnetX * size.width, magnetY + magnetHeight / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
        }
    }
}

private fun DrawScope.drawRushImage(image: ImageBitmap, left: Float, top: Float, width: Float, height: Float, alpha: Float = 1f) {
    drawImage(
        image, dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)), alpha = alpha,
    )
}

@Composable
private fun RushReadyOverlay(rewardEligible: Boolean, best: Int, model: ThunderStackViewModel) {
    Box(Modifier.fillMaxSize().background(StormNavy.copy(alpha = .75f)), contentAlignment = Alignment.Center) {
        CompactDialogSurface(Modifier.fillMaxWidth(.82f).heightIn(min = 280.dp, max = 310.dp)) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Image(painterResource(R.drawable.booster_crystal_magnet), null, Modifier.size(42.dp), contentScale = ContentScale.Fit)
                Text("CRYSTAL RUSH", color = Marble, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Catch gems, avoid cracked stone, and use lightning for a shield.",
                    color = MutedText, fontSize = 9.sp, lineHeight = 12.sp,
                    textAlign = TextAlign.Center, maxLines = 2,
                )
                Text(if (rewardEligible) "TODAY'S REWARD IS READY" else "PRACTICE • BEST $best", color = if (rewardEligible) AntiqueGold else ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                PrimaryButton("START 30 SEC", Modifier.fillMaxWidth().height(48.dp), onClick = model::startCrystalRush)
                PrimaryButton("BACK HOME", Modifier.fillMaxWidth().height(46.dp), onClick = model::leaveCrystalRush)
            }
        }
    }
}

@Composable
private fun RushPauseOverlay(model: ThunderStackViewModel) {
    Box(Modifier.fillMaxSize().background(StormNavy.copy(alpha = .78f)), contentAlignment = Alignment.Center) {
        CompactDialogSurface(Modifier.fillMaxWidth(.82f).heightIn(min = 250.dp, max = 290.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RUSH PAUSED", color = Marble, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Black)
                PrimaryButton("RESUME", Modifier.fillMaxWidth().height(48.dp), onClick = model::toggleCrystalRushPause)
                PrimaryButton("RESTART", Modifier.fillMaxWidth().height(46.dp), onClick = model::restartCrystalRush)
                PrimaryButton("HOME", Modifier.fillMaxWidth().height(46.dp), onClick = model::leaveCrystalRush)
            }
        }
    }
}

@Composable
private fun RushResultOverlay(state: CrystalRushState, progress: PlayerProgress, model: ThunderStackViewModel) {
    val coins = 40 + (state.score / 5).coerceAtMost(160)
    val crystals = when { state.score >= 360 -> 3; state.score >= 240 -> 2; state.score >= 120 -> 1; else -> 0 }
    Box(Modifier.fillMaxSize().background(StormNavy.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
        CompactDialogSurface(Modifier.fillMaxWidth(.82f).heightIn(min = 290.dp, max = 330.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("RUSH COMPLETE", color = AntiqueGold, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(state.score.toString(), color = Marble, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("BEST  ${maxOf(progress.bestCrystalRushScore, state.score)}", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                if (state.rewardEligible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(30.dp)); Text("+$coins", color = AntiqueGold, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(14.dp))
                        Image(painterResource(R.drawable.collectible_crystal), null, Modifier.size(30.dp), contentScale = ContentScale.Fit); Text("+$crystals", color = ElectricCyan, fontWeight = FontWeight.Black)
                    }
                } else Text("PRACTICE • NO CURRENCY PAYOUT", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                PrimaryButton("PLAY PRACTICE", Modifier.fillMaxWidth().height(48.dp), onClick = model::restartCrystalRush)
                PrimaryButton("HOME", Modifier.fillMaxWidth().height(46.dp), onClick = model::leaveCrystalRush)
            }
        }
    }
}

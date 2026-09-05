package studio.cortex.thunderstack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import studio.cortex.thunderstack.R
import studio.cortex.thunderstack.model.BlockKind
import studio.cortex.thunderstack.model.BoosterType
import studio.cortex.thunderstack.model.PlacementGrade
import studio.cortex.thunderstack.model.PlayerProgress
import studio.cortex.thunderstack.model.RunPhase
import studio.cortex.thunderstack.model.StackGameState
import studio.cortex.thunderstack.model.TempleCourse

@Composable
fun GameplayScreen(state: StackGameState, progress: PlayerProgress, model: ThunderStackViewModel) {
    var exitTarget by remember { mutableStateOf<ExitTarget?>(null) }
    fun requestExit(target: ExitTarget) {
        if (state.isFinished) {
            model.leaveGameplay(toLevels = target == ExitTarget.PREVIOUS && !state.endless)
            return
        }
        if (!state.isPaused) model.togglePause()
        exitTarget = target
    }
    fun cancelExit() {
        exitTarget = null
        if (state.isPaused) model.togglePause()
    }
    BackHandler {
        if (exitTarget != null) cancelExit() else requestExit(ExitTarget.PREVIOUS)
    }
    LaunchedEffect(state.isPaused, state.isFinished) {
        if (state.isPaused || state.isFinished) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { frame ->
                if (lastFrame != 0L) model.advance((frame - lastFrame) / 1_000_000_000f)
                lastFrame = frame
            }
        }
    }

    SceneBackground(R.drawable.bg_gameplay_olympus_sky, shade = 0.12f) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (!state.isFinished) GameplayHud(
                state, model,
                onBack = { requestExit(ExitTarget.PREVIOUS) },
                onHome = { requestExit(ExitTarget.HOME) },
            )
            Arena(state, progress, model, Modifier.weight(1f).fillMaxWidth())
            if (!state.isFinished) BoosterTray(state, progress, model)
        }
        when {
            exitTarget != null -> ExitConfirmationOverlay(
                target = exitTarget!!,
                previousLabel = if (state.endless) "Home" else "Path to Olympus",
                onCancel = ::cancelExit,
                onConfirm = {
                    model.leaveGameplay(toLevels = exitTarget == ExitTarget.PREVIOUS && !state.endless)
                },
            )
            state.isPaused && !state.isFinished -> PauseOverlay(state, model)
            state.isFinished -> ResultOverlay(state, model.starsFor(state), model)
        }
    }
}

@Composable
private fun GameplayHud(state: StackGameState, model: ThunderStackViewModel, onBack: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(StormNavy.copy(alpha = .42f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AssetButton(R.drawable.ic_nav_back, "Back", Modifier.size(48.dp), onClick = onBack)
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(13.dp))
                    .background(StormNavy.copy(alpha = .82f)).border(1.dp, AntiqueGold.copy(alpha = .72f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (state.endless) "ENDLESS TEMPLE" else "TRIAL ${state.level.number} • ${state.level.title.uppercase()}",
                    color = Marble, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black,
                    fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (state.endless) "HEIGHT ${state.height}" else "TEMPLE ${state.height}/${state.level.targetHeight}",
                    color = ElectricCyan, fontSize = 8.5.sp, fontWeight = FontWeight.Black, maxLines = 1,
                )
                if (!state.endless) {
                    Box(
                        Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = .16f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier.fillMaxWidth((state.height.toFloat() / state.level.targetHeight).coerceIn(0f, 1f))
                                .fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF9436FF), ElectricCyan)))
                        )
                    }
                }
            }
            AssetButton(R.drawable.ic_nav_home, "Home", Modifier.size(48.dp), onClick = onHome)
            AssetButton(R.drawable.ic_nav_pause, "Pause", Modifier.size(48.dp), onClick = model::togglePause)
        }
        Row(
            Modifier.fillMaxWidth().height(25.dp).padding(horizontal = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SCORE  ${state.score}", color = Marble, fontSize = 8.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text("COMBO  ×${state.combo}", color = if (state.combo > 1) ElectricCyan else MutedText, fontSize = 8.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text("PERFECT  ${state.perfects}", color = AntiqueGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun Arena(state: StackGameState, progress: PlayerProgress, model: ThunderStackViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val materialImages = remember(context) {
        mapOf(
            BlockKind.STONE to ImageBitmap.imageResource(context.resources, R.drawable.block_stone),
            BlockKind.MOSS to ImageBitmap.imageResource(context.resources, R.drawable.block_stone_moss),
            BlockKind.GOLD to ImageBitmap.imageResource(context.resources, R.drawable.block_gold),
            BlockKind.STORM to ImageBitmap.imageResource(context.resources, R.drawable.block_storm),
            BlockKind.CRACKED to ImageBitmap.imageResource(context.resources, R.drawable.block_cracked),
        )
    }
    val courseImages = remember(context) {
        mapOf(
            TempleCourse.STEP to ImageBitmap.imageResource(context.resources, R.drawable.temple_step),
            TempleCourse.MARBLE_COURSE to ImageBitmap.imageResource(context.resources, R.drawable.temple_marble_course),
            TempleCourse.COLUMN_COURSE to ImageBitmap.imageResource(context.resources, R.drawable.temple_column_course),
            TempleCourse.ENTABLATURE to ImageBitmap.imageResource(context.resources, R.drawable.temple_entablature),
            TempleCourse.PEDIMENT to ImageBitmap.imageResource(context.resources, R.drawable.temple_pediment),
        )
    }
    val stormCourse = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.temple_storm_altar) }
    val crackedCourse = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.temple_cracked_ruin) }
    val platform = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.platform_start) }
    val glow = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.fx_perfect_glow) }
    val lightning = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.fx_lightning) }
    val crystal = remember(context) { ImageBitmap.imageResource(context.resources, R.drawable.collectible_crystal) }
    val effect = remember { Animatable(0f) }
    LaunchedEffect(state.feedbackToken) {
        if (state.lastGrade == PlacementGrade.PERFECT || state.lastGrade == PlacementGrade.COLLAPSE) {
            effect.snapTo(1f)
            effect.animateTo(0f, tween(if (state.lastGrade == PlacementGrade.PERFECT) 620 else 850))
        }
    }

    Box(
        modifier.clickable(enabled = !state.isPaused && !state.isFinished && state.phase == RunPhase.PLAYING, onClick = model::dropBlock),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val arenaLeft = size.width * .035f
            val arenaWidth = size.width * .93f
            val baseUnit = (size.width * .105f).coerceIn(34f, 122f)
            val visualHeight: (TempleCourse) -> Float = { baseUnit * courseHeightFactor(it) }
            // Course sprites have different silhouettes. Advance by the height of the
            // block being placed (rather than the previous block) and overlap the
            // courses slightly so the masonry always reads as one connected tower.
            val stepHeight: (TempleCourse) -> Float = { visualHeight(it) * .92f }
            val baseline = size.height - baseUnit * .82f
            val placedStackHeight = state.blocks.drop(1).fold(0f) { total, block ->
                total + stepHeight(block.course)
            }
            val nextStackHeight = placedStackHeight + stepHeight(state.mover.course)
            val scrollPx = (nextStackHeight - size.height * .58f).coerceAtLeast(0f)
            val collapseLean = (state.collapseElapsed / .35f).coerceIn(0f, 1f)
            val fallT = ((state.collapseElapsed - .35f) / 1f).coerceIn(0f, 1f)
            val fallSquared = fallT.pow(2)
            val liveSway = sin(state.elapsedSeconds * 4.6f) * (state.stability / 100f) * 4.4f
            val impactSway = sin(state.elapsedSeconds * 18f) * state.impactStrength * 6.2f

            drawRect(
                color = if (progress.highContrast) StormNavy.copy(alpha = .23f) else Color.Transparent,
                topLeft = Offset(arenaLeft, 0f),
                size = Size(arenaWidth, size.height),
            )
            drawLine(
                color = if (progress.highContrast) ElectricCyan.copy(alpha = .46f) else Color.White.copy(alpha = .12f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = if (progress.highContrast) 2.5f else 1f,
            )

            drawImageRect(platform, arenaLeft - arenaWidth * .07f, baseline + baseUnit * .16f, arenaWidth * 1.14f, baseUnit * .88f)

            var verticalOffset = 0f
            state.blocks.forEachIndexed { blockPosition, block ->
                if (blockPosition > 0) verticalOffset += stepHeight(block.course)
                val width = block.width * arenaWidth
                val blockHeight = visualHeight(block.course)
                val heightFraction = (block.index.toFloat() / state.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                var center = arenaLeft + block.center * arenaWidth
                var y = baseline - verticalOffset + scrollPx
                val image = templeImage(block.kind, block.course, courseImages, materialImages, stormCourse, crackedCourse)
                var rotation = block.tilt * .58f + block.lean * 2.1f * heightFraction
                if (state.phase == RunPhase.PLAYING) {
                    rotation += (liveSway + impactSway) * heightFraction
                    center += (liveSway + impactSway) * arenaWidth * .0009f * heightFraction
                } else if (state.phase == RunPhase.COLLAPSING) {
                    center += state.collapseDirection * arenaWidth * (.025f + .16f * heightFraction) * fallSquared
                    y += size.height * (.07f + .34f * heightFraction) * fallSquared
                    rotation += state.collapseDirection * (12f * collapseLean + (28f + 52f * heightFraction) * fallT)
                }
                rotate(rotation, Offset(center, y + blockHeight / 2f)) {
                    drawImageRect(image, center - width / 2f, y, width, blockHeight)
                }
            }

            if (!state.isFinished && state.phase == RunPhase.PLAYING) {
                val moverWidth = state.mover.width * arenaWidth
                val moverCenter = arenaLeft + state.mover.center * arenaWidth
                val moverHeight = visualHeight(state.mover.course)
                val moverY = baseline - nextStackHeight + scrollPx
                val moverImage = templeImage(state.mover.kind, state.mover.course, courseImages, materialImages, stormCourse, crackedCourse)
                drawImageRect(moverImage, moverCenter - moverWidth / 2f, moverY, moverWidth, moverHeight)
                drawLine(ElectricCyan.copy(alpha = .22f), Offset(moverCenter, moverY + moverHeight), Offset(moverCenter, baseline), 1.5f)
            }

            state.missedMover?.takeIf { state.phase == RunPhase.COLLAPSING }?.let { missed ->
                val moverWidth = missed.width * arenaWidth
                val moverHeight = visualHeight(missed.course)
                val startY = baseline - nextStackHeight + scrollPx
                val x = arenaLeft + missed.center * arenaWidth + state.collapseDirection * fallSquared * arenaWidth * .2f
                val y = startY + fallSquared * size.height * .7f
                rotate(state.collapseDirection * fallT * 105f, Offset(x, y + moverHeight / 2f)) {
                    drawImageRect(
                        templeImage(missed.kind, missed.course, courseImages, materialImages, stormCourse, crackedCourse),
                        x - moverWidth / 2f, y, moverWidth, moverHeight,
                    )
                }
            }

            if (state.magnetDrops > 0 && !state.isFinished) {
                val bob = kotlin.math.sin(state.elapsedSeconds * 3f) * 10f
                drawImageRect(crystal, arenaLeft + arenaWidth * .78f, size.height * .28f + bob, 50f, 60f)
            }

            if (effect.value > 0f && !progress.reducedFlashes) {
                val fxY = baseline - placedStackHeight + scrollPx - baseUnit * 1.35f
                if (state.lastGrade == PlacementGrade.PERFECT) {
                    drawImage(
                        glow,
                        dstOffset = IntOffset((size.width * .07f).toInt(), fxY.toInt()),
                        dstSize = IntSize((size.width * .86f).toInt(), (baseUnit * 3.1f).toInt()),
                        alpha = effect.value,
                        blendMode = BlendMode.Plus,
                    )
                } else {
                    drawImage(
                        lightning,
                        dstOffset = IntOffset((size.width * .12f).toInt(), 0),
                        dstSize = IntSize((size.width * .76f).toInt(), size.height.toInt()),
                        alpha = effect.value,
                        blendMode = BlendMode.Plus,
                    )
                }
            }
        }

        Column(Modifier.align(Alignment.TopCenter).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                state.feedback,
                color = when (state.lastGrade) {
                    PlacementGrade.PERFECT -> AntiqueGold
                    PlacementGrade.CROOKED, PlacementGrade.COLLAPSE -> Danger
                    else -> Marble
                },
                fontSize = if (state.lastGrade == PlacementGrade.PERFECT) 20.sp else 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp,
                modifier = Modifier.background(StormNavy.copy(alpha = .66f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
            )
            if (state.level.world >= 2 && !state.isFinished) Text("WIND WAVES ARE ACTIVE", color = ElectricCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        if (state.height == 0 && !state.isFinished) {
            Text(
                "TAP THE SKY TO PLACE",
                color = Marble,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).background(StormNavy.copy(alpha = .75f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

private fun DrawScope.drawImageRect(image: ImageBitmap, left: Float, top: Float, width: Float, height: Float, alpha: Float = 1f) {
    drawImage(
        image,
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
        alpha = alpha,
    )
}

private fun courseHeightFactor(course: TempleCourse): Float = when (course) {
    TempleCourse.STEP -> 1.72f
    TempleCourse.MARBLE_COURSE -> 1.50f
    TempleCourse.COLUMN_COURSE -> 2.02f
    TempleCourse.ENTABLATURE -> 1.56f
    TempleCourse.PEDIMENT -> 2.08f
}

private fun templeImage(
    kind: BlockKind,
    course: TempleCourse,
    courses: Map<TempleCourse, ImageBitmap>,
    materials: Map<BlockKind, ImageBitmap>,
    storm: ImageBitmap,
    cracked: ImageBitmap,
): ImageBitmap = when {
    course == TempleCourse.PEDIMENT -> courses.getValue(TempleCourse.PEDIMENT)
    kind == BlockKind.STORM -> storm
    kind == BlockKind.CRACKED -> cracked
    kind == BlockKind.MOSS && course == TempleCourse.MARBLE_COURSE -> materials.getValue(BlockKind.MOSS)
    kind == BlockKind.GOLD && course == TempleCourse.MARBLE_COURSE -> courses.getValue(TempleCourse.ENTABLATURE)
    else -> courses.getValue(course)
}

@Composable
private fun BoosterTray(state: StackGameState, progress: PlayerProgress, model: ThunderStackViewModel) {
    Row(
        Modifier.fillMaxWidth().height(79.dp).background(StormNavy.copy(alpha = .76f)).padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoosterType.entries.forEach { type ->
            val active = when (type) {
                BoosterType.SHIELD -> state.shieldCharges > 0
                BoosterType.SLOW_TIME -> state.slowDrops > 0
                BoosterType.CRYSTAL_MAGNET -> state.magnetDrops > 0
                BoosterType.THUNDER -> false
            }
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                    .background(if (active) RoyalSapphire.copy(alpha = .92f) else StormNavy.copy(alpha = .86f))
                    .border(1.dp, if (active) ElectricCyan else AntiqueGold.copy(alpha = .76f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !state.isPaused && !state.isFinished) { model.useBooster(type) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BoosterArt(type, Modifier.size(42.dp))
                    Text("×${progress.boosters[type] ?: 0}", color = if (active) ElectricCyan else Marble, fontSize = 8.5.sp, lineHeight = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(state: StackGameState, model: ThunderStackViewModel) {
    Box(Modifier.fillMaxSize().background(StormNavy.copy(alpha = .74f)), contentAlignment = Alignment.Center) {
        PopupPanel(Modifier.fillMaxWidth(.88f).height(410.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("TEMPLE PAUSED", color = Marble, fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("The winds wait for your return.", color = MutedText, fontSize = 10.sp)
                PrimaryButton("RESUME", Modifier.fillMaxWidth().height(56.dp)) { model.togglePause() }
                PrimaryButton("RESTART TRIAL", Modifier.fillMaxWidth().height(52.dp)) { model.restartGame() }
                PrimaryButton(if (state.endless) "HOME" else "PATH TO OLYMPUS", Modifier.fillMaxWidth().height(52.dp)) { model.leaveGameplay(toLevels = !state.endless) }
                if (!state.endless) PrimaryButton("HOME", Modifier.fillMaxWidth().height(50.dp)) { model.leaveGameplay(toLevels = false) }
            }
        }
    }
}

@Composable
private fun ResultOverlay(state: StackGameState, stars: Int, model: ThunderStackViewModel) {
    Box(Modifier.fillMaxSize().background(StormNavy.copy(alpha = .78f)), contentAlignment = Alignment.Center) {
        val hasNextTrial = state.won && !state.endless && state.level.number < 60
        PopupPanel(
            Modifier
                .fillMaxWidth(.88f)
                .fillMaxHeight(if (hasNextTrial) .78f else .70f),
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.width(46.dp).height(3.dp).clip(CircleShape)
                        .background(if (state.won) AntiqueGold else Danger.copy(alpha = .9f)),
                )
                Text(
                    when { state.won -> "OLYMPUS RISES"; state.endless -> "LEGEND RECORDED"; else -> "TEMPLE FALLEN" },
                    color = if (state.won) AntiqueGold else Marble,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.won && !state.endless) Stars(stars)
                Text(
                    state.feedback,
                    color = if (state.won) Success else Danger,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(StormNavy.copy(alpha = .62f)).padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    ResultStatRow("TEMPLE HEIGHT", state.height.toString())
                    ResultStatDivider()
                    ResultStatRow("SCORE", state.score.toString())
                    ResultStatDivider()
                    ResultStatRow("PERFECT DROPS", state.perfects.toString())
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.size(31.dp), contentScale = ContentScale.Fit)
                    val reward = state.runCoins + if (state.won && !state.endless) state.level.rewardCoins else 0
                    Text("+$reward", color = AntiqueGold, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    if (state.runCrystals > 0) {
                        Spacer(Modifier.width(12.dp))
                        Image(painterResource(R.drawable.ic_crystal), null, Modifier.size(31.dp), contentScale = ContentScale.Fit)
                        Text("+${state.runCrystals}", color = ElectricCyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
                if (hasNextTrial) {
                    PrimaryButton("NEXT TRIAL", Modifier.fillMaxWidth().height(52.dp)) { model.nextLevel() }
                }
                PrimaryButton(if (state.won) "PLAY AGAIN" else "REBUILD", Modifier.fillMaxWidth().height(49.dp)) { model.restartGame() }
                PrimaryButton(if (state.endless) "HOME" else "PATH TO OLYMPUS", Modifier.fillMaxWidth().height(49.dp)) { model.leaveGameplay(toLevels = !state.endless) }
            }
        }
    }
}

@Composable
private fun ResultStatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().height(31.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(value, color = Marble, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun ResultStatDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(AntiqueGold.copy(alpha = .18f)))
}

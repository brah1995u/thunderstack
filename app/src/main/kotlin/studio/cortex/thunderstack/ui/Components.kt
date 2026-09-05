package studio.cortex.thunderstack.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import studio.cortex.thunderstack.R
import studio.cortex.thunderstack.model.BlockKind
import studio.cortex.thunderstack.model.BoosterType

@Composable
fun SceneBackground(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    shade: Float = 0.18f,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Image(painterResource(resource), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (shade > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(StormNavy.copy(alpha = shade * 1.35f), Color.Transparent, StormNavy.copy(alpha = shade))
                    )
                )
            )
        }
        content()
    }
}

/** One container, one border. All card content is clipped to a shared safe inset. */
@Composable
fun FramedSurface(
    modifier: Modifier = Modifier,
    accent: Color = AntiqueGold,
    contentPaddingHorizontal: Int = 16,
    contentPaddingVertical: Int = 13,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xF21A1848), Color(0xF20B1735))))
            .border(1.25.dp, accent.copy(alpha = .9f), shape)
            .clipToBounds()
            .padding(horizontal = contentPaddingHorizontal.dp, vertical = contentPaddingVertical.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes backgroundRes: Int? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val background = remember(context, enabled, backgroundRes) {
        ImageBitmap.imageResource(
            context.resources,
            backgroundRes ?: if (enabled) R.drawable.btn_primary_default else R.drawable.btn_primary_disabled,
        )
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .965f else 1f, tween(90), label = "buttonScale")
    Box(
        modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(if (enabled) 1f else .82f)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sourceCap = (background.height / 2).coerceAtMost(background.width / 3)
            val destinationCap = (size.height / 2f).coerceAtMost(size.width / 2f).toInt().coerceAtLeast(1)
            val centerSourceWidth = (background.width - sourceCap * 2).coerceAtLeast(1)
            val centerDestinationWidth = (size.width.toInt() - destinationCap * 2).coerceAtLeast(1)
            drawImage(
                background,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(sourceCap, background.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(destinationCap, size.height.toInt()),
            )
            drawImage(
                background,
                srcOffset = IntOffset(sourceCap, 0),
                srcSize = IntSize(centerSourceWidth, background.height),
                dstOffset = IntOffset(destinationCap, 0),
                dstSize = IntSize(centerDestinationWidth, size.height.toInt()),
            )
            drawImage(
                background,
                srcOffset = IntOffset(background.width - sourceCap, 0),
                srcSize = IntSize(sourceCap, background.height),
                dstOffset = IntOffset(destinationCap + centerDestinationWidth, 0),
                dstSize = IntSize(destinationCap, size.height.toInt()),
            )
        }
        Text(
            label,
            color = when {
                !enabled -> Color(0xFFE1DCE8)
                backgroundRes != null -> StormNavy
                else -> Marble
            },
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 12.5.sp,
            letterSpacing = .55.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun AssetButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(52.dp)
            .graphicsLayer {
                val scale = if (pressed && enabled) .92f else 1f
                scaleX = scale; scaleY = scale
            }
            .alpha(if (enabled) 1f else .5f)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.btn_square), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Image(painterResource(icon), contentDescription, Modifier.fillMaxSize(.58f), contentScale = ContentScale.Fit)
        if (!badge.isNullOrBlank()) {
            Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Danger).border(1.dp, Marble, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
fun SquareButton(
    glyph: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .graphicsLayer {
                val scale = if (pressed && enabled) .92f else 1f
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else .5f)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.btn_square), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Text(
            glyph,
            color = Marble,
            fontFamily = FontFamily.Serif,
            fontSize = if (glyph.length > 1) 18.sp else 24.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        if (!badge.isNullOrBlank()) {
            Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                    .background(Danger).border(1.dp, Marble, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, onHome: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(62.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssetButton(R.drawable.ic_nav_back, "Back", Modifier.size(52.dp), onClick = onBack)
        FramedSurface(Modifier.weight(1f).height(50.dp), contentPaddingHorizontal = 14, contentPaddingVertical = 5) {
            Text(
                title.uppercase(), color = Marble, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black,
                fontSize = 15.sp, letterSpacing = .65.sp, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        AssetButton(R.drawable.ic_nav_home, "Home", Modifier.size(52.dp), onClick = onHome ?: onBack)
    }
}

@Composable
fun CurrencyPill(@DrawableRes icon: Int, value: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(StormNavy.copy(alpha = .9f))
            .border(1.dp, AntiqueGold, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Image(painterResource(icon), null, Modifier.size(24.dp), contentScale = ContentScale.Fit)
        Text(value.toString(), color = Marble, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun UniversalCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    FramedSurface(modifier, content = content)

@Composable
fun LeaderboardCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    FramedSurface(modifier, contentPaddingHorizontal = 14, contentPaddingVertical = 8, content = content)

@Composable
fun PopupPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.panel_popup),
            null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Box(
            Modifier.fillMaxSize().padding(horizontal = 38.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
fun CompactDialogSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FramedSurface(
        modifier.widthIn(max = 340.dp),
        accent = AntiqueGold,
        contentPaddingHorizontal = 18,
        contentPaddingVertical = 16,
        content = content,
    )
}

enum class ExitTarget { PREVIOUS, HOME }

@Composable
fun ExitConfirmationOverlay(
    target: ExitTarget,
    previousLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(StormNavy.copy(alpha = .84f)),
        contentAlignment = Alignment.Center,
    ) {
        CompactDialogSurface(Modifier.fillMaxWidth(.82f).heightIn(min = 228.dp, max = 260.dp)) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("LEAVE THIS RUN?", color = AntiqueGold, fontFamily = FontFamily.Serif, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    if (target == ExitTarget.HOME) "Return to Olympus Home? The current run will end."
                    else "Return to $previousLabel? The current run will end.",
                    color = MutedText, fontSize = 9.5.sp, lineHeight = 13.sp,
                    textAlign = TextAlign.Center, maxLines = 2,
                )
                PrimaryButton("CANCEL", Modifier.fillMaxWidth().height(48.dp), onClick = onCancel)
                PrimaryButton(if (target == ExitTarget.HOME) "GO HOME" else "LEAVE RUN", Modifier.fillMaxWidth().height(48.dp), onClick = onConfirm)
            }
        }
    }
}

@Composable
fun SettingToggle(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val position by animateFloatAsState(
        if (enabled) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "togglePosition",
    )
    val pressScale by animateFloatAsState(if (pressed) .95f else 1f, tween(85), label = "togglePress")
    val onAlpha by animateFloatAsState(if (enabled) 1f else 0f, tween(200), label = "toggleTrackCrossfade")
    Box(
        modifier.size(width = 84.dp, height = 48.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .semantics { stateDescription = if (enabled) "On" else "Off" }
            .toggleable(
                value = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 76.dp, height = 30.dp),
        ) {
            Image(painterResource(R.drawable.toggle_track_off), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Image(painterResource(R.drawable.toggle_track_on), null, Modifier.fillMaxSize().alpha(onAlpha), contentScale = ContentScale.FillBounds)
            Text(
                if (enabled) "ON" else "OFF",
                color = if (enabled) ElectricCyan else Color(0xFFC4BED1),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(if (enabled) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 15.dp),
            )
            Image(
                painterResource(R.drawable.toggle_knob), "Toggle ${if (enabled) "on" else "off"}",
                Modifier.align(Alignment.CenterStart).size(30.dp).graphicsLayer {
                    translationX = position * 46.dp.toPx()
                },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun ApprovedProgress(fraction: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier.height(22.dp).clip(shape).background(StormNavy.copy(alpha = .94f))
            .border(1.25.dp, AntiqueGold.copy(alpha = .9f), shape).padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF9436FF), ElectricCyan)))
        )
    }
}

@Composable
fun BoosterArt(type: BoosterType, modifier: Modifier = Modifier) {
    Image(painterResource(boosterResource(type)), type.title, modifier, contentScale = ContentScale.Fit)
}

@Composable
fun Stars(count: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    Text(
        buildString { repeat(3) { append(if (it < count) '★' else '☆') } },
        color = AntiqueGold, fontSize = if (compact) 13.sp else 25.sp,
        letterSpacing = if (compact) 0.sp else 2.sp, modifier = modifier,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), color = AntiqueGold, fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp, modifier = modifier,
    )
}

@Composable
fun ValueBar(label: String, value: String, modifier: Modifier = Modifier, danger: Boolean = false) {
    Row(
        modifier.height(45.dp).padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = if (danger) Danger else Marble, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

fun boosterResource(type: BoosterType): Int = when (type) {
    BoosterType.THUNDER -> R.drawable.booster_thunder
    BoosterType.SHIELD -> R.drawable.booster_shield
    BoosterType.SLOW_TIME -> R.drawable.booster_slow_time
    BoosterType.CRYSTAL_MAGNET -> R.drawable.booster_crystal_magnet
}

fun blockResource(kind: BlockKind): Int = when (kind) {
    BlockKind.STONE -> R.drawable.block_stone
    BlockKind.MOSS -> R.drawable.block_stone_moss
    BlockKind.GOLD -> R.drawable.block_gold
    BlockKind.STORM -> R.drawable.block_storm
    BlockKind.CRACKED -> R.drawable.block_cracked
}

package com.example.mobilegame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

class FallingShape(
    val startX: Float,
    val startY: Float,
    val fallSpeed: Float,
    val startRot: Float,
    val rotSpeed: Float,
    val color: Color,
    val cells: List<Pair<Int, Int>>,
    val windPhase: Float = Random.nextFloat() * 6.28f
)

@Composable
fun FallingBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    speedMult: Float = 1f
) {
    val layers = remember {
        val colors = GAME_COLORS
        val shapes = listOf(
            listOf(0 to 0, 1 to 0, 0 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
            listOf(0 to 0),
            listOf(0 to 0, 1 to 0, 1 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1)
        )

        val far = List(18) {
            FallingShape(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                fallSpeed = Random.nextFloat() * 0.05f + 0.02f,
                startRot = Random.nextFloat() * 360f,
                rotSpeed = Random.nextFloat() * 20f - 10f,
                color = colors.random(),
                cells = shapes.random()
            )
        }
        val mid = List(12) {
            FallingShape(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                fallSpeed = Random.nextFloat() * 0.14f + 0.05f,
                startRot = Random.nextFloat() * 360f,
                rotSpeed = Random.nextFloat() * 55f - 27f,
                color = colors.random(),
                cells = shapes.random()
            )
        }
        val near = List(6) {
            FallingShape(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                fallSpeed = Random.nextFloat() * 0.28f + 0.16f,
                startRot = Random.nextFloat() * 360f,
                rotSpeed = Random.nextFloat() * 90f - 45f,
                color = colors.random(),
                cells = shapes.random()
            )
        }
        Triple(far, mid, near)
    }

    val currentSpeedMult by rememberUpdatedState(speedMult)
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastFrameTime != 0L) {
                    val deltaSec = (now - lastFrameTime) / 1_000_000_000f
                    time += deltaSec * currentSpeedMult
                    if (time > 10000f) time -= 10000f
                }
                lastFrameTime = now
            }
        }
    }

    Canvas(modifier = modifier) {
        val screenW = size.width
        val screenH = size.height
        val margin = 220f
        val cornerPx = 6.dp.toPx()

        fun drawLayer(
            shapes: List<FallingShape>,
            blockSizePx: Float,
            timeMult: Float,
            windMult: Float,
            alpha: Float
        ) {
            shapes.forEach { shape ->
                val totalY = shape.startY * screenH + time * timeMult * shape.fallSpeed * screenH
                val currentY = (totalY % (screenH + margin * 2)) - margin
                val windOffset = sin(time * 0.5f * timeMult + shape.windPhase) * (100f * windMult)
                val currentX = shape.startX * screenW + windOffset
                val currentRot = shape.startRot + time * timeMult * shape.rotSpeed
                val drawColor = shape.color.copy(alpha = alpha)

                rotate(degrees = currentRot, pivot = Offset(currentX, currentY)) {
                    shape.cells.forEach { (cx, cy) ->
                        drawRoundRect(
                            color = drawColor,
                            topLeft = Offset(currentX + cx * blockSizePx, currentY + cy * blockSizePx),
                            size = Size(blockSizePx - 4f, blockSizePx - 4f),
                            cornerRadius = CornerRadius(cornerPx)
                        )
                    }
                }
            }
        }

        drawLayer(layers.first, 18.dp.toPx(), 1.0f, 0.4f, if (isDarkTheme) 0.10f else 0.15f)
        drawLayer(layers.second, 28.dp.toPx(), 1.8f, 0.7f, if (isDarkTheme) 0.18f else 0.25f)
        drawLayer(layers.third, 42.dp.toPx(), 3.0f, 1.0f, if (isDarkTheme) 0.22f else 0.30f)
    }
}

@Composable
fun SkinBlock(
    color: Color,
    skin: BlockSkin,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
    borderWidth: Dp = 2.dp
) {
    val effectiveColor = if (skin == BlockSkin.MONOCHROME) {
        if (isDarkTheme) Color.White else Color.Black
    } else color

    val radius = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    when (skin) {
        BlockSkin.CLASSIC -> {
            Box(
                modifier
                    .background(effectiveColor, radius)
                    .border(borderWidth, Color.White.copy(alpha = 0.3f), radius)
            )
        }

        BlockSkin.GLOSSY -> {
            Box(
                modifier
                    .background(effectiveColor, radius)
                    .border(borderWidth, Color.White.copy(alpha = 0.4f), radius)
                    .drawWithContent {
                        drawContent()
                        val cornerPx = cornerRadius.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.55f
                            ),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height * 0.55f),
                            cornerRadius = CornerRadius(cornerPx)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
                            size = Size(size.width * 0.64f, size.height * 0.06f),
                            cornerRadius = CornerRadius(size.height * 0.03f)
                        )
                    }
            )
        }

        BlockSkin.MONOCHROME -> {
            val monoBorderColor = if (isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.85f)
            Box(
                modifier
                    .background(effectiveColor, radius)
                    .border(borderWidth, monoBorderColor, radius)
            )
        }

        BlockSkin.CUTE -> {
            val cuteRadius = remember { RoundedCornerShape(percent = 35) }
            Box(
                modifier
                    .clip(cuteRadius)
                    .background(effectiveColor)
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                                center = Offset(size.width * 0.3f, size.height * 0.25f),
                                radius = size.width * 0.55f
                            ),
                            radius = size.width * 0.55f,
                            center = Offset(size.width * 0.3f, size.height * 0.25f)
                        )
                        val eyeRadius = size.width * 0.09f
                        val eyeY = size.height * 0.55f
                        val eyeColor = Color.Black.copy(alpha = 0.75f)
                        drawCircle(eyeColor, eyeRadius, Offset(size.width * 0.32f, eyeY))
                        drawCircle(eyeColor, eyeRadius, Offset(size.width * 0.68f, eyeY))
                        drawCircle(Color.White.copy(alpha = 0.8f), eyeRadius * 0.4f, Offset(size.width * 0.34f, eyeY - eyeRadius * 0.3f))
                        drawCircle(Color.White.copy(alpha = 0.8f), eyeRadius * 0.4f, Offset(size.width * 0.70f, eyeY - eyeRadius * 0.3f))
                    }
            )
        }

        BlockSkin.NEON -> {
            Box(
                modifier
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), effectiveColor.copy(alpha = 0.35f))
                        ),
                        shape = radius
                    )
                    .border(1.5.dp, effectiveColor, radius)
                    .drawWithContent {
                        drawContent()
                        val inset = borderWidth.toPx() + 2f
                        drawRoundRect(
                            color = effectiveColor.copy(alpha = 0.55f),
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = CornerRadius(cornerRadius.toPx() * 0.8f),
                            style = Stroke(width = 2f)
                        )
                    }
            )
        }
    }
}

@Composable
fun ShinyText(text: String, color: Color) {
    val transition = rememberInfiniteTransition(label = "shine_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shine_translate"
    )
    val brush = Brush.linearGradient(
        colors = listOf(color, Color.White, color),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )
    Text(
        text = text,
        style = TextStyle(
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            brush = brush,
            shadow = Shadow(
                color = color.copy(alpha = 0.9f),
                offset = Offset.Zero,
                blurRadius = 40f
            )
        )
    )
}

@Composable
fun ExtrudedShinyText(text: String, color: Color, depth: Int = 6) {
    Box(contentAlignment = Alignment.Center) {
        val darkExtrude = Color(
            red = color.red * 0.20f,
            green = color.green * 0.20f,
            blue = color.blue * 0.20f,
            alpha = 1f
        )
        for (i in depth downTo 1) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = darkExtrude,
                modifier = Modifier.offset(y = (i * 1.6).dp)
            )
        }
        ShinyText(text = text, color = color)
    }
}

@Composable
fun ShinyButton(text: String, containerColor: Color, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn_anim")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "btn_shine_translate"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )

    val shape = RoundedCornerShape(20.dp)
    val darkEdge = Color(
        red = containerColor.red * 0.50f,
        green = containerColor.green * 0.50f,
        blue = containerColor.blue * 0.50f,
        alpha = 1f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(70.dp)
            .graphicsLayer {
                scaleX = breatheScale
                scaleY = breatheScale
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .offset(y = 8.dp)
                .background(darkEdge, shape)
        )

        Button(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .offset(y = pressOffset)
                .clip(shape)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            start = Offset(translateAnim, translateAnim),
                            end = Offset(translateAnim + 220f, translateAnim + 220f)
                        )
                    )
                },
            colors = ButtonDefaults.buttonColors(containerColor = containerColor),
            shape = shape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(2f, 3f),
                        blurRadius = 6f
                    )
                )
            )
        }
    }
}

@Composable
fun PremiumDialog(isDarkTheme: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.padding(24.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))) {
                Box(modifier = Modifier.matchParentSize().background(if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)).blur(25.dp))
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) { content() }
            }
        }
    }
}

@Composable
fun SkinPicker(
    currentSkin: BlockSkin,
    isDarkTheme: Boolean,
    onSkinChange: (BlockSkin) -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val bubbleBg = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f)
    val bubbleBorder = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Скин блоков",
            color = textColor.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bubbleBg, RoundedCornerShape(50))
                .border(1.dp, bubbleBorder, RoundedCornerShape(50))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val all = BlockSkin.values()
                val idx = (currentSkin.ordinal - 1 + all.size) % all.size
                onSkinChange(all[idx])
            }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Предыдущий скин", tint = textColor)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    AnimatedContent(
                        targetState = currentSkin,
                        transitionSpec = {
                            val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (
                                    slideInHorizontally(
                                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                                        initialOffsetX = { it * direction }
                                    ) + fadeIn(tween(250))
                                    ) togetherWith (
                                    slideOutHorizontally(
                                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                                        targetOffsetX = { -it * direction }
                                    ) + fadeOut(tween(250))
                                    )
                        },
                        label = "skin_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { skin ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val previewColor = GAME_COLORS[skin.ordinal % GAME_COLORS.size]
                            Box(modifier = Modifier.size(44.dp)) {
                                SkinBlock(
                                    color = previewColor,
                                    skin = skin,
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 8.dp,
                                    borderWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    currentSkin.displayName,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = {
                val all = BlockSkin.values()
                val idx = (currentSkin.ordinal + 1) % all.size
                onSkinChange(all[idx])
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Следующий скин", tint = textColor)
            }
        }
    }
}

@Composable
fun AppleSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val containerBg = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val selectedBg = if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.White
    val borderCol = if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerBg, RoundedCornerShape(10.dp))
            .border(0.5.dp, borderCol, RoundedCornerShape(10.dp))
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) selectedBg else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    color = if (selected) textColor else textColor.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AppleSwitchRow(
    label: String,
    checked: Boolean,
    isDarkTheme: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textColor, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun AppleSectionLabel(text: String, isDarkTheme: Boolean) {
    Text(
        text,
        color = if (isDarkTheme) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.55f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
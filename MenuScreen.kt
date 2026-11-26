package com.example.mobilegame

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun FallingBackground(modifier: Modifier = Modifier, isDarkTheme: Boolean) {
    val fallingShapes = remember {
        val colors = GAME_COLORS
        val shapes = listOf(
            listOf(0 to 0, 1 to 0, 0 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
            listOf(0 to 0),
            listOf(0 to 0, 1 to 0, 1 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1)
        )
        List(20) {
            FallingShape(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                fallSpeed = Random.nextFloat() * 0.15f + 0.05f,
                startRot = Random.nextFloat() * 360f,
                rotSpeed = Random.nextFloat() * 60f - 30f,
                color = colors.random().copy(alpha = if(isDarkTheme) 0.15f else 0.25f),
                cells = shapes.random()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier) {
        val screenW = size.width
        val screenH = size.height
        val blockSize = 30.dp.toPx()
        val margin = 200f

        fallingShapes.forEach { shape ->
            val totalY = shape.startY * screenH + time * shape.fallSpeed * screenH
            val currentY = (totalY % (screenH + margin * 2)) - margin
            val windOffset = sin(time * 0.5f + shape.windPhase) * 100f
            val currentX = (shape.startX * screenW + windOffset)
            val currentRot = shape.startRot + time * shape.rotSpeed

            rotate(degrees = currentRot, pivot = Offset(currentX, currentY)) {
                shape.cells.forEach { (cx, cy) ->
                    drawRoundRect(
                        color = shape.color,
                        topLeft = Offset(currentX + cx * blockSize, currentY + cy * blockSize),
                        size = Size(blockSize - 4f, blockSize - 4f),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun MenuScreen(
    isDarkTheme: Boolean,
    highScore: Int,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAuthorsClick: () -> Unit,
    onHighScoreClick: () -> Unit // НОВОЕ
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val bgGradient = if (isDarkTheme) {
        Brush.verticalGradient(colors = listOf(Color(0xFF1E1E2C), Color(0xFF0F0F15)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFE8F0F8), Color(0xFFB0C4DE)))
    }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        FallingBackground(modifier = Modifier.fillMaxSize(), isDarkTheme = isDarkTheme)

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = textColor)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShinyText(text = "BLOCK", color = Color(0xFF00E5FF))
            ShinyText(text = "HRYST", color = Color(0xFFFF5252))

            Spacer(modifier = Modifier.height(64.dp))

            ShinyButton(text = "ИГРАТЬ", containerColor = Color(0xFF69F0AE), onClick = onPlayClick)

            Spacer(modifier = Modifier.height(24.dp))

            // КЛИКАБЕЛЬНЫЙ ПУЗЫРЁК С РЕКОРДОМ
            HighScoreBubble(highScore, isDarkTheme, onHighScoreClick)
        }

        TextButton(
            onClick = onAuthorsClick,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            Text("Авторы", color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f), fontSize = 16.sp)
        }
    }
}

@Composable
fun HighScoreBubble(highScore: Int, isDarkTheme: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f), RoundedCornerShape(50))
            .clickable { onClick() } // ДОБАВЛЕН КЛИК
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                .blur(10.dp)
        )
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Кубок",
                tint = Color(0xFFFFD740),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "ЛУЧШИЙ СЧЁТ",
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "$highScore",
                    color = if (isDarkTheme) Color.White else Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// НОВЫЙ ДИАЛОГ СТАТИСТИКИ
@Composable
fun StatsDialog(
    isDarkTheme: Boolean,
    bestScoreRevive: Int,
    bestScoreNoRevive: Int,
    totalRevives: Int,
    totalPlayTime: Long,
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    // Форматируем время (миллисекунды в часы, минуты, секунды)
    val totalSeconds = totalPlayTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val timeString = if (hours > 0) "${hours}ч ${minutes}м ${seconds}с" else if (minutes > 0) "${minutes}м ${seconds}с" else "${seconds}с"

    PremiumDialog(isDarkTheme, onDismiss) {
        Text("Статистика", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Лучший счёт (с возрождением):", color = subTextColor, fontSize = 15.sp)
            Text("$bestScoreRevive", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Лучший счёт (без возрождения):", color = subTextColor, fontSize = 15.sp)
            Text("$bestScoreNoRevive", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Потрачено возрождений:", color = subTextColor, fontSize = 15.sp)
            Text("$totalRevives", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Время в игре:", color = subTextColor, fontSize = 15.sp)
            Text(timeString, color = Color(0xFFFFD740), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(12.dp)) {
            Text("Закрыть", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, brush = brush)
    )
}

@Composable
fun ShinyButton(text: String, containerColor: Color, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "btn_shine_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(3000, delayMillis = 1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "btn_shine_translate"
    )
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(60.dp)
            .clip(shape)
            .drawWithContent {
                drawContent()
                drawRect(brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                    start = Offset(translateAnim, translateAnim),
                    end = Offset(translateAnim + 200f, translateAnim + 200f)
                ))
            },
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = shape
    ) {
        Text(text, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

// --- ДИАЛОГИ ---
@Composable
fun PremiumDialog(isDarkTheme: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.padding(24.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))) {
                Box(modifier = Modifier.matchParentSize().background(if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)).blur(25.dp))
                Column(modifier = Modifier.padding(24.dp)) { content() }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    isRandomColor: Boolean, isDarkTheme: Boolean, noSound: Boolean, isRandomBg: Boolean, isTrayAnimEnabled: Boolean, isFallingBgEnabled: Boolean, currentTrackNumber: Int,
    onRandomColorChange: (Boolean) -> Unit, onThemeChange: (Boolean) -> Unit, onSoundChange: (Boolean) -> Unit,
    onRandomBgChange: (Boolean) -> Unit, onTrayAnimChange: (Boolean) -> Unit, onFallingBgChange: (Boolean) -> Unit, onNextMusicClick: () -> Unit, onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    PremiumDialog(isDarkTheme, onDismiss) {
        Text("Настройки", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Случайные цвета", color = textColor, fontSize = 14.sp)
            Switch(checked = isRandomColor, onCheckedChange = onRandomColorChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Тёмная тема", color = textColor, fontSize = 14.sp)
            Switch(checked = isDarkTheme, onCheckedChange = onThemeChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Без звука", color = textColor, fontSize = 14.sp)
            Switch(checked = noSound, onCheckedChange = onSoundChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Смена фона", color = textColor, fontSize = 14.sp)
            Switch(checked = isRandomBg, onCheckedChange = onRandomBgChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Анимация лотка", color = textColor, fontSize = 14.sp)
            Switch(checked = isTrayAnimEnabled, onCheckedChange = onTrayAnimChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Фон в игре", color = textColor, fontSize = 14.sp)
            Switch(checked = isFallingBgEnabled, onCheckedChange = onFallingBgChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNextMusicClick,
            enabled = !noSound,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFD1D5DB), disabledContainerColor = Color.Gray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            val buttonLabel = if (currentTrackNumber == 0) "Включить музыку" else "Трек ($currentTrackNumber / 5)"
            Text(buttonLabel, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(12.dp)) {
            Text("Закрыть", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AuthorsDialog(isDarkTheme: Boolean, onDismiss: () -> Unit) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray
    PremiumDialog(isDarkTheme, onDismiss) {
        Text("Авторы", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Text("Главные бета тестеры:", color = Color(0xFFFFD740), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Дарья, Алиса, Пашк\n", color = subTextColor, fontSize = 16.sp)
        Text("Создатели:", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("сын моего отца, брат моего брата, Лев, Собака, Мышь, чихуахуа, Алиса, Карась", color = subTextColor, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(12.dp)) {
            Text("Круто", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
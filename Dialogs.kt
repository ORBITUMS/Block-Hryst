package com.example.mobilegame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    isRandomColor: Boolean,
    isDarkTheme: Boolean,
    noSound: Boolean,
    isRandomBg: Boolean,
    isTrayAnimEnabled: Boolean,
    isFallingBgEnabled: Boolean,
    blockSkin: BlockSkin,
    startMode: StartMode,
    isStoryMode: Boolean,
    isEndlessMode: Boolean,
    fallingBgSpeed: Float,
    currentTrackNumber: Int,
    onRandomColorChange: (Boolean) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onRandomBgChange: (Boolean) -> Unit,
    onTrayAnimChange: (Boolean) -> Unit,
    onFallingBgChange: (Boolean) -> Unit,
    onFallingBgSpeedChange: (Float) -> Unit,
    onBlockSkinChange: (BlockSkin) -> Unit,
    onStartModeChange: (StartMode) -> Unit,
    onStoryModeChange: (Boolean) -> Unit,
    onEndlessModeChange: (Boolean) -> Unit,
    onNextMusicClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    var selectedTab by remember { mutableIntStateOf(0) }

    PremiumDialog(isDarkTheme, onDismiss) {
        Text(
            "Настройки",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AppleSegmentedControl(
            options = listOf("Цвет", "Механики", "Кастом"),
            selectedIndex = selectedTab,
            isDarkTheme = isDarkTheme,
            onSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (
                        slideInHorizontally(animationSpec = tween(300)) { it * direction } + fadeIn(tween(300))
                        ) togetherWith (
                        slideOutHorizontally(animationSpec = tween(300)) { -it * direction } + fadeOut(tween(300))
                        )
            },
            label = "settings_tabs",
            modifier = Modifier.fillMaxWidth()
        ) { tab ->
            Column(modifier = Modifier.fillMaxWidth()) {
                when (tab) {
                    0 -> {
                        AppleSectionLabel("Внешний вид", isDarkTheme)
                        AppleSwitchRow("Случайные цвета блоков", isRandomColor, isDarkTheme, onRandomColorChange)
                        AppleSwitchRow("Тёмная тема", isDarkTheme, isDarkTheme, onThemeChange)
                        AppleSwitchRow("Смена цвета фона", isRandomBg, isDarkTheme, onRandomBgChange)
                    }
                    1 -> {
                        AppleSectionLabel("Поведение", isDarkTheme)
                        AppleSwitchRow("Анимация лотка", isTrayAnimEnabled, isDarkTheme, onTrayAnimChange)
                        AppleSwitchRow("Падающие фигуры на фоне", isFallingBgEnabled, isDarkTheme, onFallingBgChange)

                        if (isFallingBgEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Скорость падения", color = textColor, fontSize = 14.sp)
                                Text(
                                    "${"%.1f".format(fallingBgSpeed)}x",
                                    color = Color(0xFF007AFF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = fallingBgSpeed,
                                onValueChange = onFallingBgSpeedChange,
                                valueRange = 0.1f..3f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF007AFF),
                                    activeTrackColor = Color(0xFF007AFF),
                                    inactiveTrackColor = textColor.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                    2 -> {
                        SkinPicker(
                            currentSkin = blockSkin,
                            isDarkTheme = isDarkTheme,
                            onSkinChange = onBlockSkinChange
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.12f)))
                        Spacer(modifier = Modifier.height(14.dp))

                        AppleSectionLabel("Музыка", isDarkTheme)
                        Button(
                            onClick = onNextMusicClick,
                            enabled = !noSound,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val label = if (currentTrackNumber == 0) "Включить музыку"
                            else "Следующий трек ($currentTrackNumber / 5)"
                            Text(label, color = textColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.12f)))
        Spacer(modifier = Modifier.height(14.dp))

        AppleSwitchRow(
            "Звук",
            checked = !noSound,
            isDarkTheme = isDarkTheme,
            onCheckedChange = { onSoundChange(!it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppleSectionLabel("Старт", isDarkTheme)
        AppleSegmentedControl(
            options = StartMode.values().map { it.displayName },
            selectedIndex = startMode.ordinal,
            isDarkTheme = isDarkTheme,
            onSelected = { onStartModeChange(StartMode.values()[it]) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppleSwitchRow(
            "Режим Сюжета",
            isStoryMode,
            isDarkTheme,
            onStoryModeChange
        )

        AppleSwitchRow(
            "Бесконечный режим",
            isEndlessMode,
            isDarkTheme,
            onEndlessModeChange
        )

        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Закрыть", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun StatsDialog(
    isDarkTheme: Boolean,
    bestScoreRevive: Int,
    bestScoreNoRevive: Int,
    bestScoreEndless: Int,
    totalRevives: Int,
    totalPlayTime: Long,
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    val totalSeconds = totalPlayTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val timeString = when {
        hours > 0 -> "${hours}ч ${minutes}м ${seconds}с"
        minutes > 0 -> "${minutes}м ${seconds}с"
        else -> "${seconds}с"
    }

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
            Text("Лучший счёт (бесконечный):", color = subTextColor, fontSize = 15.sp)
            Text("$bestScoreEndless", color = Color(0xFFE040FB), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
fun AuthorsDialog(isDarkTheme: Boolean, onDismiss: () -> Unit) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    val infiniteTransition = rememberInfiniteTransition(label = "authors_pulse")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "authors_breathe"
    )

    PremiumDialog(isDarkTheme, onDismiss) {
        Text(
            "Авторы",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .graphicsLayer {
                    scaleX = breatheScale
                    scaleY = breatheScale
                }
        )
        Text("Главные бета тестеры:", color = Color(0xFFFFD740), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Дарья, Алиса, Пашк\n", color = subTextColor, fontSize = 16.sp)
        Text("Разрабы:", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "брат моего брата, Лев, Карась, чихуахуа, Алиса, XD450",
            color = subTextColor, fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Круто", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    highScore: Int,
    startMode: StartMode,
    isEndlessMode: Boolean,
    isDarkTheme: Boolean,
    hasUsedRevive: Boolean,
    onRestart: () -> Unit,
    onReviveClick: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val modeLabel = buildString {
        append("Режим: ${startMode.displayName}")
        if (isEndlessMode) append(" • Бесконечный")
    }

    PremiumDialog(isDarkTheme, onDismiss = onBackToMenu) {
        Text(
            "ИГРА ОКОНЧЕНА",
            color = Color(0xFFFF5252),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            modeLabel,
            color = textColor.copy(alpha = 0.65f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        if (hasUsedRevive) {
            Text(
                "Был возрождён",
                color = Color(0xFFFFD740),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Твой счет", color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("$score", color = textColor, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(
            text = if (score >= highScore) "Новый рекорд!" else "Рекорд: $highScore",
            color = Color(0xFFFFD740),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onReviveClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ЕЩЁ ОДНА ЖИЗНЬ", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("НАЧАТЬ ЗАНОВО", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
            Text("В МЕНЮ", color = textColor.copy(alpha = 0.7f), fontSize = 16.sp)
        }
    }
}

@Composable
fun ReviveDialog(
    lives: Int,
    isDarkTheme: Boolean,
    promoCodeText: String,
    onPromoCodeChange: (String) -> Unit,
    onActivatePromo: () -> Unit,
    onRevive: () -> Unit,
    onBackToMenu: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val validCodes = listOf("даша", "алиса", "паша", "2026", "777", "8888")
    val isCodeValid = validCodes.contains(promoCodeText.trim().lowercase())

    PremiumDialog(isDarkTheme, onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Возрождение", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.background(Color(0xFFFF5252).copy(alpha = 0.15f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Жизни", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$lives", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = promoCodeText,
            onValueChange = onPromoCodeChange,
            label = { Text("Промокод", color = textColor.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = textColor.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onActivatePromo,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isCodeValid) Color(0xFF69F0AE) else Color(0xFF2C2C2C)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Активировать", color = if (isCodeValid) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRevive,
            enabled = lives > 0,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE), disabledContainerColor = Color.Gray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ВОЗРОДИТЬСЯ", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
            Text("ВЫЙТИ В ГЛАВНОЕ МЕНЮ", color = textColor.copy(alpha = 0.7f), fontSize = 16.sp)
        }
    }
}
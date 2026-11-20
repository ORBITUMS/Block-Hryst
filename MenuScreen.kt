package com.example.mobilegame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MenuScreen(isDarkTheme: Boolean, onPlayClick: () -> Unit, onSettingsClick: () -> Unit, onAuthorsClick: () -> Unit) {
    val btnBgColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFD1D5DB)
    val btnTextColor = if (isDarkTheme) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("BLOCK", color = Color(0xFF00E5FF), fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("BLAST", color = Color(0xFFFF5252), fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)

            Spacer(modifier = Modifier.height(64.dp))

            Button(onClick = onPlayClick, modifier = Modifier.fillMaxWidth(0.7f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)), shape = RoundedCornerShape(16.dp)) {
                Text("ИГРАТЬ", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth(0.7f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = btnBgColor), shape = RoundedCornerShape(16.dp)) {
                Text("НАСТРОЙКИ", color = btnTextColor, fontSize = 18.sp)
            }
        }

        TextButton(onClick = onAuthorsClick, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            Text("Авторы", color = if (isDarkTheme) Color(0xFF444444) else Color(0xFFAAAAAA), fontSize = 16.sp)
        }
    }
}

@Composable
fun PremiumDialog(isDarkTheme: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f))
                        .blur(25.dp)
                )
                Column(modifier = Modifier.padding(24.dp)) {
                    content()
                }
            }
        }
    }
}
@Composable
fun SettingsDialog(
    isRandomColor: Boolean, isDarkTheme: Boolean, noSound: Boolean, isRandomBg: Boolean, isTrayAnimEnabled: Boolean, currentTrackNumber: Int,
    onRandomColorChange: (Boolean) -> Unit, onThemeChange: (Boolean) -> Unit, onSoundChange: (Boolean) -> Unit,
    onRandomBgChange: (Boolean) -> Unit, onTrayAnimChange: (Boolean) -> Unit, onNextMusicClick: () -> Unit, onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    PremiumDialog(isDarkTheme, onDismiss) {
        Text("Настройки", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Случайные цвета", color = textColor, fontSize = 16.sp)
            Switch(checked = isRandomColor, onCheckedChange = onRandomColorChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Тёмная тема", color = textColor, fontSize = 16.sp)
            Switch(checked = isDarkTheme, onCheckedChange = onThemeChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Без звука", color = textColor, fontSize = 16.sp)
            Switch(checked = noSound, onCheckedChange = onSoundChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Смена фона", color = textColor, fontSize = 16.sp)
            Switch(checked = isRandomBg, onCheckedChange = onRandomBgChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Анимация лотка", color = textColor, fontSize = 16.sp)
            Switch(checked = isTrayAnimEnabled, onCheckedChange = onTrayAnimChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNextMusicClick,
            enabled = !noSound,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFD1D5DB),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            val buttonLabel = if (currentTrackNumber == 0) "Включить музыку" else "Следующий трек ($currentTrackNumber / 5)"
            Text(buttonLabel, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
        Text("Утка 1, Утка 2, Лев, Собака, Мышь, чихуахуа, крокодил, Карась", color = subTextColor, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(12.dp)) {
            Text("Круто", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
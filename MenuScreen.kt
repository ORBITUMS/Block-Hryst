package com.example.mobilegame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    isDarkTheme: Boolean,
    highScore: Int,
    isFallingBgEnabled: Boolean,
    fallingBgSpeed: Float,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAuthorsClick: () -> Unit,
    onHighScoreClick: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val bgGradient = if (isDarkTheme) {
        Brush.verticalGradient(colors = listOf(Color(0xFF1E1E2C), Color(0xFF0F0F15)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFE8F0F8), Color(0xFFB0C4DE)))
    }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        if (isFallingBgEnabled) {
            FallingBackground(
                modifier = Modifier.fillMaxSize(),
                isDarkTheme = isDarkTheme,
                speedMult = fallingBgSpeed
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = if (isDarkTheme) 0.65f else 0.35f)
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = maxOf(size.width, size.height) * 0.72f
                        )
                    )
                }
        )

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-32).dp)
            ) {
                Box(modifier = Modifier.offset(y = (-12).dp)) {
                    ExtrudedShinyText(text = "BLOCK", color = Color(0xFF00E5FF))
                }
                ExtrudedShinyText(text = "HRYST", color = Color(0xFFFF5252))
            }

            Spacer(modifier = Modifier.height(56.dp))

            ShinyButton(text = "ИГРАТЬ", containerColor = Color(0xFF69F0AE), onClick = onPlayClick)

            Spacer(modifier = Modifier.height(24.dp))

            HighScoreBubble(highScore, isDarkTheme, onHighScoreClick)
        }

        TextButton(
            onClick = onAuthorsClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Авторы",
                color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun HighScoreBubble(highScore: Int, isDarkTheme: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f), RoundedCornerShape(50))
            .clickable { onClick() }
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
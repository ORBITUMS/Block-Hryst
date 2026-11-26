package com.example.mobilegame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

val GAME_COLORS = listOf(
    Color(0xFF00E5FF), Color(0xFFFF5252), Color(0xFFFFD740),
    Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFFFF4081), Color(0xFF448AFF)
)

data class Piece(
    val id: Int = Random.nextInt(),
    val shape: List<List<Int>>,
    val color: Color
) {
    val rows: Int get() = shape.size
    val cols: Int get() = shape[0].size
}

data class FloatingScore(
    val id: Long = System.nanoTime(),
    val text: String,
    val x: Float,
    val y: Float
)

val cellSizeDp = 36.dp
val spacingDp = 4.dp

enum class BlockSkin(val displayName: String) {
    CLASSIC("Классика"),
    GLOSSY("Глянец"),
    MONOCHROME("Монохром"),
    CUTE("Милашки"),
    NEON("Неон")
}

enum class StartMode(val displayName: String) {
    CLASSIC("Классика"),
    ARCADE("Аркада")
}

enum class Screen { MENU, GAME }

data class SavedGame(
    val board: Array<Array<Color?>>,
    val score: Int,
    val tray: List<Piece?>,
    val hasUsedRevive: Boolean
)
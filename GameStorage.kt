package com.example.mobilegame

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class GameStorage(context: Context) {
    private val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    // --- Настройки ---
    var isRandomColor: Boolean
        get() = prefs.getBoolean("random_color", true)
        set(value) = prefs.edit().putBoolean("random_color", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var noSound: Boolean
        get() = prefs.getBoolean("no_sound", false)
        set(value) = prefs.edit().putBoolean("no_sound", value).apply()

    var isRandomBg: Boolean
        get() = prefs.getBoolean("random_bg", true)
        set(value) = prefs.edit().putBoolean("random_bg", value).apply()

    var isTrayAnimEnabled: Boolean
        get() = prefs.getBoolean("tray_anim", true)
        set(value) = prefs.edit().putBoolean("tray_anim", value).apply()

    var isFallingBgEnabled: Boolean
        get() = prefs.getBoolean("falling_bg", false)
        set(value) = prefs.edit().putBoolean("falling_bg", value).apply()

    var fallingBgSpeed: Float
        get() = prefs.getFloat("falling_bg_speed", 1f)
        set(value) = prefs.edit().putFloat("falling_bg_speed", value).apply()

    var blockSkin: BlockSkin
        get() = runCatching {
            BlockSkin.valueOf(prefs.getString("block_skin", "CLASSIC") ?: "CLASSIC")
        }.getOrDefault(BlockSkin.CLASSIC)
        set(value) = prefs.edit().putString("block_skin", value.name).apply()

    var startMode: StartMode
        get() = runCatching {
            StartMode.valueOf(prefs.getString("start_mode", "CLASSIC") ?: "CLASSIC")
        }.getOrDefault(StartMode.CLASSIC)
        set(value) = prefs.edit()
            .putString("start_mode", value.name)
            .putBoolean("has_saved_game", false)
            .putBoolean("has_used_revive", false)
            .apply()

    var isStoryMode: Boolean
        get() = prefs.getBoolean("is_story_mode", false)
        set(value) = prefs.edit()
            .putBoolean("is_story_mode", value)
            .putBoolean("has_saved_game", false)
            .putBoolean("has_used_revive", false)
            .apply()

    var isEndlessMode: Boolean
        get() = prefs.getBoolean("is_endless_mode", false)
        set(value) = prefs.edit()
            .putBoolean("is_endless_mode", value)
            .putBoolean("has_saved_game", false)
            .putBoolean("has_used_revive", false)
            .apply()

    // --- Статистика и жизни ---
    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) = prefs.edit().putInt("high_score", value).apply()

    var bestScoreRevive: Int
        get() = prefs.getInt("best_score_revive", 0)
        set(value) = prefs.edit().putInt("best_score_revive", value).apply()

    var bestScoreNoRevive: Int
        get() = prefs.getInt("best_score_no_revive", 0)
        set(value) = prefs.edit().putInt("best_score_no_revive", value).apply()

    var bestScoreEndless: Int
        get() = prefs.getInt("best_score_endless", 0)
        set(value) = prefs.edit().putInt("best_score_endless", value).apply()

    var totalRevivesSpent: Int
        get() = prefs.getInt("total_revives_spent", 0)
        set(value) = prefs.edit().putInt("total_revives_spent", value).apply()

    var totalPlayTime: Long
        get() = prefs.getLong("total_play_time", 0L)
        set(value) = prefs.edit().putLong("total_play_time", value).apply()

    var lives: Int
        get() = prefs.getInt("lives", 0)
        set(value) = prefs.edit().putInt("lives", value).apply()

    // --- Сохранение / загрузка партии ---
    fun clearSavedGame() {
        prefs.edit()
            .putBoolean("has_saved_game", false)
            .putBoolean("has_used_revive", false)
            .apply()
    }

    fun saveGameState(
        board: Array<Array<Color?>>,
        score: Int,
        trayPieces: List<Piece?>,
        isGameOver: Boolean,
        hasUsedRevive: Boolean
    ) {
        if (isGameOver) {
            clearSavedGame()
            return
        }
        val boardStr = board.joinToString(";") { row ->
            row.joinToString(",") { it?.toArgb()?.toString() ?: "null" }
        }
        val trayStr = trayPieces.joinToString(";") { piece ->
            if (piece == null) "null"
            else {
                val shapeStr = piece.shape.joinToString("|") { it.joinToString(",") }
                "$shapeStr:${piece.color.toArgb()}"
            }
        }
        prefs.edit()
            .putBoolean("has_saved_game", true)
            .putBoolean("has_used_revive", hasUsedRevive)
            .putInt("saved_score", score)
            .putString("saved_board", boardStr)
            .putString("saved_tray", trayStr)
            .apply()
    }

    fun loadGameState(): SavedGame? {
        if (!prefs.getBoolean("has_saved_game", false)) return null
        val boardStr = prefs.getString("saved_board", null) ?: return null
        val trayStr = prefs.getString("saved_tray", null) ?: return null
        val score = prefs.getInt("saved_score", 0)
        val hasUsedRevive = prefs.getBoolean("has_used_revive", false)

        val board = Array(8) { arrayOfNulls<Color>(8) }
        return try {
            val rows = boardStr.split(";")
            for (r in 0 until 8) {
                val cols = rows[r].split(",")
                for (c in 0 until 8) {
                    if (cols[c] != "null") board[r][c] = Color(cols[c].toInt())
                }
            }
            val trayPieces = mutableListOf<Piece?>()
            if (trayStr.isNotEmpty()) {
                val pieces = trayStr.split(";")
                for (p in pieces) {
                    if (p == "null") trayPieces.add(null)
                    else {
                        val parts = p.split(":")
                        val shape = parts[0].split("|").map { row -> row.split(",").map { it.toInt() } }
                        val color = Color(parts[1].toInt())
                        trayPieces.add(Piece(shape = shape, color = color))
                    }
                }
            }
            SavedGame(board, score, trayPieces, hasUsedRevive)
        } catch (e: Exception) {
            null
        }
    }
}
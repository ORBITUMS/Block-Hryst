package com.example.mobilegame

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppRouter()
            }
        }
    }
}

val GAME_COLORS = listOf(
    Color(0xFF00E5FF), Color(0xFFFF5252), Color(0xFFFFD740),
    Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFFFF4081), Color(0xFF448AFF)
)

data class Piece(val id: Int = Random.nextInt(), val shape: List<List<Int>>, val color: Color) {
    val rows = shape.size
    val cols = shape[0].size
}

data class FloatingScore(val id: Long = System.nanoTime(), val text: String, val x: Float, val y: Float)

val cellSizeDp = 36.dp
val spacingDp = 4.dp

enum class Screen { MENU, GAME }

@Composable
fun MainAppRouter() {
    var currentScreen by remember { mutableStateOf(Screen.MENU) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRandomColor by remember { mutableStateOf(prefs.getBoolean("random_color", true)) }
    var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
    var noSound by remember { mutableStateOf(prefs.getBoolean("no_sound", false)) }
    var isRandomBg by remember { mutableStateOf(prefs.getBoolean("random_bg", true)) }
    var isTrayAnimEnabled by remember { mutableStateOf(prefs.getBoolean("tray_anim", true)) }
    var currentTrackIndex by remember { mutableIntStateOf(-1) }

    var showSettings by remember { mutableStateOf(false) }
    var showAuthors by remember { mutableStateOf(false) }

    val allMusicResources = listOf(R.raw.music_bg1, R.raw.music_bg2, R.raw.music_bg3, R.raw.music_bg4, R.raw.music_bg5)
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var musicLockoutUntil by remember { mutableLongStateOf(0L) }
    var triggeredMilestones by remember { mutableStateOf(setOf<Int>()) }

    fun stopMusic() {
        currentPlayer?.release()
        currentPlayer = null
    }

    fun playNextTrackInCircle() {
        if (noSound) return
        stopMusic()
        currentTrackIndex = (currentTrackIndex + 1) % allMusicResources.size
        try {
            val resId = allMusicResources[currentTrackIndex]
            currentPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                start()
            }
            musicLockoutUntil = System.currentTimeMillis() + 5 * 60 * 1000
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun checkAndPlayMusic(currentScore: Int) {
        if (noSound) return
        val milestones = listOf(50, 100, 200, 300, 400)
        val newlyReached = milestones.filter { currentScore >= it && !triggeredMilestones.contains(it) }

        if (newlyReached.isNotEmpty()) {
            triggeredMilestones = triggeredMilestones + newlyReached
            if (currentPlayer?.isPlaying != true && Random.nextFloat() <= 0.8f && System.currentTimeMillis() > musicLockoutUntil) {
                playNextTrackInCircle()
            }
        }
    }

    LaunchedEffect(noSound) {
        if (noSound) {
            currentPlayer?.pause()
        } else {
            if (currentPlayer != null && !currentPlayer!!.isPlaying) {
                currentPlayer?.start()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                currentPlayer?.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (currentPlayer != null && !noSound) currentPlayer?.start()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentPlayer?.release()
        }
    }

    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFE8F0F8)

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Crossfade(targetState = currentScreen, animationSpec = tween(500), label = "screen_transition") { screen ->
            when (screen) {
                Screen.MENU -> {
                    MenuScreen(
                        isDarkTheme = isDarkTheme,
                        onPlayClick = { currentScreen = Screen.GAME },
                        onSettingsClick = { showSettings = true },
                        onAuthorsClick = { showAuthors = true }
                    )
                }
                Screen.GAME -> {
                    BlockBlastGame(
                        isRandomColor = isRandomColor,
                        isDarkTheme = isDarkTheme,
                        isRandomBg = isRandomBg,
                        isTrayAnimEnabled = isTrayAnimEnabled,
                        onScoreUpdated = { newScore -> checkAndPlayMusic(newScore) },
                        onGameReset = { triggeredMilestones = emptySet() },
                        onBackToMenu = { currentScreen = Screen.MENU }
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            isRandomColor = isRandomColor, isDarkTheme = isDarkTheme, noSound = noSound, isRandomBg = isRandomBg,
            isTrayAnimEnabled = isTrayAnimEnabled,
            currentTrackNumber = if (currentTrackIndex == -1) 0 else currentTrackIndex + 1,
            onRandomColorChange = { newValue -> isRandomColor = newValue; prefs.edit().putBoolean("random_color", newValue).apply() },
            onThemeChange = { newValue -> isDarkTheme = newValue; prefs.edit().putBoolean("dark_theme", newValue).apply() },
            onSoundChange = { newValue -> noSound = newValue; prefs.edit().putBoolean("no_sound", newValue).apply() },
            onRandomBgChange = { newValue -> isRandomBg = newValue; prefs.edit().putBoolean("random_bg", newValue).apply() },
            onTrayAnimChange = { newValue -> isTrayAnimEnabled = newValue; prefs.edit().putBoolean("tray_anim", newValue).apply() },
            onNextMusicClick = { playNextTrackInCircle() },
            onDismiss = { showSettings = false }
        )
    }

    if (showAuthors) {
        AuthorsDialog(isDarkTheme = isDarkTheme, onDismiss = { showAuthors = false })
    }
}

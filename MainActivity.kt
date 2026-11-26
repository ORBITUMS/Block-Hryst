package com.example.mobilegame

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

@Composable
fun MainAppRouter() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val storage = remember { GameStorage(context) }
    val musicManager = remember { MusicManager(context) }

    var currentScreen by remember { mutableStateOf(Screen.MENU) }

    var isRandomColor by remember { mutableStateOf(storage.isRandomColor) }
    var isDarkTheme by remember { mutableStateOf(storage.isDarkTheme) }
    var noSound by remember { mutableStateOf(storage.noSound) }
    var isRandomBg by remember { mutableStateOf(storage.isRandomBg) }
    var isTrayAnimEnabled by remember { mutableStateOf(storage.isTrayAnimEnabled) }
    var isFallingBgEnabled by remember { mutableStateOf(storage.isFallingBgEnabled) }
    var fallingBgSpeed by remember { mutableFloatStateOf(storage.fallingBgSpeed) }
    var blockSkin by remember { mutableStateOf(storage.blockSkin) }
    var startMode by remember { mutableStateOf(storage.startMode) }
    var isStoryMode by remember { mutableStateOf(storage.isStoryMode) }
    var isEndlessMode by remember { mutableStateOf(storage.isEndlessMode) }

    // Временные переопределения для Режима Сюжета
    var storySkin by remember { mutableStateOf<BlockSkin?>(null) }
    var storyRandomColors by remember { mutableStateOf<Boolean?>(null) }
    var storySpeedMult by remember { mutableFloatStateOf(1f) }
    var storyBgSignal by remember { mutableLongStateOf(0L) }
    var lastStoryMilestone by remember { mutableIntStateOf(0) }

    val effectiveSkin = if (isStoryMode) storySkin ?: blockSkin else blockSkin
    val effectiveRandomColor = if (isStoryMode) storyRandomColors ?: isRandomColor else isRandomColor
    val effectiveFallingSpeedMult = if (isStoryMode) fallingBgSpeed * storySpeedMult else fallingBgSpeed

    var showSettings by remember { mutableStateOf(false) }
    var showAuthors by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    val highScore = remember(currentScreen, showStats) { storage.highScore }
    val bestScoreRevive = remember(showStats) { storage.bestScoreRevive }
    val bestScoreNoRevive = remember(showStats) { storage.bestScoreNoRevive }
    val bestScoreEndless = remember(showStats) { storage.bestScoreEndless }
    val totalRevives = remember(showStats) { storage.totalRevivesSpent }
    val totalPlayTime = remember(showStats) { storage.totalPlayTime }

    fun resetStoryOverride() {
        storySkin = null
        storyRandomColors = null
        storySpeedMult = 1f
        storyBgSignal = 0L
        lastStoryMilestone = 0
    }

    LaunchedEffect(noSound) {
        if (noSound) musicManager.pause() else musicManager.resume(noSound = false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) musicManager.pause()
            else if (event == Lifecycle.Event.ON_RESUME) musicManager.resume(noSound)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            musicManager.stop()
        }
    }

    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFE8F0F8)

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Crossfade(targetState = currentScreen, animationSpec = tween(500), label = "screen_transition") { screen ->
            when (screen) {
                Screen.MENU -> {
                    MenuScreen(
                        isDarkTheme = isDarkTheme,
                        highScore = highScore,
                        isFallingBgEnabled = isFallingBgEnabled,
                        fallingBgSpeed = fallingBgSpeed,
                        onPlayClick = {
                            resetStoryOverride()
                            currentScreen = Screen.GAME
                        },
                        onSettingsClick = { showSettings = true },
                        onAuthorsClick = { showAuthors = true },
                        onHighScoreClick = { showStats = true }
                    )
                }
                Screen.GAME -> {
                    BlockBlastGame(
                        storage = storage,
                        isRandomColor = effectiveRandomColor,
                        isDarkTheme = isDarkTheme,
                        isRandomBg = isRandomBg,
                        isTrayAnimEnabled = isTrayAnimEnabled,
                        isFallingBgEnabled = isFallingBgEnabled,
                        noSound = noSound,
                        blockSkin = effectiveSkin,
                        startMode = startMode,
                        isEndlessMode = isEndlessMode,
                        fallingBgSpeedMult = effectiveFallingSpeedMult,
                        storyBgSignal = storyBgSignal,
                        onToggleSound = {
                            noSound = !noSound
                            storage.noSound = noSound
                        },
                        onScoreUpdated = { newScore ->
                            musicManager.checkAndPlayForScore(newScore, noSound)
                            if (isStoryMode && newScore >= 50) {
                                val currentMilestone = newScore / 50
                                while (lastStoryMilestone < currentMilestone) {
                                    lastStoryMilestone++
                                    val skins = BlockSkin.values()
                                    storySkin = skins[lastStoryMilestone % skins.size]
                                    storyRandomColors = !(storyRandomColors ?: isRandomColor)
                                    storySpeedMult = 0.5f + Random.nextFloat() * 1.5f
                                    storyBgSignal++
                                }
                            }
                        },
                        onGameReset = {
                            musicManager.resetMilestones()
                            resetStoryOverride()
                        },
                        onBackToMenu = { currentScreen = Screen.MENU }
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            isRandomColor = isRandomColor,
            isDarkTheme = isDarkTheme,
            noSound = noSound,
            isRandomBg = isRandomBg,
            isTrayAnimEnabled = isTrayAnimEnabled,
            isFallingBgEnabled = isFallingBgEnabled,
            blockSkin = blockSkin,
            startMode = startMode,
            isStoryMode = isStoryMode,
            isEndlessMode = isEndlessMode,
            fallingBgSpeed = fallingBgSpeed,
            currentTrackNumber = musicManager.currentTrackNumber,
            onRandomColorChange = { v -> isRandomColor = v; storage.isRandomColor = v },
            onThemeChange = { v -> isDarkTheme = v; storage.isDarkTheme = v },
            onSoundChange = { v -> noSound = v; storage.noSound = v },
            onRandomBgChange = { v -> isRandomBg = v; storage.isRandomBg = v },
            onTrayAnimChange = { v -> isTrayAnimEnabled = v; storage.isTrayAnimEnabled = v },
            onFallingBgChange = { v -> isFallingBgEnabled = v; storage.isFallingBgEnabled = v },
            onFallingBgSpeedChange = { v -> fallingBgSpeed = v; storage.fallingBgSpeed = v },
            onBlockSkinChange = { v -> blockSkin = v; storage.blockSkin = v },
            onStartModeChange = { v -> startMode = v; storage.startMode = v },
            onStoryModeChange = { v -> isStoryMode = v; storage.isStoryMode = v },
            onEndlessModeChange = { v -> isEndlessMode = v; storage.isEndlessMode = v },
            onNextMusicClick = { musicManager.playNextTrack(noSound) },
            onDismiss = { showSettings = false }
        )
    }

    if (showAuthors) {
        AuthorsDialog(isDarkTheme = isDarkTheme, onDismiss = { showAuthors = false })
    }

    if (showStats) {
        StatsDialog(
            isDarkTheme = isDarkTheme,
            bestScoreRevive = bestScoreRevive,
            bestScoreNoRevive = bestScoreNoRevive,
            bestScoreEndless = bestScoreEndless,
            totalRevives = totalRevives,
            totalPlayTime = totalPlayTime,
            onDismiss = { showStats = false }
        )
    }
}
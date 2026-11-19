package com.example.mobilegame

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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

// --- ГЛОБАЛЬНЫЕ ДАННЫЕ ---
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

// --- НАВИГАЦИЯ И ТЕМА ---
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

    var showSettings by remember { mutableStateOf(false) }
    var showAuthors by remember { mutableStateOf(false) }

    // --- МУЗЫКА ---
    val musicResources = listOf(R.raw.music_bg1, R.raw.music_bg2, R.raw.music_bg3, R.raw.music_bg4, R.raw.music_bg5)
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var musicLockoutUntil by remember { mutableLongStateOf(0L) }
    var triggeredMilestones by remember { mutableStateOf(setOf<Int>()) }

    fun checkAndPlayMusic(currentScore: Int) {
        if (noSound) return

        val milestones = listOf(50, 100, 200, 300, 400)
        val newlyReached = milestones.filter { currentScore >= it && !triggeredMilestones.contains(it) }

        if (newlyReached.isNotEmpty()) {
            triggeredMilestones = triggeredMilestones + newlyReached

            if (currentPlayer?.isPlaying != true && Random.nextFloat() <= 0.8f && System.currentTimeMillis() > musicLockoutUntil) {
                currentPlayer?.release()
                try {
                    val resId = musicResources.random()
                    currentPlayer = MediaPlayer.create(context, resId)?.apply {
                        isLooping = true
                        start()
                    }
                    musicLockoutUntil = System.currentTimeMillis() + 5 * 60 * 1000
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    LaunchedEffect(noSound) {
        if (noSound) {
            if (currentPlayer?.isPlaying == true) currentPlayer?.pause()
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
            onRandomColorChange = { newValue -> isRandomColor = newValue; prefs.edit().putBoolean("random_color", newValue).apply() },
            onThemeChange = { newValue -> isDarkTheme = newValue; prefs.edit().putBoolean("dark_theme", newValue).apply() },
            onSoundChange = { newValue -> noSound = newValue; prefs.edit().putBoolean("no_sound", newValue).apply() },
            onRandomBgChange = { newValue -> isRandomBg = newValue; prefs.edit().putBoolean("random_bg", newValue).apply() },
            onDismiss = { showSettings = false }
        )
    }

    if (showAuthors) {
        AuthorsDialog(isDarkTheme = isDarkTheme, onDismiss = { showAuthors = false })
    }
}


// --- ЭКРАНЫ МЕНЮ И ДИАЛОГИ ---

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
    isRandomColor: Boolean, isDarkTheme: Boolean, noSound: Boolean, isRandomBg: Boolean,
    onRandomColorChange: (Boolean) -> Unit, onThemeChange: (Boolean) -> Unit, onSoundChange: (Boolean) -> Unit, onRandomBgChange: (Boolean) -> Unit, onDismiss: () -> Unit
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

// --- ИГРОВОЙ ЭКРАН ---
@Composable
fun BlockBlastGame(
    isRandomColor: Boolean,
    isDarkTheme: Boolean,
    isRandomBg: Boolean,
    onScoreUpdated: (Int) -> Unit,
    onGameReset: () -> Unit,
    onBackToMenu: () -> Unit
) {

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)

    val savedState = remember { loadGameState(context) }
    val boardSize = 8

    var board by remember {
        mutableStateOf(
            savedState?.first ?: Array(boardSize) { arrayOfNulls<Color>(boardSize) })
    }
    var score by remember { mutableIntStateOf(savedState?.second ?: 0) }
    var trayPieces by remember {
        mutableStateOf(
            savedState?.third ?: generateTray(
                GAME_COLORS,
                64
            )
        )
    }

    var highScore by remember { mutableIntStateOf(prefs.getInt("high_score", 0)) }
    var isGameOver by remember { mutableStateOf(false) }

    var lives by remember { mutableIntStateOf(prefs.getInt("lives", 0)) }
    var showReviveDialog by remember { mutableStateOf(false) }
    var promoCodeText by remember { mutableStateOf("") }

    var draggedPiece by remember { mutableStateOf<Piece?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }
    var snappedTopLeft by remember { mutableStateOf<Offset?>(null) }
    var animatingCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var floatingScores by remember { mutableStateOf(emptyList<FloatingScore>()) }
    var droppingPiece by remember { mutableStateOf<Piece?>(null) }
    var droppingPos by remember { mutableStateOf(Offset.Zero) }
    var droppingGrabOffset by remember { mutableStateOf(Offset.Zero) }


    // Смена фона
    val bgColors = listOf(
        if (isDarkTheme) Color(0xFF121212) else Color(0xFFE8F0F8),
        if (isDarkTheme) Color(0xFF1A1C29) else Color(0xFFE0E5EC),
        if (isDarkTheme) Color(0xFF1C1A1C) else Color(0xFFECE0E0),
        if (isDarkTheme) Color(0xFF0E1A18) else Color(0xFFE0ECE5),
        if (isDarkTheme) Color(0xFF211818) else Color(0xFFF5E6E6)
    )
    var currentBg by remember { mutableStateOf(bgColors[0]) }

    val density = LocalDensity.current
    val stepPx = with(density) { (cellSizeDp + spacingDp).toPx() }
    val liftOffsetPx = with(density) { 160.dp.toPx() }

    var previewRow by remember { mutableIntStateOf(-1) }
    var previewCol by remember { mutableIntStateOf(-1) }
    var isPreviewValid by remember { mutableStateOf(false) }

    LaunchedEffect(score) {
        if (score > 0 && isRandomBg && score >= 50 && (score % 50 < 8)) {
            currentBg = bgColors.random()
        }
        onScoreUpdated(score)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                saveGameState(context, board, score, trayPieces, isGameOver)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            saveGameState(context, board, score, trayPieces, isGameOver)
        }
    }


    fun resetGame() {
        board = Array(boardSize) { arrayOfNulls(boardSize) }
        score = 0
        trayPieces = generateTray(GAME_COLORS, 64)
        isGameOver = false
        showReviveDialog = false
        onGameReset()
        prefs.edit().putBoolean("has_saved_game", false).apply()
    }

    fun revive() {
        if (lives > 0) {
            lives--
            prefs.edit().putInt("lives", lives).apply()
            val newBoard = board.map { it.clone() }.toTypedArray()
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (Random.nextBoolean()) newBoard[r][c] = null
                }
            }
            board = newBoard
            trayPieces = generateTray(GAME_COLORS, 64)
            isGameOver = false
            showReviveDialog = false
            prefs.edit().putBoolean("has_saved_game", false).apply()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(currentBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя панель
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            saveGameState(
                                context,
                                board,
                                score,
                                trayPieces,
                                isGameOver
                            ); onBackToMenu()
                        },
                        modifier = Modifier.background(
                            if (isDarkTheme) Color(0xFF2C2C2C) else Color(
                                0xFFD1D5DB
                            ), shape = CircleShape
                        ).size(48.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Меню", tint = textColor)
                    }

                    IconButton(
                        onClick = { resetGame() },
                        modifier = Modifier.background(
                            if (isDarkTheme) Color(0xFF2C2C2C) else Color(
                                0xFFD1D5DB
                            ), shape = CircleShape
                        ).size(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ресет", tint = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                CombinedScoreBubble(score = score, highScore = highScore, isDarkTheme = isDarkTheme)
            }

            GameBoard(
                board = board,
                previewRow = previewRow,
                previewCol = previewCol,
                isPreviewValid = isPreviewValid,
                draggedPiece = draggedPiece,
                animatingCells = animatingCells,
                onGridPositioned = { offset -> gridOrigin = offset }
            )

            // Лоток с плавным перестроением и центрированием фигур
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFE4E7EB),
                        RoundedCornerShape(16.dp)
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = false // Отключаем ручную прокрутку, чтобы не мешала перетаскиванию
            ) {
                items(
                    count = trayPieces.size,
                    key = { index -> trayPieces[index].id }
                ) { index ->
                    val piece = trayPieces[index]
                    var boxPos by remember { mutableStateOf(Offset.Zero) }
                    val isBeingDragged = draggedPiece?.id == piece.id

                    val itemScale by animateFloatAsState(
                        targetValue = if (isBeingDragged) 0.75f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                        label = "tray_item_scale"
                    )
                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isBeingDragged) 0.25f else 1f,
                        animationSpec = tween(250),
                        label = "tray_item_alpha"
                    )

                    Box(
                        modifier = Modifier
                            // animateItem плавно двигает оставшиеся фигуры на новые места и центрирует последнюю
                            .animateItem(
                                fadeInSpec = tween(300),
                                placementSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = Spring.StiffnessLow
                                ),
                                fadeOutSpec = tween(200)
                            )
                            .width(105.dp)
                            .fillMaxHeight()
                            .onGloballyPositioned { boxPos = it.positionInRoot() }
                            .pointerInput(piece) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        val absTouch = boxPos + localOffset
                                        val pw = piece.cols * cellSizeDp.value * 2
                                        val ph = piece.rows * cellSizeDp.value * 2
                                        val grab = Offset(pw / 2f, ph / 2f)
                                        draggedPiece = piece
                                        dragPosition = absTouch - Offset(0f, liftOffsetPx)
                                        grabOffset = grab
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragPosition += dragAmount
                                        draggedPiece?.let { currentPiece ->
                                            val topLeftX = dragPosition.x - grabOffset.x
                                            val topLeftY = dragPosition.y - grabOffset.y
                                            val relativeX = topLeftX - gridOrigin.x
                                            val relativeY = topLeftY - gridOrigin.y

                                            val col = (relativeX / stepPx).roundToInt()
                                            val row = (relativeY / stepPx).roundToInt()

                                            previewRow = row
                                            previewCol = col
                                            isPreviewValid =
                                                row != -1 && col != -1 && canPlacePiece(
                                                    board,
                                                    currentPiece,
                                                    row,
                                                    col
                                                )

                                            if (isPreviewValid) snappedTopLeft = Offset(
                                                gridOrigin.x + col * stepPx,
                                                gridOrigin.y + row * stepPx
                                            )
                                            else snappedTopLeft = null
                                        }
                                    },
                                    onDragEnd = {
                                        val p = draggedPiece
                                        val targetRow = previewRow
                                        val targetCol = previewCol
                                        val valid = isPreviewValid
                                        val currentDragPos = dragPosition
                                        val currentGrab = grabOffset

                                        // Мгновенно освобождаем палец, чтобы сразу можно было брать следующую фигуру!
                                        draggedPiece = null
                                        previewRow = -1
                                        previewCol = -1
                                        snappedTopLeft = null

                                        if (valid && p != null) {
                                            // Сразу убираем фигуру из лотка и генерируем новую тройку, если лоток опустел
                                            val tempBoardForTray = board.map { it.clone() }.toTypedArray()
                                            placePiece(tempBoardForTray, p, targetRow, targetCol)
                                            val freeCells = tempBoardForTray.flatten().count { it == null }

                                            val newTray = trayPieces.filter { it.id != p.id }
                                            trayPieces = if (newTray.isEmpty()) {
                                                generateTray(GAME_COLORS, freeCells)
                                            } else {
                                                newTray
                                            }

                                            // Запускаем независимый полет фигуры к целевым клеткам
                                            val targetTopLeft = Offset(
                                                gridOrigin.x + targetCol * stepPx,
                                                gridOrigin.y + targetRow * stepPx
                                            )
                                            val startTopLeft = Offset(
                                                currentDragPos.x - currentGrab.x,
                                                currentDragPos.y - currentGrab.y
                                            )

                                            droppingPiece = p
                                            droppingGrabOffset = currentGrab
                                            droppingPos = currentDragPos

                                            scope.launch {
                                                val animX = Animatable(startTopLeft.x)
                                                val animY = Animatable(startTopLeft.y)

                                                val jobX = launch {
                                                    animX.animateTo(
                                                        targetTopLeft.x,
                                                        tween(durationMillis = 130, easing = FastOutSlowInEasing)
                                                    ) {
                                                        droppingPos = Offset(value + currentGrab.x, droppingPos.y)
                                                    }
                                                }
                                                val jobY = launch {
                                                    animY.animateTo(
                                                        targetTopLeft.y,
                                                        tween(durationMillis = 130, easing = FastOutSlowInEasing)
                                                    ) {
                                                        droppingPos = Offset(droppingPos.x, value + currentGrab.y)
                                                    }
                                                }
                                                jobX.join()
                                                jobY.join()

                                                // Фигура долетела — фиксируем её на актуальной доске
                                                droppingPiece = null
                                                val actualBoard = board.map { it.clone() }.toTypedArray()
                                                placePiece(actualBoard, p, targetRow, targetCol)

                                                val (clearedBoard, linesCleared, clearedCoords) = clearLinesWithCoords(actualBoard)

                                                val addedScore = linesCleared * 8
                                                score += addedScore
                                                if (score > highScore) {
                                                    highScore = score
                                                    prefs.edit().putInt("high_score", highScore).apply()
                                                }

                                                if (linesCleared > 0) {
                                                    board = actualBoard
                                                    animatingCells = clearedCoords

                                                    if (clearedCoords.isNotEmpty()) {
                                                        val avgR = clearedCoords.map { it.first }.average()
                                                        val avgC = clearedCoords.map { it.second }.average()
                                                        val px = (gridOrigin.x + (avgC * stepPx)).toFloat()
                                                        val py = (gridOrigin.y + (avgR * stepPx)).toFloat()
                                                        floatingScores = floatingScores + FloatingScore(
                                                            text = "+$addedScore",
                                                            x = px,
                                                            y = py
                                                        )
                                                    }

                                                    delay(350)
                                                    board = changeBoardColors(clearedBoard, isRandomColor)
                                                    animatingCells = emptySet()

                                                    delay(650)
                                                    if (!canAnyPieceBePlaced(board, trayPieces)) {
                                                        isGameOver = true
                                                        prefs.edit().putBoolean("has_saved_game", false).apply()
                                                    }
                                                } else {
                                                    board = actualBoard
                                                    if (!canAnyPieceBePlaced(board, trayPieces)) {
                                                        isGameOver = true
                                                        prefs.edit().putBoolean("has_saved_game", false).apply()
                                                    }
                                                }

                                                saveGameState(context, board, score, trayPieces, isGameOver)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        draggedPiece = null
                                        previewRow = -1
                                        previewCol = -1
                                        snappedTopLeft = null
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemAlpha
                            }
                        ) {
                            AnimatedPieceWrapper(piece)
                        }
                    }
                }
            }
        } // <--- Закрывает основной Column, чтобы экран больше не подпрыгивал!

    // Отрисовка блока, который вы держите пальцем
    draggedPiece?.let { piece ->
        DraggedPieceOverlay(piece, dragPosition, grabOffset, snappedTopLeft)
    }

    // Отрисовка летящей фигуры поверх всего экрана
    droppingPiece?.let { piece ->
        DraggedPieceOverlay(piece, droppingPos, droppingGrabOffset, null)
    }

        // Летающие очки (с анимацией закручивания)
        floatingScores.forEach { fs ->
            key(fs.id) {
                var offsetY by remember { mutableFloatStateOf(0f) }
                var alpha by remember { mutableFloatStateOf(1f) }
                var scale by remember { mutableFloatStateOf(1f) }
                var rotation by remember { mutableFloatStateOf(0f) }

                LaunchedEffect(Unit) {
                    val animY = Animatable(0f)
                    val animAlpha = Animatable(1f)
                    val animScale = Animatable(1f)
                    val animRot = Animatable(0f)

                    launch { animY.animateTo(-150f, tween(1000, easing = LinearEasing)) { offsetY = value } }
                    launch { animAlpha.animateTo(0f, tween(800)) { alpha = value } }
                    launch { animScale.animateTo(0f, tween(800)) { scale = value } }
                    launch { animRot.animateTo(180f, tween(800)) { rotation = value } }

                    delay(1000)
                    floatingScores = floatingScores.filter { it.id != fs.id }
                }

                Text(
                    text = fs.text,
                    color = Color(0xFFFFD740).copy(alpha = alpha),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .offset { IntOffset(fs.x.roundToInt(), (fs.y + offsetY).roundToInt()) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                        .zIndex(20f)
                )
            }
        }

        if (isGameOver) {
            GameOverDialog(
                score = score,
                highScore = highScore,
                isDarkTheme = isDarkTheme,
                onRestart = { resetGame() },
                onReviveClick = { showReviveDialog = true },
                onBackToMenu = { onBackToMenu() }
            )
        }

        if (showReviveDialog) {
            ReviveDialog(
                lives = lives,
                isDarkTheme = isDarkTheme,
                promoCodeText = promoCodeText,
                onPromoCodeChange = { promoCodeText = it },
                onActivatePromo = {
                    val code = promoCodeText.trim().lowercase()
                    when (code) {
                        "даша" -> lives += 3
                        "алиса" -> lives += 3
                        "паша" -> lives += 3
                        "2026" -> lives += 2
                        "777" -> lives += 3
                        "8888" -> lives += 5
                    }
                    prefs.edit().putInt("lives", lives).apply()
                    promoCodeText = ""
                },
                onRevive = { revive() },
                onBackToMenu = { showReviveDialog = false; onBackToMenu() },
                onDismiss = { showReviveDialog = false }
            )
        }
    }
}

@Composable
fun AnimatedPieceWrapper(piece: Piece) {
    val scale = remember { Animatable(0.3f) }
    val rotation = remember { Animatable(-45f) }

    LaunchedEffect(piece.id) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            rotationZ = rotation.value
        }
    ) {
        DraggablePieceVisual(piece)
    }
}


@Composable
fun CombinedScoreBubble(score: Int, highScore: Int, isDarkTheme: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                .blur(15.dp)
        )

        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Счет", color = if (isDarkTheme) Color.LightGray else Color.DarkGray, fontSize = 12.sp)
                AnimatedScore(targetScore = score, textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A))
            }

            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f)))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Рекорд", color = if (isDarkTheme) Color.LightGray else Color.DarkGray, fontSize = 12.sp)
                Text("$highScore", color = Color(0xFFFFD740), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnimatedScore(targetScore: Int, textColor: Color) {
    var displayScore by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetScore) {
        if (displayScore < targetScore) {
            val diff = targetScore - displayScore
            val step = (diff / 15f).coerceAtLeast(1f)
            while (displayScore < targetScore) {
                displayScore = (displayScore + step.toInt()).coerceAtMost(targetScore)
                delay(25)
            }
        } else {
            displayScore = targetScore
        }
    }
    Text("$displayScore", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun GameOverDialog(score: Int, highScore: Int, isDarkTheme: Boolean, onRestart: () -> Unit, onReviveClick: () -> Unit, onBackToMenu: () -> Unit) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    PremiumDialog(isDarkTheme, onDismiss = onBackToMenu) {
        Text("ИГРА ОКОНЧЕНА", color = Color(0xFFFF5252), fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Твой счет", color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("$score", color = textColor, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(
            text = if (score >= highScore) "Новый рекорд!" else "Рекорд: $highScore",
            color = Color(0xFFFFD740), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onReviveClick, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)), shape = RoundedCornerShape(12.dp)) {
            Text("ВТОРАЯ ЖИЗНЬ", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(12.dp)) {
            Text("НАЧАТЬ ЗАНОВО", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("В МЕНЮ", color = textColor.copy(alpha = 0.7f), fontSize = 16.sp) }
    }
}

@Composable
fun ReviveDialog(
    lives: Int, isDarkTheme: Boolean, promoCodeText: String,
    onPromoCodeChange: (String) -> Unit, onActivatePromo: () -> Unit,
    onRevive: () -> Unit, onBackToMenu: () -> Unit, onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    PremiumDialog(isDarkTheme, onDismiss) {
        Text("Возрождение", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Text("Осталось жизней: $lives", color = Color(0xFFFFD740), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = promoCodeText, onValueChange = onPromoCodeChange, label = { Text("Промокод", color = textColor.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = textColor.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onActivatePromo, modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)), shape = RoundedCornerShape(8.dp)) {
            Text("Активировать", color = Color.White, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRevive, enabled = lives > 0, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE), disabledContainerColor = Color.Gray), shape = RoundedCornerShape(12.dp)) {
            Text("ВОЗРОДИТЬСЯ", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
            Text("ВЫЙТИ В ГЛАВНОЕ МЕНЮ", color = textColor.copy(alpha = 0.7f), fontSize = 16.sp)
        }
    }
}

// --- СОХРАНЕНИЕ И ЗАГРУЗКА ---
fun saveGameState(context: Context, board: Array<Array<Color?>>, score: Int, trayPieces: List<Piece>, isGameOver: Boolean) {
    val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    if (isGameOver) { prefs.edit().putBoolean("has_saved_game", false).apply(); return }
    val boardStr = board.joinToString(";") { row -> row.joinToString(",") { it?.toArgb()?.toString() ?: "null" } }
    val trayStr = trayPieces.joinToString(";") { piece -> val shapeStr = piece.shape.joinToString("|") { it.joinToString(",") }; "$shapeStr:${piece.color.toArgb()}" }
    prefs.edit().putBoolean("has_saved_game", true).putInt("saved_score", score).putString("saved_board", boardStr).putString("saved_tray", trayStr).apply()
}

fun loadGameState(context: Context): Triple<Array<Array<Color?>>, Int, List<Piece>>? {
    val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("has_saved_game", false)) return null
    val boardStr = prefs.getString("saved_board", null) ?: return null
    val trayStr = prefs.getString("saved_tray", null) ?: return null
    val score = prefs.getInt("saved_score", 0)
    val board = Array(8) { arrayOfNulls<Color>(8) }
    try {
        val rows = boardStr.split(";")
        for (r in 0 until 8) {
            val cols = rows[r].split(",")
            for (c in 0 until 8) { if (cols[c] != "null") board[r][c] = Color(cols[c].toInt()) }
        }
        val trayPieces = mutableListOf<Piece>()
        if (trayStr.isNotEmpty()) {
            val pieces = trayStr.split(";")
            for (p in pieces) {
                val parts = p.split(":")
                val shapeRows = parts[0].split("|")
                val shape = shapeRows.map { row -> row.split(",").map { it.toInt() } }
                val color = Color(parts[1].toInt())
                trayPieces.add(Piece(shape = shape, color = color))
            }
        }
        return Triple(board, score, trayPieces)
    } catch (e: Exception) { return null }
}

// --- ЛОГИКА ---
fun changeBoardColors(board: Array<Array<Color?>>, isRandom: Boolean): Array<Array<Color?>> {
    val newBoard = Array(8) { arrayOfNulls<Color>(8) }
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val currentColor = board[r][c]
            if (currentColor != null) {
                if (isRandom) newBoard[r][c] = GAME_COLORS.random()
                else {
                    val currentIndex = GAME_COLORS.indexOf(currentColor)
                    newBoard[r][c] = if (currentIndex != -1) GAME_COLORS[(currentIndex + 1) % GAME_COLORS.size] else GAME_COLORS.random()
                }
            }
        }
    }
    return newBoard
}

fun clearLinesWithCoords(board: Array<Array<Color?>>): Triple<Array<Array<Color?>>, Int, Set<Pair<Int, Int>>> {
    var cleared = 0
    val newBoard = Array(8) { arrayOfNulls<Color>(8) }
    for (r in 0 until 8) for (c in 0 until 8) newBoard[r][c] = board[r][c]
    val clearedCoords = mutableSetOf<Pair<Int, Int>>()
    for (r in 0 until 8) {
        if (newBoard[r].all { it != null }) {
            for (c in 0 until 8) clearedCoords.add(Pair(r, c))
            newBoard[r] = arrayOfNulls(8)
            cleared++
        }
    }
    for (c in 0 until 8) {
        if ((0 until 8).all { newBoard[it][c] != null }) {
            for (r in 0 until 8) clearedCoords.add(Pair(r, c))
            for (r in 0 until 8) newBoard[r][c] = null
            cleared++
        }
    }
    return Triple(newBoard, cleared, clearedCoords)
}

fun canAnyPieceBePlaced(board: Array<Array<Color?>>, pieces: List<Piece>): Boolean {
    for (piece in pieces) {
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                if (canPlacePiece(board, piece, r, c)) return true
            }
        }
    }
    return false
}

fun generateTray(colors: List<Color>, freeCells: Int): List<Piece> {
    val shapes = mutableListOf(
        listOf(listOf(1)), listOf(listOf(1, 1)), listOf(listOf(1), listOf(1)),
        listOf(listOf(1, 1, 1)), listOf(listOf(1), listOf(1), listOf(1)),
        listOf(listOf(1, 1, 1, 1)), listOf(listOf(1), listOf(1), listOf(1), listOf(1)),
        listOf(listOf(1, 1), listOf(1, 1)),

        // Новые диагональные фигуры 2x2:
        listOf(listOf(0, 1), listOf(1, 0)), // Диагональ снизу вверх (/)
        listOf(listOf(1, 0), listOf(0, 1)), // Диагональ сверху вниз (\)

        listOf(listOf(1, 1, 1), listOf(0, 1, 0)),
        listOf(listOf(0, 1, 0), listOf(1, 1, 1)), listOf(listOf(1, 0), listOf(1, 1), listOf(1, 0)),
        listOf(listOf(0, 1), listOf(1, 1), listOf(0, 1)), listOf(listOf(1, 1, 1), listOf(1, 0, 0)),
        listOf(listOf(1, 1, 1), listOf(0, 0, 1)), listOf(listOf(1, 0), listOf(1, 0), listOf(1, 1)),
        listOf(listOf(0, 1), listOf(0, 1), listOf(1, 1)), listOf(listOf(1, 1), listOf(1, 0), listOf(1, 0)),
        listOf(listOf(1, 1), listOf(0, 1), listOf(0, 1)), listOf(listOf(1, 1, 0), listOf(0, 1, 1)),
        listOf(listOf(0, 1, 1), listOf(1, 1, 0)), listOf(listOf(1, 0), listOf(1, 1), listOf(0, 1)),
        listOf(listOf(0, 1), listOf(1, 1), listOf(1, 0)), listOf(listOf(1, 1, 1), listOf(1, 1, 1), listOf(1, 1, 1))
    )


    if (freeCells < 32) {
        shapes.add(listOf(listOf(1)))
        shapes.add(listOf(listOf(1)))
        shapes.add(listOf(listOf(1)))
    }

    return List(3) { Piece(shape = shapes.random(), color = colors.random()) }
}

fun canPlacePiece(board: Array<Array<Color?>>, piece: Piece, row: Int, col: Int): Boolean {
    if (row < 0 || col < 0 || row + piece.rows > 8 || col + piece.cols > 8) return false
    for (r in 0 until piece.rows) {
        for (c in 0 until piece.cols) {
            if (piece.shape[r][c] == 1 && board[row + r][col + c] != null) return false
        }
    }
    return true
}

fun placePiece(board: Array<Array<Color?>>, piece: Piece, row: Int, col: Int) {
    for (r in 0 until piece.rows) {
        for (c in 0 until piece.cols) {
            if (piece.shape[r][c] == 1) board[row + r][col + c] = piece.color
        }
    }
}

// --- UI КОМПОНЕНТЫ ---

@Composable
fun GameBoard(
    board: Array<Array<Color?>>, previewRow: Int, previewCol: Int, isPreviewValid: Boolean,
    draggedPiece: Piece?, animatingCells: Set<Pair<Int, Int>>, onGridPositioned: (Offset) -> Unit
) {
    Box(
        modifier = Modifier.background(Color(0xFF000000), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(8.dp)
    ) {
        Column(modifier = Modifier.onGloballyPositioned { onGridPositioned(it.positionInRoot()) }, verticalArrangement = Arrangement.spacedBy(spacingDp)) {
            for (r in 0 until 8) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacingDp)) {
                    for (c in 0 until 8) {
                        val cellColor = board[r][c]
                        val isPreview = draggedPiece != null && isCellInPreview(r, c, previewRow, previewCol, draggedPiece)
                        val isClearing = animatingCells.contains(Pair(r, c))

                        Box(modifier = Modifier.size(cellSizeDp)) {
                            EmptyCell(size = cellSizeDp)
                            if (cellColor != null) {
                                BoardCell(color = cellColor, isClearing = isClearing)
                            } else if (isPreview && isPreviewValid) {
                                BoardCell(color = draggedPiece!!.color.copy(alpha = 0.4f), isClearing = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isCellInPreview(r: Int, c: Int, pr: Int, pc: Int, piece: Piece): Boolean {
    if (pr == -1 || pc == -1) return false
    val relR = r - pr
    val relC = c - pc
    return relR in 0 until piece.rows && relC in 0 until piece.cols && piece.shape[relR][relC] == 1
}

@Composable
fun EmptyCell(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).background(Color(0xFF121212), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp)))
}

@Composable
fun BoardCell(color: Color, isClearing: Boolean) {
    val animatedColor by androidx.compose.animation.animateColorAsState(targetValue = color, animationSpec = tween(400), label = "color")
    val scale by animateFloatAsState(targetValue = if (isClearing) 0f else 1f, animationSpec = tween(350, easing = FastOutSlowInEasing), label = "scale")
    val rotation by animateFloatAsState(targetValue = if (isClearing) 90f else 0f, animationSpec = tween(350), label = "rotation")

    Box(
        modifier = Modifier.size(cellSizeDp)
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
            .background(animatedColor, RoundedCornerShape(6.dp))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    )
}

@Composable
fun DraggablePieceVisual(piece: Piece) {
    val previewCellSize = 24.dp
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (r in 0 until piece.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until piece.cols) {
                    if (piece.shape[r][c] == 1) {
                        Box(Modifier.size(previewCellSize).background(piece.color, RoundedCornerShape(4.dp)).border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
                    } else {
                        Spacer(modifier = Modifier.size(previewCellSize))
                    }
                }
            }
        }
    }
}
@Composable
fun DraggedPieceOverlay(piece: Piece, dragPosition: Offset, grabOffset: Offset, snappedTopLeft: Offset?) {
    val renderX = dragPosition.x - grabOffset.x
    val renderY = dragPosition.y - grabOffset.y

    Column(
        modifier = Modifier.offset { IntOffset(renderX.roundToInt(), renderY.roundToInt()) }.zIndex(10f),
        verticalArrangement = Arrangement.spacedBy(spacingDp)
    ) {
        for (r in 0 until piece.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacingDp)) {
                for (c in 0 until piece.cols) {
                    if (piece.shape[r][c] == 1) {
                        Box(Modifier.size(cellSizeDp).background(piece.color, RoundedCornerShape(6.dp)).border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)))
                    } else {
                        Spacer(modifier = Modifier.size(cellSizeDp))
                    }
                }
            }
        }
    }
}

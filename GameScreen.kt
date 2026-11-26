package com.example.mobilegame

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun BlockBlastGame(
    storage: GameStorage,
    isRandomColor: Boolean,
    isDarkTheme: Boolean,
    isRandomBg: Boolean,
    isTrayAnimEnabled: Boolean,
    isFallingBgEnabled: Boolean,
    noSound: Boolean,
    blockSkin: BlockSkin,
    startMode: StartMode,
    isEndlessMode: Boolean,
    fallingBgSpeedMult: Float,
    storyBgSignal: Long,
    onToggleSound: () -> Unit,
    onScoreUpdated: (Int) -> Unit,
    onGameReset: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)

    val savedState = remember { storage.loadGameState() }
    val boardSize = 8

    val initialBoard = remember {
        savedState?.board ?: if (startMode == StartMode.ARCADE) {
            generateArcadeBoard()
        } else {
            Array(boardSize) { arrayOfNulls<Color>(boardSize) }
        }
    }
    val initialTray = remember {
        savedState?.tray ?: if (isEndlessMode || startMode == StartMode.ARCADE) {
            generateSolvableTray(initialBoard)
        } else {
            generateTray(GAME_COLORS, 64)
        }
    }

    var board by remember { mutableStateOf(initialBoard) }
    var score by remember { mutableIntStateOf(savedState?.score ?: 0) }
    var trayPieces by remember { mutableStateOf(initialTray) }

    var highScore by remember { mutableIntStateOf(storage.highScore) }
    var isGameOver by remember { mutableStateOf(false) }
    var hasUsedRevive by remember { mutableStateOf(savedState?.hasUsedRevive ?: false) }

    var lives by remember { mutableIntStateOf(storage.lives) }
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

    var sessionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastActionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val bgColors = remember(isDarkTheme) {
        if (isDarkTheme) {
            listOf(Color(0xFF121212), Color(0xFF1A1C29), Color(0xFF1C1A1C), Color(0xFF0E1A18), Color(0xFF211818))
        } else {
            listOf(Color(0xFFE8F0F8), Color(0xFFE0E5EC), Color(0xFFECE0E0), Color(0xFFE0ECE5), Color(0xFFF5E6E6))
        }
    }
    var currentBgIndex by remember { mutableIntStateOf(0) }
    var lastBgMilestone by remember { mutableIntStateOf((savedState?.score ?: 0) / 50) }
    val currentBg = bgColors[currentBgIndex % bgColors.size]

    val density = LocalDensity.current
    val stepPx = with(density) { (cellSizeDp + spacingDp).toPx() }
    val spacingPx = with(density) { spacingDp.toPx() }
    val liftOffsetPx = with(density) { 160.dp.toPx() }

    var previewRow by remember { mutableIntStateOf(-1) }
    var previewCol by remember { mutableIntStateOf(-1) }
    var isPreviewValid by remember { mutableStateOf(false) }

    val animatedSpeedMult by animateFloatAsState(
        targetValue = fallingBgSpeedMult,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label = "bg_speed_anim"
    )

    LaunchedEffect(score) {
        val milestone = score / 50
        if (isRandomBg && milestone > lastBgMilestone) {
            lastBgMilestone = milestone
            currentBgIndex = (currentBgIndex + Random.nextInt(1, bgColors.size)) % bgColors.size
        }
        onScoreUpdated(score)
    }

    LaunchedEffect(storyBgSignal) {
        if (storyBgSignal > 0L) {
            currentBgIndex = (currentBgIndex + Random.nextInt(1, bgColors.size)) % bgColors.size
        }
    }

    fun updatePlayTime() {
        val now = System.currentTimeMillis()
        val activeEndTime = minOf(now, lastActionTime + 60000L)
        val activeDuration = activeEndTime - sessionStartTime
        if (activeDuration > 0) {
            storage.totalPlayTime += activeDuration
        }
        sessionStartTime = now
        lastActionTime = now
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                storage.saveGameState(board, score, trayPieces, isGameOver, hasUsedRevive)
                updatePlayTime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            storage.saveGameState(board, score, trayPieces, isGameOver, hasUsedRevive)
            updatePlayTime()
        }
    }

    fun resetGame() {
        if (startMode == StartMode.ARCADE) {
            val newBoard = generateArcadeBoard()
            board = newBoard
            trayPieces = generateSolvableTray(newBoard)
        } else {
            val newBoard = Array(boardSize) { arrayOfNulls<Color>(boardSize) }
            board = newBoard
            trayPieces = if (isEndlessMode) generateSolvableTray(newBoard) else generateTray(GAME_COLORS, 64)
        }
        score = 0
        lastBgMilestone = 0
        isGameOver = false
        showReviveDialog = false
        hasUsedRevive = false
        storage.clearSavedGame()
        onGameReset()
    }

    fun revive() {
        if (lives > 0) {
            lives--
            storage.lives = lives
            storage.totalRevivesSpent += 1

            val newBoard = board.map { it.clone() }.toTypedArray()
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (Random.nextBoolean()) newBoard[r][c] = null
                }
            }
            board = newBoard
            trayPieces = generateSolvableTray(newBoard)
            isGameOver = false
            showReviveDialog = false
            hasUsedRevive = true
            storage.saveGameState(board, score, trayPieces, isGameOver = false, hasUsedRevive = true)
        }
    }

    fun saveStatsOnGameOver() {
        if (isEndlessMode) {
            if (score > storage.bestScoreEndless) storage.bestScoreEndless = score
        } else if (hasUsedRevive) {
            if (score > storage.bestScoreRevive) storage.bestScoreRevive = score
        } else {
            if (score > storage.bestScoreNoRevive) storage.bestScoreNoRevive = score
        }
    }

    fun finalizePlacement(p: Piece, targetRow: Int, targetCol: Int) {
        val actualBoard = board.map { it.clone() }.toTypedArray()
        placePiece(actualBoard, p, targetRow, targetCol)
        val (clearedBoard, linesCleared, clearedCoords) = clearLinesWithCoords(actualBoard)

        val addedScore = clearedCoords.size
        score += addedScore

        if (score > highScore) {
            highScore = score
            storage.highScore = highScore
        }

        if (linesCleared > 0) {
            board = actualBoard
            animatingCells = clearedCoords

            if (clearedCoords.isNotEmpty()) {
                val avgR = clearedCoords.map { it.first }.average()
                val avgC = clearedCoords.map { it.second }.average()
                val px = (gridOrigin.x + (avgC * stepPx)).toFloat()
                val py = (gridOrigin.y + (avgR * stepPx)).toFloat()
                floatingScores = floatingScores + FloatingScore(text = "+$addedScore", x = px, y = py)
            }

            scope.launch {
                delay(350)
                board = changeBoardColors(clearedBoard, isRandomColor)
                animatingCells = emptySet()
                delay(650)
                if (!canAnyPieceBePlaced(board, trayPieces)) {
                    isGameOver = true
                    saveStatsOnGameOver()
                }
                storage.saveGameState(board, score, trayPieces, isGameOver, hasUsedRevive)
            }
        } else {
            board = actualBoard
            if (!canAnyPieceBePlaced(board, trayPieces)) {
                isGameOver = true
                saveStatsOnGameOver()
            }
            storage.saveGameState(board, score, trayPieces, isGameOver, hasUsedRevive)
        }
    }

    fun executePlacement(p: Piece, targetRow: Int, targetCol: Int, currentDragPos: Offset, currentGrab: Offset) {
        lastActionTime = System.currentTimeMillis()

        val tempBoardForTray = board.map { it.clone() }.toTypedArray()
        placePiece(tempBoardForTray, p, targetRow, targetCol)
        val (boardAfterClear, _, _) = clearLinesWithCoords(tempBoardForTray)
        val freeCells = boardAfterClear.flatten().count { it == null }

        val mutableTray = trayPieces.toMutableList()
        val pIndex = mutableTray.indexOfFirst { it?.id == p.id }
        if (pIndex != -1) mutableTray[pIndex] = null

        trayPieces = if (mutableTray.all { it == null }) {
            if (isEndlessMode) generateSolvableTray(boardAfterClear) else generateTray(GAME_COLORS, freeCells)
        } else {
            mutableTray
        }

        val targetTopLeft = Offset(gridOrigin.x + targetCol * stepPx, gridOrigin.y + targetRow * stepPx)
        val startTopLeft = Offset(currentDragPos.x - currentGrab.x, currentDragPos.y - currentGrab.y)

        droppingPiece = p
        droppingGrabOffset = currentGrab
        droppingPos = currentDragPos

        scope.launch {
            val animX = Animatable(startTopLeft.x)
            val animY = Animatable(startTopLeft.y)
            val jobX = launch {
                animX.animateTo(targetTopLeft.x, tween(130, easing = FastOutSlowInEasing)) {
                    droppingPos = Offset(value + currentGrab.x, droppingPos.y)
                }
            }
            val jobY = launch {
                animY.animateTo(targetTopLeft.y, tween(130, easing = FastOutSlowInEasing)) {
                    droppingPos = Offset(droppingPos.x, value + currentGrab.y)
                }
            }
            jobX.join()
            jobY.join()
            droppingPiece = null
            finalizePlacement(p, targetRow, targetCol)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(currentBg)) {
        if (isFallingBgEnabled) {
            FallingBackground(
                modifier = Modifier.fillMaxSize(),
                isDarkTheme = isDarkTheme,
                speedMult = animatedSpeedMult
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            storage.saveGameState(board, score, trayPieces, isGameOver, hasUsedRevive)
                            onBackToMenu()
                        },
                        modifier = Modifier.background(if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFD1D5DB), shape = CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Меню", tint = textColor)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onToggleSound,
                            modifier = Modifier.background(if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFD1D5DB), shape = CircleShape).size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (noSound) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Звук",
                                tint = textColor
                            )
                        }
                        IconButton(
                            onClick = { resetGame() },
                            modifier = Modifier.background(if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFD1D5DB), shape = CircleShape).size(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Ресет", tint = textColor)
                        }
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
                onGridPositioned = { offset -> gridOrigin = offset },
                skin = blockSkin,
                isDarkTheme = isDarkTheme
            )

            val itemsToShow = if (isTrayAnimEnabled) trayPieces.filterNotNull() else trayPieces

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFE4E7EB), RoundedCornerShape(16.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = false
            ) {
                items(count = itemsToShow.size, key = { index -> itemsToShow[index]?.id ?: "empty_$index" }) { index ->
                    val piece = itemsToShow[index]
                    if (piece == null) {
                        Spacer(modifier = Modifier.width(105.dp).fillMaxHeight())
                    } else {
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
                        val animModifier = if (isTrayAnimEnabled) {
                            Modifier.animateItem(
                                fadeInSpec = tween(300),
                                placementSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
                                fadeOutSpec = tween(200)
                            )
                        } else Modifier

                        Box(
                            modifier = animModifier
                                .width(105.dp)
                                .fillMaxHeight()
                                .onGloballyPositioned { boxPos = it.positionInRoot() }
                                .pointerInput(piece, blockSkin, isDarkTheme) {
                                    detectDragGestures(
                                        onDragStart = { localOffset ->
                                            val absTouch = boxPos + localOffset
                                            val pw = piece.cols * stepPx - spacingPx
                                            val ph = piece.rows * stepPx - spacingPx
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
                                                val col = ((topLeftX - gridOrigin.x) / stepPx).roundToInt()
                                                val row = ((topLeftY - gridOrigin.y) / stepPx).roundToInt()

                                                previewRow = row
                                                previewCol = col
                                                isPreviewValid = row != -1 && col != -1 && canPlacePiece(board, currentPiece, row, col)
                                                snappedTopLeft = if (isPreviewValid) {
                                                    Offset(gridOrigin.x + col * stepPx, gridOrigin.y + row * stepPx)
                                                } else null
                                            }
                                        },
                                        onDragEnd = {
                                            val p = draggedPiece
                                            val targetRow = previewRow
                                            val targetCol = previewCol
                                            val valid = isPreviewValid
                                            val currentDragPos = dragPosition
                                            val currentGrab = grabOffset

                                            draggedPiece = null
                                            previewRow = -1
                                            previewCol = -1
                                            snappedTopLeft = null

                                            if (valid && p != null) {
                                                executePlacement(p, targetRow, targetCol, currentDragPos, currentGrab)
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
                                AnimatedPieceWrapper(piece, blockSkin, isDarkTheme)
                            }
                        }
                    }
                }
            }
        }

        draggedPiece?.let { piece -> DraggedPieceOverlay(piece, dragPosition, grabOffset, snappedTopLeft, blockSkin, isDarkTheme) }
        droppingPiece?.let { piece -> DraggedPieceOverlay(piece, droppingPos, droppingGrabOffset, null, blockSkin, isDarkTheme) }

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
                startMode = startMode,
                isEndlessMode = isEndlessMode,
                isDarkTheme = isDarkTheme,
                hasUsedRevive = hasUsedRevive,
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
                    storage.lives = lives
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
fun GameBoard(
    board: Array<Array<Color?>>,
    previewRow: Int,
    previewCol: Int,
    isPreviewValid: Boolean,
    draggedPiece: Piece?,
    animatingCells: Set<Pair<Int, Int>>,
    onGridPositioned: (Offset) -> Unit,
    skin: BlockSkin,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF000000), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.onGloballyPositioned { onGridPositioned(it.positionInRoot()) },
            verticalArrangement = Arrangement.spacedBy(spacingDp)
        ) {
            for (r in 0 until 8) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacingDp)) {
                    for (c in 0 until 8) {
                        val cellColor = board[r][c]
                        val isPreview = draggedPiece != null && isCellInPreview(r, c, previewRow, previewCol, draggedPiece)
                        val isClearing = animatingCells.contains(Pair(r, c))

                        Box(modifier = Modifier.size(cellSizeDp)) {
                            EmptyCell(size = cellSizeDp)
                            if (cellColor != null) {
                                BoardCell(color = cellColor, isClearing = isClearing, skin = skin, isDarkTheme = isDarkTheme)
                            } else if (isPreview && isPreviewValid) {
                                Box(Modifier.size(cellSizeDp).graphicsLayer { alpha = 0.5f }) {
                                    SkinBlock(
                                        color = draggedPiece!!.color,
                                        skin = skin,
                                        isDarkTheme = isDarkTheme,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCell(size: Dp) {
    Box(
        Modifier
            .size(size)
            .background(Color(0xFF121212), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
    )
}

@Composable
fun BoardCell(color: Color, isClearing: Boolean, skin: BlockSkin, isDarkTheme: Boolean) {
    val animatedColor by animateColorAsState(targetValue = color, animationSpec = tween(400), label = "color")
    val scale by animateFloatAsState(targetValue = if (isClearing) 0f else 1f, animationSpec = tween(350, easing = FastOutSlowInEasing), label = "scale")
    val rotation by animateFloatAsState(targetValue = if (isClearing) 90f else 0f, animationSpec = tween(350), label = "rotation")

    Box(modifier = Modifier.size(cellSizeDp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }) {
        SkinBlock(
            color = animatedColor,
            skin = skin,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AnimatedPieceWrapper(piece: Piece, skin: BlockSkin, isDarkTheme: Boolean) {
    val scale = remember { Animatable(0.3f) }
    val rotation = remember { Animatable(-45f) }

    LaunchedEffect(piece.id) {
        launch { scale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) }
        launch { rotation.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
    }

    Box(modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = rotation.value }) {
        DraggablePieceVisual(piece, skin, isDarkTheme)
    }
}

@Composable
fun DraggablePieceVisual(piece: Piece, skin: BlockSkin, isDarkTheme: Boolean) {
    val previewCellSize = 24.dp
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (r in 0 until piece.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until piece.cols) {
                    if (piece.shape[r][c] == 1) {
                        Box(Modifier.size(previewCellSize)) {
                            SkinBlock(
                                color = piece.color,
                                skin = skin,
                                isDarkTheme = isDarkTheme,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 4.dp,
                                borderWidth = 1.dp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(previewCellSize))
                    }
                }
            }
        }
    }
}

@Composable
fun DraggedPieceOverlay(
    piece: Piece,
    dragPosition: Offset,
    grabOffset: Offset,
    snappedTopLeft: Offset?,
    skin: BlockSkin,
    isDarkTheme: Boolean
) {
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
                        Box(Modifier.size(cellSizeDp)) {
                            SkinBlock(color = piece.color, skin = skin, isDarkTheme = isDarkTheme, modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        Spacer(modifier = Modifier.size(cellSizeDp))
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedScoreBubble(score: Int, highScore: Int, isDarkTheme: Boolean) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))) {
        Box(modifier = Modifier.matchParentSize().background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)).blur(15.dp))
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
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
        } else displayScore = targetScore
    }
    Text("$displayScore", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}
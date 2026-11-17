package com.example.mobilegame

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    BlockBlastGame()
                }
            }
        }
    }
}

// --- ДАННЫЕ ---
data class Piece(val shape: List<List<Int>>, val color: Color) {
    val rows = shape.size
    val cols = shape[0].size
}

val cellSizeDp = 36.dp
val spacingDp = 4.dp

// --- ГЛАВНЫЙ ЭКРАН ---
@Composable
fun BlockBlastGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }

    val boardSize = 8
    var board by remember { mutableStateOf(Array(boardSize) { arrayOfNulls<Color>(boardSize) }) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(prefs.getInt("high_score", 0)) }
    var isGameOver by remember { mutableStateOf(false) }

    val colors = listOf(
        Color(0xFF00E5FF), Color(0xFFFF5252), Color(0xFFFFD740),
        Color(0xFF69F0AE), Color(0xFFE040FB)
    )

    var trayPieces by remember { mutableStateOf(generateTray(colors)) }

    // Состояния перетаскивания
    var draggedPiece by remember { mutableStateOf<Piece?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current
    val stepPx = with(density) { (cellSizeDp + spacingDp).toPx() }

    var previewRow by remember { mutableIntStateOf(-1) }
    var previewCol by remember { mutableIntStateOf(-1) }
    var isPreviewValid by remember { mutableStateOf(false) }

    // Функция для сброса игры
    fun resetGame() {
        board = Array(boardSize) { arrayOfNulls(boardSize) }
        score = 0
        trayPieces = generateTray(colors)
        isGameOver = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя панель (Счет и Рекорд)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BLOCK BLAST", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Счет: $score", color = Color.LightGray, fontSize = 18.sp)
                    Text("Рекорд: $highScore", color = Color(0xFFFFD740), fontSize = 18.sp)
                }
            }

            GameBoard(
                board = board,
                previewRow = previewRow,
                previewCol = previewCol,
                isPreviewValid = isPreviewValid,
                draggedPiece = draggedPiece,
                onGridPositioned = { offset -> gridOrigin = offset }
            )

            PieceTray(
                trayPieces = trayPieces,
                onDragStart = { piece, fingerAbsPos, localGrabOffset ->
                    draggedPiece = piece
                    dragPosition = fingerAbsPos
                    grabOffset = localGrabOffset
                },
                onDrag = { dragAmount ->

                    val boostedDrag = Offset(dragAmount.x, dragAmount.y * 1.3f)
                    dragPosition += boostedDrag

                    draggedPiece?.let { piece ->
                        val topLeftX = dragPosition.x - grabOffset.x
                        val topLeftY = dragPosition.y - grabOffset.y
                        val relativeX = topLeftX - gridOrigin.x
                        val relativeY = topLeftY - gridOrigin.y

                        val col = (relativeX / stepPx).roundToInt()
                        val row = (relativeY / stepPx).roundToInt()

                        previewRow = row
                        previewCol = col
                        isPreviewValid = row != -1 && col != -1 && canPlacePiece(board, piece, row, col)
                    }
                },
                onDragEnd = {
                    if (isPreviewValid && draggedPiece != null) {
                        val piece = draggedPiece!!
                        placePiece(board, piece, previewRow, previewCol)

                        val (newBoard, linesCleared) = clearLines(board)
                        board = newBoard
                        score += linesCleared * 10

                        // Обновляем рекорд, если побили
                        if (score > highScore) {
                            highScore = score
                            prefs.edit().putInt("high_score", highScore).apply()
                        }

                        // Убираем фигуру из лотка
                        val newTray = trayPieces.filter { it != piece }

                        if (newTray.isEmpty()) {
                            // Если фигуры кончились, генерируем новые
                            val generatedTray = generateTray(colors)
                            trayPieces = generatedTray
                            // Проверяем, можно ли поставить новые фигуры
                            if (!canAnyPieceBePlaced(board, generatedTray)) {
                                isGameOver = true
                            }
                        } else {
                            trayPieces = newTray
                            // Проверяем оставшиеся фигуры
                            if (!canAnyPieceBePlaced(board, newTray)) {
                                isGameOver = true
                            }
                        }
                    }
                    draggedPiece = null
                    previewRow = -1
                    previewCol = -1
                }
            )
        }

        // Летящая фигура
        draggedPiece?.let { piece ->
            DraggedPieceOverlay(piece, dragPosition, grabOffset)
        }

        // Экран Game Over
        if (isGameOver) {
            GameOverScreen(score = score, highScore = highScore, onRestart = { resetGame() })
        }
    }
}

// --- ЛОГИКА ---

// Проверка: можно ли поставить ХОТЯ БЫ ОДНУ фигуру из списка куда-либо на поле
fun canAnyPieceBePlaced(board: Array<Array<Color?>>, pieces: List<Piece>): Boolean {
    for (piece in pieces) {
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                if (canPlacePiece(board, piece, r, c)) {
                    return true // Нашли место, игра продолжается
                }
            }
        }
    }
    return false // Ни одну фигуру поставить нельзя - Game Over
}

fun generateTray(colors: List<Color>): List<Piece> {
    val shapes = listOf(
        listOf(listOf(1)),
        listOf(listOf(1, 1, 1)),
        listOf(listOf(1, 1), listOf(1, 1)),
        listOf(listOf(1, 0), listOf(1, 1)),
        listOf(listOf(1, 1, 1), listOf(0, 1, 0)),
        listOf(listOf(1, 0), listOf(1, 1), listOf(0, 1)),
        listOf(listOf(1, 1, 1), listOf(1, 1, 1), listOf(1, 1, 1)) // Большой квадрат для сложности
    )
    return List(3) { Piece(shapes.random(), colors.random()) }
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

fun clearLines(board: Array<Array<Color?>>): Pair<Array<Array<Color?>>, Int> {
    var cleared = 0
    val newBoard = Array(8) { arrayOfNulls<Color>(8) }
    for (r in 0 until 8) for (c in 0 until 8) newBoard[r][c] = board[r][c]

    for (r in 0 until 8) {
        if (newBoard[r].all { it != null }) { newBoard[r] = arrayOfNulls(8); cleared++ }
    }
    for (c in 0 until 8) {
        if ((0 until 8).all { newBoard[it][c] != null }) {
            for (r in 0 until 8) newBoard[r][c] = null
            cleared++
        }
    }
    return Pair(newBoard, cleared)
}

// --- UI КОМПОНЕНТЫ ---

@Composable
fun GameOverScreen(score: Int, highScore: Int, onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .zIndex(20f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ИГРА ОКОНЧЕНА", color = Color(0xFFFF5252), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                Text("Твой счет: $score", color = Color.White, fontSize = 22.sp)
                Text(
                    text = if (score >= highScore) "Новый рекорд! 🎉" else "Рекорд: $highScore",
                    color = Color(0xFFFFD740),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("НАЧАТЬ ЗАНОВО", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameBoard(
    board: Array<Array<Color?>>,
    previewRow: Int, previewCol: Int, isPreviewValid: Boolean,
    draggedPiece: Piece?, onGridPositioned: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
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
                        val prevColor = if (isPreviewValid) draggedPiece?.color ?: Color.White else Color.Red

                        if (cellColor != null) FilledCell(size = cellSizeDp, color = cellColor)
                        else if (isPreview) FilledCell(size = cellSizeDp, color = prevColor.copy(alpha = 0.5f))
                        else EmptyCell(size = cellSizeDp)
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
    Box(Modifier.size(size).background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF3E3E3E), RoundedCornerShape(6.dp)))
}

@Composable
fun FilledCell(size: androidx.compose.ui.unit.Dp, color: Color) {
    Box(Modifier.size(size).background(color, RoundedCornerShape(6.dp)).border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)))
}

@Composable
fun PieceTray(
    trayPieces: List<Piece>,
    onDragStart: (Piece, Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        trayPieces.forEach { piece ->
            DraggablePiece(piece, onDragStart, onDrag, onDragEnd)
        }
    }
}

@Composable
fun DraggablePiece(
    piece: Piece,
    onDragStart: (Piece, Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var piecePos by remember { mutableStateOf(Offset.Zero) }
    val previewCellSize = 24.dp

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Column(
        modifier = Modifier
            .onGloballyPositioned { piecePos = it.positionInRoot() }
            .pointerInput(piece) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        currentOnDragStart(piece, piecePos + localOffset, localOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() }
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (r in 0 until piece.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until piece.cols) {
                    if (piece.shape[r][c] == 1) FilledCell(size = previewCellSize, color = piece.color)
                    else Spacer(modifier = Modifier.size(previewCellSize))
                }
            }
        }
    }
}

@Composable
fun DraggedPieceOverlay(piece: Piece, dragPosition: Offset, grabOffset: Offset) {
    val topLeftX = dragPosition.x - grabOffset.x
    val topLeftY = dragPosition.y - grabOffset.y

    Column(
        modifier = Modifier
            .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
            .zIndex(10f),
        verticalArrangement = Arrangement.spacedBy(spacingDp)
    ) {
        for (r in 0 until piece.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacingDp)) {
                for (c in 0 until piece.cols) {
                    if (piece.shape[r][c] == 1) FilledCell(size = cellSizeDp, color = piece.color)
                    else Spacer(modifier = Modifier.size(cellSizeDp))
                }
            }
        }
    }
}
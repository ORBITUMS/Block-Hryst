package com.example.mobilegame

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private val BASE_SHAPES: List<List<List<Int>>> = listOf(
    listOf(listOf(1)), listOf(listOf(1, 1)), listOf(listOf(1), listOf(1)),
    listOf(listOf(1, 1, 1)), listOf(listOf(1), listOf(1), listOf(1)),
    listOf(listOf(1, 1, 1, 1)), listOf(listOf(1), listOf(1), listOf(1), listOf(1)),
    listOf(listOf(1, 1), listOf(1, 1)),
    listOf(listOf(0, 1), listOf(1, 0)), listOf(listOf(1, 0), listOf(0, 1)),
    listOf(listOf(1, 1, 1), listOf(0, 1, 0)), listOf(listOf(0, 1, 0), listOf(1, 1, 1)),
    listOf(listOf(1, 0), listOf(1, 1), listOf(1, 0)), listOf(listOf(0, 1), listOf(1, 1), listOf(0, 1)),
    listOf(listOf(1, 1, 1), listOf(1, 0, 0)), listOf(listOf(1, 1, 1), listOf(0, 0, 1)),
    listOf(listOf(1, 0), listOf(1, 0), listOf(1, 1)), listOf(listOf(0, 1), listOf(0, 1), listOf(1, 1)),
    listOf(listOf(1, 1), listOf(1, 0), listOf(1, 0)), listOf(listOf(1, 1), listOf(0, 1), listOf(0, 1)),
    listOf(listOf(1, 1, 0), listOf(0, 1, 1)), listOf(listOf(0, 1, 1), listOf(1, 1, 0)),
    listOf(listOf(1, 0), listOf(1, 1), listOf(0, 1)), listOf(listOf(0, 1), listOf(1, 1), listOf(1, 0)),
    listOf(listOf(1, 1, 1), listOf(1, 1, 1), listOf(1, 1, 1))
)

fun changeBoardColors(board: Array<Array<Color?>>, isRandom: Boolean): Array<Array<Color?>> {
    val newBoard = Array(8) { arrayOfNulls<Color>(8) }
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val currentColor = board[r][c]
            if (currentColor != null) {
                if (isRandom) {
                    newBoard[r][c] = GAME_COLORS.random()
                } else {
                    val currentIndex = GAME_COLORS.indexOf(currentColor)
                    newBoard[r][c] = if (currentIndex != -1) {
                        GAME_COLORS[(currentIndex + 1) % GAME_COLORS.size]
                    } else {
                        GAME_COLORS.random()
                    }
                }
            }
        }
    }
    return newBoard
}

fun clearLinesWithCoords(board: Array<Array<Color?>>): Triple<Array<Array<Color?>>, Int, Set<Pair<Int, Int>>> {
    val clearedCoords = mutableSetOf<Pair<Int, Int>>()
    val fullRows = mutableListOf<Int>()
    val fullCols = mutableListOf<Int>()

    for (r in 0 until 8) if (board[r].all { it != null }) fullRows.add(r)
    for (c in 0 until 8) if ((0 until 8).all { board[it][c] != null }) fullCols.add(c)

    val totalLinesCleared = fullRows.size + fullCols.size

    if (totalLinesCleared > 0) {
        for (r in fullRows) for (c in 0 until 8) clearedCoords.add(Pair(r, c))
        for (c in fullCols) for (r in 0 until 8) clearedCoords.add(Pair(r, c))
    }

    val newBoard = Array(8) { arrayOfNulls<Color>(8) }
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            if (!clearedCoords.contains(Pair(r, c))) newBoard[r][c] = board[r][c]
        }
    }
    return Triple(newBoard, totalLinesCleared, clearedCoords)
}

fun canAnyPieceBePlaced(board: Array<Array<Color?>>, pieces: List<Piece?>): Boolean {
    for (piece in pieces.filterNotNull()) {
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                if (canPlacePiece(board, piece, r, c)) return true
            }
        }
    }
    return false
}

fun generateSinglePiece(color: Color): Piece = Piece(shape = BASE_SHAPES.random(), color = color)

fun generateTray(colors: List<Color>, freeCells: Int): List<Piece?> {
    val shapes = BASE_SHAPES.toMutableList()
    if (freeCells < 32) {
        repeat(3) { shapes.add(listOf(listOf(1))) }
    }
    return List(3) { Piece(shape = shapes.random(), color = colors.random()) }
}
fun generateArcadeBoard(): Array<Array<Color?>> {
    val b = Array(8) { arrayOfNulls<Color>(8) }

    // 1. Создаём 2–3 почти заполненные полосы (по 5–7 блоков подряд)
    val linesCount = Random.nextInt(2, 4)
    repeat(linesCount) {
        val horizontal = Random.nextBoolean()
        val lineIdx = Random.nextInt(8)
        val length = Random.nextInt(5, 8)
        val start = Random.nextInt(0, 9 - length)
        val color = GAME_COLORS.random()
        for (i in start until start + length) {
            if (horizontal) b[lineIdx][i] = color else b[i][lineIdx] = color
        }
    }

    // 2. Добавляем 1–2 сплошных прямоугольных кластера (2x2 или 2x3)
    val clustersCount = Random.nextInt(1, 3)
    repeat(clustersCount) {
        val h = Random.nextInt(2, 4)
        val w = Random.nextInt(2, 4)
        val r0 = Random.nextInt(0, 9 - h)
        val c0 = Random.nextInt(0, 9 - w)
        val color = GAME_COLORS.random()
        for (r in r0 until r0 + h) {
            for (c in c0 until c0 + w) {
                b[r][c] = color
            }
        }
    }

    // 3. Гарантируем, что нет уже полностью заполненных линий
    for (r in 0 until 8) {
        if (b[r].all { it != null }) b[r][Random.nextInt(8)] = null
    }
    for (c in 0 until 8) {
        if ((0 until 8).all { b[it][c] != null }) b[Random.nextInt(8)][c] = null
    }

    return b
}

private fun evaluatePlacement(board: Array<Array<Color?>>, piece: Piece, row: Int, col: Int): Int {
    fun isFilled(r: Int, c: Int): Boolean {
        if (board[r][c] != null) return true
        val pr = r - row
        val pc = c - col
        return pr in 0 until piece.rows && pc in 0 until piece.cols && piece.shape[pr][pc] == 1
    }

    var linesCleared = 0
    var nearFullBonus = 0

    for (r in 0 until 8) {
        var count = 0
        for (c in 0 until 8) if (isFilled(r, c)) count++
        if (count == 8) linesCleared++
        else if (count >= 6) nearFullBonus += (count - 5) * 8
    }
    for (c in 0 until 8) {
        var count = 0
        for (r in 0 until 8) if (isFilled(r, c)) count++
        if (count == 8) linesCleared++
        else if (count >= 6) nearFullBonus += (count - 5) * 8
    }

    // Бонус за плотное прилегание к уже стоящим блокам и краям доски («встало в паз»)
    var touchBonus = 0
    var cellsCount = 0
    for (pr in 0 until piece.rows) {
        for (pc in 0 until piece.cols) {
            if (piece.shape[pr][pc] == 1) {
                cellsCount++
                val r = row + pr
                val c = col + pc
                if (r == 0 || board[r - 1][c] != null) touchBonus += 6
                if (r == 7 || board[r + 1][c] != null) touchBonus += 6
                if (c == 0 || board[r][c - 1] != null) touchBonus += 6
                if (c == 7 || board[r][c + 1] != null) touchBonus += 6
            }
        }
    }

    return linesCleared * 250 + touchBonus + nearFullBonus + cellsCount * 3 + Random.nextInt(12)
}

/**
 * Умный подбор 3 фигур с последовательной симуляцией доски:
 * проверяет все позиции, поощряет закрытие линий и плотное прилегание.
 */
fun generateSolvableTray(currentBoard: Array<Array<Color?>>): List<Piece?> {
    var simBoard = currentBoard.map { it.clone() }.toTypedArray()
    val result = mutableListOf<Piece>()

    repeat(3) {
        var bestPiece: Piece? = null
        var bestPos: Pair<Int, Int>? = null
        var bestScore = -1

        val candidates = BASE_SHAPES.shuffled().take(18)
        for (shape in candidates) {
            val piece = Piece(shape = shape, color = GAME_COLORS.random())
            for (r in 0..(8 - piece.rows)) {
                for (c in 0..(8 - piece.cols)) {
                    if (canPlacePiece(simBoard, piece, r, c)) {
                        val moveScore = evaluatePlacement(simBoard, piece, r, c)
                        if (moveScore > bestScore) {
                            bestScore = moveScore
                            bestPiece = piece
                            bestPos = r to c
                        }
                    }
                }
            }
        }

        if (bestPiece != null && bestPos != null) {
            result.add(bestPiece)
            val nextBoard = simBoard.map { it.clone() }.toTypedArray()
            placePiece(nextBoard, bestPiece, bestPos.first, bestPos.second)
            simBoard = clearLinesWithCoords(nextBoard).first
        } else {
            result.add(Piece(shape = listOf(listOf(1)), color = GAME_COLORS.random()))
        }
    }

    return result.shuffled()
}

fun generateArcadeTray(board: Array<Array<Color?>>): List<Piece?> {
    val result = mutableListOf<Piece?>()
    repeat(3) {
        var chosen: Piece? = null
        for (attempt in 0 until 100) {
            val candidate = generateSinglePiece(GAME_COLORS.random())
            val fits = (0 until 8).any { r ->
                (0 until 8).any { c -> canPlacePiece(board, candidate, r, c) }
            }
            if (fits) {
                chosen = candidate
                break
            }
        }
        result.add(chosen ?: Piece(shape = listOf(listOf(1)), color = GAME_COLORS.random()))
    }
    return result
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

fun isCellInPreview(r: Int, c: Int, pr: Int, pc: Int, piece: Piece): Boolean {
    if (pr == -1 || pc == -1) return false
    val relR = r - pr
    val relC = c - pc
    return relR in 0 until piece.rows && relC in 0 until piece.cols && piece.shape[relR][relC] == 1
}
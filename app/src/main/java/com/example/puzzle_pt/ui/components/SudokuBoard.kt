package com.example.puzzle_pt.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.CELL_COUNT
import com.example.puzzle_pt.model.SUDOKU_SIZE
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

@Composable
fun SudokuBoard(
    puzzle: SudokuPuzzle,
    board: List<Int>,
    notes: List<Set<Int>>,
    selectedIndex: Int?,
    conflictCells: BooleanArray,
    modifier: Modifier = Modifier,
    highlightCells: Set<Int> = emptySet(),
    hintIndex: Int? = null,
    interactive: Boolean = true,
    onCellSelected: (Int) -> Unit = {},
) {
    val boardModifier = modifier.widthIn(max = 560.dp)
    if (puzzle.type == SudokuType.SKYSCRAPER) {
        BoxWithConstraints(boardModifier.fillMaxWidth().aspectRatio(1f).testTag("sudoku_board")) {
            val edge = 28.dp
            val boardSize = maxWidth - edge * 2
            BoardGrid(
                puzzle = puzzle,
                board = board,
                notes = notes,
                selectedIndex = selectedIndex,
                conflictCells = conflictCells,
                highlightCells = highlightCells,
                hintIndex = hintIndex,
                interactive = interactive,
                onCellSelected = onCellSelected,
                modifier = Modifier.size(boardSize).align(Alignment.Center),
            )
            SkyscraperClues(puzzle.skyscraperClues, boardSize, edge)
        }
    } else {
        BoardGrid(
            puzzle = puzzle,
            board = board,
            notes = notes,
            selectedIndex = selectedIndex,
            conflictCells = conflictCells,
            highlightCells = highlightCells,
            hintIndex = hintIndex,
            interactive = interactive,
            onCellSelected = onCellSelected,
            modifier = boardModifier.fillMaxWidth().aspectRatio(1f).testTag("sudoku_board"),
        )
    }
}

@Composable
private fun BoardGrid(
    puzzle: SudokuPuzzle,
    board: List<Int>,
    notes: List<Set<Int>>,
    selectedIndex: Int?,
    conflictCells: BooleanArray,
    highlightCells: Set<Int>,
    hintIndex: Int?,
    interactive: Boolean,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val related = selectedIndex?.let { SudokuRules.relatedCells(it, puzzle.type) }.orEmpty()
    val selectedValue = selectedIndex?.let { board[it] } ?: 0
    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until SUDOKU_SIZE) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (column in 0 until SUDOKU_SIZE) {
                        val index = row * SUDOKU_SIZE + column
                        val isDiagonal = puzzle.type == SudokuType.DIAGONAL && (row == column || row + column == 5)
                        val background = when {
                            selectedIndex == index -> colors.primaryContainer
                            conflictCells[index] -> colors.errorContainer
                            hintIndex == index || index in highlightCells -> colors.tertiaryContainer
                            selectedValue != 0 && board[index] == selectedValue -> colors.secondaryContainer
                            index in related -> colors.secondaryContainer.copy(alpha = 0.65f)
                            isDiagonal -> colors.surfaceVariant
                            else -> colors.surface
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(background)
                                .testTag("cell_$index")
                                .clickable(enabled = interactive) { onCellSelected(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                board[index] != 0 -> Text(
                                    text = board[index].toString(),
                                    fontSize = 24.sp,
                                    fontWeight = if (puzzle.givens[index] != 0) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        conflictCells[index] -> colors.error
                                        puzzle.givens[index] != 0 -> colors.onSurface
                                        else -> colors.primary
                                    },
                                )
                                notes[index].isNotEmpty() -> CandidateNotes(notes[index])
                            }
                            if (puzzle.type == SudokuType.KILLER) {
                                val cage = puzzle.cages.firstOrNull { it.cells.first().index == index }
                                if (cage != null) {
                                    Text(
                                        text = cage.sum.toString(),
                                        fontSize = 9.sp,
                                        color = colors.primary,
                                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        GridLines(puzzle)
    }
}

@Composable
private fun CandidateNotes(candidates: Set<Int>) {
    Column(Modifier.fillMaxSize().padding(3.dp)) {
        for (row in 0..1) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (column in 0..2) {
                    val value = row * 3 + column + 1
                    Text(
                        text = if (value in candidates) value.toString() else "",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GridLines(puzzle: SudokuPuzzle) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val strongColor = MaterialTheme.colorScheme.onSurface
    val cageColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val cellWidth = size.width / SUDOKU_SIZE
        val cellHeight = size.height / SUDOKU_SIZE
        for (line in 0..SUDOKU_SIZE) {
            val strong = line == 0 || line == SUDOKU_SIZE || line == 3
            drawLine(
                color = if (strong) strongColor else gridColor,
                start = Offset(line * cellWidth, 0f),
                end = Offset(line * cellWidth, size.height),
                strokeWidth = (if (strong) 2.5.dp else 1.dp).toPx(),
            )
            val strongRow = line == 0 || line == SUDOKU_SIZE || line % 2 == 0
            drawLine(
                color = if (strongRow) strongColor else gridColor,
                start = Offset(0f, line * cellHeight),
                end = Offset(size.width, line * cellHeight),
                strokeWidth = (if (strongRow) 2.5.dp else 1.dp).toPx(),
            )
        }
        if (puzzle.type == SudokuType.KILLER) {
            val cageAt = IntArray(CELL_COUNT) { -1 }
            puzzle.cages.forEachIndexed { cageIndex, cage ->
                cage.cells.forEach { cageAt[it.index] = cageIndex }
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))
            val cageStroke = 1.4.dp.toPx()
            puzzle.cages.forEach { cage ->
                cage.cells.forEach { position ->
                    val index = position.index
                    val row = position.row
                    val column = position.column
                    val left = column * cellWidth + 3f
                    val right = (column + 1) * cellWidth - 3f
                    val top = row * cellHeight + 3f
                    val bottom = (row + 1) * cellHeight - 3f
                    if (row == 0 || cageAt[index - SUDOKU_SIZE] != cageAt[index]) {
                        drawLine(cageColor, Offset(left, top), Offset(right, top), cageStroke, pathEffect = dash)
                    }
                    if (row == 5 || cageAt[index + SUDOKU_SIZE] != cageAt[index]) {
                        drawLine(cageColor, Offset(left, bottom), Offset(right, bottom), cageStroke, pathEffect = dash)
                    }
                    if (column == 0 || cageAt[index - 1] != cageAt[index]) {
                        drawLine(cageColor, Offset(left, top), Offset(left, bottom), cageStroke, pathEffect = dash)
                    }
                    if (column == 5 || cageAt[index + 1] != cageAt[index]) {
                        drawLine(cageColor, Offset(right, top), Offset(right, bottom), cageStroke, pathEffect = dash)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyscraperClues(clues: List<Int>, boardSize: Dp, edge: Dp) {
    val cellSize = boardSize / 6
    for (index in 0 until SUDOKU_SIZE) {
        ClueText(clues[index], Modifier.offset(x = edge + cellSize * index, y = 0.dp).size(cellSize, edge))
        ClueText(clues[6 + index], Modifier.offset(x = edge + boardSize, y = edge + cellSize * index).size(edge, cellSize))
        ClueText(clues[12 + index], Modifier.offset(x = edge + cellSize * index, y = edge + boardSize).size(cellSize, edge))
        ClueText(clues[18 + index], Modifier.offset(x = 0.dp, y = edge + cellSize * index).size(edge, cellSize))
    }
}

@Composable
private fun ClueText(value: Int, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = if (value == 0) "–" else value.toString(),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

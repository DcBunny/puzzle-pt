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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzle_pt.engine.SudokuRules
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
            SkyscraperClues(puzzle.skyscraperClues, boardSize, edge, puzzle.size)
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
    val size = puzzle.size
    val related = selectedIndex?.let { SudokuRules.relatedCells(it, puzzle) }.orEmpty()
    val selectedValue = selectedIndex?.let { board[it] } ?: 0
    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until size) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (column in 0 until size) {
                        val index = row * size + column
                        val isDiagonal = puzzle.type == SudokuType.DIAGONAL && (row == column || row + column == size - 1)
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
                            val cage = if (puzzle.type == SudokuType.KILLER) {
                                puzzle.cages.firstOrNull { it.cells.first().index == index }
                            } else {
                                null
                            }
                            when {
                                board[index] != 0 -> Text(
                                    text = board[index].toString(),
                                    fontSize = if (size == 9) 20.sp else 24.sp,
                                    fontWeight = if (puzzle.givens[index] != 0) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        conflictCells[index] -> colors.error
                                        puzzle.givens[index] != 0 -> colors.onSurface
                                        else -> colors.primary
                                    },
                                )
                                notes[index].isNotEmpty() -> CandidateNotes(
                                    candidates = notes[index],
                                    hasCageSum = cage != null,
                                    size = size,
                                )
                            }
                            if (cage != null) {
                                Text(
                                    text = cage.sum.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(3.dp)
                                        .background(
                                            color = colors.surface.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 3.dp, vertical = 1.dp),
                                )
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
private fun CandidateNotes(candidates: Set<Int>, hasCageSum: Boolean, size: Int) {
    Column(
        Modifier.fillMaxSize().padding(
            start = if (hasCageSum) 28.dp else 3.dp,
            top = 3.dp,
            end = 3.dp,
            bottom = 3.dp,
        ),
    ) {
        for (row in 0 until (size + 2) / 3) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (column in 0..2) {
                    val value = row * 3 + column + 1
                    Text(
                        text = if (value <= size && value in candidates) value.toString() else "",
                        fontSize = if (size == 9) 7.sp else 9.sp,
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
    val size = puzzle.size
    Canvas(Modifier.fillMaxSize()) {
        val cellWidth = this.size.width / size
        val cellHeight = this.size.height / size
        for (line in 0..size) {
            val strong = line == 0 || line == size ||
                (size == 6 && line == 3) || (size == 9 && line % 3 == 0)
            drawLine(
                color = if (strong) strongColor else gridColor,
                start = Offset(line * cellWidth, 0f),
                end = Offset(line * cellWidth, this.size.height),
                strokeWidth = (if (strong) 2.5.dp else 1.dp).toPx(),
            )
            val strongRow = line == 0 || line == size ||
                (size == 6 && line % 2 == 0) || (size == 9 && line % 3 == 0)
            drawLine(
                color = if (strongRow) strongColor else gridColor,
                start = Offset(0f, line * cellHeight),
                end = Offset(this.size.width, line * cellHeight),
                strokeWidth = (if (strongRow) 2.5.dp else 1.dp).toPx(),
            )
        }
        if (puzzle.regions.isNotEmpty()) {
            for (row in 0 until size) {
                for (column in 0 until size) {
                    val index = row * size + column
                    if (column < size - 1 && puzzle.regions[index] != puzzle.regions[index + 1]) {
                        drawLine(
                            color = strongColor,
                            start = Offset((column + 1) * cellWidth, row * cellHeight),
                            end = Offset((column + 1) * cellWidth, (row + 1) * cellHeight),
                            strokeWidth = 2.5.dp.toPx(),
                        )
                    }
                    if (row < size - 1 && puzzle.regions[index] != puzzle.regions[index + size]) {
                        drawLine(
                            color = strongColor,
                            start = Offset(column * cellWidth, (row + 1) * cellHeight),
                            end = Offset((column + 1) * cellWidth, (row + 1) * cellHeight),
                            strokeWidth = 2.5.dp.toPx(),
                        )
                    }
                }
            }
        }
        if (puzzle.type == SudokuType.KILLER) {
            val cageAt = IntArray(puzzle.cellCount) { -1 }
            puzzle.cages.forEachIndexed { cageIndex, cage ->
                cage.cells.forEach { cageAt[it.index] = cageIndex }
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 3.dp.toPx()))
            val cageStroke = 2.dp.toPx()
            val cageInset = 3.dp.toPx()
            puzzle.cages.forEach { cage ->
                cage.cells.forEach { position ->
                    val index = position.index
                    val row = position.row
                    val column = position.column
                    val left = column * cellWidth + cageInset
                    val right = (column + 1) * cellWidth - cageInset
                    val top = row * cellHeight + cageInset
                    val bottom = (row + 1) * cellHeight - cageInset
                    if (row == 0 || cageAt[index - size] != cageAt[index]) {
                        drawLine(
                            color = cageColor,
                            start = Offset(left, top),
                            end = Offset(right, top),
                            strokeWidth = cageStroke,
                            cap = StrokeCap.Round,
                            pathEffect = dash,
                        )
                    }
                    if (row == size - 1 || cageAt[index + size] != cageAt[index]) {
                        drawLine(
                            color = cageColor,
                            start = Offset(left, bottom),
                            end = Offset(right, bottom),
                            strokeWidth = cageStroke,
                            cap = StrokeCap.Round,
                            pathEffect = dash,
                        )
                    }
                    if (column == 0 || cageAt[index - 1] != cageAt[index]) {
                        drawLine(
                            color = cageColor,
                            start = Offset(left, top),
                            end = Offset(left, bottom),
                            strokeWidth = cageStroke,
                            cap = StrokeCap.Round,
                            pathEffect = dash,
                        )
                    }
                    if (column == size - 1 || cageAt[index + 1] != cageAt[index]) {
                        drawLine(
                            color = cageColor,
                            start = Offset(right, top),
                            end = Offset(right, bottom),
                            strokeWidth = cageStroke,
                            cap = StrokeCap.Round,
                            pathEffect = dash,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyscraperClues(clues: List<Int>, boardSize: Dp, edge: Dp, size: Int) {
    val cellSize = boardSize / size
    for (index in 0 until size) {
        ClueText(clues[index], Modifier.offset(x = edge + cellSize * index, y = 0.dp).size(cellSize, edge))
        ClueText(clues[size + index], Modifier.offset(x = edge + boardSize, y = edge + cellSize * index).size(edge, cellSize))
        ClueText(clues[size * 2 + index], Modifier.offset(x = edge + cellSize * index, y = edge + boardSize).size(cellSize, edge))
        ClueText(clues[size * 3 + index], Modifier.offset(x = 0.dp, y = edge + cellSize * index).size(edge, cellSize))
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

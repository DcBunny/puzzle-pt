package com.example.puzzle_pt.engine

import com.example.puzzle_pt.model.CELL_COUNT
import com.example.puzzle_pt.model.SUDOKU_SIZE
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

object SudokuRules {
    private val rowGroups = List(SUDOKU_SIZE) { row ->
        List(SUDOKU_SIZE) { column -> row * SUDOKU_SIZE + column }
    }
    private val columnGroups = List(SUDOKU_SIZE) { column ->
        List(SUDOKU_SIZE) { row -> row * SUDOKU_SIZE + column }
    }
    private val boxGroups = List(SUDOKU_SIZE) { box ->
        val startRow = (box / 2) * 2
        val startColumn = (box % 2) * 3
        List(SUDOKU_SIZE) { offset ->
            (startRow + offset / 3) * SUDOKU_SIZE + startColumn + offset % 3
        }
    }
    private val mainDiagonal = List(SUDOKU_SIZE) { it * SUDOKU_SIZE + it }
    private val antiDiagonal = List(SUDOKU_SIZE) { it * SUDOKU_SIZE + (SUDOKU_SIZE - 1 - it) }
    private val baseGroups = rowGroups + columnGroups + boxGroups

    fun relatedCells(index: Int, type: SudokuType): Set<Int> {
        val row = index / SUDOKU_SIZE
        val column = index % SUDOKU_SIZE
        val related = baseGroups.first { row * SUDOKU_SIZE + column in it }.toMutableSet()
        related += columnGroups[column]
        related += boxGroups[(row / 2) * 2 + column / 3]
        if (type == SudokuType.DIAGONAL) {
            if (index in mainDiagonal) related += mainDiagonal
            if (index in antiDiagonal) related += antiDiagonal
        }
        if (type == SudokuType.ANTI_KNIGHT) related += knightCells(index)
        related -= index
        return related
    }

    fun knightCells(index: Int): Set<Int> {
        val row = index / SUDOKU_SIZE
        val column = index % SUDOKU_SIZE
        val moves = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        return moves.mapNotNull { (rowDelta, columnDelta) ->
            val targetRow = row + rowDelta
            val targetColumn = column + columnDelta
            if (targetRow in 0 until SUDOKU_SIZE && targetColumn in 0 until SUDOKU_SIZE) {
                targetRow * SUDOKU_SIZE + targetColumn
            } else {
                null
            }
        }.toSet()
    }

    fun conflictingCells(board: List<Int>, puzzle: SudokuPuzzle): BooleanArray {
        require(board.size == CELL_COUNT)
        val conflicts = BooleanArray(CELL_COUNT)
        val groups = buildList {
            addAll(baseGroups)
            if (puzzle.type == SudokuType.DIAGONAL) {
                add(mainDiagonal)
                add(antiDiagonal)
            }
        }
        groups.forEach { group -> markDuplicates(board, group, conflicts) }

        if (puzzle.type == SudokuType.ANTI_KNIGHT) {
            for (index in 0 until CELL_COUNT) {
                if (board[index] == 0) continue
                knightCells(index).filter { it > index }.forEach { other ->
                    if (board[other] == board[index]) {
                        conflicts[index] = true
                        conflicts[other] = true
                    }
                }
            }
        }

        if (puzzle.type == SudokuType.KILLER) {
            puzzle.cages.forEach { cage ->
                val indices = cage.cells.map { it.index }
                markDuplicates(board, indices, conflicts)
                val values = indices.map { board[it] }
                val sum = values.sum()
                if (sum > cage.sum || (values.none { it == 0 } && sum != cage.sum)) {
                    indices.filter { board[it] != 0 }.forEach { conflicts[it] = true }
                }
            }
        }

        if (puzzle.type == SudokuType.SKYSCRAPER) {
            skyscraperLines().forEachIndexed { clueIndex, line ->
                val clue = puzzle.skyscraperClues[clueIndex]
                if (clue > 0 && line.all { board[it] != 0 }) {
                    val values = line.map { board[it] }
                    if (visibleBuildings(values) != clue) {
                        line.forEach { conflicts[it] = true }
                    }
                }
            }
        }
        return conflicts
    }

    fun isComplete(board: List<Int>, puzzle: SudokuPuzzle): Boolean =
        board.all { it in 1..6 } && conflictingCells(board, puzzle).none { it }

    fun candidates(board: IntArray, index: Int, puzzle: SudokuPuzzle): List<Int> {
        if (board[index] != 0) return emptyList()
        val working = board.copyOf()
        return (1..6).filter { value ->
            working[index] = value
            val valid = !conflictingCells(working.toList(), puzzle)[index]
            working[index] = 0
            valid
        }
    }

    fun visibleBuildings(values: List<Int>): Int {
        var tallest = 0
        var visible = 0
        values.forEach { value ->
            if (value > tallest) {
                tallest = value
                visible++
            }
        }
        return visible
    }

    fun skyscraperLines(): List<List<Int>> = buildList {
        for (column in 0 until SUDOKU_SIZE) {
            add(List(SUDOKU_SIZE) { row -> row * SUDOKU_SIZE + column })
        }
        for (row in 0 until SUDOKU_SIZE) {
            add(List(SUDOKU_SIZE) { offset -> row * SUDOKU_SIZE + (SUDOKU_SIZE - 1 - offset) })
        }
        for (column in 0 until SUDOKU_SIZE) {
            add(List(SUDOKU_SIZE) { offset -> (SUDOKU_SIZE - 1 - offset) * SUDOKU_SIZE + column })
        }
        for (row in 0 until SUDOKU_SIZE) {
            add(List(SUDOKU_SIZE) { column -> row * SUDOKU_SIZE + column })
        }
    }

    private fun markDuplicates(board: List<Int>, group: List<Int>, conflicts: BooleanArray) {
        val nonzero = group.filter { board[it] != 0 }
        nonzero.groupBy { board[it] }.values.filter { it.size > 1 }.flatten().forEach {
            conflicts[it] = true
        }
    }
}

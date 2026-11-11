package com.example.puzzle_pt.engine

import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

object SudokuRules {
    fun relatedCells(index: Int, puzzle: SudokuPuzzle): Set<Int> {
        val size = puzzle.size
        val row = index / size
        val column = index % size
        val related = rowIndices(row, size).toMutableSet()
        related += columnIndices(column, size)
        related += boxGroups(puzzle).first { index in it }
        if (puzzle.type == SudokuType.DIAGONAL) {
            if (index in mainDiagonal(size)) related += mainDiagonal(size)
            if (index in antiDiagonal(size)) related += antiDiagonal(size)
        }
        if (puzzle.type == SudokuType.ANTI_KNIGHT) related += knightCells(index, size)
        related -= index
        return related
    }

    fun knightCells(index: Int, size: Int = 6): Set<Int> {
        val row = index / size
        val column = index % size
        val moves = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        return moves.mapNotNull { (rowDelta, columnDelta) ->
            val targetRow = row + rowDelta
            val targetColumn = column + columnDelta
            if (targetRow in 0 until size && targetColumn in 0 until size) {
                targetRow * size + targetColumn
            } else {
                null
            }
        }.toSet()
    }

    fun conflictingCells(board: List<Int>, puzzle: SudokuPuzzle): BooleanArray {
        require(board.size == puzzle.cellCount)
        val conflicts = BooleanArray(puzzle.cellCount)
        val groups = buildList {
            for (row in 0 until puzzle.size) add(rowIndices(row, puzzle.size))
            for (column in 0 until puzzle.size) add(columnIndices(column, puzzle.size))
            addAll(boxGroups(puzzle))
            if (puzzle.type == SudokuType.DIAGONAL) {
                add(mainDiagonal(puzzle.size))
                add(antiDiagonal(puzzle.size))
            }
        }
        groups.forEach { group -> markDuplicates(board, group, conflicts) }

        if (puzzle.type == SudokuType.ANTI_KNIGHT) {
            for (index in 0 until puzzle.cellCount) {
                if (board[index] == 0) continue
                knightCells(index, puzzle.size).filter { it > index }.forEach { other ->
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
            skyscraperLines(puzzle.size).forEachIndexed { clueIndex, line ->
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
        board.all { it in 1..puzzle.size } && conflictingCells(board, puzzle).none { it }

    fun candidates(board: IntArray, index: Int, puzzle: SudokuPuzzle): List<Int> {
        if (board[index] != 0) return emptyList()
        val working = board.copyOf()
        return (1..puzzle.size).filter { value ->
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

    fun skyscraperLines(size: Int = 6): List<List<Int>> = buildList {
        for (column in 0 until size) {
            add(columnIndices(column, size))
        }
        for (row in 0 until size) {
            add(List(size) { offset -> row * size + (size - 1 - offset) })
        }
        for (column in 0 until size) {
            add(List(size) { offset -> (size - 1 - offset) * size + column })
        }
        for (row in 0 until size) {
            add(rowIndices(row, size))
        }
    }

    private fun rowIndices(row: Int, size: Int): List<Int> =
        List(size) { column -> row * size + column }

    private fun columnIndices(column: Int, size: Int): List<Int> =
        List(size) { row -> row * size + column }

    private fun mainDiagonal(size: Int): List<Int> = List(size) { it * size + it }

    private fun antiDiagonal(size: Int): List<Int> = List(size) { it * size + (size - 1 - it) }

    private fun boxGroups(puzzle: SudokuPuzzle): List<List<Int>> {
        if (puzzle.regions.isNotEmpty()) {
            return List(puzzle.size) { region ->
                puzzle.regions.indices.filter { puzzle.regions[it] == region }
            }
        }
        val (boxHeight, boxWidth) = when (puzzle.size) {
            6 -> 2 to 3
            9 -> 3 to 3
            else -> error("该尺寸必须提供宫区划分")
        }
        val boxesAcross = puzzle.size / boxWidth
        return List(puzzle.size) { box ->
            val startRow = (box / boxesAcross) * boxHeight
            val startColumn = (box % boxesAcross) * boxWidth
            List(puzzle.size) { offset ->
                (startRow + offset / boxWidth) * puzzle.size + startColumn + offset % boxWidth
            }
        }
    }

    private fun markDuplicates(board: List<Int>, group: List<Int>, conflicts: BooleanArray) {
        val nonzero = group.filter { board[it] != 0 }
        nonzero.groupBy { board[it] }.values.filter { it.size > 1 }.flatten().forEach {
            conflicts[it] = true
        }
    }
}

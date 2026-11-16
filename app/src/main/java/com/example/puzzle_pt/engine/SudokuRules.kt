package com.example.puzzle_pt.engine

import com.example.puzzle_pt.model.RuleKind
import com.example.puzzle_pt.model.RuleViolation
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

    /**
     * 直接对错：重复、笼内超出目标和、整行楼数已经不可能。
     * 不包含需要枚举组合才能发现的笼和或摩天楼筛选。
     */
    fun basicViolations(board: List<Int>, puzzle: SudokuPuzzle): List<RuleViolation> {
        require(board.size == puzzle.cellCount)
        val violations = mutableListOf<RuleViolation>()
        for (row in 0 until puzzle.size) {
            markDuplicates(board, rowIndices(row, puzzle.size), RuleKind.ROW, violations)
        }
        for (column in 0 until puzzle.size) {
            markDuplicates(board, columnIndices(column, puzzle.size), RuleKind.COLUMN, violations)
        }
        boxGroups(puzzle).forEach { group ->
            markDuplicates(board, group, RuleKind.BOX, violations)
        }
        if (puzzle.type == SudokuType.DIAGONAL) {
            markDuplicates(board, mainDiagonal(puzzle.size), RuleKind.DIAGONAL, violations)
            markDuplicates(board, antiDiagonal(puzzle.size), RuleKind.DIAGONAL, violations)
        }
        if (puzzle.type == SudokuType.ANTI_KNIGHT) {
            for (index in 0 until puzzle.cellCount) {
                val value = board[index]
                if (value == 0) continue
                knightCells(index, puzzle.size).filter { it > index && board[it] == value }.forEach { other ->
                    violations += RuleViolation(RuleKind.KNIGHT, listOf(index, other), value)
                }
            }
        }
        if (puzzle.type == SudokuType.KILLER) {
            puzzle.cages.forEach { cage ->
                val indices = cage.cells.map { it.index }
                markDuplicates(board, indices, RuleKind.CAGE_DUPLICATE, violations)
                val filled = indices.map { board[it] }.filter { it != 0 }
                if (filled.sum() > cage.sum) {
                    violations += RuleViolation(
                        RuleKind.CAGE_SUM,
                        indices.filter { board[it] != 0 },
                        expectedSum = cage.sum,
                    )
                }
            }
        }
        if (puzzle.type == SudokuType.SKYSCRAPER) {
            skyscraperLines(puzzle.size).forEachIndexed { clueIndex, line ->
                val clue = puzzle.skyscraperClues[clueIndex]
                if (clue > 0 && line.all { board[it] != 0 }) {
                    val values = line.map { board[it] }
                    if (visibleBuildings(values) != clue) {
                        violations += RuleViolation(RuleKind.SKYSCRAPER, line, clue = clue)
                    }
                }
            }
        }
        return violations
    }

    fun violations(board: List<Int>, puzzle: SudokuPuzzle): List<RuleViolation> {
        val violations = basicViolations(board, puzzle).toMutableList()
        if (puzzle.type == SudokuType.KILLER) {
            puzzle.cages.forEach { cage ->
                val indices = cage.cells.map { it.index }
                val values = indices.map { board[it] }
                if (values.filter { it != 0 }.let { it.size != it.toSet().size }) return@forEach
                if (values.sum() > cage.sum) return@forEach
                if (!cageCombinationExists(values, cage.sum, puzzle.size)) {
                    val marked = indices.filter { board[it] != 0 }.ifEmpty { indices }
                    violations += RuleViolation(RuleKind.CAGE_SUM, marked, expectedSum = cage.sum)
                }
            }
        }
        if (puzzle.type == SudokuType.SKYSCRAPER) {
            skyscraperLines(puzzle.size).forEachIndexed { clueIndex, line ->
                val clue = puzzle.skyscraperClues[clueIndex]
                if (clue <= 0 || line.all { board[it] != 0 }) return@forEachIndexed
                if (skyscraperPartiallyImpossible(board, line, clue, puzzle.size)) {
                    val marked = line.filter { board[it] != 0 }.ifEmpty { listOf(line.first()) }
                    violations += RuleViolation(RuleKind.SKYSCRAPER, marked, clue = clue)
                }
            }
        }
        return violations
    }

    fun conflictingCells(board: List<Int>, puzzle: SudokuPuzzle): BooleanArray {
        require(board.size == puzzle.cellCount)
        val conflicts = BooleanArray(puzzle.cellCount)
        violations(board, puzzle).forEach { violation ->
            violation.cells.forEach { conflicts[it] = true }
        }
        return conflicts
    }

    fun explain(violation: RuleViolation, puzzle: SudokuPuzzle): String = when (violation.kind) {
        RuleKind.ROW -> {
            val row = violation.cells.first() / puzzle.size + 1
            "第 $row 行已经有数字 ${violation.value}，不能重复。"
        }
        RuleKind.COLUMN -> {
            val column = violation.cells.first() % puzzle.size + 1
            "第 $column 列已经有数字 ${violation.value}，不能重复。"
        }
        RuleKind.BOX -> "这个宫里已经有数字 ${violation.value}，不能重复。"
        RuleKind.DIAGONAL -> "这条对角线上已经有数字 ${violation.value}，不能重复。"
        RuleKind.KNIGHT -> {
            val first = violation.cells[0]
            val second = violation.cells.getOrElse(1) { first }
            "第 ${first / puzzle.size + 1} 行第 ${first % puzzle.size + 1} 列和第 ${second / puzzle.size + 1} 行第 ${second % puzzle.size + 1} 列是马步，不能都是 ${violation.value}。"
        }
        RuleKind.CAGE_DUPLICATE -> "这个虚线笼里已经有数字 ${violation.value}，不能重复。"
        RuleKind.CAGE_SUM -> "这个笼的目标是 ${violation.expectedSum}，现在的数字已经凑不出来。"
        RuleKind.SKYSCRAPER -> "从这一侧看，能看到的楼数和外面的线索 ${violation.clue} 不一致。"
    }

    fun explainCell(board: List<Int>, puzzle: SudokuPuzzle, index: Int): String? {
        val hits = violations(board, puzzle).filter { index in it.cells }
        if (hits.isEmpty()) return null
        return hits
            .sortedBy { violationPriority(it.kind) }
            .distinctBy { it.kind }
            .take(2)
            .joinToString("；") { explain(it, puzzle) }
    }

    fun isComplete(board: List<Int>, puzzle: SudokuPuzzle): Boolean =
        board.all { it in 1..puzzle.size } && conflictingCells(board, puzzle).none { it }

    fun candidates(board: IntArray, index: Int, puzzle: SudokuPuzzle): List<Int> =
        legalValues(board, index, puzzle, strict = true)

    fun basicCandidates(board: IntArray, index: Int, puzzle: SudokuPuzzle): List<Int> =
        legalValues(board, index, puzzle, strict = false)

    /**
     * 行、列、宫（以及对角线题的两条对角线）。供逻辑难度分析和技巧提示复用。
     */
    fun units(puzzle: SudokuPuzzle): List<List<Int>> = buildList {
        for (row in 0 until puzzle.size) add(rowIndices(row, puzzle.size))
        for (column in 0 until puzzle.size) add(columnIndices(column, puzzle.size))
        addAll(boxGroups(puzzle))
        if (puzzle.type == SudokuType.DIAGONAL) {
            add(mainDiagonal(puzzle.size))
            add(antiDiagonal(puzzle.size))
        }
    }

    fun boxes(puzzle: SudokuPuzzle): List<List<Int>> = boxGroups(puzzle)

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

    fun sideName(clueIndex: Int, size: Int): String = when {
        clueIndex < size -> "从上往下看第 ${clueIndex + 1} 列"
        clueIndex < size * 2 -> "从右往左看第 ${clueIndex - size + 1} 行"
        clueIndex < size * 3 -> "从下往上看第 ${clueIndex - size * 2 + 1} 列"
        else -> "从左往右看第 ${clueIndex - size * 3 + 1} 行"
    }

    private fun legalValues(
        board: IntArray,
        index: Int,
        puzzle: SudokuPuzzle,
        strict: Boolean,
    ): List<Int> {
        if (board[index] != 0) return emptyList()
        val working = board.copyOf()
        return (1..puzzle.size).filter { value ->
            working[index] = value
            val found = if (strict) {
                violations(working.toList(), puzzle)
            } else {
                basicViolations(working.toList(), puzzle)
            }
            val valid = found.none { index in it.cells }
            working[index] = 0
            valid
        }
    }

    private fun cageCombinationExists(values: List<Int>, target: Int, size: Int): Boolean {
        val filled = values.filter { it != 0 }
        if (filled.size != filled.toSet().size) return false
        val emptyCount = values.count { it == 0 }
        val remain = target - filled.sum()
        if (remain < 0) return false
        if (emptyCount == 0) return remain == 0
        val available = (1..size).filter { it !in filled.toSet() }.sorted()
        fun search(start: Int, left: Int, need: Int): Boolean {
            if (left == 0) return need == 0
            if (need <= 0 || available.size - start < left) return false
            for (position in start..available.size - left) {
                val value = available[position]
                if (value > need) break
                if (search(position + 1, left - 1, need - value)) return true
            }
            return false
        }
        return search(0, emptyCount, remain)
    }

    private fun skyscraperPartiallyImpossible(
        board: List<Int>,
        line: List<Int>,
        clue: Int,
        size: Int,
    ): Boolean {
        var tallest = 0
        var visible = 0
        for (cell in line) {
            val value = board[cell]
            if (value == 0) break
            if (value > tallest) {
                tallest = value
                visible++
            }
        }
        if (visible > clue) return true
        if (clue == 1 && board[line.first()] != 0 && board[line.first()] != size) return true
        val maxPosition = line.indexOfFirst { board[it] == size }
        if (maxPosition >= 0 && line.take(maxPosition).all { board[it] != 0 }) {
            val seen = visibleBuildings(line.take(maxPosition + 1).map { board[it] })
            if (seen != clue) return true
        }
        return false
    }

    private fun violationPriority(kind: RuleKind): Int = when (kind) {
        RuleKind.CAGE_SUM -> 0
        RuleKind.CAGE_DUPLICATE -> 1
        RuleKind.KNIGHT -> 2
        RuleKind.DIAGONAL -> 3
        RuleKind.SKYSCRAPER -> 4
        RuleKind.BOX -> 5
        RuleKind.ROW -> 6
        RuleKind.COLUMN -> 7
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

    private fun markDuplicates(
        board: List<Int>,
        group: List<Int>,
        kind: RuleKind,
        violations: MutableList<RuleViolation>,
    ) {
        val nonzero = group.filter { board[it] != 0 }
        nonzero.groupBy { board[it] }.filterValues { it.size > 1 }.forEach { (value, cells) ->
            violations += RuleViolation(kind, cells, value)
        }
    }
}

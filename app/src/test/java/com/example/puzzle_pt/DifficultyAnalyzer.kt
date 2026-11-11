package com.example.puzzle_pt

import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

enum class TechniqueLevel {
    SINGLES,
    LOCKED_AND_PAIRS,
    HIDDEN_PAIRS_AND_FISH,
}

data class DifficultyAnalysis(
    val solved: Boolean,
    val topLevel: TechniqueLevel,
)

/**
 * 测试用逻辑难度分析器。只使用儿童向技巧，不搜索、不试填。
 *
 * 技巧阶梯：
 * 1. 唯一候选、隐性唯一；
 * 2. 区块排除、显性数对；
 * 3. 隐性数对、X-Wing。
 *
 * 杀手笼和与摩天楼线索作为基础候选约束处理。
 */
object DifficultyAnalyzer {
    fun analyze(puzzle: SudokuPuzzle): DifficultyAnalysis {
        val board = puzzle.givens.toIntArray()
        val candidates = arrayOfNulls<MutableSet<Int>>(puzzle.cellCount)
        fun recompute() {
            for (index in 0 until puzzle.cellCount) {
                candidates[index] = if (board[index] == 0) {
                    filteredCandidates(puzzle, board, index).toMutableSet()
                } else {
                    mutableSetOf()
                }
            }
        }
        recompute()

        fun place(index: Int, value: Int) {
            board[index] = value
            recompute()
        }

        var level = TechniqueLevel.SINGLES
        while (true) {
            if (board.all { it != 0 }) break

            var nakedSingle = -1
            for (index in 0 until puzzle.cellCount) {
                if (board[index] == 0 && candidates[index]!!.size == 1) {
                    nakedSingle = index
                    break
                }
            }
            if (nakedSingle >= 0) {
                place(nakedSingle, candidates[nakedSingle]!!.first())
                continue
            }

            var hiddenSingle: Pair<Int, Int>? = null
            for (unit in SudokuRules.units(puzzle)) {
                for (value in 1..puzzle.size) {
                    if (unit.any { board[it] == value }) continue
                    val positions = unit.filter { index ->
                        board[index] == 0 && value in candidates[index]!!
                    }
                    if (positions.size == 1) {
                        hiddenSingle = positions.first() to value
                        break
                    }
                }
                if (hiddenSingle != null) break
            }
            if (hiddenSingle != null) {
                place(hiddenSingle.first, hiddenSingle.second)
                continue
            }

            if (applyLockedCandidates(puzzle, board, candidates)) {
                level = maxLevel(level, TechniqueLevel.LOCKED_AND_PAIRS)
                continue
            }
            if (applyNakedPairs(puzzle, candidates)) {
                level = maxLevel(level, TechniqueLevel.LOCKED_AND_PAIRS)
                continue
            }
            if (applyHiddenPairs(puzzle, candidates)) {
                level = maxLevel(level, TechniqueLevel.HIDDEN_PAIRS_AND_FISH)
                continue
            }
            if (applyXWing(puzzle, candidates)) {
                level = maxLevel(level, TechniqueLevel.HIDDEN_PAIRS_AND_FISH)
                continue
            }
            break
        }

        val solved = board.all { it in 1..puzzle.size } && SudokuRules.isComplete(board.toList(), puzzle)
        return DifficultyAnalysis(solved, level)
    }

    private fun maxLevel(a: TechniqueLevel, b: TechniqueLevel): TechniqueLevel =
        if (a.ordinal >= b.ordinal) a else b

    private fun filteredCandidates(
        puzzle: SudokuPuzzle,
        board: IntArray,
        index: Int,
    ): List<Int> {
        val base = SudokuRules.candidates(board, index, puzzle)
        if (base.isEmpty()) return base
        if (puzzle.type == SudokuType.SKYSCRAPER) {
            return base.filter { value ->
                board[index] = value
                val possible = puzzle.skyscraperClues.indices.all { lineIndex ->
                    val line = SudokuRules.skyscraperLines(puzzle.size)[lineIndex]
                    if (index !in line) {
                        true
                    } else {
                        lineCanSatisfy(puzzle, board, line, puzzle.skyscraperClues[lineIndex])
                    }
                }
                board[index] = 0
                possible
            }
        }
        if (puzzle.type == SudokuType.KILLER) {
            return base.filter { value ->
                board[index] = value
                val possible = puzzle.cages
                    .filter { cage -> cage.cells.any { it.index == index } }
                    .all { cage -> cageCanSatisfy(puzzle, board, cage.cells.map { it.index }, cage.sum) }
                board[index] = 0
                possible
            }
        }
        return base
    }

    private fun lineCanSatisfy(
        puzzle: SudokuPuzzle,
        board: IntArray,
        line: List<Int>,
        clue: Int,
    ): Boolean {
        if (clue == 0) return true
        val empty = line.filter { board[it] == 0 }
        if (empty.isEmpty()) {
            return SudokuRules.visibleBuildings(line.map { board[it] }) == clue
        }
        val allowed = empty.associateWith { index -> SudokuRules.candidates(board, index, puzzle) }
        val ordered = empty.sortedBy { allowed.getValue(it).size }
        val used = BooleanArray(puzzle.size + 1)
        line.filter { board[it] != 0 }.forEach { used[board[it]] = true }
        fun search(position: Int): Boolean {
            if (position == ordered.size) {
                return SudokuRules.visibleBuildings(line.map { board[it] }) == clue
            }
            val index = ordered[position]
            for (value in allowed.getValue(index)) {
                if (used[value]) continue
                if (line.any { board[it] == value }) continue
                used[value] = true
                board[index] = value
                if (search(position + 1)) {
                    board[index] = 0
                    used[value] = false
                    return true
                }
                board[index] = 0
                used[value] = false
            }
            return false
        }
        val result = search(0)
        return result
    }

    private fun cageCanSatisfy(
        puzzle: SudokuPuzzle,
        board: IntArray,
        cage: List<Int>,
        sum: Int,
    ): Boolean {
        val empty = cage.filter { board[it] == 0 }
        if (empty.isEmpty()) {
            return cage.sumOf { board[it] } == sum
        }
        val allowed = empty.associateWith { index -> SudokuRules.candidates(board, index, puzzle) }
        val ordered = empty.sortedBy { allowed.getValue(it).size }
        val used = BooleanArray(puzzle.size + 1)
        cage.filter { board[it] != 0 }.forEach { used[board[it]] = true }
        fun search(position: Int, currentSum: Int): Boolean {
            if (currentSum > sum) return false
            if (position == ordered.size) return currentSum == sum
            val index = ordered[position]
            for (value in allowed.getValue(index)) {
                if (used[value]) continue
                used[value] = true
                board[index] = value
                if (search(position + 1, currentSum + value)) {
                    board[index] = 0
                    used[value] = false
                    return true
                }
                board[index] = 0
                used[value] = false
            }
            return false
        }
        val result = search(0, cage.filter { board[it] != 0 }.sumOf { board[it] })
        return result
    }

    private fun applyLockedCandidates(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>?>,
    ): Boolean {
        var changed = false
        val boxes = SudokuRules.boxes(puzzle)
        fun remove(index: Int, value: Int): Boolean {
            return if (index in 0 until puzzle.cellCount &&
                board[index] == 0 &&
                candidates[index]!!.remove(value)
            ) {
                changed = true
                true
            } else {
                false
            }
        }

        for (box in boxes) {
            for (value in 1..puzzle.size) {
                if (box.any { board[it] == value }) continue
                val positions = box.filter { board[it] == 0 && value in candidates[it]!! }
                if (positions.size < 2) continue
                val rows = positions.map { it / puzzle.size }.distinct()
                val columns = positions.map { it % puzzle.size }.distinct()
                if (rows.size == 1) {
                    val row = rows.first()
                    for (column in 0 until puzzle.size) {
                        val index = row * puzzle.size + column
                        if (index !in box) remove(index, value)
                    }
                }
                if (columns.size == 1) {
                    val column = columns.first()
                    for (row in 0 until puzzle.size) {
                        val index = row * puzzle.size + column
                        if (index !in box) remove(index, value)
                    }
                }
            }
        }

        val boxOf = IntArray(puzzle.cellCount) { -1 }
        boxes.forEachIndexed { boxIndex, box -> box.forEach { boxOf[it] = boxIndex } }
        for (unit in SudokuRules.units(puzzle)) {
            val rows = unit.map { it / puzzle.size }.distinct()
            val columns = unit.map { it % puzzle.size }.distinct()
            // 区块排除只对行/列/宫成立；对角线单元不参与该技巧。
            if (rows.size != 1 && columns.size != 1) continue
            for (value in 1..puzzle.size) {
                if (unit.any { board[it] == value }) continue
                val positions = unit.filter { board[it] == 0 && value in candidates[it]!! }
                if (positions.size < 2) continue
                val boxIds = positions.map { boxOf[it] }.distinct()
                if (boxIds.size == 1) {
                    val box = boxes[boxIds.first()]
                    for (index in box) {
                        if (index !in unit) remove(index, value)
                    }
                }
            }
        }
        return changed
    }

    private fun applyNakedPairs(puzzle: SudokuPuzzle, candidates: Array<MutableSet<Int>?>): Boolean {
        var changed = false
        for (unit in SudokuRules.units(puzzle)) {
            val pairs = unit.filter { candidates[it]!!.size == 2 }
                .groupBy { candidates[it]!!.toSet() }
                .filterValues { it.size == 2 }
            for ((values, cells) in pairs) {
                for (index in unit) {
                    if (index in cells) continue
                    if (candidates[index]!!.removeAll(values)) changed = true
                }
            }
        }
        return changed
    }

    private fun applyHiddenPairs(puzzle: SudokuPuzzle, candidates: Array<MutableSet<Int>?>): Boolean {
        var changed = false
        for (unit in SudokuRules.units(puzzle)) {
            for (a in 1..puzzle.size) {
                val posA = unit.filter { a in candidates[it]!! }
                if (posA.size != 2) continue
                for (b in a + 1..puzzle.size) {
                    val posB = unit.filter { b in candidates[it]!! }
                    if (posA.toSet() != posB.toSet()) continue
                    for (index in posA) {
                        val keep = setOf(a, b)
                        val before = candidates[index]!!.size
                        candidates[index]!!.retainAll(keep)
                        if (candidates[index]!!.size != before) changed = true
                    }
                }
            }
        }
        return changed
    }

    private fun applyXWing(puzzle: SudokuPuzzle, candidates: Array<MutableSet<Int>?>): Boolean {
        var changed = false
        for (value in 1..puzzle.size) {
            val rowColumns = (0 until puzzle.size).associateWith { row ->
                (0 until puzzle.size).filter { column ->
                    val index = row * puzzle.size + column
                    value in candidates[index]!!
                }.toSet()
            }.filterValues { it.size == 2 }
            val rowEntries = rowColumns.entries.toList()
            for (i in rowEntries.indices) {
                for (j in i + 1 until rowEntries.size) {
                    if (rowEntries[i].value != rowEntries[j].value) continue
                    for (row in 0 until puzzle.size) {
                        if (row == rowEntries[i].key || row == rowEntries[j].key) continue
                        for (column in rowEntries[i].value) {
                            val index = row * puzzle.size + column
                            if (candidates[index]!!.remove(value)) changed = true
                        }
                    }
                }
            }

            val columnRows = (0 until puzzle.size).associateWith { column ->
                (0 until puzzle.size).filter { row ->
                    val index = row * puzzle.size + column
                    value in candidates[index]!!
                }.toSet()
            }.filterValues { it.size == 2 }
            val columnEntries = columnRows.entries.toList()
            for (i in columnEntries.indices) {
                for (j in i + 1 until columnEntries.size) {
                    if (columnEntries[i].value != columnEntries[j].value) continue
                    for (column in 0 until puzzle.size) {
                        if (column == columnEntries[i].key || column == columnEntries[j].key) continue
                        for (row in columnEntries[i].value) {
                            val index = row * puzzle.size + column
                            if (candidates[index]!!.remove(value)) changed = true
                        }
                    }
                }
            }
        }
        return changed
    }
}

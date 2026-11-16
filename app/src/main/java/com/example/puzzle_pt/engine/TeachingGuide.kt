package com.example.puzzle_pt.engine

import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.HintKind
import com.example.puzzle_pt.model.SudokuHint
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

enum class TechniqueLevel {
    SINGLES,
    LOCKED_AND_PAIRS,
    CAGE_COMBINATION,
    LINE_ENUMERATION,
    HIDDEN_PAIRS_AND_FISH,
}

enum class StepKind {
    NAKED_SINGLE,
    HIDDEN_SINGLE,
    KILLER_SINGLE,
    SKY_SINGLE,
    LOCKED,
    NAKED_PAIR,
    HIDDEN_PAIR,
    X_WING,
    CAGE_COMBO,
    SKY_LINE,
}

data class LogicStep(
    val level: TechniqueLevel,
    val kind: StepKind,
    val index: Int?,
    val value: Int?,
    val eliminated: Map<Int, Set<Int>>,
    val relatedCells: List<Int>,
    val explanation: String,
)

data class LogicAnalysis(
    val solved: Boolean,
    val steps: List<LogicStep>,
    val topLevel: TechniqueLevel,
)

/**
 * 儿童向逻辑提示和难度分析。
 * 这里记录的是一条已经找到的推理路径，不证明该题必须使用其中的技巧。
 * 搜索只用来判断当前盘面能否完成，不会把搜索到的数字当成提示。
 */
object TeachingGuide {
    private val cache = HashMap<String, LogicAnalysis>()

    fun hint(puzzle: SudokuPuzzle, board: List<Int>): SudokuHint {
        val violations = SudokuRules.violations(board, puzzle)
        if (violations.isNotEmpty()) {
            val focus = violations.minBy { violationPriority(it.kind) }
            val cell = focus.cells.first()
            return SudokuHint(
                index = cell,
                value = null,
                explanation = SudokuRules.explainCell(board, puzzle, cell).orEmpty(),
                relatedCells = focus.cells.toSet(),
                kind = HintKind.CONFLICT,
            )
        }
        if (board.none { it == 0 }) {
            return SudokuHint(null, null, "题目已经完成。", emptySet(), HintKind.NONE)
        }
        val probe = SudokuSolver.probe(puzzle, board.toIntArray(), limit = 1, maxNodes = 30_000)
        if (!probe.complete) {
            return SudokuHint(
                index = null,
                value = null,
                explanation = "这步暂时无法确认。可以先撤销最近填写的数字，再重新观察。",
                kind = HintKind.UNCERTAIN,
            )
        }
        if (probe.solutions == 0) {
            return SudokuHint(
                index = null,
                value = null,
                explanation = "现在没有标红的格子，但这盘已经无法完成。请检查之前填写的数字。",
                kind = HintKind.UNSOLVABLE,
            )
        }
        val step = analyze(puzzle, board).steps.firstOrNull() ?: return SudokuHint(
            index = null,
            value = null,
            explanation = "暂时没有可以用已学方法解释的提示。试着先找只剩一个候选的格子。",
            kind = HintKind.NONE,
        )
        return step.asHint()
    }

    fun analyze(puzzle: SudokuPuzzle, initial: List<Int> = puzzle.givens): LogicAnalysis {
        val board = initial.toIntArray()
        var candidates = candidateSets(puzzle, board)
        val steps = mutableListOf<LogicStep>()
        var guard = 0
        val limit = puzzle.cellCount * puzzle.size * 4
        while (guard++ < limit && board.any { it == 0 }) {
            val step = nextStep(puzzle, board, candidates) ?: break
            steps += step
            if (step.value != null && step.index != null) {
                board[step.index] = step.value
                candidates = candidateSets(puzzle, board)
            } else {
                step.eliminated.forEach { (cell, values) -> candidates[cell].removeAll(values) }
                if (candidates.indices.any { board[it] == 0 && candidates[it].isEmpty() }) break
            }
        }
        val solved = board.all { it in 1..puzzle.size } && SudokuRules.isComplete(board.toList(), puzzle)
        val top = steps.maxByOrNull { it.level.ordinal }?.level ?: TechniqueLevel.SINGLES
        return LogicAnalysis(solved, steps, top)
    }

    fun analysis(puzzle: SudokuPuzzle): LogicAnalysis {
        val key = puzzle.id + ":" + puzzle.givens.hashCode() + ":" + puzzle.difficulty.ordinal
        return cache.getOrPut(key) { analyze(puzzle) }
    }

    fun pathCost(puzzle: SudokuPuzzle): Int = pathCost(analysis(puzzle))

    fun pathCost(analysis: LogicAnalysis): Int {
        if (!analysis.solved) return 1_000_000
        val advanced = analysis.steps.count { it.level != TechniqueLevel.SINGLES }
        return analysis.topLevel.ordinal * 100 + advanced * 8 + analysis.steps.size
    }

    /**
     * 按已找到的推理路径分档，不是技巧必要性证明。
     */
    fun ratedDifficulty(puzzle: SudokuPuzzle, analysis: LogicAnalysis = analysis(puzzle)): Difficulty {
        val advanced = analysis.steps.filter { it.level != TechniqueLevel.SINGLES }
        val steps = analysis.steps.size
        return when (puzzle.type) {
            SudokuType.STANDARD, SudokuType.KILLER -> if (advanced.isNotEmpty()) {
                bandFromAdvanced(advanced)
            } else when {
                steps <= 16 -> Difficulty.EASY
                steps <= 21 -> Difficulty.MEDIUM
                else -> Difficulty.HARD
            }
            // 摩天楼：很短的单次楼数筛选仍算入门；多次筛选或长链算挑战。
            SudokuType.SKYSCRAPER -> when {
                advanced.isEmpty() || (advanced.size == 1 && steps <= 13) -> Difficulty.EASY
                advanced.size >= 2 || steps >= 22 -> Difficulty.HARD
                else -> Difficulty.MEDIUM
            }
            // 无马：短的直接排除是入门，推理变长或反复使用进阶技巧后进入挑战。
            SudokuType.ANTI_KNIGHT -> when {
                advanced.isEmpty() && steps <= 16 -> Difficulty.EASY
                advanced.size >= 3 || steps >= 25 -> Difficulty.HARD
                else -> Difficulty.MEDIUM
            }
            else -> if (advanced.isEmpty()) {
                Difficulty.EASY
            } else {
                bandFromAdvanced(advanced, hardAt = if (puzzle.type == SudokuType.SEVEN) 2 else 3)
            }
        }
    }

    private fun bandFromAdvanced(advanced: List<LogicStep>, hardAt: Int = 3): Difficulty {
        val variantSteps = advanced.count {
            it.level == TechniqueLevel.CAGE_COMBINATION || it.level == TechniqueLevel.LINE_ENUMERATION
        }
        val usesHardTechnique = advanced.any { it.level == TechniqueLevel.HIDDEN_PAIRS_AND_FISH }
        return if (usesHardTechnique || variantSteps >= 2 || advanced.size >= hardAt) {
            Difficulty.HARD
        } else {
            Difficulty.MEDIUM
        }
    }

    fun focusLabel(puzzle: SudokuPuzzle): String {
        val kinds = analysis(puzzle).steps.map { it.kind }.toSet()
        val text = analysis(puzzle).steps.joinToString(" ") { it.explanation }
        return when {
            StepKind.X_WING in kinds || StepKind.HIDDEN_PAIR in kinds -> "找出隐藏的规律"
            StepKind.SKY_LINE in kinds && (StepKind.CAGE_COMBO in kinds || StepKind.LOCKED in kinds) -> "综合几种线索"
            StepKind.SKY_LINE in kinds -> "搭配这一行能看到的楼"
            StepKind.CAGE_COMBO in kinds -> "搭配笼里剩下的和"
            StepKind.LOCKED in kinds || StepKind.NAKED_PAIR in kinds -> "用数对缩小范围"
            StepKind.KILLER_SINGLE in kinds -> "用笼角把空格算出来"
            StepKind.SKY_SINGLE in kinds -> "用外面的楼数直接判断"
            "对角线" in text -> "用对角线排除重复"
            "马步" in text -> "用马步排除相同数字"
            StepKind.HIDDEN_SINGLE in kinds -> "找这一行唯一能放的位置"
            else -> "找只剩一个的数字"
        }
    }

    fun forcedStep(puzzle: SudokuPuzzle, board: List<Int>, index: Int): LogicStep? {
        if (board.getOrNull(index) != 0) return null
        val cells = candidateSets(puzzle, board.toIntArray())
        killerSingle(puzzle, board.toIntArray(), cells)?.takeIf { it.index == index }?.let { return it }
        skySingle(puzzle, board.toIntArray(), cells)?.takeIf { it.index == index }?.let { return it }
        if (cells[index].size == 1) return nakedStep(puzzle, board.toIntArray(), cells, index)
        return hiddenSingle(puzzle, board.toIntArray(), cells)?.takeIf { it.index == index }
    }

    private fun nextStep(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? =
        nakedAnywhere(puzzle, board, candidates)
            ?: hiddenSingle(puzzle, board, candidates)
            ?: killerSingle(puzzle, board, candidates)
            ?: skySingle(puzzle, board, candidates)
            ?: lockedStep(puzzle, board, candidates)
            ?: nakedPairStep(puzzle, candidates)
            ?: hiddenPairStep(puzzle, candidates)
            ?: xWingStep(puzzle, candidates)
            ?: cageComboStep(puzzle, board, candidates)
            ?: skyLineStep(puzzle, board, candidates)

    private fun candidateSets(puzzle: SudokuPuzzle, board: IntArray): Array<MutableSet<Int>> =
        Array(puzzle.cellCount) { index ->
            if (board[index] == 0) SudokuRules.basicCandidates(board, index, puzzle).toMutableSet() else mutableSetOf()
        }

    private fun nakedAnywhere(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        val index = candidates.indices.firstOrNull { board[it] == 0 && candidates[it].size == 1 } ?: return null
        return nakedStep(puzzle, board, candidates, index)
    }

    private fun nakedStep(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
        index: Int,
    ): LogicStep {
        val value = candidates[index].first()
        val reasons = (1..puzzle.size).filter { it != value }.mapNotNull { digit ->
            val trial = board.copyOf()
            trial[index] = digit
            SudokuRules.basicViolations(trial.toList(), puzzle).firstOrNull { index in it.cells }
        }.distinctBy { it.kind to it.value }
        val variantFirst = reasons.sortedBy { reason ->
            if (reason.kind == com.example.puzzle_pt.model.RuleKind.ROW ||
                reason.kind == com.example.puzzle_pt.model.RuleKind.COLUMN ||
                reason.kind == com.example.puzzle_pt.model.RuleKind.BOX
            ) 1 else 0
        }
        val where = place(index, puzzle.size)
        val because = variantFirst.take(3).joinToString("") { SudokuRules.explain(it, puzzle) }
        return LogicStep(
            level = TechniqueLevel.SINGLES,
            kind = StepKind.NAKED_SINGLE,
            index = index,
            value = value,
            eliminated = mapOf(index to ((1..puzzle.size).toSet() - value)),
            relatedCells = SudokuRules.relatedCells(index, puzzle).toList(),
            explanation = "$where 只剩下 $value。$because",
        )
    }

    private fun hiddenSingle(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        for (unit in SudokuRules.units(puzzle)) {
            for (value in 1..puzzle.size) {
                if (unit.any { board[it] == value }) continue
                val positions = unit.filter { board[it] == 0 && value in candidates[it] }
                if (positions.size != 1) continue
                val index = positions.first()
                val unitName = unitName(puzzle, unit, index)
                return LogicStep(
                    level = TechniqueLevel.SINGLES,
                    kind = StepKind.HIDDEN_SINGLE,
                    index = index,
                    value = value,
                    eliminated = mapOf(index to (candidates[index] - value)),
                    relatedCells = unit,
                    explanation = "数字 $value 在${unitName}只剩下${place(index, puzzle.size)}可以放。",
                )
            }
        }
        return null
    }

    private fun killerSingle(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        if (puzzle.type != SudokuType.KILLER) return null
        for (cage in puzzle.cages) {
            val cells = cage.cells.map { it.index }
            val empty = cells.filter { board[it] == 0 }
            if (empty.size != 1) continue
            val filled = cells.filter { board[it] != 0 }.map { board[it] }
            if (filled.size != filled.toSet().size) continue
            val need = cage.sum - filled.sum()
            val index = empty.first()
            if (need !in 1..puzzle.size || need !in candidates[index] || candidates[index].size <= 1) continue
            val removed = candidates[index] - need
            val filledText = filled.joinToString("、")
            return LogicStep(
                level = TechniqueLevel.SINGLES,
                kind = StepKind.KILLER_SINGLE,
                index = index,
                value = need,
                eliminated = mapOf(index to removed),
                relatedCells = cells,
                explanation = "这个笼的目标是 ${cage.sum}，已经有 $filledText。" +
                    "${cage.sum} 减去 ${filled.sum()} 等于 $need，所以${place(index, puzzle.size)}填 $need。",
            )
        }
        return null
    }

    private fun skySingle(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        if (puzzle.type != SudokuType.SKYSCRAPER) return null
        val lines = SudokuRules.skyscraperLines(puzzle.size)
        var elimination: LogicStep? = null
        for (clueIndex in lines.indices) {
            val clue = puzzle.skyscraperClues[clueIndex]
            if (clue <= 0) continue
            val line = lines[clueIndex]
            val side = SudokuRules.sideName(clueIndex, puzzle.size)
            if (clue == 1) {
                val cell = line.first()
                if (board[cell] == 0 && puzzle.size in candidates[cell] && candidates[cell].size > 1) {
                    return skyPlacement(
                        cell,
                        puzzle.size,
                        candidates[cell] - puzzle.size,
                        line,
                        "$side，外面是 1。第一格必须是最高的 ${puzzle.size} 楼，所以这里填 ${puzzle.size}。",
                    )
                }
            }
            if (clue == puzzle.size && line.indices.all { position ->
                    val cell = line[position]
                    board[cell] == 0 || board[cell] == position + 1
                }
            ) {
                for (position in line.indices) {
                    val expected = position + 1
                    val cell = line[position]
                    if (board[cell] != 0) continue
                    if (expected in candidates[cell] && candidates[cell] != setOf(expected)) {
                        return skyPlacement(
                            cell,
                            expected,
                            candidates[cell] - expected,
                            line,
                            "$side，外面是 ${puzzle.size}，楼要从 1 排到 ${puzzle.size}。第 ${position + 1} 格只能是 $expected。",
                        )
                    }
                    break
                }
            }
            if (clue > 1 && elimination == null) {
                val cell = line.first()
                if (board[cell] == 0 && puzzle.size in candidates[cell]) {
                    val remain = candidates[cell] - puzzle.size
                    if (remain.isNotEmpty()) {
                        elimination = LogicStep(
                            level = TechniqueLevel.SINGLES,
                            kind = StepKind.SKY_SINGLE,
                            index = cell,
                            value = remain.singleOrNull(),
                            eliminated = mapOf(cell to setOf(puzzle.size)),
                            relatedCells = line,
                            explanation = if (remain.size == 1) {
                                "$side，外面是 $clue，不是 1，第一格不能是 ${puzzle.size}。所以这里填 ${remain.first()}。"
                            } else {
                                "$side，外面是 $clue，不是 1。第一格不能是最高的 ${puzzle.size}，还剩下 ${remain.sorted().joinToString("、")}。"
                            },
                        )
                    }
                }
            }
        }
        return elimination
    }

    private fun skyPlacement(
        cell: Int,
        value: Int,
        removed: Set<Int>,
        line: List<Int>,
        explanation: String,
    ): LogicStep = LogicStep(
        level = TechniqueLevel.SINGLES,
        kind = StepKind.SKY_SINGLE,
        index = cell,
        value = value,
        eliminated = mapOf(cell to removed),
        relatedCells = line,
        explanation = explanation,
    )

    private fun lockedStep(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        val boxes = SudokuRules.boxes(puzzle)
        for (box in boxes) {
            for (value in 1..puzzle.size) {
                if (box.any { board[it] == value }) continue
                val positions = box.filter { board[it] == 0 && value in candidates[it] }
                if (positions.size < 2) continue
                val rows = positions.map { it / puzzle.size }.distinct()
                val columns = positions.map { it % puzzle.size }.distinct()
                if (rows.size == 1) {
                    val row = rows.first()
                    val victims = (0 until puzzle.size).map { row * puzzle.size + it }
                        .filter { it !in box && board[it] == 0 && value in candidates[it] }
                    if (victims.isNotEmpty()) {
                        return eliminationStep(
                            StepKind.LOCKED,
                            victims.first(),
                            victims.associateWith { setOf(value) },
                            box + victims,
                            "数字 $value 在这个宫里只出现在第 ${row + 1} 行，所以这一行其他宫不能再填 $value。",
                        )
                    }
                }
                if (columns.size == 1) {
                    val column = columns.first()
                    val victims = (0 until puzzle.size).map { it * puzzle.size + column }
                        .filter { it !in box && board[it] == 0 && value in candidates[it] }
                    if (victims.isNotEmpty()) {
                        return eliminationStep(
                            StepKind.LOCKED,
                            victims.first(),
                            victims.associateWith { setOf(value) },
                            box + victims,
                            "数字 $value 在这个宫里只出现在第 ${column + 1} 列，所以这一列其他宫不能再填 $value。",
                        )
                    }
                }
            }
        }
        val boxOf = IntArray(puzzle.cellCount) { -1 }
        boxes.forEachIndexed { boxIndex, box -> box.forEach { boxOf[it] = boxIndex } }
        for (unit in SudokuRules.units(puzzle)) {
            val rows = unit.map { it / puzzle.size }.distinct()
            val columns = unit.map { it % puzzle.size }.distinct()
            if (rows.size != 1 && columns.size != 1) continue
            for (value in 1..puzzle.size) {
                if (unit.any { board[it] == value }) continue
                val positions = unit.filter { board[it] == 0 && value in candidates[it] }
                if (positions.size < 2) continue
                val boxIds = positions.map { boxOf[it] }.distinct()
                if (boxIds.size != 1) continue
                val box = boxes[boxIds.first()]
                val victims = box.filter { it !in unit && board[it] == 0 && value in candidates[it] }
                if (victims.isEmpty()) continue
                val line = if (rows.size == 1) "第 ${rows.first() + 1} 行" else "第 ${columns.first() + 1} 列"
                return eliminationStep(
                    StepKind.LOCKED,
                    victims.first(),
                    victims.associateWith { setOf(value) },
                    unit + victims,
                    "数字 $value 在${line}只出现在同一个宫，所以这个宫的其他格子不能再填 $value。",
                )
            }
        }
        return null
    }

    private fun nakedPairStep(
        puzzle: SudokuPuzzle,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        for (unit in SudokuRules.units(puzzle)) {
            val pairs = unit.filter { candidates[it].size == 2 }
                .groupBy { candidates[it].toSet() }
                .filterValues { it.size == 2 }
            for ((values, cells) in pairs) {
                val eliminated = mutableMapOf<Int, Set<Int>>()
                for (index in unit) {
                    if (index in cells) continue
                    val removed = candidates[index].intersect(values)
                    if (removed.isNotEmpty()) eliminated[index] = removed
                }
                if (eliminated.isEmpty()) continue
                val shown = values.sorted().joinToString(" 和 ")
                return eliminationStep(
                    StepKind.NAKED_PAIR,
                    eliminated.keys.first(),
                    eliminated,
                    unit,
                    "有两格都只剩下 $shown，所以同一组里的其他格子不能再填这两个数。还要继续观察剩下的候选。",
                )
            }
        }
        return null
    }

    private fun hiddenPairStep(
        puzzle: SudokuPuzzle,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        for (unit in SudokuRules.units(puzzle)) {
            for (first in 1..puzzle.size) {
                val firstPositions = unit.filter { first in candidates[it] }
                if (firstPositions.size != 2) continue
                for (second in first + 1..puzzle.size) {
                    val secondPositions = unit.filter { second in candidates[it] }
                    if (firstPositions.toSet() != secondPositions.toSet()) continue
                    val eliminated = mutableMapOf<Int, Set<Int>>()
                    for (index in firstPositions) {
                        val removed = candidates[index] - setOf(first, second)
                        if (removed.isNotEmpty()) eliminated[index] = removed
                    }
                    if (eliminated.isEmpty()) continue
                    return eliminationStep(
                        StepKind.HIDDEN_PAIR,
                        eliminated.keys.first(),
                        eliminated,
                        unit,
                        "数字 $first 和 $second 在这一组里只出现在同样的两格，这两格可以先排除其他数字。",
                    )
                }
            }
        }
        return null
    }

    private fun xWingStep(
        puzzle: SudokuPuzzle,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        for (value in 1..puzzle.size) {
            val rowColumns = (0 until puzzle.size).mapNotNull { row ->
                val columns = (0 until puzzle.size).filter { column ->
                    value in candidates[row * puzzle.size + column]
                }
                if (columns.size == 2) row to columns else null
            }
            for (left in rowColumns.indices) {
                for (right in left + 1 until rowColumns.size) {
                    if (rowColumns[left].second != rowColumns[right].second) continue
                    val eliminated = mutableMapOf<Int, Set<Int>>()
                    for (row in 0 until puzzle.size) {
                        if (row == rowColumns[left].first || row == rowColumns[right].first) continue
                        for (column in rowColumns[left].second) {
                            val index = row * puzzle.size + column
                            if (value in candidates[index]) eliminated[index] = setOf(value)
                        }
                    }
                    if (eliminated.isEmpty()) continue
                    return eliminationStep(
                        StepKind.X_WING,
                        eliminated.keys.first(),
                        eliminated,
                        eliminated.keys.toList(),
                        "数字 $value 在两条横行里正好卡在同样的两列，其他横行的这两列就不能再填 $value。",
                    )
                }
            }
            val columnRows = (0 until puzzle.size).mapNotNull { column ->
                val rows = (0 until puzzle.size).filter { row ->
                    value in candidates[row * puzzle.size + column]
                }
                if (rows.size == 2) column to rows else null
            }
            for (left in columnRows.indices) {
                for (right in left + 1 until columnRows.size) {
                    if (columnRows[left].second != columnRows[right].second) continue
                    val eliminated = mutableMapOf<Int, Set<Int>>()
                    for (column in 0 until puzzle.size) {
                        if (column == columnRows[left].first || column == columnRows[right].first) continue
                        for (row in columnRows[left].second) {
                            val index = row * puzzle.size + column
                            if (value in candidates[index]) eliminated[index] = setOf(value)
                        }
                    }
                    if (eliminated.isEmpty()) continue
                    return eliminationStep(
                        StepKind.X_WING,
                        eliminated.keys.first(),
                        eliminated,
                        eliminated.keys.toList(),
                        "数字 $value 在两条竖列里正好卡在同样的两行，其他竖列的这两行就不能再填 $value。",
                    )
                }
            }
        }
        return null
    }

    private fun cageComboStep(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        if (puzzle.type != SudokuType.KILLER) return null
        for (cage in puzzle.cages) {
            val cells = cage.cells.map { it.index }
            val empty = cells.filter { board[it] == 0 }
            if (empty.size !in 2..4) continue
            val possible = mutableMapOf<Int, MutableSet<Int>>()
            empty.forEach { possible[it] = mutableSetOf() }
            val used = cells.map { board[it] }.filter { it != 0 }.toMutableSet()
            val filledSum = used.sum()
            val chosen = IntArray(empty.size)
            fun search(position: Int, sum: Int) {
                if (sum > cage.sum) return
                if (position == empty.size) {
                    if (sum == cage.sum) {
                        empty.forEachIndexed { offset, cell -> possible.getValue(cell) += chosen[offset] }
                    }
                    return
                }
                for (value in candidates[empty[position]]) {
                    if (value in used) continue
                    used += value
                    chosen[position] = value
                    search(position + 1, sum + value)
                    used -= value
                }
            }
            search(0, filledSum)
            if (empty.any { possible.getValue(it).isEmpty() }) continue
            val eliminated = mutableMapOf<Int, Set<Int>>()
            for (cell in empty) {
                val removed = candidates[cell] - possible.getValue(cell)
                if (removed.isNotEmpty()) eliminated[cell] = removed
            }
            if (eliminated.isEmpty()) continue
            val focus = eliminated.keys.minBy { candidates[it].size - eliminated.getValue(it).size }
            val remain = candidates[focus] - eliminated.getValue(focus)
            if (remain.isEmpty()) continue
            return LogicStep(
                level = TechniqueLevel.CAGE_COMBINATION,
                kind = StepKind.CAGE_COMBO,
                index = focus,
                value = remain.singleOrNull(),
                eliminated = eliminated,
                relatedCells = cells,
                explanation = if (remain.size == 1) {
                    "这个笼还要凑出 ${cage.sum}，数字不能重复。能加对的组合里，${place(focus, puzzle.size)}只能是 ${remain.first()}。"
                } else {
                    "这个笼还要凑出 ${cage.sum}。${place(focus, puzzle.size)}要先排除 ${eliminated.getValue(focus).sorted().joinToString("、")}，还剩下 ${remain.sorted().joinToString("、")}。"
                },
            )
        }
        return null
    }

    private fun skyLineStep(
        puzzle: SudokuPuzzle,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): LogicStep? {
        if (puzzle.type != SudokuType.SKYSCRAPER) return null
        val lines = SudokuRules.skyscraperLines(puzzle.size)
        for (clueIndex in lines.indices) {
            val clue = puzzle.skyscraperClues[clueIndex]
            if (clue <= 0) continue
            val line = lines[clueIndex]
            val empty = line.filter { board[it] == 0 }
            if (empty.isEmpty()) continue
            val possible = empty.associateWith { mutableSetOf<Int>() }
            val local = board.copyOf()
            fun search(position: Int) {
                if (position == line.size) {
                    if (SudokuRules.visibleBuildings(line.map { local[it] }) == clue) {
                        empty.forEach { possible.getValue(it) += local[it] }
                    }
                    return
                }
                val cell = line[position]
                if (local[cell] != 0) {
                    search(position + 1)
                    return
                }
                for (value in candidates[cell]) {
                    if (line.any { local[it] == value }) continue
                    local[cell] = value
                    search(position + 1)
                    local[cell] = 0
                }
            }
            search(0)
            if (empty.any { possible.getValue(it).isEmpty() }) continue
            val eliminated = mutableMapOf<Int, Set<Int>>()
            for (cell in empty) {
                val removed = candidates[cell] - possible.getValue(cell)
                if (removed.isNotEmpty()) eliminated[cell] = removed
            }
            if (eliminated.isEmpty()) continue
            val focus = eliminated.keys.minBy { candidates[it].size - eliminated.getValue(it).size }
            val remain = candidates[focus] - eliminated.getValue(focus)
            if (remain.isEmpty()) continue
            val side = SudokuRules.sideName(clueIndex, puzzle.size)
            return LogicStep(
                level = TechniqueLevel.LINE_ENUMERATION,
                kind = StepKind.SKY_LINE,
                index = focus,
                value = remain.singleOrNull(),
                eliminated = eliminated,
                relatedCells = line,
                explanation = if (remain.size == 1) {
                    "$side，外面是 $clue。把这一排还空着的楼搭配后，${place(focus, puzzle.size)}只能是 ${remain.first()}。"
                } else {
                    "$side，外面是 $clue。${place(focus, puzzle.size)}要先排除 ${eliminated.getValue(focus).sorted().joinToString("、")}，还剩下 ${remain.sorted().joinToString("、")}。"
                },
            )
        }
        return null
    }

    private fun eliminationStep(
        kind: StepKind,
        index: Int,
        eliminated: Map<Int, Set<Int>>,
        related: List<Int>,
        explanation: String,
    ): LogicStep = LogicStep(
        level = if (kind == StepKind.HIDDEN_PAIR || kind == StepKind.X_WING) {
            TechniqueLevel.HIDDEN_PAIRS_AND_FISH
        } else {
            TechniqueLevel.LOCKED_AND_PAIRS
        },
        kind = kind,
        index = index,
        value = null,
        eliminated = eliminated,
        relatedCells = related,
        explanation = explanation,
    )

    private fun unitName(puzzle: SudokuPuzzle, unit: List<Int>, index: Int): String {
        val size = puzzle.size
        val rows = unit.map { it / size }.distinct()
        val columns = unit.map { it % size }.distinct()
        return when {
            rows.size == 1 && unit.size == size -> "第 ${rows.first() + 1} 行"
            columns.size == 1 && unit.size == size -> "第 ${columns.first() + 1} 列"
            puzzle.type == SudokuType.DIAGONAL && unit.all { it / size == it % size } -> "主对角线"
            puzzle.type == SudokuType.DIAGONAL && unit.all { it / size + it % size == size - 1 } -> "另一条对角线"
            else -> "这个宫"
        }
    }

    private fun place(index: Int, size: Int): String = "第 ${index / size + 1} 行第 ${index % size + 1} 列"

    private fun violationPriority(kind: com.example.puzzle_pt.model.RuleKind): Int = when (kind) {
        com.example.puzzle_pt.model.RuleKind.CAGE_SUM -> 0
        com.example.puzzle_pt.model.RuleKind.CAGE_DUPLICATE -> 1
        com.example.puzzle_pt.model.RuleKind.KNIGHT -> 2
        com.example.puzzle_pt.model.RuleKind.DIAGONAL -> 3
        com.example.puzzle_pt.model.RuleKind.SKYSCRAPER -> 4
        com.example.puzzle_pt.model.RuleKind.BOX -> 5
        com.example.puzzle_pt.model.RuleKind.ROW -> 6
        com.example.puzzle_pt.model.RuleKind.COLUMN -> 7
    }

    private fun LogicStep.asHint(): SudokuHint {
        val highlighted = (relatedCells + eliminated.keys).toSet()
        return if (value != null) {
            SudokuHint(index, value, explanation, highlighted, HintKind.PLACE)
        } else {
            SudokuHint(index, null, explanation, highlighted, HintKind.ELIMINATE)
        }
    }
}

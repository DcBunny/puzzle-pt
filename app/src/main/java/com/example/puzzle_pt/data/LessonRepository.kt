package com.example.puzzle_pt.data

import com.example.puzzle_pt.engine.TeachingGuide
import com.example.puzzle_pt.model.CellPosition
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.KillerCage
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

enum class LessonVisualKind {
    NUMBER_FRIENDS,
    GROUP_TOUR,
    STANDARD_DUPLICATE,
    STANDARD_MISSING,
    KILLER_HOUSE,
    KILLER_ADD,
    KILLER_MAKE_SUM,
    KILLER_DUPLICATE,
    SKY_HEIGHTS,
    SKY_COUNT,
    SKY_QUIZ,
    SKY_SIX_FIRST,
    DIAGONAL_LINES,
    DIAGONAL_NUMBERS,
    DIAGONAL_TRACE,
    DIAGONAL_DUPLICATE,
    KNIGHT_MOVE,
    KNIGHT_FIND_MOVES,
    KNIGHT_SAME,
    KNIGHT_CHOOSE_SAFE,
}

enum class LessonInteraction {
    NONE,
    FIND_STANDARD_DUPLICATE,
    FIND_STANDARD_MISSING,
    MAKE_KILLER_SUM,
    FIND_KILLER_DUPLICATE,
    COUNT_SKYSCRAPERS,
    TRACE_DIAGONAL,
    FIND_DIAGONAL_DUPLICATE,
    FIND_KNIGHT_MOVES,
    CHOOSE_SAFE_KNIGHT_NUMBER,
}

data class LessonStep(
    val title: String,
    val explanation: String,
    val action: String = "",
    val visual: LessonVisualKind,
    val interaction: LessonInteraction = LessonInteraction.NONE,
)

object LessonRepository {
    fun stepsFor(type: SudokuType): List<LessonStep> {
        if (!type.hasLesson) return emptyList()
        val steps = when (type) {
            SudokuType.KILLER -> listOf(
                LessonStep(
                    "认识加法小屋",
                    "虚线笼里的数字不能重复，加起来等于笼角的数。",
                    "看动画：1、2、3 加起来正好是 6。",
                    LessonVisualKind.KILLER_ADD,
                ),
                LessonStep(
                    "找出重复数字",
                    "同一个小屋里，每个数字只能出现一次。",
                    "点出小屋里重复出现的数字。",
                    LessonVisualKind.KILLER_DUPLICATE,
                    LessonInteraction.FIND_KILLER_DUPLICATE,
                ),
                LessonStep(
                    "算出小屋空格",
                    "用笼角减去已经有的数字，再检查有没有重复。",
                    "第一行小屋已有 1 和 4，屋角是 10。",
                    LessonVisualKind.KILLER_MAKE_SUM,
                    LessonInteraction.MAKE_KILLER_SUM,
                ),
            )
            SudokuType.SKYSCRAPER -> listOf(
                LessonStep(
                    "数字变成小楼",
                    "数字是几，小楼就有几层；高楼会挡住矮楼。",
                    "从左边看，数一数能看到几栋楼。",
                    LessonVisualKind.SKY_HEIGHTS,
                    LessonInteraction.COUNT_SKYSCRAPERS,
                ),
                LessonStep(
                    "线索对不上",
                    "最高的楼在最前时，只能看到 1 栋。",
                    "如果外面不是 1，就不能把 6 放在第一格。",
                    LessonVisualKind.SKY_SIX_FIRST,
                ),
                LessonStep(
                    "棋盘边缘的数字",
                    "边缘数字表示从这一侧看过去，能看到几栋楼。",
                    "第一行从左边看，边缘线索是 3。",
                    LessonVisualKind.SKY_QUIZ,
                    LessonInteraction.COUNT_SKYSCRAPERS,
                ),
            )
            SudokuType.DIAGONAL -> listOf(
                LessonStep(
                    "找到两条斜线",
                    "从角到角的两条主对角线，也要包含 1 到 6。",
                    "蓝色斜线就是多出来的两组格子。",
                    LessonVisualKind.DIAGONAL_LINES,
                ),
                LessonStep(
                    "沿着斜线点一点",
                    "斜着相邻的格子连成一条，数字也不能重复。",
                    "把主对角线上的 6 个格子都点亮。",
                    LessonVisualKind.DIAGONAL_TRACE,
                    LessonInteraction.TRACE_DIAGONAL,
                ),
                LessonStep(
                    "斜线上缺哪个",
                    "主对角线上已有 5、1、3、6、2，还缺一个数字。",
                    "找出粗框标出的格子缺少的数字。",
                    LessonVisualKind.DIAGONAL_DUPLICATE,
                    LessonInteraction.FIND_DIAGONAL_DUPLICATE,
                ),
            )
            SudokuType.ANTI_KNIGHT -> listOf(
                LessonStep(
                    "认识马步",
                    "走两格再拐一格，像象棋里的马走日。",
                    "看动画，记住这种跳跃。",
                    LessonVisualKind.KNIGHT_MOVE,
                ),
                LessonStep(
                    "找出马步落点",
                    "和起点隔着这种走法的格子，就是马步落点。",
                    "把小马能跳到的格子都点出来。",
                    LessonVisualKind.KNIGHT_FIND_MOVES,
                    LessonInteraction.FIND_KNIGHT_MOVES,
                ),
                LessonStep(
                    "排除相同数字",
                    "按普通规则可填 2 或 4，马步上已经有 2。",
                    "第 1 行第 2 列看看马步落点后填数。",
                    LessonVisualKind.KNIGHT_CHOOSE_SAFE,
                    LessonInteraction.CHOOSE_SAFE_KNIGHT_NUMBER,
                ),
            )
            SudokuType.STANDARD, SudokuType.SEVEN, SudokuType.NINE -> emptyList()
        }
        return steps + LessonStep(
            "你来试一试",
            when (type) {
                SudokuType.KILLER -> "标出的笼子还差多少，不能只看这一行。"
                SudokuType.SKYSCRAPER -> "外侧线索是 1 时，要先想到最高的那栋楼。"
                SudokuType.DIAGONAL -> "这一格有两个普通候选，对角线能排除一个。"
                SudokuType.ANTI_KNIGHT -> "先看普通候选，再排除马步上已有的数字。"
                SudokuType.STANDARD -> "看看这一行、这一列和这一宫，找出缺少的数字。"
                SudokuType.SEVEN, SudokuType.NINE -> ""
            },
            "给粗框标出的格子填上正确的数字。",
            LessonVisualKind.NUMBER_FRIENDS,
        )
    }

    fun examplePuzzle(type: SudokuType): SudokuPuzzle = when (type) {
        SudokuType.KILLER -> lesson(
            "lesson-killer-example",
            SudokuType.KILLER,
            KILLER_SOLUTION,
            KILLER_SOLUTION,
            cages = listOf(cage(0, 1, 2)),
        )
        SudokuType.SKYSCRAPER -> lesson(
            "lesson-skyscraper-example",
            SudokuType.SKYSCRAPER,
            SKY_EASY_SOLUTION,
            SKY_EASY_SOLUTION,
            clues = SKY_EASY_CLUES,
        )
        SudokuType.DIAGONAL -> lesson(
            "lesson-diagonal-example",
            SudokuType.DIAGONAL,
            DIAGONAL_SOLUTION,
            DIAGONAL_SOLUTION,
        )
        SudokuType.ANTI_KNIGHT -> lesson(
            "lesson-knight-example",
            SudokuType.ANTI_KNIGHT,
            blank(KNIGHT_SOLUTION, 0, 1, 31),
            KNIGHT_SOLUTION,
        )
        else -> error("$type 没有独立教学示例")
    }

    fun guidedPuzzle(type: SudokuType): SudokuPuzzle = when (type) {
        SudokuType.KILLER -> lesson(
            "lesson-killer-guided",
            SudokuType.KILLER,
            blank(KILLER_SOLUTION, 0, 24, 29, 32),
            KILLER_SOLUTION,
            cages = listOf(cage(24, 30)),
        )
        SudokuType.SKYSCRAPER -> lesson(
            "lesson-skyscraper-guided",
            SudokuType.SKYSCRAPER,
            blank(SKY_HARD_SOLUTION, 0, 1, 24),
            SKY_HARD_SOLUTION,
            clues = SKY_HARD_CLUES,
        )
        SudokuType.DIAGONAL -> lesson(
            "lesson-diagonal-guided",
            SudokuType.DIAGONAL,
            blank(DIAGONAL_SOLUTION, 3, 15, 17),
            DIAGONAL_SOLUTION,
        )
        SudokuType.ANTI_KNIGHT -> lesson(
            "lesson-knight-guided",
            SudokuType.ANTI_KNIGHT,
            blank(KNIGHT_SOLUTION, 0, 8, 11, 14),
            KNIGHT_SOLUTION,
        )
        else -> error("$type 没有独立试填")
    }

    fun guidedTargetIndex(type: SudokuType): Int = when (type) {
        SudokuType.KILLER -> 24
        SudokuType.SKYSCRAPER -> 0
        SudokuType.DIAGONAL -> 15
        SudokuType.ANTI_KNIGHT -> 8
        else -> guidedPuzzle(type).givens.indexOf(0)
    }

    fun guidedFillPrompt(type: SudokuType): String {
        val puzzle = guidedPuzzle(type)
        val index = guidedTargetIndex(type)
        val row = index / puzzle.size + 1
        val column = index % puzzle.size + 1
        return "请填写粗框格：第 $row 行、第 $column 列"
    }

    const val SUCCESS_FEEDBACK = "你记住了这条规则，真棒！"

    fun guidedFocus(type: SudokuType): Set<Int> {
        val puzzle = guidedPuzzle(type)
        val target = guidedTargetIndex(type)
        return when (type) {
            SudokuType.KILLER -> puzzle.cages.first().cells.map { it.index }.toSet()
            SudokuType.SKYSCRAPER -> (0 until puzzle.size).toSet()
            SudokuType.DIAGONAL -> (0 until puzzle.size).map { it * puzzle.size + (puzzle.size - 1 - it) }.toSet()
            SudokuType.ANTI_KNIGHT -> com.example.puzzle_pt.engine.SudokuRules.knightCells(target, puzzle.size) + target
            else -> setOf(target)
        }
    }

    fun guidedSuccess(type: SudokuType): String {
        val puzzle = guidedPuzzle(type)
        val target = guidedTargetIndex(type)
        return TeachingGuide.forcedStep(puzzle, puzzle.givens, target)?.explanation
            ?: "填对啦！"
    }

    private fun lesson(
        id: String,
        type: SudokuType,
        givens: List<Int>,
        solution: List<Int>,
        cages: List<KillerCage> = emptyList(),
        clues: List<Int> = emptyList(),
    ): SudokuPuzzle = SudokuPuzzle(
        id = id,
        type = type,
        difficulty = Difficulty.EASY,
        givens = givens,
        solution = solution,
        cages = cages,
        skyscraperClues = clues,
    )

    private fun cage(vararg indices: Int): KillerCage {
        val cells = indices.map { index -> CellPosition(index / 6, index % 6) }
        return KillerCage(indices.sumOf { KILLER_SOLUTION[it] }, cells)
    }

    private fun blank(solution: List<Int>, vararg indices: Int): List<Int> =
        solution.toMutableList().apply { indices.forEach { this[it] = 0 } }

    private val KILLER_SOLUTION = ints(
        1, 4, 5, 2, 3, 6,
        3, 2, 6, 4, 1, 5,
        5, 1, 4, 3, 6, 2,
        6, 3, 2, 1, 5, 4,
        2, 6, 3, 5, 4, 1,
        4, 5, 1, 6, 2, 3,
    )
    private val SKY_EASY_SOLUTION = ints(
        2, 5, 1, 4, 3, 6,
        3, 6, 4, 1, 2, 5,
        5, 1, 2, 3, 6, 4,
        6, 4, 3, 2, 5, 1,
        1, 2, 5, 6, 4, 3,
        4, 3, 6, 5, 1, 2,
    )
    private val SKY_EASY_CLUES = ints(
        4, 2, 4, 2, 2, 1,
        1, 2, 2, 3, 3, 3,
        2, 3, 1, 2, 4, 5,
        3, 2, 2, 1, 4, 2,
    )
    private val SKY_HARD_SOLUTION = ints(
        6, 1, 2, 3, 5, 4,
        5, 4, 3, 2, 6, 1,
        2, 6, 1, 4, 3, 5,
        3, 5, 4, 1, 2, 6,
        1, 2, 6, 5, 4, 3,
        4, 3, 5, 6, 1, 2,
    )
    private val SKY_HARD_CLUES = ints(
        1, 3, 4, 4, 2, 3,
        3, 2, 2, 1, 4, 2,
        3, 3, 2, 1, 3, 3,
        1, 2, 2, 3, 3, 3,
    )
    private val DIAGONAL_SOLUTION = ints(
        5, 3, 2, 6, 1, 4,
        4, 1, 6, 2, 3, 5,
        2, 4, 3, 1, 5, 6,
        1, 6, 5, 4, 2, 3,
        3, 2, 4, 5, 6, 1,
        6, 5, 1, 3, 4, 2,
    )
    private val KNIGHT_SOLUTION = ints(
        2, 4, 1, 6, 5, 3,
        6, 5, 3, 1, 4, 2,
        5, 6, 2, 3, 1, 4,
        3, 1, 4, 2, 6, 5,
        1, 3, 5, 4, 2, 6,
        4, 2, 6, 5, 3, 1,
    )

    private fun ints(vararg values: Int): List<Int> = values.toList()
}

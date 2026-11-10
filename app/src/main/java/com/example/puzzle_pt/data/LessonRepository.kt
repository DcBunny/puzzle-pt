package com.example.puzzle_pt.data

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
        if (type == SudokuType.STANDARD) return emptyList()
        val steps = when (type) {
            SudokuType.STANDARD -> emptyList()
            SudokuType.KILLER -> listOf(
                LessonStep(
                    "加法小屋来帮忙",
                    "除了标准规则，同一虚线笼里的数字不能重复，合起来要等于笼角的数。",
                    "第一行小屋已有 1 和 4，屋角是 10。用减法找出空格的数字。",
                    LessonVisualKind.KILLER_MAKE_SUM,
                    LessonInteraction.MAKE_KILLER_SUM,
                ),
            )
            SudokuType.SKYSCRAPER -> listOf(
                LessonStep(
                    "数字变成小楼",
                    "数字是几，小楼就有几层高；高楼会挡住后面的矮楼。",
                    "从左边看，数一数能看到几栋楼。",
                    LessonVisualKind.SKY_HEIGHTS,
                    LessonInteraction.COUNT_SKYSCRAPERS,
                ),
                LessonStep(
                    "棋盘边缘的数字",
                    "边缘数字表示：从这一侧看过去，一共能看到几栋楼。",
                    "第一行从左边看，边缘线索是 3。把可见数字都选中。",
                    LessonVisualKind.SKY_QUIZ,
                    LessonInteraction.COUNT_SKYSCRAPERS,
                ),
            )
            SudokuType.DIAGONAL -> listOf(
                LessonStep(
                    "对角线也能帮你排除",
                    "除了标准规则，两条主对角线也都要有 1 到 6，数字不能重复。",
                    "主对角线上已有 5、1、3、6、2，找出空格缺少的数字。",
                    LessonVisualKind.DIAGONAL_DUPLICATE,
                    LessonInteraction.FIND_DIAGONAL_DUPLICATE,
                ),
            )
            SudokuType.ANTI_KNIGHT -> listOf(
                LessonStep(
                    "马步也能帮你排除",
                    "除了标准规则，走两格再拐一格的两格，不能填相同数字。",
                    "第 1 行第 2 列按标准规则可填 2 或 4，看看马步落点后填数。",
                    LessonVisualKind.KNIGHT_CHOOSE_SAFE,
                    LessonInteraction.CHOOSE_SAFE_KNIGHT_NUMBER,
                ),
            )
        }
        return steps + LessonStep(
            "你来试一试",
            when (type) {
                SudokuType.STANDARD -> "看看这一行、这一列和这一宫，找出缺少的数字。"
                SudokuType.KILLER -> "看看小屋还差多少，再检查这一行、这一列和这一宫。"
                SudokuType.SKYSCRAPER -> "从箭头方向数一数，看到的楼数要和外面的线索一样。"
                SudokuType.DIAGONAL -> "除了行、列和宫，也看看对角线还缺哪个数字。"
                SudokuType.ANTI_KNIGHT -> "除了行、列和宫，也看看马步落点，不能填相同数字。"
            },
            "给发光的小格填上正确的数字。",
            LessonVisualKind.NUMBER_FRIENDS,
        )
    }

    fun examplePuzzle(type: SudokuType): SudokuPuzzle {
        val puzzles = PuzzleRepository.puzzlesOf(type)
        val puzzle = if (type == SudokuType.ANTI_KNIGHT) puzzles[1] else puzzles.first()
        return if (type == SudokuType.ANTI_KNIGHT) puzzle else puzzle.copy(givens = puzzle.solution)
    }

    fun guidedPuzzle(type: SudokuType): SudokuPuzzle {
        if (type == SudokuType.ANTI_KNIGHT) return PuzzleRepository.puzzlesOf(type)[1]
        val puzzle = PuzzleRepository.puzzlesOf(type).first()
        val target = puzzle.givens.indices.first { index ->
            val row = index / 6
            val column = index % 6
            puzzle.givens[index] == 0 && (
                type != SudokuType.DIAGONAL || row == column || row + column == 5
                )
        }
        val givens = puzzle.solution.toMutableList().apply { this[target] = 0 }
        return puzzle.copy(givens = givens)
    }

    fun guidedTargetIndex(type: SudokuType): Int {
        if (type == SudokuType.ANTI_KNIGHT) return 22
        return guidedPuzzle(type).givens.indexOf(0)
    }
}

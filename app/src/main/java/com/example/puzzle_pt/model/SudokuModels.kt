package com.example.puzzle_pt.model

const val SUDOKU_SIZE = 6
const val CELL_COUNT = SUDOKU_SIZE * SUDOKU_SIZE

enum class SudokuType(
    val title: String,
    val subtitle: String,
    val ruleSummary: String,
    val size: Int = 6,
    val hasLesson: Boolean = true,
) {
    STANDARD("标准数独", "从经典规则开始", "每行、每列、每宫都包含 1–6", hasLesson = false),
    KILLER("杀手数独", "用加法寻找线索", "虚线笼内数字之和等于左上角数字"),
    SKYSCRAPER("摩天楼数独", "换个角度观察数字", "从四周观察，可见楼数必须符合线索"),
    DIAGONAL("对角线数独", "发现斜线上的规律", "两条主对角线也不能出现重复数字"),
    ANTI_KNIGHT("无马数独", "留意马步之间的格子", "马步相连的两格不能填相同数字"),
    SEVEN("七宫格数独", "挑战不规则宫", "每行、每列、每宫都包含 1–7", size = 7, hasLesson = false),
    NINE("九宫格数独", "挑战经典九宫格", "每行、每列、每宫都包含 1–9", size = 9, hasLesson = false),
}

enum class Difficulty(val title: String) {
    EASY("入门"),
    MEDIUM("进阶"),
    HARD("挑战"),
}

data class CellPosition(val row: Int, val column: Int) {
    val index: Int get() = row * SUDOKU_SIZE + column
}

data class KillerCage(
    val sum: Int,
    val cells: List<CellPosition>,
) {
    init {
        require(cells.isNotEmpty()) { "杀手笼不能为空" }
    }

    val labelIndex: Int
        get() = cells.minWith(compareBy({ it.row }, { it.column })).index
}

data class SudokuPuzzle(
    val id: String,
    val type: SudokuType,
    val difficulty: Difficulty,
    val givens: List<Int>,
    val solution: List<Int>,
    val cages: List<KillerCage> = emptyList(),
    val skyscraperClues: List<Int> = emptyList(),
    val regions: List<Int> = emptyList(),
) {
    val size: Int get() = type.size
    val cellCount: Int get() = size * size

    init {
        require(givens.size == cellCount) { "题目必须包含 $cellCount 格" }
        require(solution.size == cellCount) { "答案必须包含 $cellCount 格" }
        require(givens.all { it in 0..size } && solution.all { it in 1..size })
        require(givens.indices.all { givens[it] == 0 || givens[it] == solution[it] })
        require(type != SudokuType.KILLER || cages.isNotEmpty())
        require(type != SudokuType.SKYSCRAPER || skyscraperClues.size == size * 4)
        if (type == SudokuType.SEVEN) {
            require(regions.size == cellCount && regions.toSet() == (0 until size).toSet()) {
                "七宫格题目必须完整划分 7 个宫"
            }
            require(regions.groupingBy { it }.eachCount().values.all { it == size }) {
                "七宫格每宫必须包含 7 格"
            }
        } else {
            require(regions.isEmpty())
        }
    }
}

data class SudokuHint(
    val index: Int,
    val value: Int,
    val explanation: String,
)

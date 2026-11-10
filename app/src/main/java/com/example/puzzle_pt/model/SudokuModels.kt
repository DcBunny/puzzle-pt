package com.example.puzzle_pt.model

const val SUDOKU_SIZE = 6
const val CELL_COUNT = SUDOKU_SIZE * SUDOKU_SIZE

enum class SudokuType(
    val title: String,
    val subtitle: String,
    val ruleSummary: String,
) {
    STANDARD("标准数独", "从经典规则开始", "每行、每列、每宫都包含 1–6"),
    KILLER("杀手数独", "用加法寻找线索", "虚线笼内数字之和等于左上角数字"),
    SKYSCRAPER("摩天楼数独", "换个角度观察数字", "从四周观察，可见楼数必须符合线索"),
    DIAGONAL("对角线数独", "发现斜线上的规律", "两条主对角线也不能出现重复数字"),
    ANTI_KNIGHT("无马数独", "留意马步之间的格子", "马步相连的两格不能填相同数字"),
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
)

data class SudokuPuzzle(
    val id: String,
    val type: SudokuType,
    val difficulty: Difficulty,
    val givens: List<Int>,
    val solution: List<Int>,
    val cages: List<KillerCage> = emptyList(),
    val skyscraperClues: List<Int> = emptyList(),
) {
    init {
        require(givens.size == CELL_COUNT) { "题目必须包含 36 格" }
        require(solution.size == CELL_COUNT) { "答案必须包含 36 格" }
        require(givens.all { it in 0..6 } && solution.all { it in 1..6 })
        require(givens.indices.all { givens[it] == 0 || givens[it] == solution[it] })
        require(type != SudokuType.KILLER || cages.isNotEmpty())
        require(type != SudokuType.SKYSCRAPER || skyscraperClues.size == 24)
    }
}

data class SudokuHint(
    val index: Int,
    val value: Int,
    val explanation: String,
)

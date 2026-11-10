package com.example.puzzle_pt.data

import com.example.puzzle_pt.model.CellPosition
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.KillerCage
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

object PuzzleRepository {
    val puzzles = listOf(
        puzzle(
            "standard-easy", SudokuType.STANDARD, Difficulty.EASY,
            ints(6, 5, 0, 3, 4, 2, 3, 4, 2, 0, 5, 0, 5, 1, 6, 0, 0, 3, 4, 0, 3, 0, 1, 0, 1, 6, 5, 2, 0, 4, 0, 0, 4, 1, 0, 5),
            ints(6, 5, 1, 3, 4, 2, 3, 4, 2, 6, 5, 1, 5, 1, 6, 4, 2, 3, 4, 2, 3, 5, 1, 6, 1, 6, 5, 2, 3, 4, 2, 3, 4, 1, 6, 5),
        ),
        puzzle(
            "standard-medium", SudokuType.STANDARD, Difficulty.MEDIUM,
            ints(0, 0, 1, 0, 0, 5, 5, 0, 3, 0, 2, 0, 0, 0, 2, 0, 0, 3, 0, 5, 6, 2, 4, 1, 0, 3, 5, 0, 0, 2, 0, 1, 0, 0, 3, 6),
            ints(4, 2, 1, 3, 6, 5, 5, 6, 3, 1, 2, 4, 1, 4, 2, 6, 5, 3, 3, 5, 6, 2, 4, 1, 6, 3, 5, 4, 1, 2, 2, 1, 4, 5, 3, 6),
        ),
        puzzle(
            "standard-hard", SudokuType.STANDARD, Difficulty.HARD,
            ints(0, 6, 1, 3, 0, 0, 0, 0, 2, 0, 6, 0, 0, 3, 5, 1, 0, 0, 0, 0, 0, 0, 0, 5, 6, 1, 0, 5, 0, 0, 0, 2, 3, 0, 0, 0),
            ints(4, 6, 1, 3, 5, 2, 3, 5, 2, 4, 6, 1, 2, 3, 5, 1, 4, 6, 1, 4, 6, 2, 3, 5, 6, 1, 4, 5, 2, 3, 5, 2, 3, 6, 1, 4),
        ),
        puzzle(
            "killer-easy", SudokuType.KILLER, Difficulty.EASY,
            ints(0, 4, 5, 0, 3, 6, 0, 2, 6, 4, 0, 0, 0, 1, 0, 3, 0, 2, 6, 3, 2, 1, 0, 4, 2, 6, 3, 5, 0, 0, 4, 5, 1, 6, 2, 0),
            ints(1, 4, 5, 2, 3, 6, 3, 2, 6, 4, 1, 5, 5, 1, 4, 3, 6, 2, 6, 3, 2, 1, 5, 4, 2, 6, 3, 5, 4, 1, 4, 5, 1, 6, 2, 3),
            cageSums = ints(10, 11, 11, 10, 10, 11, 11, 10, 11, 10, 10, 11),
        ),
        puzzle(
            "killer-medium", SudokuType.KILLER, Difficulty.MEDIUM,
            ints(0, 0, 0, 5, 1, 0, 1, 5, 0, 0, 0, 0, 0, 4, 0, 2, 0, 1, 5, 2, 0, 0, 0, 6, 4, 0, 3, 0, 0, 0, 2, 1, 5, 6, 4, 3),
            ints(6, 3, 4, 5, 1, 2, 1, 5, 2, 3, 6, 4, 3, 4, 6, 2, 5, 1, 5, 2, 1, 4, 3, 6, 4, 6, 3, 1, 2, 5, 2, 1, 5, 6, 4, 3),
            cageSums = ints(13, 8, 8, 13, 13, 8, 8, 13, 13, 8, 8, 13),
        ),
        puzzle(
            "killer-hard", SudokuType.KILLER, Difficulty.HARD,
            ints(0, 6, 0, 0, 0, 2, 0, 0, 2, 0, 6, 0, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 4, 6, 3, 1, 6, 3, 0, 5, 0, 0),
            ints(1, 6, 3, 4, 5, 2, 4, 5, 2, 1, 6, 3, 3, 1, 6, 2, 4, 5, 2, 4, 5, 3, 1, 6, 5, 2, 4, 6, 3, 1, 6, 3, 1, 5, 2, 4),
            cageSums = ints(10, 11, 11, 10, 10, 11, 11, 10, 11, 10, 10, 11),
        ),
        puzzle(
            "skyscraper-easy", SudokuType.SKYSCRAPER, Difficulty.EASY,
            ints(2, 5, 1, 4, 0, 6, 3, 0, 4, 1, 2, 0, 0, 0, 2, 3, 6, 4, 6, 4, 3, 2, 0, 0, 1, 2, 0, 6, 0, 3, 0, 3, 6, 0, 0, 2),
            ints(2, 5, 1, 4, 3, 6, 3, 6, 4, 1, 2, 5, 5, 1, 2, 3, 6, 4, 6, 4, 3, 2, 5, 1, 1, 2, 5, 6, 4, 3, 4, 3, 6, 5, 1, 2),
            clues = ints(4, 2, 4, 2, 2, 1, 1, 2, 2, 3, 3, 3, 2, 3, 1, 2, 4, 5, 3, 2, 2, 1, 4, 2),
        ),
        puzzle(
            "skyscraper-medium", SudokuType.SKYSCRAPER, Difficulty.MEDIUM,
            ints(3, 4, 0, 1, 6, 2, 0, 0, 1, 0, 4, 0, 0, 3, 4, 0, 2, 1, 0, 0, 6, 4, 0, 0, 6, 0, 0, 3, 0, 4, 0, 5, 0, 2, 0, 0),
            ints(3, 4, 5, 1, 6, 2, 2, 6, 1, 5, 4, 3, 5, 3, 4, 6, 2, 1, 1, 2, 6, 4, 3, 5, 6, 1, 2, 3, 5, 4, 4, 5, 3, 2, 1, 6),
            clues = ints(3, 2, 2, 3, 1, 4, 2, 4, 3, 2, 3, 1, 2, 2, 2, 4, 3, 1, 4, 2, 2, 3, 1, 3),
        ),
        puzzle(
            "skyscraper-hard", SudokuType.SKYSCRAPER, Difficulty.HARD,
            ints(0, 1, 0, 0, 0, 0, 5, 4, 0, 0, 6, 1, 2, 0, 1, 4, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 6, 5, 0, 3, 0, 3, 0, 0, 1, 0),
            ints(6, 1, 2, 3, 5, 4, 5, 4, 3, 2, 6, 1, 2, 6, 1, 4, 3, 5, 3, 5, 4, 1, 2, 6, 1, 2, 6, 5, 4, 3, 4, 3, 5, 6, 1, 2),
            clues = ints(1, 3, 4, 4, 2, 3, 3, 2, 2, 1, 4, 2, 3, 3, 2, 1, 3, 3, 1, 2, 2, 3, 3, 3),
        ),
        puzzle(
            "diagonal-easy", SudokuType.DIAGONAL, Difficulty.EASY,
            ints(5, 0, 0, 0, 0, 4, 4, 1, 2, 6, 3, 0, 2, 4, 3, 0, 5, 6, 1, 6, 0, 4, 0, 3, 0, 2, 4, 5, 0, 1, 6, 0, 1, 0, 4, 2),
            ints(5, 3, 6, 2, 1, 4, 4, 1, 2, 6, 3, 5, 2, 4, 3, 1, 5, 6, 1, 6, 5, 4, 2, 3, 3, 2, 4, 5, 6, 1, 6, 5, 1, 3, 4, 2),
        ),
        puzzle(
            "diagonal-medium", SudokuType.DIAGONAL, Difficulty.MEDIUM,
            ints(4, 3, 6, 0, 1, 5, 0, 0, 0, 0, 0, 0, 3, 0, 5, 0, 0, 1, 0, 4, 0, 3, 0, 2, 1, 6, 0, 5, 0, 0, 2, 5, 0, 0, 4, 6),
            ints(4, 3, 6, 2, 1, 5, 5, 1, 2, 6, 3, 4, 3, 2, 5, 4, 6, 1, 6, 4, 1, 3, 5, 2, 1, 6, 4, 5, 2, 3, 2, 5, 3, 1, 4, 6),
        ),
        puzzle(
            "diagonal-hard", SudokuType.DIAGONAL, Difficulty.HARD,
            ints(0, 0, 2, 0, 0, 0, 0, 1, 6, 0, 5, 2, 0, 5, 3, 0, 0, 4, 1, 0, 0, 0, 0, 5, 0, 0, 5, 1, 0, 0, 3, 0, 0, 5, 0, 0),
            ints(5, 3, 2, 4, 6, 1, 4, 1, 6, 3, 5, 2, 2, 5, 3, 6, 1, 4, 1, 6, 4, 2, 3, 5, 6, 2, 5, 1, 4, 3, 3, 4, 1, 5, 2, 6),
        ),
        puzzle(
            "anti-knight-easy", SudokuType.ANTI_KNIGHT, Difficulty.EASY,
            ints(0, 1, 0, 6, 0, 2, 2, 4, 6, 3, 1, 0, 0, 2, 0, 1, 5, 6, 0, 0, 1, 4, 2, 3, 0, 6, 4, 2, 0, 1, 1, 3, 2, 0, 0, 4),
            ints(3, 1, 5, 6, 4, 2, 2, 4, 6, 3, 1, 5, 4, 2, 3, 1, 5, 6, 6, 5, 1, 4, 2, 3, 5, 6, 4, 2, 3, 1, 1, 3, 2, 5, 6, 4),
        ),
        puzzle(
            "anti-knight-medium", SudokuType.ANTI_KNIGHT, Difficulty.MEDIUM,
            ints(0, 0, 0, 6, 0, 3, 0, 5, 0, 1, 0, 0, 5, 6, 2, 3, 1, 4, 0, 1, 4, 0, 0, 0, 1, 3, 5, 0, 0, 0, 0, 0, 0, 5, 3, 1),
            ints(2, 4, 1, 6, 5, 3, 6, 5, 3, 1, 4, 2, 5, 6, 2, 3, 1, 4, 3, 1, 4, 2, 6, 5, 1, 3, 5, 4, 2, 6, 4, 2, 6, 5, 3, 1),
        ),
        puzzle(
            "anti-knight-hard", SudokuType.ANTI_KNIGHT, Difficulty.HARD,
            ints(0, 0, 0, 0, 0, 0, 0, 2, 6, 4, 0, 0, 4, 0, 2, 5, 3, 1, 1, 3, 0, 0, 6, 0, 0, 0, 0, 0, 4, 6, 6, 0, 0, 0, 0, 0),
            ints(3, 1, 4, 6, 2, 5, 5, 2, 6, 4, 1, 3, 4, 6, 2, 5, 3, 1, 1, 3, 5, 2, 6, 4, 2, 5, 3, 1, 4, 6, 6, 4, 1, 3, 5, 2),
        ),
    )

    fun puzzlesOf(type: SudokuType): List<SudokuPuzzle> = puzzles.filter { it.type == type }

    fun puzzleById(id: String): SudokuPuzzle = puzzles.first { it.id == id }

    private fun puzzle(
        id: String,
        type: SudokuType,
        difficulty: Difficulty,
        givens: List<Int>,
        solution: List<Int>,
        cageSums: List<Int> = emptyList(),
        clues: List<Int> = emptyList(),
    ): SudokuPuzzle {
        val cages = if (type == SudokuType.KILLER) {
            cageSums.flatMapIndexed { cageIndex, sum ->
                val row = cageIndex / 2
                val startColumn = (cageIndex % 2) * 3
                listOf(
                    KillerCage(
                        sum = sum,
                        cells = List(3) { offset -> CellPosition(row, startColumn + offset) },
                    ),
                )
            }
        } else {
            emptyList()
        }
        return SudokuPuzzle(id, type, difficulty, givens, solution, cages, clues)
    }

    private fun ints(vararg values: Int): List<Int> = values.toList()
}

package com.example.puzzle_pt

import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyTest {
    @Test
    fun allPuzzlesAreSolvableWithoutSearch() {
        PuzzleRepository.puzzles.forEach { puzzle ->
            val analysis = DifficultyAnalyzer.analyze(puzzle)
            assertTrue("${puzzle.id} 需要试填或超出技巧集", analysis.solved)
        }
    }

    @Test
    fun easyPuzzlesUseSinglesOnly() {
        PuzzleRepository.puzzles
            .filter { it.difficulty == Difficulty.EASY }
            .forEach { puzzle ->
                val analysis = DifficultyAnalyzer.analyze(puzzle)
                assertEquals(
                    "${puzzle.id} 入门题不应依赖进阶技巧",
                    TechniqueLevel.SINGLES,
                    analysis.topLevel,
                )
            }
    }

    @Test
    fun advancedTiersUseMoreThanSingles() {
        setOf(SudokuType.SEVEN, SudokuType.NINE, SudokuType.DIAGONAL).forEach { type ->
            PuzzleRepository.puzzlesOf(type)
                .filter { it.difficulty != Difficulty.EASY }
                .forEach { puzzle ->
                    val analysis = DifficultyAnalyzer.analyze(puzzle)
                    assertTrue(
                        "${puzzle.id} 中高难度应至少使用 L2 技巧",
                        analysis.solved && analysis.topLevel != TechniqueLevel.SINGLES,
                    )
                }
        }

        PuzzleRepository.puzzlesOf(SudokuType.ANTI_KNIGHT)
            .filter { it.difficulty == Difficulty.HARD }
            .forEach { puzzle ->
                val analysis = DifficultyAnalyzer.analyze(puzzle)
                assertTrue(
                    "${puzzle.id} 挑战题应至少使用 L2 技巧",
                    analysis.solved && analysis.topLevel != TechniqueLevel.SINGLES,
                )
            }

        // anti-knight-medium 是课程示例，保持不动；其余中等题应为 L2。
        PuzzleRepository.puzzlesOf(SudokuType.ANTI_KNIGHT)
            .filter { it.difficulty == Difficulty.MEDIUM && it.id != "anti-knight-medium" }
            .forEach { puzzle ->
                val analysis = DifficultyAnalyzer.analyze(puzzle)
                assertTrue(
                    "${puzzle.id} 中等题应至少使用 L2 技巧",
                    analysis.solved && analysis.topLevel != TechniqueLevel.SINGLES,
                )
            }
    }
}

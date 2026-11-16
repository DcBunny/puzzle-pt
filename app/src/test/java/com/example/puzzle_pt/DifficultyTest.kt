package com.example.puzzle_pt

import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.engine.TeachingGuide
import com.example.puzzle_pt.engine.TechniqueLevel
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyTest {
    @Test
    fun foundPathsMatchTheDeclaredBands() {
        PuzzleRepository.puzzles.forEach { puzzle ->
            val analysis = TeachingGuide.analysis(puzzle)
            assertTrue(
                "${puzzle.id} 在已教技巧内没有找到完整路径：${analysis.steps.size} 步 / ${analysis.topLevel}",
                analysis.solved,
            )
            assertEquals(
                "${puzzle.id} 找到的路径应落在${puzzle.difficulty.title}，最高技巧 ${analysis.topLevel}，进阶步数 " +
                    analysis.steps.count { it.level != TechniqueLevel.SINGLES },
                puzzle.difficulty,
                TeachingGuide.ratedDifficulty(puzzle, analysis),
            )
        }
    }

    @Test
    fun techniqueCostRisesAcrossBands() {
        SudokuType.entries.forEach { type ->
            val averages = Difficulty.entries.associateWith { difficulty ->
                PuzzleRepository.puzzles
                    .filter { it.type == type && it.difficulty == difficulty }
                    .map { TeachingGuide.pathCost(it) }
                    .average()
            }
            assertTrue(
                "${type.title} 入门平均推理成本应低于进阶：$averages",
                averages.getValue(Difficulty.EASY) < averages.getValue(Difficulty.MEDIUM),
            )
            assertTrue(
                "${type.title} 进阶平均推理成本应低于挑战：$averages",
                averages.getValue(Difficulty.MEDIUM) < averages.getValue(Difficulty.HARD),
            )
        }
    }
}

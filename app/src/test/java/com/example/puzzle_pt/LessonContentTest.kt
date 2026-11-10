package com.example.puzzle_pt

import com.example.puzzle_pt.data.LessonInteraction
import com.example.puzzle_pt.data.LessonRepository
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonContentTest {
    @Test
    fun standardHasNoLesson() {
        assertTrue(LessonRepository.stepsFor(SudokuType.STANDARD).isEmpty())
    }

    @Test
    fun everyVariantHasShortPlayableLessonsAndOneValidFinalCell() {
        SudokuType.entries.filter { it != SudokuType.STANDARD }.forEach { type ->
            val steps = LessonRepository.stepsFor(type)
            val expectedSteps = if (type == SudokuType.SKYSCRAPER) 3 else 2
            assertEquals(type.name, expectedSteps, steps.size)
            assertTrue(type.name, steps.all { it.explanation.length <= 40 && it.action.length <= 40 })
            assertTrue(type.name, steps.any { it.interaction != LessonInteraction.NONE })
            assertEquals(type.name, expectedSteps, steps.map { it.visual }.distinct().size)
            val ruleStep = steps.first()
            val expectedPrompt = when (type) {
                SudokuType.KILLER -> "第一行"
                SudokuType.SKYSCRAPER -> "从左边看"
                SudokuType.DIAGONAL -> "主对角线"
                SudokuType.ANTI_KNIGHT -> "马步落点"
                SudokuType.STANDARD -> ""
            }
            assertTrue(type.name, ruleStep.action.contains(expectedPrompt))
            if (type == SudokuType.SKYSCRAPER) {
                assertTrue(steps[1].explanation.contains("边缘数字"))
            }

            val examplePuzzle = LessonRepository.examplePuzzle(type)
            assertEquals(type.name, 36, examplePuzzle.givens.size)
            if (type == SudokuType.ANTI_KNIGHT) {
                assertEquals(0, examplePuzzle.givens[1])
                assertEquals(2, examplePuzzle.givens[14])
                val standardPuzzle = examplePuzzle.copy(type = SudokuType.STANDARD)
                assertEquals(listOf(2, 4), SudokuRules.candidates(examplePuzzle.givens.toIntArray(), 1, standardPuzzle))
                assertEquals(listOf(4), SudokuRules.candidates(examplePuzzle.givens.toIntArray(), 1, examplePuzzle))
            } else {
                assertEquals(type.name, examplePuzzle.solution, examplePuzzle.givens)
                assertTrue(type.name, SudokuRules.isComplete(examplePuzzle.givens, examplePuzzle))
            }

            val guidedPuzzle = LessonRepository.guidedPuzzle(type)
            val guidedTarget = LessonRepository.guidedTargetIndex(type)
            if (type == SudokuType.ANTI_KNIGHT) {
                assertTrue(guidedPuzzle.givens.count { it == 0 } > 1)
                assertEquals(22, guidedTarget)
                val standardPuzzle = guidedPuzzle.copy(type = SudokuType.STANDARD)
                assertEquals(listOf(2, 5, 6), SudokuRules.candidates(guidedPuzzle.givens.toIntArray(), guidedTarget, standardPuzzle))
                assertEquals(listOf(6), SudokuRules.candidates(guidedPuzzle.givens.toIntArray(), guidedTarget, guidedPuzzle))
            } else {
                assertEquals(type.name, 1, guidedPuzzle.givens.count { it == 0 })
            }
            assertTrue(type.name, SudokuRules.isComplete(guidedPuzzle.solution, guidedPuzzle))
        }
    }
}

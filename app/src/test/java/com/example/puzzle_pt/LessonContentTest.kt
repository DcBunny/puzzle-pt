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
    fun everyTypeHasShortPlayableLessonsAndOneValidFinalCell() {
        SudokuType.entries.forEach { type ->
            val steps = LessonRepository.stepsFor(type)
            assertEquals(type.name, 5, steps.size)
            assertTrue(type.name, steps.all { it.explanation.length <= 40 && it.action.length <= 40 })
            assertTrue(type.name, steps.any { it.interaction != LessonInteraction.NONE })
            assertTrue(type.name, steps.map { it.visual }.distinct().size >= 4)

            val guidedPuzzle = LessonRepository.guidedPuzzle(type)
            assertEquals(type.name, 1, guidedPuzzle.givens.count { it == 0 })
            assertTrue(type.name, SudokuRules.isComplete(guidedPuzzle.solution, guidedPuzzle))
        }
    }
}

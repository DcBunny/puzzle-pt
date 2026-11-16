package com.example.puzzle_pt

import com.example.puzzle_pt.data.LessonInteraction
import com.example.puzzle_pt.data.LessonRepository
import com.example.puzzle_pt.engine.StepKind
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.engine.SudokuSolver
import com.example.puzzle_pt.engine.TeachingGuide
import com.example.puzzle_pt.engine.TechniqueLevel
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonContentTest {
    @Test
    fun typesWithoutTeachingHaveNoLesson() {
        SudokuType.entries.filterNot { it.hasLesson }.forEach { type ->
            assertTrue(type.name, LessonRepository.stepsFor(type).isEmpty())
        }
    }

    @Test
    fun everyVariantLessonTeachesTheRuleBeforeTheFinalCell() {
        val expectedSteps = mapOf(
            SudokuType.KILLER to 4,
            SudokuType.SKYSCRAPER to 4,
            SudokuType.DIAGONAL to 4,
            SudokuType.ANTI_KNIGHT to 4,
        )
        expectedSteps.forEach { (type, count) ->
            val steps = LessonRepository.stepsFor(type)
            assertEquals(type.name, count, steps.size)
            assertTrue(type.name, steps.all { it.explanation.length <= 40 && it.action.length <= 40 })
            assertTrue(type.name, steps.dropLast(1).any { it.interaction != LessonInteraction.NONE })
            assertEquals("你来试一试", steps.last().title)

            val example = LessonRepository.examplePuzzle(type)
            val guided = LessonRepository.guidedPuzzle(type)
            val target = LessonRepository.guidedTargetIndex(type)
            assertNotEquals(example.givens, guided.givens)
            assertTrue(SudokuRules.isComplete(example.solution, example))
            assertTrue(SudokuRules.isComplete(guided.solution, guided))
            assertEquals(1, SudokuSolver.countSolutions(guided))
            assertTrue("${type.name} 试填应留下多个空格", guided.givens.count { it == 0 } > 1)

            val standard = guided.copy(
                type = SudokuType.STANDARD,
                cages = emptyList(),
                skyscraperClues = emptyList(),
            )
            val board = guided.givens.toIntArray()
            assertTrue(
                "${type.name} 去掉变体规则后目标格不应唯一",
                SudokuRules.candidates(board, target, standard).size > 1,
            )
            assertEquals(
                listOf(guided.solution[target]),
                SudokuRules.candidates(board, target, guided),
            )
            val forced = TeachingGuide.forcedStep(guided, guided.givens, target)
            assertTrue("${type.name} 应能直接解释目标格", forced != null)
            assertEquals(TechniqueLevel.SINGLES, forced!!.level)
            assertEquals(guided.solution[target], forced.value)
            val ruleWord = when (type) {
                SudokuType.KILLER -> "笼"
                SudokuType.SKYSCRAPER -> "楼"
                SudokuType.DIAGONAL -> "对角线"
                SudokuType.ANTI_KNIGHT -> "马步"
                else -> ""
            }
            assertTrue(forced.explanation, ruleWord in forced.explanation)
            if (type == SudokuType.ANTI_KNIGHT) {
                assertTrue(LessonRepository.guidedFocus(type).size < 10)
                assertEquals(StepKind.NAKED_SINGLE, forced.kind)
            }
        }
    }
}

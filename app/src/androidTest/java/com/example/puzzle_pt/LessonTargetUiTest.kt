package com.example.puzzle_pt

import android.content.Context
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzle_pt.data.LessonRepository
import com.example.puzzle_pt.data.ProgressStore
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType
import com.example.puzzle_pt.ui.components.SudokuBoard
import com.example.puzzle_pt.ui.components.lessonTargetDescription
import com.example.puzzle_pt.ui.components.lessonTargetState
import com.example.puzzle_pt.ui.screens.LessonScreen
import com.example.puzzle_pt.ui.theme.SudokuTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.unit.dp

@RunWith(AndroidJUnit4::class)
class LessonTargetUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearProgress() {
        context.getSharedPreferences("sudoku_progress", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun targetMarkerStaysUniqueAcrossReferenceSelectionAndConflict() {
        val puzzle = LessonRepository.guidedPuzzle(SudokuType.DIAGONAL)
        val target = LessonRepository.guidedTargetIndex(SudokuType.DIAGONAL)
        var selected by mutableStateOf<Int?>(null)
        var conflicting by mutableStateOf(false)
        composeRule.setContent {
            SudokuTheme {
                val board = if (conflicting) conflictingBoard(puzzle, target) else puzzle.givens
                SudokuBoard(
                    puzzle = puzzle,
                    board = board,
                    notes = List(puzzle.cellCount) { emptySet() },
                    selectedIndex = selected,
                    conflictCells = SudokuRules.conflictingCells(board, puzzle),
                    highlightCells = LessonRepository.guidedFocus(puzzle.type),
                    lessonTargetIndex = target,
                    onCellSelected = { selected = it },
                    modifier = Modifier.width(420.dp),
                )
            }
        }

        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = false, referenced = true))
        composeRule.onNodeWithTag("cell_$target").performClick()
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = true, referenced = true))
        composeRule.onNodeWithTag("cell_0").performClick()
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = false, referenced = true))
        composeRule.runOnIdle { conflicting = true }
        composeRule.onNodeWithTag("cell_$target").performClick()
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = true, selected = true, referenced = true))
    }

    @Test
    fun groupHighlightAndPracticeHintAreNotLessonTargets() {
        val puzzle = LessonRepository.examplePuzzle(SudokuType.SKYSCRAPER)
        composeRule.setContent {
            SudokuTheme {
                SudokuBoard(
                    puzzle = puzzle,
                    board = puzzle.solution,
                    notes = List(puzzle.cellCount) { emptySet() },
                    selectedIndex = null,
                    conflictCells = BooleanArray(puzzle.cellCount),
                    highlightCells = (0 until puzzle.size).toSet(),
                    hintIndex = 0,
                    modifier = Modifier.width(420.dp),
                )
            }
        }
        composeRule.onAllNodesWithTag("lesson_target_corner", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("lesson_target_border", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun killerTargetMarkLeavesRoomForSumAndDigit() {
        val puzzle = LessonRepository.guidedPuzzle(SudokuType.KILLER)
        val target = LessonRepository.guidedTargetIndex(SudokuType.KILLER)
        val board = puzzle.givens.mapIndexed { index, value ->
            if (index == target) puzzle.solution[index] else value
        }
        composeRule.setContent {
            SudokuTheme(darkTheme = true) {
                SudokuBoard(
                    puzzle = puzzle,
                    board = board,
                    notes = List(puzzle.cellCount) { emptySet() },
                    selectedIndex = target,
                    conflictCells = SudokuRules.conflictingCells(board, puzzle),
                    highlightCells = LessonRepository.guidedFocus(puzzle.type),
                    lessonTargetIndex = target,
                    modifier = Modifier.width(480.dp),
                )
            }
        }
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = true, referenced = true))
        val cell = composeRule.onNodeWithTag("cell_$target").fetchSemanticsNode().boundsInRoot
        val corner = composeRule.onNodeWithTag("lesson_target_corner", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val sum = composeRule.onNodeWithTag("cage_sum_$target", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val digit = composeRule.onNodeWithTag("cell_value_$target", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(cell.left <= corner.left && cell.top <= corner.top && cell.right >= corner.right && cell.bottom >= corner.bottom)
        assertTrue(corner.center.x > cell.center.x && corner.center.y > cell.center.y)
        assertTrue(sum.center.x < cell.center.x && sum.center.y < cell.center.y)
        assertFalse(corner.overlaps(sum))
        assertFalse(corner.overlaps(digit))
        assertTrue(sum.right < cell.center.x)
        assertTrue(digit.right < corner.left || digit.bottom < corner.top)
    }

    @Test
    fun killerFinalStepKeepsTargetAndRemovesPromptAfterSuccess() {
        composeRule.setContent {
            SudokuTheme {
                LessonScreen(
                    type = SudokuType.KILLER,
                    store = ProgressStore(context),
                    onBack = {},
                    onPractice = {},
                    onProgressChanged = {},
                )
            }
        }
        click("lesson_next")
        click("visual_killer_duplicate_1")
        click("lesson_next")
        click("visual_killer_number_5")
        click("lesson_next")

        val puzzle = LessonRepository.guidedPuzzle(SudokuType.KILLER)
        val target = LessonRepository.guidedTargetIndex(SudokuType.KILLER)
        val prompt = LessonRepository.guidedFillPrompt(SudokuType.KILLER)
        composeRule.onNodeWithTag("lesson_target_prompt").performScrollTo().assertTextEquals(prompt)
        composeRule.onNodeWithText("给粗框标出的格子填上正确的数字。").performScrollTo().assertIsDisplayed()
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = false, referenced = false))

        click("cell_0")
        click("lesson_number_1")
        composeRule.onNodeWithTag("lesson_feedback").performScrollTo().assertTextEquals("先点粗框标出的格子。")
        composeRule.onNodeWithTag("lesson_target_prompt").assertTextEquals(prompt)
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = false, referenced = false))

        click("cell_$target")
        click("lesson_number_3")
        composeRule.onNodeWithTag("lesson_target_prompt").performScrollTo().assertTextEquals(prompt)
        val wrongFeedback = nodeText("lesson_feedback")
        assertTrue(wrongFeedback.isNotEmpty())
        assertFalse(wrongFeedback.contains("请填写粗框格"))
        assertFalse(wrongFeedback == "先点粗框标出的格子。")
        assertFalse(wrongFeedback == LessonRepository.SUCCESS_FEEDBACK)
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = true, referenced = false))

        click("cell_1")
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = false, referenced = false))

        click("cell_$target")
        click("lesson_number_${puzzle.solution[target]}")
        composeRule.onNodeWithTag("lesson_target_prompt").assertDoesNotExist()
        composeRule.onNodeWithText("给粗框标出的格子填上正确的数字。").assertDoesNotExist()
        composeRule.onNodeWithTag("lesson_feedback").performScrollTo().assertTextEquals(LessonRepository.SUCCESS_FEEDBACK)
        assertSingleTarget(puzzle, target, lessonTargetState(conflict = false, selected = true, referenced = false))
    }

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun nodeText(tag: String): String {
        val texts = composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text]
        return texts.joinToString("") { it.text }
    }

    private fun assertSingleTarget(puzzle: SudokuPuzzle, target: Int, state: String) {
        composeRule.onAllNodesWithTag("lesson_target_corner", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("lesson_target_border", useUnmergedTree = true).assertCountEquals(1)
        val description = lessonTargetDescription(target / puzzle.size, target % puzzle.size)
        composeRule.onNodeWithTag("cell_$target").assertContentDescriptionEquals(description)
        composeRule.onNodeWithTag("cell_$target").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state),
        )
    }

    private fun conflictingBoard(puzzle: SudokuPuzzle, target: Int): List<Int> {
        val other = if (target % puzzle.size == puzzle.size - 1) target - 1 else target + 1
        return puzzle.solution.mapIndexed { index, value ->
            if (index == target) puzzle.solution[other] else value
        }
    }
}

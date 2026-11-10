package com.example.puzzle_pt

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzle_pt.data.LessonRepository
import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.model.SudokuType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearProgress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("sudoku_progress", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun homeOffersTeachingAndPracticeForAllFiveTypes() {
        composeRule.onNodeWithText("六宫数独学院").assertIsDisplayed()
        SudokuType.entries.forEach { type ->
            scrollHomeTo("lesson_${type.name}")
            composeRule.onNodeWithTag("lesson_${type.name}").assertIsDisplayed()
            scrollHomeTo("practice_${type.name}")
            composeRule.onNodeWithTag("practice_${type.name}").assertIsDisplayed()
        }
    }

    @Test
    fun everyVariantOpensAPlayableBoard() {
        SudokuType.entries.forEach { type ->
            scrollHomeTo("practice_${type.name}")
            click("practice_${type.name}")
            click(easyPuzzleTag(type))
            composeRule.onNodeWithTag("sudoku_board").assertIsDisplayed()
            click("back_button")
            click("back_button")
        }
    }

    @Test
    fun everyVariantLessonIncludesChildFriendlyInteraction() {
        SudokuType.entries.forEach { type ->
            scrollHomeTo("lesson_${type.name}")
            click("lesson_${type.name}")
            composeRule.onNodeWithTag("listen_button").assertIsDisplayed()
            when (type) {
                SudokuType.STANDARD -> completeStandardLesson()
                SudokuType.KILLER -> completeKillerLesson()
                SudokuType.SKYSCRAPER -> completeSkyscraperLesson()
                SudokuType.DIAGONAL -> completeDiagonalLesson()
                SudokuType.ANTI_KNIGHT -> completeKnightLesson()
            }
            val guidedPuzzle = LessonRepository.guidedPuzzle(type)
            val target = guidedPuzzle.givens.indexOf(0)
            val answer = guidedPuzzle.solution[target]
            click("cell_$target")
            click("lesson_number_${answer % 6 + 1}")
            composeRule.onNodeWithTag("lesson_feedback").performScrollTo().assertIsDisplayed()
            click("lesson_number_$answer")
            composeRule.onNodeWithTag("lesson_practice").performScrollTo().assertIsEnabled()
            click("lesson_practice")
            composeRule.onNodeWithTag(easyPuzzleTag(type)).assertExists()
            click("back_button")
        }
    }

    @Test
    fun practiceSupportsNotesUndoAndRestoringProgress() {
        openStandardPractice()
        click("cell_2")
        click("notes_mode_button")
        click("number_1")
        composeRule.onNodeWithTag("game_feedback").assertTextEquals("已记下候选数 1")
        click("undo_button")
        composeRule.onNodeWithTag("game_feedback").assertTextEquals("已撤销上一步")
        click("notes_mode_button")
        click("number_1")
        composeRule.onNodeWithTag("cell_2").assertTextEquals("1")
        click("back_button")
        click("start_standard-easy")
        composeRule.onNodeWithTag("cell_2").assertTextEquals("1")
    }

    @Test
    fun standardPuzzleCanBeCompleted() {
        openStandardPractice()
        val puzzle = PuzzleRepository.puzzleById("standard-easy")
        puzzle.givens.indices.filter { puzzle.givens[it] == 0 }.forEach { index ->
            click("cell_$index")
            click("number_${puzzle.solution[index]}")
        }
        composeRule.onNodeWithText("恭喜完成！").assertIsDisplayed()
    }

    private fun completeStandardLesson() {
        click("lesson_next")
        click("lesson_next")
        click("visual_standard_duplicate_0")
        click("visual_standard_duplicate_1")
        click("lesson_next")
        click("visual_standard_missing_1")
        click("visual_standard_missing_4")
        click("lesson_next")
    }

    private fun completeKillerLesson() {
        click("lesson_next")
        click("lesson_next")
        click("visual_killer_number_4")
        click("visual_killer_number_4")
        click("visual_killer_number_1")
        click("visual_killer_number_2")
        click("visual_killer_number_3")
        click("lesson_next")
        click("visual_killer_duplicate_0")
        click("visual_killer_duplicate_1")
        click("lesson_next")
    }

    private fun completeSkyscraperLesson() {
        click("lesson_next")
        click("lesson_next")
        click("visual_sky_left")
        click("visual_sky_answer_2")
        click("visual_sky_answer_3")
        click("lesson_next")
        click("lesson_next")
    }

    private fun completeDiagonalLesson() {
        click("lesson_next")
        click("lesson_next")
        click("visual_diagonal_cell_0_1")
        for (index in 0..5) click("visual_diagonal_cell_${index}_$index")
        click("lesson_next")
        click("visual_diagonal_duplicate_0_5")
        click("visual_diagonal_duplicate_2_3")
        click("lesson_next")
    }

    private fun completeKnightLesson() {
        click("lesson_next")
        click("visual_knight_cell_1_0")
        click("visual_knight_cell_0_3")
        click("visual_knight_cell_2_3")
        click("visual_knight_cell_3_0")
        click("visual_knight_cell_3_2")
        click("lesson_next")
        click("lesson_next")
        click("visual_knight_safe_2")
        click("visual_knight_safe_1")
        click("lesson_next")
    }

    private fun openStandardPractice() {
        scrollHomeTo("practice_STANDARD")
        click("practice_STANDARD")
        click("start_standard-easy")
    }

    private fun scrollHomeTo(tag: String) {
        composeRule.onNodeWithTag("home_screen").performScrollToNode(hasTestTag(tag))
    }

    private fun click(tag: String) {
        val node = composeRule.onNodeWithTag(tag)
        if (tag == "back_button") node.performClick() else node.performScrollTo().performClick()
    }

    private fun easyPuzzleTag(type: SudokuType): String =
        "start_${type.name.lowercase().replace("anti_knight", "anti-knight")}-easy"
}

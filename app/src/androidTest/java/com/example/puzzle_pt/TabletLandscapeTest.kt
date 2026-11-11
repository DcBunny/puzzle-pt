package com.example.puzzle_pt

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import com.example.puzzle_pt.model.SudokuType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabletLandscapeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUpLandscape() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("sudoku_progress", Context.MODE_PRIVATE).edit().clear().commit()
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        waitForOrientation(Configuration.ORIENTATION_LANDSCAPE)
    }

    @Test
    fun practiceUsesTwoPanesAndKeepsGameControlsWorking() {
        openStandardPractice()

        composeRule.onNodeWithTag("practice_landscape").assertExists()
        composeRule.onNodeWithTag("sudoku_board").assertIsDisplayed()
        click("cell_2")
        click("notes_mode_button")
        click("number_1")
        composeRule.onNodeWithTag("game_feedback").assertTextEquals("已记下候选数 1")
        click("undo_button")
        composeRule.onNodeWithTag("game_feedback").assertTextEquals("已撤销上一步")
        click("pause_button")
        composeRule.onNodeWithText("已暂停").assertIsDisplayed()
        composeRule.onNodeWithTag("sudoku_board").assertDoesNotExist()
        click("pause_button")
        composeRule.onNodeWithTag("sudoku_board").assertIsDisplayed()
    }

    @Test
    fun lessonUsesTwoPanesAndKeepsInteractionWorking() {
        composeRule.onNodeWithTag("home_screen").performScrollToNode(hasTestTag("lesson_KILLER"))
        click("lesson_KILLER")
        composeRule.onNodeWithTag("lesson_landscape").assertIsDisplayed()
        click("visual_killer_number_6")
        click("visual_killer_number_5")
        click("lesson_next")

        val puzzle = LessonRepository.guidedPuzzle(SudokuType.KILLER)
        val target = LessonRepository.guidedTargetIndex(SudokuType.KILLER)
        val answer = puzzle.solution[target]
        click("cell_$target")
        click("lesson_number_$answer")
        composeRule.onNodeWithTag("lesson_practice").performScrollTo().assertIsEnabled()
    }

    private fun openStandardPractice() {
        composeRule.onNodeWithTag("home_screen").performScrollToNode(hasTestTag("practice_STANDARD"))
        click("practice_STANDARD")
        click("difficulty_STANDARD_EASY")
        click("start_standard-easy")
    }

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun waitForOrientation(orientation: Int) {
        composeRule.waitUntil(5_000) {
            var currentOrientation = Configuration.ORIENTATION_UNDEFINED
            composeRule.activityRule.scenario.onActivity {
                currentOrientation = it.resources.configuration.orientation
            }
            currentOrientation == orientation
        }
    }
}

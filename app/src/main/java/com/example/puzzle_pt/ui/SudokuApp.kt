package com.example.puzzle_pt.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.puzzle_pt.data.ProgressStore
import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.SudokuType
import com.example.puzzle_pt.ui.screens.DifficultyScreen
import com.example.puzzle_pt.ui.screens.HomeScreen
import com.example.puzzle_pt.ui.screens.LessonScreen
import com.example.puzzle_pt.ui.screens.PracticeScreen
import com.example.puzzle_pt.ui.screens.PuzzleListScreen

private sealed interface Screen {
    data object Home : Screen
    data class Lesson(val type: SudokuType) : Screen
    data class DifficultySelection(val type: SudokuType) : Screen
    data class PuzzleList(val type: SudokuType, val difficulty: Difficulty) : Screen
    data class Practice(val puzzleId: String) : Screen
}

@Composable
fun SudokuApp() {
    val context = LocalContext.current
    val store = remember { ProgressStore(context) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var progressVersion by remember { mutableIntStateOf(0) }

    fun goBack() {
        screen = when (val current = screen) {
            Screen.Home -> Screen.Home
            is Screen.Lesson -> Screen.Home
            is Screen.DifficultySelection -> Screen.Home
            is Screen.PuzzleList -> Screen.DifficultySelection(current.type)
            is Screen.Practice -> {
                val puzzle = PuzzleRepository.puzzleById(current.puzzleId)
                Screen.PuzzleList(puzzle.type, puzzle.difficulty)
            }
        }
    }

    BackHandler(enabled = screen !is Screen.Home, onBack = ::goBack)

    Surface(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val current = screen) {
            Screen.Home -> HomeScreen(
                store = store,
                progressVersion = progressVersion,
                onTeach = { screen = Screen.Lesson(it) },
                onPractice = { screen = Screen.DifficultySelection(it) },
            )
            is Screen.Lesson -> LessonScreen(
                type = current.type,
                store = store,
                onBack = ::goBack,
                onPractice = { screen = Screen.DifficultySelection(current.type) },
                onProgressChanged = { progressVersion++ },
            )
            is Screen.DifficultySelection -> DifficultyScreen(
                type = current.type,
                onBack = ::goBack,
                onSelect = { screen = Screen.PuzzleList(current.type, it) },
            )
            is Screen.PuzzleList -> PuzzleListScreen(
                type = current.type,
                difficulty = current.difficulty,
                store = store,
                progressVersion = progressVersion,
                onBack = ::goBack,
                onSelect = { screen = Screen.Practice(it.id) },
            )
            is Screen.Practice -> {
                val puzzle = PuzzleRepository.puzzleById(current.puzzleId)
                val puzzles = PuzzleRepository.puzzlesOf(puzzle.type).filter { it.difficulty == puzzle.difficulty }
                val next = puzzles.getOrNull(puzzles.indexOf(puzzle) + 1)
                PracticeScreen(
                    puzzle = puzzle,
                    store = store,
                    onBack = ::goBack,
                    onNext = next?.let { { screen = Screen.Practice(it.id) } },
                    onProgressChanged = { progressVersion++ },
                )
            }
        }
    }
}

package com.example.puzzle_pt.data

import android.content.Context
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

data class SavedGame(
    val board: List<Int>,
    val notes: List<Set<Int>>,
    val elapsedSeconds: Long,
)

class ProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "sudoku_progress",
        Context.MODE_PRIVATE,
    )

    fun isLessonComplete(type: SudokuType): Boolean = preferences.getBoolean(lessonKey(type), false)

    fun completeLesson(type: SudokuType) {
        preferences.edit().putBoolean(lessonKey(type), true).apply()
    }

    fun isPuzzleComplete(puzzleId: String): Boolean = preferences.getBoolean(puzzleKey(puzzleId), false)

    fun completePuzzle(puzzleId: String) {
        preferences.edit().putBoolean(puzzleKey(puzzleId), true).apply()
        clearGame(puzzleId)
    }

    fun hasSavedGame(puzzleId: String): Boolean = preferences.contains(boardKey(puzzleId))

    fun saveGame(puzzleId: String, board: List<Int>, notes: List<Set<Int>>, elapsedSeconds: Long) {
        val boardValue = board.joinToString("")
        val notesValue = notes.joinToString(",") { candidates ->
            candidates.fold(0) { mask, value -> mask or (1 shl value) }.toString()
        }
        preferences.edit()
            .putString(boardKey(puzzleId), boardValue)
            .putString(notesKey(puzzleId), notesValue)
            .putLong(timeKey(puzzleId), elapsedSeconds)
            .apply()
    }

    fun loadGame(puzzle: SudokuPuzzle): SavedGame? {
        val boardValue = preferences.getString(boardKey(puzzle.id), null) ?: return null
        val maxDigit = '0' + puzzle.size
        if (boardValue.length != puzzle.cellCount || boardValue.any { it !in '0'..maxDigit }) return null
        val board = boardValue.map { it.digitToInt() }
        // 题库更新后旧存档可能与新题目的给定格不一致，直接丢弃而不是恢复错误盘面。
        if (!savedBoardMatchesGivens(board, puzzle)) return null
        val notesValue = preferences.getString(notesKey(puzzle.id), null)
        val masks = notesValue?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        val notes = List(puzzle.cellCount) { index ->
            val mask = masks.getOrElse(index) { 0 }
            (1..puzzle.size).filter { mask and (1 shl it) != 0 }.toSet()
        }
        return SavedGame(
            board = board,
            notes = notes,
            elapsedSeconds = preferences.getLong(timeKey(puzzle.id), 0L),
        )
    }

    fun clearGame(puzzleId: String) {
        preferences.edit()
            .remove(boardKey(puzzleId))
            .remove(notesKey(puzzleId))
            .remove(timeKey(puzzleId))
            .apply()
    }

    private fun lessonKey(type: SudokuType) = "lesson_${type.name}"
    private fun puzzleKey(puzzleId: String) = "complete_$puzzleId"
    private fun boardKey(puzzleId: String) = "board_$puzzleId"
    private fun notesKey(puzzleId: String) = "notes_$puzzleId"
    private fun timeKey(puzzleId: String) = "time_$puzzleId"
}

internal fun savedBoardMatchesGivens(board: List<Int>, puzzle: SudokuPuzzle): Boolean =
    board.size == puzzle.cellCount &&
        puzzle.givens.indices.all { index ->
            puzzle.givens[index] == 0 || board[index] == puzzle.givens[index]
        }

package com.example.puzzle_pt.data

import android.content.Context
import com.example.puzzle_pt.model.CELL_COUNT
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

    fun loadGame(puzzleId: String): SavedGame? {
        val boardValue = preferences.getString(boardKey(puzzleId), null) ?: return null
        if (boardValue.length != CELL_COUNT || boardValue.any { it !in '0'..'6' }) return null
        val notesValue = preferences.getString(notesKey(puzzleId), null)
        val masks = notesValue?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        val notes = List(CELL_COUNT) { index ->
            val mask = masks.getOrElse(index) { 0 }
            (1..6).filter { mask and (1 shl it) != 0 }.toSet()
        }
        return SavedGame(
            board = boardValue.map { it.digitToInt() },
            notes = notes,
            elapsedSeconds = preferences.getLong(timeKey(puzzleId), 0L),
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

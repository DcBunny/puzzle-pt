package com.example.puzzle_pt

import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.engine.SudokuSolver
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuRulesTest {
    @Test
    fun allBuiltInPuzzlesHaveValidUniqueSolutions() {
        assertEquals(105, PuzzleRepository.puzzles.size)
        assertEquals(105, PuzzleRepository.puzzles.map { it.id }.distinct().size)
        PuzzleRepository.puzzles.forEach { puzzle ->
            assertTrue(puzzle.id, SudokuRules.isComplete(puzzle.solution, puzzle))
            assertEquals(puzzle.id, puzzle.solution, SudokuSolver.solve(puzzle))
            assertEquals(puzzle.id, 1, SudokuSolver.countSolutions(puzzle))
        }
    }

    @Test
    fun everyTypeAndDifficultyHasFivePuzzles() {
        SudokuType.entries.forEach { type ->
            assertEquals(type.title, 15, PuzzleRepository.puzzlesOf(type).size)
            Difficulty.entries.forEach { difficulty ->
                assertEquals(
                    "${type.title} ${difficulty.title}",
                    5,
                    PuzzleRepository.puzzles.count { it.type == type && it.difficulty == difficulty },
                )
            }
        }
    }

    @Test
    fun diagonalDuplicatesAreRejected() {
        val puzzle = PuzzleRepository.puzzleById("diagonal-easy")
        val board = MutableList(36) { 0 }
        board[0] = 1
        board[7] = 1
        val conflicts = SudokuRules.conflictingCells(board, puzzle)
        assertTrue(conflicts[0])
        assertTrue(conflicts[7])
    }

    @Test
    fun knightMoveDuplicatesAreRejected() {
        val puzzle = PuzzleRepository.puzzleById("anti-knight-easy")
        val board = MutableList(36) { 0 }
        board[0] = 1
        board[13] = 1
        val conflicts = SudokuRules.conflictingCells(board, puzzle)
        assertTrue(conflicts[0])
        assertTrue(conflicts[13])
    }

    @Test
    fun incorrectKillerCageSumIsRejected() {
        val puzzle = PuzzleRepository.puzzleById("killer-easy")
        val board = MutableList(36) { 0 }
        board[0] = 1
        board[1] = 2
        board[2] = 3
        val conflicts = SudokuRules.conflictingCells(board, puzzle)
        assertTrue(conflicts[0])
        assertTrue(conflicts[1])
        assertTrue(conflicts[2])
    }

    @Test
    fun skyscraperVisibilityIsCheckedFromEverySide() {
        assertEquals(6, SudokuRules.visibleBuildings(listOf(1, 2, 3, 4, 5, 6)))
        assertEquals(1, SudokuRules.visibleBuildings(listOf(6, 1, 2, 3, 4, 5)))
        assertEquals(3, SudokuRules.visibleBuildings(listOf(2, 1, 4, 3, 6, 5)))
        val puzzle = PuzzleRepository.puzzleById("skyscraper-easy")
        val wrongClues = puzzle.skyscraperClues.toMutableList().apply { this[0] = 1 }
        val wrongPuzzle = puzzle.copy(skyscraperClues = wrongClues)
        assertFalse(SudokuRules.isComplete(puzzle.solution, wrongPuzzle))
    }

    @Test
    fun sevenGridUsesIrregularRegions() {
        val puzzle = PuzzleRepository.puzzleById("seven-easy")
        val board = MutableList(puzzle.cellCount) { 0 }
        board[0] = 1
        board[8] = 1
        val conflicts = SudokuRules.conflictingCells(board, puzzle)
        assertTrue(conflicts[0])
        assertTrue(conflicts[8])
        assertTrue(8 in SudokuRules.relatedCells(0, puzzle))
        assertFalse(13 in SudokuRules.relatedCells(0, puzzle))
    }

    @Test
    fun nineGridUsesThreeByThreeBoxes() {
        val puzzle = PuzzleRepository.puzzleById("nine-easy")
        val board = MutableList(puzzle.cellCount) { 0 }
        board[0] = 1
        board[10] = 1
        val conflicts = SudokuRules.conflictingCells(board, puzzle)
        assertTrue(conflicts[0])
        assertTrue(conflicts[10])
        assertTrue(10 in SudokuRules.relatedCells(0, puzzle))
        assertFalse(30 in SudokuRules.relatedCells(0, puzzle))
    }

    @Test
    fun candidatesUseThePuzzleSize() {
        val seven = PuzzleRepository.puzzleById("seven-easy")
        val nine = PuzzleRepository.puzzleById("nine-easy")
        assertEquals((1..7).toList(), SudokuRules.candidates(IntArray(49), 0, seven))
        assertEquals((1..9).toList(), SudokuRules.candidates(IntArray(81), 0, nine))
    }

    @Test
    fun incompleteBoardIsNotComplete() {
        val puzzle = PuzzleRepository.puzzleById("standard-easy")
        val board = puzzle.solution.toMutableList().apply { this[2] = 0 }
        assertFalse(SudokuRules.isComplete(board, puzzle))
    }
}

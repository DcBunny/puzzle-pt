package com.example.puzzle_pt

import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.data.savedBoardMatchesGivens
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressStoreTest {
    @Test
    fun savedBoardMustMatchCurrentGivens() {
        val puzzle = PuzzleRepository.puzzleById("killer-easy")
        assertTrue(savedBoardMatchesGivens(puzzle.solution, puzzle))

        val givenIndex = puzzle.givens.indexOfFirst { it != 0 }
        val nonGivenIndex = puzzle.givens.indexOfFirst { it == 0 }

        val clearedGiven = puzzle.solution.toMutableList().apply { this[givenIndex] = 0 }
        assertFalse(savedBoardMatchesGivens(clearedGiven, puzzle))

        val changedGiven = puzzle.solution.toMutableList().apply {
            this[givenIndex] = if (this[givenIndex] == 1) 2 else 1
        }
        assertFalse(savedBoardMatchesGivens(changedGiven, puzzle))

        val changedNonGiven = puzzle.solution.toMutableList().apply {
            this[nonGivenIndex] = if (this[nonGivenIndex] == 1) 2 else 1
        }
        assertTrue(savedBoardMatchesGivens(changedNonGiven, puzzle))

        assertFalse(savedBoardMatchesGivens(puzzle.solution.dropLast(1), puzzle))
    }
}

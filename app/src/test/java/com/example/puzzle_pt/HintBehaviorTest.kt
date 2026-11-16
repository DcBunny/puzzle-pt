package com.example.puzzle_pt

import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.engine.TeachingGuide
import com.example.puzzle_pt.model.HintKind
import com.example.puzzle_pt.model.RuleKind
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintBehaviorTest {
    @Test
    fun locallyLegalWrongEntryIsNotCalledCorrect() {
        val puzzle = PuzzleRepository.puzzleById("standard-hard")
        val board = puzzle.givens.toMutableList()
        board[0] = 5
        assertFalse(SudokuRules.conflictingCells(board, puzzle)[0])
        assertEquals("目前没有直接冲突", null, SudokuRules.explainCell(board, puzzle, 0))
        val hint = TeachingGuide.hint(puzzle, board)
        assertTrue(hint.kind == HintKind.UNSOLVABLE || hint.kind == HintKind.UNCERTAIN)
        assertNull(hint.value)
        assertFalse(hint.explanation.contains("可以填"))
    }

    @Test
    fun hintsDoNotQuoteAnUnforcedSolution() {
        PuzzleRepository.puzzles.forEach { puzzle ->
            val hint = TeachingGuide.hint(puzzle, puzzle.givens)
            if (hint.kind == HintKind.PLACE) {
                assertEquals(puzzle.id, puzzle.solution[hint.index!!], hint.value)
                assertTrue(puzzle.id, hint.explanation.contains(hint.value.toString()))
            } else {
                assertNull(puzzle.id, hint.value)
            }
        }
    }

    @Test
    fun wrongEntryDoesNotProduceAContradictoryPlacement() {
        val puzzle = PuzzleRepository.puzzleById("standard-hard")
        val board = puzzle.givens.toMutableList()
        board[0] = 5
        val hint = TeachingGuide.hint(puzzle, board)
        assertNotEquals(HintKind.PLACE, hint.kind)
        val cell = 7
        val only = SudokuRules.basicCandidates(board.toIntArray(), cell, puzzle)
        if (only.size == 1) {
            assertTrue(hint.explanation.contains("无法") || hint.explanation.contains("无法确认") || hint.kind != HintKind.PLACE)
        }
    }

    @Test
    fun incompleteKillerCageThatCannotReachItsSumIsMarked() {
        val puzzle = PuzzleRepository.puzzleById("killer-easy")
        val board = puzzle.givens.toMutableList()
        board[6] = 1
        val violations = SudokuRules.violations(board, puzzle)
        assertTrue(violations.any { it.kind == RuleKind.CAGE_SUM && 6 in it.cells })
        assertTrue(SudokuRules.conflictingCells(board, puzzle)[6])
        assertTrue(SudokuRules.explainCell(board, puzzle, 6)!!.contains("凑不出来"))
    }

    @Test
    fun conflictTextFollowsTheBrokenRuleRatherThanThePuzzleType() {
        val killer = PuzzleRepository.puzzleById("killer-easy")
        val duplicate = killer.givens.toMutableList()
        duplicate[0] = 4
        val rowHit = SudokuRules.violations(duplicate, killer).first { 0 in it.cells }
        assertEquals(RuleKind.ROW, rowHit.kind)
        assertTrue(SudokuRules.explain(rowHit, killer).contains("行"))

        val knight = PuzzleRepository.puzzleById("anti-knight-easy")
        val knightBoard = MutableList(knight.cellCount) { 0 }
        knightBoard[0] = 1
        knightBoard[13] = 1
        val knightHit = SudokuRules.violations(knightBoard, knight).first { it.kind == RuleKind.KNIGHT }
        assertTrue(SudokuRules.explain(knightHit, knight).contains("马步"))

        val sky = PuzzleRepository.puzzleById("skyscraper-easy")
        val wrong = sky.copy(skyscraperClues = List(sky.skyscraperClues.size) { 9 })
        val found = SudokuRules.violations(sky.solution, wrong)
        val skyHit = found.firstOrNull { it.kind == RuleKind.SKYSCRAPER }
            ?: error("没有摩天楼冲突：${found.map { it.kind }}")
        assertTrue(SudokuRules.explain(skyHit, wrong).contains("楼"))
        assertFalse(sky.type == SudokuType.STANDARD)
    }
}

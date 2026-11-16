package com.example.puzzle_pt.engine

import com.example.puzzle_pt.model.SudokuPuzzle

data class SolutionProbe(
    val solutions: Int,
    val complete: Boolean,
)

object SudokuSolver {
    fun countSolutions(puzzle: SudokuPuzzle, limit: Int = 2): Int {
        val result = probe(puzzle, puzzle.givens.toIntArray(), limit, Int.MAX_VALUE)
        check(result.complete)
        return result.solutions
    }

    fun probe(
        puzzle: SudokuPuzzle,
        board: IntArray,
        limit: Int = 2,
        maxNodes: Int = 30_000,
    ): SolutionProbe {
        require(board.size == puzzle.cellCount)
        val working = board.copyOf()
        var count = 0
        var nodes = 0
        var complete = true

        fun search() {
            if (!complete || count >= limit) return
            nodes++
            if (nodes > maxNodes) {
                complete = false
                return
            }
            var bestIndex = -1
            var bestCandidates: List<Int> = emptyList()
            for (index in working.indices) {
                if (working[index] != 0) continue
                val candidates = SudokuRules.candidates(working, index, puzzle)
                if (candidates.isEmpty()) return
                if (bestIndex == -1 || candidates.size < bestCandidates.size) {
                    bestIndex = index
                    bestCandidates = candidates
                    if (candidates.size == 1) break
                }
            }
            if (bestIndex == -1) {
                count++
                return
            }
            for (value in bestCandidates) {
                working[bestIndex] = value
                search()
                working[bestIndex] = 0
                if (!complete || count >= limit) return
            }
        }

        search()
        return SolutionProbe(count, complete)
    }

    fun solve(puzzle: SudokuPuzzle): List<Int>? {
        val board = puzzle.givens.toIntArray()

        fun search(): Boolean {
            var bestIndex = -1
            var bestCandidates: List<Int> = emptyList()
            for (index in board.indices) {
                if (board[index] != 0) continue
                val candidates = SudokuRules.candidates(board, index, puzzle)
                if (candidates.isEmpty()) return false
                if (bestIndex == -1 || candidates.size < bestCandidates.size) {
                    bestIndex = index
                    bestCandidates = candidates
                    if (candidates.size == 1) break
                }
            }
            if (bestIndex == -1) return true
            for (value in bestCandidates) {
                board[bestIndex] = value
                if (search()) return true
                board[bestIndex] = 0
            }
            return false
        }

        return if (search()) board.toList() else null
    }
}

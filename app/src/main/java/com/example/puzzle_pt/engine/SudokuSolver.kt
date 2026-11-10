package com.example.puzzle_pt.engine

import com.example.puzzle_pt.model.SudokuPuzzle

object SudokuSolver {
    fun countSolutions(puzzle: SudokuPuzzle, limit: Int = 2): Int {
        val board = puzzle.givens.toIntArray()
        var count = 0

        fun search() {
            if (count >= limit) return
            var bestIndex = -1
            var bestCandidates: List<Int> = emptyList()
            for (index in board.indices) {
                if (board[index] != 0) continue
                val candidates = SudokuRules.candidates(board, index, puzzle)
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
            bestCandidates.forEach { value ->
                board[bestIndex] = value
                search()
                board[bestIndex] = 0
                if (count >= limit) return
            }
        }

        search()
        return count
    }

    fun solve(puzzle: SudokuPuzzle): List<Int>? {
        val working = puzzle.copy(solution = puzzle.solution)
        val board = working.givens.toIntArray()

        fun search(): Boolean {
            var bestIndex = -1
            var bestCandidates: List<Int> = emptyList()
            for (index in board.indices) {
                if (board[index] != 0) continue
                val candidates = SudokuRules.candidates(board, index, working)
                if (candidates.isEmpty()) return false
                if (bestIndex == -1 || candidates.size < bestCandidates.size) {
                    bestIndex = index
                    bestCandidates = candidates
                    if (candidates.size == 1) break
                }
            }
            if (bestIndex == -1) return true
            bestCandidates.forEach { value ->
                board[bestIndex] = value
                if (search()) return true
                board[bestIndex] = 0
            }
            return false
        }

        return if (search()) board.toList() else null
    }
}

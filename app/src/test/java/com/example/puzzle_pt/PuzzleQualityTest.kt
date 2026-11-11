package com.example.puzzle_pt

import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.engine.SudokuSolver
import com.example.puzzle_pt.model.Difficulty
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleQualityTest {
    private val variantTypes = setOf(
        SudokuType.KILLER,
        SudokuType.SKYSCRAPER,
        SudokuType.DIAGONAL,
        SudokuType.ANTI_KNIGHT,
    )

    @Test
    fun everyVariantPuzzleNeedsItsVariantRule() {
        PuzzleRepository.puzzles.filter { it.type in variantTypes }.forEach { puzzle ->
            val standardOnly = puzzle.copy(type = SudokuType.STANDARD)
            assertTrue(
                "${puzzle.id} 在只用普通规则时仍只有唯一解，变体规则没有参与",
                SudokuSolver.countSolutions(standardOnly, 2) > 1,
            )
        }
    }

    @Test
    fun givensMatchDifficultyProfile() {
        PuzzleRepository.puzzles.forEach { puzzle ->
            val count = puzzle.givens.count { it != 0 }
            assertTrue(
                "${puzzle.id} 已知数 $count 不在 ${
                    expectedGivensRange(puzzle.type.size, puzzle.difficulty)
                } 内",
                count in expectedGivensRange(puzzle.type.size, puzzle.difficulty),
            )
        }
    }

    @Test
    fun difficultyAveragesAreMonotonic() {
        SudokuType.entries.forEach { type ->
            val averages = Difficulty.entries.associateWith { difficulty ->
                PuzzleRepository.puzzlesOf(type)
                    .filter { it.difficulty == difficulty }
                    .map { it.givens.count { cell -> cell != 0 } }
                    .average()
            }
            assertTrue(
                "${type.title} 入门平均已知数应多于进阶",
                averages.getValue(Difficulty.EASY) > averages.getValue(Difficulty.MEDIUM),
            )
            assertTrue(
                "${type.title} 进阶平均已知数应多于挑战",
                averages.getValue(Difficulty.MEDIUM) > averages.getValue(Difficulty.HARD),
            )
        }
    }

    @Test
    fun killerCagesCoverBoardAndMatchSolution() {
        PuzzleRepository.puzzles.filter { it.type == SudokuType.KILLER }.forEach { puzzle ->
            val cells = puzzle.cages.flatMap { cage -> cage.cells.map { it.index } }
            assertEquals("${puzzle.id} 笼格数量", puzzle.cellCount, cells.size)
            assertEquals("${puzzle.id} 笼格应不重不漏", (0 until puzzle.cellCount).toSet(), cells.toSet())
            assertTrue("${puzzle.id} 笼数应为 10–14", puzzle.cages.size in 10..14)
            assertTrue(
                "${puzzle.id} 应至少有一个跨宫笼",
                puzzle.cages.any { cage -> cage.cells.map { boxId(it.row, it.column) }.distinct().size > 1 },
            )
            assertTrue(
                "${puzzle.id} 应至少有一个非单行笼",
                puzzle.cages.any { cage -> cage.cells.map { it.row }.distinct().size > 1 },
            )
            puzzle.cages.forEach { cage ->
                assertTrue("${puzzle.id} 每笼应为 2–4 格", cage.cells.size in 2..4)
                assertEquals(
                    "${puzzle.id} 笼内解数字不能重复",
                    cage.cells.size,
                    cage.cells.map { puzzle.solution[it.index] }.distinct().size,
                )
                assertEquals(
                    "${puzzle.id} 笼和应与解一致",
                    cage.sum,
                    cage.cells.sumOf { puzzle.solution[it.index] },
                )
                assertTrue(
                    "${puzzle.id} 每个笼应连通",
                    isConnected(cage.cells.map { it.index }, puzzle.size),
                )
                assertEquals(
                    "${puzzle.id} 笼和应画在左上格",
                    cage.cells.minWith(compareBy({ it.row }, { it.column })).index,
                    cage.labelIndex,
                )
            }
        }
    }

    @Test
    fun killerCageLayoutsAreDiverse() {
        val signatures = PuzzleRepository.puzzles
            .filter { it.type == SudokuType.KILLER }
            .map { puzzle ->
                puzzle.cages
                    .map { cage -> cage.cells.map { it.index }.sorted() }
                    .sortedBy { it.joinToString(",") }
            }
            .distinct()
        assertTrue("杀手笼布局至少应有 10 种，当前 ${signatures.size}", signatures.size >= 10)
    }

    @Test
    fun skyscraperCluesMatchEveryDirection() {
        PuzzleRepository.puzzles.filter { it.type == SudokuType.SKYSCRAPER }.forEach { puzzle ->
            assertEquals("${puzzle.id} 线索数量", puzzle.size * 4, puzzle.skyscraperClues.size)
            val lines = SudokuRules.skyscraperLines(puzzle.size)
            puzzle.skyscraperClues.forEachIndexed { index, clue ->
                assertTrue("${puzzle.id} 第 $index 个线索越界", clue in 1..puzzle.size)
                assertEquals(
                    "${puzzle.id} 第 $index 个线索与解不一致",
                    clue,
                    SudokuRules.visibleBuildings(lines[index].map { puzzle.solution[it] }),
                )
            }
        }
    }

    @Test
    fun sevenGridRegionsAreConnected() {
        PuzzleRepository.puzzles.filter { it.type == SudokuType.SEVEN }.forEach { puzzle ->
            assertEquals("${puzzle.id} 宫区数量", puzzle.cellCount, puzzle.regions.size)
            for (region in 0 until puzzle.size) {
                val cells = puzzle.regions.indices.filter { puzzle.regions[it] == region }
                assertEquals("${puzzle.id} 第 $region 宫大小", puzzle.size, cells.size)
                assertTrue("${puzzle.id} 第 $region 宫应连通", isConnected(cells, puzzle.size))
            }
        }
    }

    @Test
    fun solutionsAreDistinctWithinType() {
        SudokuType.entries.forEach { type ->
            val duplicates = PuzzleRepository.puzzlesOf(type)
                .groupBy { it.solution }
                .filterValues { it.size > 1 }
            assertTrue(
                "${type.title} 存在重复解：${duplicates.values.map { puzzles -> puzzles.map { it.id } }}",
                duplicates.isEmpty(),
            )
        }
    }

    @Test
    fun lessonCriticalValuesRemainStable() {
        val killer = PuzzleRepository.puzzleById("killer-easy")
        assertEquals(listOf(0, 1, 2), killer.cages.first().cells.map { it.index })
        assertEquals(0, killer.cages.first().labelIndex)
        assertEquals(10, killer.cages.first().sum)
        assertEquals(5, killer.solution[2])

        val skyscraper = PuzzleRepository.puzzleById("skyscraper-easy")
        assertEquals(3, skyscraper.skyscraperClues[skyscraper.size * 3])
        val firstRow = (0 until skyscraper.size).map { skyscraper.solution[it] }
        val visibleFromLeft = mutableListOf<Int>()
        var tallest = 0
        firstRow.forEach { value ->
            if (value > tallest) {
                tallest = value
                visibleFromLeft += value
            }
        }
        assertEquals(listOf(2, 5, 6), visibleFromLeft)

        val diagonal = PuzzleRepository.puzzleById("diagonal-easy")
        assertEquals(
            listOf(5, 1, 3, 4, 6, 2),
            (0 until diagonal.size).map { diagonal.solution[it * diagonal.size + it] },
        )
        assertEquals(4, diagonal.solution[21])
        assertTrue(
            "对角线课程示例需要至少一个对角线空格",
            (0 until diagonal.size).any { diagonal.givens[it * diagonal.size + it] == 0 } ||
                (0 until diagonal.size).any { diagonal.givens[it * diagonal.size + (diagonal.size - 1 - it)] == 0 },
        )

        val knight = PuzzleRepository.puzzleById("anti-knight-medium")
        assertEquals(0, knight.givens[1])
        assertEquals(4, knight.solution[1])
        assertEquals(2, knight.givens[14])
    }

    private fun expectedGivensRange(size: Int, difficulty: Difficulty): IntRange = when (size) {
        6 -> when (difficulty) {
            Difficulty.EASY -> 19..24
            Difficulty.MEDIUM -> 14..19
            Difficulty.HARD -> 10..14
        }
        7 -> when (difficulty) {
            Difficulty.EASY -> 24..25
            Difficulty.MEDIUM -> 18..20
            Difficulty.HARD -> 14..16
        }
        9 -> when (difficulty) {
            Difficulty.EASY -> 44..46
            Difficulty.MEDIUM -> 34..36
            Difficulty.HARD -> 28..30
        }
        else -> error("未定义的棋盘尺寸 $size")
    }

    private fun boxId(row: Int, column: Int): Int = (row / 2) * 2 + column / 3

    private fun isConnected(cells: List<Int>, size: Int): Boolean {
        if (cells.isEmpty()) return false
        val remaining = cells.toSet()
        val visited = mutableSetOf(cells.first())
        val queue = ArrayDeque<Int>()
        queue.add(cells.first())
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val row = index / size
            val column = index % size
            val neighbours = listOf(
                index - size,
                index + size,
                if (column > 0) index - 1 else -1,
                if (column < size - 1) index + 1 else -1,
            )
            neighbours.filter { it in remaining && it !in visited }.forEach { neighbour ->
                visited += neighbour
                queue.add(neighbour)
            }
        }
        return visited == remaining
    }
}

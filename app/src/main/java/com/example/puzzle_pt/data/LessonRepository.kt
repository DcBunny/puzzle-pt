package com.example.puzzle_pt.data

import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType

enum class LessonVisualKind {
    NUMBER_FRIENDS,
    GROUP_TOUR,
    STANDARD_DUPLICATE,
    STANDARD_MISSING,
    KILLER_HOUSE,
    KILLER_ADD,
    KILLER_MAKE_SUM,
    KILLER_DUPLICATE,
    SKY_HEIGHTS,
    SKY_COUNT,
    SKY_QUIZ,
    SKY_SIX_FIRST,
    DIAGONAL_LINES,
    DIAGONAL_NUMBERS,
    DIAGONAL_TRACE,
    DIAGONAL_DUPLICATE,
    KNIGHT_MOVE,
    KNIGHT_FIND_MOVES,
    KNIGHT_SAME,
    KNIGHT_CHOOSE_SAFE,
}

enum class LessonInteraction {
    NONE,
    FIND_STANDARD_DUPLICATE,
    FIND_STANDARD_MISSING,
    MAKE_KILLER_SUM,
    FIND_KILLER_DUPLICATE,
    COUNT_SKYSCRAPERS,
    TRACE_DIAGONAL,
    FIND_DIAGONAL_DUPLICATE,
    FIND_KNIGHT_MOVES,
    CHOOSE_SAFE_KNIGHT_NUMBER,
}

data class LessonStep(
    val title: String,
    val explanation: String,
    val action: String = "",
    val visual: LessonVisualKind,
    val interaction: LessonInteraction = LessonInteraction.NONE,
)

object LessonRepository {
    fun stepsFor(type: SudokuType): List<LessonStep> {
        val steps = when (type) {
            SudokuType.STANDARD -> listOf(
                LessonStep(
                    "数字小伙伴来排队",
                    "每个小队都要有 1 到 6。",
                    "一样的数字不能来两次。",
                    LessonVisualKind.NUMBER_FRIENDS,
                ),
                LessonStep(
                    "找找三个数字小队",
                    "横着叫一行，竖着叫一列。",
                    "粗线围起来的小家，叫做一宫。",
                    LessonVisualKind.GROUP_TOUR,
                ),
                LessonStep(
                    "谁多来了一个？",
                    "这个小队里，有一个数字出现了两次。",
                    "点一点重复的 2。",
                    LessonVisualKind.STANDARD_DUPLICATE,
                    LessonInteraction.FIND_STANDARD_DUPLICATE,
                ),
                LessonStep(
                    "谁还没有来？",
                    "小队在等一个数字小伙伴。",
                    "点一下缺少的数字牌。",
                    LessonVisualKind.STANDARD_MISSING,
                    LessonInteraction.FIND_STANDARD_MISSING,
                ),
            )
            SudokuType.KILLER -> listOf(
                LessonStep(
                    "数字住进加法小屋",
                    "虚线围起来的地方，就是加法小屋。",
                    "小屋里也要遵守数独的小队规则。",
                    LessonVisualKind.KILLER_HOUSE,
                ),
                LessonStep(
                    "屋角的小数会说话",
                    "1、2、3 合起来是 6。",
                    "所以屋角写着 6。",
                    LessonVisualKind.KILLER_ADD,
                ),
                LessonStep(
                    "帮小屋凑到 6",
                    "选一些数字牌放进小屋。",
                    "让它们合起来正好是 6。",
                    LessonVisualKind.KILLER_MAKE_SUM,
                    LessonInteraction.MAKE_KILLER_SUM,
                ),
                LessonStep(
                    "一样的数字不能住一起",
                    "这间小屋里，2 来了两次。",
                    "点一点重复的 2。",
                    LessonVisualKind.KILLER_DUPLICATE,
                    LessonInteraction.FIND_KILLER_DUPLICATE,
                ),
            )
            SudokuType.SKYSCRAPER -> listOf(
                LessonStep(
                    "数字变成小楼",
                    "数字是几，小楼就有几层高。",
                    "6 楼最高，1 楼最矮。",
                    LessonVisualKind.SKY_HEIGHTS,
                ),
                LessonStep(
                    "高楼挡住矮楼",
                    "从箭头这边向小楼看。",
                    "前面更高的楼，会挡住后面的矮楼。",
                    LessonVisualKind.SKY_COUNT,
                ),
                LessonStep(
                    "你看到几栋楼？",
                    "先选从哪边看，再慢慢数一数。",
                    "只看到挡住别人、或没被挡住的高楼。",
                    LessonVisualKind.SKY_QUIZ,
                    LessonInteraction.COUNT_SKYSCRAPERS,
                ),
                LessonStep(
                    "线索 1 的小秘密",
                    "如果只能看到 1 栋楼，",
                    "最高的 6 楼一定站在最前面。",
                    LessonVisualKind.SKY_SIX_FIRST,
                ),
            )
            SudokuType.DIAGONAL -> listOf(
                LessonStep(
                    "两条彩色滑梯线",
                    "角对角连起来，就是一条滑梯线。",
                    "蓝色和粉色滑梯，都住着数字小伙伴。",
                    LessonVisualKind.DIAGONAL_LINES,
                ),
                LessonStep(
                    "滑梯小队也不重复",
                    "每条滑梯上，都要有 1 到 6。",
                    "一样的数字不能来两次。",
                    LessonVisualKind.DIAGONAL_NUMBERS,
                ),
                LessonStep(
                    "沿着滑梯走一走",
                    "蓝色滑梯一共有 6 格。",
                    "把这 6 格全部点亮。",
                    LessonVisualKind.DIAGONAL_TRACE,
                    LessonInteraction.TRACE_DIAGONAL,
                ),
                LessonStep(
                    "谁在滑梯上重复了？",
                    "粉色滑梯上，4 来了两次。",
                    "点一点其中一个 4。",
                    LessonVisualKind.DIAGONAL_DUPLICATE,
                    LessonInteraction.FIND_DIAGONAL_DUPLICATE,
                ),
            )
            SudokuType.ANTI_KNIGHT -> listOf(
                LessonStep(
                    "小马跳一个“日”字",
                    "小马先走两格，再拐一格。",
                    "跟着亮起来的小路看一看。",
                    LessonVisualKind.KNIGHT_MOVE,
                ),
                LessonStep(
                    "小马能跳到哪里？",
                    "从这个“马”字出发，找一找落点。",
                    "点亮小马能跳到的 4 个格子。",
                    LessonVisualKind.KNIGHT_FIND_MOVES,
                    LessonInteraction.FIND_KNIGHT_MOVES,
                ),
                LessonStep(
                    "相同数字不能见面",
                    "小马和落点上，不能住一样的数字。",
                    "不同的数字就可以。",
                    LessonVisualKind.KNIGHT_SAME,
                ),
                LessonStep(
                    "哪个数字安全？",
                    "小马那里住着一个 2。",
                    "选一个不是 2 的数字放到落点。",
                    LessonVisualKind.KNIGHT_CHOOSE_SAFE,
                    LessonInteraction.CHOOSE_SAFE_KNIGHT_NUMBER,
                ),
            )
        }
        return steps + LessonStep(
            "你来试一试",
            "现在回到真正的六宫棋盘。",
            "给发光的小格填上正确的数字。",
            LessonVisualKind.NUMBER_FRIENDS,
        )
    }

    fun examplePuzzle(type: SudokuType): SudokuPuzzle {
        val puzzle = PuzzleRepository.puzzlesOf(type).first()
        return puzzle.copy(givens = puzzle.solution)
    }

    fun guidedPuzzle(type: SudokuType): SudokuPuzzle {
        val puzzle = PuzzleRepository.puzzlesOf(type).first()
        val target = puzzle.givens.indices.first { index ->
            val row = index / 6
            val column = index % 6
            puzzle.givens[index] == 0 && (
                type != SudokuType.DIAGONAL || row == column || row + column == 5
                )
        }
        val givens = puzzle.solution.toMutableList().apply { this[target] = 0 }
        return puzzle.copy(givens = givens)
    }
}

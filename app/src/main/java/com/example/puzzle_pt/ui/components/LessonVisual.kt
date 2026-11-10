package com.example.puzzle_pt.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzle_pt.data.LessonStep
import com.example.puzzle_pt.data.LessonVisualKind
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.CELL_COUNT
import com.example.puzzle_pt.model.SudokuPuzzle

@Composable
fun LessonVisual(
    step: LessonStep,
    puzzle: SudokuPuzzle,
    replayKey: Int,
    onSolved: () -> Unit,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(step.visual, replayKey) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(1400))
    }
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            key(step.visual, replayKey) {
                when (step.visual) {
                    LessonVisualKind.NUMBER_FRIENDS -> NumberFriends(reveal.value)
                    LessonVisualKind.GROUP_TOUR -> GroupTour(reveal.value)
                    LessonVisualKind.STANDARD_DUPLICATE -> StandardDuplicate(onSolved)
                    LessonVisualKind.STANDARD_MISSING -> StandardMissing(onSolved)
                    LessonVisualKind.KILLER_HOUSE -> KillerHouseDemo(reveal.value, false)
                    LessonVisualKind.KILLER_ADD -> KillerAddition(reveal.value)
                    LessonVisualKind.KILLER_MAKE_SUM -> KillerMakeSum(puzzle, onSolved)
                    LessonVisualKind.KILLER_DUPLICATE -> KillerDuplicate(onSolved)
                    LessonVisualKind.SKY_HEIGHTS -> SkyBuildingQuiz(onSolved)
                    LessonVisualKind.SKY_COUNT -> SkyCountDemo(reveal.value)
                    LessonVisualKind.SKY_QUIZ -> SkyQuiz(puzzle, onSolved)
                    LessonVisualKind.SKY_SIX_FIRST -> SkySixFirst(reveal.value)
                    LessonVisualKind.DIAGONAL_LINES -> DiagonalDemo(reveal.value, showNumbers = false)
                    LessonVisualKind.DIAGONAL_NUMBERS -> DiagonalDemo(reveal.value, showNumbers = true)
                    LessonVisualKind.DIAGONAL_TRACE -> DiagonalTrace(onSolved)
                    LessonVisualKind.DIAGONAL_DUPLICATE -> DiagonalDuplicate(puzzle, onSolved)
                    LessonVisualKind.KNIGHT_MOVE -> KnightMoveDemo(reveal.value)
                    LessonVisualKind.KNIGHT_FIND_MOVES -> KnightFindMoves(onSolved)
                    LessonVisualKind.KNIGHT_SAME -> KnightSameDemo()
                    LessonVisualKind.KNIGHT_CHOOSE_SAFE -> KnightChooseSafe(puzzle, onSolved)
                }
            }
        }
    }
}

@Composable
private fun NumberFriends(reveal: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (value in 1..6) {
            NumberBadge(
                value = if (value <= reveal * 6 + 0.5f) value else null,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
    Text(
        "1、2、3、4、5、6，一个也不少！",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun GroupTour(reveal: Float) {
    val phase = (reveal * 3).toInt().coerceIn(0, 2)
    val highlighted = when (phase) {
        0 -> List(6) { 2 * 6 + it }.toSet()
        1 -> List(6) { it * 6 + 4 }.toSet()
        else -> setOf(0, 1, 2, 6, 7, 8)
    }
    MiniGrid(
        size = 6,
        cellSize = 38.dp,
        cellContent = { row, column ->
            val index = row * 6 + column
            Box(
                Modifier.fillMaxSize().background(
                    if (index in highlighted) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
    )
    Text(
        when (phase) {
            0 -> "这一横排，是一行"
            1 -> "这一竖排，是一列"
            else -> "粗线里的 6 格，是一宫"
        },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun StandardDuplicate(onSolved: () -> Unit) {
    val values = listOf(1, 2, 3, 2, 5, 6)
    var message by remember { mutableStateOf("哪一个 2 都可以点") }
    var solved by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEachIndexed { index, value ->
            NumberBadge(
                value = value,
                color = if (solved && value == 2) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                tag = "visual_standard_duplicate_$index",
                onClick = {
                    if (value == 2) {
                        solved = true
                        message = "找到了！两个 2 不能在同一个队里。"
                        onSolved()
                    } else {
                        message = "$value 只来了一次，再找一找。"
                    }
                },
            )
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun StandardMissing(onSolved: () -> Unit) {
    val values = listOf(1, 2, 3, null, 5, 6)
    var message by remember { mutableStateOf("先看看谁已经来了") }
    var solved by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            NumberBadge(value = value, color = MaterialTheme.colorScheme.secondaryContainer)
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (value in 1..6) {
            NumberBadge(
                value = value,
                color = MaterialTheme.colorScheme.primaryContainer,
                tag = "visual_standard_missing_$value",
                onClick = {
                    if (value == 4) {
                        solved = true
                        message = "答对了！小队正好缺少 4。"
                        onSolved()
                    } else {
                        message = "$value 已经在小队里了，再看看空格。"
                    }
                },
            )
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun KillerHouseDemo(reveal: Float, showEquation: Boolean) {
    Box(Modifier.size(270.dp, 170.dp)) {
        val cageColor = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = cageColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f)),
                ),
            )
        }
        Text(
            "6",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(10.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (value in 1..3) {
                NumberBadge(
                    value = if (value <= reveal * 3 + 0.4f) value else null,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
        }
        if (showEquation) {
            Text(
                "1 + 2 + 3 = 6",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun KillerAddition(reveal: Float) {
    KillerHouseDemo(reveal, true)
}

@Composable
private fun KillerMakeSum(puzzle: SudokuPuzzle, onSolved: () -> Unit) {
    val target = 2
    val cageCells = puzzle.cages.first().cells.map { it.index }.toSet()
    var entry by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("第一行小屋：1 + 4 + ? = 10") }
    val solved = entry == puzzle.solution[target]
    val board = puzzle.solution.mapIndexed { index, value -> if (index == target) entry ?: 0 else value }
    SixGridLessonBoard(puzzle, board, highlightCells = cageCells, hintIndex = target)
    LessonNumberChoices("visual_killer_number") { value ->
        entry = value
        when (value) {
            puzzle.solution[target] -> {
                message = "对了！1 + 4 + 5 = 10，小屋里也没有重复数字。"
                onSolved()
            }
            1, 4 -> message = "小屋里已经有 $value 了，数字不能重复。"
            else -> message = "10 减去 1 和 4 等于 5，再算一算。"
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun KillerDuplicate(onSolved: () -> Unit) {
    var message by remember { mutableStateOf("小屋里只能有一个 2") }
    var solved by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(1, 2, 2).forEachIndexed { index, value ->
            NumberBadge(
                value = value,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tag = "visual_killer_duplicate_$index",
                onClick = {
                    if (value == 2) {
                        solved = true
                        message = "找到了！小屋里的数字也不能重复。"
                        onSolved()
                    } else {
                        message = "1 只来了一个，再看看另外两个。"
                    }
                },
            )
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun BuildingRow(
    heights: List<Int>,
    visible: Set<Int> = heights.indices.toSet(),
    reveal: Float = 1f,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { index, height ->
            val amount = (reveal * heights.size - index).coerceIn(0f, 1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(height.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height((22 * height + 12).dp * amount)
                        .background(
                            if (index in visible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SkyBuildingQuiz(onSolved: () -> Unit) {
    var answer by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("从左边看，只数比前面都高的楼。") }
    val solved = answer == 3
    Text("从左边看  →", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    BuildingRow(
        heights = listOf(2, 3, 1, 4),
        visible = setOf(0, 1, 3),
    )
    LessonNumberChoices("visual_sky_height_answer") { value ->
        answer = value
        if (value == 3) {
            message = "数对了！2 楼、3 楼和 4 楼能被看到，1 楼被挡住了。"
            onSolved()
        } else {
            message = "2 楼先看到，后面更高的 3 楼和 4 楼也能看到。"
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun SkyCountDemo(reveal: Float) {
    Text("从这里看  →", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    BuildingRow(listOf(2, 1, 4, 3), visible = setOf(0, 2), reveal = reveal)
    Text(
        if (reveal > 0.8f) "能看到 2 栋：2 楼和 4 楼" else "2 楼挡住 1 楼，4 楼又挡住后面的楼",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SkyQuiz(puzzle: SudokuPuzzle, onSolved: () -> Unit) {
    val visibleNumbers = setOf(2, 5, 6)
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf("把能看见的数字都选中，再点一次可以取消。") }
    val solved = selected == visibleNumbers
    SixGridLessonBoard(
        puzzle = puzzle,
        board = puzzle.solution,
        highlightCells = if (solved) setOf(0, 1, 5) else (0..5).toSet(),
    )
    Text(
        "从左边看  →  第一行外侧线索是 3",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        for (value in 1..6) {
            NumberBadge(
                value = value,
                size = 42.dp,
                color = if (value in selected) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
                tag = "visual_sky_answer_$value",
                onClick = {
                    if (!solved) {
                        selected = if (value in selected) selected - value else selected + value
                        when {
                            selected == visibleNumbers -> {
                                message = "找齐了！2、5、6 都能看见，正好是边缘数字 3。"
                                onSolved()
                            }
                            value !in visibleNumbers && value in selected ->
                                message = "$value 被前面更高的楼挡住了，再点一次可以取消。"
                            value !in selected -> message = "已取消 $value，继续找齐可见数字。"
                            else -> message = "已选中 ${selected.sorted().joinToString("、")}，继续找齐可见数字。"
                        }
                    }
                },
            )
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun SkySixFirst(reveal: Float) {
    Text("从这里看  →", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    BuildingRow(listOf(6, 2, 3, 1), visible = setOf(0), reveal = reveal)
    Text("6 楼把后面全挡住了，所以只看到 1 栋。", style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DiagonalDemo(reveal: Float, showNumbers: Boolean) {
    val values = listOf(5, 3, 4, 1, 6, 2)
    MiniGrid(
        size = 6,
        cellSize = 38.dp,
        cellContent = { row, column ->
            val main = row == column
            val anti = row + column == 5
            val shown = row <= (reveal * 6).toInt()
            Box(
                Modifier.fillMaxSize().background(
                    when {
                        !shown -> MaterialTheme.colorScheme.surfaceVariant
                        main && anti -> MaterialTheme.colorScheme.tertiaryContainer
                        main -> Color(0xFFBBD7FF)
                        anti -> Color(0xFFFFD4E2)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (showNumbers && main && shown) {
                    Text(values[row].toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        },
    )
    Text(
        if (showNumbers) "蓝色滑梯上，1 到 6 都来了" else "蓝色一条，粉色一条",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun DiagonalTrace(onSolved: () -> Unit) {
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf("从四个角中的一个角开始也可以") }
    val solved = selected.size == 6
    MiniGrid(
        size = 6,
        cellSize = 40.dp,
        cellContent = { row, column ->
            val index = row * 6 + column
            val main = row == column
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("visual_diagonal_cell_${row}_$column")
                    .background(
                        when {
                            index in selected -> MaterialTheme.colorScheme.tertiaryContainer
                            main -> Color(0xFFBBD7FF)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .clickable {
                        if (main) {
                            selected = selected + index
                            if (selected.size == 6) {
                                message = "6 格全亮了！这条滑梯也不能有重复数字。"
                                onSolved()
                            } else {
                                message = "点亮 ${selected.size} 格，再找 ${6 - selected.size} 格。"
                            }
                        } else {
                            message = "这一格不在蓝色滑梯上，找斜着相邻的格子。"
                        }
                    },
            )
        },
    )
    InteractionMessage(message, solved)
}

@Composable
private fun DiagonalDuplicate(puzzle: SudokuPuzzle, onSolved: () -> Unit) {
    val target = 21
    val diagonalCells = List(6) { it * 6 + it }.toSet()
    var entry by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("主对角线：5、1、3、?、6、2") }
    val solved = entry == puzzle.solution[target]
    val board = puzzle.solution.mapIndexed { index, value -> if (index == target) entry ?: 0 else value }
    SixGridLessonBoard(puzzle, board, highlightCells = diagonalCells, hintIndex = target)
    LessonNumberChoices("visual_diagonal_number") { value ->
        entry = value
        if (value == puzzle.solution[target]) {
            message = "对了！主对角线现在正好是 5、1、3、4、6、2。"
            onSolved()
        } else {
            message = "主对角线上已经有 $value 了，看看还缺少谁。"
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun KnightMoveDemo(reveal: Float) {
    val targets = knightTargets(2, 2, 5)
    val activeTargets = targets.take((reveal * (targets.size + 1)).toInt().coerceIn(0, targets.size)).toSet()
    Box(Modifier.size(230.dp)) {
        val pathColor = MaterialTheme.colorScheme.primary
        KnightGrid(5, 230.dp, 2 to 2, activeTargets)
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / 5
            val start = Offset(2.5f * cell, 2.5f * cell)
            if (activeTargets.isNotEmpty()) {
                val target = activeTargets.first()
                val end = Offset((target % 5 + 0.5f) * cell, (target / 5 + 0.5f) * cell)
                drawLine(pathColor, start, end, strokeWidth = 4.dp.toPx())
                drawCircle(pathColor, radius = 6.dp.toPx(), center = end)
            }
        }
    }
    Text("走两格，再拐一格，就是一个“日”字", style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun KnightFindMoves(onSolved: () -> Unit) {
    val targets = knightTargets(1, 1, 4)
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf("走两格，再拐一格") }
    val solved = selected == targets
    KnightGrid(
        gridSize = 4,
        boardSize = 220.dp,
        horse = 1 to 1,
        highlighted = selected,
        onCell = { row, column ->
            val index = row * 4 + column
            if (index in targets) {
                selected = selected + index
                if (selected == targets) {
                    message = "4 个落点全找到了，小马跳得真远！"
                    onSolved()
                } else {
                    message = "找到一个！还差 ${targets.size - selected.size} 个。"
                }
            } else {
                message = "这里不是“日”字落点，再数一数格子。"
            }
        },
        tagPrefix = "visual_knight_cell",
    )
    InteractionMessage(message, solved)
}

@Composable
private fun KnightSameDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        KnightPair(value = 2, allowed = false)
        KnightPair(value = 5, allowed = true)
    }
}

@Composable
private fun KnightPair(value: Int, allowed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(2, color = MaterialTheme.colorScheme.primaryContainer)
            Text("  跳  ", fontWeight = FontWeight.Bold)
            NumberBadge(
                value,
                color = if (allowed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
            )
        }
        Text(
            if (allowed) "数字不同，可以" else "都是 2，不可以",
            color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun KnightChooseSafe(puzzle: SudokuPuzzle, onSolved: () -> Unit) {
    val target = 1
    val knightCells = SudokuRules.knightCells(target)
    var entry by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("按行、列、宫，发光格可以填 2 或 4。") }
    val solved = entry == puzzle.solution[target]
    val board = puzzle.givens.mapIndexed { index, value -> if (index == target) entry ?: 0 else value }
    SixGridLessonBoard(puzzle, board, highlightCells = knightCells, hintIndex = target)
    LessonNumberChoices("visual_knight_safe") { value ->
        entry = value
        when (value) {
            puzzle.solution[target] -> {
                message = "对了！马步落点的 2 排除了 2，所以这里填 4。"
                onSolved()
            }
            2 -> message = "第 3 行第 3 列的 2 和这里是马步关系，所以这里不能填 2。"
            else -> message = "先看行、列、宫，这一格只可能填 2 或 4。"
        }
    }
    InteractionMessage(message, solved)
}

@Composable
private fun SixGridLessonBoard(
    puzzle: SudokuPuzzle,
    board: List<Int>,
    highlightCells: Set<Int>,
    hintIndex: Int? = null,
) {
    SudokuBoard(
        puzzle = puzzle,
        board = board,
        notes = List(CELL_COUNT) { emptySet() },
        selectedIndex = null,
        conflictCells = SudokuRules.conflictingCells(board, puzzle),
        highlightCells = highlightCells,
        hintIndex = hintIndex,
        interactive = false,
    )
}

@Composable
private fun LessonNumberChoices(tagPrefix: String, onClick: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        for (value in 1..6) {
            NumberBadge(
                value = value,
                size = 42.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                tag = "${tagPrefix}_$value",
                onClick = { onClick(value) },
            )
        }
    }
}

@Composable
private fun KnightGrid(
    gridSize: Int,
    boardSize: Dp,
    horse: Pair<Int, Int>,
    highlighted: Set<Int>,
    onCell: ((Int, Int) -> Unit)? = null,
    tagPrefix: String = "visual_knight_demo",
) {
    MiniGrid(
        size = gridSize,
        cellSize = boardSize / gridSize,
        cellContent = { row, column ->
            val index = row * gridSize + column
            val isHorse = horse == row to column
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            isHorse -> MaterialTheme.colorScheme.primaryContainer
                            index in highlighted -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .testTag("${tagPrefix}_${row}_$column")
                    .clickable(enabled = onCell != null) { onCell?.invoke(row, column) },
                contentAlignment = Alignment.Center,
            ) {
                if (isHorse) Text("马", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun MiniGrid(
    size: Int,
    cellSize: Dp,
    cellContent: @Composable (Int, Int) -> Unit,
) {
    Column(
        Modifier.size(cellSize * size).background(MaterialTheme.colorScheme.outline),
    ) {
        for (row in 0 until size) {
            Row(Modifier.weight(1f)) {
                for (column in 0 until size) {
                    Box(Modifier.size(cellSize).padding(1.dp)) {
                        cellContent(row, column)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberBadge(
    value: Int?,
    color: Color,
    size: Dp = 46.dp,
    tag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .size(size)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = CircleShape,
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                value?.toString() ?: "?",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InteractionMessage(message: String, solved: Boolean) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = if (solved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp).testTag("visual_feedback"),
    )
}

private fun visibleBuildingIndices(heights: List<Int>, fromLeft: Boolean): Set<Int> {
    val order = if (fromLeft) heights.indices.toList() else heights.indices.reversed()
    var tallest = 0
    return order.filter { index ->
        val visible = heights[index] > tallest
        tallest = maxOf(tallest, heights[index])
        visible
    }.toSet()
}

private fun knightTargets(row: Int, column: Int, size: Int): Set<Int> {
    val moves = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
    return moves.mapNotNull { (dr, dc) ->
        val r = row + dr
        val c = column + dc
        if (r in 0 until size && c in 0 until size) r * size + c else null
    }.toSet()
}

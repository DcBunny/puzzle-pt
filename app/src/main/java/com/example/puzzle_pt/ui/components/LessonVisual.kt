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

@Composable
fun LessonVisual(
    step: LessonStep,
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
                    LessonVisualKind.KILLER_MAKE_SUM -> KillerMakeSum(onSolved)
                    LessonVisualKind.KILLER_DUPLICATE -> KillerDuplicate(onSolved)
                    LessonVisualKind.SKY_HEIGHTS -> BuildingRow(List(6) { it + 1 }, reveal = reveal.value)
                    LessonVisualKind.SKY_COUNT -> SkyCountDemo(reveal.value)
                    LessonVisualKind.SKY_QUIZ -> SkyQuiz(onSolved)
                    LessonVisualKind.SKY_SIX_FIRST -> SkySixFirst(reveal.value)
                    LessonVisualKind.DIAGONAL_LINES -> DiagonalDemo(reveal.value, showNumbers = false)
                    LessonVisualKind.DIAGONAL_NUMBERS -> DiagonalDemo(reveal.value, showNumbers = true)
                    LessonVisualKind.DIAGONAL_TRACE -> DiagonalTrace(onSolved)
                    LessonVisualKind.DIAGONAL_DUPLICATE -> DiagonalDuplicate(onSolved)
                    LessonVisualKind.KNIGHT_MOVE -> KnightMoveDemo(reveal.value)
                    LessonVisualKind.KNIGHT_FIND_MOVES -> KnightFindMoves(onSolved)
                    LessonVisualKind.KNIGHT_SAME -> KnightSameDemo()
                    LessonVisualKind.KNIGHT_CHOOSE_SAFE -> KnightChooseSafe(onSolved)
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
private fun KillerMakeSum(onSolved: () -> Unit) {
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf("点数字牌，再点一次可以拿回来") }
    val solved = selected == setOf(1, 2, 3)
    Text("屋角数字：6", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "小屋里现在：${selected.sorted().joinToString(" + ").ifEmpty { "空空的" }}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 12.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (value in 1..4) {
            NumberBadge(
                value = value,
                color = if (value in selected) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                tag = "visual_killer_number_$value",
                onClick = {
                    selected = if (value in selected) selected - value else selected + value
                    val sum = selected.sum()
                    when {
                        selected == setOf(1, 2, 3) -> {
                            message = "正好！1 + 2 + 3 = 6。"
                            onSolved()
                        }
                        sum > 6 -> message = "现在超过 6 啦，点掉一张数字牌试试。"
                        else -> message = "现在合起来是 $sum，还没到 6。"
                    }
                },
            )
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
private fun SkyQuiz(onSolved: () -> Unit) {
    val heights = listOf(2, 3, 1, 4)
    var fromLeft by remember { mutableStateOf<Boolean?>(null) }
    var message by remember { mutableStateOf("先点“从左边看”或“从右边看”") }
    var solved by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { fromLeft = true },
            modifier = Modifier.testTag("visual_sky_left"),
        ) { Text("从左边看") }
        OutlinedButton(
            onClick = { fromLeft = false },
            modifier = Modifier.testTag("visual_sky_right"),
        ) { Text("从右边看") }
    }
    val visible = fromLeft?.let { visibleBuildingIndices(heights, it) }.orEmpty()
    Text(
        if (fromLeft == false) "←  从这里看" else "从这里看  →",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
    BuildingRow(heights, visible = visible)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (value in 1..6) {
            NumberBadge(
                value = value,
                size = 42.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                tag = "visual_sky_answer_$value",
                onClick = {
                    val direction = fromLeft
                    if (direction == null) {
                        message = "先选一个观察方向吧。"
                    } else {
                        val answer = visibleBuildingIndices(heights, direction).size
                        if (value == answer) {
                            solved = true
                            message = "数对了！从这里能看到 $answer 栋楼。"
                            onSolved()
                        } else {
                            message = "再数一次：只看最前面没被挡住的高楼。"
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
private fun DiagonalDuplicate(onSolved: () -> Unit) {
    val values = listOf(1, 2, 4, 4, 5, 6)
    var message by remember { mutableStateOf("只看粉色滑梯上的数字") }
    var solved by remember { mutableStateOf(false) }
    MiniGrid(
        size = 6,
        cellSize = 40.dp,
        cellContent = { row, column ->
            val anti = row + column == 5
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (anti) Color(0xFFFFD4E2) else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (anti && values[row] == 4) {
                            solved = true
                            message = "找到了！粉色滑梯上有两个 4。"
                            onSolved()
                        } else {
                            message = "这个数字没有重复，再沿着粉色滑梯看看。"
                        }
                    }
                    .testTag("visual_diagonal_duplicate_${row}_$column"),
                contentAlignment = Alignment.Center,
            ) {
                if (anti) Text(values[row].toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
    )
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
private fun KnightChooseSafe(onSolved: () -> Unit) {
    var message by remember { mutableStateOf("选 1、2、3 中的一张牌") }
    var solved by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberBadge(2, color = MaterialTheme.colorScheme.primaryContainer)
        Text("  小马跳到  ", style = MaterialTheme.typography.titleMedium)
        NumberBadge(null, color = MaterialTheme.colorScheme.secondaryContainer)
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (value in 1..3) {
            NumberBadge(
                value = value,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tag = "visual_knight_safe_$value",
                onClick = {
                    if (value == 2) {
                        message = "落点也变成 2 了，会和小马那里的数字一样。"
                    } else {
                        solved = true
                        message = "$value 和 2 不一样，这个数字很安全！"
                        onSolved()
                    }
                },
            )
        }
    }
    InteractionMessage(message, solved)
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

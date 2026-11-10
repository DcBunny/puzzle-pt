package com.example.puzzle_pt.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzle_pt.data.LessonInteraction
import com.example.puzzle_pt.data.LessonRepository
import com.example.puzzle_pt.data.ProgressStore
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.CELL_COUNT
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType
import com.example.puzzle_pt.ui.components.AppTopBar
import com.example.puzzle_pt.ui.components.LessonVisual
import com.example.puzzle_pt.ui.components.SudokuBoard
import com.example.puzzle_pt.ui.components.rememberChineseSpeaker

@Composable
fun LessonScreen(
    type: SudokuType,
    store: ProgressStore,
    onBack: () -> Unit,
    onPractice: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    val steps = remember(type) { LessonRepository.stepsFor(type) }
    val examplePuzzle = remember(type) { LessonRepository.examplePuzzle(type) }
    val guidedPuzzle = remember(type) { LessonRepository.guidedPuzzle(type) }
    val targetIndex = remember(guidedPuzzle) { LessonRepository.guidedTargetIndex(type) }
    val alreadyComplete = remember(type) { store.isLessonComplete(type) }
    val speaker = rememberChineseSpeaker()
    var stepIndex by remember(type) { mutableIntStateOf(0) }
    var replayKey by remember(type) { mutableIntStateOf(0) }
    var completedSteps by remember(type) { mutableStateOf(setOf<Int>()) }
    var selectedIndex by remember(type) { mutableStateOf<Int?>(null) }
    var finalEntry by remember(type) { mutableStateOf<Int?>(null) }
    var feedback by remember(type) { mutableStateOf("先点发光的小格，再选一张数字牌") }
    var speechMessage by remember(type) { mutableStateOf("") }
    val step = steps[stepIndex]
    val isFinalStep = stepIndex == steps.lastIndex
    val board = guidedPuzzle.givens.mapIndexed { index, value -> if (index == targetIndex) finalEntry ?: 0 else value }
    val finalComplete = finalEntry == guidedPuzzle.solution[targetIndex]
    val canAdvance = step.interaction == LessonInteraction.NONE || stepIndex in completedSteps

    LaunchedEffect(finalComplete) {
        if (finalComplete && !alreadyComplete) {
            store.completeLesson(type)
            onProgressChanged()
        }
    }

    fun moveTo(newStep: Int) {
        stepIndex = newStep.coerceIn(0, steps.lastIndex)
        replayKey++
        selectedIndex = null
        speechMessage = ""
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("${type.title} · 小课堂", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LessonDots(steps.size, stepIndex)
            Text(
                text = "第 ${stepIndex + 1} 步 / 共 ${steps.size} 步",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = step.title,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = step.explanation,
                fontSize = 20.sp,
                lineHeight = 29.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (step.action.isNotEmpty()) {
                Text(
                    text = step.action,
                    fontSize = 20.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        val text = "${step.title}。${step.explanation}${step.action}"
                        speechMessage = if (speaker.speak(text)) "" else "这台设备暂时不能朗读，我们还可以一起看图。"
                    },
                    modifier = Modifier.testTag("listen_button"),
                ) { Text("听一听", fontSize = 18.sp) }
                if (!isFinalStep) {
                    TextButton(
                        onClick = { replayKey++ },
                        modifier = Modifier.testTag("replay_lesson"),
                    ) { Text("再看一遍", fontSize = 18.sp) }
                }
            }
            if (speechMessage.isNotEmpty()) {
                Text(speechMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isFinalStep) {
                val selectedHighlights = selectedIndex?.let { SudokuRules.relatedCells(it, type) }.orEmpty()
                SudokuBoard(
                    puzzle = guidedPuzzle,
                    board = board,
                    notes = List(CELL_COUNT) { emptySet() },
                    selectedIndex = selectedIndex,
                    conflictCells = SudokuRules.conflictingCells(board, guidedPuzzle),
                    highlightCells = selectedHighlights + targetIndex,
                    onCellSelected = { selectedIndex = it },
                )
                Text(
                    text = if (finalComplete) "你记住了这条规则，真棒！" else feedback,
                    fontSize = 19.sp,
                    color = if (finalComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp).testTag("lesson_feedback"),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (value in 1..6) {
                        Button(
                            onClick = {
                                when {
                                    selectedIndex != targetIndex -> feedback = "先点那个发光的小空格。"
                                    value == guidedPuzzle.solution[targetIndex] -> {
                                        finalEntry = value
                                        feedback = "填对啦！"
                                    }
                                    else -> feedback = childFriendlyHint(type, guidedPuzzle, targetIndex, value)
                                }
                            },
                            enabled = !finalComplete,
                            modifier = Modifier.weight(1f).testTag("lesson_number_$value"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { Text(value.toString(), fontSize = 20.sp) }
                    }
                }
            } else {
                LessonVisual(
                    step = step,
                    puzzle = examplePuzzle,
                    replayKey = replayKey,
                    onSolved = { completedSteps = completedSteps + stepIndex },
                )
                if (stepIndex in completedSteps) {
                    Text(
                        "做到了！可以进入下一步。",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 4.dp).testTag("interaction_complete"),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stepIndex > 0) {
                    OutlinedButton(
                        onClick = { moveTo(stepIndex - 1) },
                        modifier = Modifier.weight(1f),
                    ) { Text("再看前一页", fontSize = 17.sp) }
                }
                if (!isFinalStep) {
                    Button(
                        onClick = { moveTo(stepIndex + 1) },
                        enabled = canAdvance,
                        modifier = Modifier.weight(1f).testTag("lesson_next"),
                    ) { Text(if (canAdvance) "我会啦，继续" else "先试一试上图", fontSize = 17.sp) }
                } else {
                    Button(
                        onClick = onPractice,
                        enabled = finalComplete || alreadyComplete,
                        modifier = Modifier.weight(1f).testTag("lesson_practice"),
                    ) { Text("我会了，去练习", fontSize = 17.sp) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LessonDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == current) 14.dp else 10.dp)
                    .background(
                        if (index <= current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

private fun childFriendlyHint(
    type: SudokuType,
    puzzle: SudokuPuzzle,
    targetIndex: Int,
    value: Int,
): String = when (type) {
    SudokuType.STANDARD -> "行、列和宫里都不能有重复数字，看看还缺哪个数字。"
    SudokuType.KILLER -> "看看虚线笼：数字不能重复，合起来要等于笼角的数。"
    SudokuType.SKYSCRAPER -> "从箭头方向数一数，看到的楼数要和外面的线索一样。"
    SudokuType.DIAGONAL -> "除了行、列和宫，也检查对角线上有没有重复的 $value。"
    SudokuType.ANTI_KNIGHT -> "看看马步落点，走两格再拐一格的两格不能填相同数字。"
}

package com.example.puzzle_pt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.puzzle_pt.data.ProgressStore
import com.example.puzzle_pt.engine.SudokuRules
import com.example.puzzle_pt.model.CELL_COUNT
import com.example.puzzle_pt.model.SudokuHint
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType
import com.example.puzzle_pt.ui.components.AppTopBar
import com.example.puzzle_pt.ui.components.SudokuBoard
import kotlinx.coroutines.delay

private data class GameMove(
    val board: List<Int>,
    val notes: List<Set<Int>>,
)

@Composable
fun PracticeScreen(
    puzzle: SudokuPuzzle,
    store: ProgressStore,
    onBack: () -> Unit,
    onNext: (() -> Unit)?,
    onProgressChanged: () -> Unit,
) {
    val savedGame = remember(puzzle.id) { store.loadGame(puzzle.id) }
    var board by remember(puzzle.id) { mutableStateOf(savedGame?.board ?: puzzle.givens) }
    var notes by remember(puzzle.id) {
        mutableStateOf(savedGame?.notes ?: List(CELL_COUNT) { emptySet() })
    }
    var elapsedSeconds by remember(puzzle.id) { mutableStateOf(savedGame?.elapsedSeconds ?: 0L) }
    var selectedIndex by remember(puzzle.id) { mutableStateOf<Int?>(null) }
    var notesMode by remember(puzzle.id) { mutableStateOf(false) }
    var history by remember(puzzle.id) { mutableStateOf<List<GameMove>>(emptyList()) }
    var feedback by remember(puzzle.id) { mutableStateOf("选择一个空格，再点击数字开始解题") }
    var hint by remember(puzzle.id) { mutableStateOf<SudokuHint?>(null) }
    var running by remember(puzzle.id) { mutableStateOf(true) }
    var solvedThisSession by remember(puzzle.id) { mutableStateOf(false) }
    var showCompletion by remember(puzzle.id) { mutableStateOf(false) }
    val conflicts = remember(board, puzzle) { SudokuRules.conflictingCells(board, puzzle) }

    fun persist() {
        if (!solvedThisSession) store.saveGame(puzzle.id, board, notes, elapsedSeconds)
    }

    fun recordHistory() {
        history = history + GameMove(board, notes)
    }

    fun finishIfComplete(newBoard: List<Int>) {
        if (SudokuRules.isComplete(newBoard, puzzle)) {
            solvedThisSession = true
            running = false
            showCompletion = true
            store.completePuzzle(puzzle.id)
            onProgressChanged()
        }
    }

    fun enterNumber(value: Int) {
        val index = selectedIndex
        if (index == null) {
            feedback = "请先选择一个空格"
            return
        }
        if (puzzle.givens[index] != 0) {
            feedback = "题目给出的数字不能修改"
            return
        }
        recordHistory()
        hint = null
        running = true
        if (notesMode && board[index] == 0) {
            val removing = value in notes[index]
            notes = notes.mapIndexed { cell, candidates ->
                if (cell == index) {
                    if (removing) candidates - value else candidates + value
                } else {
                    candidates
                }
            }
            feedback = "已${if (removing) "移除" else "记下"}候选数 $value"
        } else {
            val newBoard = board.toMutableList().apply { this[index] = value }
            board = newBoard
            val affected = SudokuRules.relatedCells(index, puzzle.type) + index
            notes = notes.mapIndexed { cell, candidates ->
                when {
                    cell == index -> emptySet()
                    cell in affected -> candidates - value
                    else -> candidates
                }
            }
            val newConflicts = SudokuRules.conflictingCells(newBoard, puzzle)
            feedback = if (newConflicts[index]) {
                when (puzzle.type) {
                    SudokuType.STANDARD -> "这里有重复数字，检查所在行、列和宫"
                    SudokuType.KILLER -> "检查笼内是否重复，以及数字之和是否正确"
                    SudokuType.SKYSCRAPER -> "这一行或列的可见楼数与外侧线索不符"
                    SudokuType.DIAGONAL -> "检查所在行、列、宫和两条对角线"
                    SudokuType.ANTI_KNIGHT -> "相同数字不能出现在马步相连的位置"
                }
            } else {
                "填写正确，继续观察空格"
            }
            finishIfComplete(newBoard)
        }
        persist()
    }

    fun erase() {
        val index = selectedIndex ?: return
        if (puzzle.givens[index] != 0 || (board[index] == 0 && notes[index].isEmpty())) return
        recordHistory()
        board = board.toMutableList().apply { this[index] = 0 }
        notes = notes.mapIndexed { cell, candidates -> if (cell == index) emptySet() else candidates }
        hint = null
        feedback = "已清除这一格"
        persist()
    }

    fun undo() {
        val move = history.lastOrNull() ?: run {
            feedback = "还没有可以撤销的操作"
            return
        }
        board = move.board
        notes = move.notes
        history = history.dropLast(1)
        hint = null
        feedback = "已撤销上一步"
        persist()
    }

    fun reset() {
        recordHistory()
        board = puzzle.givens
        notes = List(CELL_COUNT) { emptySet() }
        elapsedSeconds = 0
        solvedThisSession = false
        showCompletion = false
        running = true
        hint = null
        feedback = "题目已重置"
        persist()
    }

    fun requestHint() {
        val emptyCells = board.indices.filter { board[it] == 0 }
        if (emptyCells.isEmpty()) {
            feedback = "还有标红的冲突需要修改"
            return
        }
        val index = emptyCells.firstOrNull {
            SudokuRules.candidates(board.toIntArray(), it, puzzle).size == 1
        } ?: emptyCells.first()
        val row = index / 6 + 1
        val column = index % 6 + 1
        val reason = when (puzzle.type) {
            SudokuType.STANDARD -> "排除这一行、这一列和这一宫已有的数字"
            SudokuType.KILLER -> "结合所在笼还需凑出的和，并排除行、列、宫已有数字"
            SudokuType.SKYSCRAPER -> "结合外侧楼数线索，再排除行、列、宫已有数字"
            SudokuType.DIAGONAL -> "除了行、列、宫，也检查它所在的对角线"
            SudokuType.ANTI_KNIGHT -> "除了行、列、宫，也排除马步关联格的数字"
        }
        hint = SudokuHint(index, puzzle.solution[index], "看看第 $row 行第 $column 列：$reason。这里可以填 ${puzzle.solution[index]}。")
        selectedIndex = index
        feedback = "已为你指出一个可填格"
    }

    LaunchedEffect(running, puzzle.id) {
        while (running) {
            delay(1000)
            elapsedSeconds++
            persist()
        }
    }

    DisposableEffect(puzzle.id) {
        onDispose { persist() }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "${puzzle.type.title} · ${puzzle.difficulty.title}",
            onBack = {
                persist()
                onBack()
            },
            trailing = {
                TextButton(
                    onClick = { running = !running },
                    modifier = Modifier.testTag("pause_button"),
                ) {
                    Text(if (running) "暂停" else "继续")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "用时 ${formatTime(elapsedSeconds)}  ·  剩余 ${board.count { it == 0 }} 格",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            if (running) {
                SudokuBoard(
                    puzzle = puzzle,
                    board = board,
                    notes = notes,
                    selectedIndex = selectedIndex,
                    conflictCells = conflicts,
                    hintIndex = hint?.index,
                    onCellSelected = { selectedIndex = it },
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("已暂停", style = MaterialTheme.typography.headlineMedium)
                        Text("点击右上角“继续”回到题目", modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            hint?.let {
                Text(
                    text = it.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp).testTag("game_feedback"),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (value in 1..6) {
                    Button(
                        onClick = { enterNumber(value) },
                        enabled = running,
                        modifier = Modifier.weight(1f).testTag("number_$value"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(value.toString(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                TextButton(
                    onClick = { notesMode = !notesMode },
                    modifier = Modifier.weight(1f).testTag("notes_mode_button"),
                ) { Text(if (notesMode) "笔记：开" else "笔记：关") }
                TextButton(onClick = ::undo, modifier = Modifier.weight(1f).testTag("undo_button")) { Text("撤销") }
                TextButton(onClick = ::erase, modifier = Modifier.weight(1f)) { Text("擦除") }
                TextButton(onClick = ::reset, modifier = Modifier.weight(1f).testTag("reset_button")) { Text("重置") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = ::requestHint, modifier = Modifier.weight(1f).testTag("hint_button")) {
                    Text("给我提示")
                }
                FilledTonalButton(
                    onClick = { notesMode = !notesMode },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (notesMode) "返回填数" else "记录候选数")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCompletion) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("恭喜完成！") },
            text = { Text("你用 ${formatTime(elapsedSeconds)} 完成了这道${puzzle.type.title}，所有规则都满足。") },
            confirmButton = {
                Button(onClick = onNext ?: onBack) { Text(if (onNext != null) "下一题" else "返回选题") }
            },
            dismissButton = {
                TextButton(onClick = { showCompletion = false }) { Text("查看棋盘") }
            },
        )
    }
}

private fun formatTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

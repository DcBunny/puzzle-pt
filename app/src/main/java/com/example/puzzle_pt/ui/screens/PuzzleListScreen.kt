package com.example.puzzle_pt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.puzzle_pt.data.ProgressStore
import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.model.SudokuPuzzle
import com.example.puzzle_pt.model.SudokuType
import com.example.puzzle_pt.ui.components.AppTopBar

@Composable
fun PuzzleListScreen(
    type: SudokuType,
    store: ProgressStore,
    progressVersion: Int,
    onBack: () -> Unit,
    onSelect: (SudokuPuzzle) -> Unit,
) {
    val puzzles = PuzzleRepository.puzzlesOf(type)
    Column(Modifier.fillMaxSize()) {
        AppTopBar("${type.title}练习", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = type.ruleSummary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(puzzles) { puzzle ->
                val completed = progressVersion.let { store.isPuzzleComplete(puzzle.id) }
                val inProgress = store.hasSavedGame(puzzle.id)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = puzzle.difficulty.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = when {
                                    completed -> "已完成，可再次挑战"
                                    inProgress -> "进行中，继续上次进度"
                                    else -> "${puzzle.givens.count { it == 0 }} 个空格等待完成"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Button(
                            onClick = { onSelect(puzzle) },
                            modifier = Modifier.testTag("start_${puzzle.id}"),
                        ) {
                            Text(if (inProgress) "继续" else if (completed) "重玩" else "开始")
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.puzzle_pt.data.ProgressStore
import com.example.puzzle_pt.data.PuzzleRepository
import com.example.puzzle_pt.model.SudokuType

@Composable
fun HomeScreen(
    store: ProgressStore,
    progressVersion: Int,
    onTeach: (SudokuType) -> Unit,
    onPractice: (SudokuType) -> Unit,
) {
    val completedPuzzles = progressVersion.let {
        PuzzleRepository.puzzles.count { store.isPuzzleComplete(it.id) }
    }
    val completedLessons = SudokuType.entries.count { it != SudokuType.STANDARD && store.isLessonComplete(it) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "六宫数独学院",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "学规则，练思维，一步步成为数独高手",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "已完成 $completedLessons/4 课  ·  $completedPuzzles/15 题",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
        }
        item {
            Text(
                text = "选择一种玩法",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(SudokuType.entries) { type ->
            val puzzles = PuzzleRepository.puzzlesOf(type)
            val completed = puzzles.count { store.isPuzzleComplete(it.id) }
            val hasLesson = type != SudokuType.STANDARD
            val lessonComplete = hasLesson && store.isLessonComplete(type)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (hasLesson) {
                            Text(
                                text = if (lessonComplete) "已学完" else "未学完",
                                color = if (lessonComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Text(
                        text = type.subtitle,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = type.ruleSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "练习进度 $completed/3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (hasLesson) {
                            OutlinedButton(
                                onClick = { onTeach(type) },
                                modifier = Modifier.weight(1f).testTag("lesson_${type.name}"),
                            ) { Text("教学") }
                        }
                        Button(
                            onClick = { onPractice(type) },
                            modifier = Modifier.weight(1f).testTag("practice_${type.name}"),
                        ) { Text("练习") }
                    }
                }
            }
        }
    }
}

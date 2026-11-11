package com.example.puzzle_pt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.puzzle_pt.ui.isTabletLandscape

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
    val lessonCount = SudokuType.entries.count { it.hasLesson }
    val completedLessons = SudokuType.entries.count { it.hasLesson && store.isLessonComplete(it) }
    if (isTabletLandscape()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().testTag("home_screen"),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader(completedLessons, completedPuzzles, lessonCount)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeSectionTitle()
            }
            items(SudokuType.entries) { type ->
                SudokuTypeCard(type, store, onTeach, onPractice)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("home_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HomeHeader(completedLessons, completedPuzzles, lessonCount) }
            item { HomeSectionTitle() }
            items(SudokuType.entries) { type ->
                SudokuTypeCard(type, store, onTeach, onPractice)
            }
        }
    }
}

@Composable
private fun HomeHeader(completedLessons: Int, completedPuzzles: Int, lessonCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "数独学院",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "学规则，练思维，一步步成为数独高手",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "已完成 $completedLessons/$lessonCount 课  ·  $completedPuzzles/${PuzzleRepository.puzzles.size} 题",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun HomeSectionTitle() {
    Text(
        text = "选择一种玩法",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SudokuTypeCard(
    type: SudokuType,
    store: ProgressStore,
    onTeach: (SudokuType) -> Unit,
    onPractice: (SudokuType) -> Unit,
) {
    val puzzles = PuzzleRepository.puzzlesOf(type)
    val completed = puzzles.count { store.isPuzzleComplete(it.id) }
    val hasLesson = type.hasLesson
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
                text = "练习进度 $completed/${puzzles.size}",
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

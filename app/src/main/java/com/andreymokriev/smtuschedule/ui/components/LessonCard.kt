// ui/components/LessonCard.kt
package com.andreymokriev.smtuschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andreymokriev.smtuschedule.data.model.Lesson
import com.andreymokriev.smtuschedule.data.model.WeekType

@Composable
fun LessonCard(lesson: Lesson) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(lesson.time, style = MaterialTheme.typography.titleMedium)
                val weekLabel = when (lesson.week) {
                    WeekType.UP -> "верхняя"
                    WeekType.DOWN -> "нижняя"
                    WeekType.BOTH -> "каждую"
                    WeekType.UNKNOWN -> ""
                }
                if (weekLabel.isNotEmpty())
                    Text(weekLabel, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(lesson.subject, style = MaterialTheme.typography.bodyLarge)
            lesson.type?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text("🚪 ${lesson.room}", style = MaterialTheme.typography.bodyMedium)
            if (lesson.teacher.isNotBlank()) {
                Text("👤 ${lesson.teacher}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
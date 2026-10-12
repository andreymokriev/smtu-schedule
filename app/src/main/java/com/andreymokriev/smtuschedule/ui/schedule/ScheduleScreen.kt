// ui/schedule/ScheduleScreen.kt
package com.andreymokriev.smtuschedule.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andreymokriev.smtuschedule.data.model.DayStatus
import com.andreymokriev.smtuschedule.ui.components.LessonCard
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    vm: ScheduleViewModel,
    title: String,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    WeekFilterMenu(
                        selected = state.weekFilter,
                        onSelect = { vm.setWeekFilter(it) }
                    )
                    IconButton(onClick = { vm.load(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading && state.schedule == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null ->
                    Text("Ошибка: ${state.error}", Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error)
                state.schedule != null -> {
                    val schedule = state.schedule!!
                    if (schedule.days.isEmpty()) {
                        // Страница скачалась, но без карточек дней: некорректный id
                        // (например, teacher/0 из-за ошибки навигации) или расписание
                        // ещё не опубликовано. Показываем сообщение, а не пустоту.
                        Text(
                            "Расписание не найдено — попробуйте обновить (↻)",
                            Modifier.align(Alignment.Center).padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                    val days = vm.filteredDays(schedule, state.weekFilter)
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                        schedule.dateInfo?.let {
                            item { Text(it, Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium) }
                        }
                        val todayName = todayRussianName()
                        days.forEach { day ->
                            item {
                                Text(
                                    text = day.dayName + if (day.dayName == todayName) " • сегодня" else "",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            when (day.status) {
                                DayStatus.PENDING -> item {
                                    Text("Составляется",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                DayStatus.EMPTY -> item {
                                    Text("Нет занятий",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                DayStatus.OK -> items(day.lessons) { LessonCard(it) }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                    } // else days.isNotEmpty
                }
            }
        }
    }
}

@Composable
private fun WeekFilterMenu(
    selected: WeekFilter,
    onSelect: (WeekFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected.label, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            WeekFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            filter.label,
                            color = if (filter == selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun todayRussianName(): String {
    val names = listOf(
        "Понедельник","Вторник","Среда","Четверг","Пятница","Суббота","Воскресенье"
    )
    val cal = Calendar.getInstance()
    // Calendar.MONDAY = 2, поэтому сдвиг
    val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return names[idx]
}
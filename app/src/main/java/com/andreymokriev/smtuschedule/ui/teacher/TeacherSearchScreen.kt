// ui/teacher/TeacherSearchScreen.kt
package com.andreymokriev.smtuschedule.ui.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSearchScreen(
    vm: TeacherSearchViewModel,
    onBack: () -> Unit,
    onTeacherSelected: (name: String, teacherId: Int) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск преподавателя") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQuery,
                label = { Text("Фамилия преподавателя") },
                placeholder = { Text("Например, Кайнова") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { vm.searchNow() }),
                trailingIcon = {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { vm.searchNow() }) {
                            Icon(Icons.Default.Search, contentDescription = "Найти")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            when {
                state.error != null ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Ошибка: ${state.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { vm.searchNow() }) { Text("Повторить") }
                        }
                    }
                state.searchedQuery != null && state.results.isEmpty() && !state.loading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(
                            "Преподаватели не найдены",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                else ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.results, key = { it.teacherId }) { t ->
                            ListItem(
                                headlineContent = { Text(t.name) },
                                modifier = Modifier.clickable {
                                    onTeacherSelected(t.name, t.teacherId)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}

// ui/group/GroupPickerScreen.kt
package com.andreymokriev.smtuschedule.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerScreen(
    vm: GroupPickerViewModel,
    onGroupSelected: (String, Int) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбор группы") },
                actions = {
                    IconButton(onClick = { vm.load(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQuery,
                label = { Text("Поиск группы") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            when {
                state.loading && state.groups.isEmpty() ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.error != null ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Ошибка: ${state.error}", color = MaterialTheme.colorScheme.error)
                    }
                else ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.filtered, key = { it.pageId }) { g ->
                            ListItem(
                                headlineContent = { Text(g.name) },
                                modifier = Modifier.clickable {
                                    onGroupSelected(g.name, g.pageId)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}
// MainActivity.kt
package com.andreymokriev.smtuschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.andreymokriev.smtuschedule.data.local.Prefs
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository
import com.andreymokriev.smtuschedule.ui.group.GroupPickerScreen
import com.andreymokriev.smtuschedule.ui.group.GroupPickerViewModel
import com.andreymokriev.smtuschedule.ui.schedule.ScheduleScreen
import com.andreymokriev.smtuschedule.ui.schedule.ScheduleViewModel
import com.andreymokriev.smtuschedule.ui.theme.SmtuScheduleTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = (application as ScheduleApp).repository
        val prefs = Prefs(this)

        // Синхронно читаем сохранённую группу (на старте допустимо)
        val saved = runBlocking { prefs.selectedGroup.first() }

        setContent {
            SmtuScheduleTheme {
                val nav = rememberNavController()

                val startRoute = if (saved != null)
                    "schedule/${saved.first}/${saved.second}"
                else "picker"

                NavHost(navController = nav, startDestination = startRoute) {

                    composable("picker") {
                        val vm: GroupPickerViewModel = viewModel(
                            factory = simpleFactory { GroupPickerViewModel(repo) }
                        )
                        GroupPickerScreen(vm) { name, pageId ->
                            // сохраняем выбор и переходим
                            kotlinx.coroutines.GlobalScope.launch {
                                prefs.setGroup(name, pageId)
                            }
                            nav.navigate("schedule/$name/$pageId") {
                                popUpTo("picker") { inclusive = true }
                            }
                        }
                    }

                    composable("schedule/{name}/{pageId}") { entry ->
                        val name = entry.arguments?.getString("name").orEmpty()
                        val pageId = entry.arguments?.getString("pageId")?.toIntOrNull() ?: 0
                        val vm: ScheduleViewModel = viewModel(
                            factory = simpleFactory {
                                ScheduleViewModel(repo, pageId, name)
                            }
                        )
                        ScheduleScreen(vm, name) {
                            nav.navigate("picker") {
                                popUpTo("schedule/{name}/{pageId}") { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Минимальная фабрика VM, чтобы не тащить Hilt ради 2 экранов
private inline fun <VM : ViewModel> simpleFactory(crossinline create: () -> VM)
        : ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
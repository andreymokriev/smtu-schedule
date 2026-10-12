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
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.andreymokriev.smtuschedule.data.local.Prefs
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository
import com.andreymokriev.smtuschedule.ui.group.GroupPickerScreen
import com.andreymokriev.smtuschedule.ui.group.GroupPickerViewModel
import com.andreymokriev.smtuschedule.ui.schedule.ScheduleScreen
import com.andreymokriev.smtuschedule.ui.schedule.ScheduleViewModel
import com.andreymokriev.smtuschedule.ui.teacher.TeacherSearchScreen
import com.andreymokriev.smtuschedule.ui.teacher.TeacherSearchViewModel
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

                // Стартуем всегда с picker'а, чтобы он был в стеке: если группа
                // сохранена, сразу открываем её — тогда «назад» с расписания
                // корректно вернёт к выбору группы и при повторном запуске.
                LaunchedEffect(Unit) {
                    saved?.let { (name, pageId) ->
                        nav.navigate("schedule_group?id=$pageId&name=${Uri.encode(name)}")
                    }
                }

                NavHost(navController = nav, startDestination = "picker") {

                    composable("picker") {
                        val vm: GroupPickerViewModel = viewModel(
                            factory = simpleFactory { GroupPickerViewModel(repo) }
                        )
                        GroupPickerScreen(
                            vm = vm,
                            onGroupSelected = { name, pageId ->
                                // сохраняем выбор и переходим.
                                // popUpTo picker НЕ inclusive: экран выбора остаётся
                                // в стеке, чтобы кнопка «назад» с расписания работала.
                                kotlinx.coroutines.GlobalScope.launch {
                                    prefs.setGroup(name, pageId)
                                }
                                nav.navigate("schedule_group?id=$pageId&name=${Uri.encode(name)}")
                            },
                            onTeacherSearch = {
                                nav.navigate("teacher_search")
                            }
                        )
                    }

                    composable("teacher_search") {
                        val vm: TeacherSearchViewModel = viewModel(
                            factory = simpleFactory { TeacherSearchViewModel(repo) }
                        )
                        TeacherSearchScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onTeacherSelected = { name, teacherId ->
                                nav.navigate("schedule_teacher?id=$teacherId&name=${Uri.encode(name)}")
                            }
                        )
                    }

                    composable(
                        "schedule_group?id={id}&name={name}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.IntType },
                            navArgument("name") { type = NavType.StringType; nullable = true }
                        )
                    ) { entry ->
                        val name = entry.arguments?.getString("name").orEmpty()
                        val pageId = entry.arguments?.getInt("id") ?: 0
                        ScheduleRoute(nav, repo, pageId, name, isTeacher = false)
                    }

                    composable(
                        "schedule_teacher?id={id}&name={name}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.IntType },
                            navArgument("name") { type = NavType.StringType; nullable = true }
                        )
                    ) { entry ->
                        val name = entry.arguments?.getString("name").orEmpty()
                        val teacherId = entry.arguments?.getInt("id") ?: 0
                        ScheduleRoute(nav, repo, teacherId, name, isTeacher = true)
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

@Composable
private fun ScheduleRoute(
    nav: androidx.navigation.NavHostController,
    repo: ScheduleRepository,
    id: Int,
    title: String,
    isTeacher: Boolean,
) {
    // Ключ viewModel уникален для пары (isTeacher, id), чтобы при смене
    // расписания не показывалось прошлое состояние.
    val vm: ScheduleViewModel = viewModel(
        key = "schedule_" + (if (isTeacher) "t" else "g") + "_" + id,
        factory = simpleFactory {
            ScheduleViewModel(repo, id, title, isTeacher)
        }
    )
    ScheduleScreen(vm, title) {
        nav.popBackStack()
    }
}
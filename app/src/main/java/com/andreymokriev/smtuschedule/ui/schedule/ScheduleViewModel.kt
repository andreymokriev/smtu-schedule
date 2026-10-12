// ui/schedule/ScheduleViewModel.kt
package com.andreymokriev.smtuschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.andreymokriev.smtuschedule.data.model.DaySchedule
import com.andreymokriev.smtuschedule.data.model.DayStatus
import com.andreymokriev.smtuschedule.data.model.GroupSchedule
import com.andreymokriev.smtuschedule.data.model.WeekType
import com.andreymokriev.smtuschedule.data.remote.SmtuParser
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository

/** Какой тип недель показывать на экране расписания. */
enum class WeekFilter(val label: String) {
    CURRENT("Текущая неделя"),
    UP("Верхняя неделя"),
    DOWN("Нижняя неделя"),
    ALL("Все недели");

    companion object {
        fun of(raw: String?): WeekFilter =
            entries.firstOrNull { it.name == raw } ?: CURRENT
    }
}

data class ScheduleUiState(
    val loading: Boolean = false,
    val schedule: GroupSchedule? = null,
    val error: String? = null,
    val weekFilter: WeekFilter = WeekFilter.CURRENT,
)

class ScheduleViewModel(
    private val repo: ScheduleRepository,
    private val id: Int,
    private val title: String,
    private val isTeacher: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init { load() }

    fun load(force: Boolean = false) {
        val keepFilter = _state.value.weekFilter
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val data = if (isTeacher) repo.getTeacherSchedule(id, forceRefresh = force)
                else repo.getSchedule(id, forceRefresh = force)
                _state.value = ScheduleUiState(
                    loading = false,
                    schedule = data,
                    weekFilter = keepFilter,
                )
            } catch (t: Throwable) {
                _state.value = ScheduleUiState(
                    loading = false,
                    error = t.message ?: "Ошибка",
                    weekFilter = keepFilter,
                )
            }
        }
    }

    fun setWeekFilter(filter: WeekFilter) {
        _state.value = _state.value.copy(weekFilter = filter)
    }

    /**
     * Возвращает расписание, отфильтрованное по выбранному типу недели.
     *
     * CURRENT — пары текущей недели: пары, идущие каждую неделю (BOTH), плюс
     * пары, чей тип совпадает с типом текущей недели (определяется по строке
     * dateInfo вида «Сегодня: … , верхняя/нижняя неделя»).
     * UP / DOWN — только пары соответствующего типа (BOTH тоже показывается:
     * они идут каждую неделю, в том числе в выбранную).
     * ALL — расписание без фильтрации.
     */
    fun filteredDays(schedule: GroupSchedule, filter: WeekFilter): List<DaySchedule> {
        if (filter == WeekFilter.ALL) return schedule.days

        val target: WeekType = when (filter) {
            WeekFilter.UP -> WeekType.UP
            WeekFilter.DOWN -> WeekType.DOWN
            WeekFilter.CURRENT -> SmtuParser.parseCurrentWeek(schedule.dateInfo)
            WeekFilter.ALL -> return schedule.days
        }
        if (target == WeekType.UNKNOWN) {
            // Не удалось определить тип недели — не скрываем пары, а показываем все.
            return schedule.days
        }
        return schedule.days.map { day -> filterDay(day, target) }
    }

    private fun filterDay(day: DaySchedule, target: WeekType): DaySchedule {
        val lessons = day.lessons.filter { it.week == WeekType.BOTH || it.week == target }
        val status = when {
            day.status == DayStatus.PENDING -> DayStatus.PENDING
            lessons.isEmpty() -> DayStatus.EMPTY
            else -> DayStatus.OK
        }
        return DaySchedule(dayName = day.dayName, status = status, lessons = lessons)
    }
}

// ui/schedule/ScheduleViewModel.kt
package com.andreymokriev.smtuschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.andreymokriev.smtuschedule.data.model.GroupSchedule
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository

data class ScheduleUiState(
    val loading: Boolean = false,
    val schedule: GroupSchedule? = null,
    val error: String? = null,
)

class ScheduleViewModel(
    private val repo: ScheduleRepository,
    private val pageId: Int,
    private val groupName: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init { load() }

    fun load(force: Boolean = false) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val data = repo.getSchedule(pageId, forceRefresh = force)
                _state.value = ScheduleUiState(loading = false, schedule = data)
            } catch (t: Throwable) {
                _state.value = ScheduleUiState(loading = false, error = t.message ?: "Ошибка")
            }
        }
    }
}
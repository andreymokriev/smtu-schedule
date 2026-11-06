// ui/teacher/TeacherSearchViewModel.kt
package com.andreymokriev.smtuschedule.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.andreymokriev.smtuschedule.data.remote.TeacherRef
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository

data class TeacherSearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<TeacherRef> = emptyList(),
    val searchedQuery: String? = null, // запрос, для которого показаны results
    val error: String? = null,
)

class TeacherSearchViewModel(
    private val repo: ScheduleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherSearchUiState())
    val state: StateFlow<TeacherSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        // Автопоиск с дебаунсом: запрос уходит через 600 мс после ввода.
        // Фамилия обычно набирается целиком, но сайт ищет и по подстроке.
        searchJob?.cancel()
        val query = q.trim()
        if (query.length < 2) {
            _state.value = _state.value.copy(loading = false, results = emptyList(), searchedQuery = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(600)
            search(query)
        }
    }

    /** Принудительный поиск (кнопка), без дебаунса. */
    fun searchNow() {
        searchJob?.cancel()
        val query = _state.value.query.trim()
        if (query.isNotEmpty()) {
            searchJob = viewModelScope.launch { search(query) }
        }
    }

    private suspend fun search(query: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val results = repo.searchTeachers(query)
            _state.value = _state.value.copy(
                loading = false,
                results = results,
                searchedQuery = query,
            )
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                loading = false,
                error = t.message ?: "Ошибка",
                searchedQuery = query,
            )
        }
    }
}

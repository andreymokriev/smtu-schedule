// ui/group/GroupPickerViewModel.kt
package com.andreymokriev.smtuschedule.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.andreymokriev.smtuschedule.data.model.GroupRef
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository

data class GroupPickerUiState(
    val loading: Boolean = false,
    val groups: List<GroupRef> = emptyList(),
    val query: String = "",
    val error: String? = null,
) {
    val filtered: List<GroupRef>
        get() = if (query.isBlank()) groups
        else groups.filter { it.name.contains(query, ignoreCase = true) }
}

class GroupPickerViewModel(private val repo: ScheduleRepository) : ViewModel() {

    private val _state = MutableStateFlow(GroupPickerUiState())
    val state: StateFlow<GroupPickerUiState> = _state.asStateFlow()

    init { load() }

    fun onQuery(q: String) { _state.value = _state.value.copy(query = q) }

    fun load(force: Boolean = false) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val groups = if (force) repo.refreshGroups() else repo.loadGroups()
                _state.value = _state.value.copy(loading = false, groups = groups)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message)
            }
        }
    }
}
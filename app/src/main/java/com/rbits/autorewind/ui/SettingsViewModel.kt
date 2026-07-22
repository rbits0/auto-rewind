package com.rbits.autorewind.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbits.autorewind.data.Settings
import com.rbits.autorewind.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: Settings,
    val isLoaded: Boolean,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settingsState: StateFlow<SettingsState> = repository.settingsFlow
        .map { SettingsState(it, isLoaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsState(Settings(), isLoaded = false),
        )

    fun setRewindTime(rewindTimeMs: Long) {
        viewModelScope.launch {
            repository.setRewindTime(rewindTimeMs)
        }
    }

}

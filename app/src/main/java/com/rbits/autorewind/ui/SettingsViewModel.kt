package com.rbits.autorewind.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbits.autorewind.data.Settings
import com.rbits.autorewind.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settingsState = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = Settings(),
    )

    fun setRewindTime(rewindTime: Duration) {
        viewModelScope.launch {
            repository.setRewindTime(rewindTime)
        }
    }

}

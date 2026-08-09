package com.rbits.autorewind.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ServiceStateViewModel @Inject constructor() : ViewModel() {
    private val _isForegroundServiceRunning = MutableStateFlow(false)
    val isForegroundServiceRunning = _isForegroundServiceRunning.asStateFlow()

    fun setIsForegroundServiceRunning(value: Boolean) {
        _isForegroundServiceRunning.update { value }
    }
}
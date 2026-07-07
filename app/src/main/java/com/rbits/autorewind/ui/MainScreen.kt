package com.rbits.autorewind.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbits.autorewind.R
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun MainScreen(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    var rewindTimeText by remember { mutableStateOf(
        settingsState.rewindTime
            .toDouble(DurationUnit.SECONDS)
            .toString()
    ) }
    var rewindTimeError by remember { mutableStateOf(false) }
    // TODO: Implement
    var serviceEnabled by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
    ) {
        OutlinedTextField(
            value = rewindTimeText,
            onValueChange = { value ->
                rewindTimeText = value

                try {
                    settingsViewModel.setRewindTime(value.toDouble().seconds)
                    rewindTimeError = false
                } catch (_: Exception) {
                    rewindTimeError = true
                }
            },
            label = { Text(stringResource(R.string.rewind_time_seconds)) },
            isError = rewindTimeError,
        )

        Button(
            onClick = { serviceEnabled = !serviceEnabled },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (serviceEnabled) {
                        stringResource(R.string.stop_service)
                    } else {
                        stringResource(R.string.start_service)
                    },
            )
        }
    }
}

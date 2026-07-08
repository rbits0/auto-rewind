package com.rbits.autorewind.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbits.autorewind.AutoRewindService
import com.rbits.autorewind.R
import com.rbits.autorewind.TAG
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun MainScreen(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current

    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    var rewindTimeText by remember { mutableStateOf(
        settingsState.rewindTime
            .toDouble(DurationUnit.SECONDS)
            .toString()
    ) }
    var rewindTimeError by remember { mutableStateOf(false) }
    // TODO: Implement
    val serviceEnabled = false
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // TODO: Re-launch foreground service when rewindTime is modified
            launchForegroundService(localContext, settingsState.rewindTime)
        } else {
            Log.e(TAG, "Permission denied")
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically),
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

        Spacer(
            modifier = Modifier
                .height(30.dp)
        )

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    launchForegroundService(localContext, settingsState.rewindTime)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.start_service))
        }

        Button(
            onClick = {
                stopForegroundService(localContext)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.stop_service))
        }
    }
}

fun launchForegroundService(context: Context, rewindTime: Duration) {
    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        val intent = Intent(
            context,
            AutoRewindService::class.java
        )
        intent.putExtra("com.rbits.autorewind.rewindTimeMs", rewindTime.inWholeMilliseconds)
        ContextCompat.startForegroundService(context, intent)
    } else {
        Log.e(TAG, "Notifications are not enabled")
    }
}

fun stopForegroundService(context: Context) {
    val intent = Intent(
        context,
        AutoRewindService::class.java
    )
    context.stopService(intent)
}
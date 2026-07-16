package com.rbits.autorewind

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.dataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbits.autorewind.data.SettingsRepository
import com.rbits.autorewind.data.SettingsSerializer
import com.rbits.autorewind.ui.MainScreen
import com.rbits.autorewind.ui.SettingsViewModel
import com.rbits.autorewind.ui.theme.AutoRewindTheme

const val TAG: String = "com.rbits.autorewind"
const val SETTINGS_STORE_FILE_NAME = "settings.json"
const val AUTO_REWIND_SERVICE_CHANNEL_ID = "auto_rewind_service"

const val NOTIFICATION_ID_AUTO_REWIND_SERVICE = 100
const val REQUEST_CODE_MAIN_ACTIVITY = 101
const val REQUEST_CODE_ACTION_STOP_AUTO_REWIND = 102
const val ACTION_STOP_AUTO_REWIND = "com.rbits.autorewind.ACTION_STOP_AUTO_REWIND"

private val Context.settingsStore by dataStore(
    fileName = SETTINGS_STORE_FILE_NAME,
    serializer = SettingsSerializer,
)

class MainActivity : ComponentActivity() {
//    val isForegroundServiceRunning = MutableSharedFlow<Boolean>()

//    val autoRewindServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
//            val binder = binder as AutoRewindService.AutoRewindServiceBinder
//             binder.service.isForegroundServiceRunning
//        }
//
//        override fun onServiceDisconnected(p0: ComponentName?) {
//            TODO("Not yet implemented")
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        val intent = Intent(this, AutoRewindService::class.java)
//        bindService(intent)

        createNotificationChannel()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(SettingsRepository(settingsStore))
            }

            AutoRewindTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat.Builder(
            AUTO_REWIND_SERVICE_CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW,
        )
            .setName(getString(R.string.service_notification_channel_name))
            .setDescription(getString(R.string.service_notification_channel_description))
            .build()
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}
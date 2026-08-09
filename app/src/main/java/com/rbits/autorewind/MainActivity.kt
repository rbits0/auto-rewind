package com.rbits.autorewind

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.rbits.autorewind.AutoRewindService.Companion.MSG_REGISTER_CLIENT
import com.rbits.autorewind.AutoRewindService.Companion.MSG_UNREGISTER_CLIENT
import com.rbits.autorewind.ui.MainScreen
import com.rbits.autorewind.ui.ServiceStateViewModel
import com.rbits.autorewind.ui.SettingsViewModel
import com.rbits.autorewind.ui.theme.AutoRewindTheme
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

const val TAG: String = "com.rbits.autorewind"
const val AUTO_REWIND_SERVICE_CHANNEL_ID = "auto_rewind_service"

const val NOTIFICATION_ID_AUTO_REWIND_SERVICE = 100
const val REQUEST_CODE_MAIN_ACTIVITY = 101
const val REQUEST_CODE_ACTION_STOP_AUTO_REWIND = 102
const val ACTION_START_AUTO_REWIND = "com.rbits.autorewind.ACTION_START_AUTO_REWIND"
const val ACTION_STOP_AUTO_REWIND = "com.rbits.autorewind.ACTION_STOP_AUTO_REWIND"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var autoRewindServiceMessenger: Messenger? = null
    var isForegroundServiceRunning = false
        set(value) {
            field = value
            updateAutoRewindServiceState()
        }

    class IncomingHandler(activity: MainActivity) : Handler(Looper.getMainLooper()) {
        val activity = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                AutoRewindService.MSG_FOREGROUND_SERVICE_RUNNING -> {
                    activity.get()?.isForegroundServiceRunning = true
                }

                AutoRewindService.MSG_FOREGROUND_SERVICE_STOPPED -> {
                    activity.get()?.isForegroundServiceRunning = false
                }

                else -> super.handleMessage(msg)
            }
        }
    }

    private val messenger = Messenger(IncomingHandler(this))

    private val autoRewindServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            autoRewindServiceMessenger = Messenger(binder)

            try {
                val message = Message.obtain(null, MSG_REGISTER_CLIENT)
                message.replyTo = messenger
                autoRewindServiceMessenger?.send(message)
            } catch (_: DeadObjectException) {
                // AutoRewindService crashed, therefore onServiceDisconnected will be run
            }
        }

        override fun onServiceDisconnected(p0: ComponentName) {
            // AutoRewindService crashed
            autoRewindServiceMessenger = null
            updateAutoRewindServiceState()
        }
    }
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val serviceStateViewModel: ServiceStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()

        setContent {
            AutoRewindTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        settingsViewModel = settingsViewModel,
                        serviceStateViewModel = serviceStateViewModel,
                        modifier = Modifier
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindAutoRewindService()
    }

    override fun onStop() {
        super.onStop()
        unbindAutoRewindService()
    }

    private fun bindAutoRewindService() {
        val intent = Intent(
            this,
            AutoRewindService::class.java,
        )
        bindService(intent, autoRewindServiceConnection, BIND_AUTO_CREATE)
    }

    private fun unbindAutoRewindService() {
        if (autoRewindServiceMessenger != null) {
            try {
                val message = Message.obtain(null, MSG_UNREGISTER_CLIENT)
                message.replyTo = messenger
                autoRewindServiceMessenger?.send(message)
            } catch (_: DeadObjectException) {
                // AutoRewindService crashed
            }
        }

        unbindService(autoRewindServiceConnection)
        autoRewindServiceMessenger = null
        updateAutoRewindServiceState()
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

    private fun updateAutoRewindServiceState() {
        serviceStateViewModel.setIsForegroundServiceRunning(
            isForegroundServiceRunning && autoRewindServiceMessenger != null
        )
    }
}
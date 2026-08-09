package com.rbits.autorewind

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.TaskStackBuilder
import androidx.datastore.core.DataStore
import com.rbits.autorewind.data.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject


@AndroidEntryPoint
class AutoRewindService : Service() {
    private val messengerClients: MutableList<Messenger> = mutableListOf()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var mediaSessionManager: MediaSessionManager
    private var mediaPauseCallback: MediaController.Callback? = null
    private var activeSession: MediaController? = null

    @Inject
    lateinit var settingsDataStore: DataStore<Settings>
    var rewindTimeMs = 5_000L

    // TODO: Update UI with foreground service state:
    var isForegroundServiceRunning = false
        set(value) {
            field = value
            sendForegroundServiceState()
        }

    private fun sendForegroundServiceState() {
        val what = if (isForegroundServiceRunning) {
            MSG_FOREGROUND_SERVICE_RUNNING
        } else {
            MSG_FOREGROUND_SERVICE_STOPPED
        }

        for (messengerClient in messengerClients) {
            try {
                val message = Message.obtain(null, what)
                messengerClient.send(message)
            } catch (_: DeadObjectException) {
                // messengerClient is dead
                messengerClients.remove(messengerClient)
            }
        }
    }

    companion object {
        const val MSG_REGISTER_CLIENT = 1
        const val MSG_UNREGISTER_CLIENT = 2
        const val MSG_FOREGROUND_SERVICE_RUNNING = 3
        const val MSG_FOREGROUND_SERVICE_STOPPED = 4
    }


    // TODO: Test whether Looper.getMainLooper() works as intended when starting AutoRewindService
    // from outside the app
    class IncomingHandler(service: AutoRewindService) : Handler(Looper.getMainLooper()) {
        val service = WeakReference(service)

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REGISTER_CLIENT -> {
                    service.get()?.messengerClients?.add(msg.replyTo)
                    service.get()?.sendForegroundServiceState()
                }
                MSG_UNREGISTER_CLIENT -> service.get()?.messengerClients?.remove(msg.replyTo)
                else -> super.handleMessage(msg)
            }
        }
    }

    private val messenger = Messenger(IncomingHandler(this))
    override fun onCreate() {
        super.onCreate()

        mediaSessionManager = getSystemService(MediaSessionManager::class.java)

        serviceScope.launch {
            settingsDataStore.data.collect { settings ->
                rewindTimeMs = settings.rewindTimeMs
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_START_AUTO_REWIND -> startForeground()
            ACTION_STOP_AUTO_REWIND -> stopForeground()
            else -> Log.e(TAG, "Invalid action for AutoRewindService")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        stopForeground()
        serviceScope.cancel()

        Log.i(TAG, "AutoRewindService destroyed")
        super.onDestroy()
    }

    fun startForeground() {
        Log.i(TAG, "startForeground()")

        createForegroundServiceNotification()
        registerAutoRewindCallback()
        isForegroundServiceRunning = true
        // TODO: Unregister and register new callback when event from addOnActiveSessionsChangedListener
    }

    fun stopForeground() {
        unregisterAutoRewindCallback()
        ServiceCompat.stopForeground(
            this,
            ServiceCompat.STOP_FOREGROUND_REMOVE
        )
        isForegroundServiceRunning = false
    }

    private fun createForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (
                checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE) == PackageManager.PERMISSION_DENIED
            ) {
                Log.e(TAG, "Don't have foreground service permissions")
                stopSelf()
                return
            }
        }

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentTaskStack = TaskStackBuilder.create(this)
        contentTaskStack.addNextIntent(contentIntent)
        val contentPendingIntent = contentTaskStack.getPendingIntent(
            REQUEST_CODE_MAIN_ACTIVITY,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(ACTION_STOP_AUTO_REWIND)
        stopIntent.setPackage(applicationInfo.packageName)
        val stopPendingIntent = PendingIntentCompat.getBroadcast(
            this,
            REQUEST_CODE_ACTION_STOP_AUTO_REWIND,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            true,
        )
        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.stop,
            getString(R.string.stop_service),
            stopPendingIntent,
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_STOP)
            .build()

        val notification = NotificationCompat.Builder(
            this, AUTO_REWIND_SERVICE_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.service_notification_name))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPendingIntent)
            .addAction(stopAction)
            .setDeleteIntent(stopPendingIntent)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID_AUTO_REWIND_SERVICE,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            } else {
                0
            }
        )
    }

    /**
     * Registers (or re-registers) a callback to automatically rewind the media
     */
    private fun registerAutoRewindCallback() {
        Log.i(TAG, "Registering auto rewind callback")

        unregisterAutoRewindCallback()

        val activeSession = getActiveSession() ?: return

        // Register callback
        val mediaPauseCallback = (object : MediaController.Callback() {
            var previousState: PlaybackState? = null

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                Log.i(TAG, "onPlaybackStateChanged: $state")

                // Check that the state has changed to STATE_PAUSED
                val previousState = this.previousState
                this.previousState = state
                if (
                    previousState?.state != PlaybackState.STATE_PAUSED
                    && state?.state == PlaybackState.STATE_PAUSED
                ) {
                    rewind(activeSession)
                }
            }
        })
        activeSession.registerCallback(mediaPauseCallback)

        this.mediaPauseCallback = mediaPauseCallback
        this.activeSession = activeSession
    }

    /**
     * Unregister auto rewind callback if it is already registered
     */
    private fun unregisterAutoRewindCallback() {
        val mediaPauseCallback = this.mediaPauseCallback
        val activeSession = this.activeSession
        if (mediaPauseCallback != null && activeSession != null) {
            activeSession.unregisterCallback(mediaPauseCallback)
            this.mediaPauseCallback = null
            this.activeSession = null
        }
    }

    private fun getActiveSession(): MediaController? {
        val activeSessions = mediaSessionManager.getActiveSessions(
            ComponentName(
                this,
                AutoRewindNotificationListenerService::class.java
            )
        )

        return activeSessions.firstOrNull()
    }

    private fun rewind(mediaController: MediaController) {
        Log.i(TAG, "Rewinding $rewindTimeMs ms")
        val sessionName = mediaController.packageName
        // TODO: Check if sessionName matches a target session

        val position = mediaController.playbackState?.position ?: return
        val newPosition = position - rewindTimeMs

        mediaController.transportControls.seekTo(newPosition)
    }
}
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
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.TaskStackBuilder

class AutoRewindService : Service() {
    inner class AutoRewindServiceBinder() : Binder() {
        fun getService() = this@AutoRewindService
    }
    val binder = AutoRewindServiceBinder()

    private lateinit var mediaSessionManager: MediaSessionManager
    private var mediaPauseCallback: MediaController.Callback? = null
    private var activeSession: MediaController? = null

    // TODO: Update UI with foreground service state:
//    private val _isForegroundServiceRunning = MutableStateFlow(false)
//    var isForegroundServiceRunning = _isForegroundServiceRunning.asStateFlow()

    override fun onCreate() {
        mediaSessionManager = getSystemService(MediaSessionManager::class.java)

        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
//        _isForegroundServiceRunning.update { true }

        return super.onStartCommand(intent, flags, startId)
    }

    fun startForeground() {
        Log.i(TAG, "startForeground()")

        createForegroundServiceNotification()
        registerAutoRewindCallback()
        // TODO: Unregister and register new callback when event from addOnActiveSessionsChangedListener
    }

    override fun onDestroy() {
        unregisterAutoRewindCallback()
        ServiceCompat.stopForeground(
            this,
            ServiceCompat.STOP_FOREGROUND_REMOVE
        )
//        _isForegroundServiceRunning.update { false }

        Log.i(TAG, "AutoRewindService destroyed")
        super.onDestroy()
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
            .setPriority(NotificationCompat.PRIORITY_MAX)
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
        Log.i(TAG, "Rewinding")
        val sessionName = mediaController.packageName
        // TODO: Check if sessionName matches a target session

        val position = mediaController.playbackState?.position ?: return
        // TODO: Use user-configured rewind time
        val newPosition = position - 5_000L

        mediaController.transportControls.seekTo(newPosition)
    }
}
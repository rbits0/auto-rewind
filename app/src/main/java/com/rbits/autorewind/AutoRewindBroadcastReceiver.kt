package com.rbits.autorewind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AutoRewindBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_STOP_AUTO_REWIND) {
            if (context == null) {
                Log.e(TAG, "ACTION_STOP_AUTO_REWIND broadcast is missing its context")
                return
            }

            // Stop foreground service
            val intent = Intent(
                context,
                AutoRewindService::class.java
            )
            intent.action = ACTION_STOP_AUTO_REWIND
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
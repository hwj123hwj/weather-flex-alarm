package com.example.weatherflexalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class AlarmRingingService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val autoStopRunnable = Runnable { stopRingingAndSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRingingAndSelf()
                return START_NOT_STICKY
            }

            else -> {
                val reason = intent?.getStringExtra(EXTRA_REASON) ?: "到点啦"
                startForeground(NOTIFICATION_ID, buildNotification(reason))
                startRinging()
                mainHandler.removeCallbacks(autoStopRunnable)
                mainHandler.postDelayed(autoStopRunnable, AUTO_STOP_MS)
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        stopRingingOnly()
        super.onDestroy()
    }

    private fun startRinging() {
        stopRingingOnly()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        val p = MediaPlayer()
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        p.isLooping = true
        p.setDataSource(this, alarmUri)
        p.prepare()
        p.start()
        player = p
    }

    private fun stopRingingOnly() {
        mainHandler.removeCallbacks(autoStopRunnable)
        try {
            player?.stop()
        } catch (_: Throwable) {
        }
        try {
            player?.release()
        } catch (_: Throwable) {
        }
        player = null
    }

    private fun stopRingingAndSelf() {
        stopRingingOnly()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildNotification(reason: String): Notification {
        createChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this,
            8001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this,
            8002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("起床啦")
            .setContentText(reason)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPi)
            .addAction(0, "停止闹钟", stopPi)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Weather Alarm Ringing",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(null, null)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "com.example.weatherflexalarm.action.START_RINGING"
        private const val ACTION_STOP = "com.example.weatherflexalarm.action.STOP_RINGING"
        private const val EXTRA_REASON = "extra_reason"
        private const val CHANNEL_ID = "weather_alarm_ringing_v1"
        private const val NOTIFICATION_ID = 5005
        private const val AUTO_STOP_MS = 2 * 60 * 1000L

        fun start(context: Context, reason: String) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, AlarmRingingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_REASON, reason)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, AlarmRingingService::class.java).apply {
                action = ACTION_STOP
            }
            appContext.startService(intent)
        }
    }
}

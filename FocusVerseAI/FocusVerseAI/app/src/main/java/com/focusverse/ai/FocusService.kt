package com.focusverse.ai

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings

/** Is Usage Access granted? (Needed to detect which app is in the foreground.) */
fun hasUsageAccess(ctx: Context): Boolean {
    val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

fun hasOverlayAccess(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

/** Apps that have a launcher icon, as (packageName, label), sorted by label. */
fun launchableApps(ctx: Context): List<Pair<String, String>> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
        .filter { it.first != ctx.packageName }
        .distinctBy { it.first }
        .sortedBy { it.second.lowercase() }
}

/**
 * Foreground service that keeps the focus timer honest and, if the user allowed Usage Access,
 * watches which app is in front. When a blocked app opens it brings FocusVerse back on top.
 * It only reads WHICH app is open. It never reads screen content, messages or passwords.
 */
class FocusService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastFg: String? = null
    private var lastQuery = 0L
    private var lastInterrupt = 0L

    private val ticker = object : Runnable {
        override fun run() {
            tick()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppState.init(applicationContext)
        createChannels()
        val n = buildOngoing()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        lastQuery = System.currentTimeMillis()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun tick() {
        if (AppState.focusEnd == 0L) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (AppState.checkExpiry()) {
            notifyDone()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (AppState.focusEnforced) detectBlockedApp()
        handler.postDelayed(ticker, 1000)
    }

    @Suppress("DEPRECATION")
    private fun detectBlockedApp() {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(lastQuery - 2000, now)
            lastQuery = now
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastFg = e.packageName
                }
            }
            val fg = lastFg
            if (fg != null && fg != packageName && fg in AppState.blocked && now - lastInterrupt > 3000) {
                lastInterrupt = now
                val i = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("interrupt", fg)
                startActivity(i)
            }
        } catch (e: Exception) {
            // Permission revoked or OEM restriction: the timer keeps running, enforcement just stops.
        }
    }

    private fun createChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("focus", "Focus session", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel("done", "Session finished", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildOngoing(): Notification =
        Notification.Builder(this, "focus")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Focus session running")
            .setContentText("Stay on track. Tap to open FocusVerse.")
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun notifyDone() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = Notification.Builder(this, "done")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Focus session finished")
            .setContentText(AppState.lastResult.take(120))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        nm.notify(2, n)
    }
}

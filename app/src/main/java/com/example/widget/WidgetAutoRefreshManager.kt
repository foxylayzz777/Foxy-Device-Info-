package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock

object WidgetAutoRefreshManager {

    const val ACTION_AUTO_REFRESH = "com.example.widget.ACTION_AUTO_REFRESH_WIDGET"
    private const val PREFS_NAME = "foxy_device_info_prefs"
    private const val PREF_KEY_ENABLED = "widget_auto_refresh_enabled"
    private const val PREF_KEY_INTERVAL = "widget_auto_refresh_interval_sec"
    private const val PREF_KEY_SCREEN_ON_ONLY = "widget_refresh_screen_on_only"
    private const val REQUEST_CODE_ALARM = 8881

    fun isAutoRefreshEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_ENABLED, true)
    }

    fun getRefreshIntervalSec(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_KEY_INTERVAL, 15)
    }

    fun isScreenOnOnly(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_SCREEN_ON_ONLY, true)
    }

    fun setRefreshConfig(
        context: Context,
        enabled: Boolean,
        intervalSec: Int,
        screenOnOnly: Boolean
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PREF_KEY_ENABLED, enabled)
            putInt(PREF_KEY_INTERVAL, intervalSec)
            putBoolean(PREF_KEY_SCREEN_ON_ONLY, screenOnOnly)
            apply()
        }

        if (enabled) {
            scheduleNextRefresh(context)
        } else {
            cancelAutoRefresh(context)
        }
    }

    fun scheduleNextRefresh(context: Context) {
        try {
            if (!isAutoRefreshEnabled(context)) {
                cancelAutoRefresh(context)
                return
            }

            // Verify if user actually has placed any widgets on the home screen
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, FoxyAppWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (widgetIds == null || widgetIds.isEmpty()) {
                // No active widgets on desktop, do not waste battery
                cancelAutoRefresh(context)
                return
            }

            // If user prefers only refreshing when screen is on / interactive, check power manager
            if (isScreenOnOnly(context)) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isScreenInteractive = powerManager?.isInteractive ?: true
                if (!isScreenInteractive) {
                    // Screen is currently off; pause alarm and wait for SCREEN_ON broadcast
                    return
                }
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intervalSec = getRefreshIntervalSec(context).coerceAtLeast(5)

            val intent = Intent(context, FoxyAppWidgetProvider::class.java).apply {
                action = ACTION_AUTO_REFRESH
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)
            val triggerAtMillis = SystemClock.elapsedRealtime() + (intervalSec * 1000L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Throwable) {}
    }

    fun cancelAutoRefresh(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, FoxyAppWidgetProvider::class.java).apply {
                action = ACTION_AUTO_REFRESH
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)
            alarmManager.cancel(pendingIntent)
        } catch (_: Throwable) {}
    }
}

package com.example.widget

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Locale

class FoxyAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.foxy_app_widget)

            // Click to open main app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            // Read battery
            val bIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = bIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) ?: 50
            val scale = bIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 50
            val tempC = (bIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 290) ?: 290) / 10

            views.setTextViewText(R.id.widget_battery, "$pct% ⚡")
            views.setTextViewText(R.id.widget_temp, "${tempC}°C")

            // Read RAM
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val ramTotalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            val ramAvailGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            val ramUsedGb = ramTotalGb - ramAvailGb

            views.setTextViewText(
                R.id.widget_ram,
                String.format(Locale.US, "RAM: %.1f / %.1f GB", ramUsedGb, ramTotalGb)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

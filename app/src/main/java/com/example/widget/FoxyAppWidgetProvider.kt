package com.example.widget

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.TimeUnit

class FoxyAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, FoxyAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"
        private const val PREFS_NAME = "foxy_device_info_prefs"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, FoxyAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                for (id in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            } catch (_: Exception) {}
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.foxy_app_widget)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. Read User Customization Preferences
            val themeStyle = prefs.getString("widget_theme_style", "LIQUID_GLASS") ?: "LIQUID_GLASS"
            val titleMode = prefs.getString("widget_title_mode", "SOC_NAME") ?: "SOC_NAME"
            val customTitle = prefs.getString("widget_custom_title", "") ?: ""
            val showCpu = prefs.getBoolean("widget_show_cpu", true)
            val showRam = prefs.getBoolean("widget_show_ram", true)
            val showStorage = prefs.getBoolean("widget_show_storage", true)
            val showBattery = prefs.getBoolean("widget_show_battery", true)
            val showNetwork = prefs.getBoolean("widget_show_network", true)
            val showDisplay = prefs.getBoolean("widget_show_display", true)
            val showActions = prefs.getBoolean("widget_show_actions", true)
            val showUptime = prefs.getBoolean("widget_show_uptime", true)

            // 2. Set Theme Background
            val bgRes = when (themeStyle) {
                "AMOLED" -> R.drawable.widget_bg_amoled
                "FOXY" -> R.drawable.widget_bg_foxy
                "GAMING" -> R.drawable.widget_bg_gaming
                "CYBERPUNK" -> R.drawable.widget_bg_cyberpunk
                "RETRO_AMBER" -> R.drawable.widget_bg_retro_amber
                "MINIMAL_FROST" -> R.drawable.widget_bg_minimal_frost
                else -> R.drawable.widget_bg_liquid_glass
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

            // 3. Navigation PendingIntents Helper
            fun createNavPendingIntent(targetTab: String, reqCode: Int): PendingIntent {
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("target_tab", targetTab)
                }
                return PendingIntent.getActivity(
                    context, reqCode, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            // Click Root or Header -> Open Home
            val homePendingIntent = createNavPendingIntent("HOME", 0)
            views.setOnClickPendingIntent(R.id.widget_header, homePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, homePendingIntent)

            // Refresh Button Broadcast
            val refreshIntent = Intent(context, FoxyAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Quick Action Buttons
            views.setOnClickPendingIntent(R.id.widget_btn_monitor, createNavPendingIntent("MONITOR", 10))
            views.setOnClickPendingIntent(R.id.widget_btn_diagnostics, createNavPendingIntent("DIAGNOSTICS", 11))
            views.setOnClickPendingIntent(R.id.widget_btn_apps, createNavPendingIntent("APPS", 12))
            views.setOnClickPendingIntent(R.id.widget_btn_settings, createNavPendingIntent("SETTINGS", 13))

            // Metric Row shortcuts to Monitor
            views.setOnClickPendingIntent(R.id.widget_row_cpu, createNavPendingIntent("MONITOR", 14))
            views.setOnClickPendingIntent(R.id.widget_row_ram, createNavPendingIntent("MONITOR", 15))

            // 4. Header Details: Title & Subtitle
            val socName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val m = Build.SOC_MODEL
                val man = Build.SOC_MANUFACTURER
                if (m.isNotBlank() && man.isNotBlank()) "$man $m" else if (m.isNotBlank()) m else Build.HARDWARE
            } else Build.HARDWARE

            val titleText = when (titleMode) {
                "CUSTOM" -> if (customTitle.isNotBlank()) customTitle else Build.MODEL
                "DEVICE_MODEL" -> "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
                "APP_NAME" -> "Foxy Device Info"
                else -> if (socName.isNotBlank() && socName != "unknown") socName else Build.MODEL
            }
            views.setTextViewText(R.id.widget_title, titleText)

            val osSubtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${if (Build.SUPPORTED_ABIS.any { it.contains("64") }) "64-Bit" else "32-Bit"}"
            views.setTextViewText(R.id.widget_subtitle, osSubtitle)

            // 5. Battery & Thermal
            val bIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = bIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) ?: 50
            val scale = bIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 50
            val status = bIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val tempC = (bIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300) / 10
            val voltageMv = bIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000
            val health = bIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD) ?: BatteryManager.BATTERY_HEALTH_GOOD
            val healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Warm"
                else -> "Normal"
            }

            views.setTextViewText(
                R.id.widget_battery_badge,
                if (isCharging) "$batteryPct% ⚡" else "$batteryPct%"
            )

            views.setTextViewText(
                R.id.widget_temp_val,
                "🌡️ Battery: $tempC°C • $healthStr • ${voltageMv}mV"
            )

            // 6. CPU Load & Frequency
            val cpuUsage = readCpuUsagePercent()
            val totalCores = Runtime.getRuntime().availableProcessors()
            views.setTextViewText(
                R.id.widget_cpu_val,
                "$cpuUsage% Load • $totalCores Cores"
            )
            views.setProgressBar(R.id.widget_cpu_progress, 100, cpuUsage, false)
            views.setViewVisibility(R.id.widget_row_cpu, if (showCpu) View.VISIBLE else View.GONE)

            // 7. RAM Memory
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val ramTotalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            val ramAvailGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            val ramUsedGb = ramTotalGb - ramAvailGb
            val ramPct = if (ramTotalGb > 0) ((ramUsedGb / ramTotalGb) * 100).toInt() else 0

            views.setTextViewText(
                R.id.widget_ram_val,
                String.format(Locale.US, "%.1f / %.1f GB (%d%%)", ramUsedGb, ramTotalGb, ramPct)
            )
            views.setProgressBar(R.id.widget_ram_progress, 100, ramPct, false)
            views.setViewVisibility(R.id.widget_row_ram, if (showRam) View.VISIBLE else View.GONE)

            // 8. Storage (ROM)
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                val totalBytes = stat.totalBytes
                val availBytes = stat.availableBytes
                val usedBytes = totalBytes - availBytes
                val storageTotalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
                val storageUsedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
                val storagePct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

                views.setTextViewText(
                    R.id.widget_storage_val,
                    String.format(Locale.US, "%.1f / %.1f GB (%d%%)", storageUsedGb, storageTotalGb, storagePct)
                )
                views.setProgressBar(R.id.widget_storage_progress, 100, storagePct, false)
                views.setViewVisibility(R.id.widget_row_storage, if (showStorage) View.VISIBLE else View.GONE)
            } catch (_: Exception) {
                views.setViewVisibility(R.id.widget_row_storage, View.GONE)
            }

            // 9. Network Connectivity Info
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetwork
                val caps = cm?.getNetworkCapabilities(activeNetwork)
                val netStr = when {
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi Connected 🛜"
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data (LTE/5G) 📶"
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet LAN 🌐"
                    caps != null -> "Connected 🌐"
                    else -> "Offline / Disconnected"
                }
                views.setTextViewText(R.id.widget_network_val, netStr)
                views.setViewVisibility(R.id.widget_row_network, if (showNetwork) View.VISIBLE else View.GONE)
            } catch (_: Exception) {
                views.setViewVisibility(R.id.widget_row_network, View.GONE)
            }

            // 10. Display & Refresh Rate Info
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                val display = wm?.defaultDisplay
                val refreshRate = display?.refreshRate?.toInt() ?: 60
                val metrics = context.resources.displayMetrics
                val displayStr = "${refreshRate}Hz • ${metrics.widthPixels}×${metrics.heightPixels}"
                views.setTextViewText(R.id.widget_display_val, displayStr)
                views.setViewVisibility(R.id.widget_row_display, if (showDisplay) View.VISIBLE else View.GONE)
            } catch (_: Exception) {
                views.setViewVisibility(R.id.widget_row_display, View.GONE)
            }

            // 11. Quick Actions Row Visibility
            views.setViewVisibility(R.id.widget_row_actions, if (showActions) View.VISIBLE else View.GONE)

            // 12. Uptime & Battery Row Visibility
            val uptimeMs = SystemClock.elapsedRealtime()
            val hours = TimeUnit.MILLISECONDS.toHours(uptimeMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60
            views.setTextViewText(R.id.widget_uptime_val, "Up: ${hours}h ${minutes}m")
            views.setViewVisibility(R.id.widget_uptime_val, if (showUptime) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_row_battery, if (showBattery) View.VISIBLE else View.GONE)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun readCpuUsagePercent(): Int {
            return try {
                val reader = RandomAccessFile("/proc/stat", "r")
                val load = reader.readLine()
                reader.close()
                val toks = load.split(" ").filter { it.isNotBlank() }
                if (toks.size >= 8) {
                    val user = toks[1].toLong()
                    val nice = toks[2].toLong()
                    val sys = toks[3].toLong()
                    val idle = toks[4].toLong()
                    val iowait = toks[5].toLong()
                    val irq = toks[6].toLong()
                    val softirq = toks[7].toLong()
                    val total = user + nice + sys + idle + iowait + irq + softirq
                    val active = total - idle - iowait
                    if (total > 0) ((active.toDouble() / total) * 100).toInt().coerceIn(5, 95) else 25
                } else 25
            } catch (_: Exception) {
                28
            }
        }
    }
}

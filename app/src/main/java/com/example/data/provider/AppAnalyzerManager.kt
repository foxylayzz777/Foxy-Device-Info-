package com.example.data.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.AppInfoItem
import com.example.data.model.AppPermissionInfo
import com.example.data.model.PermissionCategorySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppAnalyzerManager(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val list = mutableListOf<AppInfoItem>()

        try {
            val flags = PackageManager.GET_PERMISSIONS
            val packages: List<PackageInfo> = pm.getInstalledPackages(flags)

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = try {
                    appInfo.loadLabel(pm).toString()
                } catch (_: Exception) {
                    pkg.packageName
                }

                val apkFile = File(appInfo.sourceDir)
                val apkSize = if (apkFile.exists()) apkFile.length() else 0L

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else 21

                val permList = mutableListOf<AppPermissionInfo>()
                val reqPerms = pkg.requestedPermissions
                val reqFlags = pkg.requestedPermissionsFlags

                if (reqPerms != null) {
                    for (i in reqPerms.indices) {
                        val perm = reqPerms[i]
                        val isGranted = if (reqFlags != null && i < reqFlags.size) {
                            (reqFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                        } else false

                        val isDangerous = isDangerousPermission(perm)
                        val simpleName = perm.substringAfterLast(".")

                        permList.add(
                            AppPermissionInfo(
                                permission = perm,
                                simpleName = simpleName,
                                isGranted = isGranted,
                                isDangerous = isDangerous
                            )
                        )
                    }
                }

                list.add(
                    AppInfoItem(
                        packageName = pkg.packageName,
                        appName = appName,
                        versionName = pkg.versionName ?: "1.0",
                        versionCode = vCode,
                        targetSdkVersion = appInfo.targetSdkVersion,
                        minSdkVersion = minSdk,
                        isSystemApp = isSystem,
                        apkSizeBytes = apkSize,
                        firstInstallTime = pkg.firstInstallTime,
                        lastUpdateTime = pkg.lastUpdateTime,
                        permissions = permList
                    )
                )
            }
        } catch (_: Exception) {}

        list.sortedBy { it.appName.lowercase() }
    }

    suspend fun getPermissionSummaries(apps: List<AppInfoItem>): List<PermissionCategorySummary> = withContext(Dispatchers.Default) {
        val dangerousGroups = mapOf(
            "Camera" to ("android.permission.CAMERA" to "Access to device photo and video cameras"),
            "Microphone" to ("android.permission.RECORD_AUDIO" to "Record surrounding audio and voice"),
            "Precise Location" to ("android.permission.ACCESS_FINE_LOCATION" to "Precise GPS coordinates"),
            "Coarse Location" to ("android.permission.ACCESS_COARSE_LOCATION" to "Approximate network-based location"),
            "Contacts" to ("android.permission.READ_CONTACTS" to "Read device address book contacts"),
            "Phone State" to ("android.permission.READ_PHONE_STATE" to "Read phone calls and carrier state"),
            "Post Notifications" to ("android.permission.POST_NOTIFICATIONS" to "Show notification badges and alerts"),
            "Nearby Bluetooth" to ("android.permission.BLUETOOTH_CONNECT" to "Connect to nearby Bluetooth devices")
        )

        val results = mutableListOf<PermissionCategorySummary>()

        for ((label, pair) in dangerousGroups) {
            val (permKey, desc) = pair
            val appsWithPerm = apps.filter { app ->
                app.permissions.any { it.permission == permKey && it.isGranted }
            }.map { it.appName }

            results.add(
                PermissionCategorySummary(
                    permissionName = label,
                    description = desc,
                    iconKey = when (label) {
                        "Camera" -> "camera"
                        "Microphone" -> "mic"
                        "Precise Location", "Coarse Location" -> "location"
                        "Contacts" -> "contacts"
                        "Phone State" -> "phone"
                        "Post Notifications" -> "notifications"
                        else -> "bluetooth"
                    },
                    grantedAppCount = appsWithPerm.size,
                    apps = appsWithPerm
                )
            )
        }

        results
    }

    private fun isDangerousPermission(perm: String): Boolean {
        return perm.contains("CAMERA") ||
                perm.contains("RECORD_AUDIO") ||
                perm.contains("LOCATION") ||
                perm.contains("CONTACTS") ||
                perm.contains("CALENDAR") ||
                perm.contains("STORAGE") ||
                perm.contains("MEDIA") ||
                perm.contains("PHONE") ||
                perm.contains("SMS") ||
                perm.contains("BLUETOOTH_CONNECT") ||
                perm.contains("POST_NOTIFICATIONS")
    }
}

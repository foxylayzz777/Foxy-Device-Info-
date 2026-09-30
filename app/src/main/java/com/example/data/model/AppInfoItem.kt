package com.example.data.model

data class AppPermissionInfo(
    val permission: String,
    val simpleName: String,
    val isGranted: Boolean,
    val isDangerous: Boolean
)

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val isSystemApp: Boolean,
    val apkSizeBytes: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val permissions: List<AppPermissionInfo> = emptyList()
)

data class PermissionCategorySummary(
    val permissionName: String,
    val description: String,
    val iconKey: String,
    val grantedAppCount: Int,
    val apps: List<String>
)

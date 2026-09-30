package com.example.data.model

data class ThermalZone(
    val name: String,
    val tempCelsius: Float
)

data class RealtimeMetrics(
    val cpuUsagePercent: Float,
    val cpuCoreFrequenciesMhz: List<Int>,
    val ramUsedBytes: Long,
    val ramTotalBytes: Long,
    val ramUsedPercent: Float,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val storageUsedPercent: Float,
    val batteryPercent: Int,
    val batteryTempCelsius: Float,
    val batteryVoltageMv: Int,
    val batteryCurrentMa: Int,
    val batteryStatus: String,
    val downloadSpeedBytesPerSec: Long,
    val uploadSpeedBytesPerSec: Long,
    val thermalZones: List<ThermalZone>,
    val cpuHistory: List<Float> = emptyList(),
    val ramHistory: List<Float> = emptyList(),
    val downloadSpeedHistory: List<Float> = emptyList(),
    val uploadSpeedHistory: List<Float> = emptyList()
)

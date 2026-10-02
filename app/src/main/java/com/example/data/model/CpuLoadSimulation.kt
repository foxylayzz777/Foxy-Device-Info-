package com.example.data.model

data class CpuSimulationProgress(
    val isRunning: Boolean = false,
    val percent: Float = 0f,
    val elapsedSeconds: Float = 0f,
    val totalSeconds: Int = 10,
    val currentOps: Long = 0L,
    val currentOpsPerSec: Double = 0.0,
    val currentCpuUsagePct: Int = 0,
    val currentTempCelsius: Double = 0.0,
    val threadsCount: Int = 1,
    val statusMessage: String = "Ready"
)

data class CpuLoadSimulationResult(
    val score: Int,
    val totalOps: Long,
    val avgOpsPerSec: Double,
    val threadsUsed: Int,
    val durationSeconds: Int,
    val startTempCelsius: Double,
    val peakTempCelsius: Double,
    val tempDeltaCelsius: Double,
    val stabilityPercent: Double,
    val performanceTier: String,
    val timestamp: Long = System.currentTimeMillis()
)

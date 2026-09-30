package com.example.data.model

data class BenchmarkResult(
    val cpuSingleCoreScore: Int,
    val cpuMultiCoreScore: Int,
    val ramSpeedMbPerSec: Double,
    val storageReadMbPerSec: Double,
    val storageWriteMbPerSec: Double,
    val totalBenchmarkScore: Int,
    val tierLabel: String,
    val timestamp: Long
)

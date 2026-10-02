package com.example.data.provider

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.data.model.CpuLoadSimulationResult
import com.example.data.model.CpuSimulationProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class CpuLoadSimulator(private val context: Context) {

    private val isCancelled = AtomicBoolean(false)

    fun cancelSimulation() {
        isCancelled.set(true)
    }

    suspend fun runSimulation(
        durationSeconds: Int = 10,
        threadsCount: Int = Runtime.getRuntime().availableProcessors(),
        intensityPercent: Int = 100,
        onProgress: (CpuSimulationProgress) -> Unit
    ): CpuLoadSimulationResult = withContext(Dispatchers.Default) {
        isCancelled.set(false)
        val initialTemp = readBatteryTemperature()
        var peakTemp = initialTemp
        val totalOpsCounter = AtomicLong(0L)

        val totalDurationMs = durationSeconds * 1000L
        val startTime = System.currentTimeMillis()
        val endTime = startTime + totalDurationMs

        val safeThreads = threadsCount.coerceIn(1, Runtime.getRuntime().availableProcessors() * 2)
        val dutyCycleMs = 100L
        val activeMs = (dutyCycleMs * (intensityPercent.coerceIn(10, 100) / 100f)).toLong()
        val idleMs = dutyCycleMs - activeMs

        // Interval rates to calculate stability (first third vs last third)
        var earlyOpsPerSec = 0.0
        var lateOpsPerSec = 0.0
        var earlyOpsSampled = false

        coroutineScope {
            // Launch worker threads for CPU compute
            val workers = (0 until safeThreads).map { threadIndex ->
                async(Dispatchers.Default) {
                    var localOps = 0L
                    var seed = (threadIndex + 1) * 31.4159
                    while (System.currentTimeMillis() < endTime && !isCancelled.get() && isActive) {
                        val sliceEnd = System.currentTimeMillis() + activeMs
                        // Active computation loop
                        while (System.currentTimeMillis() < sliceEnd && !isCancelled.get() && isActive) {
                            for (i in 0 until 500) {
                                seed = sin(seed) * cos(seed) + sqrt(seed.coerceAtLeast(1.0))
                                if (seed > 1000.0) seed /= 10.0
                            }
                            localOps += 500
                            if (localOps % 5000 == 0L) {
                                totalOpsCounter.addAndGet(5000)
                                localOps = 0
                            }
                        }
                        if (localOps > 0) {
                            totalOpsCounter.addAndGet(localOps)
                            localOps = 0
                        }

                        // Throttle idle duty cycle if intensity < 100%
                        if (idleMs > 0 && !isCancelled.get() && isActive) {
                            delay(idleMs)
                        }
                    }
                }
            }

            // Progress Reporter Coroutine (Updating at ~60-90 FPS interval for ultra-smooth UI)
            val progressJob = async(Dispatchers.Default) {
                var lastOps = 0L
                var lastTime = startTime

                while (System.currentTimeMillis() < endTime && !isCancelled.get() && isActive) {
                    delay(50L) // 20 times per second for smooth UI animation
                    val now = System.currentTimeMillis()
                    val elapsedMs = now - startTime
                    val elapsedSec = elapsedMs / 1000f
                    val progressFraction = (elapsedMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)

                    val currentTotalOps = totalOpsCounter.get()
                    val windowTimeSec = (now - lastTime) / 1000.0
                    val windowOps = currentTotalOps - lastOps
                    val instantOpsPerSec = if (windowTimeSec > 0) windowOps / windowTimeSec else 0.0
                    lastOps = currentTotalOps
                    lastTime = now

                    val currentTemp = readBatteryTemperature()
                    if (currentTemp > peakTemp) peakTemp = currentTemp

                    // Capture early vs late throughput for throttle detection
                    if (!earlyOpsSampled && elapsedSec >= (durationSeconds * 0.25f)) {
                        earlyOpsPerSec = instantOpsPerSec
                        earlyOpsSampled = true
                    }
                    if (elapsedSec >= (durationSeconds * 0.75f)) {
                        lateOpsPerSec = instantOpsPerSec
                    }

                    val cpuLoad = readCpuUsagePercent()

                    onProgress(
                        CpuSimulationProgress(
                            isRunning = true,
                            percent = progressFraction,
                            elapsedSeconds = elapsedSec,
                            totalSeconds = durationSeconds,
                            currentOps = currentTotalOps,
                            currentOpsPerSec = instantOpsPerSec,
                            currentCpuUsagePct = cpuLoad,
                            currentTempCelsius = currentTemp,
                            threadsCount = safeThreads,
                            statusMessage = "Simulating load on $safeThreads cores (${intensityPercent}%)..."
                        )
                    )
                }
            }

            workers.awaitAll()
            progressJob.await()
        }

        val totalOps = totalOpsCounter.get()
        val totalElapsedSec = max(1.0, (System.currentTimeMillis() - startTime) / 1000.0)
        val avgOpsPerSec = totalOps / totalElapsedSec
        val tempDelta = (peakTemp - initialTemp).coerceAtLeast(0.0)

        // Calculate stability: ratio of late throughput to early throughput
        val stability = if (earlyOpsPerSec > 0) {
            ((lateOpsPerSec / earlyOpsPerSec) * 100.0).coerceIn(40.0, 100.0)
        } else {
            98.5
        }

        // Benchmark Score calculation
        val baseScore = (avgOpsPerSec / 15_000.0).roundToInt()
        val multiCoreMultiplier = 1.0 + (safeThreads * 0.15)
        val finalScore = (baseScore * multiCoreMultiplier).roundToInt().coerceAtLeast(500)

        val tier = when {
            finalScore >= 8500 -> "Ultra Flagship Octa-Core 🦊🔥"
            finalScore >= 6000 -> "High Performance Gaming Tier ⚡"
            finalScore >= 4000 -> "Upper Mid-Range Sustained ✨"
            finalScore >= 2000 -> "Balanced Multi-Core Everyday 👍"
            else -> "Entry Level Performance 🌱"
        }

        onProgress(
            CpuSimulationProgress(
                isRunning = false,
                percent = 1f,
                elapsedSeconds = durationSeconds.toFloat(),
                totalSeconds = durationSeconds,
                currentOps = totalOps,
                currentOpsPerSec = avgOpsPerSec,
                currentCpuUsagePct = 0,
                currentTempCelsius = peakTemp,
                threadsCount = safeThreads,
                statusMessage = "Simulation completed successfully!"
            )
        )

        CpuLoadSimulationResult(
            score = finalScore,
            totalOps = totalOps,
            avgOpsPerSec = avgOpsPerSec,
            threadsUsed = safeThreads,
            durationSeconds = durationSeconds,
            startTempCelsius = ((initialTemp * 10).roundToInt() / 10.0),
            peakTempCelsius = ((peakTemp * 10).roundToInt() / 10.0),
            tempDeltaCelsius = ((tempDelta * 10).roundToInt() / 10.0),
            stabilityPercent = ((stability * 10).roundToInt() / 10.0),
            performanceTier = tier,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun readBatteryTemperature(): Double {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300
            temp / 10.0
        } catch (_: Throwable) {
            32.0
        }
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
                if (total > 0) ((active.toDouble() / total) * 100).toInt().coerceIn(5, 100) else 50
            } else 50
        } catch (_: Throwable) {
            50
        }
    }
}

package com.example.data.provider

import android.content.Context
import com.example.data.model.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

class BenchmarkRunner(private val context: Context) {

    suspend fun runBenchmark(onProgress: (step: String, percent: Float) -> Unit): BenchmarkResult = withContext(Dispatchers.Default) {
        // Step 1: Single Core CPU Benchmark
        onProgress("Running Single-Core CPU compute...", 0.15f)
        val singleCoreScore = runSingleCoreTest()

        // Step 2: Multi Core CPU Benchmark
        onProgress("Running Multi-Core Parallel compute...", 0.45f)
        val multiCoreScore = runMultiCoreTest()

        // Step 3: RAM Memory Bandwidth Benchmark
        onProgress("Testing RAM memory throughput...", 0.70f)
        val ramSpeedMbPerSec = runRamSpeedTest()

        // Step 4: Storage Sequential Read/Write Benchmark
        onProgress("Benchmarking Storage I/O read & write...", 0.90f)
        val (storageReadMb, storageWriteMb) = runStorageSpeedTest()

        // Calculate aggregate performance score
        val totalScore = (
                (singleCoreScore * 1.5) +
                        (multiCoreScore * 0.8) +
                        (ramSpeedMbPerSec * 0.05) +
                        (storageReadMb * 0.5) +
                        (storageWriteMb * 0.5)
                ).roundToInt()

        val tier = when {
            totalScore >= 8000 -> "Ultra Flagship 🦊🔥"
            totalScore >= 5500 -> "High Performance 🦊⚡"
            totalScore >= 3500 -> "Upper Mid-Range 🦊✨"
            totalScore >= 2000 -> "Balanced Daily 🦊👍"
            else -> "Entry Level 🦊🌱"
        }

        onProgress("Benchmark completed!", 1.0f)

        BenchmarkResult(
            cpuSingleCoreScore = singleCoreScore,
            cpuMultiCoreScore = multiCoreScore,
            ramSpeedMbPerSec = (ramSpeedMbPerSec * 10).roundToInt() / 10.0,
            storageReadMbPerSec = (storageReadMb * 10).roundToInt() / 10.0,
            storageWriteMbPerSec = (storageWriteMb * 10).roundToInt() / 10.0,
            totalBenchmarkScore = totalScore,
            tierLabel = tier,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun runSingleCoreTest(): Int {
        val start = System.currentTimeMillis()
        var primesCount = 0
        val maxNumber = 120_000

        for (i in 2..maxNumber) {
            var isPrime = true
            val sqrtI = kotlin.math.sqrt(i.toDouble()).toInt()
            for (j in 2..sqrtI) {
                if (i % j == 0) {
                    isPrime = false
                    break
                }
            }
            if (isPrime) primesCount++
        }

        val elapsedMs = max(1L, System.currentTimeMillis() - start)
        val score = (primesCount.toDouble() / elapsedMs * 18_000).roundToInt()
        return score.coerceIn(500, 3500)
    }

    private suspend fun runMultiCoreTest(): Int = withContext(Dispatchers.Default) {
        val cores = Runtime.getRuntime().availableProcessors()
        val start = System.currentTimeMillis()

        val deferreds = (0 until cores).map {
            async(Dispatchers.Default) {
                val md = MessageDigest.getInstance("SHA-256")
                var hashData = "FoxyDeviceBenchmarkSeedString".toByteArray()
                for (iter in 0 until 40_000) {
                    md.update(hashData)
                    hashData = md.digest()
                }
                hashData.size
            }
        }

        deferreds.awaitAll()
        val elapsedMs = max(1L, System.currentTimeMillis() - start)
        val score = ((cores * 40_000.0) / elapsedMs * 35).roundToInt()
        score.coerceIn(1200, 12000)
    }

    private fun runRamSpeedTest(): Double {
        val bufferSize = 8 * 1024 * 1024 // 8MB buffer
        val src = ByteArray(bufferSize) { it.toByte() }
        val dst = ByteArray(bufferSize)

        val iterations = 10
        val start = System.nanoTime()

        for (i in 0 until iterations) {
            System.arraycopy(src, 0, dst, 0, bufferSize)
        }

        val elapsedNano = max(1L, System.nanoTime() - start)
        val totalMbCopied = (bufferSize.toDouble() * iterations) / (1024.0 * 1024.0)
        val seconds = elapsedNano / 1_000_000_000.0

        val speed = totalMbCopied / seconds
        return speed.coerceIn(150.0, 18000.0)
    }

    private fun runStorageSpeedTest(): Pair<Double, Double> {
        val testFile = File(context.cacheDir, "foxy_io_bench.tmp")
        val sizeBytes = 6 * 1024 * 1024 // 6MB
        val buffer = ByteArray(64 * 1024) { (it % 128).toByte() }

        var writeSpeed = 45.0
        var readSpeed = 120.0

        try {
            // Write
            val writeStart = System.nanoTime()
            val fos = FileOutputStream(testFile)
            var bytesWritten = 0
            while (bytesWritten < sizeBytes) {
                fos.write(buffer)
                bytesWritten += buffer.size
            }
            fos.fd.sync()
            fos.close()
            val writeElapsed = max(1L, System.nanoTime() - writeStart)
            val writeSec = writeElapsed / 1_000_000_000.0
            writeSpeed = (sizeBytes / (1024.0 * 1024.0)) / writeSec

            // Read
            val readStart = System.nanoTime()
            val fis = FileInputStream(testFile)
            val readBuf = ByteArray(64 * 1024)
            var bytesRead = 0
            while (fis.read(readBuf).also { bytesRead = it } != -1) {
                // read
            }
            fis.close()
            val readElapsed = max(1L, System.nanoTime() - readStart)
            val readSec = readElapsed / 1_000_000_000.0
            readSpeed = (sizeBytes / (1024.0 * 1024.0)) / readSec

            testFile.delete()
        } catch (_: Exception) {
            testFile.delete()
        }

        return Pair(readSpeed.coerceIn(30.0, 3500.0), writeSpeed.coerceIn(20.0, 2500.0))
    }
}

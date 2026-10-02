package com.example.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CpuLoadSimulationResult
import com.example.data.model.CpuSimulationProgress
import com.example.ui.viewmodel.FoxyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuLoadSimulationScreen(
    viewModel: FoxyViewModel,
    onNavigateBack: () -> Unit,
    onOpenRefreshRateMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress by viewModel.cpuSimulationProgress.collectAsState()
    val result by viewModel.cpuSimulationResult.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val realtimeMetrics by viewModel.realtimeMetrics.collectAsState()

    val totalCores = remember { Runtime.getRuntime().availableProcessors() }
    var selectedDurationSec by remember { mutableIntStateOf(10) }
    var selectedIntensityPct by remember { mutableIntStateOf(100) }
    var selectedThreads by remember { mutableIntStateOf(totalCores) }

    BackHandler {
        if (progress.isRunning) {
            viewModel.stopCpuLoadSimulation()
        }
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CPU Load Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Thread Stress & Compute Benchmark",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (progress.isRunning) {
                            viewModel.stopCpuLoadSimulation()
                        }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRefreshRateMenu) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "90Hz 90Fps Menu",
                            tint = Color(0xFF00E676)
                        )
                    }
                    if (result != null && !progress.isRunning) {
                        IconButton(onClick = { viewModel.clearCpuSimulationResult() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
            // 1. Hardware Specs & Smooth 90 FPS Badge
            item {
                val displayHz = deviceInfo?.gpuDisplay?.refreshRateHz?.toInt() ?: 90
                CpuHardwareBanner(
                    socName = deviceInfo?.cpu?.socName ?: Build.HARDWARE,
                    cpuCores = totalCores,
                    currentTemp = realtimeMetrics.batteryTempCelsius,
                    displayHz = displayHz,
                    onOpenRefreshRateMenu = onOpenRefreshRateMenu
                )
            }

            // 2. Active Simulation State vs Results vs Configuration
            if (progress.isRunning) {
                item {
                    ActiveSimulationDashboard(
                        progress = progress,
                        onStop = { viewModel.stopCpuLoadSimulation() }
                    )
                }
            } else if (result != null) {
                item {
                    SimulationResultsView(
                        result = result!!,
                        onRunAgain = {
                            viewModel.startCpuLoadSimulation(
                                durationSec = selectedDurationSec,
                                threadsCount = selectedThreads,
                                intensityPct = selectedIntensityPct
                            )
                        },
                        onConfigureNew = { viewModel.clearCpuSimulationResult() },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🦊 Foxy CPU Load Benchmark Result:\n" +
                                            "• Score: ${result!!.score} Pts (${result!!.performanceTier})\n" +
                                            "• Operations: ${String.format(Locale.US, "%.1f", result!!.avgOpsPerSec / 1_000_000.0)} M Ops/s\n" +
                                            "• Cores: ${result!!.threadsUsed} Cores for ${result!!.durationSeconds}s\n" +
                                            "• Thermal Rise: +${result!!.tempDeltaCelsius}°C\n" +
                                            "• Stability: ${result!!.stabilityPercent}%\n" +
                                            "Tested with Foxy Device Info"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share CPU Benchmark"))
                        }
                    )
                }
            } else {
                // Configuration Controls
                item {
                    SimulationConfigCard(
                        totalCores = totalCores,
                        selectedDuration = selectedDurationSec,
                        selectedIntensity = selectedIntensityPct,
                        selectedThreads = selectedThreads,
                        onDurationChange = { selectedDurationSec = it },
                        onIntensityChange = { selectedIntensityPct = it },
                        onThreadsChange = { selectedThreads = it },
                        onStart = {
                            viewModel.startCpuLoadSimulation(
                                durationSec = selectedDurationSec,
                                threadsCount = selectedThreads,
                                intensityPct = selectedIntensityPct
                            )
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun CpuHardwareBanner(
    socName: String,
    cpuCores: Int,
    currentTemp: Float,
    displayHz: Int,
    onOpenRefreshRateMenu: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "CPU",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = socName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$cpuCores Cores • ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 90 FPS / High Refresh Display Pill (Click to open 90Hz 90Fps Menu)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenRefreshRateMenu() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Smooth",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (displayHz >= 88) "${displayHz}Hz 90 FPS" else "${displayHz}Hz Smooth",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676)
                        )
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configure",
                            tint = Color(0xFF00E676).copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniTag(
                    icon = Icons.Default.Thermostat,
                    label = "Current Temp",
                    value = "${currentTemp.toInt()}°C",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniTag(
                    icon = Icons.Default.Speed,
                    label = "Target Mode",
                    value = "SIMD NEON Math",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ActiveSimulationDashboard(
    progress: CpuSimulationProgress,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CPU LOAD SIMULATION IN PROGRESS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Large Circular Progress Indicator with dynamic animated stroke
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.percent,
                    animationSpec = tween(durationMillis = 80, easing = LinearEasing),
                    label = "sim_progress"
                )

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.tertiary
                val trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    // Track
                    drawCircle(
                        color = trackColor,
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                    // Animated Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primaryColor, secondaryColor, primaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress.percent * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val remainingSec = (progress.totalSeconds - progress.elapsedSeconds).coerceAtLeast(0f)
                    Text(
                        text = "${String.format(Locale.US, "%.1fs", remainingSec)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimulationMetricTile(
                    title = "Compute Rate",
                    value = "${String.format(Locale.US, "%.1f", progress.currentOpsPerSec / 1_000_000.0)} M",
                    subtitle = "Ops / sec",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SimulationMetricTile(
                    title = "Temperature",
                    value = "${String.format(Locale.US, "%.1f", progress.currentTempCelsius)}°C",
                    subtitle = "Live Sensor",
                    accent = if (progress.currentTempCelsius >= 42) Color(0xFFFF5252) else Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )
                SimulationMetricTile(
                    title = "Workers",
                    value = "${progress.threadsCount}",
                    subtitle = "Threads",
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Operations ticker
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Completed Ops:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", progress.currentOps),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abort Simulation", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SimulationMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimulationResultsView(
    result: CpuLoadSimulationResult,
    onRunAgain: () -> Unit,
    onConfigureNew: () -> Unit,
    onShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Color(0xFF00E676).copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "SIMULATION COMPLETED ✓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF00E676),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${result.score}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = result.performanceTier,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sustained load across ${result.threadsUsed} threads for ${result.durationSeconds} seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Detailed Bento Metrics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Performance & Thermal Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultStatBox(
                        title = "Compute Speed",
                        value = "${String.format(Locale.US, "%.1f", result.avgOpsPerSec / 1_000_000.0)} M",
                        unit = "Ops / second",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "Thermal Delta",
                        value = "+${result.tempDeltaCelsius}°C",
                        unit = "${result.startTempCelsius}° -> ${result.peakTempCelsius}°",
                        color = if (result.tempDeltaCelsius > 5.0) Color(0xFFFFB300) else Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultStatBox(
                        title = "Thermal Stability",
                        value = "${result.stabilityPercent}%",
                        unit = if (result.stabilityPercent >= 90) "No Throttling" else "Minor Throttle",
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "Total Math Ops",
                        value = String.format(Locale.US, "%.1f M", result.totalOps / 1_000_000.0),
                        unit = "Operations",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRunAgain,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run Again", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Run Again", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }

        TextButton(
            onClick = onConfigureNew,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adjust Simulation Parameters ⚙️")
        }
    }
}

@Composable
private fun ResultStatBox(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimulationConfigCard(
    totalCores: Int,
    selectedDuration: Int,
    selectedIntensity: Int,
    selectedThreads: Int,
    onDurationChange: (Int) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onThreadsChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Simulation Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Configure workload intensity, active cores, and duration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Intensity Selector
            Text(
                text = "1. Workload Intensity (${selectedIntensity}%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    25 to "Light (25%)",
                    50 to "Daily (50%)",
                    75 to "Gaming (75%)",
                    100 to "Stress (100%)"
                ).forEach { (pct, label) ->
                    val isSelected = selectedIntensity == pct
                    Surface(
                        onClick = { onIntensityChange(pct) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Duration Selector
            Text(
                text = "2. Test Duration (${selectedDuration}s)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    5 to "5 Seconds",
                    10 to "10 Seconds",
                    20 to "20 Seconds",
                    30 to "30 Seconds"
                ).forEach { (sec, label) ->
                    val isSelected = selectedDuration == sec
                    Surface(
                        onClick = { onDurationChange(sec) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Threads / Core Allocation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3. Active Threads / Cores",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$selectedThreads / $totalCores Cores",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = selectedThreads.toFloat(),
                onValueChange = { onThreadsChange(it.toInt().coerceIn(1, totalCores)) },
                valueRange = 1f..totalCores.toFloat(),
                steps = if (totalCores > 2) totalCores - 2 else 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start Button
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Start", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start CPU Load Simulation", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

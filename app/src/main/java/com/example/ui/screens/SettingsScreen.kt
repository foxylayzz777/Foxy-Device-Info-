package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InfoRowItem
import com.example.ui.viewmodel.FoxyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FoxyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val metrics by viewModel.realtimeMetrics.collectAsState()
    val benchmarkResult by viewModel.benchmarkResult.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val benchmarkProgress by viewModel.benchmarkProgress.collectAsState()

    var showWifiAnalyzer by remember { mutableStateOf(false) }
    var showSensorExplorer by remember { mutableStateOf(false) }
    var showBenchmarkModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Benchmark Hero Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(
                                    text = "Hardware Benchmark",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Compute, RAM & Storage I/O test",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (benchmarkResult != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${benchmarkResult!!.totalBenchmarkScore} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (benchmarkResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Tier: ${benchmarkResult!!.tierLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Single-Core: ${benchmarkResult!!.cpuSingleCoreScore}", style = MaterialTheme.typography.bodySmall)
                            Text("Multi-Core: ${benchmarkResult!!.cpuMultiCoreScore}", style = MaterialTheme.typography.bodySmall)
                            Text("RAM: ${benchmarkResult!!.ramSpeedMbPerSec} MB/s", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Storage Read: ${benchmarkResult!!.storageReadMbPerSec} MB/s", style = MaterialTheme.typography.bodySmall)
                            Text("Storage Write: ${benchmarkResult!!.storageWriteMbPerSec} MB/s", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (isBenchmarking) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = benchmarkProgress.first,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { benchmarkProgress.second },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.runBenchmark() },
                        enabled = !isBenchmarking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Run")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBenchmarking) "Benchmarking in Progress..." else "Run Full Benchmark")
                    }
                }
            }
        }

        // Tools & Analyzers
        item {
            Text(
                text = "Diagnostics & Analyzers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Wi-Fi Analyzer Card
        item {
            val net = deviceInfo?.network
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWifiAnalyzer = !showWifiAnalyzer },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Wi-Fi",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Wi-Fi Analyzer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${net?.wifiSsid ?: "Connected"} • ${net?.wifiLinkSpeedMbps ?: 150} Mbps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { showWifiAnalyzer = !showWifiAnalyzer }) {
                            Icon(if (showWifiAnalyzer) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand")
                        }
                    }

                    AnimatedVisibility(visible = showWifiAnalyzer) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRowItem("SSID", net?.wifiSsid ?: "N/A")
                            InfoRowItem("BSSID / MAC", net?.wifiBssid ?: "--")
                            InfoRowItem("Signal RSSI", "${net?.wifiRssiDbm ?: -65} dBm")
                            InfoRowItem("Link Speed", "${net?.wifiLinkSpeedMbps ?: 150} Mbps")
                            InfoRowItem("Frequency", "${net?.wifiFrequencyMhz ?: 5180} MHz (${if (net?.is5GhzSupported == true) "5 GHz" else "2.4 GHz"})")
                            InfoRowItem("Wi-Fi 6 (802.11ax)", if (net?.isWifi6Supported == true) "Supported" else "Standard (Wi-Fi 5)")
                            InfoRowItem("IPv4 Address", net?.ipAddress ?: "192.168.1.100")
                            InfoRowItem("IPv6 Address", net?.ipv6Address ?: "::1")
                        }
                    }
                }
            }
        }

        // Sensor Explorer Card
        item {
            val sensors = deviceInfo?.sensors ?: emptyList()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSensorExplorer = !showSensorExplorer },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Sensors",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sensor Explorer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${sensors.size} Hardware Sensors Detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { showSensorExplorer = !showSensorExplorer }) {
                            Icon(if (showSensorExplorer) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand")
                        }
                    }

                    AnimatedVisibility(visible = showSensorExplorer) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            sensors.take(15).forEach { sensor ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sensor.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(sensor.vendor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                                        Text(
                                            text = "${sensor.powerMa} mA",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export Device Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Hardware Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Share complete specifications as Markdown / TXT report", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.exportReport(context) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export")
                    }
                }
            }
        }

        // Material You Home Screen Widget Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = "Widgets", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Material You Home Widgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add the Foxy Battery & Memory Glance widget to your home screen by long-pressing your desktop and selecting 'Foxy Info'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Privacy First Manifesto
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("Privacy-First Guarantee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Foxy Device Info operates 100% locally on your phone. No user accounts, zero analytics trackers, zero cloud telemetry. Your hardware data stays strictly yours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

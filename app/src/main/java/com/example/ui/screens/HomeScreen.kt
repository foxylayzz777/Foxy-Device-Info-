package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.data.model.TestType
import com.example.ui.components.FoxyHeaderBanner
import com.example.ui.components.InfoRowItem
import com.example.ui.components.MetricCard
import com.example.ui.viewmodel.FoxyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FoxyViewModel,
    onNavigateToTests: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val metrics by viewModel.realtimeMetrics.collectAsState()

    var isSpecsExpanded by remember { mutableStateOf(false) }

    val summary = deviceInfo?.summary
    val cpu = deviceInfo?.cpu
    val ramTotalGb = (metrics.ramTotalBytes / (1024.0 * 1024.0 * 1024.0))
    val ramUsedGb = (metrics.ramUsedBytes / (1024.0 * 1024.0 * 1024.0))
    val storageTotalGb = (metrics.storageTotalBytes / (1024.0 * 1024.0 * 1024.0))
    val storageUsedGb = (metrics.storageUsedBytes / (1024.0 * 1024.0 * 1024.0))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Cute Foxy Hero Banner
        item {
            FoxyHeaderBanner(
                deviceName = summary?.brand ?: "Foxy Device",
                deviceModel = summary?.model ?: "Android Device",
                batteryPct = metrics.batteryPercent,
                cpuUsage = metrics.cpuUsagePercent,
                tempC = metrics.batteryTempCelsius
            )
        }

        // 2. Quick Hardware Test Shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToTests) {
                            Text("All Tests")
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View all tests",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickTestButton(
                            title = "Display",
                            icon = Icons.Default.Tv,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.openInteractiveTest(TestType.DISPLAY)
                                onNavigateToTests()
                            }
                        )
                        QuickTestButton(
                            title = "Audio",
                            icon = Icons.Default.VolumeUp,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.openInteractiveTest(TestType.SPEAKER)
                                onNavigateToTests()
                            }
                        )
                        QuickTestButton(
                            title = "Touch",
                            icon = Icons.Default.TouchApp,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.openInteractiveTest(TestType.TOUCHSCREEN)
                                onNavigateToTests()
                            }
                        )
                        QuickTestButton(
                            title = "Torch",
                            icon = Icons.Default.FlashlightOn,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.openInteractiveTest(TestType.FLASHLIGHT)
                                onNavigateToTests()
                            }
                        )
                    }
                }
            }
        }

        // 3. Grid of Main Performance Cards
        item {
            Text(
                text = "Hardware Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // CPU & SoC Card
        item {
            MetricCard(
                title = "Processor & CPU",
                value = "${metrics.cpuUsagePercent.toInt()}% Load",
                subtitle = "${cpu?.socName ?: "Multi-core"} • ${cpu?.totalCores ?: 8} Cores",
                icon = Icons.Default.Memory,
                iconTint = MaterialTheme.colorScheme.primary,
                badgeText = "${metrics.cpuCoreFrequenciesMhz.firstOrNull() ?: 1800} MHz",
                progress = metrics.cpuUsagePercent / 100f,
                progressColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToMonitor
            )
        }

        // RAM Card
        item {
            MetricCard(
                title = "RAM Memory",
                value = String.format(Locale.US, "%.1f / %.1f GB", ramUsedGb, ramTotalGb),
                subtitle = "${(metrics.ramUsedPercent).toInt()}% utilized",
                icon = Icons.Default.Speed,
                iconTint = MaterialTheme.colorScheme.secondary,
                badgeText = String.format(Locale.US, "%.1f GB Free", (ramTotalGb - ramUsedGb).coerceAtLeast(0.0)),
                progress = metrics.ramUsedPercent / 100f,
                progressColor = MaterialTheme.colorScheme.secondary,
                onClick = onNavigateToMonitor
            )
        }

        // Storage Card
        item {
            MetricCard(
                title = "Internal Storage",
                value = String.format(Locale.US, "%.1f / %.1f GB", storageUsedGb, storageTotalGb),
                subtitle = "${(metrics.storageUsedPercent).toInt()}% used",
                icon = Icons.Default.Storage,
                iconTint = MaterialTheme.colorScheme.tertiary,
                badgeText = String.format(Locale.US, "%.1f GB Free", (storageTotalGb - storageUsedGb).coerceAtLeast(0.0)),
                progress = metrics.storageUsedPercent / 100f,
                progressColor = MaterialTheme.colorScheme.tertiary
            )
        }

        // Battery Card
        item {
            val batHealth = deviceInfo?.battery?.health ?: "Good"
            MetricCard(
                title = "Battery & Health",
                value = "${metrics.batteryPercent}% (${metrics.batteryStatus})",
                subtitle = "$batHealth • ${metrics.batteryVoltageMv} mV • ${metrics.batteryTempCelsius}°C",
                icon = Icons.Default.BatteryChargingFull,
                iconTint = if (metrics.batteryPercent > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                badgeText = "${metrics.batteryTempCelsius}°C",
                progress = metrics.batteryPercent / 100f,
                progressColor = if (metrics.batteryPercent > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        // Network Status Card
        item {
            val net = deviceInfo?.network
            MetricCard(
                title = "Network Connection",
                value = net?.networkType ?: "Connected",
                subtitle = "IP: ${net?.ipAddress ?: "127.0.0.1"} • ${net?.wifiSsid ?: "Online"}",
                icon = Icons.Default.Wifi,
                iconTint = MaterialTheme.colorScheme.tertiary,
                badgeText = "${net?.wifiLinkSpeedMbps ?: 150} Mbps"
            )
        }

        // Vulkan Graphics & VulkanMod Card
        item {
            val vulkan = deviceInfo?.vulkan
            MetricCard(
                title = "Vulkan & VulkanMod",
                value = vulkan?.apiVersionString ?: "Checking...",
                subtitle = vulkan?.vulkanModStatus ?: "Hardware 3D Graphics API",
                icon = Icons.Default.SportsEsports,
                iconTint = if (vulkan?.isVulkanModSupported == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                badgeText = if (vulkan?.isVulkanModSupported == true) "VulkanMod Ready 🎮" else "Check Info",
                onClick = {
                    viewModel.openInteractiveTest(TestType.VULKAN)
                    onNavigateToTests()
                }
            )
        }

        // Detailed Hardware Specs Accordion Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSpecsExpanded = !isSpecsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Specs",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Device System Specifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { isSpecsExpanded = !isSpecsExpanded }) {
                            Icon(
                                imageVector = if (isSpecsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle specs"
                            )
                        }
                    }

                    AnimatedVisibility(visible = isSpecsExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRowItem("Manufacturer", summary?.manufacturer ?: "Android")
                            InfoRowItem("Model", summary?.model ?: "Generic")
                            InfoRowItem("Codename", summary?.deviceName ?: "device")
                            InfoRowItem("Android OS", "Android ${summary?.androidVersion} (API ${summary?.apiLevel})")
                            InfoRowItem("Security Patch", summary?.securityPatch ?: "Current")
                            InfoRowItem("Kernel Version", summary?.kernelVersion ?: "Linux")
                            InfoRowItem("Display Resolution", deviceInfo?.gpuDisplay?.resolution ?: "1080x2400")
                            InfoRowItem("Refresh Rate", "${deviceInfo?.gpuDisplay?.refreshRateHz ?: 60f} Hz")
                            InfoRowItem("Screen Density", "${deviceInfo?.gpuDisplay?.densityDpi ?: 420} DPI")
                            InfoRowItem("Architecture", cpu?.architecture ?: "arm64-v8a")
                            InfoRowItem("Supported ABIs", cpu?.supportedAbis?.joinToString(", ") ?: "arm64-v8a")
                            InfoRowItem("Sensors Count", "${deviceInfo?.sensors?.size ?: 0} sensors")
                            InfoRowItem("NFC Available", if (deviceInfo?.capabilities?.hasNfc == true) "Yes" else "No")
                            InfoRowItem("Biometrics", if (deviceInfo?.capabilities?.hasFingerprint == true) "Fingerprint Supported" else "Standard")
                            InfoRowItem("Vulkan API", deviceInfo?.vulkan?.apiVersionString ?: "N/A")
                            InfoRowItem("VulkanMod Gaming", if (deviceInfo?.vulkan?.isVulkanModSupported == true) "Supported ⚡" else "Not Supported")

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.exportReport(context) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Export")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Full Device Report")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTestButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

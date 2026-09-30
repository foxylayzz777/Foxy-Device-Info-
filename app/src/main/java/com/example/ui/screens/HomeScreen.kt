package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
    var isVulkanExpanded by remember { mutableStateOf(false) }
    var isCpuExpanded by remember { mutableStateOf(false) }

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
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                        TextButton(
                            onClick = onNavigateToTests,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("All Tests", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
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

        // 3. Processor & SoC Full Architecture Card
        item {
            ProcessorCard(
                cpu = cpu,
                metrics = metrics,
                isExpanded = isCpuExpanded,
                onToggleExpand = { isCpuExpanded = !isCpuExpanded },
                onNavigateToMonitor = onNavigateToMonitor
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

        // Dedicated Vulkan API & VulkanMod Support Card
        item {
            val vulkan = deviceInfo?.vulkan
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isVulkanExpanded = !isVulkanExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "Vulkan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Vulkan API & VulkanMod",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = vulkan?.apiVersionString ?: "Checking hardware...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (vulkan?.isVulkanModSupported == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (vulkan?.isVulkanModSupported == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = if (vulkan?.isVulkanModSupported == true) "VulkanMod Ready ⚡" else "Check Info",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (vulkan?.isVulkanModSupported == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Quick Spec Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(vulkan?.apiVersionString ?: "N/A", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("HW Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Level ${vulkan?.hardwareLevel ?: 0}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Architecture", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (vulkan?.is64BitAbi == true) "64-Bit" else "32-Bit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    AnimatedVisibility(visible = isVulkanExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "VulkanMod Compatibility Status:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = vulkan?.vulkanModStatus ?: "Not Supported",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (vulkan?.isVulkanModSupported == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "PojavLauncher / VulkanMod Gaming Checklist:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            vulkan?.compatibilityDetails?.forEach { detail ->
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.openInteractiveTest(TestType.VULKAN)
                                    onNavigateToTests()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = "Test")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Vulkan Diagnostic Test")
                            }
                        }
                    }

                    if (!isVulkanExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isVulkanExpanded = true },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tap to view VulkanMod gaming checklist",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Detailed Hardware Specs Accordion Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                            InfoRowItem("Platform Processor", cpu?.socName ?: "Multi-Core")
                            InfoRowItem("Cluster Topology", cpu?.clustersDescription ?: "Octa-Core")
                            InfoRowItem("Architecture", cpu?.architecture ?: "arm64-v8a")
                            InfoRowItem("Microarchitecture", cpu?.coreMicroarchitecture ?: "ARMv8-A")
                            InfoRowItem("Max Clock Speed", "${cpu?.maxFreqMhz ?: 2400} MHz")
                            InfoRowItem("Supported ABIs", cpu?.supportedAbis?.joinToString(", ") ?: "arm64-v8a")
                            InfoRowItem("Process Technology", cpu?.processNodeEstimated ?: "Advanced FinFET")
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
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProcessorCard(
    cpu: com.example.data.model.CpuSpec?,
    metrics: com.example.data.model.RealtimeMetrics,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToMonitor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon, Chipset Name, Core Count & Real-time load
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Processor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = cpu?.socName ?: "Multi-core Processor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${cpu?.coreMicroarchitecture ?: "ARM Architecture"} • ${cpu?.totalCores ?: 8} Cores",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = "${metrics.cpuUsagePercent.toInt()}% Load",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Quick Hardware Summary Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Architecture", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (cpu?.is64Bit == true) "64-Bit" else "32-Bit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Max Clock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${cpu?.maxFreqMhz ?: 2400} MHz", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Governor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cpu?.governor ?: "schedutil", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Per-Core Frequencies Grid
            Text(
                text = "Live Core Frequencies (${metrics.cpuCoreFrequenciesMhz.size} Cores Active):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            val chunked = metrics.cpuCoreFrequenciesMhz.take(8).chunked(4)
            chunked.forEachIndexed { rowIndex, rowCores ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowCores.forEachIndexed { colIndex, freq ->
                        val coreIdx = rowIndex * 4 + colIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("C$coreIdx", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${freq}M", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Expandable Full Processor Specs & Instruction Sets
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Full Processor Hardware Specifications:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    InfoRowItem("SoC Platform", cpu?.socName ?: "Multi-Core")
                    InfoRowItem("CPU Implementer", cpu?.cpuImplementer ?: "ARM Limited (0x41)")
                    InfoRowItem("Cluster Topology", cpu?.clustersDescription ?: "Octa-Core Big.LITTLE")
                    InfoRowItem("Microarchitecture", cpu?.coreMicroarchitecture ?: "ARMv8-A")
                    InfoRowItem("CPU Part Register", cpu?.cpuPart ?: "Standard")
                    InfoRowItem("Frequency Range", "${cpu?.minFreqMhz ?: 300} MHz - ${cpu?.maxFreqMhz ?: 2400} MHz")
                    InfoRowItem("Scaling Governor", cpu?.governor ?: "schedutil")
                    InfoRowItem("Instruction Sets", cpu?.instructionSets ?: "arm64-v8a")
                    InfoRowItem("Supported ABIs", cpu?.supportedAbis?.joinToString(", ") ?: "arm64-v8a")
                    InfoRowItem("Process Technology", cpu?.processNodeEstimated ?: "Advanced FinFET")
                    InfoRowItem("BogoMIPS", cpu?.bogoMips ?: "N/A")
                    InfoRowItem("Cache Hierarchy", cpu?.cacheInfo ?: "L1/L2 Unified")
                    InfoRowItem("Platform Board", cpu?.hardwareBoard ?: "Hardware")

                    if (!cpu?.features.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Hardware Features & SIMD Instructions:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cpu?.features?.take(24)?.forEach { feat ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    Text(
                                        text = feat.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToMonitor,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Monitor", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Real-Time CPU Timeline Graph")
                    }
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to view full CPU registers, features & instructions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

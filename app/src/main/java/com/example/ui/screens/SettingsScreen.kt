package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DisplayRefreshRateHelper
import com.example.ui.components.InfoRowItem
import com.example.ui.components.RefreshRatePreference
import com.example.ui.components.rememberLiveFps
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemeStyle
import com.example.ui.viewmodel.FoxyViewModel
import com.example.widget.FoxyAppWidgetProvider
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FoxyViewModel,
    onOpenRefreshRateMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val metrics by viewModel.realtimeMetrics.collectAsState()
    val benchmarkResult by viewModel.benchmarkResult.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val benchmarkProgress by viewModel.benchmarkProgress.collectAsState()

    val currentThemeStyle by viewModel.themeStyle.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()

    val currentRefreshPref by viewModel.refreshRatePreference.collectAsState()
    val customDisplayModeId by viewModel.customDisplayModeId.collectAsState()
    val isFpsOverlayEnabled by viewModel.isFpsOverlayEnabled.collectAsState()
    val liveFps = rememberLiveFps()
    val activity = context as? android.app.Activity
    val displayHz = deviceInfo?.gpuDisplay?.refreshRateHz?.toInt() ?: 90

    var showWifiAnalyzer by remember { mutableStateOf(false) }
    var showSensorExplorer by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. YouTube Creator Showcase Card: FoxyPlayzZ
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E0A0D)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF0000).copy(alpha = 0.25f),
                                    Color(0xFFFF5252).copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF0000),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "YouTube",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "FoxyPlayzZ",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFF0000).copy(alpha = 0.85f)
                                        ) {
                                            Text(
                                                text = "CREATOR",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Created with ❤️ by FoxyPlayzZ on YouTube",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE0E0E0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Check out device tests, gaming benchmarks, and hardware reviews on the official channel!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCCCCCC)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val ytUrl = "https://youtube.com/@foxyplayzz?si=6Dpx1QA49S00zQhJ"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Subscriptions, contentDescription = "Subscribe")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visit FoxyPlayzZ on YouTube", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. New Themes Customizer (Gaming, Cyberpunk, Cute, Amoled, Fox, Material You)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "App Theme & Visual Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Theme Style",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Saved permanently",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Theme style chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppThemeStyle.entries) { style ->
                            val isSelected = currentThemeStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setThemeStyle(style) },
                                label = { Text("${style.badge} ${style.displayName}") },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Light / Dark / System Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = currentThemeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.displayName) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2.5. 90Hz & 90 FPS Display Mode Manager Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    Color(0xFF00E676).copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.45f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "90Hz & 90 FPS Display",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Target refresh rate & UI fluidity tuning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Live FPS Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), CircleShape))
                                Text(
                                    text = "${liveFps.toInt()} FPS • ${displayHz}Hz",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Refresh Rate Preference",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 Mode selection chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RefreshRatePreference.entries.forEach { pref ->
                            val isSelected = currentRefreshPref == pref && customDisplayModeId <= 0
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.setRefreshRatePreference(pref, null)
                                        DisplayRefreshRateHelper.applyPreference(activity, pref, null, showToast = true)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = pref.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (pref == RefreshRatePreference.FORCE_90) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF00E676).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "RECOMMENDED",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF00E676),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = pref.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = pref.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Floating FPS toggle switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Floating Realtime FPS Pill",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display live FPS overlay while using the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isFpsOverlayEnabled,
                            onCheckedChange = { viewModel.toggleFpsOverlay(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Open Full Menu Button
                    OutlinedButton(
                        onClick = onOpenRefreshRateMenu,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00E676)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Full 90Hz & 90 FPS Menu ⚙️",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Benchmark Hero Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                color = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Text(
                                    text = "${benchmarkResult!!.totalBenchmarkScore} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
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
                        Spacer(modifier = Modifier.height(10.dp))
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
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Single-Core", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${benchmarkResult!!.cpuSingleCoreScore}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Multi-Core", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${benchmarkResult!!.cpuMultiCoreScore}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("RAM Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${benchmarkResult!!.ramSpeedMbPerSec} M/s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Storage Read", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${benchmarkResult!!.storageReadMbPerSec} MB/s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Storage Write", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${benchmarkResult!!.storageWriteMbPerSec} MB/s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
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

        // 5. Diagnostics & Analyzers Header
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
                                Text("Wi-Fi Analyzer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                                Text("Sensor Explorer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                                        Text(sensor.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(sensor.vendor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        Text(
                                            text = "${sensor.powerMa} mA",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                        Text("Export Hardware Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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

        // Material You Home Screen Widget Studio & Customizer
        item {
            HomeScreenWidgetStudioCard(
                context = context,
                deviceInfo = deviceInfo
            )
        }

        // Privacy First & Privacy Policy Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                            Column {
                                Text(
                                    text = "Privacy-First Architecture",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Zero tracking • 100% on-device operation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Foxy Device Info is built with an absolute privacy-first pledge. No accounts, zero cloud telemetry, zero advertising identifiers, and no user data collection of any kind.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showPrivacyPolicyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = "Policy")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Read Complete Privacy Policy")
                    }
                }
            }
        }
    }

    // Comprehensive Privacy Policy Modal Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("Privacy Policy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    item {
                        Text(
                            text = "Foxy Device Info — Privacy & Data Protection Policy\nLast Updated: October 2026\nDeveloper: FoxyPlayzZ (YouTube: @foxyplayzz)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "1. Zero Personal Data Collection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Foxy Device Info does not collect, harvest, store, or transmit any personally identifiable information (PII), device serial numbers, location logs, contacts, photos, or browsing data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "2. Diagnostic Permissions Usage",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Camera & Flashlight: Accessed strictly when you initiate the Flashlight / Camera Specs diagnostic test. No photos or video frames are ever captured or saved.\n• Microphone (Audio Record): Temporarily sampled in volatile RAM solely during the live Microphone decibel meter test. Audio data is never recorded to disk or transmitted.\n• Bluetooth & Wi-Fi: Queried locally to report connection speed, frequency, and radio availability. No location tracking is performed.\n• Installed Applications: Scanned locally by PackageManager solely for the App & Permission Analyzer tool.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "3. 100% Offline & Local Execution",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All CPU, GPU, Vulkan, RAM, Storage, and battery telemetry is calculated directly on your processor. The application operates without requiring internet access or server connectivity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "4. Report Export Control",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Generated device specification reports remain on your device and are only shared if you explicitly initiate an Android system share action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "5. Third-Party Services & Ads",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "This application contains zero advertising networks, zero tracking SDKs, and zero telemetry analytics. Developed independently by FoxyPlayzZ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun HomeScreenWidgetStudioCard(
    context: Context,
    deviceInfo: com.example.data.model.FullDeviceInfo?
) {
    val prefs = remember { context.getSharedPreferences("foxy_device_info_prefs", Context.MODE_PRIVATE) }

    var widgetTheme by remember {
        mutableStateOf(prefs.getString("widget_theme_style", "LIQUID_GLASS") ?: "LIQUID_GLASS")
    }
    var titleMode by remember {
        mutableStateOf(prefs.getString("widget_title_mode", "SOC_NAME") ?: "SOC_NAME")
    }
    var customTitle by remember {
        mutableStateOf(prefs.getString("widget_custom_title", "") ?: "")
    }
    var showCpu by remember { mutableStateOf(prefs.getBoolean("widget_show_cpu", true)) }
    var showRam by remember { mutableStateOf(prefs.getBoolean("widget_show_ram", true)) }
    var showStorage by remember { mutableStateOf(prefs.getBoolean("widget_show_storage", true)) }
    var showBattery by remember { mutableStateOf(prefs.getBoolean("widget_show_battery", true)) }
    var showNetwork by remember { mutableStateOf(prefs.getBoolean("widget_show_network", true)) }
    var showDisplay by remember { mutableStateOf(prefs.getBoolean("widget_show_display", true)) }
    var showActions by remember { mutableStateOf(prefs.getBoolean("widget_show_actions", true)) }
    var showUptime by remember { mutableStateOf(prefs.getBoolean("widget_show_uptime", true)) }

    fun syncPreferences() {
        prefs.edit().apply {
            putString("widget_theme_style", widgetTheme)
            putString("widget_title_mode", titleMode)
            putString("widget_custom_title", customTitle)
            putBoolean("widget_show_cpu", showCpu)
            putBoolean("widget_show_ram", showRam)
            putBoolean("widget_show_storage", showStorage)
            putBoolean("widget_show_battery", showBattery)
            putBoolean("widget_show_network", showNetwork)
            putBoolean("widget_show_display", showDisplay)
            putBoolean("widget_show_actions", showActions)
            putBoolean("widget_show_uptime", showUptime)
            apply()
        }
        FoxyAppWidgetProvider.updateAllWidgets(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Widgets",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Home Screen Widget Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Live telemetry glance & customizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Live Sync ✓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Interactive Live Preview of Home Screen Widget
            Text(
                text = "Live Widget Preview (Home Screen Appearance):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preview Container Box matching selected theme
            val previewBgColor = when (widgetTheme) {
                "AMOLED" -> Color(0xFF000000)
                "FOXY" -> Color(0xFF16082B)
                "GAMING" -> Color(0xFF08150C)
                "CYBERPUNK" -> Color(0xFF150529)
                "RETRO_AMBER" -> Color(0xFF1A1308)
                "MINIMAL_FROST" -> Color(0xFF22242D)
                else -> Color(0xFF0A1830) // Liquid glass
            }
            val previewBorderColor = when (widgetTheme) {
                "AMOLED" -> Color(0xFF333333)
                "FOXY" -> Color(0xFF7C4DFF)
                "GAMING" -> Color(0xFF00E676)
                "CYBERPUNK" -> Color(0xFF00E5FF)
                "RETRO_AMBER" -> Color(0xFFFFB300)
                "MINIMAL_FROST" -> Color(0xFF888899)
                else -> Color(0xFF00E5FF)
            }
            val previewAccentColor = when (widgetTheme) {
                "AMOLED" -> Color(0xFFFFFFFF)
                "FOXY" -> Color(0xFFB388FF)
                "GAMING" -> Color(0xFF00E676)
                "CYBERPUNK" -> Color(0xFF00E5FF)
                "RETRO_AMBER" -> Color(0xFFFFD54F)
                "MINIMAL_FROST" -> Color(0xFFE0E0FF)
                else -> Color(0xFF00E5FF)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(previewBgColor)
                    .border(1.2.dp, previewBorderColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // Preview Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🦊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val soc = deviceInfo?.cpu?.socName ?: Build.HARDWARE
                            val previewTitle = when (titleMode) {
                                "CUSTOM" -> if (customTitle.isNotBlank()) customTitle else "My Device"
                                "DEVICE_MODEL" -> "${Build.MANUFACTURER} ${Build.MODEL}"
                                "APP_NAME" -> "Foxy Device Info"
                                else -> if (soc.isNotBlank() && soc != "unknown") soc else Build.MODEL
                            }
                            Text(
                                text = previewTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Android ${Build.VERSION.RELEASE} • 64-Bit",
                                style = MaterialTheme.typography.labelSmall,
                                color = previewAccentColor
                            )
                        }
                        Text(
                            text = "${deviceInfo?.battery?.levelPercent ?: 85}% ⚡",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text("🔄", fontSize = 14.sp)
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // CPU Row
                    if (showCpu) {
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚡ CPU Load", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0BEC5))
                                Text("28% • ${deviceInfo?.cpu?.totalCores ?: 8} Cores", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = previewAccentColor)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { 0.28f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = previewAccentColor,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // RAM Row
                    if (showRam) {
                        val ramTotalGb = (deviceInfo?.memoryStorage?.totalRamBytes ?: (8L * 1024 * 1024 * 1024)) / (1024.0 * 1024.0 * 1024.0)
                        val ramAvailGb = (deviceInfo?.memoryStorage?.availableRamBytes ?: (4L * 1024 * 1024 * 1024)) / (1024.0 * 1024.0 * 1024.0)
                        val ramUsedGb = ramTotalGb - ramAvailGb
                        val ramPct = if (ramTotalGb > 0) ((ramUsedGb / ramTotalGb) * 100).toInt() else 48

                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🧠 RAM Memory", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0BEC5))
                                Text(
                                    String.format(Locale.US, "%.1f / %.1f GB (%d%%)", ramUsedGb, ramTotalGb, ramPct),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB388FF)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { (ramPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFFB388FF),
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // Storage Row
                    if (showStorage) {
                        val totalBytes = deviceInfo?.memoryStorage?.totalStorageBytes ?: (256L * 1024 * 1024 * 1024)
                        val availBytes = deviceInfo?.memoryStorage?.availableStorageBytes ?: (192L * 1024 * 1024 * 1024)
                        val usedBytes = totalBytes - availBytes
                        val storageTotalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
                        val storageUsedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
                        val storagePct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 25

                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("💾 Internal Storage", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0BEC5))
                                Text(
                                    String.format(Locale.US, "%.1f / %.1f GB (%d%%)", storageUsedGb, storageTotalGb, storagePct),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80CBC4)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { (storagePct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF80CBC4),
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // Battery & Uptime Row
                    if (showBattery || showUptime) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = if (showNetwork || showDisplay || showActions) 6.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showBattery) {
                                Text(
                                    text = "🌡️ Battery: ${deviceInfo?.battery?.temperatureCelsius ?: 31}°C • Good • 4120mV",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                            if (showUptime) {
                                Text(
                                    text = "Up: 24h 12m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }
                    }

                    // Network Row
                    if (showNetwork) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = if (showDisplay || showActions) 6.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📶 Network", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0BEC5))
                            Text("Wi-Fi Connected 🛜 • 5 GHz", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4DD0E1))
                        }
                    }

                    // Display Row
                    if (showDisplay) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = if (showActions) 8.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖥️ Display", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0BEC5))
                            Text("${deviceInfo?.gpuDisplay?.refreshRateHz?.toInt() ?: 120}Hz • ${deviceInfo?.gpuDisplay?.resolution ?: "1080×2400"}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                        }
                    }

                    // Quick Actions Row
                    if (showActions) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("🚀 Monitor", "🛠️ Tests", "📱 Apps", "⚙️ Customize").forEach { label ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Widget Visual Theme Selector
            Text(
                text = "Widget Visual Theme Style (7 Styles):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf(
                    "LIQUID_GLASS" to "🫧 Liquid Glass",
                    "AMOLED" to "🖤 AMOLED Dark",
                    "FOXY" to "🦊 Foxy Violet",
                    "GAMING" to "🎮 RGB Gaming",
                    "CYBERPUNK" to "⚡ Cyber Neon",
                    "RETRO_AMBER" to "🏆 Retro Gold",
                    "MINIMAL_FROST" to "❄️ Minimal Frost"
                )) { (themeId, label) ->
                    val isSelected = widgetTheme == themeId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            widgetTheme = themeId
                            syncPreferences()
                        },
                        label = { Text(label) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Header Title Mode
            Text(
                text = "Header Chipset / Title Label:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "SOC_NAME" to "⚡ SoC",
                    "DEVICE_MODEL" to "📱 Model",
                    "APP_NAME" to "🦊 App",
                    "CUSTOM" to "✏️ Custom"
                ).forEach { (modeId, label) ->
                    val isSelected = titleMode == modeId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            titleMode = modeId
                            syncPreferences()
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (titleMode == "CUSTOM") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = {
                        customTitle = it
                        syncPreferences()
                    },
                    label = { Text("Custom Widget Title / Nickname") },
                    placeholder = { Text("e.g. Foxy Phone, Beast Edition 🦊") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Metric Display Toggles
            Text(
                text = "Hardware Metrics & Panels to Display in Widget:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WidgetToggleRow(
                    title = "CPU Load & Core Count",
                    subtitle = "Real-time CPU percentage & mini progress bar",
                    checked = showCpu,
                    onCheckedChange = {
                        showCpu = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "RAM Memory Usage",
                    subtitle = "Active used RAM, total capacity & progress bar",
                    checked = showRam,
                    onCheckedChange = {
                        showRam = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "Internal Storage (ROM)",
                    subtitle = "Disk capacity, used GB & storage bar",
                    checked = showStorage,
                    onCheckedChange = {
                        showStorage = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "Battery & Thermal State",
                    subtitle = "Charge %, charging lightning badge, voltage & temperature",
                    checked = showBattery,
                    onCheckedChange = {
                        showBattery = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "Network Connectivity",
                    subtitle = "Wi-Fi SSID, cellular state & connection badge",
                    checked = showNetwork,
                    onCheckedChange = {
                        showNetwork = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "Display & Refresh Rate",
                    subtitle = "Screen Hz rate and native panel resolution",
                    checked = showDisplay,
                    onCheckedChange = {
                        showDisplay = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "Quick Navigation Actions",
                    subtitle = "Shortcut pills for Monitor, Tests, Apps & Customize",
                    checked = showActions,
                    onCheckedChange = {
                        showActions = it
                        syncPreferences()
                    }
                )
                WidgetToggleRow(
                    title = "System Uptime",
                    subtitle = "Elapsed operating time since boot",
                    checked = showUptime,
                    onCheckedChange = {
                        showUptime = it
                        syncPreferences()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Actions: Add to Home Screen & Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        syncPreferences()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                            val myProvider = ComponentName(context, FoxyAppWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                Toast.makeText(context, "Adding widget to home screen...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Long-press home screen desktop and select 'Foxy Hardware' widget", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Long-press home screen desktop and select 'Foxy Hardware' widget", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddHome, contentDescription = "Add Widget", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin to Home Screen")
                }

                OutlinedButton(
                    onClick = {
                        syncPreferences()
                        Toast.makeText(context, "Widgets refreshed with custom settings! ✓", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync")
                }
            }
        }
    }
}

@Composable
private fun WidgetToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


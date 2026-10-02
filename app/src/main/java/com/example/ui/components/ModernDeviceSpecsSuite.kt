package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FullDeviceInfo
import java.util.Locale

data class SpecEntry(
    val category: String,
    val title: String,
    val value: String,
    val icon: ImageVector,
    val tag: String? = null,
    val isMono: Boolean = false,
    val isHighlighted: Boolean = false,
    val tintColor: Color? = null,
    val progressFraction: Float? = null
)

enum class SpecCategoryTab(val label: String, val categoryName: String, val icon: ImageVector) {
    ALL("All", "All", Icons.Default.Dashboard),
    SYSTEM("System & OS", "System & OS", Icons.Default.Android),
    CPU("CPU & SoC", "CPU & SoC", Icons.Default.Memory),
    DISPLAY("Display & GPU", "Display & GPU", Icons.Default.Tv),
    MEMORY("RAM & Storage", "Memory & Storage", Icons.Default.Storage),
    BATTERY("Battery & Power", "Battery & Power", Icons.Default.BatteryChargingFull),
    CONNECTIVITY("Network & Wireless", "Network & Wireless", Icons.Default.Wifi),
    SECURITY("Security & DRM", "Security & DRM", Icons.Default.Security),
    AUDIO("Audio & Media", "Audio & Media", Icons.Default.GraphicEq),
    GNSS("GNSS & Location", "GNSS & Location", Icons.Default.Navigation),
    CAMERA("Camera Matrix", "Camera System", Icons.Default.PhotoCamera),
    SENSORS("Sensors", "Hardware Sensors", Icons.Default.Sensors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDeviceSpecsSuite(
    deviceInfo: FullDeviceInfo?,
    onExportReport: () -> Unit,
    onOpenRefreshRateMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(SpecCategoryTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var copiedLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedLabel) {
        if (copiedLabel != null) {
            kotlinx.coroutines.delay(2200)
            copiedLabel = null
        }
    }

    val allSpecs = remember(deviceInfo) {
        buildAllSpecsList(deviceInfo)
    }

    val categoryCounts = remember(allSpecs) {
        val map = mutableMapOf<String, Int>()
        allSpecs.forEach { spec ->
            map[spec.category] = (map[spec.category] ?: 0) + 1
        }
        map
    }

    val filteredSpecs = remember(allSpecs, selectedCategory, searchQuery) {
        allSpecs.filter { entry ->
            val matchesCategory = if (selectedCategory == SpecCategoryTab.ALL) {
                true
            } else {
                entry.category == selectedCategory.categoryName
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.title.contains(searchQuery, ignoreCase = true) ||
                        entry.value.contains(searchQuery, ignoreCase = true) ||
                        entry.category.contains(searchQuery, ignoreCase = true) ||
                        (entry.tag?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesCategory && matchesSearch
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Device System Specifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "${allSpecs.size} Specs",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isExpanded) "Tap to collapse catalog" else "Tap to expand hardware & OS catalog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .testTag("toggle_specs_accordion")
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Specifications" else "Expand Specifications",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Collapse" else "Expand",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(animationSpec = spring()),
                exit = fadeOut() + shrinkVertically(animationSpec = spring())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Quick Stat Highlights Bento Tiles (2x2 adaptive grid)
                    DeviceHighlightsBentoGrid(deviceInfo = deviceInfo)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("specs_search_input"),
                        placeholder = { Text("Search specifications (e.g. Widevine, Vulkan, RAM, 64-bit, 5G)...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        )
                    )

                    // Quick suggestion chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Widevine", "64-bit", "RAM", "Vulkan", "Battery", "Kernel", "Camera", "5G", "Audio", "GNSS").forEach { chip ->
                            val isSelected = searchQuery.equals(chip, ignoreCase = true)
                            SuggestionChip(
                                onClick = {
                                    searchQuery = if (isSelected) "" else chip
                                },
                                label = { Text(chip, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Horizontal Category Tabs with Count Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecCategoryTab.entries.forEach { tab ->
                            val isSelected = selectedCategory == tab
                            val count = if (tab == SpecCategoryTab.ALL) allSpecs.size else (categoryCounts[tab.categoryName] ?: 0)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = tab },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(tab.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Copied toast banner
                    AnimatedVisibility(visible = copiedLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Copied to clipboard: $copiedLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }

                    // Specifications List
                    if (filteredSpecs.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No specifications match '$searchQuery'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredSpecs.forEach { spec ->
                                ModernSpecRow(
                                    spec = spec,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString("${spec.title}: ${spec.value}"))
                                        copiedLabel = spec.title
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportReport,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_specs_report_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Report", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val allText = filteredSpecs.joinToString("\n") { "${it.category} > ${it.title}: ${it.value}" }
                                clipboardManager.setText(AnnotatedString(allText))
                                Toast.makeText(context, "Copied ${filteredSpecs.size} specifications to clipboard! 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copy_all_specs_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy All", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = onOpenRefreshRateMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("specs_90hz_menu_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF00E676).copy(alpha = 0.16f),
                            contentColor = Color(0xFF00E676)
                        )
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "90 FPS Menu", tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("90Hz & 90 FPS Fluidity Display Menu ⚡", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceHighlightsBentoGrid(deviceInfo: FullDeviceInfo?) {
    val summary = deviceInfo?.summary
    val cpu = deviceInfo?.cpu
    val gpuDisplay = deviceInfo?.gpuDisplay
    val drm = deviceInfo?.drmSecurity
    val battery = deviceInfo?.battery
    val mem = deviceInfo?.memoryStorage

    val ramUsedPct = if (mem != null && mem.totalRamBytes > 0) {
        ((mem.totalRamBytes - mem.availableRamBytes).toFloat() / mem.totalRamBytes).coerceIn(0f, 1f)
    } else 0.45f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Processor + Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoMiniTile(
                title = "Processor SoC",
                value = cpu?.socName?.take(18) ?: "Multi-Core",
                sub = "${cpu?.processNodeEstimated ?: "4nm FinFET"} • ${if (cpu?.is64Bit == true) "64-bit" else "32-bit"}",
                icon = Icons.Default.Memory,
                tint = Color(0xFF00E5FF),
                progress = null,
                modifier = Modifier.weight(1f)
            )

            BentoMiniTile(
                title = "Display & DRM",
                value = "${gpuDisplay?.refreshRateHz?.toInt() ?: 90}Hz Fluidity",
                sub = "${gpuDisplay?.resolution?.take(14) ?: "Full HD+"} • ${drm?.widevineSecurityLevel?.take(2) ?: "L1"}",
                icon = Icons.Default.Tv,
                tint = Color(0xFF00E676),
                progress = null,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: RAM Gauge + Battery & Power
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalRamGb = if (mem != null) String.format(Locale.US, "%.1f GB", mem.totalRamBytes / (1024.0 * 1024.0 * 1024.0)) else "8.0 GB"
            val availRamGb = if (mem != null) String.format(Locale.US, "%.1f GB", mem.availableRamBytes / (1024.0 * 1024.0 * 1024.0)) else "3.8 GB"

            BentoMiniTile(
                title = "Unified RAM",
                value = "$availRamGb Free",
                sub = "of $totalRamGb (${(ramUsedPct * 100).toInt()}% used)",
                icon = Icons.Default.Storage,
                tint = Color(0xFFFFB300),
                progress = ramUsedPct,
                modifier = Modifier.weight(1f)
            )

            val batPct = battery?.levelPercent ?: 88
            val batStatus = if (battery?.isCharging == true) "Charging ⚡" else "On Battery"

            BentoMiniTile(
                title = "Battery & Health",
                value = "$batPct% $batStatus",
                sub = "${battery?.temperatureCelsius?.toInt() ?: 29}°C • ${battery?.health ?: "Good"}",
                icon = Icons.Default.BatteryChargingFull,
                tint = Color(0xFFB388FF),
                progress = (batPct / 100f).coerceIn(0f, 1f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BentoMiniTile(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    tint: Color,
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tint.copy(alpha = 0.18f),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progress != null) {
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = tint,
                    trackColor = tint.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ModernSpecRow(
    spec: SpecEntry,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCopy() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
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
                        shape = RoundedCornerShape(10.dp),
                        color = (spec.tintColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                tint = spec.tintColor ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = spec.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (spec.tag != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = (spec.tintColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = spec.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = spec.tintColor ?: MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = spec.value,
                            style = if (spec.isMono) {
                                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (spec.isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (spec.isHighlighted) (spec.tintColor ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy ${spec.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (spec.progressFraction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { spec.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = spec.tintColor ?: MaterialTheme.colorScheme.primary,
                    trackColor = (spec.tintColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                )
            }
        }
    }
}

private fun buildAllSpecsList(deviceInfo: FullDeviceInfo?): List<SpecEntry> {
    if (deviceInfo == null) return emptyList()
    val summary = deviceInfo.summary
    val cpu = deviceInfo.cpu
    val gpu = deviceInfo.gpuDisplay
    val mem = deviceInfo.memoryStorage
    val battery = deviceInfo.battery
    val net = deviceInfo.network
    val vulkan = deviceInfo.vulkan
    val drm = deviceInfo.drmSecurity
    val audio = deviceInfo.audioMedia
    val gnss = deviceInfo.gnssLocation
    val caps = deviceInfo.capabilities

    val list = mutableListOf<SpecEntry>()

    // 1. System & OS
    list.add(SpecEntry("System & OS", "Device Model", "${summary.brand} ${summary.model}", Icons.Default.PhoneAndroid, "Model", isHighlighted = true))
    list.add(SpecEntry("System & OS", "Manufacturer", summary.manufacturer, Icons.Default.Business))
    list.add(SpecEntry("System & OS", "Codename / Device", summary.deviceName, Icons.Default.DeveloperMode, "Codename"))
    list.add(SpecEntry("System & OS", "Product Name", summary.product, Icons.Default.Inventory))
    list.add(SpecEntry("System & OS", "Board & Hardware", "${summary.board} / ${summary.hardware}", Icons.Default.Memory))
    list.add(SpecEntry("System & OS", "Android Version", "Android ${summary.androidVersion}", Icons.Default.Android, "v${summary.androidVersion}", isHighlighted = true, tintColor = Color(0xFF00E676)))
    list.add(SpecEntry("System & OS", "Release Codename", summary.androidCodename, Icons.Default.AutoAwesome))
    list.add(SpecEntry("System & OS", "API Level", "API ${summary.apiLevel}", Icons.Default.Code, "SDK ${summary.apiLevel}"))
    list.add(SpecEntry("System & OS", "Security Patch", summary.securityPatch, Icons.Default.VerifiedUser, "Patch", isHighlighted = true))
    list.add(SpecEntry("System & OS", "Kernel Version", summary.kernelVersion, Icons.Default.Terminal, "Kernel", isMono = true))
    list.add(SpecEntry("System & OS", "Build ID", summary.buildId, Icons.Default.Numbers, isMono = true))
    list.add(SpecEntry("System & OS", "Build Type / Tags", "${summary.buildType} (${summary.buildTags})", Icons.Default.Label))
    list.add(SpecEntry("System & OS", "Build Fingerprint", summary.buildFingerprint.ifEmpty { "Official Release Build" }, Icons.Default.Fingerprint, isMono = true))
    list.add(SpecEntry("System & OS", "Baseband / Radio", summary.radioVersion, Icons.Default.CellTower))
    list.add(SpecEntry("System & OS", "Bootloader", summary.bootloaderVersion, Icons.Default.PowerSettingsNew))
    list.add(SpecEntry("System & OS", "Java VM Runtime", "${summary.javaVmVersion} (Ahead-Of-Time / JIT Compiler)", Icons.Default.Coffee))
    list.add(SpecEntry("System & OS", "SELinux Status", summary.selinuxStatus, Icons.Default.Shield, if (summary.selinuxStatus.contains("Enforcing", ignoreCase = true)) "Secure" else "Permissive", isHighlighted = true, tintColor = Color(0xFF00E676)))
    list.add(SpecEntry("System & OS", "Root Access", if (summary.isRooted) "Rooted / Test-Keys" else "No (Official Unrooted)", Icons.Default.AdminPanelSettings, if (summary.isRooted) "Rooted" else "Secure"))
    list.add(SpecEntry("System & OS", "Project Treble", if (summary.isTrebleSupported) "Supported (Full Treble)" else "Legacy", Icons.Default.Extension))
    list.add(SpecEntry("System & OS", "Seamless A/B Updates", if (summary.isSeamlessUpdateSupported) "Supported (Dual Partition)" else "Standard", Icons.Default.SystemUpdate))
    val uptimeHours = summary.uptimeMillis / 3_600_000L
    val uptimeMinutes = (summary.uptimeMillis % 3_600_000L) / 60_000L
    list.add(SpecEntry("System & OS", "System Uptime", "${uptimeHours}h ${uptimeMinutes}m", Icons.Default.Timer))

    // 2. CPU & SoC
    list.add(SpecEntry("CPU & SoC", "Platform SoC", cpu.socName, Icons.Default.Memory, "Chipset", isHighlighted = true, tintColor = Color(0xFF00E5FF)))
    list.add(SpecEntry("CPU & SoC", "Process Technology", cpu.processNodeEstimated, Icons.Default.PrecisionManufacturing, "Node", tintColor = Color(0xFF00E5FF)))
    list.add(SpecEntry("CPU & SoC", "CPU Cores", "${cpu.totalCores} Processors", Icons.Default.Memory, "${cpu.totalCores} Cores"))
    list.add(SpecEntry("CPU & SoC", "Cluster Layout", cpu.clustersDescription, Icons.Default.Hub))
    list.add(SpecEntry("CPU & SoC", "Microarchitecture", cpu.coreMicroarchitecture, Icons.Default.Architecture))
    list.add(SpecEntry("CPU & SoC", "Core Frequency Range", "${cpu.minFreqMhz} MHz - ${cpu.maxFreqMhz} MHz", Icons.Default.Speed, isMono = true))
    list.add(SpecEntry("CPU & SoC", "Scaling Governor", cpu.governor, Icons.Default.Tune))
    list.add(SpecEntry("CPU & SoC", "Instruction Sets / ABI", cpu.supportedAbis.joinToString(", "), Icons.Default.Code, if (cpu.is64Bit) "64-bit" else "32-bit", isHighlighted = true))
    list.add(SpecEntry("CPU & SoC", "CPU Implementer", cpu.cpuImplementer, Icons.Default.CorporateFare))
    list.add(SpecEntry("CPU & SoC", "CPU Part Number", cpu.cpuPart, Icons.Default.Tag, isMono = true))
    list.add(SpecEntry("CPU & SoC", "BogoMIPS", cpu.bogoMips, Icons.Default.Speed, isMono = true))
    list.add(SpecEntry("CPU & SoC", "Cache Hierarchy", cpu.cacheInfo, Icons.Default.Storage))
    list.add(SpecEntry("CPU & SoC", "Sustained Performance", if (caps.hasSustainedPerformance) "Supported (Thermal Governor)" else "Standard", Icons.Default.Bolt))
    if (cpu.features.isNotEmpty()) {
        list.add(SpecEntry("CPU & SoC", "SIMD & Hardware Flags", cpu.features.take(12).joinToString(" ").uppercase(), Icons.Default.CheckCircle, "SIMD", isMono = true))
    }

    // 3. Display & GPU
    list.add(SpecEntry("Display & GPU", "Screen Resolution", gpu.resolution, Icons.Default.Tv, "Resolution", isHighlighted = true, tintColor = Color(0xFFB388FF)))
    list.add(SpecEntry("Display & GPU", "Aspect Ratio", gpu.aspectRatio, Icons.Default.AspectRatio, "Ratio"))
    list.add(SpecEntry("Display & GPU", "Refresh Rate", "${gpu.refreshRateHz} Hz", Icons.Default.Speed, "${gpu.refreshRateHz.toInt()}Hz", isHighlighted = true, tintColor = Color(0xFF00E676)))
    list.add(SpecEntry("Display & GPU", "Supported Modes", gpu.supportedRefreshRates.joinToString(", ") { "${it.toInt()}Hz" }, Icons.Default.Tune))
    list.add(SpecEntry("Display & GPU", "Screen Density", "${gpu.densityDpi} DPI (${gpu.densityBucket})", Icons.Default.DensityMedium))
    list.add(SpecEntry("Display & GPU", "Exact DPI", "X: ${gpu.xdpi} dpi • Y: ${gpu.ydpi} dpi", Icons.Default.Grid4x4, isMono = true))
    list.add(SpecEntry("Display & GPU", "Diagonal Physical Size", "~${gpu.screenPhysicalInches}", Icons.Default.Straighten))
    list.add(SpecEntry("Display & GPU", "HDR Capabilities", if (gpu.isHdrSupported) gpu.hdrCapabilities else "SDR Only", Icons.Default.HdrOn, if (gpu.isHdrSupported) "HDR" else "SDR"))
    list.add(SpecEntry("Display & GPU", "Wide Color Gamut", if (gpu.isWideColorGamutSupported) "Supported (DCI-P3)" else "Standard sRGB", Icons.Default.Palette))
    list.add(SpecEntry("Display & GPU", "GPU Renderer", gpu.renderer, Icons.Default.VideogameAsset, "Renderer", isHighlighted = true, tintColor = Color(0xFFB388FF)))
    list.add(SpecEntry("Display & GPU", "GPU Vendor", gpu.vendor, Icons.Default.CorporateFare))
    list.add(SpecEntry("Display & GPU", "Vulkan API Version", vulkan.apiVersionString, Icons.Default.SportsEsports, if (vulkan.isVulkanSupported) "Vulkan Ready" else "No Vulkan", isHighlighted = vulkan.isVulkanSupported, tintColor = Color(0xFFFF5252)))
    list.add(SpecEntry("Display & GPU", "Vulkan Hardware Level", "Level ${vulkan.hardwareLevel}", Icons.Default.Speed))
    list.add(SpecEntry("Display & GPU", "Vulkan Acceleration", vulkan.vulkanDriverStatus, Icons.Default.Bolt))
    list.add(SpecEntry("Display & GPU", "Multi-touch Contacts", "${caps.multiTouchPoints} Simultaneous Touch Points", Icons.Default.TouchApp, "${caps.multiTouchPoints} Fingers"))

    // 4. Memory & Storage
    val ramTotalGb = String.format(Locale.US, "%.2f GB", mem.totalRamBytes / (1024.0 * 1024.0 * 1024.0))
    val ramAvailGb = String.format(Locale.US, "%.2f GB", mem.availableRamBytes / (1024.0 * 1024.0 * 1024.0))
    val ramUsedFraction = if (mem.totalRamBytes > 0) ((mem.totalRamBytes - mem.availableRamBytes).toFloat() / mem.totalRamBytes).coerceIn(0f, 1f) else 0.5f
    val storageTotalGb = String.format(Locale.US, "%.1f GB", mem.totalStorageBytes / (1024.0 * 1024.0 * 1024.0))
    val storageAvailGb = String.format(Locale.US, "%.1f GB", mem.availableStorageBytes / (1024.0 * 1024.0 * 1024.0))
    val storageUsedFraction = if (mem.totalStorageBytes > 0) ((mem.totalStorageBytes - mem.availableStorageBytes).toFloat() / mem.totalStorageBytes).coerceIn(0f, 1f) else 0.5f

    list.add(SpecEntry("Memory & Storage", "Installed RAM", ramTotalGb, Icons.Default.Speed, "RAM", isHighlighted = true, tintColor = Color(0xFFFFAB40), progressFraction = ramUsedFraction))
    list.add(SpecEntry("Memory & Storage", "Available Free RAM", ramAvailGb, Icons.Default.Check, "Free", tintColor = Color(0xFF00E676)))
    list.add(SpecEntry("Memory & Storage", "RAM Generation", mem.ramTypeEstimated, Icons.Default.Memory))
    if (mem.zramSizeBytes > 0) {
        val zramMb = mem.zramSizeBytes / (1024 * 1024)
        list.add(SpecEntry("Memory & Storage", "ZRAM Swap Compression", "$zramMb MB ZRAM Swap", Icons.Default.Compress))
    }
    list.add(SpecEntry("Memory & Storage", "Low RAM Device Flag", if (mem.ramLowMemory) "Yes (Android Go Edition)" else "No (Full Android)", Icons.Default.Info))
    list.add(SpecEntry("Memory & Storage", "Total Internal Storage", storageTotalGb, Icons.Default.Storage, "Storage", isHighlighted = true, progressFraction = storageUsedFraction))
    list.add(SpecEntry("Memory & Storage", "Available Free Storage", storageAvailGb, Icons.Default.CloudDone, "Free", tintColor = Color(0xFF00E676)))
    list.add(SpecEntry("Memory & Storage", "Storage Filesystem", mem.filesystemType, Icons.Default.FolderSpecial, isMono = true))

    // 5. Battery & Power
    val batFrac = (battery.levelPercent / 100f).coerceIn(0f, 1f)
    list.add(SpecEntry("Battery & Power", "Battery Level", "${battery.levelPercent}%", Icons.Default.BatteryChargingFull, "${battery.levelPercent}%", isHighlighted = true, tintColor = Color(0xFFFFD700), progressFraction = batFrac))
    list.add(SpecEntry("Battery & Power", "Battery Status", if (battery.isCharging) "Charging (${battery.chargingSource})" else "On Battery", Icons.Default.Power))
    list.add(SpecEntry("Battery & Power", "Fast Charging", battery.fastChargingStatus, Icons.Default.Bolt, "Power", tintColor = Color(0xFFFFD700)))
    list.add(SpecEntry("Battery & Power", "Battery Health", battery.health, Icons.Default.Favorite, "Health", isHighlighted = true))
    list.add(SpecEntry("Battery & Power", "Temperature", "${battery.temperatureCelsius}°C (${(battery.temperatureCelsius * 9 / 5 + 32).toInt()}°F)", Icons.Default.Thermostat))
    list.add(SpecEntry("Battery & Power", "Voltage", "${battery.voltageMv} mV (${String.format(Locale.US, "%.2f", battery.voltageMv / 1000f)} V)", Icons.Default.ElectricBolt))
    list.add(SpecEntry("Battery & Power", "Technology", battery.technology, Icons.Default.Science))
    list.add(SpecEntry("Battery & Power", "Design Capacity", "~${battery.capacityMah.toInt()} mAh", Icons.Default.BatterySaver, "${battery.capacityMah.toInt()} mAh"))

    // 6. Security & DRM
    list.add(SpecEntry("Security & DRM", "Widevine DRM Level", drm.widevineSecurityLevel, Icons.Default.Security, if (drm.widevineSecurityLevel.contains("L1")) "Widevine L1" else "Widevine L3", isHighlighted = true, tintColor = Color(0xFF00E676)))
    list.add(SpecEntry("Security & DRM", "Widevine Vendor / Version", "${drm.widevineVendor} (v${drm.widevineVersion})", Icons.Default.Verified))
    list.add(SpecEntry("Security & DRM", "Device Encryption", drm.deviceEncryptionStatus, Icons.Default.Lock, "Encrypted", isHighlighted = true))
    list.add(SpecEntry("Security & DRM", "Hardware Keymaster", if (drm.strongBoxAvailable) "Hardware StrongBox Keymaster" else "Standard TEE Keymaster", Icons.Default.Key))
    list.add(SpecEntry("Security & DRM", "Biometric Authentication", drm.biometricHardware, Icons.Default.Fingerprint, "Biometrics"))
    list.add(SpecEntry("Security & DRM", "Hardware Face Auth", if (caps.hasFaceAuth) "Supported (Biometric Class 3)" else "Not Available", Icons.Default.Face))

    // 7. Network & Wireless
    list.add(SpecEntry("Network & Wireless", "Connection Status", if (net.isConnected) "Online (${net.networkType})" else "Disconnected", Icons.Default.Wifi, if (net.isConnected) "Connected" else "Offline", isHighlighted = true, tintColor = Color(0xFF40C4FF)))
    list.add(SpecEntry("Network & Wireless", "Local IPv4", net.ipAddress, Icons.Default.Language, isMono = true))
    if (net.ipv6Address.isNotEmpty()) {
        list.add(SpecEntry("Network & Wireless", "IPv6 Address", net.ipv6Address, Icons.Default.Public, isMono = true))
    }
    if (net.wifiSsid.isNotEmpty()) {
        list.add(SpecEntry("Network & Wireless", "Wi-Fi Network SSID", net.wifiSsid, Icons.Default.WifiTethering))
        list.add(SpecEntry("Network & Wireless", "Wi-Fi Signal Strength", "${net.wifiRssiDbm} dBm", Icons.Default.SignalCellularAlt))
        list.add(SpecEntry("Network & Wireless", "Link Speed & Frequency", "${net.wifiLinkSpeedMbps} Mbps • ${net.wifiFrequencyMhz} MHz", Icons.Default.Speed))
    }
    list.add(SpecEntry("Network & Wireless", "Wi-Fi 6 Protocol", if (net.isWifi6Supported) "Supported (802.11ax)" else "Wi-Fi 5 / Standard", Icons.Default.Wifi))
    list.add(SpecEntry("Network & Wireless", "5 GHz Dual-Band Wi-Fi", if (net.is5GhzSupported) "Supported" else "2.4 GHz Only", Icons.Default.NetworkCheck))
    list.add(SpecEntry("Network & Wireless", "Wi-Fi Direct (P2P)", if (caps.hasWifiDirect) "Supported" else "Not Supported", Icons.Default.SwapCalls))
    list.add(SpecEntry("Network & Wireless", "Wi-Fi Aware (NAN)", if (caps.hasWifiAware) "Supported" else "Not Supported", Icons.Default.Sensors))
    list.add(SpecEntry("Network & Wireless", "Cellular Radio Telephony", if (caps.has5gTelephony) "5G NR / 4G LTE Advanced" else "Standard Telephony", Icons.Default.CellTower))
    list.add(SpecEntry("Network & Wireless", "eSIM Capability", if (caps.hasEsim) "Supported (eUICC Hardware)" else "Physical SIM", Icons.Default.SimCard))
    list.add(SpecEntry("Network & Wireless", "NFC", if (caps.hasNfc) "Supported (Contactless & Beam)" else "Not Available", Icons.Default.Nfc))
    list.add(SpecEntry("Network & Wireless", "Bluetooth & BLE", if (caps.hasBluetoothLe) "Bluetooth 5.0+ LE Supported" else "Standard Bluetooth", Icons.Default.Bluetooth))
    list.add(SpecEntry("Network & Wireless", "Ultra-Wideband (UWB)", if (caps.hasUwb) "Supported (Fine Ranging)" else "Not Supported", Icons.Default.Radar))
    list.add(SpecEntry("Network & Wireless", "USB OTG / Host", if (caps.hasUsbHost) "Supported (High-Speed Host)" else "Standard", Icons.Default.Usb))

    // 8. Audio & Media
    list.add(SpecEntry("Audio & Media", "Audio Hardware Outputs", audio.audioOutputs, Icons.Default.VolumeUp, isHighlighted = true))
    list.add(SpecEntry("Audio & Media", "Spatial Audio", if (audio.spatialAudioSupported) "Supported (Dynamic Head Tracking)" else "Stereo Virtualized", Icons.Default.SurroundSound))
    list.add(SpecEntry("Audio & Media", "Hi-Res Audio Engine", if (audio.hiResAudioSupported) "Supported (24-bit / 192kHz DAC)" else "Standard", Icons.Default.GraphicEq))
    list.add(SpecEntry("Audio & Media", "Low Latency Audio", if (caps.hasLowLatencyAudio) "Supported (AAudio Pro Engine)" else "Standard Latency", Icons.Default.Speed))
    list.add(SpecEntry("Audio & Media", "Hardware MIDI", if (caps.hasMidi) "Supported (MIDI 2.0 / USB)" else "Not Supported", Icons.Default.Piano))
    list.add(SpecEntry("Audio & Media", "Hardware Video Decoders", audio.supportedVideoDecoders.joinToString(", "), Icons.Default.Movie, isMono = true))
    list.add(SpecEntry("Audio & Media", "Hardware Video Encoders", audio.supportedVideoEncoders.joinToString(", "), Icons.Default.VideoCall, isMono = true))

    // 9. GNSS & Location
    list.add(SpecEntry("GNSS & Location", "GPS Receiver", if (gnss.hasGps) "Active (Hardware GNSS Chipset)" else "Not Available", Icons.Default.GpsFixed, if (gnss.hasGps) "Ready" else "No GPS", isHighlighted = true, tintColor = Color(0xFF00E5FF)))
    list.add(SpecEntry("GNSS & Location", "Constellations", gnss.constellations.joinToString(", "), Icons.Default.SatelliteAlt))
    list.add(SpecEntry("GNSS & Location", "Dual-Frequency GNSS", if (gnss.hasDualFrequency) "Supported (L1 + L5 Carrier Phases)" else "Single Band (L1)", Icons.Default.Hub))
    list.add(SpecEntry("GNSS & Location", "Raw GNSS Measurements", if (gnss.hasGnssMeasurements) "Supported (Carrier Phase & Doppler)" else "Standard", Icons.Default.Timeline))
    list.add(SpecEntry("GNSS & Location", "Location Providers", gnss.supportedProviders, Icons.Default.LocationOn))
    list.add(SpecEntry("GNSS & Location", "Hardware Geofencing", if (gnss.hasGeofencing) "Supported (Low-Power Fused)" else "Software", Icons.Default.NearMe))

    // 10. Camera System
    list.add(SpecEntry("Camera System", "Total Cameras", "${deviceInfo.cameras.size} Sensor Lenses Detected", Icons.Default.PhotoCamera, "${deviceInfo.cameras.size} Lenses", isHighlighted = true, tintColor = Color(0xFFFF5252)))
    deviceInfo.cameras.forEach { cam ->
        list.add(SpecEntry("Camera System", "Camera ${cam.cameraId} (${cam.facing})", "${cam.resolutionMegapixels} MP (${cam.sensorSize}) • Zoom ${cam.maxZoom}x • Flash: ${if (cam.flashAvailable) "Yes" else "No"}", Icons.Default.CameraAlt, "${cam.resolutionMegapixels} MP"))
    }

    // 11. Hardware Sensors
    list.add(SpecEntry("Hardware Sensors", "Total Sensors", "${deviceInfo.sensors.size} Hardware Sensors Active", Icons.Default.Sensors, "${deviceInfo.sensors.size} Sensors", isHighlighted = true, tintColor = Color(0xFF69F0AE)))
    deviceInfo.sensors.take(15).forEach { sensor ->
        list.add(SpecEntry("Hardware Sensors", sensor.name, "Vendor: ${sensor.vendor} • Power: ${sensor.powerMa}mA • Range: ${sensor.maxRange}", Icons.Default.Adjust))
    }

    return list
}

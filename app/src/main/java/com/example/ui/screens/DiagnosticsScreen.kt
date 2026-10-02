package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.DiagnosticTestItem
import com.example.data.model.TestStatus
import com.example.data.model.TestType
import com.example.ui.viewmodel.FoxyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: FoxyViewModel,
    modifier: Modifier = Modifier
) {
    val tests by viewModel.diagnosticTests.collectAsState()
    val activeTest by viewModel.activeInteractiveTest.collectAsState()

    val passedCount = tests.count { it.status == TestStatus.PASSED }
    val failedCount = tests.count { it.status == TestStatus.FAILED }
    val pendingCount = tests.count { it.status == TestStatus.NOT_RUN }
    val totalSupported = tests.count { it.status != TestStatus.NOT_SUPPORTED }
    val testProgress = if (totalSupported > 0) passedCount.toFloat() / totalSupported else 0f

    var selectedFilter by remember { mutableStateOf("All") }

    val displayedTests = remember(tests, selectedFilter) {
        when (selectedFilter) {
            "Pass", "Passed" -> tests.filter { it.status == TestStatus.PASSED }
            "Failed" -> tests.filter { it.status == TestStatus.FAILED }
            "Pending" -> tests.filter { it.status == TestStatus.NOT_RUN }
            else -> tests
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Modern Hardware Health Overview Bento Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Bar with Pulsing Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (failedCount > 0) Color(0xFFFF5252) else Color(0xFF00E676)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM DIAGNOSTICS SUITE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (failedCount > 0) MaterialTheme.colorScheme.errorContainer
                            else if (passedCount == totalSupported && totalSupported > 0) Color(0xFF00E676).copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (failedCount > 0) MaterialTheme.colorScheme.onErrorContainer
                            else if (passedCount == totalSupported && totalSupported > 0) Color(0xFF00E676)
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = if (failedCount > 0) "$failedCount Issues Found"
                                else if (passedCount == totalSupported && totalSupported > 0) "100% Verified ✓"
                                else "${(testProgress * 100).toInt()}% Health",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title & Quick Counter Summary
                    Text(
                        text = "Hardware Diagnostic Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$passedCount passed • $failedCount failed • $pendingCount remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Modern Multi-Segment Diagnostic Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        val passWeight = if (totalSupported > 0) passedCount.toFloat() / totalSupported else 0f
                        val failWeight = if (totalSupported > 0) failedCount.toFloat() / totalSupported else 0f

                        Row(modifier = Modifier.fillMaxSize()) {
                            if (passWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(passWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFF00E676))
                                )
                            }
                            if (failWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(failWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF5252))
                                )
                            }
                            val remainWeight = 1f - (passWeight + failWeight)
                            if (remainWeight > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .weight(remainWeight.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(Color.Transparent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val nextTest = tests.firstOrNull { it.status == TestStatus.NOT_RUN }
                                    ?: tests.firstOrNull()
                                if (nextTest != null) {
                                    viewModel.openInteractiveTest(nextTest.type)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pendingCount > 0) "Run Pending ($pendingCount)" else "Run All Tests",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetAllTests() },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }

        // 2. Modern 4-Tab Filter Bento Grid (All, Pending, Pass, Failed)
        item {
            ModernTestFilterTabs(
                selectedFilter = selectedFilter,
                allCount = tests.size,
                pendingCount = pendingCount,
                passedCount = passedCount,
                failedCount = failedCount,
                onFilterSelected = { selectedFilter = it }
            )
        }

        // 3. Test items list or Empty state
        if (displayedTests.isEmpty()) {
            item {
                ModernDiagnosticEmptyState(
                    currentFilter = selectedFilter,
                    onResetFilter = { selectedFilter = "All" },
                    onRunPending = {
                        val nextTest = tests.firstOrNull { it.status == TestStatus.NOT_RUN }
                            ?: tests.firstOrNull()
                        if (nextTest != null) {
                            viewModel.openInteractiveTest(nextTest.type)
                        }
                    }
                )
            }
        } else {
            items(displayedTests, key = { it.type.name }) { testItem ->
                DiagnosticTestCard(
                    item = testItem,
                    onTestClick = { viewModel.openInteractiveTest(testItem.type) }
                )
            }
        }
    }

    // Interactive Test Dialog Router
    if (activeTest != null) {
        val currentIndex = tests.indexOfFirst { it.type == activeTest }
        val testNumber = if (currentIndex >= 0) currentIndex + 1 else 1

        key(activeTest) {
            InteractiveTestModal(
                testType = activeTest!!,
                testIndex = testNumber,
                totalTests = tests.size,
                viewModel = viewModel,
                onDismiss = { viewModel.closeInteractiveTest() },
                onNextTest = {
                    val nextTest = tests.drop(currentIndex + 1).firstOrNull { it.status == TestStatus.NOT_RUN }
                        ?: tests.firstOrNull { it.status == TestStatus.NOT_RUN }
                    if (nextTest != null) {
                        viewModel.openInteractiveTest(nextTest.type)
                    } else {
                        viewModel.closeInteractiveTest()
                    }
                }
            )
        }
    }
}

// Modern 4-Tab Filter Bento Grid (All, Pending, Pass, Failed)
@Composable
fun ModernTestFilterTabs(
    selectedFilter: String,
    allCount: Int,
    pendingCount: Int,
    passedCount: Int,
    failedCount: Int,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        FilterTabItem("All", allCount, Icons.Default.Layers, MaterialTheme.colorScheme.primary),
        FilterTabItem("Pending", pendingCount, Icons.Default.HourglassTop, Color(0xFFFFB300)),
        FilterTabItem("Pass", passedCount, Icons.Default.CheckCircle, Color(0xFF00E676)),
        FilterTabItem("Failed", failedCount, Icons.Default.Cancel, Color(0xFFFF5252))
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedFilter == tab.name || (tab.name == "Pass" && selectedFilter == "Passed")
            val containerColor = if (isSelected) {
                tab.accentColor.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
            val borderColor = if (isSelected) {
                tab.accentColor
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            }

            Surface(
                onClick = { onFilterSelected(tab.name) },
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.name,
                            tint = if (isSelected) tab.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${tab.count}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) tab.accentColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class FilterTabItem(
    val name: String,
    val count: Int,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun ModernDiagnosticEmptyState(
    currentFilter: String,
    onResetFilter: () -> Unit,
    onRunPending: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val (icon, title, desc, accent) = when (currentFilter) {
                "Failed" -> Quadruple(
                    Icons.Default.VerifiedUser,
                    "Zero Hardware Failures",
                    "All tested sensors, audio devices, and display systems passed with zero defects detected.",
                    Color(0xFF00E676)
                )
                "Pending" -> Quadruple(
                    Icons.Default.EmojiEvents,
                    "All Diagnostics Complete!",
                    "Every hardware component has been tested. Check the Pass tab to review results.",
                    Color(0xFFFFB300)
                )
                "Pass" -> Quadruple(
                    Icons.Default.PlayCircle,
                    "No Tests Passed Yet",
                    "Begin interactive diagnostics to verify display, touchscreen, audio, and sensors.",
                    MaterialTheme.colorScheme.primary
                )
                else -> Quadruple(
                    Icons.Default.FactCheck,
                    "No Diagnostic Tests Found",
                    "Hardware tests could not be loaded.",
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentFilter == "Failed" || currentFilter == "Pending") {
                    Button(
                        onClick = onResetFilter,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View All Tests", fontWeight = FontWeight.Bold)
                    }
                } else if (currentFilter == "Pass") {
                    Button(
                        onClick = onRunPending,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Tests", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun DiagnosticTestCard(
    item: DiagnosticTestItem,
    onTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.status != TestStatus.NOT_SUPPORTED) { onTestClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.status == TestStatus.PASSED) Color(0xFF00E676).copy(alpha = 0.3f)
            else if (item.status == TestStatus.FAILED) Color(0xFFFF5252).copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Category Pill & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = getTestCategory(item.type),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                TestStatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Icon + Title & Description + Quick Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(getTestIconBgColor(item.status)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTestIcon(item.type),
                            contentDescription = item.title,
                            tint = getTestIconColor(item.status),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.details != null && item.details.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.status == TestStatus.PASSED) Color(0xFF00E676)
                                else if (item.status == TestStatus.FAILED) Color(0xFFFF5252)
                                else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Instant Test Action Button
                if (item.status != TestStatus.NOT_SUPPORTED) {
                    Surface(
                        onClick = onTestClick,
                        shape = RoundedCornerShape(12.dp),
                        color = when (item.status) {
                            TestStatus.NOT_RUN -> MaterialTheme.colorScheme.primary
                            TestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            TestStatus.PASSED -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = when (item.status) {
                            TestStatus.NOT_RUN -> MaterialTheme.colorScheme.onPrimary
                            TestStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.PASSED -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.status == TestStatus.NOT_RUN) Icons.Default.PlayArrow else Icons.Default.Refresh,
                                contentDescription = "Test action",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (item.status == TestStatus.NOT_RUN) "Test" else "Retest",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getTestCategory(type: TestType): String {
    return when (type) {
        TestType.DISPLAY, TestType.TOUCHSCREEN -> "DISPLAY & TOUCH"
        TestType.SPEAKER, TestType.EARPIECE, TestType.MICROPHONE, TestType.HEADSET, TestType.VIBRATION -> "AUDIO & HAPTICS"
        TestType.FLASHLIGHT, TestType.PROXIMITY, TestType.LIGHT_SENSOR, TestType.ACCELEROMETER, TestType.GYROSCOPE, TestType.COMPASS -> "SENSORS & MOTION"
        TestType.FINGERPRINT, TestType.VOLUME_BUTTONS, TestType.BLUETOOTH, TestType.CHARGING -> "PORTS & HARDWARE"
        TestType.VULKAN -> "GRAPHICS & GPU"
    }
}

@Composable
fun TestStatusBadge(status: TestStatus) {
    val (bgColor, textColor, label, icon) = when (status) {
        TestStatus.PASSED -> Tuple4(
            Color(0xFF00E676).copy(alpha = 0.18f),
            Color(0xFF00E676),
            "Pass ✓",
            Icons.Default.CheckCircle
        )
        TestStatus.FAILED -> Tuple4(
            Color(0xFFFF5252).copy(alpha = 0.18f),
            Color(0xFFFF5252),
            "Failed ✕",
            Icons.Default.Cancel
        )
        TestStatus.NOT_SUPPORTED -> Tuple4(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            "N/A",
            Icons.Default.Block
        )
        TestStatus.RUNNING -> Tuple4(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Testing",
            Icons.Default.Sync
        )
        TestStatus.NOT_RUN -> Tuple4(
            Color(0xFFFFB300).copy(alpha = 0.15f),
            Color(0xFFFFB300),
            "Pending",
            Icons.Default.HourglassTop
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun FullScreenInteractiveTest(
    testType: TestType,
    testIndex: Int,
    totalTests: Int,
    viewModel: FoxyViewModel,
    onDismiss: () -> Unit,
    onNextTest: () -> Unit,
    onPrevTest: (() -> Unit)? = null
) {
    when (testType) {
        TestType.DISPLAY -> {
            FullScreenDisplayTest(
                testIndex = testIndex,
                totalTests = totalTests,
                onDismiss = onDismiss,
                onMarkPassed = {
                    viewModel.setTestStatus(testType, TestStatus.PASSED)
                    onNextTest()
                },
                onMarkFailed = {
                    viewModel.setTestStatus(testType, TestStatus.FAILED)
                    onNextTest()
                },
                onNextTest = onNextTest
            )
        }
        TestType.TOUCHSCREEN -> {
            FullScreenTouchscreenTest(
                testIndex = testIndex,
                totalTests = totalTests,
                onDismiss = onDismiss,
                onMarkPassed = {
                    viewModel.setTestStatus(testType, TestStatus.PASSED)
                    onNextTest()
                },
                onMarkFailed = {
                    viewModel.setTestStatus(testType, TestStatus.FAILED)
                    onNextTest()
                },
                onNextTest = onNextTest
            )
        }
        else -> {
            FullScreenStandardHardwareTest(
                testType = testType,
                testIndex = testIndex,
                totalTests = totalTests,
                viewModel = viewModel,
                onDismiss = onDismiss,
                onMarkPassed = {
                    viewModel.setTestStatus(testType, TestStatus.PASSED)
                    onNextTest()
                },
                onMarkFailed = {
                    viewModel.setTestStatus(testType, TestStatus.FAILED)
                    onNextTest()
                },
                onNextTest = onNextTest
            )
        }
    }
}

@Composable
fun InteractiveTestModal(
    testType: TestType,
    testIndex: Int,
    totalTests: Int,
    viewModel: FoxyViewModel,
    onDismiss: () -> Unit,
    onNextTest: () -> Unit
) {
    FullScreenInteractiveTest(
        testType = testType,
        testIndex = testIndex,
        totalTests = totalTests,
        viewModel = viewModel,
        onDismiss = onDismiss,
        onNextTest = onNextTest
    )
}

@Composable
private fun FullScreenDisplayTest(
    testIndex: Int,
    totalTests: Int,
    onDismiss: () -> Unit,
    onMarkPassed: () -> Unit,
    onMarkFailed: () -> Unit,
    onNextTest: () -> Unit
) {
    val colors = remember {
        listOf(
            Triple(Color(0xFFFF0000), "Pure Red", "Check subpixels & dead pixel matrix"),
            Triple(Color(0xFF00FF00), "Pure Green", "Check subpixels & green tint"),
            Triple(Color(0xFF0000FF), "Pure Blue", "Check subpixels & blue aging"),
            Triple(Color(0xFFFFFFFF), "Pure White", "Check backlight uniformity & tint"),
            Triple(Color(0xFF000000), "Pure Black", "Check OLED true black & edge bleed"),
            Triple(Color(0xFFFFFF00), "Pure Yellow", "Check RG subpixel balance"),
            Triple(Color(0xFF00FFFF), "Pure Cyan", "Check GB subpixel balance"),
            Triple(Color(0xFFFF00FF), "Pure Magenta", "Check RB subpixel balance"),
            Triple(Color(0xFF808080), "50% Neutral Gray", "Check display banding & DSE"),
            Triple(Color(0xFF333333), "20% Dark Gray", "Check near-black OLED crush")
        )
    }
    var colorIndex by remember { mutableIntStateOf(0) }
    var showOverlayControls by remember { mutableStateOf(true) }

    val (currentColor, name, desc) = colors[colorIndex]

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        colorIndex = (colorIndex + 1) % colors.size
                    },
                    onDoubleTap = {
                        showOverlayControls = !showOverlayControls
                    }
                )
            }
    ) {
        // Floating Top Header
        AnimatedVisibility(
            visible = showOverlayControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("test_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Display Test",
                            tint = Color.White
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${colorIndex + 1}/${colors.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { showOverlayControls = false },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_immersion")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide controls for immersion",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Floating Bottom Actions
        AnimatedVisibility(
            visible = showOverlayControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkFailed,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFF5252).copy(alpha = 0.2f),
                            contentColor = Color(0xFFFF5252)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("mark_failed_button")
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Failed", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onMarkPassed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("mark_passed_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Passed", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onNextTest,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("next_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Test",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Tap hint when controls hidden
        if (!showOverlayControls) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Tap to cycle • Double-tap for controls",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FullScreenTouchscreenTest(
    testIndex: Int,
    totalTests: Int,
    onDismiss: () -> Unit,
    onMarkPassed: () -> Unit,
    onMarkFailed: () -> Unit,
    onNextTest: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    var touchCount by remember { mutableIntStateOf(0) }
    var lastTouchPos by remember { mutableStateOf<Offset?>(null) }
    val touchedCells = remember { mutableStateMapOf<Int, Boolean>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val numCols = (screenWidth / 46.dp).toInt().coerceIn(6, 16)
        val numRows = (screenHeight / 46.dp).toInt().coerceIn(8, 28)
        val totalCells = numCols * numRows

        val touchedCount = touchedCells.size
        val coveragePct = if (totalCells > 0) (touchedCount.toFloat() / totalCells * 100).toInt() else 0

        // Full Screen Gesture Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastTouchPos = offset
                            touchCount++
                            val col = (offset.x / (size.width / numCols)).toInt().coerceIn(0, numCols - 1)
                            val row = (offset.y / (size.height / numRows)).toInt().coerceIn(0, numRows - 1)
                            touchedCells[row * numCols + col] = true
                        },
                        onDrag = { change, _ ->
                            lastTouchPos = change.position
                            val col = (change.position.x / (size.width / numCols)).toInt().coerceIn(0, numCols - 1)
                            val row = (change.position.y / (size.height / numRows)).toInt().coerceIn(0, numRows - 1)
                            touchedCells[row * numCols + col] = true
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cW = w / numCols
                val cH = h / numRows

                for (r in 0 until numRows) {
                    for (c in 0 until numCols) {
                        val idx = r * numCols + c
                        val isTouched = touchedCells[idx] == true
                        val left = c * cW
                        val top = r * cH

                        if (isTouched) {
                            drawRect(
                                color = Color(0xFF00E676).copy(alpha = 0.65f),
                                topLeft = Offset(left, top),
                                size = Size(cW, cH)
                            )
                        }

                        drawRect(
                            color = Color(0xFF30363D),
                            topLeft = Offset(left, top),
                            size = Size(cW, cH),
                            style = Stroke(width = 1f)
                        )
                    }
                }

                lastTouchPos?.let { pos ->
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = 28f,
                        center = pos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = pos
                    )
                }
            }

            if (touchedCells.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "👆 Slide Finger Across Entire Screen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cover every grid block to test digitizer touch matrix",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Top Floating Info Bar
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("test_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Touchscreen Test",
                        tint = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Touch Digitizer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = lastTouchPos?.let { "(${it.x.toInt()}, ${it.y.toInt()}) px" } ?: "Touch anywhere",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (coveragePct >= 75) Color(0xFF00E676) else Color(0xFF388E3C)
                ) {
                    Text(
                        text = "$coveragePct% ($touchedCount/$totalCells)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (coveragePct >= 75) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                TextButton(
                    onClick = {
                        touchedCells.clear()
                        lastTouchPos = null
                    },
                    modifier = Modifier.testTag("clear_touch_grid"),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Text("Clear")
                }
            }
        }

        // Bottom Floating Action Bar
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkFailed,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFF5252).copy(alpha = 0.2f),
                        contentColor = Color(0xFFFF5252)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("mark_failed_button")
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Failed", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onMarkPassed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("mark_passed_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Passed", fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onNextTest,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("next_test_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Test",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenStandardHardwareTest(
    testType: TestType,
    testIndex: Int,
    totalTests: Int,
    viewModel: FoxyViewModel,
    onDismiss: () -> Unit,
    onMarkPassed: () -> Unit,
    onMarkFailed: () -> Unit,
    onNextTest: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var volUpTriggered by remember { mutableStateOf(false) }
    var volDownTriggered by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(testType) {
        if (testType == TestType.VOLUME_BUTTONS) {
            delay(120)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (testType == TestType.VOLUME_BUTTONS && keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            volUpTriggered = true
                            true
                        }
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            volDownTriggered = true
                            true
                        }
                        else -> false
                    }
                } else false
            },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getTestIcon(testType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = getTestTitle(testType),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Test $testIndex of $totalTests • ${getTestCategory(testType)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("test_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onMarkFailed,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mark_failed_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Fail", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Failed", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onMarkPassed,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mark_passed_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Pass", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Passed", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onNextTest,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("next_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Test",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (testType) {
                        TestType.DISPLAY -> DisplayTestCanvas()
                        TestType.TOUCHSCREEN -> TouchscreenCalibrationGrid()
                        TestType.SPEAKER -> SpeakerToneTest(viewModel, isEarpiece = false)
                        TestType.EARPIECE -> SpeakerToneTest(viewModel, isEarpiece = true)
                        TestType.MICROPHONE -> MicrophoneDecibelTest(viewModel)
                        TestType.VIBRATION -> VibrationInteractiveTest(viewModel)
                        TestType.FLASHLIGHT -> FlashlightInteractiveTest(viewModel)
                        TestType.PROXIMITY -> ProximityInteractiveTest(context)
                        TestType.LIGHT_SENSOR -> LightSensorInteractiveTest(context)
                        TestType.ACCELEROMETER -> AccelerometerLevelTest(context)
                        TestType.GYROSCOPE -> GyroscopeInteractiveTest(context)
                        TestType.COMPASS -> CompassInteractiveTest(context)
                        TestType.FINGERPRINT -> FingerprintInteractiveTest(context)
                        TestType.VOLUME_BUTTONS -> VolumeButtonsInteractiveTest(
                            volUpPressed = volUpTriggered,
                            volDownPressed = volDownTriggered,
                            onVolUpClick = { volUpTriggered = true },
                            onVolDownClick = { volDownTriggered = true }
                        )
                        TestType.BLUETOOTH -> BluetoothCheckTest(context)
                        TestType.CHARGING -> ChargingCheckTest(context)
                        TestType.HEADSET -> HeadsetCheckTest(context)
                        TestType.VULKAN -> VulkanDiagnosticTest(viewModel)
                    }
                }
            }
        }
    }
}

// 1. Display Test: Pure RGBW & CMYK Full Block Cycling
@Composable
private fun DisplayTestCanvas() {
    val colors = listOf(
        Color.Red to "Pure Red (Check subpixels)",
        Color.Green to "Pure Green (Check subpixels)",
        Color.Blue to "Pure Blue (Check subpixels)",
        Color.White to "Pure White (Check backlight uniformity)",
        Color.Black to "Pure Black (Check light bleed / OLED off)",
        Color.Yellow to "Yellow",
        Color.Cyan to "Cyan",
        Color.Magenta to "Magenta",
        Color(0xFF888888) to "50% Gray Banding"
    )
    var colorIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (currentColor, desc) = colors[colorIndex]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(currentColor)
                .clickable { colorIndex = (colorIndex + 1) % colors.size },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap screen to cycle color (${colorIndex + 1}/${colors.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// 2. Touchscreen & Digitizer Calibration Grid
@Composable
private fun TouchscreenCalibrationGrid() {
    val numCols = 6
    val numRows = 8
    val totalCells = numCols * numRows
    val touchedCells = remember { mutableStateMapOf<Int, Boolean>() }
    var touchCount by remember { mutableIntStateOf(0) }
    var lastTouchPos by remember { mutableStateOf<Offset?>(null) }

    val touchedCount = touchedCells.size
    val coveragePct = (touchedCount.toFloat() / totalCells * 100).toInt()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Coverage: $coveragePct% ($touchedCount / $totalCells cells)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (coveragePct >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = {
                    touchedCells.clear()
                    touchCount = 0
                    lastTouchPos = null
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Clear Grid", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastTouchPos = offset
                            touchCount++
                            val col = (offset.x / (size.width / numCols)).toInt().coerceIn(0, numCols - 1)
                            val row = (offset.y / (size.height / numRows)).toInt().coerceIn(0, numRows - 1)
                            touchedCells[row * numCols + col] = true
                        },
                        onDrag = { change, _ ->
                            lastTouchPos = change.position
                            val col = (change.position.x / (size.width / numCols)).toInt().coerceIn(0, numCols - 1)
                            val row = (change.position.y / (size.height / numRows)).toInt().coerceIn(0, numRows - 1)
                            touchedCells[row * numCols + col] = true
                        }
                    )
                }
        ) {
            val cellW = maxWidth / numCols
            val cellH = maxHeight / numRows
            val primaryColor = MaterialTheme.colorScheme.primary
            val gridBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cW = w / numCols
                val cH = h / numRows

                for (r in 0 until numRows) {
                    for (c in 0 until numCols) {
                        val idx = r * numCols + c
                        val isTouched = touchedCells[idx] == true
                        val left = c * cW
                        val top = r * cH

                        if (isTouched) {
                            drawRect(
                                color = primaryColor.copy(alpha = 0.45f),
                                topLeft = Offset(left, top),
                                size = Size(cW, cH)
                            )
                        }

                        drawRect(
                            color = gridBorder,
                            topLeft = Offset(left, top),
                            size = Size(cW, cH),
                            style = Stroke(width = 1f)
                        )
                    }
                }

                lastTouchPos?.let { pos ->
                    drawCircle(
                        color = primaryColor,
                        radius = 24f,
                        center = pos
                    )
                }
            }

            if (touchedCells.isEmpty()) {
                Text(
                    text = "Slide your finger across all grid blocks! 👆",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        if (coveragePct >= 70) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = "✓ Touch Response Verified ($coveragePct%)! Ready to Pass.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Text(
                text = lastTouchPos?.let { "Coordinate: (${it.x.toInt()}px, ${it.y.toInt()}px)" } ?: "Touch anywhere to test digitizer",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 3 & 4. Speaker & Earpiece Audio Tone Test
@Composable
private fun SpeakerToneTest(viewModel: FoxyViewModel, isEarpiece: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var selectedFreq by remember { mutableIntStateOf(if (isEarpiece) 600 else 440) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isEarpiece) Icons.Default.PhoneInTalk else Icons.Default.VolumeUp,
                contentDescription = "Speaker",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isEarpiece) "Front Call Earpiece Test" else "Main Loudspeaker Test",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isEarpiece) "Hold front earpiece to ear and test voice audio channel" else "Listen for clear harmonic acoustic sine wave",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Frequency Selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(220 to "Low (220Hz)", 440 to "Standard (440Hz)", 880 to "High (880Hz)").forEach { (freq, label) ->
                FilterChip(
                    selected = selectedFreq == freq,
                    onClick = { selectedFreq = freq },
                    label = { Text(label) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isPlaying = true
                    viewModel.playAudioTone(frequency = selectedFreq, durationMs = 1200, isEarpiece = isEarpiece)
                    isPlaying = false
                }
            },
            enabled = !isPlaying,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isPlaying) "Playing $selectedFreq Hz..." else "Play Tone")
        }
    }
}

// 5. Authentic Real Microphone Decibel Sampler
@Composable
private fun MicrophoneDecibelTest(viewModel: FoxyViewModel) {
    val context = LocalContext.current
    var decibel by remember { mutableFloatStateOf(35f) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val listener = viewModel.startMicrophoneListener { db ->
                decibel = db
            }
            onDispose {
                try {
                    listener?.close()
                } catch (_: Exception) {}
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Mic",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!hasPermission) {
            Text(
                text = "Microphone Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Grant audio recording permission to measure live microphone acoustics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Microphone Access")
            }
        } else {
            Text(
                text = "Speak into microphone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${decibel.toInt()} dB SPL",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    decibel < 45 -> "Quiet Ambient Noise"
                    decibel < 65 -> "Normal Speech Detected ✓"
                    decibel < 80 -> "Loud Voice Detected ✓"
                    else -> "High Sound Pressure Level"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { ((decibel - 30f) / 70f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (decibel >= 55f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// 6. Vibration Interactive Test with Patterns
@Composable
private fun VibrationInteractiveTest(viewModel: FoxyViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = "Vibration",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Haptic Vibration Motor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tap to trigger tactile haptic vibration patterns",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { viewModel.triggerVibration() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Bolt, contentDescription = "Vibrate", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Trigger Vibration Pulse")
        }
    }
}

// 7. Flashlight / LED Torch Test
@Composable
private fun FlashlightInteractiveTest(viewModel: FoxyViewModel) {
    var isOn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.toggleTorch(false)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isOn) Color(0xFFFFB300).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashlightOn,
                contentDescription = "Torch",
                modifier = Modifier.size(38.dp),
                tint = if (isOn) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Camera LED Flashlight",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isOn) "Torch is active • Tap to turn off" else "Tap below to turn rear LED flash on",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        FilledTonalButton(
            onClick = {
                val newState = !isOn
                isOn = newState
                viewModel.toggleTorch(newState)
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isOn) "Turn Torch OFF" else "Turn Torch ON")
        }
    }
}

// 8. Real Proximity Sensor Test
@Composable
private fun ProximityInteractiveTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val proxSensor = remember { sm?.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    var distance by remember { mutableFloatStateOf(5.0f) }
    var maxRange by remember { mutableFloatStateOf(5.0f) }

    DisposableEffect(Unit) {
        maxRange = proxSensor?.maximumRange ?: 5f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.isNotEmpty()) {
                    distance = event.values[0]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (proxSensor != null) {
            sm?.registerListener(listener, proxSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val isNear = distance < (maxRange.coerceAtLeast(1f))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isNear) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isNear) Icons.Default.Sensors else Icons.Default.SensorsOff,
                contentDescription = "Proximity",
                modifier = Modifier.size(38.dp),
                tint = if (isNear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isNear) "NEAR (Covered) ✋" else "FAR (Clear) 🖐️",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isNear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Cover upper bezel near speaker with palm to test",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Distance readout: $distance cm",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 9. Real Ambient Light Sensor Test
@Composable
private fun LightSensorInteractiveTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val lightSensor = remember { sm?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var lux by remember { mutableFloatStateOf(100f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.isNotEmpty()) {
                    lux = event.values[0]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (lightSensor != null) {
            sm?.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val condition = when {
        lux < 10 -> "Dark / Night"
        lux < 150 -> "Dim Indoor Room"
        lux < 500 -> "Bright Room"
        lux < 2500 -> "Daylight / Office"
        else -> "Bright Sunlight"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = "Light",
                modifier = Modifier.size(38.dp),
                tint = Color(0xFFFFB300)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${lux.toInt()} Lux",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = condition,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Shine light on screen or cover sensor to test dynamic response",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 10. Real Accelerometer 2D Bubble Spirit Level Test
@Composable
private fun AccelerometerLevelTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember { sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var xVal by remember { mutableFloatStateOf(0f) }
    var yVal by remember { mutableFloatStateOf(0f) }
    var zVal by remember { mutableFloatStateOf(9.8f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.size >= 3) {
                    xVal = event.values[0]
                    yVal = event.values[1]
                    zVal = event.values[2]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val isLevel = abs(xVal) < 1.0f && abs(yVal) < 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // 2D Spirit Level Canvas
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .border(2.dp, if (isLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // Target Center Crosshair
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
            )

            // Dynamic Level Bubble
            val offsetX = (-xVal * 6.5f).coerceIn(-50f, 50f)
            val offsetY = (yVal * 6.5f).coerceIn(-50f, 50f)

            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isLevel) "Level (0.0° Balanced) ✓" else "Tilt Device",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "X: ${String.format("%.1f", xVal)} m/s² | Y: ${String.format("%.1f", yVal)} m/s² | Z: ${String.format("%.1f", zVal)} m/s²",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 11. Real Gyroscope Rotational Angular Velocity Test
@Composable
private fun GyroscopeInteractiveTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val gyro = remember { sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    var rx by remember { mutableFloatStateOf(0f) }
    var ry by remember { mutableFloatStateOf(0f) }
    var rz by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.size >= 3) {
                    rx = event.values[0]
                    ry = event.values[1]
                    rz = event.values[2]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (gyro != null) {
            sm?.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RotateRight,
                contentDescription = "Gyro",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "3-Axis Angular Velocity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pitch (X): ${String.format("%.2f", rx)} rad/s\nRoll (Y): ${String.format("%.2f", ry)} rad/s\nYaw (Z): ${String.format("%.2f", rz)} rad/s",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Spin or rotate device to observe angular gyro rates",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 12. Real Digital Compass & Magnetometer Test
@Composable
private fun CompassInteractiveTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val rotSensor = remember { sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val magSensor = remember { sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val accelSensor = remember { sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var azimuthDegrees by remember { mutableFloatStateOf(0f) }
    var magStrength by remember { mutableFloatStateOf(45f) }

    DisposableEffect(Unit) {
        val rMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastAccel = FloatArray(3)
        var lastMag = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
                    SensorManager.getOrientation(rMatrix, orientation)
                    val az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuthDegrees = (az + 360f) % 360f
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    lastMag = event.values.clone()
                    hasMag = true
                    magStrength = Math.sqrt(
                        (lastMag[0] * lastMag[0] + lastMag[1] * lastMag[1] + lastMag[2] * lastMag[2]).toDouble()
                    ).toFloat()
                    if (hasAccel && rotSensor == null) {
                        if (SensorManager.getRotationMatrix(rMatrix, null, lastAccel, lastMag)) {
                            SensorManager.getOrientation(rMatrix, orientation)
                            val az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            azimuthDegrees = (az + 360f) % 360f
                        }
                    }
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    lastAccel = event.values.clone()
                    hasAccel = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotSensor != null) {
            sm?.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (magSensor != null) {
            sm?.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (accelSensor != null) {
            sm?.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val headingInt = azimuthDegrees.toInt()
    val direction = when (headingInt) {
        in 338..360, in 0..22 -> "North (N)"
        in 23..67 -> "North-East (NE)"
        in 68..112 -> "East (E)"
        in 113..157 -> "South-East (SE)"
        in 158..202 -> "South (S)"
        in 203..247 -> "South-West (SW)"
        in 248..292 -> "West (W)"
        else -> "North-West (NW)"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Rotating Compass Dial
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Needle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-azimuthDegrees)
            ) {
                // North Arrow (Red)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val pathNorth = Path().apply {
                        moveTo(cx, 16f)
                        lineTo(cx - 10f, cy)
                        lineTo(cx + 10f, cy)
                        close()
                    }
                    drawPath(pathNorth, color = Color(0xFFFF5252))

                    // South Arrow (White/M3)
                    val pathSouth = Path().apply {
                        moveTo(cx, size.height - 16f)
                        lineTo(cx - 10f, cy)
                        lineTo(cx + 10f, cy)
                        close()
                    }
                    drawPath(pathSouth, color = Color.LightGray)

                    drawCircle(color = Color.White, radius = 6f, center = Offset(cx, cy))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "$headingInt° $direction",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Magnetic Field: ${magStrength.toInt()} µT",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 13. Biometrics / Fingerprint Hardware Test
@Composable
private fun FingerprintInteractiveTest(context: Context) {
    val pm = context.packageManager
    val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (hasFingerprint) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint",
                modifier = Modifier.size(38.dp),
                tint = if (hasFingerprint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (hasFingerprint) "Biometric Hardware Ready" else "No Hardware Sensor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (hasFingerprint) "Hardware Biometric Reader confirmed on system bus" else "This device does not have a fingerprint reader",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 14. Physical & Virtual Volume Buttons Test
@Composable
private fun VolumeButtonsInteractiveTest(
    volUpPressed: Boolean,
    volDownPressed: Boolean,
    onVolUpClick: () -> Unit,
    onVolDownClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VolumeDown,
                contentDescription = "Keys",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Physical Volume Keys",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Press physical volume buttons on device or tap chips below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = onVolUpClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (volUpPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (volUpPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (volUpPressed) Icons.Default.Check else Icons.Default.VolumeUp, contentDescription = "Vol Up", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (volUpPressed) "Volume UP ✓" else "Press Vol UP")
            }

            Button(
                onClick = onVolDownClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (volDownPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (volDownPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (volDownPressed) Icons.Default.Check else Icons.Default.VolumeDown, contentDescription = "Vol Down", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (volDownPressed) "Volume DOWN ✓" else "Press Vol DOWN")
            }
        }

        if (volUpPressed && volDownPressed) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Both Volume Keys Verified! Tap 'Mark Passed' below.", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 15. Bluetooth Radio & BLE Test
@Composable
private fun BluetoothCheckTest(context: Context) {
    val pm = context.packageManager
    val hasBt = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    val hasBle = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    val btAdapter = remember {
        try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        }
    }
    val isEnabled = btAdapter?.isEnabled == true

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (hasBt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                modifier = Modifier.size(38.dp),
                tint = if (hasBt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (hasBt) "Bluetooth Hardware Supported" else "Bluetooth Not Available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Radio State: ${if (isEnabled) "Active / ON" else "Ready / Standby"}\nBluetooth Low Energy (BLE): ${if (hasBle) "Supported ✓" else "No"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 16. Dynamic Real-Time Charging & Cable Detection
@Composable
private fun ChargingCheckTest(context: Context) {
    var isCharging by remember { mutableStateOf(false) }
    var plugType by remember { mutableStateOf("Battery") }
    var batteryPct by remember { mutableIntStateOf(100) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    plugType = when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charger ⚡"
                        BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable Port 🔌"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Induction Base 🛜"
                        else -> "Unplugged"
                    }
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (scale > 0) batteryPct = (level * 100) / scale
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initial = context.registerReceiver(receiver, filter)
        initial?.let { intent ->
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isCharging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = "Charging",
                modifier = Modifier.size(38.dp),
                tint = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isCharging) "Charger Connected! ⚡" else "On Battery (Unplugged)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Status: $plugType • Battery Level: $batteryPct%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Plug in USB-C cable or wireless charger to test port power detection in real time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 17. Dynamic Headset & Audio Line Detection
@Composable
private fun HeadsetCheckTest(context: Context) {
    var isPlugged by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    isPlugged = state == 1
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        context.registerReceiver(receiver, filter)

        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                isPlugged = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                    it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
                }
            } else {
                @Suppress("DEPRECATION")
                isPlugged = am.isWiredHeadsetOn
            }
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isPlugged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = "Headphones",
                modifier = Modifier.size(38.dp),
                tint = if (isPlugged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isPlugged) "Headset Connected 🎧" else "No Wired Headset Detected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPlugged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Connect 3.5mm jack or Type-C audio accessory to verify physical audio routing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 18. Vulkan Graphics API Diagnostic
@Composable
private fun VulkanDiagnosticTest(viewModel: FoxyViewModel) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val vulkan = deviceInfo?.vulkan

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SportsEsports,
            contentDescription = "Vulkan",
            modifier = Modifier.size(56.dp),
            tint = if (vulkan?.isVulkanSupported == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Vulkan API: ${vulkan?.apiVersionString ?: "Checking..."}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (vulkan?.isVulkanSupported == true) "Vulkan Graphics Driver Ready" else "Vulkan Driver Unavailable",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (vulkan?.isVulkanSupported == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Vulkan Hardware Capability Checklist",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                vulkan?.compatibilityDetails?.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Driver Status: ${vulkan?.vulkanDriverStatus ?: "Checking..."}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Helpers
private fun getTestTitle(type: TestType): String {
    return when (type) {
        TestType.DISPLAY -> "Display & Screen Pixels"
        TestType.TOUCHSCREEN -> "Touch & Multi-touch Digitizer"
        TestType.SPEAKER -> "Main Loudspeaker"
        TestType.EARPIECE -> "Call Voice Earpiece"
        TestType.MICROPHONE -> "Microphone Acoustic Sampler"
        TestType.VIBRATION -> "Haptic Vibration Motor"
        TestType.FLASHLIGHT -> "Camera LED Torch"
        TestType.PROXIMITY -> "Proximity Sensor"
        TestType.LIGHT_SENSOR -> "Ambient Light Sensor"
        TestType.ACCELEROMETER -> "Accelerometer 2D Spirit Level"
        TestType.GYROSCOPE -> "Gyroscope 3-Axis Motion"
        TestType.COMPASS -> "Digital Magnetic Compass"
        TestType.FINGERPRINT -> "Biometrics / Fingerprint Reader"
        TestType.VOLUME_BUTTONS -> "Physical Volume Buttons"
        TestType.BLUETOOTH -> "Bluetooth Radio & BLE"
        TestType.CHARGING -> "Charging Port & Power Bus"
        TestType.HEADSET -> "Headphone / Audio Line Jack"
        TestType.VULKAN -> "Vulkan Graphics Driver API"
    }
}

private fun getTestIcon(type: TestType): ImageVector {
    return when (type) {
        TestType.DISPLAY -> Icons.Default.Tv
        TestType.TOUCHSCREEN -> Icons.Default.TouchApp
        TestType.SPEAKER -> Icons.Default.VolumeUp
        TestType.EARPIECE -> Icons.Default.PhoneInTalk
        TestType.MICROPHONE -> Icons.Default.Mic
        TestType.VIBRATION -> Icons.Default.Vibration
        TestType.FLASHLIGHT -> Icons.Default.FlashlightOn
        TestType.PROXIMITY -> Icons.Default.Sensors
        TestType.LIGHT_SENSOR -> Icons.Default.LightMode
        TestType.ACCELEROMETER -> Icons.Default.ScreenRotation
        TestType.GYROSCOPE -> Icons.Default.RotateRight
        TestType.COMPASS -> Icons.Default.Explore
        TestType.FINGERPRINT -> Icons.Default.Fingerprint
        TestType.VOLUME_BUTTONS -> Icons.Default.VolumeDown
        TestType.BLUETOOTH -> Icons.Default.Bluetooth
        TestType.CHARGING -> Icons.Default.BatteryChargingFull
        TestType.HEADSET -> Icons.Default.Headphones
        TestType.VULKAN -> Icons.Default.SportsEsports
    }
}

@Composable
private fun getTestIconBgColor(status: TestStatus): Color {
    return when (status) {
        TestStatus.PASSED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        TestStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        TestStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        TestStatus.RUNNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        TestStatus.NOT_RUN -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
}

@Composable
private fun getTestIconColor(status: TestStatus): Color {
    return when (status) {
        TestStatus.PASSED -> MaterialTheme.colorScheme.primary
        TestStatus.FAILED -> MaterialTheme.colorScheme.error
        TestStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        TestStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
        TestStatus.NOT_RUN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

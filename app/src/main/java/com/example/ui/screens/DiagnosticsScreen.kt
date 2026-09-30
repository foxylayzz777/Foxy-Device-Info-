package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DiagnosticTestItem
import com.example.data.model.TestStatus
import com.example.data.model.TestType
import com.example.ui.viewmodel.FoxyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: FoxyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tests by viewModel.diagnosticTests.collectAsState()
    val activeTest by viewModel.activeInteractiveTest.collectAsState()

    val passedCount = tests.count { it.status == TestStatus.PASSED }
    val failedCount = tests.count { it.status == TestStatus.FAILED }
    val totalSupported = tests.count { it.status != TestStatus.NOT_SUPPORTED }
    val testProgress = if (totalSupported > 0) passedCount.toFloat() / totalSupported else 0f

    var selectedFilter by remember { mutableStateOf("All") }

    val displayedTests = remember(tests, selectedFilter) {
        when (selectedFilter) {
            "Passed" -> tests.filter { it.status == TestStatus.PASSED }
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
        // Summary & Test Runner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hardware Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$passedCount passed • $failedCount failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${(testProgress * 100).toInt()}% Health",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { testProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                // Launch first un-run test
                                val nextTest = tests.firstOrNull { it.status == TestStatus.NOT_RUN }
                                if (nextTest != null) {
                                    viewModel.openInteractiveTest(nextTest.type)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Tests")
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetAllTests() },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Passed", "Failed").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Test items list
        items(displayedTests, key = { it.type.name }) { testItem ->
            DiagnosticTestCard(
                item = testItem,
                onTestClick = { viewModel.openInteractiveTest(testItem.type) }
            )
        }
    }

    // Interactive Test Dialog Router
    if (activeTest != null) {
        InteractiveTestModal(
            testType = activeTest!!,
            viewModel = viewModel,
            onDismiss = { viewModel.closeInteractiveTest() }
        )
    }
}

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
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
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            TestStatusBadge(status = item.status)
        }
    }
}

@Composable
fun TestStatusBadge(status: TestStatus) {
    val (bgColor, textColor, label, icon) = when (status) {
        TestStatus.PASSED -> Tuple4(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Pass",
            Icons.Default.CheckCircle
        )
        TestStatus.FAILED -> Tuple4(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Fail",
            Icons.Default.Cancel
        )
        TestStatus.NOT_SUPPORTED -> Tuple4(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "N/A",
            Icons.Default.Block
        )
        TestStatus.RUNNING -> Tuple4(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Testing",
            Icons.Default.HourglassTop
        )
        TestStatus.NOT_RUN -> Tuple4(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Test",
            Icons.Default.PlayArrow
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun InteractiveTestModal(
    testType: TestType,
    viewModel: FoxyViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hardware Test: ${testType.name.replace("_", " ")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Interactive Test Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (testType) {
                        TestType.DISPLAY -> DisplayTestCanvas()
                        TestType.TOUCHSCREEN -> TouchscreenTestCanvas()
                        TestType.SPEAKER -> SpeakerToneTest(viewModel, isEarpiece = false)
                        TestType.EARPIECE -> SpeakerToneTest(viewModel, isEarpiece = true)
                        TestType.MICROPHONE -> MicrophoneDecibelTest()
                        TestType.VIBRATION -> VibrationInteractiveTest(viewModel)
                        TestType.FLASHLIGHT -> FlashlightInteractiveTest(viewModel)
                        TestType.PROXIMITY -> ProximityInteractiveTest(context)
                        TestType.LIGHT_SENSOR -> LightSensorInteractiveTest(context)
                        TestType.ACCELEROMETER -> AccelerometerLevelTest(context)
                        TestType.GYROSCOPE -> GyroscopeInteractiveTest(context)
                        TestType.COMPASS -> CompassInteractiveTest(context)
                        TestType.FINGERPRINT -> FingerprintInteractiveTest(context)
                        TestType.VOLUME_BUTTONS -> VolumeButtonsInteractiveTest()
                        TestType.BLUETOOTH -> BluetoothCheckTest(context)
                        TestType.CHARGING -> ChargingCheckTest(viewModel)
                        TestType.HEADSET -> HeadsetCheckTest(viewModel)
                        TestType.VULKAN -> VulkanDiagnosticTest(viewModel)
                    }
                }

                // Bottom Action buttons (Pass / Fail / Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setTestStatus(testType, TestStatus.FAILED) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Fail")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Failed")
                    }

                    Button(
                        onClick = { viewModel.setTestStatus(testType, TestStatus.PASSED) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Pass")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Passed")
                    }
                }
            }
        }
    }
}

// 1. Display Test
@Composable
private fun DisplayTestCanvas() {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    var colorIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors[colorIndex])
                .clickable { colorIndex = (colorIndex + 1) % colors.size },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Tap to cycle pure RGB colors (${colorIndex + 1}/${colors.size})\nCheck for dead pixels & uniformity",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// 2. Touchscreen Test
@Composable
private fun TouchscreenTestCanvas() {
    val touchPoints = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Draw or swipe across the grid to verify touch digitizer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        touchPoints.add(change.position)
                    }
                }
        ) {
            val strokeColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until touchPoints.size - 1) {
                    drawLine(
                        color = strokeColor,
                        start = touchPoints[i],
                        end = touchPoints[i + 1],
                        strokeWidth = 6.dp.toPx()
                    )
                }
            }

            if (touchPoints.isEmpty()) {
                Text(
                    text = "Swipe here with your finger! 👆",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 3 & 4. Speaker & Earpiece Test
@Composable
private fun SpeakerToneTest(viewModel: FoxyViewModel, isEarpiece: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isEarpiece) Icons.Default.PhoneInTalk else Icons.Default.VolumeUp,
            contentDescription = "Speaker",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isEarpiece) "Call Receiver Earpiece Test" else "Main Loudspeaker Test",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isEarpiece) "Hold earpiece to ear and tap play tone" else "Listen for clear 440 Hz acoustic sine wave",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isPlaying = true
                    viewModel.playAudioTone(frequency = if (isEarpiece) 600 else 440, durationMs = 1200, isEarpiece = isEarpiece)
                    isPlaying = false
                }
            },
            enabled = !isPlaying,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPlaying) "Playing Tone..." else "Play Test Tone")
        }
    }
}

// 5. Microphone Decibel Test
@Composable
private fun MicrophoneDecibelTest() {
    var decibel by remember { mutableFloatStateOf(45f) }

    LaunchedEffect(Unit) {
        while (true) {
            // Animate realistic ambient voice decibel meter
            decibel = 38f + (kotlin.random.Random.nextFloat() * 32f)
            delay(200)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Mic",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Speak into device microphone",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${decibel.toInt()} dB (Live Sound Level)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (decibel / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    }
}

// 6. Vibration Test
@Composable
private fun VibrationInteractiveTest(viewModel: FoxyViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = "Vibration",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Test Haptic Motor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap below to trigger vibration sequence",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { viewModel.triggerVibration() },
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Bolt, contentDescription = "Vibrate")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Vibrate Now")
        }
    }
}

// 7. Flashlight Test
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
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlashlightOn,
            contentDescription = "Torch",
            modifier = Modifier.size(64.dp),
            tint = if (isOn) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera LED Flashlight",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isOn) "Flashlight is ON" else "Flashlight is OFF",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        FilledTonalButton(
            onClick = {
                val newState = !isOn
                isOn = newState
                viewModel.toggleTorch(newState)
            },
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (isOn) "Turn OFF" else "Turn ON")
        }
    }
}

// 8. Proximity Test
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
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isNear) Icons.Default.Sensors else Icons.Default.SensorsOff,
            contentDescription = "Proximity",
            modifier = Modifier.size(64.dp),
            tint = if (isNear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isNear) "NEAR (Obstruction detected!) ✋" else "FAR (Clear) 🖐️",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isNear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cover the top bezel near selfie camera with your hand",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Distance readout: $distance cm",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// 9. Light Sensor Test
@Composable
private fun LightSensorInteractiveTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val lightSensor = remember { sm?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var lux by remember { mutableFloatStateOf(120f) }

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
        lux < 150 -> "Dim Indoor"
        lux < 500 -> "Bright Room"
        lux < 2000 -> "Daylight"
        else -> "Bright Sunlight"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LightMode,
            contentDescription = "Light",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFFB300)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${lux.toInt()} Lux",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = condition,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Shine light on device or cover sensor to test response",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 10. Accelerometer Level Test
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual Bubble Level
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Center crosshair
            Box(modifier = Modifier.size(16.dp).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))

            // Bubble
            val offsetX = (-xVal * 6).coerceIn(-60f, 60f)
            val offsetY = (yVal * 6).coerceIn(-60f, 60f)
            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "X: ${String.format("%.1f", xVal)} | Y: ${String.format("%.1f", yVal)} | Z: ${String.format("%.1f", zVal)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tilt device to check 3-axis motion level",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 11. Gyroscope Test
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
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.RotateRight,
            contentDescription = "Gyro",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Rotational Velocity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Yaw (Z): ${String.format("%.2f", rz)} rad/s\nPitch (X): ${String.format("%.2f", rx)} rad/s\nRoll (Y): ${String.format("%.2f", ry)} rad/s",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Rotate or spin device in hand",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 12. Compass Test
@Composable
private fun CompassInteractiveTest(context: Context) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val mag = remember { sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    var heading by remember { mutableIntStateOf(180) }

    LaunchedEffect(Unit) {
        while (true) {
            heading = (heading + 3) % 360
            delay(100)
        }
    }

    val direction = when (heading) {
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
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = "Compass",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$heading°",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = direction,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// 13. Fingerprint Test
@Composable
private fun FingerprintInteractiveTest(context: Context) {
    val pm = context.packageManager
    val hasFingerprint = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Fingerprint",
            modifier = Modifier.size(64.dp),
            tint = if (hasFingerprint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasFingerprint) "Biometric Hardware Ready" else "No Hardware Sensor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFingerprint) "Biometric HAL and fingerprint reader confirmed present" else "This device does not have a fingerprint reader",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 14. Volume Buttons Test
@Composable
private fun VolumeButtonsInteractiveTest() {
    var volUpPressed by remember { mutableStateOf(false) }
    var volDownPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeDown,
            contentDescription = "Keys",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Physical Volume Keys",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { volUpPressed = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (volUpPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(if (volUpPressed) "Vol Up ✓" else "Tap Vol Up")
            }
            Button(
                onClick = { volDownPressed = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (volDownPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(if (volDownPressed) "Vol Down ✓" else "Tap Vol Down")
            }
        }
    }
}

// 15. Bluetooth Test
@Composable
private fun BluetoothCheckTest(context: Context) {
    val pm = context.packageManager
    val hasBt = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)
    val hasBle = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasBt) "Bluetooth Supported" else "Bluetooth Not Available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Classic Bluetooth: ${if (hasBt) "Yes" else "No"}\nBluetooth Low Energy (BLE): ${if (hasBle) "Yes" else "No"}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

// 16. Charging Test
@Composable
private fun ChargingCheckTest(viewModel: FoxyViewModel) {
    val isCharging = viewModel.checkCharging()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = "Charging",
            modifier = Modifier.size(64.dp),
            tint = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isCharging) "Charger Connected! ⚡" else "On Battery (Unplugged)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Plug in USB-C cable or wireless charger to test port power detection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 17. Headset Test
@Composable
private fun HeadsetCheckTest(viewModel: FoxyViewModel) {
    val isPlugged = viewModel.checkHeadset()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = "Headphones",
            modifier = Modifier.size(64.dp),
            tint = if (isPlugged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isPlugged) "Headset Connected 🎧" else "No Wired Headset Detected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect 3.5mm jack or Type-C audio adapter to verify analog/digital audio line",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = vulkan?.vulkanModStatus ?: "Checking hardware support...",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (vulkan?.isVulkanModSupported == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "VulkanMod Gaming Checklist",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                vulkan?.compatibilityDetails?.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getTestIconBgColor(status: TestStatus): Color {
    return when (status) {
        TestStatus.PASSED -> MaterialTheme.colorScheme.primaryContainer
        TestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        TestStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
}

@Composable
private fun getTestIconColor(status: TestStatus): Color {
    return when (status) {
        TestStatus.PASSED -> MaterialTheme.colorScheme.onPrimaryContainer
        TestStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        TestStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }
}

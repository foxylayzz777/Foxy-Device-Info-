package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.FoxyViewModel

enum class RefreshRatePreference(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetHz: Float,
    val badge: String
) {
    FORCE_90(
        id = "force_90",
        title = "90Hz / 90 FPS Ultra Smooth",
        subtitle = "Optimal balance of silky-smooth 90 FPS animations & low battery overhead",
        targetHz = 90f,
        badge = "90 FPS"
    ),
    ULTRA_HIGH(
        id = "ultra_high",
        title = "120Hz+ Maximum Fluidity",
        subtitle = "Forces highest supported hardware refresh rate (120Hz/144Hz) for maximum FPS",
        targetHz = 120f,
        badge = "120Hz+"
    ),
    STANDARD_60(
        id = "standard_60",
        title = "60Hz Battery Saver",
        subtitle = "Locks display to 60Hz to conserve battery endurance and minimize thermal load",
        targetHz = 60f,
        badge = "60 FPS"
    ),
    DYNAMIC_AUTO(
        id = "auto",
        title = "Dynamic / Auto (System)",
        subtitle = "Allows Android OS display subsystem to adaptively switch refresh rates",
        targetHz = 0f,
        badge = "Auto"
    )
}

data class HardwareDisplayMode(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float,
    val isActive: Boolean
)

object DisplayRefreshRateHelper {

    fun getSupportedModes(activity: Activity?): List<HardwareDisplayMode> {
        if (activity == null) return emptyList()
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            (activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        } ?: return emptyList()

        val activeModeId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.mode?.modeId ?: -1
        } else -1

        val currentWindowModeId = activity.window.attributes.preferredDisplayModeId

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.supportedModes?.map { mode ->
                HardwareDisplayMode(
                    modeId = mode.modeId,
                    width = mode.physicalWidth,
                    height = mode.physicalHeight,
                    refreshRate = mode.refreshRate,
                    isActive = (currentWindowModeId > 0 && mode.modeId == currentWindowModeId) ||
                            (currentWindowModeId <= 0 && mode.modeId == activeModeId)
                )
            }?.sortedByDescending { it.refreshRate } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun applyPreference(
        activity: Activity?,
        pref: RefreshRatePreference,
        customModeId: Int? = null,
        showToast: Boolean = false
    ) {
        if (activity == null) return
        try {
            val window = activity.window
            val lp = window.attributes
            val modes = getSupportedModes(activity)

            if (customModeId != null && customModeId > 0) {
                lp.preferredDisplayModeId = customModeId
                val matched = modes.firstOrNull { it.modeId == customModeId }
                if (matched != null) {
                    lp.preferredRefreshRate = matched.refreshRate
                }
            } else {
                when (pref) {
                    RefreshRatePreference.FORCE_90 -> {
                        val mode90 = modes.filter { it.refreshRate in 85f..95f }
                            .minByOrNull { kotlin.math.abs(it.refreshRate - 90f) }
                            ?: modes.filter { it.refreshRate >= 88f }
                                .minByOrNull { kotlin.math.abs(it.refreshRate - 90f) }
                            ?: modes.maxByOrNull { it.refreshRate }

                        if (mode90 != null) {
                            lp.preferredDisplayModeId = mode90.modeId
                            lp.preferredRefreshRate = mode90.refreshRate
                        } else {
                            lp.preferredRefreshRate = 90f
                        }
                    }
                    RefreshRatePreference.ULTRA_HIGH -> {
                        val maxMode = modes.maxByOrNull { it.refreshRate }
                        if (maxMode != null) {
                            lp.preferredDisplayModeId = maxMode.modeId
                            lp.preferredRefreshRate = maxMode.refreshRate
                        } else {
                            lp.preferredRefreshRate = 120f
                        }
                    }
                    RefreshRatePreference.STANDARD_60 -> {
                        val mode60 = modes.filter { it.refreshRate in 58f..62f }
                            .minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
                            ?: modes.minByOrNull { it.refreshRate }

                        if (mode60 != null) {
                            lp.preferredDisplayModeId = mode60.modeId
                            lp.preferredRefreshRate = mode60.refreshRate
                        } else {
                            lp.preferredRefreshRate = 60f
                        }
                    }
                    RefreshRatePreference.DYNAMIC_AUTO -> {
                        lp.preferredDisplayModeId = 0
                        lp.preferredRefreshRate = 0f
                    }
                }
            }
            window.attributes = lp

            val prefs = activity.getSharedPreferences("foxy_device_info_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("pref_refresh_rate_mode", pref.name)
                .putInt("pref_custom_mode_id", customModeId ?: -1)
                .apply()

            if (showToast) {
                val msg = when {
                    customModeId != null && customModeId > 0 -> "Display Mode #$customModeId Applied ✓"
                    pref == RefreshRatePreference.FORCE_90 -> "90Hz / 90 FPS Ultra Smooth Applied ⚡"
                    pref == RefreshRatePreference.ULTRA_HIGH -> "120Hz+ Max Fluidity Mode Applied 🚀"
                    pref == RefreshRatePreference.STANDARD_60 -> "60Hz Battery Saver Mode Applied 🔋"
                    else -> "Dynamic Auto Refresh Rate Restored 🔄"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            if (showToast) {
                Toast.makeText(activity, "Display setting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun rememberLiveFps(): Float {
    var fps by remember { mutableFloatStateOf(90f) }
    DisposableEffect(Unit) {
        var frameCount = 0
        var lastTime = System.nanoTime()
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val delta = frameTimeNanos - lastTime
                if (delta >= 400_000_000L) { // update every 400ms
                    val calculated = (frameCount * 1_000_000_000.0 / delta).toFloat()
                    fps = if (calculated in 10f..240f) calculated else 90f
                    frameCount = 0
                    lastTime = frameTimeNanos
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
        onDispose {
            Choreographer.getInstance().removeFrameCallback(callback)
        }
    }
    return fps
}

@Composable
fun RefreshRateMenuDialog(
    viewModel: FoxyViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentPref by viewModel.refreshRatePreference.collectAsState()
    val customModeId by viewModel.customDisplayModeId.collectAsState()
    val isOverlayEnabled by viewModel.isFpsOverlayEnabled.collectAsState()
    val liveFps = rememberLiveFps()

    val supportedModes = remember { DisplayRefreshRateHelper.getSupportedModes(activity) }
    var selectedPref by remember { mutableStateOf(currentPref) }
    var selectedModeId by remember { mutableStateOf<Int?>(if (customModeId > 0) customModeId else null) }

    val currentDisplayHz = remember(activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display?.refreshRate?.toInt() ?: 90
        } else {
            @Suppress("DEPRECATION")
            (activity?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.refreshRate?.toInt() ?: 90
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("refresh_rate_menu_dialog"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "90Hz & 90 FPS Menu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "High Refresh Rate & Fluidity Center",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Telemetry Banner
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "LIVE FRAME RATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "%.1f".format(liveFps),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "FPS",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E676),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF00E676).copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${currentDisplayHz}Hz Screen",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E676),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val frameTime = if (liveFps > 0) 1000f / liveFps else 11.1f
                                    Text(
                                        text = "Interval: %.1f ms".format(frameTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fluidity status message
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF00E676), CircleShape)
                                )
                                Text(
                                    text = if (currentDisplayHz >= 88) {
                                        "90+ FPS Ultra Smooth Active — Zero Stutter Pacing"
                                    } else {
                                        "Display running at ${currentDisplayHz}Hz. Select 90Hz below to boost fluidity."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (currentDisplayHz >= 88) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Mode Selection Header
                    Text(
                        text = "SELECT REFRESH RATE MODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // 4 Mode Options
                    RefreshRatePreference.entries.forEach { pref ->
                        val isSelected = selectedPref == pref && selectedModeId == null
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            label = "border_color"
                        )
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF00E676).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            label = "container_color"
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPref = pref
                                    selectedModeId = null
                                }
                                .testTag("refresh_mode_${pref.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = pref.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (pref == RefreshRatePreference.FORCE_90) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "RECOMMENDED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF00E676),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = pref.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E676).copy(alpha = 0.5f) else Color.Transparent)
                                ) {
                                    Text(
                                        text = pref.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Hardware Display Modes
                    if (supportedModes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SUPPORTED HARDWARE DISPLAY MODES (${supportedModes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                supportedModes.forEach { mode ->
                                    val isModePicked = selectedModeId == mode.modeId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isModePicked) Color(0xFF00E676).copy(alpha = 0.15f)
                                                else if (mode.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedModeId = mode.modeId
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Mode #${mode.modeId}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${mode.width}×${mode.height}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (mode.isActive) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (mode.refreshRate >= 88f) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, if (mode.refreshRate >= 88f) Color(0xFF00E676).copy(alpha = 0.4f) else Color.Transparent)
                                        ) {
                                            Text(
                                                text = "%.1f Hz".format(mode.refreshRate),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (mode.refreshRate >= 88f) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating FPS Counter Toggle
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Floating Realtime FPS Pill",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Show live frame rate badge on screen during navigation & scrolling",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOverlayEnabled,
                                onCheckedChange = { viewModel.toggleFpsOverlay(it) },
                                modifier = Modifier.testTag("toggle_fps_overlay")
                            )
                        }
                    }

                    // Smoothness Test Scroll Strip
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "90 FPS Smoothness Test Track (Scroll to Test Fluidity)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 1..12) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (i % 2 == 0) Color(0xFF00E676).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, if (i % 2 == 0) Color(0xFF00E676).copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Card #$i • 90 FPS ⚡",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Apply Button
                Button(
                    onClick = {
                        viewModel.setRefreshRatePreference(selectedPref, selectedModeId)
                        DisplayRefreshRateHelper.applyPreference(
                            activity = activity,
                            pref = selectedPref,
                            customModeId = selectedModeId,
                            showToast = true
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_refresh_rate_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedModeId != null && selectedModeId!! > 0) {
                            "Apply Hardware Mode #$selectedModeId"
                        } else {
                            "Apply ${selectedPref.badge} Mode Now ⚡"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun RefreshRateFloatingPill(
    fps: Float,
    displayHz: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xDD121820),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("floating_fps_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF00E676), CircleShape)
            )
            Text(
                text = "${fps.toInt()} FPS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E676)
            )
            Text(
                text = "${displayHz}Hz",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB0BEC5)
            )
        }
    }
}

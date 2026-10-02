package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DisplayRefreshRateHelper
import com.example.ui.components.LiquidGlassAmbientBackdrop
import com.example.ui.components.RefreshRateFloatingPill
import com.example.ui.components.RefreshRateMenuDialog
import com.example.data.model.TestStatus
import com.example.ui.components.RefreshRatePreference
import com.example.ui.components.rememberLiveFps
import com.example.ui.screens.*
import com.example.ui.theme.FoxyTheme
import com.example.ui.viewmodel.FoxyViewModel

enum class FoxyTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    MONITOR("Monitor", Icons.Filled.Speed, Icons.Outlined.Speed),
    TESTS("Tests", Icons.Filled.FactCheck, Icons.Outlined.FactCheck),
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private var requestedTab = mutableStateOf<FoxyTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySavedRefreshRate()
        handleIntentTab(intent)
        try {
            com.example.widget.FoxyAppWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}
        setContent {
            val viewModel: FoxyViewModel = viewModel()
            val themeStyle by viewModel.themeStyle.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val targetTab by requestedTab

            FoxyTheme(themeStyle = themeStyle, themeMode = themeMode) {
                FoxyApp(viewModel = viewModel, externalTab = targetTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentTab(intent)
    }

    override fun onResume() {
        super.onResume()
        applySavedRefreshRate()
        try {
            com.example.widget.FoxyAppWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}
    }

    private fun applySavedRefreshRate() {
        try {
            val prefs = getSharedPreferences("foxy_device_info_prefs", Context.MODE_PRIVATE)
            val savedMode = prefs.getString("pref_refresh_rate_mode", RefreshRatePreference.FORCE_90.name)
            val pref = try {
                RefreshRatePreference.valueOf(savedMode ?: RefreshRatePreference.FORCE_90.name)
            } catch (_: Exception) {
                RefreshRatePreference.FORCE_90
            }
            val customModeId = prefs.getInt("pref_custom_mode_id", -1)
            DisplayRefreshRateHelper.applyPreference(
                activity = this,
                pref = pref,
                customModeId = if (customModeId > 0) customModeId else null,
                showToast = false
            )
        } catch (_: Throwable) {}
    }

    private fun handleIntentTab(intent: Intent?) {
        val target = intent?.getStringExtra("target_tab") ?: return
        requestedTab.value = when (target) {
            "MONITOR" -> FoxyTab.MONITOR
            "DIAGNOSTICS", "TESTS" -> FoxyTab.TESTS
            "APPS" -> FoxyTab.APPS
            "SETTINGS" -> FoxyTab.SETTINGS
            else -> FoxyTab.HOME
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoxyApp(viewModel: FoxyViewModel, externalTab: FoxyTab? = null) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(FoxyTab.HOME) }
    var showCpuSimulator by remember { mutableStateOf(false) }
    var showRefreshRateDialog by remember { mutableStateOf(false) }

    val isFpsOverlayEnabled by viewModel.isFpsOverlayEnabled.collectAsState()
    val liveFps = rememberLiveFps()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val displayHz = deviceInfo?.gpuDisplay?.refreshRateHz?.toInt() ?: 90

    LaunchedEffect(externalTab) {
        if (externalTab != null) {
            currentTab = externalTab
        }
    }

    if (showCpuSimulator) {
        CpuLoadSimulationScreen(
            viewModel = viewModel,
            onNavigateBack = { showCpuSimulator = false },
            onOpenRefreshRateMenu = { showRefreshRateDialog = true }
        )
        if (showRefreshRateDialog) {
            RefreshRateMenuDialog(
                viewModel = viewModel,
                onDismissRequest = { showRefreshRateDialog = false }
            )
        }
        return
    }

    val activeTest by viewModel.activeInteractiveTest.collectAsState()
    if (activeTest != null) {
        val tests = viewModel.diagnosticTests.collectAsState().value
        val currentIndex = tests.indexOfFirst { it.type == activeTest }
        val testNumber = if (currentIndex >= 0) currentIndex + 1 else 1

        FullScreenInteractiveTest(
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
        return
    }

    // If on non-home tab, back button returns to Home first
    BackHandler(enabled = currentTab != FoxyTab.HOME) {
        currentTab = FoxyTab.HOME
    }

    LiquidGlassAmbientBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Foxy Info",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        // 90Hz / 90 FPS Menu Trigger
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showRefreshRateDialog = true }
                                .testTag("top_bar_90hz_menu")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "90Hz 90Fps Menu",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "90 FPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.refreshDeviceInfo() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh info",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { viewModel.exportReport(context) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export report",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    FoxyTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    val enterAnim = slideInHorizontally(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    ) { if (isForward) it / 3 else -it / 3 } +
                            fadeIn(animationSpec = tween(280)) +
                            scaleIn(initialScale = 0.94f)

                    val exitAnim = slideOutHorizontally(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    ) { if (isForward) -it / 3 else it / 3 } +
                            fadeOut(animationSpec = tween(200)) +
                            scaleOut(targetScale = 0.94f)

                    enterAnim togetherWith exitAnim
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "liquid_smooth_tab_transition"
            ) { tab ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        when (tab) {
                            FoxyTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToTests = { currentTab = FoxyTab.TESTS },
                                onNavigateToMonitor = { currentTab = FoxyTab.MONITOR },
                                onNavigateToCpuSimulator = { showCpuSimulator = true },
                                onOpenRefreshRateMenu = { showRefreshRateDialog = true }
                            )
                            FoxyTab.MONITOR -> MonitorScreen(
                                viewModel = viewModel,
                                onNavigateToCpuSimulator = { showCpuSimulator = true },
                                onOpenRefreshRateMenu = { showRefreshRateDialog = true }
                            )
                            FoxyTab.TESTS -> DiagnosticsScreen(viewModel = viewModel)
                            FoxyTab.APPS -> AppsScreen(viewModel = viewModel)
                            FoxyTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onOpenRefreshRateMenu = { showRefreshRateDialog = true }
                            )
                        }
                    }
                }
            }
        }

        if (isFpsOverlayEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                RefreshRateFloatingPill(
                    fps = liveFps,
                    displayHz = displayHz,
                    onClick = { showRefreshRateDialog = true }
                )
            }
        }

        if (showRefreshRateDialog) {
            RefreshRateMenuDialog(
                viewModel = viewModel,
                onDismissRequest = { showRefreshRateDialog = false }
            )
        }
    }
}

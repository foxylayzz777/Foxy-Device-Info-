package com.example

import android.content.Intent
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LiquidGlassAmbientBackdrop
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
        handleIntentTab(intent)
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

    LaunchedEffect(externalTab) {
        if (externalTab != null) {
            currentTab = externalTab
        }
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
                        Row {
                            Text(
                                text = "Foxy Device Info",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
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
                when (tab) {
                    FoxyTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToTests = { currentTab = FoxyTab.TESTS },
                        onNavigateToMonitor = { currentTab = FoxyTab.MONITOR }
                    )
                    FoxyTab.MONITOR -> MonitorScreen(viewModel = viewModel)
                    FoxyTab.TESTS -> DiagnosticsScreen(viewModel = viewModel)
                    FoxyTab.APPS -> AppsScreen(viewModel = viewModel)
                    FoxyTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

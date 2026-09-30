package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FoxyViewModel = viewModel()
            val themeStyle by viewModel.themeStyle.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            FoxyTheme(themeStyle = themeStyle, themeMode = themeMode) {
                FoxyApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoxyApp(viewModel: FoxyViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(FoxyTab.HOME) }

    // If on non-home tab, back button returns to Home first
    BackHandler(enabled = currentTab != FoxyTab.HOME) {
        currentTab = FoxyTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text("Foxy Device Info", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDeviceInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh info")
                    }
                    IconButton(onClick = { viewModel.exportReport(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tab_transition"
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

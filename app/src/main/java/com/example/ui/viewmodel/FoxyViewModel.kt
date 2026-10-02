package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.provider.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoxyViewModel(application: Application) : AndroidViewModel(application) {

    private val systemInfoProvider = SystemInfoProvider(application)
    private val realtimeMonitorManager = RealtimeMonitorManager(application)
    private val diagnosticsManager = HardwareDiagnosticsManager(application)
    private val appAnalyzerManager = AppAnalyzerManager(application)
    private val benchmarkRunner = BenchmarkRunner(application)
    private val cpuLoadSimulator = CpuLoadSimulator(application)

    // Full device specs
    private val _deviceInfo = MutableStateFlow<FullDeviceInfo?>(null)
    val deviceInfo: StateFlow<FullDeviceInfo?> = _deviceInfo.asStateFlow()

    // Realtime live telemetry
    private val _realtimeMetrics = MutableStateFlow(realtimeMonitorManager.readCurrentMetrics())
    val realtimeMetrics: StateFlow<RealtimeMetrics> = _realtimeMetrics.asStateFlow()

    // Hardware Diagnostics
    private val _diagnosticTests = MutableStateFlow(diagnosticsManager.getInitialTestList())
    val diagnosticTests: StateFlow<List<DiagnosticTestItem>> = _diagnosticTests.asStateFlow()

    private val _activeInteractiveTest = MutableStateFlow<TestType?>(null)
    val activeInteractiveTest: StateFlow<TestType?> = _activeInteractiveTest.asStateFlow()

    // Apps & Permissions
    private val _rawInstalledApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    private val _appsSearchQuery = MutableStateFlow("")
    val appsSearchQuery: StateFlow<String> = _appsSearchQuery.asStateFlow()

    private val _filterIncludeSystem = MutableStateFlow(false)
    val filterIncludeSystem: StateFlow<Boolean> = _filterIncludeSystem.asStateFlow()

    val filteredApps: StateFlow<List<AppInfoItem>> = combine(
        _rawInstalledApps,
        _appsSearchQuery,
        _filterIncludeSystem
    ) { apps, query, includeSystem ->
        apps.filter { app ->
            (includeSystem || !app.isSystemApp) &&
                    (query.isBlank() ||
                            app.appName.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _permissionSummaries = MutableStateFlow<List<PermissionCategorySummary>>(emptyList())
    val permissionSummaries: StateFlow<List<PermissionCategorySummary>> = _permissionSummaries.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Benchmark
    private val _benchmarkResult = MutableStateFlow<BenchmarkResult?>(null)
    val benchmarkResult: StateFlow<BenchmarkResult?> = _benchmarkResult.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _benchmarkProgress = MutableStateFlow(Pair("Ready to benchmark", 0.0f))
    val benchmarkProgress: StateFlow<Pair<String, Float>> = _benchmarkProgress.asStateFlow()

    // CPU Load Simulation & Benchmark
    private val _cpuSimulationProgress = MutableStateFlow(CpuSimulationProgress())
    val cpuSimulationProgress: StateFlow<CpuSimulationProgress> = _cpuSimulationProgress.asStateFlow()

    private val _cpuSimulationResult = MutableStateFlow<CpuLoadSimulationResult?>(null)
    val cpuSimulationResult: StateFlow<CpuLoadSimulationResult?> = _cpuSimulationResult.asStateFlow()

    private var simulationJob: Job? = null

    // Floating HUD mini overlay state
    private val _isFloatingHudEnabled = MutableStateFlow(false)
    val isFloatingHudEnabled: StateFlow<Boolean> = _isFloatingHudEnabled.asStateFlow()

    // Persistent Theme Settings
    private val prefs by lazy {
        application.getSharedPreferences("foxy_device_info_prefs", android.content.Context.MODE_PRIVATE)
    }

    private fun loadSavedThemeStyle(): com.example.ui.theme.AppThemeStyle {
        val savedName = prefs.getString("pref_theme_style", com.example.ui.theme.AppThemeStyle.LIQUID_GLASS.name)
        return try {
            com.example.ui.theme.AppThemeStyle.valueOf(savedName ?: com.example.ui.theme.AppThemeStyle.LIQUID_GLASS.name)
        } catch (_: Exception) {
            com.example.ui.theme.AppThemeStyle.LIQUID_GLASS
        }
    }

    private fun loadSavedThemeMode(): com.example.ui.theme.AppThemeMode {
        val savedName = prefs.getString("pref_theme_mode", com.example.ui.theme.AppThemeMode.SYSTEM.name)
        return try {
            com.example.ui.theme.AppThemeMode.valueOf(savedName ?: com.example.ui.theme.AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            com.example.ui.theme.AppThemeMode.SYSTEM
        }
    }

    // Themes (Default is LIQUID_GLASS, persisted across app restarts)
    private val _themeStyle = MutableStateFlow(loadSavedThemeStyle())
    val themeStyle: StateFlow<com.example.ui.theme.AppThemeStyle> = _themeStyle.asStateFlow()

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())
    val themeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _themeMode.asStateFlow()

    // 90Hz & High Refresh Rate Display Controls
    private fun loadSavedRefreshRatePreference(): com.example.ui.components.RefreshRatePreference {
        val saved = prefs.getString("pref_refresh_rate_mode", com.example.ui.components.RefreshRatePreference.FORCE_90.name)
        return try {
            com.example.ui.components.RefreshRatePreference.valueOf(saved ?: com.example.ui.components.RefreshRatePreference.FORCE_90.name)
        } catch (_: Exception) {
            com.example.ui.components.RefreshRatePreference.FORCE_90
        }
    }

    private val _refreshRatePreference = MutableStateFlow(loadSavedRefreshRatePreference())
    val refreshRatePreference: StateFlow<com.example.ui.components.RefreshRatePreference> = _refreshRatePreference.asStateFlow()

    private val _customDisplayModeId = MutableStateFlow(prefs.getInt("pref_custom_mode_id", -1))
    val customDisplayModeId: StateFlow<Int> = _customDisplayModeId.asStateFlow()

    private val _isFpsOverlayEnabled = MutableStateFlow(prefs.getBoolean("pref_fps_overlay_enabled", false))
    val isFpsOverlayEnabled: StateFlow<Boolean> = _isFpsOverlayEnabled.asStateFlow()

    fun setRefreshRatePreference(pref: com.example.ui.components.RefreshRatePreference, customModeId: Int? = null) {
        _refreshRatePreference.value = pref
        _customDisplayModeId.value = customModeId ?: -1
        prefs.edit()
            .putString("pref_refresh_rate_mode", pref.name)
            .putInt("pref_custom_mode_id", customModeId ?: -1)
            .apply()
    }

    fun toggleFpsOverlay(enabled: Boolean? = null) {
        val next = enabled ?: !_isFpsOverlayEnabled.value
        _isFpsOverlayEnabled.value = next
        prefs.edit().putBoolean("pref_fps_overlay_enabled", next).apply()
    }

    private var monitorJob: Job? = null

    init {
        refreshDeviceInfo()
        startMetricsCollection()
        loadApps()
    }

    fun setThemeStyle(style: com.example.ui.theme.AppThemeStyle) {
        _themeStyle.value = style
        prefs.edit().putString("pref_theme_style", style.name).apply()
    }

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("pref_theme_mode", mode.name).apply()
    }

    fun refreshDeviceInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _deviceInfo.value = systemInfoProvider.getFullDeviceInfo()
        }
    }

    private fun startMetricsCollection() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch(Dispatchers.Default) {
            realtimeMonitorManager.getMetricsFlow(1000L).collect { metrics ->
                _realtimeMetrics.value = metrics
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = appAnalyzerManager.getInstalledApps()
            _rawInstalledApps.value = apps
            _permissionSummaries.value = appAnalyzerManager.getPermissionSummaries(apps)
            _isLoadingApps.value = false
        }
    }

    fun setAppsSearchQuery(query: String) {
        _appsSearchQuery.value = query
    }

    fun setFilterIncludeSystem(include: Boolean) {
        _filterIncludeSystem.value = include
    }

    // Diagnostics Test handling
    fun setTestStatus(type: TestType, status: TestStatus, details: String? = null) {
        _diagnosticTests.value = _diagnosticTests.value.map { item ->
            if (item.type == type) {
                item.copy(
                    status = status,
                    details = details,
                    lastTestedTime = System.currentTimeMillis()
                )
            } else item
        }
        if (_activeInteractiveTest.value == type) {
            _activeInteractiveTest.value = null
        }
    }

    fun openInteractiveTest(type: TestType) {
        _activeInteractiveTest.value = type
    }

    fun closeInteractiveTest() {
        _activeInteractiveTest.value = null
    }

    fun resetAllTests() {
        _diagnosticTests.value = diagnosticsManager.getInitialTestList()
    }

    // Benchmark Execution
    fun runBenchmark() {
        if (_isBenchmarking.value) return
        viewModelScope.launch {
            _isBenchmarking.value = true
            try {
                val res = benchmarkRunner.runBenchmark { step, pct ->
                    _benchmarkProgress.value = Pair(step, pct)
                }
                _benchmarkResult.value = res
            } catch (e: Exception) {
                _benchmarkProgress.value = Pair("Benchmark failed: ${e.message}", 0f)
            } finally {
                _isBenchmarking.value = false
            }
        }
    }

    // CPU Load Simulation Execution
    fun startCpuLoadSimulation(
        durationSec: Int = 10,
        threadsCount: Int = Runtime.getRuntime().availableProcessors(),
        intensityPct: Int = 100
    ) {
        if (_cpuSimulationProgress.value.isRunning) return
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            try {
                val res = cpuLoadSimulator.runSimulation(
                    durationSeconds = durationSec,
                    threadsCount = threadsCount,
                    intensityPercent = intensityPct
                ) { progress ->
                    _cpuSimulationProgress.value = progress
                }
                _cpuSimulationResult.value = res
            } catch (_: Exception) {
                _cpuSimulationProgress.value = _cpuSimulationProgress.value.copy(
                    isRunning = false,
                    statusMessage = "Simulation interrupted"
                )
            }
        }
    }

    fun stopCpuLoadSimulation() {
        cpuLoadSimulator.cancelSimulation()
        simulationJob?.cancel()
        _cpuSimulationProgress.value = _cpuSimulationProgress.value.copy(
            isRunning = false,
            statusMessage = "Simulation stopped"
        )
    }

    fun clearCpuSimulationResult() {
        _cpuSimulationResult.value = null
        _cpuSimulationProgress.value = CpuSimulationProgress()
    }

    fun toggleFloatingHud() {
        _isFloatingHudEnabled.value = !_isFloatingHudEnabled.value
    }

    fun triggerVibration() {
        diagnosticsManager.testVibration()
    }

    fun toggleTorch(enable: Boolean): Boolean {
        return diagnosticsManager.toggleTorch(enable)
    }

    fun startMicrophoneListener(onAmplitude: (Float) -> Unit): AutoCloseable? {
        return diagnosticsManager.startMicrophoneListener(onAmplitude)
    }

    suspend fun playAudioTone(frequency: Int = 440, durationMs: Int = 1000, isEarpiece: Boolean = false) {
        diagnosticsManager.playTestTone(frequency, durationMs, isEarpiece)
    }

    fun checkCharging(): Boolean {
        return diagnosticsManager.checkChargingState()
    }

    fun checkHeadset(): Boolean {
        return diagnosticsManager.checkHeadsetPlugged()
    }

    fun exportReport(context: android.content.Context) {
        val info = _deviceInfo.value ?: systemInfoProvider.getFullDeviceInfo()
        val md = DeviceReportExporter.generateMarkdownReport(info, _benchmarkResult.value)
        DeviceReportExporter.shareReport(context, md)
    }
}

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

    // Floating HUD mini overlay state
    private val _isFloatingHudEnabled = MutableStateFlow(false)
    val isFloatingHudEnabled: StateFlow<Boolean> = _isFloatingHudEnabled.asStateFlow()

    private var monitorJob: Job? = null

    init {
        refreshDeviceInfo()
        startMetricsCollection()
        loadApps()
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

    fun toggleFloatingHud() {
        _isFloatingHudEnabled.value = !_isFloatingHudEnabled.value
    }

    fun triggerVibration() {
        diagnosticsManager.testVibration()
    }

    fun toggleTorch(enable: Boolean): Boolean {
        return diagnosticsManager.toggleTorch(enable)
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

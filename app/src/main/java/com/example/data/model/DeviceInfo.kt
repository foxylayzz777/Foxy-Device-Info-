package com.example.data.model

data class DeviceSummary(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val deviceName: String,
    val product: String,
    val board: String,
    val hardware: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildId: String,
    val kernelVersion: String,
    val uptimeMillis: Long,
    val androidCodename: String = "Current Release",
    val buildFingerprint: String = "",
    val buildType: String = "user",
    val buildTags: String = "release-keys",
    val bootloaderVersion: String = "unknown",
    val radioVersion: String = "unknown",
    val javaVmVersion: String = "ART",
    val isRooted: Boolean = false,
    val isTrebleSupported: Boolean = true,
    val isSeamlessUpdateSupported: Boolean = true,
    val selinuxStatus: String = "Enforcing"
)

data class CpuSpec(
    val socName: String,
    val architecture: String,
    val totalCores: Int,
    val supportedAbis: List<String>,
    val instructionSets: String,
    val governor: String,
    val minFreqMhz: Int,
    val maxFreqMhz: Int,
    val clustersDescription: String = "",
    val coreMicroarchitecture: String = "",
    val features: List<String> = emptyList(),
    val cpuImplementer: String = "",
    val cpuPart: String = "",
    val bogoMips: String = "",
    val hardwareBoard: String = "",
    val is64Bit: Boolean = true,
    val cacheInfo: String = "",
    val processNodeEstimated: String = ""
)

data class GpuDisplaySpec(
    val renderer: String,
    val vendor: String,
    val resolution: String,
    val refreshRateHz: Float,
    val densityDpi: Int,
    val screenPhysicalInches: String,
    val hdrCapabilities: String,
    val isHdrSupported: Boolean,
    val aspectRatio: String = "20:9",
    val xdpi: Float = 420f,
    val ydpi: Float = 420f,
    val densityBucket: String = "xxhdpi",
    val supportedRefreshRates: List<Float> = listOf(60f, 90f, 120f),
    val isWideColorGamutSupported: Boolean = true
)

data class MemoryStorageSpec(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val ramLowMemory: Boolean,
    val ramTypeEstimated: String = "LPDDR5 / LPDDR4X Unified",
    val zramSizeBytes: Long = 0L,
    val filesystemType: String = "f2fs / ext4"
)

data class DrmSecuritySpec(
    val widevineSecurityLevel: String = "L1 (Highest Security)",
    val widevineVendor: String = "Google Inc.",
    val widevineVersion: String = "16.0.0",
    val deviceEncryptionStatus: String = "File-Based Encryption (FBE)",
    val strongBoxAvailable: Boolean = true,
    val biometricHardware: String = "Fingerprint & Biometric Face"
)

data class AudioMediaSpec(
    val audioOutputs: String = "Stereo Speakers / USB-C / Bluetooth Audio",
    val spatialAudioSupported: Boolean = true,
    val hiResAudioSupported: Boolean = true,
    val supportedVideoDecoders: List<String> = listOf("AV1", "HEVC/H.265", "AVC/H.264", "VP9", "MPEG-4"),
    val supportedVideoEncoders: List<String> = listOf("HEVC/H.265", "AVC/H.264", "VP8")
)

data class BatterySpec(
    val levelPercent: Int,
    val isCharging: Boolean,
    val chargingSource: String, // AC, USB, Wireless
    val health: String, // Good, Overheat, etc.
    val technology: String,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val capacityMah: Double,
    val fastChargingStatus: String = "Fast Charging Supported"
)

data class NetworkSpec(
    val isConnected: Boolean,
    val networkType: String, // Wi-Fi, Cellular, Ethernet, None
    val ipAddress: String,
    val ipv6Address: String,
    val wifiSsid: String,
    val wifiBssid: String,
    val wifiRssiDbm: Int,
    val wifiLinkSpeedMbps: Int,
    val wifiFrequencyMhz: Int,
    val isWifi6Supported: Boolean,
    val is5GhzSupported: Boolean
)

data class SensorItem(
    val id: Int,
    val name: String,
    val vendor: String,
    val typeName: String,
    val powerMa: Float,
    val maxRange: Float,
    val resolution: Float
)

data class CameraSpec(
    val cameraId: String,
    val facing: String, // Front, Back, External
    val resolutionMegapixels: Float,
    val sensorSize: String,
    val flashAvailable: Boolean,
    val maxZoom: Float,
    val focalLengths: String
)

data class CapabilitiesSpec(
    val hasNfc: Boolean,
    val hasBluetooth: Boolean,
    val hasBluetoothLe: Boolean,
    val hasFingerprint: Boolean,
    val hasFaceAuth: Boolean,
    val hasUsbHost: Boolean,
    val hasVibrator: Boolean,
    val hasCameraFlash: Boolean,
    val hasUwb: Boolean = false,
    val hasEsim: Boolean = false,
    val has5gTelephony: Boolean = false,
    val hasWifiDirect: Boolean = true,
    val hasWifiAware: Boolean = false,
    val hasWifiRtt: Boolean = false,
    val hasMidi: Boolean = true,
    val hasLowLatencyAudio: Boolean = true,
    val hasProAudio: Boolean = false,
    val multiTouchPoints: Int = 10,
    val hasSustainedPerformance: Boolean = true,
    val hasHdrDisplay: Boolean = true,
    val hasHapticFeedback: Boolean = true
)

data class GnssLocationSpec(
    val hasGps: Boolean = true,
    val constellations: List<String> = listOf("GPS (L1/L5)", "GLONASS (G1/G2)", "Galileo (E1/E5a)", "BeiDou (B1/B2a)", "QZSS (L1/L5)", "NavIC (India)"),
    val hasGnssMeasurements: Boolean = true,
    val hasDualFrequency: Boolean = true,
    val supportedProviders: String = "GPS, Network Cell/Wi-Fi, Fused Location Provider",
    val hasGeofencing: Boolean = true,
    val hasGnssAntennaInfo: Boolean = false
)

data class VulkanSpec(
    val isVulkanSupported: Boolean,
    val apiVersionString: String,
    val majorVersion: Int,
    val minorVersion: Int,
    val patchVersion: Int,
    val hardwareLevel: Int, // 0 = Level 0 (basic), 1 = Level 1 (full hardware)
    val hardwareComputeLevel: Int,
    val is64BitAbi: Boolean,
    val isVulkanHardwareAccelerated: Boolean = false,
    val vulkanDriverStatus: String = "Not Supported",
    val compatibilityDetails: List<String>,
    @Deprecated("Replaced by vulkanDriverStatus")
    val isVulkanModSupported: Boolean = isVulkanHardwareAccelerated,
    @Deprecated("Replaced by vulkanDriverStatus")
    val vulkanModStatus: String = vulkanDriverStatus
)

data class FullDeviceInfo(
    val summary: DeviceSummary,
    val cpu: CpuSpec,
    val gpuDisplay: GpuDisplaySpec,
    val memoryStorage: MemoryStorageSpec,
    val battery: BatterySpec,
    val network: NetworkSpec,
    val cameras: List<CameraSpec>,
    val sensors: List<SensorItem>,
    val capabilities: CapabilitiesSpec,
    val vulkan: VulkanSpec = VulkanSpec(
        isVulkanSupported = false,
        apiVersionString = "Not Supported",
        majorVersion = 0,
        minorVersion = 0,
        patchVersion = 0,
        hardwareLevel = -1,
        hardwareComputeLevel = -1,
        is64BitAbi = false,
        isVulkanHardwareAccelerated = false,
        vulkanDriverStatus = "Not Supported",
        compatibilityDetails = emptyList()
    ),
    val drmSecurity: DrmSecuritySpec = DrmSecuritySpec(),
    val audioMedia: AudioMediaSpec = AudioMediaSpec(),
    val gnssLocation: GnssLocationSpec = GnssLocationSpec()
)

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
    val uptimeMillis: Long
)

data class CpuSpec(
    val socName: String,
    val architecture: String,
    val totalCores: Int,
    val supportedAbis: List<String>,
    val instructionSets: String,
    val governor: String,
    val minFreqMhz: Int,
    val maxFreqMhz: Int
)

data class GpuDisplaySpec(
    val renderer: String,
    val vendor: String,
    val resolution: String,
    val refreshRateHz: Float,
    val densityDpi: Int,
    val screenPhysicalInches: String,
    val hdrCapabilities: String,
    val isHdrSupported: Boolean
)

data class MemoryStorageSpec(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val ramLowMemory: Boolean
)

data class BatterySpec(
    val levelPercent: Int,
    val isCharging: Boolean,
    val chargingSource: String, // AC, USB, Wireless
    val health: String, // Good, Overheat, etc.
    val technology: String,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val capacityMah: Double
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
    val hasCameraFlash: Boolean
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
    val capabilities: CapabilitiesSpec
)

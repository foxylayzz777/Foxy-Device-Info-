package com.example.data.provider

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.example.data.model.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.Collections
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SystemInfoProvider(private val context: Context) {

    fun getFullDeviceInfo(): FullDeviceInfo {
        return FullDeviceInfo(
            summary = getDeviceSummary(),
            cpu = getCpuSpec(),
            gpuDisplay = getGpuDisplaySpec(),
            memoryStorage = getMemoryStorageSpec(),
            battery = getBatterySpec(),
            network = getNetworkSpec(),
            cameras = getCameraSpecs(),
            sensors = getSensors(),
            capabilities = getCapabilities()
        )
    }

    fun getDeviceSummary(): DeviceSummary {
        val kernelVersion = System.getProperty("os.version") ?: "Linux"
        val uptime = SystemClock.elapsedRealtime()

        return DeviceSummary(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            brand = Build.BRAND.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            deviceName = Build.DEVICE,
            product = Build.PRODUCT,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH ?: "N/A"
            } else "N/A",
            buildId = Build.DISPLAY,
            kernelVersion = kernelVersion,
            uptimeMillis = uptime
        )
    }

    fun getCpuSpec(): CpuSpec {
        val totalCores = Runtime.getRuntime().availableProcessors()
        val arch = System.getProperty("os.arch") ?: "arm64-v8a"
        val abis = Build.SUPPORTED_ABIS.toList()

        var socName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socMan = Build.SOC_MANUFACTURER
            val socMod = Build.SOC_MODEL
            if (socMan.isNotEmpty() || socMod.isNotEmpty()) "$socMan $socMod".trim() else ""
        } else ""

        if (socName.isEmpty()) {
            socName = readCpuInfoHardware() ?: "${Build.HARDWARE} (${Build.BOARD})"
        }

        var minFreq = 0
        var maxFreq = 0
        var governor = "interactive"

        try {
            val minFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
            if (minFreqFile.exists()) {
                minFreq = (minFreqFile.readText().trim().toLongOrNull() ?: 0L).toInt() / 1000
            }
            val maxFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (maxFreqFile.exists()) {
                maxFreq = (maxFreqFile.readText().trim().toLongOrNull() ?: 0L).toInt() / 1000
            }
            val govFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            if (govFile.exists()) {
                governor = govFile.readText().trim()
            }
        } catch (_: Exception) {
            // fallback
        }

        if (maxFreq == 0) {
            maxFreq = 2400
            minFreq = 300
        }

        return CpuSpec(
            socName = socName,
            architecture = arch,
            totalCores = totalCores,
            supportedAbis = abis,
            instructionSets = abis.firstOrNull() ?: "arm64-v8a",
            governor = governor,
            minFreqMhz = minFreq,
            maxFreqMhz = maxFreq
        )
    }

    private fun readCpuInfoHardware(): String? {
        try {
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("Hardware", ignoreCase = true) || line!!.startsWith("model name", ignoreCase = true)) {
                    val parts = line!!.split(":")
                    if (parts.size > 1) {
                        val hw = parts[1].trim()
                        if (hw.isNotEmpty()) {
                            reader.close()
                            return hw
                        }
                    }
                }
            }
            reader.close()
        } catch (_: Exception) {}
        return null
    }

    fun getGpuDisplaySpec(): GpuDisplaySpec {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = context.resources.displayMetrics

        var width = metrics.widthPixels
        var height = metrics.heightPixels
        var refreshRate = 60f
        var isHdr = false
        var hdrCaps = "SDR"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val bounds = wm.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        }

        if (wm != null) {
            @Suppress("DEPRECATION")
            val defaultDisplay = wm.defaultDisplay
            if (defaultDisplay != null) {
                refreshRate = defaultDisplay.refreshRate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val caps = defaultDisplay.hdrCapabilities
                    if (caps != null && caps.supportedHdrTypes.isNotEmpty()) {
                        isHdr = true
                        hdrCaps = "HDR10, HLG, Dolby Vision"
                    }
                }
            }
        }

        val xDpi = if (metrics.xdpi > 0) metrics.xdpi else metrics.densityDpi.toFloat()
        val yDpi = if (metrics.ydpi > 0) metrics.ydpi else metrics.densityDpi.toFloat()
        val widthInches = width / xDpi
        val heightInches = height / yDpi
        val diagonalInches = String.format("%.1f\"", sqrt((widthInches * widthInches + heightInches * heightInches).toDouble()))

        return GpuDisplaySpec(
            renderer = "Qualcomm Adreno / ARM Mali / OpenGL ES",
            vendor = Build.MANUFACTURER,
            resolution = "${width} x ${height} px",
            refreshRateHz = (refreshRate * 10).roundToInt() / 10f,
            densityDpi = metrics.densityDpi,
            screenPhysicalInches = diagonalInches,
            hdrCapabilities = hdrCaps,
            isHdrSupported = isHdr
        )
    }

    fun getMemoryStorageSpec(): MemoryStorageSpec {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem
        val isLowMem = memInfo.lowMemory

        var totalStorage = 0L
        var availStorage = 0L
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            totalStorage = statFs.totalBytes
            availStorage = statFs.availableBytes
        } catch (_: Exception) {
            totalStorage = 64L * 1024 * 1024 * 1024
            availStorage = 32L * 1024 * 1024 * 1024
        }

        return MemoryStorageSpec(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            totalStorageBytes = totalStorage,
            availableStorageBytes = availStorage,
            ramLowMemory = isLowMem
        )
    }

    fun getBatterySpec(): BatterySpec {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct: Int = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 50

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugSource = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi"
            else -> if (isCharging) "Charger" else "Not Charging (On Battery)"
        }

        val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthStr = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good (Optimal)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good (Healthy)"
        }

        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280) / 10.0f
        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000
        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        // Estimated design capacity
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var capacityMah = 4500.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bm != null) {
            val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (chargeCounter > 0) {
                capacityMah = chargeCounter / 1000.0
            }
        }

        return BatterySpec(
            levelPercent = batteryPct,
            isCharging = isCharging,
            chargingSource = plugSource,
            health = healthStr,
            technology = technology,
            temperatureCelsius = temp,
            voltageMv = voltageMv,
            capacityMah = capacityMah
        )
    }

    fun getNetworkSpec(): NetworkSpec {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)

        val isConnected = caps != null && (
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )

        val netType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile Data)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth Tethering"
            else -> "Connected"
        }

        var ipV4 = "127.0.0.1"
        var ipV6 = "::1"
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (addr is Inet4Address && ipV4 == "127.0.0.1") {
                            ipV4 = host
                        } else if (addr is Inet6Address && ipV6 == "::1") {
                            val delim = host.indexOf('%')
                            ipV6 = if (delim < 0) host.uppercase() else host.substring(0, delim).uppercase()
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        var ssid = "<Connected Wi-Fi>"
        var bssid = "--:--:--:--:--:--"
        var rssi = -60
        var linkSpeed = 150
        var freq = 5180
        var isWifi6 = false
        var is5Ghz = true

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wm != null && wm.isWifiEnabled) {
            val connInfo: WifiInfo? = wm.connectionInfo
            if (connInfo != null) {
                val rawSsid = connInfo.ssid
                if (!rawSsid.isNullOrEmpty() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid.removePrefix("\"").removeSuffix("\"")
                }
                bssid = connInfo.bssid ?: bssid
                rssi = connInfo.rssi
                linkSpeed = connInfo.linkSpeed
                freq = connInfo.frequency
                is5Ghz = freq > 4900
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    isWifi6 = connInfo.wifiStandard == 6 // ScanResult.WIFI_STANDARD_11AX
                }
            }
        }

        return NetworkSpec(
            isConnected = isConnected || caps != null,
            networkType = netType,
            ipAddress = ipV4,
            ipv6Address = ipV6,
            wifiSsid = ssid,
            wifiBssid = bssid,
            wifiRssiDbm = rssi,
            wifiLinkSpeedMbps = linkSpeed,
            wifiFrequencyMhz = freq,
            isWifi6Supported = isWifi6,
            is5GhzSupported = is5Ghz
        )
    }

    fun getCameraSpecs(): List<CameraSpec> {
        val list = mutableListOf<CameraSpec>()
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return list

        try {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val facingCode = chars.get(CameraCharacteristics.LENS_FACING)
                val facing = when (facingCode) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front (Selfie)"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back (Primary)"
                    else -> "External"
                }

                val arraySize = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                var megaPixels = 12.0f
                var sensorDimensions = "Unknown"
                if (arraySize != null) {
                    val w = arraySize.width()
                    val h = arraySize.height()
                    megaPixels = (w * h) / 1_000_000f
                    megaPixels = (megaPixels * 10).roundToInt() / 10f
                    sensorDimensions = "${w} x ${h} px"
                }

                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                val focalArray = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focals = focalArray?.joinToString(", ") { "${it}mm" } ?: "Fixed"

                list.add(
                    CameraSpec(
                        cameraId = id,
                        facing = facing,
                        resolutionMegapixels = megaPixels,
                        sensorSize = sensorDimensions,
                        flashAvailable = flash,
                        maxZoom = maxZoom,
                        focalLengths = focals
                    )
                )
            }
        } catch (_: Exception) {}

        return list
    }

    fun getSensors(): List<SensorItem> {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return emptyList()
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)

        return sensors.map { s ->
            SensorItem(
                id = s.type,
                name = s.name,
                vendor = s.vendor,
                typeName = s.stringType ?: "sensor.type.${s.type}",
                powerMa = s.power,
                maxRange = s.maximumRange,
                resolution = s.resolution
            )
        }
    }

    fun getCapabilities(): CapabilitiesSpec {
        val pm = context.packageManager
        val nfc = NfcAdapter.getDefaultAdapter(context) != null
        val hasBt = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val hasBle = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else false
        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val hasVib = vibrator?.hasVibrator() ?: false
        val hasFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        return CapabilitiesSpec(
            hasNfc = nfc,
            hasBluetooth = hasBt,
            hasBluetoothLe = hasBle,
            hasFingerprint = hasFingerprint,
            hasFaceAuth = hasFace,
            hasUsbHost = hasUsbHost,
            hasVibrator = hasVib,
            hasCameraFlash = hasFlash
        )
    }
}

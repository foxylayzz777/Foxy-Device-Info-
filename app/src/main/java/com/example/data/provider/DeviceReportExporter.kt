package com.example.data.provider

import android.content.Context
import android.content.Intent
import com.example.data.model.BenchmarkResult
import com.example.data.model.FullDeviceInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceReportExporter {

    fun generateMarkdownReport(deviceInfo: FullDeviceInfo, benchmark: BenchmarkResult?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val ramTotalGb = String.format(Locale.US, "%.2f GB", deviceInfo.memoryStorage.totalRamBytes / (1024.0 * 1024.0 * 1024.0))
        val storageTotalGb = String.format(Locale.US, "%.1f GB", deviceInfo.memoryStorage.totalStorageBytes / (1024.0 * 1024.0 * 1024.0))

        return buildString {
            appendLine("# 🦊 Foxy Device Info — Hardware Specification Report")
            appendLine("Generated on: $dateStr")
            appendLine("App: Foxy Device Info | 100% On-Device Privacy")
            appendLine()
            appendLine("## 📱 Device Summary")
            appendLine("- **Manufacturer:** ${deviceInfo.summary.manufacturer}")
            appendLine("- **Brand:** ${deviceInfo.summary.brand}")
            appendLine("- **Model:** ${deviceInfo.summary.model}")
            appendLine("- **Device / Codename:** ${deviceInfo.summary.deviceName}")
            appendLine("- **Board / Hardware:** ${deviceInfo.summary.board} / ${deviceInfo.summary.hardware}")
            appendLine("- **Android OS:** ${deviceInfo.summary.androidVersion} (API ${deviceInfo.summary.apiLevel})")
            appendLine("- **Security Patch:** ${deviceInfo.summary.securityPatch}")
            appendLine("- **Kernel Version:** ${deviceInfo.summary.kernelVersion}")
            appendLine("- **Build ID:** ${deviceInfo.summary.buildId}")
            appendLine("- **Android Codename:** ${deviceInfo.summary.androidCodename}")
            appendLine("- **Bootloader:** ${deviceInfo.summary.bootloaderVersion}")
            appendLine("- **Baseband / Radio:** ${deviceInfo.summary.radioVersion}")
            appendLine("- **SELinux Status:** ${deviceInfo.summary.selinuxStatus}")
            appendLine("- **Root Access:** ${if (deviceInfo.summary.isRooted) "Rooted / Custom Keys" else "No (Official Unrooted)"}")
            appendLine("- **Treble / A-B Updates:** ${if (deviceInfo.summary.isTrebleSupported) "Treble Supported" else "Legacy"} • ${if (deviceInfo.summary.isSeamlessUpdateSupported) "Seamless A/B" else "Standard"}")
            appendLine()
            appendLine("## ⚡ Processor & SoC")
            appendLine("- **SoC / Chipset:** ${deviceInfo.cpu.socName}")
            appendLine("- **CPU Cores:** ${deviceInfo.cpu.totalCores} cores")
            appendLine("- **Architecture:** ${deviceInfo.cpu.architecture}")
            appendLine("- **Supported ABIs:** ${deviceInfo.cpu.supportedAbis.joinToString(", ")}")
            appendLine("- **Scaling Governor:** ${deviceInfo.cpu.governor}")
            appendLine("- **Frequency Range:** ${deviceInfo.cpu.minFreqMhz} MHz - ${deviceInfo.cpu.maxFreqMhz} MHz")
            appendLine()
            appendLine("## 💾 Memory & Storage")
            appendLine("- **RAM:** $ramTotalGb")
            appendLine("- **Internal Storage:** $storageTotalGb")
            appendLine()
            appendLine("## 🖥️ GPU & Display")
            appendLine("- **Resolution:** ${deviceInfo.gpuDisplay.resolution}")
            appendLine("- **Refresh Rate:** ${deviceInfo.gpuDisplay.refreshRateHz} Hz")
            appendLine("- **Screen Density:** ${deviceInfo.gpuDisplay.densityDpi} DPI")
            appendLine("- **Physical Size:** ~${deviceInfo.gpuDisplay.screenPhysicalInches} (${deviceInfo.gpuDisplay.aspectRatio})")
            appendLine("- **Density Bucket:** ${deviceInfo.gpuDisplay.densityBucket} (xdpi: ${deviceInfo.gpuDisplay.xdpi}, ydpi: ${deviceInfo.gpuDisplay.ydpi})")
            appendLine("- **Supported Refresh Rates:** ${deviceInfo.gpuDisplay.supportedRefreshRates.joinToString(", ") { "${it}Hz" }}")
            appendLine("- **Wide Color Gamut (P3):** ${if (deviceInfo.gpuDisplay.isWideColorGamutSupported) "Yes" else "Standard sRGB"}")
            appendLine("- **HDR Support:** ${if (deviceInfo.gpuDisplay.isHdrSupported) "Yes (${deviceInfo.gpuDisplay.hdrCapabilities})" else "SDR"}")
            appendLine()
            appendLine("## 🔒 Security & Widevine DRM")
            appendLine("- **Widevine DRM Level:** ${deviceInfo.drmSecurity.widevineSecurityLevel}")
            appendLine("- **Widevine Vendor / Version:** ${deviceInfo.drmSecurity.widevineVendor} (v${deviceInfo.drmSecurity.widevineVersion})")
            appendLine("- **Device Encryption:** ${deviceInfo.drmSecurity.deviceEncryptionStatus}")
            appendLine("- **StrongBox Keystore:** ${if (deviceInfo.drmSecurity.strongBoxAvailable) "Hardware StrongBox Keymaster" else "Standard TEE Keymaster"}")
            appendLine("- **Biometric Hardware:** ${deviceInfo.drmSecurity.biometricHardware}")
            appendLine()
            appendLine("## 🎵 Audio & Media Codecs")
            appendLine("- **Audio Routes:** ${deviceInfo.audioMedia.audioOutputs}")
            appendLine("- **Spatial Audio:** ${if (deviceInfo.audioMedia.spatialAudioSupported) "Supported" else "Standard Stereo"}")
            appendLine("- **Hardware Decoders:** ${deviceInfo.audioMedia.supportedVideoDecoders.joinToString(", ")}")
            appendLine("- **Hardware Encoders:** ${deviceInfo.audioMedia.supportedVideoEncoders.joinToString(", ")}")
            appendLine()
            appendLine("## 🎮 Vulkan Graphics API Specifications")
            appendLine("- **Vulkan API Version:** ${deviceInfo.vulkan.apiVersionString}")
            appendLine("- **Hardware Level:** Level ${deviceInfo.vulkan.hardwareLevel}")
            appendLine("- **Compute Shaders:** ${if (deviceInfo.vulkan.hardwareComputeLevel >= 0) "Supported (Level ${deviceInfo.vulkan.hardwareComputeLevel})" else "None"}")
            appendLine("- **64-Bit ABI:** ${if (deviceInfo.vulkan.is64BitAbi) "Yes" else "No"}")
            appendLine("- **Driver Status:** ${if (deviceInfo.vulkan.isVulkanSupported) "Supported & Hardware Accelerated" else "Unavailable"}")
            for (detail in deviceInfo.vulkan.compatibilityDetails) {
                appendLine("  $detail")
            }
            appendLine()
            appendLine("## 🔋 Battery")
            appendLine("- **Level:** ${deviceInfo.battery.levelPercent}%")
            appendLine("- **Health:** ${deviceInfo.battery.health}")
            appendLine("- **Voltage:** ${deviceInfo.battery.voltageMv} mV")
            appendLine("- **Temperature:** ${deviceInfo.battery.temperatureCelsius}°C")
            appendLine("- **Technology:** ${deviceInfo.battery.technology}")
            appendLine()
            appendLine("## 📡 Network & Connectivity")
            appendLine("- **Active Connection:** ${deviceInfo.network.networkType}")
            appendLine("- **IPv4 Address:** ${deviceInfo.network.ipAddress}")
            appendLine("- **IPv6 Address:** ${deviceInfo.network.ipv6Address}")
            appendLine("- **Wi-Fi SSID:** ${deviceInfo.network.wifiSsid}")
            appendLine("- **Wi-Fi Link Speed:** ${deviceInfo.network.wifiLinkSpeedMbps} Mbps")
            appendLine("- **Wi-Fi 6 Supported:** ${if (deviceInfo.network.isWifi6Supported) "Yes" else "No"}")
            appendLine()
            appendLine("## 🛰️ GNSS & Satellite Navigation")
            appendLine("- **GPS Receiver:** ${if (deviceInfo.gnssLocation.hasGps) "Active" else "Unavailable"}")
            appendLine("- **Supported Constellations:** ${deviceInfo.gnssLocation.constellations.joinToString(", ")}")
            appendLine("- **Dual-Frequency (L1+L5):** ${if (deviceInfo.gnssLocation.hasDualFrequency) "Supported" else "Single-band"}")
            appendLine("- **Location Providers:** ${deviceInfo.gnssLocation.supportedProviders}")
            appendLine()
            appendLine("## 📷 Cameras (${deviceInfo.cameras.size})")
            for (cam in deviceInfo.cameras) {
                appendLine("- **Camera ${cam.cameraId} (${cam.facing}):** ${cam.resolutionMegapixels} MP, Max Zoom ${cam.maxZoom}x, Flash: ${if (cam.flashAvailable) "Yes" else "No"}")
            }
            appendLine()
            appendLine("## 🎛️ Sensors (${deviceInfo.sensors.size} detected)")
            for (s in deviceInfo.sensors.take(10)) {
                appendLine("- **${s.name}** (${s.vendor})")
            }
            if (deviceInfo.sensors.size > 10) {
                appendLine("- *...and ${deviceInfo.sensors.size - 10} more hardware sensors*")
            }
            appendLine()
            if (benchmark != null) {
                appendLine("## 🚀 Performance Benchmark")
                appendLine("- **Total Benchmark Score:** ${benchmark.totalBenchmarkScore} (${benchmark.tierLabel})")
                appendLine("- **Single-Core Score:** ${benchmark.cpuSingleCoreScore}")
                appendLine("- **Multi-Core Score:** ${benchmark.cpuMultiCoreScore}")
                appendLine("- **RAM Throughput:** ${benchmark.ramSpeedMbPerSec} MB/s")
                appendLine("- **Storage Read Speed:** ${benchmark.storageReadMbPerSec} MB/s")
                appendLine("- **Storage Write Speed:** ${benchmark.storageWriteMbPerSec} MB/s")
                appendLine()
            }
            appendLine("---")
            appendLine("Created by FoxyPlayzZ on YouTube: https://youtube.com/@foxyplayzz?si=6Dpx1QA49S00zQhJ")
            appendLine("Generated with ❤️ by Foxy Device Info")
        }
    }

    fun shareReport(context: Context, reportText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Foxy Device Info Report")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        val chooser = Intent.createChooser(intent, "Share Foxy Device Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

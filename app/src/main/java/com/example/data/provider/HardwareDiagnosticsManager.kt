package com.example.data.provider

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.data.model.DiagnosticTestItem
import com.example.data.model.TestStatus
import com.example.data.model.TestType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class HardwareDiagnosticsManager(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun getInitialTestList(): List<DiagnosticTestItem> {
        val pm = context.packageManager
        return listOf(
            DiagnosticTestItem(
                type = TestType.DISPLAY,
                title = "Display & Pixels",
                description = "Inspect screen color uniformity, RGB channels and dead pixels",
                iconName = "tv"
            ),
            DiagnosticTestItem(
                type = TestType.TOUCHSCREEN,
                title = "Touch & Multi-touch",
                description = "Test multi-finger touch responsiveness and digitizer accuracy",
                iconName = "touch_app"
            ),
            DiagnosticTestItem(
                type = TestType.SPEAKER,
                title = "Main Speaker",
                description = "Play high & low acoustic test frequencies through loudspeaker",
                iconName = "volume_up"
            ),
            DiagnosticTestItem(
                type = TestType.EARPIECE,
                title = "Call Earpiece",
                description = "Test front voice-call receiver speaker output",
                iconName = "phone_in_talk"
            ),
            DiagnosticTestItem(
                type = TestType.MICROPHONE,
                title = "Microphone",
                description = "Capture live voice audio & measure decibel waveform",
                iconName = "mic"
            ),
            DiagnosticTestItem(
                type = TestType.VIBRATION,
                title = "Haptic Vibration",
                description = "Trigger distinct haptic vibration motor patterns",
                iconName = "vibration",
                status = if (vibrator?.hasVibrator() == true) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.FLASHLIGHT,
                title = "LED Flashlight",
                description = "Turn rear camera LED torch on and off",
                iconName = "flashlight_on",
                status = if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.PROXIMITY,
                title = "Proximity Sensor",
                description = "Detect near / far obstruction above display",
                iconName = "sensors",
                status = if (sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.LIGHT_SENSOR,
                title = "Ambient Light Sensor",
                description = "Measure real-time ambient illuminance in Lux",
                iconName = "light_mode",
                status = if (sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) != null) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.ACCELEROMETER,
                title = "Accelerometer",
                description = "Test 3-axis motion and device tilt bubble level",
                iconName = "screen_rotation",
                status = if (sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.GYROSCOPE,
                title = "Gyroscope",
                description = "Detect 3D rotational angular velocity and spin",
                iconName = "rotate_right",
                status = if (sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.COMPASS,
                title = "Digital Compass",
                description = "Measure magnetic field and cardinal heading",
                iconName = "explore",
                status = if (sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.FINGERPRINT,
                title = "Biometrics / Fingerprint",
                description = "Verify biometric hardware and sensor availability",
                iconName = "fingerprint",
                status = if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.VOLUME_BUTTONS,
                title = "Volume Buttons",
                description = "Check physical Volume Up and Volume Down hardware keys",
                iconName = "volume_down"
            ),
            DiagnosticTestItem(
                type = TestType.BLUETOOTH,
                title = "Bluetooth Adapter",
                description = "Check Bluetooth radio, BLE support and state",
                iconName = "bluetooth",
                status = if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) TestStatus.NOT_RUN else TestStatus.NOT_SUPPORTED
            ),
            DiagnosticTestItem(
                type = TestType.CHARGING,
                title = "Charging Port & Power",
                description = "Check power connection, USB/AC plug and voltage",
                iconName = "battery_charging_full"
            ),
            DiagnosticTestItem(
                type = TestType.HEADSET,
                title = "Headphone / Audio Jack",
                description = "Detect wired 3.5mm jack or Type-C audio accessory connection",
                iconName = "headphones"
            ),
            DiagnosticTestItem(
                type = TestType.VULKAN,
                title = "Vulkan API & VulkanMod",
                description = "Inspect Vulkan graphics API, driver level and VulkanMod gaming support",
                iconName = "sports_esports"
            )
        )
    }

    // 1. Audio tone generation for speaker and earpiece
    suspend fun playTestTone(frequencyHz: Int = 440, durationMs: Int = 1200, isEarpiece: Boolean = false) = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (sampleRate.toDouble() / frequencyHz)
            // Apply gentle attack/decay envelope to prevent clicking
            val envelope = when {
                i < 2000 -> i / 2000.0
                i > numSamples - 2000 -> (numSamples - i) / 2000.0
                else -> 1.0
            }
            val sample = (sin(angle) * 32767.0 * envelope).toInt().toShort()
            generatedSnd[2 * i] = (sample.toInt() and 0x00ff).toByte()
            generatedSnd[2 * i + 1] = ((sample.toInt() and 0xff00) shr 8).toByte()
        }

        val usage = if (isEarpiece) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA
        val streamType = if (isEarpiece) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSnd.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 100)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {}
    }

    // 2. Vibration test
    fun testVibration() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 150, 100, 150, 100, 300)
            val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vib.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(longArrayOf(0, 150, 100, 150, 100, 300), -1)
        }
    }

    // 3. Torch / Flashlight test
    private var isTorchOn = false
    fun toggleTorch(enable: Boolean): Boolean {
        val cm = cameraManager ?: return false
        try {
            for (id in cm.cameraIdList) {
                try {
                    cm.setTorchMode(id, enable)
                    isTorchOn = enable
                    return true
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return false
    }

    // 4. Quick charging detection
    fun checkChargingState(): Boolean {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val statusIntent = context.registerReceiver(null, ifilter)
        val status = statusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    // 5. Headset plug detection
    fun checkHeadsetPlugged(): Boolean {
        val am = audioManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            return devices.any {
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } else {
            @Suppress("DEPRECATION")
            return am.isWiredHeadsetOn
        }
    }
}

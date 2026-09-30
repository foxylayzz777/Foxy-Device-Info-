package com.example.data.model

enum class TestStatus {
    NOT_RUN,
    RUNNING,
    PASSED,
    FAILED,
    NOT_SUPPORTED
}

enum class TestType {
    DISPLAY,
    TOUCHSCREEN,
    SPEAKER,
    EARPIECE,
    MICROPHONE,
    VIBRATION,
    FLASHLIGHT,
    PROXIMITY,
    LIGHT_SENSOR,
    ACCELEROMETER,
    GYROSCOPE,
    COMPASS,
    FINGERPRINT,
    VOLUME_BUTTONS,
    BLUETOOTH,
    CHARGING,
    HEADSET
}

data class DiagnosticTestItem(
    val type: TestType,
    val title: String,
    val description: String,
    val iconName: String,
    val status: TestStatus = TestStatus.NOT_RUN,
    val details: String? = null,
    val lastTestedTime: Long? = null
)

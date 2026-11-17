package com.mohgwatch.phone.data

internal object NightTestPolicy {
    fun interruptionReason(initialBoot: Int?, currentBoot: Int?, initialConfiguration: String, currentConfiguration: String): String? = when {
        initialBoot == null || currentBoot == null -> "INTERRUPTED_BOOT_UNKNOWN"
        initialBoot != currentBoot -> "INTERRUPTED_REBOOT"
        initialConfiguration != currentConfiguration -> "INTERRUPTED_CONFIGURATION_CHANGE"
        else -> null
    }
}

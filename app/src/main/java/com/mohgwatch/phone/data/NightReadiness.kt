package com.mohgwatch.phone.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Problems that can stop overnight glucose monitoring. */
enum class ReadinessIssue { BATTERY_OPTIMIZATION, EXACT_ALARMS, NOTIFICATIONS, OEM_SLEEP_STANDBY, OEM_BACKGROUND_ACTIVITY, NIGHT_OUTAGE }

data class NightOutage(val startMillis: Long, val endMillis: Long)

/** Manual answers about OEM settings that Android does not expose to apps. */
data class OemChecklist(
    val sleepStandbyOff: Boolean = false,
    val backgroundActivityAllowed: Boolean = false,
    /** System build at the time of the answers; an OS update may reset OEM settings. */
    val systemFingerprint: String? = null,
    val outage: NightOutage? = null
) {
    fun validFor(fingerprint: String): OemChecklist =
        if (systemFingerprint == fingerprint) this else copy(sleepStandbyOff = false, backgroundActivityAllowed = false)
}

data class ReadinessInputs(
    val batteryExempt: Boolean,
    val exactAlarmsAllowed: Boolean,
    val notificationsAllowed: Boolean,
    val oemFamily: Boolean,
    val checklist: OemChecklist,
    val systemFingerprint: String
)

object NightReadinessPolicy {
    const val OUTAGE_THRESHOLD_MILLIS = 10 * 60_000L

    fun issues(inputs: ReadinessInputs): List<ReadinessIssue> {
        val checklist = inputs.checklist.validFor(inputs.systemFingerprint)
        return buildList {
            if (!inputs.batteryExempt) add(ReadinessIssue.BATTERY_OPTIMIZATION)
            if (!inputs.exactAlarmsAllowed) add(ReadinessIssue.EXACT_ALARMS)
            if (!inputs.notificationsAllowed) add(ReadinessIssue.NOTIFICATIONS)
            if (inputs.oemFamily && !checklist.sleepStandbyOff) add(ReadinessIssue.OEM_SLEEP_STANDBY)
            if (inputs.oemFamily && !checklist.backgroundActivityAllowed) add(ReadinessIssue.OEM_BACKGROUND_ACTIVITY)
            if (checklist.outage != null) add(ReadinessIssue.NIGHT_OUTAGE)
        }
    }

    /** A long loss of internet that started while the phone was asleep is worth a morning warning. */
    fun outageOf(lostAtMillis: Long?, startedWhileAsleep: Boolean, restoredAtMillis: Long): NightOutage? =
        lostAtMillis?.takeIf { startedWhileAsleep && restoredAtMillis - it >= OUTAGE_THRESHOLD_MILLIS }
            ?.let { NightOutage(it, restoredAtMillis) }

    /** OnePlus, OPPO and realme share the ColorOS power manager with the sleep standby optimization. */
    fun isOemFamily(manufacturer: String, brand: String): Boolean =
        listOf(manufacturer, brand).any { it.lowercase() in setOf("oneplus", "oppo", "realme") }
}

object NightReadinessStore {
    private const val PREFS = "mohgwatch_night_readiness"
    private val state = MutableStateFlow<OemChecklist?>(null)

    val currentFingerprint: String get() = Build.FINGERPRINT.orEmpty()
    val isOemFamily: Boolean get() = NightReadinessPolicy.isOemFamily(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty())

    @Synchronized fun checklist(context: Context): StateFlow<OemChecklist?> {
        if (state.value == null) state.value = load(context)
        return state
    }

    fun setSleepStandbyOff(context: Context, value: Boolean) = update(context) { it.copy(sleepStandbyOff = value) }

    fun setBackgroundActivityAllowed(context: Context, value: Boolean) = update(context) { it.copy(backgroundActivityAllowed = value) }

    fun dismissOutage(context: Context) = update(context) { it.copy(outage = null) }

    /** An outage can have causes other than OEM settings (e.g. a router schedule), so user answers stay untouched. */
    fun recordOutage(context: Context, outage: NightOutage) {
        update(context) { it.copy(outage = outage) }
        Diagnostics.record(context, "NIGHT_OUTAGE", "startMs=${outage.startMillis} endMs=${outage.endMillis} " +
            "minutes=${(outage.endMillis - outage.startMillis) / 60_000L}")
    }

    /** Start of the current loss of internet; persisted so it survives the service being killed while offline. */
    /** @return true when this call started a new loss of internet. */
    @Synchronized fun markOffline(context: Context, nowMillis: Long, asleep: Boolean): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong("offline_since", -1L) >= 0L) return false
        prefs.edit().putLong("offline_since", nowMillis).putBoolean("offline_asleep", asleep).apply()
        return true
    }

    @Synchronized fun refineOfflineAsleep(context: Context, asleep: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong("offline_since", -1L) >= 0L) prefs.edit().putBoolean("offline_asleep", asleep).apply()
    }

    /** Ends the current loss of internet and records it when it was a long overnight outage. */
    fun markOnline(context: Context, nowMillis: Long) {
        val outage = synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val since = prefs.getLong("offline_since", -1L).takeIf { it >= 0L } ?: return
            val asleep = prefs.getBoolean("offline_asleep", false)
            prefs.edit().remove("offline_since").remove("offline_asleep").apply()
            NightReadinessPolicy.outageOf(since, asleep, nowMillis)
        }
        outage?.let { recordOutage(context, it) }
    }

    @Synchronized private fun update(context: Context, change: (OemChecklist) -> OemChecklist) {
        val fingerprint = currentFingerprint
        val next = change(load(context).validFor(fingerprint)).copy(systemFingerprint = fingerprint)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("sleep_standby_off", next.sleepStandbyOff)
            .putBoolean("background_activity", next.backgroundActivityAllowed)
            .putString("fingerprint", next.systemFingerprint)
            .putLong("outage_start", next.outage?.startMillis ?: -1L)
            .putLong("outage_end", next.outage?.endMillis ?: -1L)
            .apply()
        state.value = next
    }

    private fun load(context: Context): OemChecklist {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val start = prefs.getLong("outage_start", -1L)
        val end = prefs.getLong("outage_end", -1L)
        return OemChecklist(
            sleepStandbyOff = prefs.getBoolean("sleep_standby_off", false),
            backgroundActivityAllowed = prefs.getBoolean("background_activity", false),
            systemFingerprint = prefs.getString("fingerprint", null),
            outage = if (start >= 0 && end >= start) NightOutage(start, end) else null
        )
    }
}

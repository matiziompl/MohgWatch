package com.mohgwatch.phone.data

import com.mohgwatch.core.model.UserSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.time.Instant

internal object SettingsExport {
    const val MAX_IMPORT_BYTES = 256 * 1024

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(Map::class.java).indent("  ")
    private val settingsAdapter = moshi.adapter(UserSettings::class.java)
    private val fieldTypes = settingsAdapter.serializeNulls().toJsonValue(UserSettings()) as Map<*, *>

    fun fromJson(json: String): UserSettings {
        require(json.length <= MAX_IMPORT_BYTES && json.toByteArray(Charsets.UTF_8).size <= MAX_IMPORT_BYTES)
        val document = requireNotNull(adapter.fromJson(json))
        require(document["schemaVersion"] == 1.0) { "Unsupported settings schema" }
        val values = document["settings"] as? Map<*, *> ?: error("Missing settings")
        require(values.keys.any { it in fieldTypes }) { "Empty settings" }
        // Moshi accepts numeric strings; imports must preserve the actual JSON field types.
        values.filterKeys { it in fieldTypes }.forEach { (key, value) ->
            val valid = when (fieldTypes[key]) {
                null -> value == null || value is String
                is Boolean -> value is Boolean
                is Int, is Long -> value is Number && value.toDouble() == value.toInt().toDouble()
                is Number -> value is Number && value.toFloat().isFinite()
                else -> value is String
            }
            require(valid) { "Invalid settings field: $key" }
        }
        val settings = requireNotNull(settingsAdapter.fromJsonValue(values))
        with(settings) {
            listOf(lowThreshold, highThreshold, veryHighThreshold).forEach {
                require(it.isFinite() && it > 0f) { "Invalid glucose threshold" }
            }
            require(lowThreshold < highThreshold) { "Inverted glucose thresholds" }
            require(pollIntervalMinutes in listOf(1, 2, 3, 5))
            require(staleDataMinutes in 1..60)
            require(notificationVolume in 0f..1f && nightNotificationVolume in 0f..1f)
            require(notificationSoundDelaySeconds in 0..30)
            require(alertSnoozeMinutes in 0..30)
            val time = Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]")
            require(time.matches(nightStartTime) && time.matches(nightEndTime))
        }
        return settings
    }

    fun read(input: InputStream): UserSettings {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, MAX_IMPORT_BYTES - bytes.size() + 1))
            if (count == -1) break
            bytes.write(buffer, 0, count)
            require(bytes.size() <= MAX_IMPORT_BYTES) { "Settings file is too large" }
        }
        return fromJson(bytes.toString(Charsets.UTF_8.name()))
    }

    fun toJson(settings: UserSettings, nowMillis: Long): String =
        adapter.toJson(mapOf("schemaVersion" to 1, "exportedAt" to Instant.ofEpochMilli(nowMillis).toString(), "settings" to settings))

    fun write(directory: File, settings: UserSettings, nowMillis: Long): File {
        directory.mkdirs()
        val file = directory.resolve("mohgwatch-settings-$nowMillis.json")
        file.writeText(toJson(settings, nowMillis), Charsets.UTF_8)
        directory.listFiles()?.filter { it.name.startsWith("mohgwatch-settings-") && it.extension == "json" && it != file }
            ?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach { it.delete() }
        return file
    }
}

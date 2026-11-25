package com.mohgwatch.phone.data

import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.core.model.TrendVibrationPattern
import com.mohgwatch.core.model.UserSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class SettingsExportTest {
    data class Document(val schemaVersion: Int, val exportedAt: String, val settings: UserSettings)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test fun `export preserves custom settings in a versioned dated document`() {
        val settings = UserSettings(
            unit = GlucoseUnit.MMOL_L, lowThreshold = 83f, highThreshold = 177f,
            showDemoButton = false, doseBreakfast = "4j", nightStartTime = "23:15",
            alertLowHapticEnabled = false, trendRisingPattern = TrendVibrationPattern.SHORT,
            staleDataMinutes = 23
        )
        val document = moshi.adapter(Document::class.java)
            .fromJson(SettingsExport.toJson(settings, 0L))!!
        assertEquals(1, document.schemaVersion)
        assertEquals("1970-01-01T00:00:00Z", document.exportedAt)
        assertEquals(settings, document.settings)
    }

    @Test fun `export contains settings without account credential fields`() {
        val json = SettingsExport.toJson(UserSettings(), 0L)
        @Suppress("UNCHECKED_CAST")
        val document = moshi.adapter(Map::class.java).fromJson(json) as Map<String, Any?>
        assertEquals(setOf("schemaVersion", "exportedAt", "settings"), document.keys)
        val settings = document["settings"] as Map<*, *>
        for (key in listOf("email", "password", "token", "userId", "accountId", "patientId", "Authorization")) {
            assertFalse("Credential field must be absent: $key", settings.containsKey(key))
        }
        assertTrue(settings.containsKey("lowThreshold"))
    }

    @Test fun `export writes a readable file and bounds older exports without deleting other files`() {
        val directory = Files.createTempDirectory("settings-export").toFile()
        try {
            directory.resolve("keep.txt").writeText("unrelated")
            repeat(4) { index ->
                SettingsExport.write(directory, UserSettings(doseBreakfast = "${index}j"), index.toLong())
            }
            val document = moshi.adapter(Document::class.java)
                .fromJson(directory.resolve("mohgwatch-settings-3.json").readText())!!
            assertEquals("3j", document.settings.doseBreakfast)
            assertEquals(3, directory.listFiles()!!.count { it.extension == "json" })
            assertEquals("unrelated", directory.resolve("keep.txt").readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun `import restores exported custom settings including upper UI threshold`() {
        val settings = UserSettings(
            unit = GlucoseUnit.MMOL_L, lowThreshold = 83f, highThreshold = 280f,
            showDemoButton = false, doseBreakfast = "4j", nightStartTime = "23:15",
            alertLowHapticEnabled = false, trendRisingPattern = TrendVibrationPattern.SHORT,
            staleDataMinutes = 23, isSyncEnabled = false, notificationVolume = 0.4f,
            nightNotificationVolume = 0f, notificationSoundDelaySeconds = 30
        )
        val json = SettingsExport.toJson(settings, 0L)
        assertEquals(settings, SettingsExport.fromJson(json))
        assertEquals(settings, SettingsExport.read(json.byteInputStream()))
    }

    @Test fun `older partial export restores omitted settings from defaults`() {
        val imported = SettingsExport.fromJson(document("""{"unit":"MMOL_L","doseLunch":"3j"}"""))
        assertEquals(UserSettings(unit = GlucoseUnit.MMOL_L, doseLunch = "3j"), imported)
    }

    @Test fun `unknown fields are ignored for compatible exports`() {
        val imported = SettingsExport.fromJson(
            document("""{"unit":"MMOL_L","futureSetting":{"enabled":true}}""")
                .replace("\"schemaVersion\":1", "\"futureMetadata\":true,\"schemaVersion\":1")
        )
        assertEquals(UserSettings(unit = GlucoseUnit.MMOL_L), imported)
    }

    @Test fun `legacy schema one export preserves active settings and ignores retired settings`() {
        val settings = UserSettings(
            unit = GlucoseUnit.MMOL_L, lowThreshold = 83f, highThreshold = 177f,
            appTheme = AppTheme.BLUE, themeMode = ThemeMode.DARK, showDemoButton = false,
            doseDinner = "6j", alertLowHapticEnabled = false,
            trendRisingPattern = TrendVibrationPattern.LONG, staleDataMinutes = 23
        )
        val retired = mapOf(
            "preset" to "H5_AURORA", "syncThemeWithWatch" to false, "watchAppTheme" to "PURPLE",
            "alertLowThreshold" to 50, "alertHighThreshold" to 250, "alertVeryHighThreshold" to 300,
            "trendThresholdFastFalling" to -8, "trendThresholdFalling" to -3,
            "trendThresholdRising" to 3, "trendThresholdFastRising" to 8,
            "trendRisingEnabled" to false, "trendFallingEnabled" to false,
            "watchGlucoseFont" to "legacy", "watchTrendArrowStyle" to "under_value",
            "watchLowThreshold" to 60, "watchHighThreshold" to 200, "watchVeryHighThreshold" to 260
        )
        assertEquals(settings, SettingsExport.fromJson(withLegacySettings(settings, retired)))
    }

    @Test fun `invalid retired enum and field values do not reject a legacy export`() {
        val settings = UserSettings(lowThreshold = 80f, highThreshold = 160f,
            appTheme = AppTheme.TEAL, themeMode = ThemeMode.LIGHT, doseLunch = "3j")
        val retired = mapOf(
            "preset" to "REMOVED_PRESET", "watchAppTheme" to "REMOVED_COLOR",
            "syncThemeWithWatch" to "obsolete", "trendRisingEnabled" to "obsolete",
            "trendFallingEnabled" to null, "alertLowThreshold" to -1,
            "alertHighThreshold" to 0, "alertVeryHighThreshold" to "obsolete",
            "trendThresholdFastFalling" to "obsolete", "trendThresholdFalling" to null,
            "trendThresholdRising" to "obsolete", "trendThresholdFastRising" to "obsolete",
            "watchGlucoseFont" to null, "watchTrendArrowStyle" to 42,
            "watchLowThreshold" to 200, "watchHighThreshold" to 100, "watchVeryHighThreshold" to "obsolete"
        )
        assertEquals(settings, SettingsExport.fromJson(withLegacySettings(settings, retired)))
    }

    @Test fun `export omits retired fields and retains phone appearance and demo settings`() {
        val settings = UserSettings(appTheme = AppTheme.BLUE, themeMode = ThemeMode.LIGHT, showDemoButton = false)
        val document = moshi.adapter(Map::class.java).fromJson(SettingsExport.toJson(settings, 0L))!!
        val fields = document["settings"] as Map<*, *>
        listOf("preset", "syncThemeWithWatch", "watchAppTheme",
            "alertLowThreshold", "alertHighThreshold", "alertVeryHighThreshold",
            "trendThresholdFastFalling", "trendThresholdFalling", "trendThresholdRising", "trendThresholdFastRising",
            "trendRisingEnabled", "trendFallingEnabled", "watchGlucoseFont", "watchTrendArrowStyle",
            "watchLowThreshold", "watchHighThreshold", "watchVeryHighThreshold").forEach {
            assertFalse("Retired field must be absent: $it", fields.containsKey(it))
        }
        assertEquals("BLUE", fields["appTheme"])
        assertEquals("LIGHT", fields["themeMode"])
        assertEquals(false, fields["showDemoButton"])
    }

    @Test fun `import rejects unknown or missing schema and absent settings`() {
        listOf(
            "{}", "null", "[]",
            """{"settings":{"unit":"MMOL_L"}}""",
            """{"schemaVersion":2,"settings":{"unit":"MMOL_L"}}""",
            """{"schemaVersion":"1","settings":{"unit":"MMOL_L"}}""",
            """{"schemaVersion":1}""",
            document("null"), document("{}"), document("[]"), document("42")
        ).forEach(::assertRejected)
    }

    @Test fun `import rejects malformed trailing or incorrectly typed JSON`() {
        listOf(
            "", "{", document("""{"unit":"MG_DL"}""") + " {}",
            document("""{"unit":"invalid"}"""),
            document("""{"themeMode":null}"""),
            document("""{"pollIntervalMinutes":"5"}"""),
            document("""{"pollIntervalMinutes":2.5}"""),
            document("""{"lowThreshold":"70"}"""),
            document("""{"notifyOutOfRange":"true"}"""),
            document("""{"doseBreakfast":4}""")
        ).forEach(::assertRejected)
    }

    @Test fun `import rejects unsafe thresholds without changing their values silently`() {
        listOf("lowThreshold", "highThreshold", "veryHighThreshold").forEach { field ->
            listOf("0", "-1", "1e100").forEach { value ->
                assertRejected(document("""{"$field":$value}"""))
            }
        }
        assertRejected(document("""{"lowThreshold":180,"highThreshold":180}"""))
        assertRejected(document("""{"lowThreshold":200,"highThreshold":180}"""))
    }

    @Test fun `import rejects unsupported polling and out of bounds notification settings`() {
        listOf(
            "pollIntervalMinutes" to listOf("0", "4", "6"),
            "staleDataMinutes" to listOf("0", "61"),
            "notificationVolume" to listOf("-0.1", "1.1", "1e100"),
            "nightNotificationVolume" to listOf("-0.1", "1.1", "1e100"),
            "notificationSoundDelaySeconds" to listOf("-1", "31"),
            "alertSnoozeMinutes" to listOf("-1", "31"),
            "nightStartTime" to listOf("\"24:00\"", "\"9:00\"", "\"22:60\""),
            "nightEndTime" to listOf("\"-1:00\"", "\"07:0\"", "\"noon\"")
        ).forEach { (field, values) ->
            values.forEach { value -> assertRejected(document("""{"$field":$value}""")) }
        }
    }

    @Test fun `import accepts valid control boundaries`() {
        listOf(1, 2, 3, 5).forEach { interval ->
            val settings = UserSettings(pollIntervalMinutes = interval, staleDataMinutes = 1,
                notificationVolume = 0f, nightNotificationVolume = 1f,
                notificationSoundDelaySeconds = 0, nightStartTime = "00:00", nightEndTime = "23:59")
            assertEquals(settings, SettingsExport.fromJson(SettingsExport.toJson(settings, 0L)))
        }
        val settings = UserSettings(staleDataMinutes = 60, notificationSoundDelaySeconds = 30)
        assertEquals(settings, SettingsExport.fromJson(SettingsExport.toJson(settings, 0L)))
        listOf(0, 30).forEach { minutes ->
            val snooze = UserSettings(alertSnoozeMinutes = minutes)
            assertEquals(snooze, SettingsExport.fromJson(SettingsExport.toJson(snooze, 0L)))
        }
    }

    @Test fun `import rejects oversized streams before reading them in full`() {
        val stream = object : java.io.InputStream() {
            var bytesRead = 0
            override fun read(): Int {
                bytesRead++
                check(bytesRead <= 256 * 1024 + 1) { "Import read past its size limit" }
                return ' '.code
            }
        }
        assertThrows(Exception::class.java) { SettingsExport.read(stream) }
        assertTrue("The stream must stop at the import limit", stream.bytesRead <= 256 * 1024 + 1)
    }

    @Test fun `import size limit permits its exact byte boundary`() {
        val json = SettingsExport.toJson(UserSettings(), 0L).padEnd(256 * 1024, ' ')
        assertEquals(UserSettings(), SettingsExport.read(json.byteInputStream()))
        assertThrows(Exception::class.java) { SettingsExport.read((json + " ").byteInputStream()) }
    }

    private fun document(settings: String): String =
        """{"schemaVersion":1,"exportedAt":"1970-01-01T00:00:00Z","settings":$settings}"""

    private fun withLegacySettings(settings: UserSettings, retired: Map<String, Any?>): String {
        val adapter = moshi.adapter(Map::class.java).serializeNulls()
        val document = adapter.fromJson(SettingsExport.toJson(settings, 0L))!!
        return adapter.toJson(document + ("settings" to (document["settings"] as Map<*, *> + retired)))
    }

    private fun assertRejected(json: String) {
        assertThrows("Must reject: $json", Exception::class.java) { SettingsExport.fromJson(json) }
    }
}

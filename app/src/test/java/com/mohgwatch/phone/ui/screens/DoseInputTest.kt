package com.mohgwatch.phone.ui.screens

import com.mohgwatch.core.model.UserSettings
import org.junit.Assert.*
import org.junit.Test

class DoseInputTest {
    @Test fun storageKeepsOneUnitAndAcceptsDotCommaAndWhitespace() {
        val cases = listOf(
            "1" to "1j", "11.1" to "11,1j", "1.1" to "1,1j",
            "1,1" to "1,1j", "1,1j" to "1,1j", " 1,1J " to "1,1j",
            "  " to "", "" to "", "1," to "1,j", "1j,1j" to "1,1j"
        )
        for ((input, expected) in cases) {
            assertEquals(expected, DoseInputFormatter.forStorage(input))
            assertEquals(expected, DoseInputFormatter.forStorage(DoseInputFormatter.forStorage(input)))
        }
    }

    @Test fun persistedUnitIsDisplayedOutsideTheEditableNumber() {
        assertEquals("1,1", DoseInputFormatter.editingText("1,1j"))
        assertEquals("2.5", DoseInputFormatter.editingText(" 2.5J "))
        assertEquals("", DoseInputFormatter.editingText(""))
    }

    @Test fun noDraftCanBeSavedBeforeActualPreferencesArrive() {
        val loading = DoseInputState()
        assertNull(loading.storedValue)
        assertNull(loading.edit("1").storedValue)
        val loaded = loading.receive("2,5j")
        assertEquals("2,5", loaded.text)
        assertEquals("2,5j", loaded.storedValue)
    }

    @Test fun saveEmissionsDoNotRewriteTheNumberDuringTyping() {
        var input = DoseInputState().receive("2j")
        input = input.edit("1")
        assertEquals("1j", input.storedValue)
        input = input.receive("1j")
        assertEquals("1", input.text)
        input = input.edit(input.text + ",")
        assertEquals("1,j", input.storedValue)
        input = input.receive("1,j")
        assertEquals("1,", input.text)
        input = input.edit(input.text + "1")
        assertEquals("1,1", input.text)
        assertEquals("1,1j", input.storedValue)
        assertEquals("1,1", input.receive("1j").text)
    }

    @Test fun rapidEditsToDifferentMealsPreserveTheOtherDosesAndSettings() {
        var saved = UserSettings(doseBreakfast = "9j", doseLunch = "8j", doseDinner = "7j", doseNight = "6j", lowThreshold = 80f)
        for ((field, raw) in listOf(
            DoseField.BREAKFAST to "1", DoseField.LUNCH to "2", DoseField.BREAKFAST to "1,",
            DoseField.DINNER to "3", DoseField.BREAKFAST to "1,1"
        )) {
            saved = saved.withDose(field, raw)
        }
        assertEquals("1,1j", saved.doseBreakfast)
        assertEquals("2j", saved.doseLunch)
        assertEquals("3j", saved.doseDinner)
        assertEquals("6j", saved.doseNight)
        assertEquals(80f, saved.lowThreshold)
        assertEquals("", saved.withDose(DoseField.LUNCH, " ").doseLunch)
    }
}

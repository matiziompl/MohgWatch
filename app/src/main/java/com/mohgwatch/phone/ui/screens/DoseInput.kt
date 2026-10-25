package com.mohgwatch.phone.ui.screens

import com.mohgwatch.core.model.UserSettings

internal enum class DoseField { BREAKFAST, LUNCH, DINNER, NIGHT }

internal object DoseInputFormatter {
    fun editingText(stored: String): String = stored.replace("j", "", ignoreCase = true).trim()

    fun forStorage(raw: String): String {
        val value = editingText(raw).replace(".", ",")
        return if (value.isEmpty()) "" else "${value}j"
    }
}

internal data class DoseInputState(val text: String? = null) {
    // A save emission must not replace the raw number being edited with the widget label.
    fun receive(stored: String): DoseInputState =
        if (text == null) copy(text = DoseInputFormatter.editingText(stored)) else this
    fun edit(raw: String): DoseInputState =
        if (text == null) this else copy(text = raw.replace("j", "", ignoreCase = true))
    val storedValue: String? get() = text?.let(DoseInputFormatter::forStorage)
}

internal fun UserSettings.withDose(field: DoseField, raw: String): UserSettings {
    val value = DoseInputFormatter.forStorage(raw)
    return when (field) {
        DoseField.BREAKFAST -> copy(doseBreakfast = value)
        DoseField.LUNCH -> copy(doseLunch = value)
        DoseField.DINNER -> copy(doseDinner = value)
        DoseField.NIGHT -> copy(doseNight = value)
    }
}

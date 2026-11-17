package com.mohgwatch.phone.companion

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import java.io.File

internal class CompanionPreferenceStore(context: Context) : CompanionPreferences {
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { File(context.noBackupFilesDir, "mohgwatch_companion.preferences_pb") }
    )

    private val observation = booleanPreferencesKey("observation_requested")
    private val selectedId = intPreferencesKey("selected_association_id")
    private val battery = booleanPreferencesKey("last_battery_exempt")
    private val data = stringPreferencesKey("last_background_data_status")
    private val exemptionChanged = booleanPreferencesKey("exemption_changed")

    override suspend fun load(): CompanionPreferencesSnapshot {
        val values = store.data.first()
        return CompanionPreferencesSnapshot(values[observation] ?: false, values[selectedId], values[battery],
            values[data], values[exemptionChanged] ?: false)
    }

    override suspend fun save(value: CompanionPreferencesSnapshot) {
        store.edit { values ->
            values[observation] = value.observationRequested
            value.selectedAssociationId?.let { values[selectedId] = it } ?: values.remove(selectedId)
            value.previousBatteryExempt?.let { values[battery] = it } ?: values.remove(battery)
            value.previousBackgroundDataStatus?.let { values[data] = it } ?: values.remove(data)
            values[exemptionChanged] = value.exemptionChanged
        }
    }
}

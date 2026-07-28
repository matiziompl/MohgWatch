package com.mohgwatch.phone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mohgwatch.core.model.DisconnectLog
import com.mohgwatch.core.model.DisconnectReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.logsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mohgwatch_logs")

class LogsStore(private val context: Context) {
    private val LOGS_KEY = stringPreferencesKey("disconnect_logs_csv")

    val logsFlow: Flow<List<DisconnectLog>> = context.logsDataStore.data.map { prefs ->
        val csv = prefs[LOGS_KEY] ?: ""
        if (csv.isBlank()) return@map emptyList()
        csv.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 4) {
                try {
                    DisconnectLog(
                        id = parts[0],
                        disconnectTime = parts[1].toLong(),
                        reconnectTime = if (parts[2] == "null") null else parts[2].toLong(),
                        reason = DisconnectReason.valueOf(parts[3])
                    )
                } catch (e: Exception) { null }
            } else null
        }
    }

    suspend fun saveLogs(logs: List<DisconnectLog>) {
        context.logsDataStore.edit { prefs ->
            val csv = logs.joinToString(";") {
                "${it.id},${it.disconnectTime},${it.reconnectTime ?: "null"},${it.reason.name}"
            }
            prefs[LOGS_KEY] = csv
        }
    }
}

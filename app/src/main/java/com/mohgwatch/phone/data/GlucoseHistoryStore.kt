package com.mohgwatch.phone.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mohgwatch.core.model.GlucoseHistoryPolicy
import com.mohgwatch.core.model.GlucoseReading
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.glucoseHistoryDataStore by preferencesDataStore(name = "mohgwatch_history")

/** The watch chart needs only a bounded cache belonging to the current account. */
class GlucoseHistoryStore(context: Context) {
    private val store = context.applicationContext.glucoseHistoryDataStore
    private val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        .adapter<List<GlucoseReading>>(Types.newParameterizedType(List::class.java, GlucoseReading::class.java))

    suspend fun load(accountId: String): List<GlucoseReading> {
        val prefs = store.data.first()
        if (prefs[ACCOUNT] != accountId) return emptyList()
        return GlucoseHistoryPolicy.merge(emptyList(), decode(prefs[READINGS]), System.currentTimeMillis())
    }

    suspend fun save(accountId: String, readings: List<GlucoseReading>, recordSuccessfulSync: Boolean = true) {
        val now = System.currentTimeMillis()
        store.edit { prefs ->
            val sameAccount = prefs[ACCOUNT] == accountId
            val previous = if (sameAccount) decode(prefs[READINGS]) else emptyList()
            prefs[READINGS] = adapter.toJson(GlucoseHistoryPolicy.merge(previous, readings, now))
            prefs[ACCOUNT] = accountId
            if (recordSuccessfulSync) prefs[LAST_SUCCESS] = now
            else if (!sameAccount) prefs.remove(LAST_SUCCESS)
        }
    }

    suspend fun lastSuccessfulSync(): Long = store.data.first()[LAST_SUCCESS] ?: 0L

    private fun decode(json: String?): List<GlucoseReading> {
        if (json == null) return emptyList()
        return try {
            adapter.fromJson(json).orEmpty()
        } catch (e: JsonDataException) {
            Log.w("GlucoseHistoryStore", "Nie można odczytać bufora historii", e)
            emptyList()
        } catch (e: IOException) {
            Log.w("GlucoseHistoryStore", "Uszkodzony bufor historii", e)
            emptyList()
        }
    }

    private companion object {
        val ACCOUNT = stringPreferencesKey("account_id")
        val READINGS = stringPreferencesKey("readings_json")
        val LAST_SUCCESS = longPreferencesKey("last_success")
    }
}

package com.mohgwatch.phone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.credentialDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mohgwatch_credentials"
)

/**
 * Bezpieczne przechowywanie danych logowania LibreLinkUp.
 * Używa DataStore z Preferences.
 *
 * UWAGA: W produkcji powinno używać Tink + EncryptedDataStore.
 * Obecna implementacja używa standardowego DataStore dla uproszczenia.
 */
class CredentialStore(private val context: Context) {

    data class Credentials(
        val token: String,
        val userId: String,
        val region: String,
        val expires: Long,
        val patientId: String? = null,
        val email: String? = null,
        val password: String? = null
    )

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val REGION = stringPreferencesKey("region")
        val EXPIRES = longPreferencesKey("expires")
        val PATIENT_ID = stringPreferencesKey("patient_id")
        val EMAIL = stringPreferencesKey("email")
        val PASSWORD = stringPreferencesKey("password")
    }

    val credentialsFlow: Flow<Credentials?> = context.credentialDataStore.data.map { prefs ->
        val token = prefs[Keys.TOKEN] ?: return@map null
        val userId = prefs[Keys.USER_ID] ?: return@map null
        Credentials(
            token = token,
            userId = userId,
            region = prefs[Keys.REGION] ?: "global",
            expires = prefs[Keys.EXPIRES] ?: 0L,
            patientId = prefs[Keys.PATIENT_ID],
            email = prefs[Keys.EMAIL],
            password = prefs[Keys.PASSWORD]
        )
    }

    suspend fun getCredentials(): Credentials? = credentialsFlow.first()

    suspend fun saveCredentials(
        token: String,
        userId: String,
        region: String,
        expires: Long,
        patientId: String? = null,
        email: String? = null,
        password: String? = null
    ) {
        context.credentialDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.REGION] = region
            prefs[Keys.EXPIRES] = expires
            patientId?.let { prefs[Keys.PATIENT_ID] = it }
            email?.let { prefs[Keys.EMAIL] = it }
            password?.let { prefs[Keys.PASSWORD] = it }
        }
    }

    suspend fun clear() {
        context.credentialDataStore.edit { it.clear() }
    }

    suspend fun hasCredentials(): Boolean = getCredentials() != null
}

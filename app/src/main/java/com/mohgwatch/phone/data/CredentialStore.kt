package com.mohgwatch.phone.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn

private val Context.credentialDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mohgwatch_credentials",
    produceMigrations = {
        listOf(object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) =
                CredentialPreferences.needsMigration(currentData)

            override suspend fun migrate(currentData: Preferences): Preferences =
                CredentialPreferences.migrate(currentData, createCredentialAead())

            override suspend fun cleanUp() = Unit
        })
    }
)

// Use the non-exportable Keystore key directly; never persist a software fallback keyset.
private fun createCredentialAead(): Aead =
    AndroidKeystoreKmsClient.getOrGenerateNewAeadKey("android-keystore://mohgwatch_credentials")

/** Saved login stays available for automatic renewal; secrets are encrypted at rest. */
class CredentialStore(context: Context) {
    private val context = context.applicationContext
    private val aead by lazy { createCredentialAead() }

    data class Credentials(
        val token: String,
        val userId: String,
        val region: String,
        val expires: Long,
        val patientId: String? = null,
        val email: String? = null,
        val password: String? = null
    )

    val credentialsFlow: Flow<Credentials?> = CredentialPreferences.observe(
        context.credentialDataStore.data, { aead }
    ) { failure ->
        Diagnostics.record(this.context, "CREDENTIAL_READ_ERROR", Diagnostics.error(failure))
    }.flowOn(Dispatchers.IO)

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
            CredentialPreferences.save(prefs, aead, token, userId, region, expires, patientId, email, password)
        }
    }

    suspend fun clear() {
        context.credentialDataStore.edit { it.clear() }
    }

    suspend fun hasCredentials(): Boolean = getCredentials() != null
}

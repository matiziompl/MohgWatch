package com.mohgwatch.phone.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import java.util.Base64
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.security.GeneralSecurityException

internal object CredentialPreferences {
    fun observe(
        preferences: kotlinx.coroutines.flow.Flow<Preferences>,
        aead: () -> Aead,
        onUnreadable: (Throwable) -> Unit = {}
    ): kotlinx.coroutines.flow.Flow<CredentialStore.Credentials?> = preferences.map { read(it, aead()) }.catch { failure ->
        if (failure !is GeneralSecurityException && failure !is IllegalArgumentException) throw failure
        onUnreadable(failure)
        // Keep the encrypted data: a temporarily unavailable Keystore may recover later.
        emit(null)
    }

    private val userIdKey = stringPreferencesKey("user_id")
    private val regionKey = stringPreferencesKey("region")
    private val expiresKey = longPreferencesKey("expires")
    private val patientIdKey = stringPreferencesKey("patient_id")
    private val secretNames = listOf("token", "email", "password")

    fun needsMigration(prefs: Preferences): Boolean =
        secretNames.any { prefs.contains(stringPreferencesKey(it)) }

    fun migrate(prefs: Preferences, aead: Aead): Preferences {
        val migrated = prefs.toMutablePreferences()
        for (name in secretNames) {
            val oldKey = stringPreferencesKey(name)
            val plaintext = prefs[oldKey] ?: continue
            writeSecret(migrated, aead, name, plaintext)
            migrated.remove(oldKey)
        }
        return migrated.toPreferences()
    }

    fun read(prefs: Preferences, aead: Aead): CredentialStore.Credentials? {
        val token = readSecret(prefs, aead, "token") ?: return null
        val userId = prefs[userIdKey] ?: return null
        return CredentialStore.Credentials(
            token = token,
            userId = userId,
            region = prefs[regionKey] ?: "global",
            expires = prefs[expiresKey] ?: 0L,
            patientId = prefs[patientIdKey],
            email = readSecret(prefs, aead, "email"),
            password = readSecret(prefs, aead, "password")
        )
    }

    fun save(
        prefs: MutablePreferences,
        aead: Aead,
        token: String,
        userId: String,
        region: String,
        expires: Long,
        patientId: String?,
        email: String?,
        password: String?
    ) {
        if (prefs[userIdKey] != userId) {
            prefs.remove(patientIdKey)
            prefs.remove(stringPreferencesKey("email_encrypted"))
            prefs.remove(stringPreferencesKey("password_encrypted"))
        }
        writeSecret(prefs, aead, "token", token)
        prefs[userIdKey] = userId
        prefs[regionKey] = region
        prefs[expiresKey] = expires
        patientId?.let { prefs[patientIdKey] = it }
        email?.let { writeSecret(prefs, aead, "email", it) }
        password?.let { writeSecret(prefs, aead, "password", it) }
    }

    private fun writeSecret(prefs: MutablePreferences, aead: Aead, name: String, value: String) {
        val encrypted = aead.encrypt(value.toByteArray(Charsets.UTF_8), name.toByteArray(Charsets.UTF_8))
        prefs[stringPreferencesKey("${name}_encrypted")] = Base64.getEncoder().encodeToString(encrypted)
    }

    private fun readSecret(prefs: Preferences, aead: Aead, name: String): String? {
        val encoded = prefs[stringPreferencesKey("${name}_encrypted")] ?: return null
        val plaintext = aead.decrypt(Base64.getDecoder().decode(encoded), name.toByteArray(Charsets.UTF_8))
        return String(plaintext, Charsets.UTF_8)
    }
}

package com.mohgwatch.phone.data

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import java.security.GeneralSecurityException
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CredentialPreferencesTest {
    private fun cipher(): Aead {
        AeadConfig.register()
        return KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM)
            .getPrimitive(Aead::class.java)
    }

    @Test fun `unreadable encrypted login can be presented without changing saved data`() = kotlinx.coroutines.test.runTest {
        val malformed = preferencesOf(stringPreferencesKey("token_encrypted") to "%invalid%", stringPreferencesKey("user_id") to "user")
        val failures = mutableListOf<Throwable>()
        val result = CredentialPreferences.observe(kotlinx.coroutines.flow.flowOf(malformed), { cipher() }, failures::add).first()
        assertNull(result)
        assertEquals(1, failures.size)
        assertEquals("%invalid%", malformed[stringPreferencesKey("token_encrypted")])
    }

    @Test fun `a different encryption key does not crash or erase the stored login`() = kotlinx.coroutines.test.runTest {
        val prefs = preferencesOf().toMutablePreferences()
        CredentialPreferences.save(prefs, cipher(), "token", "user", "eu", 1L, null, "email", "password")
        val before = prefs.toPreferences()
        assertNull(CredentialPreferences.observe(kotlinx.coroutines.flow.flowOf(prefs), { cipher() }).first())
        assertEquals(before, prefs.toPreferences())
    }

    @Test fun `keystore initialization and migration errors permit returning to login`() = kotlinx.coroutines.test.runTest {
        assertNull(CredentialPreferences.observe(kotlinx.coroutines.flow.flowOf(preferencesOf()), { throw GeneralSecurityException() }).first())
        val unavailable = kotlinx.coroutines.flow.flow<androidx.datastore.preferences.core.Preferences> { throw GeneralSecurityException() }
        assertNull(CredentialPreferences.observe(unavailable, { cipher() }).first())
    }

    @Test fun `credential read cancellation is preserved`() = kotlinx.coroutines.test.runTest {
        try {
            CredentialPreferences.observe(kotlinx.coroutines.flow.flowOf(preferencesOf()), { throw kotlinx.coroutines.CancellationException() }).first()
            fail("Cancellation must propagate")
        } catch (_: kotlinx.coroutines.CancellationException) { }
    }

    @Test fun `migration preserves saved login and removes plaintext secrets`() {
        val aead = cipher()
        val original = preferencesOf(
            stringPreferencesKey("token") to "saved-token",
            stringPreferencesKey("user_id") to "user",
            stringPreferencesKey("email") to "me@example.com",
            stringPreferencesKey("password") to "saved-password"
        )
        val migrated = CredentialPreferences.migrate(original, aead)
        val credentials = CredentialPreferences.read(migrated, aead)!!
        assertEquals("saved-token", credentials.token)
        assertEquals("me@example.com", credentials.email)
        assertEquals("saved-password", credentials.password)
        for (key in listOf("token", "email", "password")) {
            assertFalse(migrated.contains(stringPreferencesKey(key)))
        }
        assertFalse(CredentialPreferences.needsMigration(migrated))
        assertFalse(migrated.asMap().values.contains("saved-password"))
    }

    @Test fun `failed encryption does not change the existing login`() {
        val original = preferencesOf(stringPreferencesKey("token") to "saved-token")
        val failing = object : Aead {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
                throw GeneralSecurityException("Keystore unavailable")
            override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
                throw GeneralSecurityException("Keystore unavailable")
        }
        assertThrows(GeneralSecurityException::class.java) {
            CredentialPreferences.migrate(original, failing)
        }
        assertEquals("saved-token", original[stringPreferencesKey("token")])
    }

    @Test fun `refresh preserves saved password and patient for the same account`() {
        val aead = cipher()
        val prefs = preferencesOf().toMutablePreferences()
        CredentialPreferences.save(prefs, aead, "old", "user", "eu", 1L,
            "patient", "me@example.com", "password")
        CredentialPreferences.save(prefs, aead, "new", "user", "eu", 2L,
            null, null, null)
        val restored = CredentialPreferences.read(prefs, aead)!!
        assertEquals("new", restored.token)
        assertEquals("password", restored.password)
        assertEquals("patient", restored.patientId)
    }

    @Test fun `switching account does not retain the previous account secrets or patient`() {
        val aead = cipher()
        val prefs = preferencesOf().toMutablePreferences()
        CredentialPreferences.save(prefs, aead, "old", "user-one", "eu", 1L,
            "old-patient", "old@example.com", "old-password")
        CredentialPreferences.save(prefs, aead, "new", "user-two", "eu", 2L,
            null, "new@example.com", "new-password")
        val restored = CredentialPreferences.read(prefs, aead)!!
        assertNull(restored.patientId)
        assertEquals("new@example.com", restored.email)
        assertEquals("new-password", restored.password)
    }

    @Test fun `switching account without supplied credentials clears previous secrets`() {
        val aead = cipher()
        val prefs = preferencesOf().toMutablePreferences()
        CredentialPreferences.save(prefs, aead, "old", "user-one", "eu", 1L,
            "old-patient", "old@example.com", "old-password")
        CredentialPreferences.save(prefs, aead, "new", "user-two", "eu", 2L,
            null, null, null)
        val restored = CredentialPreferences.read(prefs, aead)!!
        assertNull(restored.patientId)
        assertNull(restored.email)
        assertNull(restored.password)
    }
}

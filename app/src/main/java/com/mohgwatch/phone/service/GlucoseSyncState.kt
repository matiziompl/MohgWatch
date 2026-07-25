package com.mohgwatch.phone.service

import com.mohgwatch.core.model.GlucoseReading
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Singleton przechowujący aktualny stan synchronizacji z LibreLinkUp
 * widoczny na żywo na ekranie Status w aplikacji na telefonie.
 */
object GlucoseSyncState {
    val latestReading = MutableStateFlow<GlucoseReading?>(null)
    val history = MutableStateFlow<List<GlucoseReading>>(emptyList())
    val syncStatusText = MutableStateFlow("Zatrzynany — brak logowania")
    val isSyncing = MutableStateFlow(false)
    val lastError = MutableStateFlow<String?>(null)
    val lastSyncTimestamp = MutableStateFlow(0L)
}

package com.mohgwatch.core.model

enum class DisconnectReason(val label: String) {
    NETWORK_ERROR("Błąd sieci (brak internetu)"),
    AUTH_ERROR("Błąd autoryzacji (nieprawidłowe dane)"),
    API_ERROR("Błąd serwera LibreLinkUp"),
    SYSTEM_KILLED("Aplikacja zabita przez system"),
    UNKNOWN("Nieznany powód")
}

data class DisconnectLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val disconnectTime: Long,
    var reconnectTime: Long? = null,
    val reason: DisconnectReason = DisconnectReason.UNKNOWN
) {
    fun getDurationMinutes(): Long {
        if (reconnectTime == null) return 0
        return (reconnectTime!! - disconnectTime) / 60000
    }
}

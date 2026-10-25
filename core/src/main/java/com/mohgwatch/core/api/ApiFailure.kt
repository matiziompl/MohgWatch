package com.mohgwatch.core.api

import com.mohgwatch.core.api.LibreLinkUpClient.ApiResult
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException

enum class NetworkFailureKind(val message: String) {
    CONNECTION("Nie udało się połączyć z serwerem LibreLinkUp. Sprawdź dostęp do tej usługi i spróbuj ponownie."),
    DNS("Nie udało się odnaleźć serwera LibreLinkUp (DNS). Sprawdź ustawienia sieci, prywatnego DNS lub VPN."),
    TIMEOUT("Serwer LibreLinkUp nie odpowiedział w wymaganym czasie. Spróbuj ponownie."),
    TLS("Nie udało się nawiązać bezpiecznego połączenia z LibreLinkUp (TLS). Sprawdź datę telefonu i ustawienia sieci lub VPN.")
}

internal fun loginHttpFailure(code: Int, region: String? = null): ApiResult<Nothing> = ApiResult.Error(
    if (code == 430) {
        "Serwer LibreLinkUp odrzucił logowanie (HTTP 430). Spróbuj ponownie później i sprawdź logowanie w oficjalnej aplikacji LibreLinkUp. Jeśli błąd się powtarza, skopiuj raport diagnostyczny."
    } else {
        "Błąd logowania${region?.let { " (region $it)" }.orEmpty()}: HTTP $code"
    }, code
)

internal fun loginApplicationFailure(status: Int): ApiResult<Nothing>? = when (status) {
    0 -> null
    2 -> ApiResult.LoginRequired
    4 -> ApiResult.Error("Konto wymaga dokończenia wymaganych kroków w oficjalnej aplikacji LibreLinkUp (np. potwierdzenia dokumentów). Otwórz ją i dokończ logowanie, a następnie spróbuj ponownie w MohgWatch.")
    else -> ApiResult.Error("LibreLinkUp odrzucił logowanie (status aplikacji: $status). Sprawdź konto w oficjalnej aplikacji LibreLinkUp i skopiuj raport diagnostyczny, jeśli błąd się powtarza.")
}

internal fun classifyApiFailure(
    failure: Exception,
    diagnostic: (String, String) -> Unit = { _, _ -> }
): ApiResult<Nothing> {
    if (failure is CancellationException) throw failure
    val result = when (failure) {
        // JsonEncodingException is also an IOException; classify the payload first.
        is JsonDataException, is JsonEncodingException, is EOFException -> ApiResult.Error(
            "Serwer LibreLinkUp zwrócił odpowiedź w nieobsługiwanym formacie. Skopiuj raport diagnostyczny z ustawień aplikacji."
        )
        is UnknownHostException -> ApiResult.NetworkError(NetworkFailureKind.DNS)
        is SSLException -> ApiResult.NetworkError(NetworkFailureKind.TLS)
        is InterruptedIOException -> ApiResult.NetworkError(NetworkFailureKind.TIMEOUT)
        is IOException -> ApiResult.NetworkError()
        else -> ApiResult.Error(
            "Nie udało się obsłużyć odpowiedzi LibreLinkUp. Skopiuj raport diagnostyczny z ustawień aplikacji."
        )
    }
    diagnostic(
        if (result is ApiResult.NetworkError) "API_NETWORK_ERROR" else "API_RESPONSE_ERROR",
        "kind=${(result as? ApiResult.NetworkError)?.kind ?: "RESPONSE"} causes=" +
            generateSequence(failure as Throwable) { it.cause }.take(6).joinToString(" <- ") { it.javaClass.simpleName } +
            safeJsonContext(failure)
    )
    return result
}

private fun safeJsonContext(failure: Exception): String {
    if (failure !is JsonDataException && failure !is JsonEncodingException && failure !is EOFException) return ""
    val message = failure.message.orEmpty()
    val paths = listOf("$", "$.status", "$.data", "$.data.user", "$.data.user.id",
        "$.data.authTicket", "$.data.authTicket.token", "$.data.authTicket.expires",
        "$.data.authTicket.duration", "$.data.redirect", "$.data.region")
    // Only fixed schema names may leave this function; exception messages can contain credentials.
    val path = paths.firstOrNull { message.endsWith(" at $it") || message.endsWith(" at path $it") }
        ?: "nieznane"
    val missingField = listOf("status", "data", "user", "id", "authTicket", "token", "expires", "duration", "redirect", "region")
        .firstOrNull { message.startsWith("Required value '$it' missing") }
    return " jsonPath=$path" + (missingField?.let { " missingField=$it" } ?: "")
}

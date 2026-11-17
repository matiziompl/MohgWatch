package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.*
import com.mohgwatch.core.model.*
import com.mohgwatch.core.util.TimestampParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Wysokopoziomowy klient LibreLinkUp API z obsługą:
 * - Automatycznego wykrywania regionu (redirect)
 * - SHA-256 Account-Id
 * - Retry z exponential backoff
 * - Auto-relogin przy wygaśnięciu tokenu
 */
class LibreLinkUpClient(private val diagnostic: (String, String) -> Unit = { _, _ -> }) {

    /** Stan sesji autoryzacji */
    data class AuthSession(
        val token: String,
        val userId: String,
        val accountId: String,
        val region: String,
        val expires: Long,
        val patientId: String? = null
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() / 1000 > expires
        val bearerToken: String get() = "Bearer $token"
    }

    /** Wynik operacji API */
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
        data object LoginRequired : ApiResult<Nothing>()
        data class NetworkError(val kind: NetworkFailureKind = NetworkFailureKind.CONNECTION) : ApiResult<Nothing>() {
            val message: String get() = kind.message
            val retryable: Boolean get() = kind != NetworkFailureKind.TLS
        }
    }

    private var session: AuthSession? = null
    private var currentApi: LibreLinkUpApi? = null

    private val moshi: Moshi = Moshi.Builder()
        .add(LoginResponseAdapterFactory)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /** Interceptor dodający wymagane nagłówki do wszystkich requestów */
    private val headersInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(ApiHeaders.HEADER_PRODUCT, ApiHeaders.PRODUCT)
            .addHeader(ApiHeaders.HEADER_VERSION, ApiHeaders.VERSION)
            .addHeader("Content-Type", ApiHeaders.CONTENT_TYPE)
            .addHeader("Accept", ApiHeaders.ACCEPT)
            .addHeader("Cache-Control", ApiHeaders.CACHE_CONTROL)
            .addHeader("Connection", "keep-alive")
            .build()
        chain.proceed(request)
    }

    private fun buildOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(headersInterceptor)
        .addInterceptor { chain ->
            val started = System.nanoTime()
            val path = chain.request().url.encodedPath
            val endpoint = when {
                path == "/llu/auth/login" -> "login"
                path == "/llu/connections" -> "latest"
                path.startsWith("/llu/connections/") && path.endsWith("/graph") -> "history"
                else -> "other"
            }
            try {
                chain.proceed(chain.request()).also {
                    diagnostic("HTTP_RESULT", "endpoint=$endpoint code=${it.code} durationMs=${(System.nanoTime() - started) / 1_000_000L}")
                }
            } catch (error: Exception) {
                diagnostic("HTTP_ERROR", "endpoint=$endpoint causes=${generateSequence(error as Throwable) { it.cause }.take(6).joinToString(" <- ") { it.javaClass.simpleName }} durationMs=${(System.nanoTime() - started) / 1_000_000L}")
                throw error
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildApi(baseUrl: String): LibreLinkUpApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(LibreLinkUpApi::class.java).also { currentApi = it }
    }

    /**
     * Logowanie do LibreLinkUp z automatycznym wykrywaniem regionu.
     *
     * @param email Email konta LibreLinkUp
     * @param password Hasło
     * @param region Region (null = auto-detect przez global endpoint)
     * @return AuthSession lub błąd
     */
    suspend fun login(
        email: String,
        password: String,
        region: String? = null
    ): ApiResult<AuthSession> {
        return withRetry(maxAttempts = 3) {
            try {
                val startUrl = if (region != null && region != "global") {
                    ApiHeaders.getBaseUrl(region)
                } else {
                    ApiHeaders.getBaseUrl("global")
                }

                val api = buildApi(startUrl)
                val response = api.login(LoginRequest(email, password))

                if (response.code() == 401 || response.code() == 403) {
                    return@withRetry ApiResult.LoginRequired
                }
                if (!response.isSuccessful) {
                    return@withRetry loginHttpFailure(response.code())
                }

                val body = response.body() ?: return@withRetry ApiResult.Error("Pusta odpowiedź serwera")
                diagnostic("LOGIN_API_STATUS", "stage=initial status=${body.status}")
                loginApplicationFailure(body.status)?.let { return@withRetry it }

                if (body.data?.redirect == true) {
                    // Redirect do regionalnego serwera
                    val redirectRegion = body.data.region
                        ?: return@withRetry ApiResult.Error("Serwer zwrócił redirect bez regionu")

                    val regionalUrl = ApiHeaders.getBaseUrl(redirectRegion)
                    val regionalApi = buildApi(regionalUrl)
                    val regionalResponse = regionalApi.login(LoginRequest(email, password))

                    if (regionalResponse.code() == 401 || regionalResponse.code() == 403) {
                        return@withRetry ApiResult.LoginRequired
                    }
                    if (!regionalResponse.isSuccessful) {
                        return@withRetry loginHttpFailure(regionalResponse.code(), redirectRegion)
                    }

                    val regionalBody = regionalResponse.body()
                        ?: return@withRetry ApiResult.Error("Pusta odpowiedź regionalnego serwera")
                    diagnostic("LOGIN_API_STATUS", "stage=regional status=${regionalBody.status}")
                    loginApplicationFailure(regionalBody.status)?.let { return@withRetry it }

                    return@withRetry createSession(regionalBody, redirectRegion)
                }

                createSession(body, region ?: "global")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                classifyApiFailure(e, diagnostic)
            }
        }
    }

    /**
     * Przywraca sesję z zapisanych danych (bez ponownego logowania).
     */
    fun restoreSession(
        token: String,
        userId: String,
        region: String,
        expires: Long,
        patientId: String? = null
    ) {
        val accountId = computeAccountId(userId)
        session = AuthSession(token, userId, accountId, region, expires, patientId)
        buildApi(ApiHeaders.getBaseUrl(region))
    }

    /**
     * Pobiera najnowszy odczyt glukozy z pierwszego połączenia (connection).
     */
    suspend fun getLatestReading(): ApiResult<GlucoseReading> {
        val auth = session ?: return ApiResult.LoginRequired
        if (auth.isExpired) return ApiResult.LoginRequired

        val api = currentApi ?: return ApiResult.Error("API nie zainicjalizowane")

        return withRetry(maxAttempts = 3) {
            try {
                val response = api.getConnections(auth.bearerToken, auth.accountId)

                when (response.code()) {
                    401, 403 -> {
                        session = null
                        return@withRetry ApiResult.LoginRequired
                    }
                }

                if (!response.isSuccessful) {
                    return@withRetry ApiResult.Error(
                        "Błąd pobierania danych: HTTP ${response.code()}",
                        response.code()
                    )
                }

                val connections = response.body()?.data
                if (connections.isNullOrEmpty()) {
                    return@withRetry ApiResult.Error(
                        "Brak połączeń LibreLinkUp. Upewnij się, że jesteś dodany jako follower."
                    )
                }

                val connection = connections.first()
                // Zapisz patientId na przyszłość
                session = auth.copy(patientId = connection.patientId)

                val measurement = connection.glucoseMeasurement
                    ?: return@withRetry ApiResult.Error("Brak danych glukozy z sensora")

                val reading = measurement.toGlucoseReading()
                    ?: return@withRetry ApiResult.Error("Brak prawidłowego czasu pomiaru glukozy")
                ApiResult.Success(reading)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                classifyApiFailure(e, diagnostic)
            }
        }
    }

    /**
     * Pobiera 12h historii odczytów glukozy.
     */
    suspend fun getGraphData(): ApiResult<List<GlucoseReading>> {
        val auth = session ?: return ApiResult.LoginRequired
        if (auth.isExpired) return ApiResult.LoginRequired

        val patientId = auth.patientId
        if (patientId == null) {
            // Potrzebujemy patientId — najpierw pobierz connections
            val latestResult = getLatestReading()
            when (latestResult) {
                is ApiResult.Success -> Unit
                is ApiResult.Error -> return latestResult
                is ApiResult.LoginRequired -> return latestResult
                is ApiResult.NetworkError -> return latestResult
            }
        }

        val currentPatientId = session?.patientId
            ?: return ApiResult.Error("Nie znaleziono patientId")
        val api = currentApi ?: return ApiResult.Error("API nie zainicjalizowane")

        return withRetry(maxAttempts = 3) {
            try {
                val response = api.getGraph(
                    currentPatientId,
                    auth.bearerToken,
                    auth.accountId
                )

                when (response.code()) {
                    401, 403 -> {
                        session = null
                        return@withRetry ApiResult.LoginRequired
                    }
                }

                if (!response.isSuccessful) {
                    return@withRetry ApiResult.Error(
                        "Błąd pobierania historii: HTTP ${response.code()}",
                        response.code()
                    )
                }

                val graphData = response.body()?.data?.graphData
                if (graphData.isNullOrEmpty()) {
                    return@withRetry ApiResult.Success(emptyList())
                }

                val readings = graphData.mapNotNull { it.toGlucoseReading() }
                ApiResult.Success(readings)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                classifyApiFailure(e, diagnostic)
            }
        }
    }

    /** Czy sesja jest aktywna i nieexpirowana */
    fun isAuthenticated(): Boolean = session?.isExpired == false

    /** Czyści sesję (logout) */
    fun clearSession() {
        session = null
    }

    // -- Prywatne helpery --

    private fun createSession(response: LoginResponse, region: String): ApiResult<AuthSession> {
        val data = response.data ?: return ApiResult.Error("Brak danych w odpowiedzi logowania")
        val user = data.user ?: return ApiResult.Error("Brak danych użytkownika")
        val ticket = data.authTicket ?: return ApiResult.Error("Brak tokenu autoryzacji")

        val accountId = computeAccountId(user.id)
        val authSession = AuthSession(
            token = ticket.token,
            userId = user.id,
            accountId = accountId,
            region = region,
            expires = ticket.expires
        )
        session = authSession
        return ApiResult.Success(authSession)
    }

    /**
     * Oblicza Account-Id jako SHA-256 hex digest z userId.
     */
    private fun computeAccountId(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(userId.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Retry z exponential backoff.
     */
    internal suspend fun <T> withRetry(
        maxAttempts: Int = 5,
        initialDelayMs: Long = 3000,
        block: suspend () -> ApiResult<T>
    ): ApiResult<T> {
        var currentDelay = initialDelayMs
        repeat(maxAttempts - 1) { attempt ->
            val result = block()
            if (result is ApiResult.NetworkError && result.retryable) {
                delay(currentDelay)
                currentDelay = (currentDelay * 1.5).toLong()
            } else {
                return result
            }
        }
        return block()
    }
}

/**
 * Konwertuje GlucoseMeasurement DTO na model GlucoseReading.
 */
internal fun GlucoseMeasurement.toGlucoseReading(): GlucoseReading? {
    val parsedTimestamp = TimestampParser.parseTimestamp(timestamp)
        ?: TimestampParser.parseFactoryTimestamp(factoryTimestamp)
        ?: return null

    return GlucoseReading(
        value = valueInMgPerDl,
        trendArrow = TrendArrow.fromApiValue(trendArrow),
        measurementColor = MeasurementColor.fromApiValue(measurementColor),
        timestamp = parsedTimestamp,
        isHigh = isHigh,
        isLow = isLow
    )
}

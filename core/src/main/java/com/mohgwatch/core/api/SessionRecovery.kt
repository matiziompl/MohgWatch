package com.mohgwatch.core.api

import com.mohgwatch.core.api.LibreLinkUpClient.ApiResult

/** A rejected replacement token must not trigger an unbounded login loop. */
suspend fun <T> fetchWithSessionRecovery(
    fetch: suspend () -> ApiResult<T>,
    recoverSession: suspend () -> ApiResult<*>
): ApiResult<T> {
    val result = fetch()
    if (result !is ApiResult.LoginRequired) return result
    return when (val recovery = recoverSession()) {
        is ApiResult.Success -> fetch()
        is ApiResult.Error -> recovery
        is ApiResult.LoginRequired -> recovery
        is ApiResult.NetworkError -> recovery
    }
}

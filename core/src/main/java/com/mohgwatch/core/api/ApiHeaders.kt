package com.mohgwatch.core.api

/**
 * Stałe nagłówków HTTP wymagane przez LibreLinkUp API.
 * API jest nieoficjalne — nagłówki imitują oficjalną aplikację mobilną.
 */
object ApiHeaders {
    const val PRODUCT = "llu.android"
    const val VERSION = "4.16.0"
    const val CONTENT_TYPE = "application/json"
    const val ACCEPT = "application/json"
    const val CACHE_CONTROL = "no-cache"

    const val HEADER_PRODUCT = "product"
    const val HEADER_VERSION = "version"
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_ACCOUNT_ID = "Account-Id"

    /** Regionalne adresy bazowe API */
    val BASE_URLS = mapOf(
        "global" to "https://api.libreview.io",
        "eu" to "https://api-eu.libreview.io",
        "eu2" to "https://api-eu2.libreview.io",
        "us" to "https://api-us.libreview.io",
        "de" to "https://api-de.libreview.io",
        "fr" to "https://api-fr.libreview.io",
        "jp" to "https://api-jp.libreview.io",
        "ap" to "https://api-ap.libreview.io",
        "au" to "https://api-au.libreview.io"
    )

    /** Zwraca URL bazowy dla regionu (fallback: global) */
    fun getBaseUrl(region: String): String =
        BASE_URLS[region.lowercase()] ?: BASE_URLS["global"]!!
}

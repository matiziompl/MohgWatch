package com.mohgwatch.core.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val status: Int,
    val data: LoginData?
)

@JsonClass(generateAdapter = true)
data class LoginData(
    val user: UserData?,
    val authTicket: AuthTicket?,
    val redirect: Boolean = false,
    val region: String? = null
)

@JsonClass(generateAdapter = true)
data class UserData(
    val id: String
)

@JsonClass(generateAdapter = true)
data class AuthTicket(
    val token: String,
    val expires: Long,
    val duration: Long
)

package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface dla nieoficjalnego API LibreLinkUp.
 * Nagłówki product/version/content-type dodawane przez OkHttp interceptor.
 */
interface LibreLinkUpApi {

    @POST("/llu/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/llu/connections")
    suspend fun getConnections(
        @Header("Authorization") authorization: String,
        @Header("Account-Id") accountId: String
    ): Response<ConnectionsResponse>

    @GET("/llu/connections/{patientId}/graph")
    suspend fun getGraph(
        @Path("patientId") patientId: String,
        @Header("Authorization") authorization: String,
        @Header("Account-Id") accountId: String
    ): Response<GraphResponse>
}

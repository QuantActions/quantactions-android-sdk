/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api

import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.data.api.ApiService.CapabilitiesResponse
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface TokenApi {

    @POST("credentials/devicereset")
    suspend fun resetPassword(
        @Body passwordResetBody: PasswordResetBody
    ): ApiResponse<Void>

    @POST("identities")
    suspend fun registerIdentity(
        @Body identityRegistrationRequestBody: IdentityRegistration
    ): ApiResponse<AuthResponse>

    @GET("auth/login")
    suspend fun login(@HeaderMap headers: Map<String, String>): ApiResponse<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): ApiResponse<AuthResponse>

    @GET("auth/grant")
    suspend fun enableOauth(
        @HeaderMap headers: Map<String, String>,
    ): ApiResponse<Void>

    @GET("capabilities")
    suspend fun getCapabilities(): ApiResponse<CapabilitiesResponse>

    @JsonClass(generateAdapter = true)
    @Serializable
    data class AuthResponse(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val expiresIn: Int? = null,
        val identity: Identity? = null,
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class PasswordResetBody(
        var id: String,
        val password: String,
        val verificationCode: String
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class IdentityRegistration(
        var id: String? = null,
        val password: String? = null,
        val gender: String? = "U",
        val nameAlias: String = "deviceAndroid",
        val yearOfBirth: Int? = null,
        val selfDeclaredHealthy: Boolean? = null,
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class Identity(
        val id: String? = null,
        val nameAlias: String? = null,
        val nameFirst: String? = null,
        val gender: String? = "U",
        val nameLast: String? = null,
        val phoneNumber: String? = null,
        val yearOfBirth: Int? = null,
        val selfDeclaredHealthy: Boolean? = null,
        val identityId: String? = null,
    )

    companion object {
        fun buildTokenApi(
            apiKey: String,
            cookieJar: ApiService.UvCookieJar
        ): TokenApi {
            return Retrofit.Builder()
                .baseUrl(BuildConfig.QA_API_ROUTE)
                .client(getRetrofitClient(apiKey, cookieJar))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addCallAdapterFactory(FlowerCallAdapterFactory.create())
                .build()
                .create(TokenApi::class.java)
        }
    }
}

private fun getRetrofitClient(
    apiKey: String,
    cookieJar: CookieJar
): OkHttpClient {

    val logger = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    return OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().also {
                it.addHeader("Accept", "application/json")
                it.addHeader("Content-Type", "application/json; charset=utf8")
                it.addHeader("x-api-key", apiKey)
            }.build())
        }
        .addInterceptor(logger)
        .build()
}

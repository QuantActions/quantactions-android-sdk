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
import com.quantactions.sdk.ManagePref2
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface TokenApi {


    @POST("flows/identities")
    suspend fun registerIdentity(
        @Body identityRegistrationRequestBody: IdentityRegistration
    ): ApiResponse<Identity>

    @GET("flows/oauth")  // LOGIN
    suspend fun login(@HeaderMap headers: Map<String, String>): ApiResponse<Void>

    @GET("flows/oauth/refresh")
    suspend fun refreshToken(
    ): ApiResponse<Void>

    @GET("flows/oauth/grant")
    suspend fun enableOauth(
        @HeaderMap headers: Map<String, String>,
    ): ApiResponse<Void>


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

//    val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
//    val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }

    return OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().also {
                it.addHeader("Accept", "application/json")
                it.addHeader("Content-Type", "application/json; charset=utf8")
                it.addHeader("x-api-key", apiKey)
//                it.addHeader("cache-control", "max-age=30")
            }.build())
        }
        .addInterceptor(logger)
        .build()
}

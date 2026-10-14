/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api

import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.CapabilitiesManager
import com.quantactions.sdk.GenericPreferences
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.adapters.SleepSummaryAdapter
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.adapters.StatisticStringAdapter
import com.quantactions.sdk.data.api.adapters.TrendAdapter
import com.quantactions.sdk.data.model.AppToPush
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.HttpURLConnection

@OptIn(ExperimentalCoroutinesApi::class)
class MockNetworkTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private lateinit var tokenAuthenticator: TokenAuthenticator
    private lateinit var cookieJar: ApiService.UvCookieJar
    private val preferences: GenericPreferences = mockk()
    private val capabilitiesManager: CapabilitiesManager = mockk()
    private val tokenApi: TokenApi = mockk()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        every { preferences.accessToken } returns "fake_access_token"
        every { preferences.refreshToken } returns "fake_refresh_token"
        every { preferences.identityId } returns "test_identity_id"
        every { preferences.password } returns "test_password"
        
        cookieJar = ApiService.UvCookieJar(preferences, "test_context")
        tokenAuthenticator = TokenAuthenticator(tokenApi, preferences, capabilitiesManager)

        val baseUrl = mockWebServer.url("/")
        
        // Use reflection or a test builder to inject the mock URL if ApiService.create has a hardcoded URL.
        // For testing purposes we're assuming we can build Retrofit here since ApiService.create uses a hardcoded URL
        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            // .authenticator(tokenAuthenticator) // skip authenticator for pure parsing tests
            .build()
            
        val customMoshi = Moshi.Builder()
            .add(StatisticAdapter())
            .add(StatisticStringAdapter())
            .add(SleepSummaryAdapter())
            .add(QuestionnaireAdapter())
            .add(TrendAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addCallAdapterFactory(FlowerCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(customMoshi))
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test updateAppList parses success response correctly`() = runTest {
        val mockResponseBody = """
            [
                {
                    "package": "com.example.app1",
                    "id": 1,
                    "category_main": "productivity",
                    "category_secondary": "tools"
                }
            ]
        """.trimIndent()

        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockResponseBody)
        mockWebServer.enqueue(response)

        val appList = listOf(AppToPush("com.example.app1", 1))
        val apiResponse = apiService.updateAppList("identityId", "deviceId", appList)

        assertTrue(apiResponse is ApiSuccessResponse)
        val successResponse = apiResponse as ApiSuccessResponse
        assertEquals(1, successResponse.body?.size)
        assertEquals("com.example.app1", successResponse.body?.get(0)?.`package`)
        assertEquals("productivity", successResponse.body?.get(0)?.categoryMain)
    }
}

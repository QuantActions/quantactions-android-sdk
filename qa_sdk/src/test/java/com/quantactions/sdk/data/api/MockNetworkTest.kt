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
                    "installationId": 1,
                    "categoryMain": "productivity",
                    "categorySecondary": "tools"
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

    @Test
    fun `test getStatSleepSummaryEntity parses correctly`() = runTest {
        val mockResponseBody = """
            [
              {
                "id": "8ff6b05d-6b3c-4833-9e2f-d40f2fa88543:001-002-006-004:2023-11",
                "code": "001-002-006-004",
                "extras": null,
                "participationId": "8ff6b05d-6b3c-4833-9e2f-d40f2fa88543",
                "publicName": "Sleep Summary",
                "timestamp": "2023-11",
                "metrics": {
                  "schema": {
                    "fields": [
                      {"name": "date", "type": "datetime", "tz": "UTC"},
                      {"name": "sleep-utc", "type": "integer"},
                      {"name": "wake-utc", "type": "integer"},
                      {"name": "int-start", "type": "string"},
                      {"name": "int-stop", "type": "string"},
                      {"name": "int-ntaps", "type": "string"},
                      {"name": "time-zone", "type": "string"},
                      {"name": "conf", "type": "number"}
                    ],
                    "primaryKey": ["date"],
                    "pandas_version": "1.4.0"
                  },
                  "data": [
                    {
                      "date": "2023-11-01T00:00:00.000Z",
                      "sleep-utc": 1698792598946.0,
                      "wake-utc": 1698819500926.0,
                      "int-start": [],
                      "int-stop": [],
                      "int-ntaps": [],
                      "time-zone": "Europe/Zurich",
                      "conf": 0.9715675023
                    }
                  ]
                }
              }
            ]
        """.trimIndent()

        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockResponseBody)
        mockWebServer.enqueue(response)

        val apiResponse = apiService.getStatSleepSummaryEntity("identityId", "participationId", "filter", "001")
        assertTrue(apiResponse is ApiSuccessResponse)
        val successResponse = apiResponse as ApiSuccessResponse
        assertEquals(1, successResponse.body?.size)
        assertEquals("Europe/Zurich", successResponse.body?.get(0)?.timeZoneId)
        assertEquals(1698792598946L, successResponse.body?.get(0)?.sleepStart)
    }

    @Test
    fun `test getStatStatisticEntity parses sleep score with GMT offset correctly`() = runTest {
        val mockResponseBody = """
            [
              {
                "id": "8ff6b05d-6b3c-4833-9e2f-d40f2fa88543:003-001-001-002:2022-02",
                "code": "003-001-001-002",
                "extras": null,
                "participationId": "8ff6b05d-6b3c-4833-9e2f-d40f2fa88543",
                "publicName": "Sleep Score",
                "timestamp": "2022-02",
                "metrics": {
                  "schema": {
                    "fields": [
                      {"name": "index", "type": "datetime", "tz": "UTC"},
                      {"name": "sleep-score", "type": "number"},
                      {"name": "ci-l", "type": "number"},
                      {"name": "ci-h", "type": "number"},
                      {"name": "wake-utc", "type": "integer"},
                      {"name": "time-zone", "type": "string"},
                      {"name": "conf", "type": "number"}
                    ],
                    "primaryKey": ["index"],
                    "pandas_version": "1.4.0"
                  },
                  "data": [
                    {
                      "index": "2022-02-15T00:00:00.000Z",
                      "sleep-score": 55.4367656755,
                      "ci-l": 51.1507898524,
                      "ci-h": 67.195157794,
                      "wake-utc": 1644908400668.0,
                      "time-zone": "GMT+1:0",
                      "conf": 0.5281669982
                    }
                  ]
                }
              }
            ]
        """.trimIndent()

        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockResponseBody)
        mockWebServer.enqueue(response)

        val apiResponse = apiService.getStatStatisticEntity("identityId", "participationId", "filter", "003")
        assertTrue(apiResponse is ApiSuccessResponse)
        val successResponse = apiResponse as ApiSuccessResponse
        assertEquals(1, successResponse.body?.size)
        assertEquals(55.4367656755, successResponse.body?.get(0)?.value)
    }

    @Test
    fun `test parseZoneId handles all formats robustly`() {
        assertEquals("Europe/Zurich", com.quantactions.sdk.data.parseZoneId("Europe/Zurich").id)
        assertEquals("GMT+01:00", com.quantactions.sdk.data.parseZoneId("GMT+1:0").id)
        assertEquals("GMT-05:00", com.quantactions.sdk.data.parseZoneId("GMT-5:0").id)
        assertEquals("GMT", com.quantactions.sdk.data.parseZoneId("GMT+0:0").id)
        assertEquals("GMT+08:00", com.quantactions.sdk.data.parseZoneId("GMT+8:0").id)
        assertEquals("GMT+09:00", com.quantactions.sdk.data.parseZoneId("GMT+9:0").id)
        assertEquals("UTC", com.quantactions.sdk.data.parseZoneId("UTC").id)
        assertEquals("UTC", com.quantactions.sdk.data.parseZoneId(null).id)
        assertEquals("UTC", com.quantactions.sdk.data.parseZoneId("").id)
        assertEquals("UTC", com.quantactions.sdk.data.parseZoneId("invalid_zone_id").id)
    }

    @Test
    fun `test parseFlexDateTime handles multiple date formats`() {
        val dt1 = com.quantactions.sdk.data.parseFlexDateTime("2023-11-01T10:15:30.123Z")
        assertEquals(2023, dt1.year)
        assertEquals(11, dt1.monthValue)
        assertEquals(1, dt1.dayOfMonth)

        val dt2 = com.quantactions.sdk.data.parseFlexDateTime("2023-11-01T10:15:30Z")
        assertEquals(2023, dt2.year)
        assertEquals(11, dt2.monthValue)

        val dt3 = com.quantactions.sdk.data.parseFlexDateTime("2023-11-01")
        assertEquals(2023, dt3.year)
        assertEquals(11, dt3.monthValue)

        val dt4 = com.quantactions.sdk.data.parseFlexDateTime(null)
        assertEquals(1970, dt4.year)
    }

    @Test
    fun `test prepareReturnData handles legacy GMT offset entities gracefully`() {
        val sleepSummaryEntities = listOf(
            com.quantactions.sdk.data.entity.SleepSummaryEntity(
                id = "001-002-006-0041700000000",
                timestamp = 1700000000L,
                sleepStart = 1700000000000L,
                sleepEnd = 1700028800000L,
                interruptionsStart = listOf(1700010000000L),
                interruptionsEnd = listOf(1700011000000L),
                interruptionsNumberOfTaps = listOf(5),
                timeZoneId = "GMT+1:0"
            )
        )

        val timeSeries = com.quantactions.sdk.Metric.SLEEP_SUMMARY.prepareReturnData(sleepSummaryEntities, 0L, 2000000000000L)
        assertEquals(1, timeSeries.values.size)
        assertEquals(1, timeSeries.timestamps.size)

        val statisticEntities = listOf(
            com.quantactions.sdk.data.entity.StatisticEntity(
                id = "003-001-001-0021700000000",
                stat = "003-001-001-002",
                timestamp = 1700000000L,
                value = 75.0,
                timeZone = "GMT+1:0",
                reset = 0,
                confidenceIntervalLow = 70.0,
                confidenceIntervalHigh = 80.0,
                confidence = 0.95
            )
        )

        val scoreTimeSeries = com.quantactions.sdk.Metric.SLEEP_SCORE.prepareReturnData(statisticEntities, 0L, 2000000000000L)
        assertEquals(1, scoreTimeSeries.values.size)
        assertEquals(75.0, scoreTimeSeries.values[0], 0.001)
    }
}

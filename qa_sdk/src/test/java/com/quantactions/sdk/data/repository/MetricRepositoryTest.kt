/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.Metric
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.adapters.SleepSummaryAdapter
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.adapters.StatisticStringAdapter
import com.quantactions.sdk.data.api.adapters.TrendAdapter
import com.quantactions.sdk.data.entity.StatisticEntity
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.local.MVPRoomDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.HttpURLConnection
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MetricRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: MVPRoomDatabase
    private lateinit var mvpDao: MVPDao
    private lateinit var metricRepository: MetricRepository
    private lateinit var apiService: ApiService

    private val identityRepository: IdentityRepository = mockk(relaxed = true)
    private val networkManager: NetworkManager = mockk()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val context: Context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(
            context,
            MVPRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        mvpDao = database.mvpDao()

        val customMoshi = Moshi.Builder()
            .add(QuestionnaireAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addCallAdapterFactory(FlowerCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(customMoshi))
            .build()

        apiService = retrofit.create(ApiService::class.java)

        every { networkManager.apiService } returns apiService
        every { identityRepository.identityId } returns "test_user_id"

        metricRepository = MetricRepository(
            identityRepository = identityRepository,
            networkManager = networkManager,
            mvpDao = mvpDao
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        database.close()
    }

    @Test
    fun testGetStatEmitsCachedLocalDataWithoutNetworkCallWhenFresh() = runTest {
        val nowSec = Instant.now().epochSecond
        val sampleStat = StatisticEntity(
            id = "test_stat_1",
            stat = Metric.SLEEP_SCORE.code,
            timestamp = nowSec,
            value = 82.5,
            timeZone = "UTC",
            reset = 0,
            confidenceIntervalLow = 75.0,
            confidenceIntervalHigh = 90.0,
            confidence = 0.95
        )

        mvpDao.insertOrUpdateMetric(listOf(sampleStat))

        val flow = metricRepository.getStat(
            metricOrTrend = Metric.SLEEP_SCORE,
            iamParticipationId = "", // No participation ID -> fetch only local
            from = (nowSec - 3600) * 1000,
            to = (nowSec + 3600) * 1000,
            refresh = false
        )

        val result = flow.first()
        assertNotNull(result)
        assertTrue(result.values.isNotEmpty())
        assertEquals(82.5, result.values.first(), 0.001)
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun testGetStatFetchesFromNetworkOnRefreshAndUpdatesRoom() = runTest {
        val dateIso = "2026-09-14T00:00:00.000Z"
        val targetInstant = java.time.ZonedDateTime.parse(dateIso).toInstant()
        val fromMillis = targetInstant.minusSeconds(86400).toEpochMilli()
        val toMillis = targetInstant.plusSeconds(86400).toEpochMilli()

        val statResponse = com.quantactions.sdk.data.api.responses.StatisticResponse(
            code = Metric.SLEEP_SCORE.code,
            extras = null,
            participationId = "test_participation_123",
            id = "123",
            metrics = com.quantactions.sdk.data.api.responses.StatisticCore(
                schema = com.quantactions.sdk.data.api.responses.DataFrameSchema(
                    fields = listOf(
                        mapOf("name" to "index", "type" to "datetime"),
                        mapOf("name" to "sleep-score", "type" to "number"),
                        mapOf("name" to "ci-l", "type" to "number"),
                        mapOf("name" to "ci-h", "type" to "number"),
                        mapOf("name" to "conf", "type" to "number")
                    ),
                    primaryKey = listOf("index"),
                    pandas_version = "1.4.0"
                ),
                data = listOf(
                    mapOf(
                        "index" to dateIso,
                        "sleep-score" to 91.0,
                        "ci-l" to 85.0,
                        "ci-h" to 95.0,
                        "conf" to 0.9
                    )
                )
            ),
            publicName = "Sleep Score",
            timestamp = "2026-09-14"
        )

        val listType = com.squareup.moshi.Types.newParameterizedType(
            List::class.java,
            com.quantactions.sdk.data.api.responses.StatisticResponse::class.java
        )
        val customMoshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter = customMoshi.adapter<List<com.quantactions.sdk.data.api.responses.StatisticResponse>>(listType)
        val mockResponseBody = jsonAdapter.toJson(listOf(statResponse))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(mockResponseBody)
        )

        val flow = metricRepository.getStat(
            metricOrTrend = Metric.SLEEP_SCORE,
            iamParticipationId = "test_participation_123",
            from = fromMillis,
            to = toMillis,
            refresh = true
        )

        val result = flow.first()
        assertNotNull(result)

        // Verify request was sent to MockWebServer with participationId
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path?.contains("test_participation_123") == true)

        // Verify data was written to Room database
        val cachedFromRoom = mvpDao.getMetricStatistic(
            Metric.SLEEP_SCORE.code,
            targetInstant.epochSecond - 86400,
            targetInstant.epochSecond + 86400
        ).first()

        assertTrue("Cached Room list should have entry", cachedFromRoom.isNotEmpty())
        assertEquals(91.0, cachedFromRoom.first().value, 0.001)
    }

    @Test
    fun testGetStatEmitsCachedDataWhenNetworkFails() = runTest {
        val nowSec = Instant.now().epochSecond
        val cachedStat = StatisticEntity(
            id = "cached_stat_1",
            stat = Metric.SLEEP_SCORE.code,
            timestamp = nowSec,
            value = 77.0,
            timeZone = "UTC",
            reset = 0,
            confidenceIntervalLow = 70.0,
            confidenceIntervalHigh = 84.0,
            confidence = 0.8
        )
        mvpDao.insertOrUpdateMetric(listOf(cachedStat))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                .setBody("""{"error": "Server unavailable"}""")
        )

        val flow = metricRepository.getStat(
            metricOrTrend = Metric.SLEEP_SCORE,
            iamParticipationId = "test_participation_123",
            from = (nowSec - 3600) * 1000,
            to = (nowSec + 3600) * 1000,
            refresh = true
        )

        val result = flow.first()
        assertNotNull(result)
        assertEquals(77.0, result.values.first(), 0.001)
    }

    @Test
    fun testGetStatChecksLatestTimestampFor3HourCacheTtlAndNotOldest() = runTest {
        val nowSec = Instant.now().epochSecond
        val oldStat = StatisticEntity(
            id = "old_stat",
            stat = Metric.SLEEP_SCORE.code,
            timestamp = nowSec - 10 * 86400, // 10 days ago (> 3 hours old)
            value = 70.0,
            timeZone = "UTC",
            reset = 0,
            confidenceIntervalLow = 65.0,
            confidenceIntervalHigh = 75.0,
            confidence = 0.9
        )
        val freshStat = StatisticEntity(
            id = "fresh_stat",
            stat = Metric.SLEEP_SCORE.code,
            timestamp = nowSec - 1800, // 30 minutes ago (< 3 hours old)
            value = 85.0,
            timeZone = "UTC",
            reset = 0,
            confidenceIntervalLow = 80.0,
            confidenceIntervalHigh = 90.0,
            confidence = 0.95
        )
        mvpDao.insertOrUpdateMetric(listOf(oldStat, freshStat))

        val flow = metricRepository.getStat(
            metricOrTrend = Metric.SLEEP_SCORE,
            iamParticipationId = "test_participation_123",
            from = (nowSec - 15 * 86400) * 1000,
            to = (nowSec + 3600) * 1000,
            refresh = false
        )

        val result = flow.first()
        assertEquals(2, result.values.size)
        // Because the newest entry is only 30 minutes old (< 3 hours), no network request should be made
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun testGetStatUsesIncrementalMonthlyDeltaSyncWhenStaleCacheExists() = runTest {
        val staleJanInstant = java.time.ZonedDateTime.parse("2026-03-10T00:00:00.000Z").toInstant()
        val fromJanInstant = java.time.ZonedDateTime.parse("2026-01-01T00:00:00.000Z").toInstant()
        val toAprInstant = java.time.ZonedDateTime.parse("2026-04-15T00:00:00.000Z").toInstant()

        mvpDao.insertOrUpdateMetric(
            listOf(
                StatisticEntity(
                    id = "cached_march",
                    stat = Metric.SLEEP_SCORE.code,
                    timestamp = staleJanInstant.epochSecond,
                    value = 78.0,
                    timeZone = "UTC",
                    reset = 0,
                    confidenceIntervalLow = 72.0,
                    confidenceIntervalHigh = 84.0,
                    confidence = 0.9
                )
            )
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("[]")
        )

        val flow = metricRepository.getStat(
            metricOrTrend = Metric.SLEEP_SCORE,
            iamParticipationId = "test_participation_123",
            from = fromJanInstant.toEpochMilli(),
            to = toAprInstant.toEpochMilli(),
            refresh = false
        )

        flow.first()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("2026-03", recordedRequest.requestUrl?.queryParameter("start"))
        assertEquals("2026-04", recordedRequest.requestUrl?.queryParameter("end"))
        assertEquals(Metric.SLEEP_SCORE.code, recordedRequest.requestUrl?.queryParameter("code"))
        assertEquals(null, recordedRequest.requestUrl?.queryParameter("containerId"))
    }

    @Test
    fun testCacheAllHealthyRangesBatchesAllCodesInSingleRequest() = runTest {
        val preferences: com.quantactions.sdk.GenericPreferences = mockk(relaxed = true)
        every { preferences.getHealthyRanges(any()) } returns com.quantactions.sdk.PopulationRange()

        val batchResponseJson = """
            [
                {
                    "id": "003-001-001-002",
                    "code": "003-001-001-002",
                    "ranges": {
                        "lower": {"0": 0.25},
                        "median": {"0": 0.50},
                        "upper": {"0": 0.75}
                    }
                },
                {
                    "id": "003-001-001-003",
                    "code": "003-001-001-003",
                    "ranges": {
                        "lower": {"0": 0.30},
                        "median": {"0": 0.60},
                        "upper": {"0": 0.85}
                    }
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(batchResponseJson)
        )

        Metric.cacheAllHealthyRanges(apiService, preferences)

        assertEquals(1, mockWebServer.requestCount)
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        val codesParam = recordedRequest.requestUrl?.queryParameter("codes")
        assertNotNull(codesParam)
        assertTrue(codesParam!!.contains(Metric.SLEEP_SCORE.code))
        assertTrue(codesParam.contains(Metric.COGNITIVE_FITNESS.code))
        assertTrue(codesParam.contains(Metric.SOCIAL_ENGAGEMENT.code))
        io.mockk.verify { preferences.saveHealthyRanges(Metric.SLEEP_SCORE.code, any()) }
        io.mockk.verify { preferences.saveHealthyRanges(Metric.COGNITIVE_FITNESS.code, any()) }
    }
}

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
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.adapters.SleepSummaryAdapter
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.adapters.StatisticStringAdapter
import com.quantactions.sdk.data.api.adapters.TrendAdapter
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.TapDataParsed
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.local.MVPRoomDatabase
import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.model.TapDataBody
import com.quantactions.sdk.exceptions.QASDKException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: MVPRoomDatabase
    private lateinit var mvpDao: MVPDao
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var apiService: ApiService

    private val preferences: ManagePref2 = mockk(relaxed = true)
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
            .add(StatisticAdapter())
            .add(StatisticStringAdapter())
            .add(SleepSummaryAdapter())
            .add(QuestionnaireAdapter())
            .add(TrendAdapter())
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
        every { identityRepository.identityId } returns "test_identity_123"
        every { identityRepository.deviceID } returns "test_device_456"

        deviceRepository = DeviceRepository(
            context = context,
            preferences = preferences,
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
    fun testSubmitTapDataParsedSendsCorrectRequest() = runTest {
        val jsonResponse = """
            {
                "records": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(jsonResponse)
        )

        val tapDataBody = TapDataBody(listOf())
        val response = deviceRepository.submitTapDataParsed(tapDataBody)

        assertTrue("Expected ApiSuccessResponse", response is ApiSuccessResponse)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/flows/identities/test_identity_123/devices/test_device_456/recordings", recordedRequest.path)
    }

    @Test
    fun testSubmitNoteThrowsExceptionOnError() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .setBody("""{"message": "Invalid note content"}""")
        )

        try {
            deviceRepository.submitNote("Hello test note")
            org.junit.Assert.fail("Expected QASDKException to be thrown")
        } catch (e: QASDKException) {
            assertTrue(e.message?.contains("API Error") == true)
        }
    }

    @Test
    fun testUpdateAppListMapsResponseCorrectly() = runTest {
        val jsonResponse = """
            [
                {
                    "package": "com.quantactions.testapp",
                    "installationId": 42,
                    "categoryMain": "health_and_fitness"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(jsonResponse)
        )

        val appList = listOf(AppToPush("com.quantactions.testapp", 42))
        val response = deviceRepository.updateAppList(appList)

        assertTrue(response is ApiSuccessResponse)
        val body = (response as ApiSuccessResponse).body
        assertNotNull(body)
        assertEquals(1, body?.size)
        assertEquals("com.quantactions.testapp", body?.get(0)?.`package`)
        assertEquals("health_and_fitness", body?.get(0)?.categoryMain)
        assertEquals(42, body?.get(0)?.installationId)
    }

    @Test
    fun testTapDataSyncLifecycleViaLocalDao() = runTest {
        val tapSession = TapDataParsed(
            id = 0,
            taps = "[10, 20, 30]",
            start = 5000L,
            stop = 6000L,
            orientations = "[1, 1, 1]",
            appIds0 = "[1, 1, 1]",
            appIds1 = "[]",
            appIds2 = "[]",
            tapsSession = 3L,
            lengthSession = 1000L,
            timeZone = "UTC",
            inCharge = "0",
            sync = 0 // Pending sync
        )

        mvpDao.insertOrUpdateTapDataParsed(tapSession)

        // Verify pending
        val pendingToSync = deviceRepository.getTapDataParsedToSync()
        assertEquals(1, pendingToSync.size)
        assertEquals(5000L, pendingToSync[0].start)

        // Mark as synced
        deviceRepository.updateTapDataParsedSyncStatus(listOf(5000L))

        // Verify no longer pending
        val pendingAfterUpdate = deviceRepository.getTapDataParsedToSync()
        assertEquals(0, pendingAfterUpdate.size)
    }

    @Test
    fun testGetTapsInTimeWindowAndAppCodes() = runTest {
        val appCode = CodeOfApp(id = 10, appName = "com.android.chrome", sync = 1, category = "browser")
        deviceRepository.insertOrUpdateAppCode(listOf(appCode))

        val tapSession = TapDataParsed(
            id = 0,
            taps = "[100, 200]",
            start = 10000L,
            stop = 12000L,
            orientations = "[1, 1]",
            appIds0 = "[10, 10]",
            appIds1 = "[]",
            appIds2 = "[]",
            tapsSession = 2L,
            lengthSession = 2000L,
            timeZone = "UTC",
            inCharge = "0",
            sync = 1
        )
        mvpDao.insertOrUpdateTapDataParsed(tapSession)

        val retrievedTaps = deviceRepository.getTapsInTimeWindow(9000L, 13000L)
        assertEquals(1, retrievedTaps.size)
        assertEquals(10000L, retrievedTaps[0].start)

        val retrievedApps = deviceRepository.getAppCodesByIds(listOf(10))
        assertEquals(1, retrievedApps.size)
        assertEquals("com.android.chrome", retrievedApps[0].appName)
    }
}

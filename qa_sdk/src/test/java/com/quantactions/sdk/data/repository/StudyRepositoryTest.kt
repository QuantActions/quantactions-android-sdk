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
import androidx.work.WorkManager
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.TokenApi
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.adapters.SleepSummaryAdapter
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.adapters.StatisticStringAdapter
import com.quantactions.sdk.data.api.adapters.TrendAdapter
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.local.MVPRoomDatabase
import com.quantactions.sdk.exceptions.QASDKException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: MVPRoomDatabase
    private lateinit var mvpDao: MVPDao
    private lateinit var studyRepository: StudyRepository
    private lateinit var apiService: ApiService
    private val tokenApi: TokenApi = mockk(relaxed = true)

    private val preferences: ManagePref2 = mockk(relaxed = true)
    private val identityRepository: IdentityRepository = mockk(relaxed = true)
    private val networkManager: NetworkManager = mockk()
    private val workManager: WorkManager = mockk(relaxed = true)

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
        every { networkManager.tokenApi } returns tokenApi
        every { identityRepository.identityId } returns "test_user_id"

        studyRepository = StudyRepository(
            preferences = preferences,
            identityRepository = identityRepository,
            networkManager = networkManager,
            mvpDao = mvpDao,
            workManager = workManager
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        database.close()
    }

    @Test
    fun testRegisterToStudySavesParticipationIdAndCohort() = runTest {
        val registerResponseJson = """
            {
                "id": "part_12345",
                "studyId": "study_abc",
                "ttl": "2026-12-31T23:59:59Z"
            }
        """.trimIndent()

        val participationsJson = """
            [
                {
                    "id": "part_12345",
                    "studyId": "study_abc",
                    "study": {
                        "id": "study_abc",
                        "name": "Test Study",
                        "privacyPolicy": "https://example.com/privacy",
                        "privacyPolicyDate": "2024-01-01",
                        "organisationId": "org_1",
                        "created": "2024-01-01",
                        "enableWithdraw": true,
                        "enableDeviceIdAccess": false,
                        "enableDeviceNotes": false,
                        "enableHighGpsResolution": false,
                        "enableLocationAccess": false,
                        "enableRawDataAccess": false,
                        "enableStudyIdSignUp": true,
                        "enableSyncOnScreenOff": false,
                        "enableAppIdAccess": true,
                        "enableDrawOverAccess": false,
                        "enableCognitiveTests": true
                    }
                }
            ]
        """.trimIndent()

        val questionnairesJson = "[]"

        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(registerResponseJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(participationsJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(questionnairesJson))

        val result = studyRepository.registerToStudy("study_abc")

        assertEquals("part_12345", result.subscriptionId)
        verify { preferences.participationId = "part_12345" }

        val cachedStudies = mvpDao.getStudies()
        assertEquals(1, cachedStudies.size)
        val cohort = cachedStudies[0]
        assertEquals("study_abc", cohort.cohortId)
        assertEquals("Test Study", cohort.cohortName)
        assertEquals(1, cohort.permAppId)
        assertEquals(0, cohort.permDrawOver)
        assertEquals(true, cohort.enableCognitiveTests)
    }

    @Test
    fun testWithdrawClearsParticipationId() = runTest {
        every { preferences.participationId } returns "part_12345"
        val withdrawResponseJson = """
            {
                "id": "part_12345",
                "studyId": "study_abc"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(withdrawResponseJson))

        studyRepository.withdraw("part_12345", "study_abc")

        verify { preferences.participationId = "" }
    }

    @Test
    fun testSendQuestionnaireResponseThrowsOnApiError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST).setBody("{\"error\":\"Invalid answer\"}"))

        val responseEntity = QuestionnaireResponseEntity(
            id = 1L,
            qFullID = "study_abc:q_1",
            qName = "Survey",
            qCode = "q_1",
            qDate = System.currentTimeMillis(),
            qResponse = "{\"question_1\": 5}"
        )

        var thrown = false
        try {
            studyRepository.sendQuestionnaireResponse(responseEntity)
        } catch (e: QASDKException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}

/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk

import android.util.Base64
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getArguments
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_core.Resource
import com.quantactions.sdk.data.api.*
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.responses.DataFrameSchema
import com.quantactions.sdk.data.api.responses.StatisticCore
import com.quantactions.sdk.data.api.responses.StatisticData
import com.quantactions.sdk.data.api.responses.StatisticResponse
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.model.QuestionnaireResponse
import com.quantactions.sdk.data.model.SignUpForStudy
import com.quantactions.sdk.data.repository.*
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import junit.framework.TestCase
import junit.framework.TestCase.fail
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.runner.RunWith
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class SDKFunctionalityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private var apiKey = getArguments().getString("apiKey") ?: ""
    private var identityId = getArguments().getString("testIdentityId") ?: ""
    private var deviceId = getArguments().getString("testDeviceId") ?: ""
    private var password = getArguments().getString("testPassword") ?: ""
    private var participationId = getArguments().getString("testParticipationId") ?: ""
    private var studyId = getArguments().getString("testStudyId") ?: ""
    private var fbToken = "thisIsATestFirebaseToken"

    @OptIn(DelicateCoroutinesApi::class)
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    private lateinit var repository: MVPRepository
    private lateinit var preferences: ManagePref2


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = ManagePref2.getInstance(appContext)
        preferences.isOauthActivated = false
        preferences.isDeviceRegistered = true
        preferences.areCredentialsRegistered = true
        preferences.apiKey = apiKey
        preferences.identityId = identityId
        preferences.deviceID = deviceId
        preferences.password = password
        preferences.setFBCode(fbToken)
        repository = MVPRepository.getInstance(appContext, apiKey)
        runBlocking {
            repository.checkRegisteredStatus()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }


    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.quantactions.sdk.test", appContext.packageName)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUpdateDeviceInfo() {

        val job = runBlocking {
            when (val response = repository.updateDeviceInfo()) {
                is ApiErrorResponse -> {
                    Resource.error("", 404, null)
                }

                is ApiEmptyResponse -> {
                    Resource.error("", 404, null)
                }

                is ApiSuccessResponse -> {
                    Resource.success(response)
                }
            }
        }
        assert(job.status is Resource.Status.Success)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSubmitHealth() {

        val deviceHealthBody = HealthDataBody(Payloads().deviceHealthParsedPayload)
        val job = runBlocking {
            when (val response = repository.submitHealthDataParsed(deviceHealthBody)) {
                is ApiErrorResponse -> Resource.error("", 404, null)
                is ApiEmptyResponse -> Resource.error("", 404, null)
                is ApiSuccessResponse -> Resource.success(response)
            }
        }
        assert(job.status is Resource.Status.Success)
    }

    @Test
    @Throws(InterruptedException::class, SDKNotInitialisedException::class)
    fun testSubmitQResponse() {

        val answer = QuestionnaireResponseEntity(
            0, "${studyId}:017ebd77-4ac6-4af5-9aa9-3ef11d95ecca",
            "testQ",
            "qa-basic-v1",
            1633408038806,
            "{\"qabasics1q1\":6,\"qabasics2q1\":6}"
        )

        runBlocking {
            repository.sendQuestionnaireResponse(
                answer
            )
        }
    }

    @Test
    @Throws(InterruptedException::class, SDKNotInitialisedException::class)
    fun testSendNote() {

        runBlocking {
            repository.submitNote("test note", identityId)
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUpdateAppList() {

        val job = runBlocking {

            when (val response =
                repository.updateAppList(Payloads().appsToPush)) {
                is ApiErrorResponse -> {
                    Log.e("ErrorTest", response.errorMessage)
                    Resource.error("", 404, null)
                }

                is ApiEmptyResponse -> Resource.error("", 404, null)
                is ApiSuccessResponse -> Resource.success(response)
            }
        }
        assert(job.status is Resource.Status.Success)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSubmitStatistic() {

        val tapsStats = TapsStats(repository.mvpDao).toDeviceStats()

        val job = runBlocking {

            when (val response = repository.submitStatistic(tapsStats)) {
                is ApiErrorResponse -> {
                    Resource.error("", 404, null)
                }

                is ApiEmptyResponse -> {
                    Resource.error("", 404, null)
                }

                is ApiSuccessResponse -> {
                    Log.d("TEST", response.body.toString())
                    Resource.success(response)
                }
            }
        }
        assert(job.status is Resource.Status.Success)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSubmitTapDataParsed() {


        val tapData = TapDataBody(Payloads().tapDataParsedPayload)

        val job = runBlocking {
            when (val response = repository.submitTapDataParsed(tapData)) {
                is ApiErrorResponse -> {
                    Resource.error("", 404, null)
                }

                is ApiSuccessResponse -> {
                    Resource.success(response)
                }

                is ApiEmptyResponse -> {
                    Resource.error("", 404, null)
                }
            }
        }

        assert(job.status is Resource.Status.Success)

    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testGetStat() = runTest {
        val thisFlow = repository.getStat(
            Metric.SLEEP_SCORE,
            from = 0,
            to = Instant.now().toEpochMilli(),
            sampleParticipationId = "138e8ff6b05d6b3c48339e2fd40f2fa8854328eb",
            refresh = true
        )
        val stat = thisFlow.first()

        Log.d("TEST", stat.size.toString())
        TestCase.assertEquals(61.2547207966, stat.values.first())

    }


    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testGetJournalEntries() = runTest {
        val entries = repository.cacheJournal()
        TestCase.assertEquals(entries.size, 0)
    }

    @Test
    @Throws(InterruptedException::class, SDKNotInitialisedException::class)
    fun testSignUpForStudyAndWithdraw() = runTest {

        repository.registerToStudyWithParticipationId(participationId)

        repository.withdraw(participationId, studyId)

//        repository.reSubscribe(participationId)

    }
//    fun <T> LiveData<T>.getOrAwaitValue(
//        time: Long = 10,
//        timeUnit: TimeUnit = TimeUnit.SECONDS
//    ): T {
//        var data: T? = null
//        val latch = CountDownLatch(1)
//        val observer = object : Observer<T> {
//            override fun onChanged(o: T?) {
//                data = o
//                latch.countDown()
//                this@getOrAwaitValue.removeObserver(this)
//            }
//        }
//
//        this.observeForever(observer)
//
//        // Don't wait indefinitely if the LiveData is not set.
//        if (!latch.await(time, timeUnit)) {
//            throw TimeoutException("LiveData value was never set.")
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        return data as T
//    }


//    @Test
//    fun testIamEntityRegistration() {
//
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//
//        val preferences = ManagePref2.getInstance(appContext)
//
//        preferences.deviceID = deviceId
//        preferences.password = password
//        preferences.gender
//
//        val cookieJar = ApiService.UvCookieJar(preferences, "ApiService")
//        val tokenApi = TokenApi.buildTokenApi(apiKey, cookieJar)
//        val tokenAuthenticator = TokenAuthenticator(tokenApi, preferences)
//        val iamApi = ApiService.create(apiKey, tokenAuthenticator, cookieJar)
////        val checkRegisteredStatus = CheckRegisteredStatus(preferences, iamApi)
//
////        val apiService = ApiService.create(apiKey, tokenAuthenticator, checkRegisteredStatus, preferences.accessToken ?: "")
//
//
//        runBlocking {
//            Log.d("testIamEntityRegistration", "calling testIamEntityRegistration...")
//
//            val identityRegistration = ApiService.IdentityRegistration(
//                id = deviceId,
//                gender = "M",
//                yearOfBirth = 1991,
//                nameAlias = "device"
//            )
//
//            when (val registerResponse = iamApi.registerIdentity(
//                identityRegistration
//            )) {
//                is ApiSuccessResponse -> {
//                    println("IN API SUCC")
//                    println(registerResponse.toString())
//                }
//
//                else -> {
//                    println("IN API ELSE")
//                    println(registerResponse.toString())
//                }
//            }
//
//        }
//    }

    @Test
    fun testStatisticAdapter() {

//        val params = mapOf(
//            "saturationQuantile" to 0.9,
//            "weights" to listOf(0.167, 0.167, 0.167, 0.167, 0.167, 0.167), "version" to "v2.3"
//        )

        val fields = listOf(
            mapOf(
                "name" to "index",
                "type" to "datetime",
                "tz" to "UTC"
            ),
            mapOf(
                "name" to "sleep-score",
                "type" to "number"
            ),
            mapOf(
                "name" to "ci-l",
                "type" to "number"
            ),
            mapOf(
                "name" to "ci-h",
                "type" to "number"
            ),
            mapOf(
                "name" to "conf",
                "type" to "number"
            ),
            mapOf(
                "name" to "wake-utc",
                "type" to "number"
            ),
            mapOf(
                "name" to "time-zone",
                "type" to "string"
            )
        )


        val schema = DataFrameSchema(fields, listOf("index"), "1.4.0")

        val data = listOf(
            mapOf(
                "index" to "2022-11-22T00:00:00.000Z",
                "sleep-score" to 76.3009121357,
                "ci-l" to 55.1270515367,
                "ci-h" to 77.6004343099,
                "conf" to 0.3,
                "wake-utc" to null,
                "time-zone" to null
            ),
            mapOf(
                "index" to "2022-11-23T00:00:00.000Z",
                "sleep-score" to 78.2438443317,
                "ci-l" to 57.1255167622,
                "ci-h" to 78.7226169463,
                "conf" to 0.3056436227,
                "wake-utc" to null,
                "time-zone" to null
            ),
            mapOf(
                "index" to "2023-01-25T00:00:00.000Z",
                "sleep-score" to 42.9087047003,
                "ci-l" to 38.3708034625,
                "ci-h" to 53.9345492968,
                "conf" to 0.2896419051,
                "wake-utc" to 1674596552472.0,
                "time-zone" to "Europe/Copenhagen"
            )
        )

        val core = StatisticCore(schema, data)

//        val statisticData = StatisticData(params, core)

        val statisticResponse =
            StatisticResponse(
                "1234",
                "003-001-001-002-RO:1",
                "4321",
                "1234",
                core,
                "1234",
                "2023-05"
            )

        val listOfStatisticEntity = StatisticAdapter().fromJson(listOf(statisticResponse))

        println(listOfStatisticEntity.toString())

    }

    @Test
    fun testParseLiteralList() {
        val literalListOfInt = "[0,1,2,3,4,5,6,7,8,9]"
        val literalListOfLong =
            "[1641471608647, 1641471615774, 1641471615905, 1641471616045, 1641471616189, 1641471616333, 1641471616461]"

        val listOfInt = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val listOfLong = listOf(
            1641471608647,
            1641471615774,
            1641471615905,
            1641471616045,
            1641471616189,
            1641471616333,
            1641471616461
        )

        assert(literalListOfInt.literalToIntList() == listOfInt)
        assert(literalListOfLong.literalToLongList() == listOfLong)

        val literalEmptyListOfInt1 = "[]"
        val literalEmptyListOfLong1 = "[]"
        val literalEmptyListOfInt2 = ""
        val literalEmptyListOfLong2 = ""

        val emptyListOfInt = listOf<Int>()
        val emptyListOfLong = listOf<Long>()

        assert(literalEmptyListOfInt1.literalToIntList() == emptyListOfInt)
        assert(literalEmptyListOfLong1.literalToLongList() == emptyListOfLong)
        assert(literalEmptyListOfInt2.literalToIntList() == emptyListOfInt)
        assert(literalEmptyListOfLong2.literalToLongList() == emptyListOfLong)

        val literalListOfInt1 = "[7]"
        val literalListOfLong1 = "[1641471616045]"
        val literalListOfInt2 = "7"
        val literalListOfLong2 = "1641471616045"

        val listOfInt1 = listOf(7)
        val listOfLong1 = listOf(1641471616045)

        assert(literalListOfInt1.literalToIntList() == listOfInt1)
        assert(literalListOfLong1.literalToLongList() == listOfLong1)

        assert(literalListOfInt2.literalToIntList() == listOfInt1)
        assert(literalListOfLong2.literalToLongList() == listOfLong1)

    }

}

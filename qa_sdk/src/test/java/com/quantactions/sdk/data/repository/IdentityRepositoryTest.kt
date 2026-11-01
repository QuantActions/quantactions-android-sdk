/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.QA
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.TokenApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
class IdentityRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var identityRepository: IdentityRepository
    private val preferences: ManagePref2 = mockk(relaxed = true)
    private val networkManager: NetworkManager = mockk(relaxed = true)
    private lateinit var tokenApi: TokenApi
    private lateinit var apiService: ApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        every { preferences.identityId } returns "test-identity-id"
        every { preferences.password } returns "test-password"
        every { preferences.gender } returns QA.Gender.MALE
        every { preferences.selfDeclaredHealthy } returns true
        every { preferences.areCredentialsRegistered } returns false
        every { preferences.isOauthActivated } returns false

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(FlowerCallAdapterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
        tokenApi = retrofit.create(TokenApi::class.java)
        every { networkManager.apiService } returns apiService
        every { networkManager.tokenApi } returns tokenApi

        identityRepository = IdentityRepository(
            preferences = preferences,
            networkManager = networkManager
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testUpdateIdentitySendsNullYearOfBirthWhenZeroOrInvalid() = runTest {
        every { preferences.yearOfBirth } returns 0

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"id": "test-identity-id", "gender": "M", "yearOfBirth": null, "selfDeclaredHealthy": true}""")
        )

        val response = identityRepository.updateIdentity()
        assertTrue(response is ApiSuccessResponse)

        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/flows/identities/test-identity-id", request.path)

        val requestJson = JSONObject(request.body.readUtf8())
        assertFalse("yearOfBirth should not be non-null when yearOfBirth is 0", requestJson.has("yearOfBirth") && !requestJson.isNull("yearOfBirth"))
    }

    @Test
    fun testUpdateIdentitySendsYearOfBirthWhenValid() = runTest {
        every { preferences.yearOfBirth } returns 1995

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"id": "test-identity-id", "gender": "M", "yearOfBirth": 1995, "selfDeclaredHealthy": true}""")
        )

        val response = identityRepository.updateIdentity()
        assertTrue(response is ApiSuccessResponse)

        val request = mockWebServer.takeRequest()
        val requestJson = JSONObject(request.body.readUtf8())
        assertTrue("yearOfBirth should be present", requestJson.has("yearOfBirth"))
        assertEquals(1995, requestJson.getInt("yearOfBirth"))
    }

    @Test
    fun testCheckRegisteredStatusSendsNullYearOfBirthWhenZero() = runTest {
        every { preferences.yearOfBirth } returns 0

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"id": "test-identity-id", "gender": "M", "yearOfBirth": null, "selfDeclaredHealthy": true}""")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"token": "sample-token"}""")
        )

        val result = identityRepository.checkRegisteredStatus()
        assertTrue(result)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/flows/identities", request.path)

        val requestJson = JSONObject(request.body.readUtf8())
        assertFalse("yearOfBirth should not be non-null when yearOfBirth is 0", requestJson.has("yearOfBirth") && !requestJson.isNull("yearOfBirth"))
    }
}


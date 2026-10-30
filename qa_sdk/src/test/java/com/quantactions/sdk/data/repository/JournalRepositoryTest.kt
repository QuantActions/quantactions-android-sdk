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
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.adapters.SleepSummaryAdapter
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.adapters.StatisticStringAdapter
import com.quantactions.sdk.data.api.adapters.TrendAdapter
import com.quantactions.sdk.data.entity.JournalEntryEntity
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.local.MVPRoomDatabase
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
class JournalRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: MVPRoomDatabase
    private lateinit var mvpDao: MVPDao
    private lateinit var journalRepository: JournalRepository
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
        every { identityRepository.identityId } returns "test_user_id"

        journalRepository = JournalRepository(
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
    fun testDeleteJournalEntryMarksDeletedLocallyAndUpdatesSyncOnSuccess() = runTest {
        val entry = JournalEntryEntity(
            id = "entry_1",
            description = "Test note",
            deviceId = "test_user_id",
            created = "1700000000000",
            modified = "1700000000000",
            sync = 1,
            deleted = 0
        )
        mvpDao.insertOrUpdateEntry(entry)

        // API returns 200 or 204
        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT))

        journalRepository.deleteJournalEntry("entry_1")

        val stored = mvpDao.getEntry("entry_1")
        assertNotNull(stored)
        assertEquals(1, stored!!.deleted)
        assertEquals(1, stored.sync)
    }

    @Test
    fun testDeleteJournalEntryMarksDeletedLocallyAndLeavesPendingSyncOnError() = runTest {
        val entry = JournalEntryEntity(
            id = "entry_2",
            description = "Another note",
            deviceId = "test_user_id",
            created = "1700000000000",
            modified = "1700000000000",
            sync = 1,
            deleted = 0
        )
        mvpDao.insertOrUpdateEntry(entry)

        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR).setBody("Server error"))

        var thrown = false
        try {
            journalRepository.deleteJournalEntry("entry_2")
        } catch (e: QASDKException) {
            thrown = true
        }
        assertTrue(thrown)

        // On error, it was marked deleted=1 locally, but sync=0 so background worker will retry
        val stored = mvpDao.getEntry("entry_2")
        assertNotNull(stored)
        assertEquals(1, stored!!.deleted)
        assertEquals(0, stored.sync)
    }
}

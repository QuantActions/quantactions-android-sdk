/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.entity.TapDataParsed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MVPDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: MVPRoomDatabase
    private lateinit var mvpDao: MVPDao

    @Before
    fun setup() {
        // Use an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MVPRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        mvpDao = database.mvpDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetHourlyTaps() = runTest {
        val date = "2024-08-25"
        val hour = 14
        val entity = HourlyTapsEntity(
            id = 0, // Auto-generated
            date = date,
            hour = hour,
            taps = 150,
            speed = 3.5f
        )

        mvpDao.insertOrUpdateHourlyTapsEntity(listOf(entity))

        val retrieved = mvpDao.getTapsInDateAndHour(date, hour)
        
        // Assertions
        assertTrue(retrieved != null)
        assertEquals(date, retrieved?.date)
        assertEquals(hour, retrieved?.hour)
        assertEquals(150, retrieved?.taps)
        assertEquals(3.5f, retrieved?.speed)
    }
    
    @Test
    fun insertAndGetTapDataParsedToSync() = runTest {
        val tapData = TapDataParsed(
            id = 0,
            taps = "[1,2,3]",
            start = 1000L,
            stop = 2000L,
            orientations = "[1,1,1]",
            appIds0 = "[10,10,10]",
            appIds1 = "[]",
            appIds2 = "[]",
            tapsSession = 3L,
            lengthSession = 1000L,
            timeZone = "UTC",
            inCharge = "0",
            sync = 0 // Needs sync
        )
        
        mvpDao.insertOrUpdateTapDataParsed(tapData)
        
        val pendingSync = mvpDao.getTapDataParsedToSync()
        
        assertEquals(1, pendingSync.size)
        assertEquals(1000L, pendingSync[0].start)
        
        // Mark as synced
        mvpDao.updateTapDataParsedSyncStatus(listOf(1000L))
        
        val pendingSyncAfterUpdate = mvpDao.getTapDataParsedToSync()
        assertEquals(0, pendingSyncAfterUpdate.size)
    }

    @Test
    fun getTapsInTimeWindow() = runTest {
        val tapData1 = TapDataParsed(
            id = 0,
            taps = "[1,2,3]",
            start = 1000L,
            stop = 2000L,
            orientations = "[1,1,1]",
            appIds0 = "[10,10,10]",
            appIds1 = "[]",
            appIds2 = "[]",
            tapsSession = 3L,
            lengthSession = 1000L,
            timeZone = "UTC",
            inCharge = "0",
            sync = 0
        )

        val tapData2 = TapDataParsed(
            id = 0,
            taps = "[4,5,6]",
            start = 3000L,
            stop = 4000L,
            orientations = "[1,1,1]",
            appIds0 = "[10,10,10]",
            appIds1 = "[]",
            appIds2 = "[]",
            tapsSession = 3L,
            lengthSession = 1000L,
            timeZone = "UTC",
            inCharge = "0",
            sync = 0
        )

        mvpDao.insertOrUpdateTapDataParsed(tapData1)
        mvpDao.insertOrUpdateTapDataParsed(tapData2)

        // Query overlapping with first session
        val result1 = mvpDao.getTapsInTimeWindow(500L, 1500L)
        assertEquals(1, result1.size)
        assertEquals(1000L, result1[0].start)

        // Query overlapping with second session
        val result2 = mvpDao.getTapsInTimeWindow(3500L, 4500L)
        assertEquals(1, result2.size)
        assertEquals(3000L, result2[0].start)

        // Query fully encompassing both sessions
        val result3 = mvpDao.getTapsInTimeWindow(0L, 5000L)
        assertEquals(2, result3.size)

        // Query between sessions (no overlap)
        val result4 = mvpDao.getTapsInTimeWindow(2100L, 2900L)
        assertEquals(0, result4.size)
    }
}

/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.local.MVPRoomDatabase
import com.quantactions.sdk.data.repository.CognitiveTestRepository
import com.quantactions.sdk.data.repository.DeviceRepository
import com.quantactions.sdk.data.repository.IdentityRepository
import com.quantactions.sdk.data.repository.JournalRepository
import com.quantactions.sdk.data.repository.MetricRepository
import com.quantactions.sdk.data.repository.StudyRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QATapsHistoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: MVPRoomDatabase
    private lateinit var mvpDao: MVPDao
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var qaPrivate: QAPrivate

    private val preferences: ManagePref2 = mockk(relaxed = true)
    private val identityRepository: IdentityRepository = mockk(relaxed = true)
    private val networkManager: NetworkManager = mockk(relaxed = true)
    private val studyRepository: StudyRepository = mockk(relaxed = true)
    private val journalRepository: JournalRepository = mockk(relaxed = true)
    private val cognitiveRepo: CognitiveTestRepository = mockk(relaxed = true)
    private val metricRepo: MetricRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(
            context,
            MVPRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        mvpDao = database.mvpDao()

        deviceRepository = DeviceRepository(
            context,
            preferences,
            identityRepository,
            networkManager,
            mvpDao
        )

        // Reflection to instantiate QAPrivate with in-memory DAO
        val constructor = QAPrivate::class.java.getDeclaredConstructor(
            IdentityRepository::class.java,
            StudyRepository::class.java,
            JournalRepository::class.java,
            DeviceRepository::class.java,
            CognitiveTestRepository::class.java,
            MetricRepository::class.java,
            NetworkManager::class.java,
            ManagePref2::class.java,
            String::class.java
        )
        constructor.isAccessible = true
        qaPrivate = constructor.newInstance(
            identityRepository,
            studyRepository,
            journalRepository,
            deviceRepository,
            cognitiveRepo,
            metricRepo,
            networkManager,
            preferences,
            null
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testGetLastTapsDayRolling24Hours() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val nowCal = Calendar.getInstance()
        val nowDate = sdf.format(nowCal.time)
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)

        val oneHourAgoCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -1) }
        val oneHourAgoDate = sdf.format(oneHourAgoCal.time)
        val oneHourAgoHour = oneHourAgoCal.get(Calendar.HOUR_OF_DAY)

        val twentyThreeHoursAgoCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -23) }
        val twentyThreeDate = sdf.format(twentyThreeHoursAgoCal.time)
        val twentyThreeHour = twentyThreeHoursAgoCal.get(Calendar.HOUR_OF_DAY)

        val twentyFourHoursAgoCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -24) }
        val twentyFourDate = sdf.format(twentyFourHoursAgoCal.time)
        val twentyFourHour = twentyFourHoursAgoCal.get(Calendar.HOUR_OF_DAY)

        // Insert taps for now (50 taps), 1h ago (30 taps), 23h ago (20 taps), and 24h ago (100 taps outside window)
        mvpDao.insertOrUpdateHourlyTapsEntity(
            listOf(
                HourlyTapsEntity(0, nowDate, nowHour, 50, 2.5f),
                HourlyTapsEntity(0, oneHourAgoDate, oneHourAgoHour, 30, 1.8f),
                HourlyTapsEntity(0, twentyThreeDate, twentyThreeHour, 20, 1.2f),
                HourlyTapsEntity(0, twentyFourDate, twentyFourHour, 100, 3.0f)
            )
        )

        val result = qaPrivate.getLastTaps(QA.Flag.DAY)

        // Must return exactly 24 elements
        assertEquals(24, result.taps.size)
        assertEquals(24, result.speed.size)

        // Index 0 is the current hour (Now)
        assertEquals(50, result.taps[0])
        assertEquals(2.5f, result.speed[0])

        // Index 1 is 1 hour ago
        assertEquals(30, result.taps[1])
        assertEquals(1.8f, result.speed[1])

        // Index 23 is 23 hours ago
        assertEquals(20, result.taps[23])
        assertEquals(1.2f, result.speed[23])

        // 24 hours ago (100 taps) must be excluded from the 24-hour total
        assertEquals(100, result.totalTaps) // 50 + 30 + 20
    }

    @Test
    fun testGetLastTapsDayDoesNotCollideSameHourOnDifferentDates() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val nowCal = Calendar.getInstance()
        val nowDate = sdf.format(nowCal.time)
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)

        // Exactly 24 hours ago (yesterday at same hour)
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val yesterdayDate = sdf.format(yesterdayCal.time)
        val yesterdayHour = yesterdayCal.get(Calendar.HOUR_OF_DAY)

        assertEquals(nowHour, yesterdayHour)

        // Insert 75 taps for today's current hour, and 200 taps for yesterday's same hour
        mvpDao.insertOrUpdateHourlyTapsEntity(
            listOf(
                HourlyTapsEntity(0, yesterdayDate, yesterdayHour, 200, 4.0f),
                HourlyTapsEntity(0, nowDate, nowHour, 75, 2.0f)
            )
        )

        val result = qaPrivate.getLastTaps(QA.Flag.DAY)

        assertEquals(24, result.taps.size)
        // Index 0 (today at current hour) must have 75 taps, NOT 200 taps from yesterday!
        assertEquals(75, result.taps[0])
        assertEquals(2.0f, result.speed[0])
        assertEquals(75, result.totalTaps)
    }

    @Test
    fun testGetLastTapsWeekAndMonthBuckets() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val todayCal = Calendar.getInstance()
        val todayDate = sdf.format(todayCal.time)

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDate = sdf.format(yesterdayCal.time)

        val sixDaysAgoCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val sixDaysAgoDate = sdf.format(sixDaysAgoCal.time)

        val sevenDaysAgoCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val sevenDaysAgoDate = sdf.format(sevenDaysAgoCal.time)

        mvpDao.insertOrUpdateHourlyTapsEntity(
            listOf(
                HourlyTapsEntity(0, todayDate, 10, 100, 2.0f),
                HourlyTapsEntity(0, todayDate, 15, 50, 1.5f),
                HourlyTapsEntity(0, yesterdayDate, 12, 80, 2.2f),
                HourlyTapsEntity(0, sixDaysAgoDate, 9, 60, 1.8f),
                HourlyTapsEntity(0, sevenDaysAgoDate, 14, 500, 3.5f) // outside 7-day window
            )
        )

        val weekResult = qaPrivate.getLastTaps(QA.Flag.WEEK)
        assertEquals(7, weekResult.taps.size)
        assertEquals(150, weekResult.taps[0]) // Today: 100 + 50
        assertEquals(80, weekResult.taps[1])  // Yesterday: 80
        assertEquals(60, weekResult.taps[6])  // 6 days ago: 60
        assertEquals(290, weekResult.totalTaps) // 150 + 80 + 60 (7 days ago excluded)

        val monthResult = qaPrivate.getLastTaps(QA.Flag.MONTH)
        assertEquals(30, monthResult.taps.size)
        assertEquals(150, monthResult.taps[0])
        assertEquals(80, monthResult.taps[1])
        assertEquals(790, monthResult.totalTaps) // includes 7 days ago (500)
    }

    @Test
    fun testGetLastTapsSanitizesCorruptedInfinitySpeed() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nowCal = Calendar.getInstance()
        val nowDate = sdf.format(nowCal.time)
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)

        // Insert an entry with POSITIVE_INFINITY in DB (simulating legacy corrupted data)
        mvpDao.insertOrUpdateHourlyTapsEntity(
            listOf(
                HourlyTapsEntity(0, nowDate, nowHour, 42, Float.POSITIVE_INFINITY)
            )
        )

        val dayResult = qaPrivate.getLastTaps(QA.Flag.DAY)
        assertEquals(42, dayResult.taps[0])
        org.junit.Assert.assertTrue("Day speed must be finite", dayResult.speed[0].isFinite())
        assertEquals(0f, dayResult.speed[0])

        val weekResult = qaPrivate.getLastTaps(QA.Flag.WEEK)
        assertEquals(42, weekResult.taps[0])
        org.junit.Assert.assertTrue("Week speed must be finite", weekResult.speed[0].isFinite())
        assertEquals(0f, weekResult.speed[0])
    }
}

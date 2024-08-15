/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */

class SDKUnitFunctionalityTest {

    @Test
    fun testWeeklyAverages() = runTest {

        val timestamps = listOf(
        "2022-09-04T04:00:19.403+02:00[Europe/Zurich]",
        "2022-09-05T23:49:06.181+02:00[Europe/Zurich]",
        "2022-09-06T04:32:57.924+02:00[Europe/Zurich]",
        "2022-09-07T23:39:49.956+02:00[Europe/Zurich]",
        "2022-09-08T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-09T23:18:43.557+02:00[Europe/Zurich]",
        "2022-09-10T00:38:22.838+02:00[Europe/Zurich]",
        "2022-09-11T04:00:19.403+02:00[Europe/Zurich]",
        "2022-09-12T23:49:06.181+02:00[Europe/Zurich]",
        "2022-09-13T23:49:06.181+02:00[Europe/Zurich]",
        "2022-09-14T04:32:57.924+02:00[Europe/Zurich]",
        "2022-09-15T23:08:30.384+02:00[Europe/Zurich]",
        "2022-09-16T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-17T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-18T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-19T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-20T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-21T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-22T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-23T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-24T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-25T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-26T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-27T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-28T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-29T23:57:08.012+02:00[Europe/Zurich]",
        "2022-09-30T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-01T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-02T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-03T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-04T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-05T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-06T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-07T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-08T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-09T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-10T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-11T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-12T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-13T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-14T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-15T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-16T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-17T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-18T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-19T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-20T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-21T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-22T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-23T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-24T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-25T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-26T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-27T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-28T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-29T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-30T23:57:08.012+02:00[Europe/Zurich]",
        "2022-10-31T23:57:08.012+02:00[Europe/Zurich]",
        ).map { ZonedDateTime.parse(it) }

        val values = timestamps.map{ Random.nextDouble() * 100 }
        val ciH = timestamps.map{ Random.nextDouble() * 100 }
        val chL = timestamps.map{ Random.nextDouble() * 100 }
        val conf = timestamps.map{ Random.nextDouble() }

        val timeSeries = TimeSeries.DoubleTimeSeries(values, timestamps, chL, ciH, conf)
        val weeklyAvg = timeSeries.fillMissingDays(100).extractWeeklyAverages()
        weeklyAvg.values.zip(weeklyAvg.timestamps).forEach { (v, t) ->
            println("$v : $t")
        }



    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testEmptyTimeSeries() = runTest {
        val emptyTimeSeries = TimeSeries.DoubleTimeSeries()
        emptyTimeSeries.fillMissingDays(8, inplace = true)
        assert(emptyTimeSeries.size == 8) { "Time series should be 8 timestamps long instead it is ${emptyTimeSeries.size}" }
        assert(ChronoUnit.DAYS.between(emptyTimeSeries.timestamps.first(),
            emptyTimeSeries.timestamps.last()) == 7L)
        assert(emptyTimeSeries.values.all { it.isNaN() })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTimeSeriesOneValueThreeDaysAgo() = runTest {

        val today = ZonedDateTime.now()
        val threeDaysAgo = today.minusDays(3)

        val oneTimeSeries = TimeSeries.DoubleTimeSeries(
            listOf(10.0), listOf(threeDaysAgo), listOf(0.0), listOf(0.0), listOf(0.0),
        )

        oneTimeSeries.fillMissingDays(8, inplace = true)

        assertEquals(11, oneTimeSeries.size)
        assertEquals(10L, ChronoUnit.DAYS.between(oneTimeSeries.timestamps.first(),
            oneTimeSeries.timestamps.last()))
        assertEquals(0L, ChronoUnit.DAYS.between(oneTimeSeries.timestamps.last(), today))
        assertEquals(10.0, oneTimeSeries.values.filter { !it.isNaN() }.sum())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTimeSeriesThreeValuesThreeDaysAgo() = runTest {

        val today = ZonedDateTime.now()
        val threeDaysAgo = today.minusDays(3)

        val oneTimeSeries = TimeSeries.DoubleTimeSeries(
            listOf(10.0, 20.0, 30.0),
            listOf(threeDaysAgo.minusDays(2), threeDaysAgo.minusDays(1), threeDaysAgo),
            listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0),
        )

        oneTimeSeries.fillMissingDays(8, inplace = true)

        assertEquals(13, oneTimeSeries.size)
        assertEquals(12L, ChronoUnit.DAYS.between(oneTimeSeries.timestamps.first(),
            oneTimeSeries.timestamps.last()))
        assertEquals(0L, ChronoUnit.DAYS.between(oneTimeSeries.timestamps.last(), today))
        assertEquals(60.0, oneTimeSeries.values.filter { !it.isNaN() }.sum())
    }

    @Test
    fun testFillWeird() {
        val data = listOf(
            10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0,
            80.0, 90.0, 80.0, 70.0, 60.0, 50.0, 40.0
        )
//    val data = List(nValues) { Random.nextDouble() * 100 }.map{ if (Random.nextDouble() > 0.9) Double.NaN else it }
        val timestamps = listOf(
            "2023-01-18T04:00:19.403+02:00[Europe/Zurich]",
            "2023-01-19T23:49:06.181+02:00[Europe/Zurich]",
            "2023-01-20T04:32:57.924+02:00[Europe/Zurich]",
            "2023-01-21T23:39:49.956+02:00[Europe/Zurich]",
            "2023-01-22T23:57:08.012+02:00[Europe/Zurich]",
            "2023-01-24T23:18:43.557+02:00[Europe/Zurich]",
            "2023-01-26T00:38:22.838+02:00[Europe/Zurich]",
            "2023-01-27T04:00:19.403+02:00[Europe/Zurich]",
            "2023-01-28T23:49:06.181+02:00[Europe/Zurich]",
            "2023-01-29T23:49:06.181+02:00[Europe/Zurich]",
            "2023-01-30T04:32:57.924+02:00[Europe/Zurich]",
            "2023-01-31T23:08:30.384+02:00[Europe/Zurich]",
            "2023-02-01T23:57:08.012+02:00[Europe/Zurich]",
            "2023-02-02T23:57:08.012+02:00[Europe/Zurich]",
        ).map { ZonedDateTime.parse(it) }

        val timeSeries = TimeSeries.DoubleTimeSeries(
            data,
            timestamps,
            data,
            data,
            data
        )

        println(timeSeries.timestamps)
        timeSeries.fillMissingDays(14)
        println(timeSeries.timestamps)
    }
}

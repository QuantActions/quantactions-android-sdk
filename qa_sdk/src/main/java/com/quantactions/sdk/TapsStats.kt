/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk

import com.quantactions.sdk.data.model.DeviceStats
import com.quantactions.sdk.data.repository.MVPDao
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 *
 * Created by enea on 3/7/18.
 */

internal class TapsStats(mvpDao: MVPDao) {

    private var hourlyTaps: List<Int>
    private var hourlyTapsSpeed: List<Float>
    private var hourlyTapsAverage: Float
    private var hourlyTapsSpeedAverage : Float
    private var tapsCount: Int
    var date: String

    init {

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = Instant.now().toEpochMilli()

            // reset to previous midnight
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val endDate: String = sdf.format(calendar.timeInMillis)
            val speedList = FloatArray(24)
            val aggregate = IntArray(24)
            calendar.add(Calendar.DAY_OF_YEAR, -1)

            val rollBackDate: String = sdf.format(calendar.timeInMillis)

            val tapsToAggregate = mvpDao.getTapsForStats(rollBackDate, endDate)


            for (i in tapsToAggregate.indices) {
                aggregate[tapsToAggregate[i].hour] = tapsToAggregate[i].taps
                speedList[tapsToAggregate[i].hour] = tapsToAggregate[i].speed
                if (tapsToAggregate[i].hour == 23) break
            }

            var totalTaps = 0
            var totalSpeed = 0
            for (anAggregate in aggregate) totalTaps += anAggregate
            for (aSpeed in speedList) totalSpeed += aSpeed.toInt()

            hourlyTaps = aggregate.toList()
            hourlyTapsSpeed = speedList.toList()
            tapsCount = totalTaps
            hourlyTapsAverage = totalTaps / 24.0f
            hourlyTapsSpeedAverage = totalSpeed / 24.0f
            date = calendar.time.toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)

    }

    fun toDeviceStats(): DeviceStats {
        return DeviceStats(
            date,
            hourlyTaps,
            hourlyTapsAverage,
            hourlyTapsSpeed,
            hourlyTapsSpeedAverage,
            BuildConfig.VERSION_NAME,
            tapsCount,
//            UUID.randomUUID().toString()
        )

    }

}

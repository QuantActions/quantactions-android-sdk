/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.adapters

import com.quantactions.sdk.data.api.responses.StatisticResponse
import com.quantactions.sdk.data.entity.SleepSummaryEntity
import com.quantactions.sdk.data.entity.StatisticEntity
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SleepSummaryAdapter {

    @FromJson
    fun fromJson(stat: List<StatisticResponse>): List<SleepSummaryEntity> {
        return stat.flatMap { prepareOneMonthOfSleepMetric(it) }
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") value: List<StatisticEntity>): StatisticResponse {
        throw UnsupportedOperationException()
    }
}

fun prepareOneMonthOfSleepMetric(stat: StatisticResponse): List<SleepSummaryEntity> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    val ret = stat.metrics.data.map {
        val ld = LocalDateTime.parse(it["date"]!! as String, formatter)
        val localZoneId = it["time-zone"]!! as String
        val oldZdt = ZonedDateTime.of(ld, ZoneId.of("UTC"))
        val zdt = ZonedDateTime.of(ld, ZoneId.of(localZoneId))
        val sleepStart = it["sleep-utc"]!! as Double
        val sleepEnd = it["wake-utc"]!! as Double
        val intStart = it["int-start"]!! as List<Double>
        val intEnd = it["int-stop"]!! as List<Double>
        val intTaps = it["int-ntaps"]!! as List<Double>
        SleepSummaryEntity(
            stat.code + oldZdt.toEpochSecond(),
            zdt.toEpochSecond(),
            sleepStart.toLong(),
            sleepEnd.toLong(),
            intStart.map{i->i.toLong()},
            intEnd.map{i->i.toLong()},
            intTaps.map{i -> i.toInt()},
            localZoneId
        )
    }
    return ret
}
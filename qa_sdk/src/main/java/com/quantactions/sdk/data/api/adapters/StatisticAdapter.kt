/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.adapters

import androidx.annotation.Keep
import com.quantactions.sdk.data.api.responses.StatisticResponse
import com.quantactions.sdk.data.entity.StatisticEntity
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import timber.log.Timber
import java.lang.Double.NaN
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


//@Retention(AnnotationRetention.RUNTIME)
//@JsonQualifier
//annotation class WrappedStatisticList

//@JsonClass(generateAdapter = true)
//data class StatisticList(val items: List<Statistic>)

class StatisticAdapter {
    @FromJson
    fun fromJson(stat: List<StatisticResponse>): List<StatisticEntity> {
        return stat.flatMap { prepareOneMonthOfMetric(it) }
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") value: List<StatisticEntity>): StatisticResponse {
        throw UnsupportedOperationException()
    }
}

fun prepareOneMonthOfMetric(stat: StatisticResponse): List<StatisticEntity> {
    val columnNames = stat.metrics.schema.fields.map { it["name"] }
    val hasTz = columnNames.contains("time-zone")
    val hasWakeUTC = columnNames.contains("wake-utc")
    val statName = columnNames[1]

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    val ret = stat.metrics.data.filter { it[statName] != null }.map {
        val timeZone = if (hasTz && it["time-zone"] != null) it["time-zone"]!! as String else "UTC"
        val zoneId = ZoneId.of(timeZone)
        val ld = LocalDateTime.parse(it["index"]!! as String, formatter)
        val zdt = ZonedDateTime.of(ld, zoneId)

        val timestamp = if (hasWakeUTC && it["wake-utc"] != null) (it["wake-utc"]!! as Double / 1000).toLong() else zdt.toEpochSecond()

        val oldLd = LocalDateTime.parse(it["index"]!! as String, formatter)
        val oldZdt = ZonedDateTime.of(oldLd, ZoneId.of("UTC"))
        val oldTimestamp = oldZdt.toEpochSecond()

        StatisticEntity(
            stat.code + oldTimestamp,
            stat.code,
            timestamp,
            it[statName]!! as Double,
            timeZone,
            zdt.hour,
            if ("ci-l" in columnNames) it["ci-l"]!! as Double else NaN,
            if ("ci-h" in columnNames) it["ci-h"]!! as Double else NaN,
            if ("conf" in columnNames) it["conf"]!! as Double else NaN,
        )
    }
    return ret
}
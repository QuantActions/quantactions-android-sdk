/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.adapters

import com.quantactions.sdk.data.api.responses.StatisticResponse
import com.quantactions.sdk.data.entity.SleepSummaryEntity
import com.quantactions.sdk.data.entity.StatisticEntity
import com.quantactions.sdk.data.parseFlexDateTime
import com.quantactions.sdk.data.parseZoneId
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val ret = stat.metrics.data
        .filter { (it["sleep-utc"] != null || it["wake-utc"] != null) && (it["date"] != null || it["index"] != null) }
        .map {
            val dateStr = (it["date"] ?: it["index"]) as? String
            val ld = parseFlexDateTime(dateStr)
            val rawZoneId = it["time-zone"] as? String
            val zoneId = parseZoneId(rawZoneId)
            val normalizedZoneId = zoneId.id
            val oldZdt = ZonedDateTime.of(ld, ZoneId.of("UTC"))
            val zdt = ZonedDateTime.of(ld, zoneId)
            val sleepStart = (it["sleep-utc"] as? Number)?.toLong() ?: 0L
            val sleepEnd = (it["wake-utc"] as? Number)?.toLong() ?: 0L
            val intStart = (it["int-start"] as? List<*>)?.mapNotNull { item -> (item as? Number)?.toLong() } ?: emptyList()
            val intEnd = (it["int-stop"] as? List<*>)?.mapNotNull { item -> (item as? Number)?.toLong() } ?: emptyList()
            val intTaps = (it["int-ntaps"] as? List<*>)?.mapNotNull { item -> (item as? Number)?.toInt() } ?: emptyList()
            SleepSummaryEntity(
                stat.code + oldZdt.toEpochSecond(),
                zdt.toEpochSecond(),
                sleepStart,
                sleepEnd,
                intStart,
                intEnd,
                intTaps,
                normalizedZoneId
            )
        }
    return ret
}
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
import com.quantactions.sdk.data.parseFlexDateTime
import com.quantactions.sdk.data.parseZoneId
import java.lang.Double.NaN
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val statName = columnNames.getOrNull(1) ?: return emptyList()

    val ret = stat.metrics.data.filter { it[statName] != null }.map {
        val rawTimeZone = if (hasTz && it["time-zone"] != null) it["time-zone"] as String else "UTC"
        val zoneId = parseZoneId(rawTimeZone)
        val normalizedTz = zoneId.id
        val dateStr = (it["index"] ?: it["date"]) as? String
        val ld = parseFlexDateTime(dateStr)
        val zdt = ZonedDateTime.of(ld, zoneId)

        val timestamp = if (hasWakeUTC && it["wake-utc"] != null) {
            ((it["wake-utc"] as? Number)?.toDouble()?.div(1000))?.toLong() ?: zdt.toEpochSecond()
        } else {
            zdt.toEpochSecond()
        }

        val oldZdt = ZonedDateTime.of(ld, ZoneId.of("UTC"))
        val oldTimestamp = oldZdt.toEpochSecond()

        StatisticEntity(
            stat.code + oldTimestamp,
            stat.code,
            timestamp,
            (it[statName] as? Number)?.toDouble() ?: NaN,
            normalizedTz,
            zdt.hour,
            if ("ci-l" in columnNames && it["ci-l"] != null) (it["ci-l"] as? Number)?.toDouble() ?: NaN else NaN,
            if ("ci-h" in columnNames && it["ci-h"] != null) (it["ci-h"] as? Number)?.toDouble() ?: NaN else NaN,
            if ("conf" in columnNames && it["conf"] != null) (it["conf"] as? Number)?.toDouble() ?: NaN else NaN,
        )
    }
    return ret
}
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
import com.quantactions.sdk.data.entity.StatisticStringEntity
import com.quantactions.sdk.data.entity.TrendEntity
import com.quantactions.sdk.data.parseFlexDateTime
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.ZoneId
import java.time.ZonedDateTime

class TrendAdapter {
    @FromJson
    fun fromJson(stat: List<StatisticResponse>): List<TrendEntity> {
        return stat.flatMap { prepareOneMonthOfTrend(it) }
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") value: List<TrendEntity>): StatisticResponse {
        throw UnsupportedOperationException()
    }
}

fun prepareOneMonthOfTrend(stat: StatisticResponse): List<TrendEntity> {
    val z = ZoneId.of("UTC")

    val ret = stat.metrics.data
        .filter { it["index"] != null || it["date"] != null }
        .map {
            val dateStr = (it["index"] ?: it["date"]) as? String
            val ld = parseFlexDateTime(dateStr)
            val zdt = ZonedDateTime.of(ld, z)
            TrendEntity(
                stat.code + zdt.toEpochSecond(),
                stat.code,
                zdt.toEpochSecond(),
                (it["diff2W"] as? Number)?.toDouble() ?: Double.NaN,
                (it["stat2W"] as? Number)?.toDouble() ?: Double.NaN,
                (it["sign2W"] as? Number)?.toDouble() ?: Double.NaN,
                (it["diff6W"] as? Number)?.toDouble() ?: Double.NaN,
                (it["stat6W"] as? Number)?.toDouble() ?: Double.NaN,
                (it["sign6W"] as? Number)?.toDouble() ?: Double.NaN,
                (it["diff1Y"] as? Number)?.toDouble() ?: Double.NaN,
                (it["stat1Y"] as? Number)?.toDouble() ?: Double.NaN,
                (it["sign1Y"] as? Number)?.toDouble() ?: Double.NaN,
            )
        }
    return ret
}
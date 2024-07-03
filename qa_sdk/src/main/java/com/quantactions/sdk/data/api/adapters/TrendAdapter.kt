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
import com.quantactions.sdk.data.entity.StatisticStringEntity
import com.quantactions.sdk.data.entity.TrendEntity
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

//@Retention(AnnotationRetention.RUNTIME)
//@JsonQualifier
//annotation class WrappedStatisticList

//@JsonClass(generateAdapter = true)
//data class StatisticList(val items: List<Statistic>)

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
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val z = ZoneId.of("UTC")

    val ret = stat.metrics.data.map {
        val ld = LocalDateTime.parse(it["index"]!! as String, formatter)
        // might be that we need to accommodate if the index it's actually a "date"

        val zdt = ZonedDateTime.of(ld, z)
        TrendEntity(
            stat.code + zdt.toEpochSecond(),
            stat.code,
            zdt.toEpochSecond(),
            if (it["diff2W"] == null) Double.NaN else it["diff2W"] as Double,
            if (it["stat2W"] == null) Double.NaN else it["stat2W"] as Double,
            if (it["sign2W"] == null) Double.NaN else it["sign2W"] as Double,
            if (it["diff6W"] == null) Double.NaN else it["diff6W"] as Double,
            if (it["stat6W"] == null) Double.NaN else it["stat6W"] as Double,
            if (it["sign6W"] == null) Double.NaN else it["sign6W"] as Double,
            if (it["diff1Y"] == null) Double.NaN else it["diff1Y"] as Double,
            if (it["stat1Y"] == null) Double.NaN else it["stat1Y"] as Double,
            if (it["sign1Y"] == null) Double.NaN else it["sign1Y"] as Double,
        )
    }
    return ret
}
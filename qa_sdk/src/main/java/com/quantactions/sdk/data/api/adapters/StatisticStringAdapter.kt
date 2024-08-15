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
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

//@Retention(AnnotationRetention.RUNTIME)
//@JsonQualifier
//annotation class WrappedStatisticList

//@JsonClass(generateAdapter = true)
//data class StatisticList(val items: List<Statistic>)

class StatisticStringAdapter {
    @FromJson
    fun fromJson(stat: List<StatisticResponse>): List<StatisticStringEntity> {
        return stat.flatMap { prepareOneMonthOfStringMetric(it) }
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") value: List<StatisticStringEntity>): StatisticResponse {
        throw UnsupportedOperationException()
    }
}

fun prepareOneMonthOfStringMetric(stat: StatisticResponse): List<StatisticStringEntity> {
    val nCols = stat.metrics.schema.fields.size - 1

    val statNames = stat.metrics.schema.fields.map {it["name"]}.subList(1, nCols + 1)

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val z = ZoneId.of("UTC")


    val ret = stat.metrics.data.map {
        val ld = LocalDateTime.parse(it["index"]!! as String, formatter)
        val zdt = ZonedDateTime.of(ld, z)
        StatisticStringEntity(stat.code + zdt.toEpochSecond(),
            stat.code,
            zdt.toEpochSecond(),
            statNames.map { statName -> (it[statName]!! as Double).toLong().toString() }.reduce{ acc, string -> "$acc;$string" },
            "UTC",
            zdt.hour
        ) }
    return ret
}
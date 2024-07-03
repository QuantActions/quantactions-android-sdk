/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Entity of the database holding the sleep summary data. The sleep summary contains for each night
 * the start and end of the sleep, and number and length of the interruptions and the number of taps
 * during those interruptions.
 * @suppress
 */
@Entity(tableName = "sleep_summary_table")
@JsonClass(generateAdapter = true)
data class SleepSummaryEntity(
    /** Unique, defined as the concatenation between UNIX timestamp and stat code */
    @PrimaryKey
    val id: String,

    /** stat code e.g. 001-002-003-004 */
    @ColumnInfo(name = "timestamp")
    override val timestamp: Long,

    /** UNIX timestamp in milliseconds of sleep start */
    @ColumnInfo(name = "sleep_start")
    val sleepStart: Long,

    /** UNIX timestamp in milliseconds of sleep end */
    @ColumnInfo(name = "sleep_end")
    val sleepEnd: Long,

    /** List of UNIX timestamps in milliseconds of sleep interruption start */
    @ColumnInfo(name = "int_start")
    val interruptionsStart: List<Long>,

    /** List of UNIX timestamps in milliseconds of sleep interruption end */
    @ColumnInfo(name = "int_stop")
    val interruptionsEnd: List<Long>,

    /** List of number of taps for each interruption */
    @ColumnInfo(name = "int_ntaps")
    val interruptionsNumberOfTaps: List<Int>,

    /** ID of the time zone e.g. Europe/Zurich */
    @ColumnInfo(name = "time_zone_id")
    val timeZoneId: String,

) : TimestampedEntity

/**
 * @hide
 */
@Keep
interface TimestampedEntity{
    val timestamp: Long
}
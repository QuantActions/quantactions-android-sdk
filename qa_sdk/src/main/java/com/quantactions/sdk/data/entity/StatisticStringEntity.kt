/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Entity of QA statistics obtained from the TapCloud API. In particular this is the entity for the
 * string stats (e.g. amount of time in hours and minutes 03:45).
 * The string stat is generally stored as a value corresponding to the amount in the last 24 hours.
 * Since the stats are calculated over 24 hours window the API provides scores with window strides of 1 hour,
 * in this way no matter the Time Zone a device can retrieve the stat corresponding to the correct time zone shift.
 * To understand which subsets of the stats a device has to retrieve, one can simply extract all of the stats
 * where the 'reset' value of the ROW is equal to the raw time shift in the current time zone.
 * @suppress
 */
@Entity(tableName = "stat_string_table")
@JsonClass(generateAdapter = true)
data class StatisticStringEntity(
    /** Unique, defined as the concatenation between UNIX timestamp and stat code */
    @PrimaryKey
    val id: String,

    /** stat code e.g. 001-002-003-004:TS:DD */
    @ColumnInfo(name = "stat")
    val stat: String,

    /** UNIX timestamp in milliseconds of the entry stat */
    @ColumnInfo(name = "timestamp")
    override val timestamp: Long,

    /** Value of the string stat e.g. 03:45 */
    @ColumnInfo(name = "value")
    val value: String,

    /** ID of the time zone e.g. Europe/Zurich */
    @ColumnInfo(name = "tz")
    val timeZone: String,

    /** Value corresponding to the raw offset for which the 24-hours window was selected to
     * calculate the stat. E.g. if reset is 2 this means that users in a time zone with raw offset
     * to GMT is +2 should use this value to identify the correct stat of the day
     * (corresponding to a reset in THEIR midnight).*/
    @ColumnInfo(name = "reset")
    val reset: Int
): TimestampedEntity
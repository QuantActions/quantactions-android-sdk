/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/***
 * Entity of the table trend containing all information about a trend.
 * @hide
 */
@Entity(tableName = "trend_table")
@JsonClass(generateAdapter = true)
data class TrendEntity(
    /** Unique, defined as the concatenation between UNIX timestamp and stat code */
    @PrimaryKey
    val id: String,

    /** trend code e.g. 001-002-003-004 */
    @ColumnInfo(name = "trend")
    val trend: String,

    /** UNIX timestamp in milliseconds of the entry */
    @ColumnInfo(name = "timestamp")
    override val timestamp: Long,

    /** Metric difference over 2 weeks */
    @ColumnInfo(name = "diff2W")
    val diff2W: Double?,

    /** Metric difference statistic over 2 weeks */
    @ColumnInfo(name = "stat2W")
    val stat2W: Double?,

    /** Metric difference significance over 2 weeks */
    @ColumnInfo(name = "sign2W")
    val sign2W: Double?,

    /** Metric difference over 6 weeks */
    @ColumnInfo(name = "diff6W")
    val diff6W: Double?,

    /** Metric difference statistic over 6 weeks */
    @ColumnInfo(name = "stat6W")
    val stat6W: Double?,

    /** Metric difference significance over 6 weeks */
    @ColumnInfo(name = "sign6W")
    val sign6W: Double?,

    /** Metric difference over 1 year */
    @ColumnInfo(name = "diff1Y")
    val diff1Y: Double?,

    /** Metric difference statistic over 1 year */
    @ColumnInfo(name = "stat1Y")
    val stat1Y: Double?,

    /** Metric difference significance over 1 year */
    @ColumnInfo(name = "sign1Y")
    val sign1Y: Double?,
) : TimestampedEntity

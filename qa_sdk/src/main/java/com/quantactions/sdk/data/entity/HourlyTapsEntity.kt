/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * When subscribing to a study with a participation ID or a study ID, the call will return an
 * object of this class which contains all information related to the study including necessary
 * permissions and privacy policy.
 * @suppress
 */
@Entity(tableName = "hourly_taps")
@JsonClass(generateAdapter = true)
@Keep
data class HourlyTapsEntity(
    /** Identification for row */
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    /** date */
    @ColumnInfo(name = "date_tap")
    val date: String,

    /** Hour */
    @ColumnInfo(name = "hour")
    val hour: Int,

    /** Number of taps in the hour */
    @ColumnInfo(name = "num_taps")
    val taps: Int,

    /** Speed of taps in the hour */
    @ColumnInfo(name = "speed")
    val speed: Float,
)
/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass


@Entity(tableName = "taps_table")
@JsonClass(generateAdapter = true)
data class TapDataParsed(

    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "taps")
    val taps: String,

    @ColumnInfo(name = "start")
    val start: Long,

    @ColumnInfo(name = "stop")
    val stop: Long,

    @ColumnInfo(name = "orientations")
    val orientations: String,

    @ColumnInfo(name = "appIds0")
    val appIds0: String,

    @ColumnInfo(name = "appIds1")
    val appIds1: String,

    @ColumnInfo(name = "appIds2")
    val appIds2: String,

    @ColumnInfo(name = "tapsSession")
    val tapsSession: Long,

    @ColumnInfo(name = "lengthSession")
    val lengthSession: Long,

    @ColumnInfo(name = "timeZone")
    val timeZone: String,

    @ColumnInfo(name = "inCharge")
    val inCharge: String,

    @Transient
    @ColumnInfo(name = "sync")
    val sync: Int = 0

)
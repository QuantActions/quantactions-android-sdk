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


@Entity(tableName = "health_table")
@JsonClass(generateAdapter = true)

data class DeviceHealthParsed(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "timestamps")
    val timestamps: String,

    @ColumnInfo(name = "charge")
    val charge: String,

    @Transient
    @ColumnInfo(name = "event")
    val event: String = "",

    @ColumnInfo(name = "start")
    val start: Long,

    @ColumnInfo(name = "stop")
    val stop: Long,

    @Transient
    @ColumnInfo(name = "sync")
    val sync: Int = 0

)
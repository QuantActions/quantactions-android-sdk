/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TapDataBody(
    val records: List<TapDataParsedToPush>,
)

@JsonClass(generateAdapter = true)
data class HealthDataBody(
    val records: List<DeviceHealthParsedToPush>,
)

@JsonClass(generateAdapter = true)
data class ActivityBody(
    val records: List<ActivityToPush>,
)

@JsonClass(generateAdapter = true)
data class TapDataParsedToPush(
    val id: String,
    val taps: List<Long>,
    val start: Long,
    val stop: Long,
    val orientations: List<Int>,
    val appIdsSet: List<List<Int>>,
    val timezone: String,
    val charging: Int,
)

@JsonClass(generateAdapter = true)
data class DeviceHealthParsedToPush(
    val timestamp: Long,
    val charge: Int,
    val id: String,
)

@JsonClass(generateAdapter = true)
data class ActivityToPush(
    val timestamp: Long,
    val activity: String,
    val transition: Int,
)
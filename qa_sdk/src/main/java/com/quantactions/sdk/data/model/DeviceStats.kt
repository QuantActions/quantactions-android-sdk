/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

/***
 * @hide
 *
 * @property date
 * @property hourlyTaps
 * @property hourlyTapsAverage
 * @property hourlyTapsSpeed
 * @property hourlyTapsSpeedAverage
 * @property sdkVersion
 * @property tapsCount
 */
@JsonClass(generateAdapter = true)
@Serializable
data class DeviceStats(
    var date: String,               // Formatted ISO 8601 date
    var hourlyTaps: List<Int>,         // csv of # taps per hour
    var hourlyTapsAverage: Float,  // average # taps per hour
    var hourlyTapsSpeed: List<Float>,    // csv of speed per hour
    var hourlyTapsSpeedAverage: Float,       // average # taps per hour
    var sdkVersion: String,
    var tapsCount: Int,             // # taps in the day

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
@Serializable
data class DeviceStatsResponse(
    var date: String,               // Formatted ISO 8601 date
    var hourlyTaps: List<Int>,         // csv of # taps per hour
    var hourlyTapsAverage: Float,  // average # taps per hour
    var hourlyTapsSpeed: List<Float>,    // csv of speed per hour
    var hourlyTapsSpeedAverage: Float,       // average # taps per hour
    var sdkVersion: String,
    var tapsCount: Int,             // # taps in the day
    var id: String
)
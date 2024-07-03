/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.responses

import com.squareup.moshi.JsonClass

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class HealthDataResponse(
    val message: HealthDataInfo
)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class HealthDataInfo(
    val id: String,
    val healthData: String,
    val type: String,
    val dataParsed: Int,
    val created: Long,
    val modified: Long,
    val tapDeviceId: String
)

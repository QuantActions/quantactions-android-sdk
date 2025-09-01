/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.pvt

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

/**
 * @hide
 */
@JsonClass(generateAdapter = true)
@Serializable
data class PVTResponse(
    val reactionTimes: List<Long>,
    val waitTimes: List<Long>,
    val falseStartCount: Int,
    val noResponseCount: Int
)
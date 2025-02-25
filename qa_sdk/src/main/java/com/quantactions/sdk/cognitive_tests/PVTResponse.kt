/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitive_tests

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PVTResponse(
    val reactionTimes: List<Long>,
    val waitTimes: List<Long>,
    val date: Long,
    val localTime: String,
    val falseStartCount: Int,
    val noResponseCount: Int
)
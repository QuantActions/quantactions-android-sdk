/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.dotmemory

import com.squareup.moshi.JsonClass
/**
 * @hide
 */
@JsonClass(generateAdapter = true)
data class DotMemoryTestResponse(
    val timeTaken: List<Long>,
    val recallErrorScore: List<Double>,
    val proportionOfDistractors: List<Double>,
)
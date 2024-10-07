/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.responses

import androidx.annotation.Keep
import com.quantactions.sdk.PopulationRange
import com.squareup.moshi.JsonClass

/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
data class HealthyRangesResponse (
    val code: String,
    val id: String,
    val ranges: PopulationRange
)







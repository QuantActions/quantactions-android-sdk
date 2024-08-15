/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
@Serializable
class BodyPlainTapData(
        val sessionId: String,
        val startUnixTime: Long,
        val endUnixTime: Long,
        val timeZone: String,
        val appContext: String,
        val orientation: String,
        val taps: List<Long>
)



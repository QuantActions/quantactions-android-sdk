/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
data class AppToPush(
    val `package`: String,
    val installationId: Int?,
)

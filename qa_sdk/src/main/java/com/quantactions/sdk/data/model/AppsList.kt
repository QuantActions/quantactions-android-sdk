/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
data class AppToPush(
    val `package`: String,
    val installationId: Int?,
)

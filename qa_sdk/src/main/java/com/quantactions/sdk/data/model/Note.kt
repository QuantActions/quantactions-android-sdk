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

/**
 * @hide
 *
 */
@JsonClass(generateAdapter = true)
@Serializable
data class Note(
    val content: String        // simple text
)

/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import com.squareup.moshi.JsonClass

/**
 * @hide
 *
 * @property response a JSON object with the response
 */
@JsonClass(generateAdapter = true)
data class QuestionnaireResponse(
    val created: String,
    val response: Map<String, Any?>?
)

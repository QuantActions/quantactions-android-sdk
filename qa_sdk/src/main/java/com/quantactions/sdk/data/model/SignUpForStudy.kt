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

/**
 * @hide
 *
 * @property tapDeviceId UUID of the device
 * @property participationId of the study to signup or withdraw from up for (or the study id)
 */
@JsonClass(generateAdapter = true)
data class SignUpForStudy(
    val tapDeviceId: String,
    val participationId: String,
)

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
import kotlinx.serialization.Serializable

/**
 * This data class holds information about total and social screen time.
 *
 * @property totalScreenTime total screen time in milliseconds.
 * @property socialScreenTime social screen time in milliseconds.
 */
@Keep
@Serializable
data class ScreenTimeAggregate (
    val totalScreenTime: Double = Double.NaN,
    val socialScreenTime: Double = Double.NaN,
)



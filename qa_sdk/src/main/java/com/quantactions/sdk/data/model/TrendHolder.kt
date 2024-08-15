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
import kotlinx.serialization.Serializable

/**
 * Thi object contains 9 values. For each time resolution (short: 2 Weeks, medium: 6 Weeks,
 * long: 1 Year) the trend has 3 values: (difference: value of the change, statistic: p-value,
 * significance: significance of the change)
 *
 * @property difference2Weeks the amount the metric has increased decrease (note: it is reported in the same unit as the metric)
 * @property statistic2Weeks p-value of the test done to check if the trend is significant
 * @property significance2Weeks the significance of the trend, +1 means metric has significantly increase, -1 means metric has significantly decreased, 0 means no significant change
 * @property difference6Weeks the amount the metric has increased decrease (note: it is reported in the same unit as the metric)
 * @property statistic6Weeks p-value of the test done to check if the trend is significant
 * @property significance6Weeks the significance of the trend, +1 means metric has significantly increase, -1 means metric has significantly decreased, 0 means no significant change
 * @property difference1Year the amount the metric has increased decrease (note: it is reported in the same unit as the metric)
 * @property statistic1Year p-value of the test done to check if the trend is significant
 * @property significance1Year the significance of the trend, +1 means metric has significantly increase, -1 means metric has significantly decreased, 0 means no significant change
 */
@Keep
@JsonClass(generateAdapter = true)
@Serializable
data class TrendHolder(
    val difference2Weeks: Double = Double.NaN,
    val statistic2Weeks: Double = Double.NaN,
    val significance2Weeks: Double = Double.NaN,
    val difference6Weeks: Double = Double.NaN,
    val statistic6Weeks: Double = Double.NaN,
    val significance6Weeks: Double = Double.NaN,
    val difference1Year: Double = Double.NaN,
    val statistic1Year: Double = Double.NaN,
    val significance1Year: Double = Double.NaN,
) {
    @Keep
    fun isEmpty(): Boolean {
        return difference2Weeks.isNaN() &&
                statistic2Weeks.isNaN() &&
                significance2Weeks.isNaN() &&
                difference6Weeks.isNaN() &&
                statistic6Weeks.isNaN() &&
                significance6Weeks.isNaN() &&
                difference1Year.isNaN() &&
                statistic1Year.isNaN() &&
                significance1Year.isNaN()
    }
}



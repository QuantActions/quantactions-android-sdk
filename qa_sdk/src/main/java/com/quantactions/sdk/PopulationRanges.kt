/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import java.time.LocalDate


/**
 * For each subpopulation we provide a [percentile25] and a [percentile75] value for a range of a metric. Where [percentile75]
 * is the 75% percentile while [percentile25] is the 25% percentile.
 * */
@Keep
@JsonClass(generateAdapter = true)
@Serializable
class Range(
    @Json(name="25th")
    val percentile25: Float,
    @Json(name="75th")
    val percentile75: Float
)

/**
 * Range for a particular [QA.Gender] group. The range is divided by age where:
 *  - [lowerThan30] age <= 30
 *  - [between30And50]  30 < age <= 50
 *  - [greaterThan50] 50 < age
 *  @suppress
 * */
@Keep
@JsonClass(generateAdapter = true)
@Serializable
data class AgeStratifiedRange(
    val all: Range = Range(0f, 0f),
    @Json(name="<30")
    val lowerThan30: Range = Range(0f, 0f),
    @Json(name="30-50")
    val between30And50: Range = Range(0f, 0f),
    @Json(name=">50")
    val greaterThan50: Range = Range(0f, 0f),
) {
    /** Get the lower end of the range (25% percentile) for the selected [yearOfBirth].*/
    @Keep
    fun get25thPercentile(yearOfBirth: Int?): Float {
        if (yearOfBirth == null) return all.percentile25
        return when (LocalDate.now().year - yearOfBirth) {
            in 1..29 -> lowerThan30.percentile25
            in 30..50 -> between30And50.percentile25
            in 51..Int.MAX_VALUE -> greaterThan50.percentile25
            else -> all.percentile25
        }
    }

    /** Get the higher end of the range (75% percentile) */
    @Keep
    fun get75thPercentile(yearOfBirth: Int?): Float {
        if (yearOfBirth == null) return all.percentile75
        return when (LocalDate.now().year - yearOfBirth) {
            in 1..30 -> lowerThan30.percentile75
            in 31..50 -> between30And50.percentile75
            in 51..Int.MAX_VALUE -> greaterThan50.percentile75
            else -> all.percentile75
        }
    }
}


/**
 * Class providing functionality to define a [Range] of values
 * @suppress
 * */
@Keep
@JsonClass(generateAdapter = true)
@Serializable
class PopulationRange(
    /** Across the whole population */
    val global: AgeStratifiedRange = AgeStratifiedRange(),
    /** Across the male population divided by age group */
    val male: AgeStratifiedRange = AgeStratifiedRange(),
    /** Across the female population divided by age group */
    val female: AgeStratifiedRange = AgeStratifiedRange(),
) {

    /** Get the lower end of the range (25% percentile) for a selected subgroup based on
     * [yearOfBirth] and [gender].
     * */
    @Keep
    fun get25thPercentile(yearOfBirth: Int? = 0, gender: QA.Gender = QA.Gender.UNKNOWN): Float {
        return when (gender) {
            QA.Gender.MALE -> male.get25thPercentile(yearOfBirth)
            QA.Gender.FEMALE -> female.get25thPercentile(yearOfBirth)
            else -> global.get25thPercentile(yearOfBirth)
        }
    }

    /** Get the higher end of the range (75% percentile) for a selected subgroup based on
     * [yearOfBirth] and [gender].
     * */
    @Keep
    fun get75thPercentile(yearOfBirth: Int? = 0, gender: QA.Gender = QA.Gender.UNKNOWN): Float {
            return when (gender) {
                QA.Gender.MALE -> male.get75thPercentile(yearOfBirth)
                QA.Gender.FEMALE -> female.get75thPercentile(yearOfBirth)
                else -> global.get75thPercentile(yearOfBirth)
            }
    }
}

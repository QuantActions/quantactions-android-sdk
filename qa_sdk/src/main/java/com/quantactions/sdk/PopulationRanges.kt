/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import androidx.annotation.Keep
import java.time.LocalDate


/**
 * For each subpopulation we provide a [low] and a [high] value for a range of a metric. Where [high]
 * is the 75% percentile while [low] is the 25% percentile.
 * */
@Keep
class Range(
    val low: Float,
    val high: Float
)

/**
 * Range for a particular [QA.Gender] group. The range is divided by age where:
 *  - [young] age <= 30
 *  - [mid]  30 < age <= 50
 *  - [old] 50 < age
 *  @suppress
 * */
@Keep
data class SexRange(
    val young: Range = Range(0f, 0f),
    val mid: Range = Range(0f, 0f),
    val old: Range = Range(0f, 0f),
) {
    /** Get the lower end of the range (25% percentile) for the selected [yearOfBirth].*/
    @Keep
    fun getLow(yearOfBirth: Int): Float {
        return when (LocalDate.now().year - yearOfBirth) {
            in 0..30 -> young.low
            in 31..50 -> mid.low
            else -> old.low
        }
    }

    /** Get the higher end of the range (75% percentile) */
    @Keep
    fun getHigh(yob: Int): Float {
        return when (LocalDate.now().year - yob) {
            in 0..30 -> young.high
            in 31..50 -> mid.high
            else -> old.high
        }
    }
}


/**
 * Class providing functionality to define a [Range] of values
 * @suppress
 * */
@Keep
class PopulationRange(
    /** Across the whole population */
    private val global: Range = Range(0f, 0f),
    /** Across the male population */
    private val globalMale: Range = Range(0f, 0f),
    /** Across the female population */
    private val globalFemale: Range = Range(0f, 0f),
    /** Across the male population divided by age group */
    private val male: SexRange = SexRange(),
    /** Across the female population divided by age group */
    private val female: SexRange = SexRange(),
    /** Across the non-male/non-female population divided by age group */
    private val other: SexRange = SexRange(),
) {

    /** Get the lower end of the range (25% percentile) for a selected subgroup based on
     * [yearOfBirth] and [gender].
     * */
    @Keep
    fun getLow(yearOfBirth: Int = 0, gender: QA.Gender = QA.Gender.UNKNOWN): Float {
        if (yearOfBirth == 0) {
            return when (gender) {
                QA.Gender.MALE -> globalMale.low
                QA.Gender.FEMALE -> globalFemale.low
                else -> global.low
            }
        }
        return when (gender) {
            QA.Gender.MALE -> male.getLow(yearOfBirth)
            QA.Gender.FEMALE -> female.getLow(yearOfBirth)
            else -> other.getLow(yearOfBirth)
        }
    }

    /** Get the higher end of the range (75% percentile) for a selected subgroup based on
     * [yearOfBirth] and [gender].
     * */
    @Keep
    fun getHigh(yearOfBirth: Int = 0, gender: QA.Gender = QA.Gender.UNKNOWN): Float {
        if (yearOfBirth == 0) {
            return when (gender) {
                QA.Gender.MALE -> globalMale.high
                QA.Gender.FEMALE -> globalFemale.high
                else -> global.high
            }
        }
        return when (gender) {
            QA.Gender.MALE -> male.getHigh(yearOfBirth)
            QA.Gender.FEMALE -> female.getHigh(yearOfBirth)
            else -> other.getHigh(yearOfBirth)
        }
    }
}

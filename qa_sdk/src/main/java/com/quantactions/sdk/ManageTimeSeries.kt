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
import java.time.ZonedDateTime

/**
 * @suppress
 */
@Keep
interface ManageTimeSeries<T> {

    var values: List<T>
    var timestamps: List<ZonedDateTime>
    var confidenceIntervalLow: List<T>
    var confidenceIntervalHigh: List<T>
    var confidence: List<Double>

    @Keep
    fun fillMissingDays(rewindDays: Int, inplace: Boolean = false): TimeSeries<T>

    /**
     * Returns a time series containing the last [n] elements.
     * @param n how many elements to take
     * */
    @Keep
    fun takeLast(n: Int): TimeSeries<T>

    /**
     * Returns a list containing all elements except last n elements.
     * @param n how many elements to drop
     * */
    @Keep
    fun dropLast(n: Int): TimeSeries<T>

    @Keep
    fun getRandomSample(n: Int): TimeSeries<T>

    @Keep
    fun extractDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries

    @Keep
    fun extractMonthlyAverages(): TimeSeries<T>

    @Keep
    fun extractWeeklyAverages(): TimeSeries<T>

    @Keep
    fun dropna(): TimeSeries<T>

    @Keep
    fun <P> map(transform: (T, ZonedDateTime) -> P): List<P> {
        return values.zip(timestamps).map { (v, t) -> transform (v, t) }
    }

    @Keep
    fun getPlaceholderTimeSeries(bias: Double, deviation: Double): TimeSeries<T>

}
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
import com.quantactions.sdk.data.model.ScreenTimeAggregate
import com.quantactions.sdk.data.model.SleepSummary
import com.quantactions.sdk.data.model.SleepSummary.Companion.ZonedDateTimePlaceholder
import com.quantactions.sdk.data.model.TrendHolder
import timber.log.Timber
import java.lang.Double.NaN
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/**
 * @suppress
 */
fun Long.localize(zoneId: ZoneId? = null): ZonedDateTime {
    val rawOffset = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds
    return if (rawOffset > 0) {
        Instant.ofEpochMilli(this * 1000)
            .atZone(zoneId ?: ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS)
    } else {
        Instant.ofEpochMilli(this * 1000)
            .atZone(zoneId ?: ZoneId.systemDefault()).plusDays(1).truncatedTo(ChronoUnit.DAYS)
    }
}


/**
 * @suppress
 * */
@Keep
fun ZonedDateTime.norm(): ZonedDateTime {
    return this.truncatedTo(ChronoUnit.DAYS)
}


/**
 * Utility class to hold a pair of lists, one with the score values and one with the timestamps
 * @property values values of the score
 * @property timestamps timestamps relative to the score, are always normalized to the CURRENT local
 * time zone of the device
 * */
@Keep
sealed class TimeSeries<T>(
    final override var values: List<T> = mutableListOf(),
    final override var timestamps: List<ZonedDateTime> = mutableListOf(),
    final override var confidenceIntervalLow: List<T> = mutableListOf(),
    final override var confidenceIntervalHigh: List<T> = mutableListOf(),
    final override var confidence: List<Double> = mutableListOf(),
) : ManageTimeSeries<T> {

    init {
        val sizes = listOf(
            values.size,
            timestamps.size,
            confidenceIntervalLow.size,
            confidenceIntervalHigh.size,
            confidence.size,
        )
        assert(sizes.all { it == values.size }) {
            "All provided sequences should be of equal length but they are\n" +
                    "Values                   = ${values.size}\n" +
                    "Timestamps               = ${timestamps.size}\n" +
                    "Confidence Interval Low  = ${confidenceIntervalLow.size}\n" +
                    "Confidence Interval High = ${confidenceIntervalHigh.size}\n" +
                    "Confidence               = ${confidence.size}\n"
        }

        // assure monotonicity
        timestamps = timestamps.sorted()
    }

    /**
     * Number of data points in the time series
     * */
    val size: Int
        get() = values.size

    @Keep
    class DoubleTimeSeries(
        values: List<Double> = mutableListOf(),
        timestamps: List<ZonedDateTime> = mutableListOf(),
        confidenceIntervalLow: List<Double> = mutableListOf(),
        confidenceIntervalHigh: List<Double> = mutableListOf(),
        confidence: List<Double> = mutableListOf(),
    ) : TimeSeries<Double>(
        values,
        timestamps,
        confidenceIntervalLow,
        confidenceIntervalHigh,
        confidence
    ) {
        /**
         * This is an `in-place` operation. Use this function to fill missing days (the function adds NaNs) both in the future and in the
         * past (up to the specified number of days in the past). Since the SDK does not return a value
         * for the score in a day where the confidence it too low, this function can be useful to fill
         * in blanks for UI/UX purposes.
         * @param rewindDays how many days to fill in in the past
         * @return the current [TimeSeries] as this is an `in-place` operation
         * */
        @Keep
        override fun fillMissingDays(rewindDays: Int, inplace: Boolean): DoubleTimeSeries {
            val newTimestamps = mutableListOf<ZonedDateTime>()
            val newValues = mutableListOf<Double>()
            val newConfidenceIntervalLow = mutableListOf<Double>()
            val newConfidenceIntervalHigh = mutableListOf<Double>()
            val newConfidence = mutableListOf<Double>()
            val currentDay = ZonedDateTime.now()
            var prevDate =
                if (timestamps.isNotEmpty()) timestamps[0].minusDays(rewindDays.toLong()) else currentDay.minusDays(
                    rewindDays.toLong()
                )

            if (timestamps.isEmpty()) {
                val nMissingDays = ChronoUnit.DAYS.between(prevDate.norm(), currentDay.norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(NaN)
                    newConfidenceIntervalLow.add(NaN)
                    newConfidenceIntervalHigh.add(NaN)
                    newConfidence.add(NaN)
                }
            }

            for (i in values.indices) {
                val nMissingDays =
                    ChronoUnit.DAYS.between(prevDate.norm(), timestamps[i].norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(NaN)
                    newConfidenceIntervalLow.add(NaN)
                    newConfidenceIntervalHigh.add(NaN)
                    newConfidence.add(NaN)
                }
                newTimestamps.add(timestamps[i])
                newValues.add(values[i])
                newConfidenceIntervalLow.add(confidenceIntervalLow[i])
                newConfidenceIntervalHigh.add(confidenceIntervalHigh[i])
                newConfidence.add(confidence[i])
                prevDate = timestamps[i]
            }

            // Adding missing days up to today
            val nMissingFutureDays = if (newTimestamps.isNotEmpty()) ChronoUnit.DAYS.between(
                newTimestamps.last().norm(),
                currentDay.norm()
            ) else 0
            val lastValue = newTimestamps.last()
            for (j in 0 until nMissingFutureDays) {
                newTimestamps.add(lastValue.norm().plusDays(j + 1L))
                newValues.add(NaN)
                newConfidenceIntervalLow.add(NaN)
                newConfidenceIntervalHigh.add(NaN)
                newConfidence.add(NaN)
            }

            return if (inplace) {
                this.values = newValues
                this.confidenceIntervalLow = newConfidenceIntervalLow
                this.confidenceIntervalHigh = newConfidenceIntervalHigh
                this.confidence = newConfidence
                this.timestamps = newTimestamps
                this
            } else {
                DoubleTimeSeries(
                    newValues,
                    newTimestamps,
                    newConfidenceIntervalLow,
                    newConfidenceIntervalHigh,
                    newConfidence
                )
            }
        }

        @Keep
        override fun takeLast(n: Int): DoubleTimeSeries {
            return DoubleTimeSeries(
                values.takeLast(n),
                timestamps.takeLast(n),
                confidenceIntervalLow.takeLast(n),
                confidenceIntervalHigh.takeLast(n),
                confidence.takeLast(n),
            )
        }

        @Keep
        override fun dropLast(n: Int): DoubleTimeSeries {
            return DoubleTimeSeries(
                values.dropLast(n), timestamps.dropLast(n),
                confidenceIntervalLow.dropLast(n),
                confidenceIntervalHigh.dropLast(n),
                confidence.dropLast(n)
            )
        }

        override fun getRandomSample(n: Int): DoubleTimeSeries {
            val values = List(28) { Random.nextDouble() * 100 }
            val emptyTimeSeries = DoubleTimeSeries()
            emptyTimeSeries.fillMissingDays(28, inplace = true)

            return DoubleTimeSeries(values,emptyTimeSeries.timestamps,values,values,values)

        }

        override fun extractDoubleTimeSeries(flag: Int): DoubleTimeSeries {
            return this
        }

        override fun extractMonthlyAverages(): DoubleTimeSeries {


            val averagedValues = timestamps.zip(values).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            val averagedConfidenceIntervalLow = timestamps.zip(confidenceIntervalLow).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            val averagedConfidenceIntervalHigh = timestamps.zip(confidenceIntervalHigh).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            val averagedConfidence = timestamps.zip(confidence).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            return DoubleTimeSeries(
                averagedValues.values.toList(),
                averagedValues.keys.toList(),
                averagedConfidenceIntervalLow.values.toList(),
                averagedConfidenceIntervalHigh.values.toList(),
                averagedConfidence.values.toList()
            )

        }

        override fun extractWeeklyAverages(): DoubleTimeSeries {

            val averagedValues = timestamps.zip(values).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            val averagedConfidenceIntervalLow = timestamps.zip(confidenceIntervalLow).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            val averagedConfidenceIntervalHigh = timestamps.zip(confidenceIntervalHigh).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            val averagedConfidence = timestamps.zip(confidence).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            return DoubleTimeSeries(
                averagedValues.values.toList(),
                averagedValues.keys.toList(),
                averagedConfidenceIntervalLow.values.toList(),
                averagedConfidenceIntervalHigh.values.toList(),
                averagedConfidence.values.toList()
            )
        }

        override fun dropna(): TimeSeries<Double> {
            return this.filterByValues { !it.isNaN() }
        }

        override fun getPlaceholderTimeSeries(bias: Double, deviation: Double): DoubleTimeSeries {
            val plc = DoubleTimeSeries().fillMissingDays(366)
            plc.values = placeholderScore.map { it * deviation + bias }
            plc.confidenceIntervalHigh = plc.values.map { it + 5 }
            plc.confidenceIntervalLow = plc.values.map { it - 5 }
            return plc
        }

        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * timestamps. Provide a lambda that receives a [ZonedDateTime] and return a boolean.
         * */
        @Suppress("unused")
        @Keep
        fun filterByTimestamps(filter: (ZonedDateTime) -> Boolean): DoubleTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            return DoubleTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }

        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * values. Provide a lambda that receives a [Double] and returns a boolean.
         * */
        @Suppress("unused")
        @Keep
        fun filterByValues(filter: (Double) -> Boolean): DoubleTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(values[i])
            }
            return DoubleTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }
    }

    @Keep
    class SleepSummaryTimeTimeSeries(
        values: List<SleepSummary> = mutableListOf(),
        timestamps: List<ZonedDateTime> = mutableListOf(),
        confidenceIntervalLow: List<SleepSummary> = mutableListOf(),
        confidenceIntervalHigh: List<SleepSummary> = mutableListOf(),
        confidence: List<Double> = mutableListOf(),
    ) : TimeSeries<SleepSummary>(
        values,
        timestamps,
        confidenceIntervalLow,
        confidenceIntervalHigh,
        confidence
    ) {
        /**
         * This is an `in-place` operation. Use this function to fill missing days (the function adds NaNs) both in the future and in the
         * past (up to the specified number of days in the past). Since the SDK does not return a value
         * for the score in a day where the confidence it too low, this function can be useful to fill
         * in blanks for UI/UX purposes.
         * @param rewindDays how many days to fill in in the past
         * @return the current [TimeSeries] as this is an `in-place` operation
         * */
        @Keep
        override fun fillMissingDays(
            rewindDays: Int,
            inplace: Boolean
        ): SleepSummaryTimeTimeSeries {
            val newTimestamps = mutableListOf<ZonedDateTime>()
            val newValues = mutableListOf<SleepSummary>()
            val newConfidenceIntervalLow = mutableListOf<SleepSummary>()
            val newConfidenceIntervalHigh = mutableListOf<SleepSummary>()
            val newConfidence = mutableListOf<Double>()
            val currentDay = ZonedDateTime.now()
            var prevDate =
                if (timestamps.isNotEmpty()) timestamps[0].minusDays(rewindDays.toLong()) else currentDay.minusDays(
                    rewindDays.toLong()
                )

            if (timestamps.isEmpty()) {
                val nMissingDays = ChronoUnit.DAYS.between(prevDate.norm(), currentDay.norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(SleepSummary())
                    newConfidenceIntervalLow.add(SleepSummary())
                    newConfidenceIntervalHigh.add(SleepSummary())
                    newConfidence.add(NaN)
                }
            }

            for (i in values.indices) {
                val nMissingDays =
                    ChronoUnit.DAYS.between(prevDate.norm(), timestamps[i].norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(SleepSummary())
                    newConfidenceIntervalLow.add(SleepSummary())
                    newConfidenceIntervalHigh.add(SleepSummary())
                    newConfidence.add(NaN)
                }
                newTimestamps.add(timestamps[i])
                newValues.add(values[i])
                newConfidenceIntervalLow.add(confidenceIntervalLow[i])
                newConfidenceIntervalHigh.add(confidenceIntervalHigh[i])
                newConfidence.add(confidence[i])
                prevDate = timestamps[i]
            }

            // Adding missing days up to today
            val nMissingFutureDays = if (newTimestamps.isNotEmpty()) ChronoUnit.DAYS.between(
                newTimestamps.last().norm(),
                currentDay.norm()
            ) else 0
            val lastValue = newTimestamps.last()
            for (j in 0 until nMissingFutureDays) {
                newTimestamps.add(lastValue.norm().plusDays(j + 1L))
                newValues.add(SleepSummary())
                newConfidenceIntervalLow.add(SleepSummary())
                newConfidenceIntervalHigh.add(SleepSummary())
                newConfidence.add(NaN)
            }

            return if (inplace) {
                this.values = newValues
                this.confidenceIntervalLow = newConfidenceIntervalLow
                this.confidenceIntervalHigh = newConfidenceIntervalHigh
                this.confidence = newConfidence
                this.timestamps = newTimestamps
                this
            } else {
                SleepSummaryTimeTimeSeries(
                    newValues,
                    newTimestamps,
                    newConfidenceIntervalLow,
                    newConfidenceIntervalHigh,
                    newConfidence
                )
            }
        }

        @Keep
        override fun takeLast(n: Int): SleepSummaryTimeTimeSeries {
            return SleepSummaryTimeTimeSeries(
                values.takeLast(n),
                timestamps.takeLast(n),
                confidenceIntervalLow.takeLast(n),
                confidenceIntervalHigh.takeLast(n),
                confidence.takeLast(n),
            )
        }

        @Keep
        override fun dropLast(n: Int): SleepSummaryTimeTimeSeries {
            return SleepSummaryTimeTimeSeries(
                values.dropLast(n), timestamps.dropLast(n),
                confidenceIntervalLow.dropLast(n),
                confidenceIntervalHigh.dropLast(n),
                confidence.dropLast(n)
            )
        }

        override fun getRandomSample(n: Int): TimeSeries<SleepSummary> {
            TODO("Not yet implemented")
        }

        override fun extractDoubleTimeSeries(flag: Int): DoubleTimeSeries {

            return when(flag){
                0 -> DoubleTimeSeries(
                    this.values.map { ChronoUnit.MILLIS.between(it.sleepStart, it.sleepEnd).toDouble() },
                    this.timestamps,
                    this.confidenceIntervalLow.map { ChronoUnit.MILLIS.between(it.sleepStart, it.sleepEnd).toDouble() },
                    this.confidenceIntervalHigh.map { ChronoUnit.MILLIS.between(it.sleepStart, it.sleepEnd).toDouble() },
                    this.confidence
                )
                else -> DoubleTimeSeries(
                    this.values.map { it.interruptionsEnd.size.toDouble() },
                    this.timestamps,
                    this.confidenceIntervalLow.map { ChronoUnit.MILLIS.between(it.sleepStart, it.sleepEnd).toDouble() },
                    this.confidenceIntervalHigh.map { ChronoUnit.MILLIS.between(it.sleepStart, it.sleepEnd).toDouble() },
                    this.confidence
                )
            }


        }

        override fun extractMonthlyAverages(): SleepSummaryTimeTimeSeries {

            val averagedValues = timestamps.zip(values).groupBy({
                it.first.withDayOfMonth(1).withHour(0).toLocalDateTime()
            }, { Pair(it.first, it.second) }).mapValues { (k, v) ->

                val vFiltered = v.filter { it.second.sleepStart != ZonedDateTimePlaceholder }
                val vFilteredAverage = vFiltered.map { it.second.interruptionsStart.size }.average()
                val averageNumberOfInterruptions = if (vFilteredAverage.isNaN()) 0 else kotlin.math.ceil(
                    vFilteredAverage
                ).toInt()

                SleepSummary(
                    periodicMean(
                        v.map { it.second.sleepStart }.dropna(),
                        vFiltered.map { it.first },
                        k
                    ),
                    periodicMean(
                        v.map { it.second.sleepEnd }.dropna(),
                        v.filter { it.second.sleepStart != ZonedDateTimePlaceholder }.map { it.first },
                        k
                    ),
                    List(averageNumberOfInterruptions) { ZonedDateTimePlaceholder },
                    List(averageNumberOfInterruptions) { ZonedDateTimePlaceholder },
                    List(averageNumberOfInterruptions) { 0 },
                )
            }

            return SleepSummaryTimeTimeSeries(
                averagedValues.values.toList(),
                averagedValues.keys.toList().map { ZonedDateTime.of(it, ZoneId.systemDefault()) },
                averagedValues.values.toList(),
                averagedValues.values.toList(),
                List(averagedValues.size) { Double.NaN }
            )
        }

        override fun extractWeeklyAverages(): SleepSummaryTimeTimeSeries {

            val averagedValues = timestamps.zip(values).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1)))
                    .truncatedTo(ChronoUnit.DAYS).toLocalDateTime()
            }, { Pair(it.first, it.second) }).mapValues { (k, v) ->

                val vFiltered = v.filter { it.second.sleepStart != ZonedDateTimePlaceholder }
                val vFilteredAverage = vFiltered.map { it.second.interruptionsStart.size }.average()
                val averageNumberOfInterruptions = if (vFilteredAverage.isNaN()) 0 else kotlin.math.ceil(
                    vFilteredAverage
                ).toInt()

                SleepSummary(
                    periodicMean(
                        v.map { it.second.sleepStart }.dropna(),
                        vFiltered.map { it.first },
                        k
                    ),
                    periodicMean(
                        v.map { it.second.sleepEnd }.dropna(),
                        v.filter { it.second.sleepStart != ZonedDateTimePlaceholder }.map { it.first },
                        k
                    ),
                    List(averageNumberOfInterruptions) { ZonedDateTimePlaceholder },
                    List(averageNumberOfInterruptions) { ZonedDateTimePlaceholder },
                    List(averageNumberOfInterruptions) { 0 },
                )
            }

            return SleepSummaryTimeTimeSeries(
                averagedValues.values.toList(),
                averagedValues.keys.toList().map { ZonedDateTime.of(it, ZoneId.systemDefault()) },
                averagedValues.values.toList(),
                averagedValues.values.toList(),
                List(averagedValues.size) { Double.NaN }
            )
        }

        override fun dropna(): TimeSeries<SleepSummary> {
            return this.filterByValue { it.sleepStart != ZonedDateTimePlaceholder }
        }

        override fun getPlaceholderTimeSeries(bias: Double, deviation: Double): SleepSummaryTimeTimeSeries {

            val plc = SleepSummaryTimeTimeSeries().fillMissingDays(366)
            plc.values = plc.timestamps.mapIndexed { i, it ->
                SleepSummary(
                    it.minusMinutes(placeholderMinutes[i]),
                    it.plusHours(8).plusMinutes(placeholderMinutes[365 - i]),
                    listOf(it.plusHours(placeholderHours[i]).plusMinutes(placeholderMinutes[365 - i])),
                    listOf(it.plusHours(placeholderHours[i]).plusMinutes(placeholderMinutes[365 - i] - 5)),
                    listOf(1)
                )
            }
            return plc
        }

        @Keep
        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * timestamps. Provide a lambda that receives a [ZonedDateTime] and return a boolean.
         * */
        @Suppress("unused")
        fun filterByTimestamps(filter: (ZonedDateTime) -> Boolean): SleepSummaryTimeTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            return SleepSummaryTimeTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }

        @Keep
        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * values. Provide a lambda that receives a [SleepSummary] and returns a boolean.
         * */
        @Suppress("unused")
        fun filterByValue(filter: (SleepSummary) -> Boolean): SleepSummaryTimeTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(values[i])
            }
            return SleepSummaryTimeTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }
    }

    @Keep
    class TrendTimeSeries(
        values: List<TrendHolder> = mutableListOf(),
        timestamps: List<ZonedDateTime> = mutableListOf(),
        confidenceIntervalLow: List<TrendHolder> = mutableListOf(),
        confidenceIntervalHigh: List<TrendHolder> = mutableListOf(),
        confidence: List<Double> = mutableListOf(),
    ) : TimeSeries<TrendHolder>(
        values,
        timestamps,
        confidenceIntervalLow,
        confidenceIntervalHigh,
        confidence
    ) {
        /**
         * This is an `in-place` operation. Use this function to fill missing days (the function adds NaNs) both in the future and in the
         * past (up to the specified number of days in the past). Since the SDK does not return a value
         * for the score in a day where the confidence it too low, this function can be useful to fill
         * in blanks for UI/UX purposes.
         * @param rewindDays how many days to fill in in the past
         * @return the current [TimeSeries] as this is an `in-place` operation
         * */
        @Keep
        override fun fillMissingDays(
            rewindDays: Int,
            inplace: Boolean
        ): TrendTimeSeries {
            val newTimestamps = mutableListOf<ZonedDateTime>()
            val newValues = mutableListOf<TrendHolder>()
            val newConfidenceIntervalLow = mutableListOf<TrendHolder>()
            val newConfidenceIntervalHigh = mutableListOf<TrendHolder>()
            val newConfidence = mutableListOf<Double>()
            val currentDay = ZonedDateTime.now()
            var prevDate =
                if (timestamps.isNotEmpty()) timestamps[0].minusDays(rewindDays.toLong()) else currentDay.minusDays(
                    rewindDays.toLong()
                )

            if (timestamps.isEmpty()) {
                val nMissingDays = ChronoUnit.DAYS.between(prevDate.norm(), currentDay.norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(TrendHolder())
                    newConfidenceIntervalLow.add(TrendHolder())
                    newConfidenceIntervalHigh.add(TrendHolder())
                    newConfidence.add(NaN)
                }
            }

            for (i in values.indices) {
                val nMissingDays =
                    ChronoUnit.DAYS.between(prevDate.norm(), timestamps[i].norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(TrendHolder())
                    newConfidenceIntervalLow.add(TrendHolder())
                    newConfidenceIntervalHigh.add(TrendHolder())
                    newConfidence.add(NaN)
                }
                newTimestamps.add(timestamps[i])
                newValues.add(values[i])
                newConfidenceIntervalLow.add(confidenceIntervalLow[i])
                newConfidenceIntervalHigh.add(confidenceIntervalHigh[i])
                newConfidence.add(confidence[i])
                prevDate = timestamps[i]
            }

            // Adding missing days up to today
            val nMissingFutureDays = if (newTimestamps.isNotEmpty()) ChronoUnit.DAYS.between(
                newTimestamps.last().norm(),
                currentDay.norm()
            ) else 0
            val lastValue = newTimestamps.last()
            for (j in 0 until nMissingFutureDays) {
                newTimestamps.add(lastValue.norm().plusDays(j + 1L))
                newValues.add(TrendHolder())
                newConfidenceIntervalLow.add(TrendHolder())
                newConfidenceIntervalHigh.add(TrendHolder())
                newConfidence.add(NaN)
            }

            return if (inplace) {
                this.values = newValues
                this.confidenceIntervalLow = newConfidenceIntervalLow
                this.confidenceIntervalHigh = newConfidenceIntervalHigh
                this.confidence = newConfidence
                this.timestamps = newTimestamps
                this
            } else {
                TrendTimeSeries(
                    newValues,
                    newTimestamps,
                    newConfidenceIntervalLow,
                    newConfidenceIntervalHigh,
                    newConfidence
                )
            }
        }

        @Keep
        override fun takeLast(n: Int): TrendTimeSeries {
            return TrendTimeSeries(
                values.takeLast(n),
                timestamps.takeLast(n),
                confidenceIntervalLow.takeLast(n),
                confidenceIntervalHigh.takeLast(n),
                confidence.takeLast(n),
            )
        }

        @Keep
        override fun dropLast(n: Int): TrendTimeSeries {
            return TrendTimeSeries(
                values.dropLast(n), timestamps.dropLast(n),
                confidenceIntervalLow.dropLast(n),
                confidenceIntervalHigh.dropLast(n),
                confidence.dropLast(n)
            )
        }

        @Keep
        override fun getRandomSample(n: Int): TrendTimeSeries {
            val diff2w = List(n) { Random.nextDouble() * 100 }
            val stat2w = List(n) { Random.nextDouble() * 100 }
            val sign2w = List(n) { Random.nextDouble() }
            val diff6w = List(n) { Random.nextDouble() * 100 }
            val stat6w = List(n) { Random.nextDouble() * 100 }
            val sign6w = List(n) { Random.nextDouble() }
            val diff1y = List(n) { Random.nextDouble() * 100 }
            val stat1y = List(n) { Random.nextDouble() * 100 }
            val sign1y = List(n) { Random.nextDouble() }
            val conf = List(n) { Random.nextDouble() }
            val emptyTimeSeries = TrendTimeSeries()
            emptyTimeSeries.fillMissingDays(28, inplace = true)

            val values = List(n) {
                TrendHolder(
                    diff2w[it],
                    stat2w[it],
                    sign2w[it],
                    diff6w[it],
                    stat6w[it],
                    sign6w[it],
                    diff1y[it],
                    stat1y[it],
                    sign1y[it]
                )
            }

            return TrendTimeSeries(values, emptyTimeSeries.timestamps, values, values, conf)
        }

        override fun extractDoubleTimeSeries(flag: Int): DoubleTimeSeries {

            return when(flag) {
                0 -> DoubleTimeSeries(
                    this.values.map { it.difference2Weeks },
                    this.timestamps,
                    this.confidenceIntervalLow.map { it.difference2Weeks },
                    this.confidenceIntervalHigh.map { it.difference2Weeks },
                    this.confidence
                )
                1 -> DoubleTimeSeries(
                    this.values.map { it.difference6Weeks },
                    this.timestamps,
                    this.confidenceIntervalLow.map { it.difference6Weeks},
                    this.confidenceIntervalHigh.map { it.difference6Weeks },
                    this.confidence
                )
                else -> DoubleTimeSeries(
                    this.values.map { it.difference1Year },
                    this.timestamps,
                    this.confidenceIntervalLow.map { it.difference1Year },
                    this.confidenceIntervalHigh.map { it.difference1Year },
                    this.confidence
                )

            }
        }

        override fun extractMonthlyAverages(): TimeSeries<TrendHolder> {
            TODO("Not yet implemented")
        }

        override fun extractWeeklyAverages(): TimeSeries<TrendHolder> {
            TODO("Not yet implemented")
        }

        override fun dropna(): TimeSeries<TrendHolder> {
            return this.filterByValue { !it.isEmpty() }
        }

        override fun getPlaceholderTimeSeries(bias: Double, deviation: Double): TrendTimeSeries {
            TODO("Not yet implemented")
        }

        @Keep
        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * timestamps. Provide a lambda that receives a [ZonedDateTime] and return a boolean.
         * */
        @Suppress("unused")
        fun filterByTimestamps(filter: (ZonedDateTime) -> Boolean): TrendTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            return TrendTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }

        @Keep
        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * values. Provide a lambda that receives a [SleepSummary] and returns a boolean.
         * */
        @Suppress("unused")
        fun filterByValue(filter: (TrendHolder) -> Boolean): TrendTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(values[i])
            }
            return TrendTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }
    }

    @Keep
    class ScreenTimeAggregateTimeSeries(
        values: List<ScreenTimeAggregate> = mutableListOf(),
        timestamps: List<ZonedDateTime> = mutableListOf(),
        confidenceIntervalLow: List<ScreenTimeAggregate> = mutableListOf(),
        confidenceIntervalHigh: List<ScreenTimeAggregate> = mutableListOf(),
        confidence: List<Double> = mutableListOf(),
    ) : TimeSeries<ScreenTimeAggregate>(
        values,
        timestamps,
        confidenceIntervalLow,
        confidenceIntervalHigh,
        confidence
    ) {
        /**
         * This is an `in-place` operation. Use this function to fill missing days (the function adds NaNs) both in the future and in the
         * past (up to the specified number of days in the past). Since the SDK does not return a value
         * for the score in a day where the confidence it too low, this function can be useful to fill
         * in blanks for UI/UX purposes.
         * @param rewindDays how many days to fill in in the past
         * @return the current [TimeSeries] as this is an `in-place` operation
         * */
        @Keep
        override fun fillMissingDays(rewindDays: Int, inplace: Boolean): ScreenTimeAggregateTimeSeries {
            val newTimestamps = mutableListOf<ZonedDateTime>()
            val newValues = mutableListOf<ScreenTimeAggregate>()
            val newConfidenceIntervalLow = mutableListOf<ScreenTimeAggregate>()
            val newConfidenceIntervalHigh = mutableListOf<ScreenTimeAggregate>()
            val newConfidence = mutableListOf<Double>()
            val currentDay = ZonedDateTime.now()
            var prevDate =
                if (timestamps.isNotEmpty()) timestamps[0].minusDays(rewindDays.toLong()) else currentDay.minusDays(
                    rewindDays.toLong()
                )

            if (timestamps.isEmpty()) {
                val nMissingDays = ChronoUnit.DAYS.between(prevDate.norm(), currentDay.norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(ScreenTimeAggregate())
                    newConfidenceIntervalLow.add(ScreenTimeAggregate())
                    newConfidenceIntervalHigh.add(ScreenTimeAggregate())
                    newConfidence.add(NaN)
                }
            }

            for (i in values.indices) {
                val nMissingDays =
                    ChronoUnit.DAYS.between(prevDate.norm(), timestamps[i].norm()) - 1
                for (j in 0 until nMissingDays) {
                    newTimestamps.add(prevDate.norm().plusDays(j + 1L))
                    newValues.add(ScreenTimeAggregate())
                    newConfidenceIntervalLow.add(ScreenTimeAggregate())
                    newConfidenceIntervalHigh.add(ScreenTimeAggregate())
                    newConfidence.add(NaN)
                }
                newTimestamps.add(timestamps[i])
                newValues.add(values[i])
                newConfidenceIntervalLow.add(confidenceIntervalLow[i])
                newConfidenceIntervalHigh.add(confidenceIntervalHigh[i])
                newConfidence.add(confidence[i])
                prevDate = timestamps[i]
            }

            // Adding missing days up to today
            val nMissingFutureDays = if (newTimestamps.isNotEmpty()) ChronoUnit.DAYS.between(
                newTimestamps.last().norm(),
                currentDay.norm()
            ) else 0
            val lastValue = newTimestamps.last()
            for (j in 0 until nMissingFutureDays) {
                newTimestamps.add(lastValue.norm().plusDays(j + 1L))
                newValues.add(ScreenTimeAggregate())
                newConfidenceIntervalLow.add(ScreenTimeAggregate())
                newConfidenceIntervalHigh.add(ScreenTimeAggregate())
                newConfidence.add(NaN)
            }

            return if (inplace) {
                this.values = newValues
                this.confidenceIntervalLow = newConfidenceIntervalLow
                this.confidenceIntervalHigh = newConfidenceIntervalHigh
                this.confidence = newConfidence
                this.timestamps = newTimestamps
                this
            } else {
                ScreenTimeAggregateTimeSeries(
                    newValues,
                    newTimestamps,
                    newConfidenceIntervalLow,
                    newConfidenceIntervalHigh,
                    newConfidence
                )
            }
        }

        @Keep
        override fun takeLast(n: Int): ScreenTimeAggregateTimeSeries {
            return ScreenTimeAggregateTimeSeries(
                values.takeLast(n),
                timestamps.takeLast(n),
                confidenceIntervalLow.takeLast(n),
                confidenceIntervalHigh.takeLast(n),
                confidence.takeLast(n),
            )
        }

        @Keep
        override fun dropLast(n: Int): ScreenTimeAggregateTimeSeries {
            return ScreenTimeAggregateTimeSeries(
                values.dropLast(n), timestamps.dropLast(n),
                confidenceIntervalLow.dropLast(n),
                confidenceIntervalHigh.dropLast(n),
                confidence.dropLast(n)
            )
        }

        override fun getRandomSample(n: Int): TimeSeries<ScreenTimeAggregate> {
            TODO("Not yet implemented")
        }

        override fun extractDoubleTimeSeries(flag: Int): DoubleTimeSeries {
            return DoubleTimeSeries(
                this.values.map { it.socialScreenTime },
                this.timestamps,
                this.confidenceIntervalLow.map { it.socialScreenTime },
                this.confidenceIntervalHigh.map { it.socialScreenTime },
                this.confidence
            )
        }

        override fun extractMonthlyAverages(): ScreenTimeAggregateTimeSeries {

            val averagedValues = timestamps.zip(values).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) ->
                ScreenTimeAggregate(
                    v.map { it.totalScreenTime }.filter { !it.isNaN() }.average(),
                    v.map { it.socialScreenTime }.filter { !it.isNaN() }.average()
                )
            }

            val averagedConfidenceIntervalLow = timestamps.zip(confidenceIntervalLow).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) ->
                ScreenTimeAggregate(
                    v.map { it.totalScreenTime }.filter { !it.isNaN() }.average(),
                    v.map { it.socialScreenTime }.filter { !it.isNaN() }.average()
                )
            }

            val averagedConfidenceIntervalHigh = timestamps.zip(confidenceIntervalHigh).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) ->
                ScreenTimeAggregate(
                    v.map { it.totalScreenTime }.filter { !it.isNaN() }.average(),
                    v.map { it.socialScreenTime }.filter { !it.isNaN() }.average()
                )
            }

            val averagedConfidence = timestamps.zip(confidence).groupBy({
                it.first.withDayOfMonth(1).withHour(0).norm().withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            return ScreenTimeAggregateTimeSeries(
                averagedValues.values.toList(),
                averagedValues.keys.toList(),
                averagedConfidenceIntervalLow.values.toList(),
                averagedConfidenceIntervalHigh.values.toList(),
                averagedConfidence.values.toList()
            )

        }


        override fun extractWeeklyAverages(): ScreenTimeAggregateTimeSeries {

            val averagedValues = timestamps.zip(values).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) ->
                ScreenTimeAggregate(
                    v.map { it.totalScreenTime }.filter { !it.isNaN() }.average(),
                    v.map { it.socialScreenTime }.filter { !it.isNaN() }.average()
                )
            }

            val averagedConfidenceIntervalLow = timestamps.zip(confidenceIntervalLow).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) ->
                ScreenTimeAggregate(
                    v.map { it.totalScreenTime }.filter { !it.isNaN() }.average(),
                    v.map { it.socialScreenTime }.filter { !it.isNaN() }.average()
                )
            }

            val averagedConfidenceIntervalHigh = timestamps.zip(confidenceIntervalHigh).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) ->
                ScreenTimeAggregate(
                    v.map { it.totalScreenTime }.filter { !it.isNaN() }.average(),
                    v.map { it.socialScreenTime }.filter { !it.isNaN() }.average()
                )
            }

            val averagedConfidence = timestamps.zip(confidence).groupBy({
                it.first.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1))).norm()
                    .withZoneSameLocal(ZoneId.systemDefault())
            }, { it.second }).mapValues { (_, v) -> v.filter { !it.isNaN() }.average() }

            return ScreenTimeAggregateTimeSeries(
                averagedValues.values.toList(),
                averagedValues.keys.toList(),
                averagedConfidenceIntervalLow.values.toList(),
                averagedConfidenceIntervalHigh.values.toList(),
                averagedConfidence.values.toList()
            )
        }

        override fun dropna(): TimeSeries<ScreenTimeAggregate> {
            return this.filterByValues { !it.totalScreenTime.isNaN() }
        }

        override fun getPlaceholderTimeSeries(bias: Double, deviation: Double): ScreenTimeAggregateTimeSeries {
            val plc = ScreenTimeAggregateTimeSeries().fillMissingDays(366)
            plc.values = placeholderScreenTime.map { ScreenTimeAggregate(it, it / 3) }
            return plc
        }

        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * timestamps. Provide a lambda that receives a [ZonedDateTime] and return a boolean.
         * */
        @Suppress("unused")
        @Keep
        fun filterByTimestamps(filter: (ZonedDateTime) -> Boolean): ScreenTimeAggregateTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(timestamps[i])
            }
            return ScreenTimeAggregateTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }

        /**
         * This function helps filtering the [TimeSeries] for elements that follow a certain rule in the
         * values. Provide a lambda that receives a [Double] and returns a boolean.
         * */
        @Suppress("unused")
        @Keep
        fun filterByValues(filter: (ScreenTimeAggregate) -> Boolean): ScreenTimeAggregateTimeSeries {
            val newValues = values.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalLow = confidenceIntervalLow.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidenceIntervalHigh = confidenceIntervalHigh.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newConfidence = confidence.filterIndexed { i, _ ->
                filter(values[i])
            }
            val newTimestamps = timestamps.filterIndexed { i, _ ->
                filter(values[i])
            }
            return ScreenTimeAggregateTimeSeries(
                newValues,
                newTimestamps,
                newConfidenceIntervalLow,
                newConfidenceIntervalHigh,
                newConfidence
            )
        }
    }
}

/**
 * This function calculates the period mean of a series of [ZonedDateTime]. Given this is a periodic
 * mean and not a full mean the result is not a [ZonedDateTime] but a [LocalDateTime].
 * This function only cares about the `local` time when calculating the mean.
 * You can ue this to calculate for example the average wake up time of a person
 * given a list of wake up times.
 * @param zonedDateTimes list of ZonedDateTimes to average
 * @return a [LocalDateTime] corresponding to the average (note the seed of the local date time is [Instant.now])
 * */
@Keep
fun periodicMean(
    zonedDateTimes: List<ZonedDateTime>,
    references: List<ZonedDateTime>,
    reference: LocalDateTime
): ZonedDateTime {

    if (zonedDateTimes.isEmpty()) return ZonedDateTimePlaceholder
    val rawShifts = zonedDateTimes.zip(references).map { (z, r) ->
        z.toEpochSecond() - r.toEpochSecond()
    }
//    print('periodic mean raw shifts: $rawShifts');
//    print('periodic mean reference: $reference');
//    print('periodic mean: ${reference.add(Duration(seconds: rawShifts.average.toInt()))}');

//    Timber.d("periodic mean raw shifts: $rawShifts")
//    Timber.d("periodic mean reference: $reference")
//    Timber.d("periodic mean: ${reference.plusSeconds(rawShifts.average().toLong())}")


    return ZonedDateTime.of(
        reference.plusSeconds(rawShifts.average().toLong()),
        ZoneId.systemDefault()
    )
}


/**
 * @suppress
 * */
@JvmName("dropnaDouble")
@Keep
fun List<Double>.dropna(): List<Double> {
    return this.filter { !it.isNaN() }
}

/**
 * @suppress
 * */
@JvmName("dropnaScreenTime")
@Keep
fun List<ScreenTimeAggregate>.dropna(): List<ScreenTimeAggregate> {
    return this.filter { !it.totalScreenTime.isNaN() }
}

/**
 * @suppress
 * */
@JvmName("dropnaZonedDateTime")
@Keep
fun List<ZonedDateTime>.dropna(): List<ZonedDateTime> {
    return this.filter { it != ZonedDateTimePlaceholder }
}
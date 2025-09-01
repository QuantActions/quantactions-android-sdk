/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("unused", "ClassName", "MemberVisibilityCanBePrivate")

package com.quantactions.sdk

import androidx.annotation.Keep
import com.hadiyarajesh.flower_core.ApiResponse
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.entity.TrendEntity
import com.quantactions.sdk.data.model.TrendHolder
import com.quantactions.sdk.data.repository.MVPDao
import kotlinx.coroutines.flow.Flow

/**
 * Enumeration class that holds all of the info for the metrics
 * @property id name of the trend
 * @property code to get it from TIE (e.g. XXX-XXX-XXX-XXX)
 * @property range [PopulationRange] of the population distribution
 * */
@Suppress("HardCodedStringLiteral")
@Keep
sealed class Trend<P : TimestampedEntity, T> (
    val id: String,
    val code: String,
) : CanReturnCompiledTimeSeries<P, T> {

    // SPEED SCORES TRENDS
    @Keep
    object COGNITIVE_FITNESS : FilterByTimeZoneTrendObject(
        "cognitive_trend",
        "003-003-002-004",
    )

    @Keep
    object ACTION_SPEED : FilterByTimeZoneTrendObject(
        "action_trend",
        "003-003-002-001",
    )

    @Keep
    object TYPING_SPEED : FilterByTimeZoneTrendObject(
        "typing_trend",
        "003-003-002-003",
    )

    // SLEEP SCORES TREND
    @Keep
    object SLEEP_SCORE : BasicTrendObject(
        "sleep_trend",
        "003-003-001-004",
    )

    @Keep
    object SLEEP_LENGTH : BasicTrendObject(
        "sleep_length_trend",
        "003-003-001-001",
    )

    @Keep
    object SLEEP_INTERRUPTIONS : BasicTrendObject(
        "sleep_interruptions_trend",
        "003-003-001-003",
    )

    // SOCIAL ENGAGEMENT TREND
    @Keep
    object SOCIAL_ENGAGEMENT : FilterByTimeZoneTrendObject(
        "social_engagement_trend",
        "003-003-003-005",
    ) {
        override suspend fun cacheHealthyRanges(apiService: ApiService,
                                                managePref2: GenericPreferences,
                                                identityId: String) {
            TODO("Not yet defined for this trend")
        }
    }

    @Keep
    object SOCIAL_SCREEN_TIME : FilterByTimeZoneTrendObject(
        "social_screen_time_trend",
        "003-003-003-002",
    )

    @Keep
    object SOCIAL_TAPS : FilterByTimeZoneTrendObject(
        "social_taps_trend",
        "003-003-003-004", // for now this is screen time trend but it should be in taps
    )

    /**
     * The mental fitness dynamics shows the most recent changes in your mental fitness indicators,
     * including sleep and cognitive fitness. We look at the last week's data to see if you are
     * trending up, stable, or down overall.
     *
     */
    @Keep
    object THE_WAVE : BasicTrendObject(
        "the_wave_trend",
        "003-003-004-001",
    )
}


/**
 * @suppress
 */
@Keep
open class BasicTrendObject(id: String, code: String) :
    Metric<TrendEntity, TrendHolder>(
        id, code, 14, // eta is always 14 for trends
        PopulationRange()
    ) {

    @Keep
    override fun prepareReturnData(
        values: List<TrendEntity>,
        from: Long,
        to: Long
    ): TimeSeries<TrendHolder> {
        val trends = values.map {
            TrendHolder(
                it.diff2W ?: Double.NaN,
                it.stat2W ?: Double.NaN,
                it.sign2W ?: Double.NaN,
                it.diff6W ?: Double.NaN,
                it.stat6W ?: Double.NaN,
                it.sign6W ?: Double.NaN,
                it.diff1Y ?: Double.NaN,
                it.stat1Y ?: Double.NaN,
                it.sign1Y ?: Double.NaN,
            )
        }

        return TimeSeries.TrendTimeSeries(
            trends,
            values.map { statistic -> statistic.timestamp.localize() },
            values.map { TrendHolder() },
            values.map { TrendHolder() },
            values.map { Double.NaN },
        )
    }

    /**
     * Use this function to retrieve the reference population values for the current user.
     * You need to provide the [BasicInfo] that you can obtain from [QA.basicInfo].
     * @param basicInfo a [BasicInfo] object.
     * @return [Range] object containing high (75th percentile) and low (25th percentile) for
     * the score in the reference healthy population.
     * */
    @Keep
    override fun getReferencePopulationRange(basicInfo: BasicInfo): Range {
        val high = range.get75thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
        val low = range.get25thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
        return Range(high, low)
    }

    override fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<TrendEntity>> {
        return mvpDao.getTrend(code, from, to)
    }

    override suspend fun getStat(
        apiService: ApiService,
        identityId: String,
        participationId: String,
        from: String,
        to: String
    ): ApiResponse<List<TrendEntity>> {
        val filter = prepareFilter(code, from, to)
        return apiService.getTrendEntity(identityId, participationId, filter, code.container())
    }

    override fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<TrendEntity>) {
        mvpDao.insertOrUpdateTrend(statistics)
    }

    @Keep
    override fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<TrendHolder>): TimeSeries<TrendHolder> {
        return timeSeries
    }

    @Keep
    override fun returnEmptyTimeSeries(): TimeSeries<TrendHolder> {
        return TimeSeries.TrendTimeSeries()
    }

    override suspend fun cacheHealthyRanges(apiService: ApiService,
                                            managePref2: GenericPreferences,
                                            identityId: String) {
        TODO("Not yet defined for this trend")
    }
}

/**
 * @suppress
 */
@Keep
open class FilterByTimeZoneTrendObject(id: String, code: String) :
    Metric<TrendEntity, TrendHolder>(
        id, code, 14, // eta is always 14 for trends
        PopulationRange()
    ) {

    @Keep
    override fun prepareReturnData(
        values: List<TrendEntity>,
        from: Long,
        to: Long
    ): TimeSeries<TrendHolder> {
        val trends = values.map {
            TrendHolder(
                it.diff2W ?: Double.NaN,
                it.stat2W ?: Double.NaN,
                it.sign2W ?: Double.NaN,
                it.diff6W ?: Double.NaN,
                it.stat6W ?: Double.NaN,
                it.sign6W ?: Double.NaN,
                it.diff1Y ?: Double.NaN,
                it.stat1Y ?: Double.NaN,
                it.sign1Y ?: Double.NaN,
            )
        }

        val ret = TimeSeries.TrendTimeSeries(
            trends,
            values.map { statistic ->
                statistic.timestamp.localize()
            },
            values.map { TrendHolder() },
            values.map { TrendHolder() },
            values.map { Double.NaN },
        )

        return ret
    }

    /**
     * Use this function to retrieve the reference population values for the current user.
     * You need to provide the [BasicInfo] that you can obtain from [QA.basicInfo].
     * @param basicInfo a [BasicInfo] object.
     * @return [Range] object containing high (75th percentile) and low (25th percentile) for
     * the score in the reference healthy population.
     * */
    @Keep
    override fun getReferencePopulationRange(basicInfo: BasicInfo): Range {
        val high = range.get75thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
        val low = range.get25thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
        return Range(high, low)
    }

    override fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<TrendEntity>> {
        return mvpDao.getTrend(code, from, to)
    }

    override suspend fun getStat(
        apiService: ApiService,
        identityId: String,
        participationId: String,
        from: String,
        to: String
    ): ApiResponse<List<TrendEntity>> {
        val filter = prepareFilter(code, from, to)
        return apiService.getTrendEntity(identityId, participationId, filter, code.container())
    }

    override fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<TrendEntity>) {
        mvpDao.insertOrUpdateTrend(statistics)
    }

    @Keep
    override fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<TrendHolder>): TimeSeries<TrendHolder> {
        val evolution =
            timeSeries.values.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

        val timestamps = timeSeries.timestamps.filterIndexed { i, _ ->
            timeSeries.timestamps[i].hour == 0
        }

        val confidenceLow = timeSeries.confidenceIntervalLow.filterIndexed { i, _ ->
            timeSeries.timestamps[i].hour == 0
        }

        val confidenceHigh = timeSeries.confidenceIntervalHigh.filterIndexed { i, _ ->
            timeSeries.timestamps[i].hour == 0
        }

        val confidence = timeSeries.confidence.filterIndexed { i, _ ->
            timeSeries.timestamps[i].hour == 0
        }

        return TimeSeries.TrendTimeSeries(evolution, timestamps, confidenceLow, confidenceHigh, confidence)
    }

    @Keep
    override fun returnEmptyTimeSeries(): TimeSeries<TrendHolder> {
        return TimeSeries.TrendTimeSeries()
    }

    override suspend fun cacheHealthyRanges(apiService: ApiService,
                                            managePref2: GenericPreferences,
                                            identityId: String) {
        TODO("Not yet defined for this trend")
    }
}
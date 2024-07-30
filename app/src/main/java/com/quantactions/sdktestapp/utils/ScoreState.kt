package com.quantactions.sdktestapp.utils

import com.quantactions.sdk.TimeSeries
import com.quantactions.sdktestapp.WatchableScoreOrTrend

sealed class ScoreState(open val scoreOrTrend: WatchableScoreOrTrend) : GetDoubleTimeSeriesFromState {
    data class ScoreLoading(override val scoreOrTrend: WatchableScoreOrTrend) : ScoreState(scoreOrTrend) {
        override fun getDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries {
            return TimeSeries.DoubleTimeSeries()
        }
    }

    data class ScoreCurrentlyUnavailable(override val scoreOrTrend: WatchableScoreOrTrend) : ScoreState(scoreOrTrend) {
        override fun getDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries {
            return TimeSeries.DoubleTimeSeries()
        }
    }

    data class ScoreAvailable(
        override val scoreOrTrend: WatchableScoreOrTrend,
        val timeSeries: TimeSeries.DoubleTimeSeries,
        var visibility: Boolean = true
        ) : ScoreState(scoreOrTrend) {
        override fun getDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries {
            return timeSeries.extractDoubleTimeSeries(flag)
        }
    }

    data class ScreenTimeAggregateAvailable(
        override val scoreOrTrend: WatchableScoreOrTrend,
        val timeSeries: TimeSeries.ScreenTimeAggregateTimeSeries,
        var visibility: Boolean = true
    ) : ScoreState(scoreOrTrend) {
        override fun getDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries {
            return timeSeries.extractDoubleTimeSeries(flag)
        }
    }

    data class TrendAvailable(
        override val scoreOrTrend: WatchableScoreOrTrend,
        val timeSeries: TimeSeries.TrendTimeSeries,
        var visibility: Boolean = true
    ) : ScoreState(scoreOrTrend) {
        override fun getDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries {
            return timeSeries.extractDoubleTimeSeries(flag)
        }
    }

    data class SleepSummaryAvailable(
        override val scoreOrTrend: WatchableScoreOrTrend,
        val timeSeries: TimeSeries.SleepSummaryTimeTimeSeries,
        var visibility: Boolean = true
    ) : ScoreState(scoreOrTrend) {
        override fun getDoubleTimeSeries(flag: Int): TimeSeries.DoubleTimeSeries {
            return timeSeries.extractDoubleTimeSeries(flag)
        }
    }
}

interface GetDoubleTimeSeriesFromState {
    fun getDoubleTimeSeries(flag: Int = 0): TimeSeries.DoubleTimeSeries
}
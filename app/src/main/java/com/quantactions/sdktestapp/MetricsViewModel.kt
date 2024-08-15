/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("unused")

package com.quantactions.sdktestapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.quantactions.sdk.*
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdktestapp.utils.ScoreState

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * This is the view model that holds the connection to the SDK and it is responsible to retrieve
 * and serve the scores from the SDK.
 * Holds also a reference to the calendar to retrieve correctly the needed data.
 * The implementation is modular in such a way that when new metrics will come in it is easy enough
 * to add them to here
 * @param application Android application
 * */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
open class MetricsViewModel @Inject constructor(
    application: Application,
    private val qa: QA,
) : AndroidViewModel(application) {

    private val apiKey = BuildConfig.QA_API_KEY

    private val mapOfScoresStates =
        mutableMapOf<WatchableScoreOrTrend, MutableStateFlow<ScoreState>>(

            // Scores

            Score.SLEEP_SCORE to MutableStateFlow(ScoreState.ScoreLoading(Score.SLEEP_SCORE)),
            Score.SLEEP_SUMMARY to MutableStateFlow(ScoreState.ScoreLoading(Score.SLEEP_SUMMARY)),
            Score.SLEEP_INTERRUPTIONS to MutableStateFlow(ScoreState.ScoreLoading(Score.SLEEP_INTERRUPTIONS)),

            Score.COGNITIVE_FITNESS to MutableStateFlow(ScoreState.ScoreLoading(Score.COGNITIVE_FITNESS)),
            Score.ACTION_SPEED to MutableStateFlow(ScoreState.ScoreLoading(Score.ACTION_SPEED)),
            Score.TYPING_SPEED to MutableStateFlow(ScoreState.ScoreLoading(Score.TYPING_SPEED)),

            Score.SOCIAL_ENGAGEMENT to MutableStateFlow(ScoreState.ScoreLoading(Score.SOCIAL_ENGAGEMENT)),
            Score.SCREEN_TIME_AGGREGATE to MutableStateFlow(ScoreState.ScoreLoading(Score.SCREEN_TIME_AGGREGATE)),
            Score.SOCIAL_TAPS to MutableStateFlow(ScoreState.ScoreLoading(Score.SOCIAL_TAPS)),

            // Trends

            ScoreTrend.SLEEP_SCORE to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.SLEEP_SCORE)),
            ScoreTrend.SLEEP_LENGTH to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.SLEEP_LENGTH)),
            ScoreTrend.SLEEP_INTERRUPTIONS to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.SLEEP_INTERRUPTIONS)),

            ScoreTrend.COGNITIVE_FITNESS to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.COGNITIVE_FITNESS)),
            ScoreTrend.ACTION_SPEED to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.ACTION_SPEED)),
            ScoreTrend.TYPING_SPEED to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.TYPING_SPEED)),

            ScoreTrend.SOCIAL_ENGAGEMENT to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.SOCIAL_ENGAGEMENT)),
            ScoreTrend.SOCIAL_SCREEN_TIME to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.SOCIAL_SCREEN_TIME)),
            ScoreTrend.SOCIAL_TAPS to MutableStateFlow(ScoreState.ScoreLoading(ScoreTrend.SOCIAL_TAPS)),
        )

    // UI state
    internal val mapOfScoresStateFlows = mutableMapOf<WatchableScoreOrTrend, StateFlow<ScoreState>>(

        // Scores

        Score.SLEEP_SCORE to mapOfScoresStates[Score.SLEEP_SCORE]!!.asStateFlow(),
        Score.SLEEP_SUMMARY to mapOfScoresStates[Score.SLEEP_SUMMARY]!!.asStateFlow(),
        Score.SLEEP_INTERRUPTIONS to mapOfScoresStates[Score.SLEEP_INTERRUPTIONS]!!.asStateFlow(),

        Score.COGNITIVE_FITNESS to mapOfScoresStates[Score.COGNITIVE_FITNESS]!!.asStateFlow(),
        Score.ACTION_SPEED to mapOfScoresStates[Score.ACTION_SPEED]!!.asStateFlow(),
        Score.TYPING_SPEED to mapOfScoresStates[Score.TYPING_SPEED]!!.asStateFlow(),

        Score.SOCIAL_ENGAGEMENT to mapOfScoresStates[Score.SOCIAL_ENGAGEMENT]!!.asStateFlow(),
        Score.SCREEN_TIME_AGGREGATE to mapOfScoresStates[Score.SCREEN_TIME_AGGREGATE]!!.asStateFlow(),
        Score.SOCIAL_TAPS to mapOfScoresStates[Score.SOCIAL_TAPS]!!.asStateFlow(),

        // Trends
        ScoreTrend.SLEEP_SCORE to mapOfScoresStates[ScoreTrend.SLEEP_SCORE]!!.asStateFlow(),
        ScoreTrend.SLEEP_LENGTH to mapOfScoresStates[ScoreTrend.SLEEP_LENGTH]!!.asStateFlow(),
        ScoreTrend.SLEEP_INTERRUPTIONS to mapOfScoresStates[ScoreTrend.SLEEP_INTERRUPTIONS]!!.asStateFlow(),

        ScoreTrend.COGNITIVE_FITNESS to mapOfScoresStates[ScoreTrend.COGNITIVE_FITNESS]!!.asStateFlow(),
        ScoreTrend.ACTION_SPEED to mapOfScoresStates[ScoreTrend.ACTION_SPEED]!!.asStateFlow(),
        ScoreTrend.TYPING_SPEED to mapOfScoresStates[ScoreTrend.TYPING_SPEED]!!.asStateFlow(),

        ScoreTrend.SOCIAL_ENGAGEMENT to mapOfScoresStates[ScoreTrend.SOCIAL_ENGAGEMENT]!!.asStateFlow(),
        ScoreTrend.SOCIAL_SCREEN_TIME to mapOfScoresStates[ScoreTrend.SOCIAL_SCREEN_TIME]!!.asStateFlow(),
        ScoreTrend.SOCIAL_TAPS to mapOfScoresStates[ScoreTrend.SOCIAL_TAPS]!!.asStateFlow(),

        )

    private val _showConfidence = MutableStateFlow(true)
    val showConfidence = _showConfidence.asStateFlow()

    private val metricsAndTrendsList = listOf(
        Metric.COGNITIVE_FITNESS,
        Metric.ACTION_SPEED,
        Metric.TYPING_SPEED,
        Metric.SOCIAL_ENGAGEMENT,
        Metric.SLEEP_SUMMARY,
        Metric.SLEEP_SCORE,
        Metric.SCREEN_TIME_AGGREGATE,
        Metric.SOCIAL_TAPS,
        Trend.COGNITIVE_FITNESS,
        Trend.ACTION_SPEED,
        Trend.SOCIAL_ENGAGEMENT,
        Trend.SLEEP_SCORE,
        Trend.SLEEP_LENGTH,
        Trend.SLEEP_INTERRUPTIONS,
        Trend.SOCIAL_SCREEN_TIME,
        Trend.SOCIAL_TAPS,
        Trend.TYPING_SPEED,
    )

    private val preparation by lazy {
        metricsAndTrendsList.forEach { getStat(it) }
        true
    }

    init {
        preparation
    }

    fun changeShowConfidence(newValue: Boolean) {
        _showConfidence.value = newValue
    }

    fun toggleMetricVisibility(scoreOrTrend: WatchableScoreOrTrend) {
        val currentVal = mapOfScoresStates[scoreOrTrend]!!.value
        val newValue: ScoreState.ScoreAvailable
        if (currentVal is ScoreState.ScoreAvailable) {
            newValue =
                ScoreState.ScoreAvailable(
                    scoreOrTrend,
                    currentVal.timeSeries,
                    !currentVal.visibility
                )
            mapOfScoresStates[scoreOrTrend]!!.value = newValue
        }
    }


    @ExperimentalCoroutinesApi
    open fun <P : TimestampedEntity, T> getStat(score: Metric<P, T>, force: Boolean = false) {

        val from = 0L
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                when (score) {
                    Trend.COGNITIVE_FITNESS -> {
                        qa.getMetricSample(context, apiKey, Trend.COGNITIVE_FITNESS, from = from)
                            .collect {
                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[ScoreTrend.COGNITIVE_FITNESS]!!.value =
                                        ScoreState.TrendAvailable(
                                            ScoreTrend.COGNITIVE_FITNESS,
                                            it as TimeSeries.TrendTimeSeries
                                        )
                                }
                            }
                    }

                    Trend.SLEEP_SCORE -> {
                        qa.getMetricSample(context, apiKey, Trend.SLEEP_SCORE, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[ScoreTrend.SLEEP_SCORE]!!.value =
                                    ScoreState.TrendAvailable(
                                        ScoreTrend.SLEEP_SCORE,
                                        it as TimeSeries.TrendTimeSeries
                                    )
                            }
                        }
                    }

                    Trend.SOCIAL_ENGAGEMENT -> {
                        qa.getMetricSample(context, apiKey, Trend.SOCIAL_ENGAGEMENT, from = from)
                            .collect {
                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[ScoreTrend.SOCIAL_ENGAGEMENT]!!.value =
                                        ScoreState.TrendAvailable(
                                            ScoreTrend.SOCIAL_ENGAGEMENT,
                                            it as TimeSeries.TrendTimeSeries
                                        )
                                }
                            }
                    }

                    Trend.SLEEP_LENGTH -> {
                        qa.getMetricSample(context, apiKey, Trend.SLEEP_LENGTH, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[ScoreTrend.SLEEP_LENGTH]!!.value =
                                    ScoreState.TrendAvailable(
                                        ScoreTrend.SLEEP_LENGTH,
                                        it as TimeSeries.TrendTimeSeries
                                    )
                            }
                        }
                    }

                    Trend.SLEEP_INTERRUPTIONS -> {
                        qa.getMetricSample(context, apiKey, Trend.SLEEP_INTERRUPTIONS, from = from)
                            .collect {
                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[ScoreTrend.SLEEP_INTERRUPTIONS]!!.value =
                                        ScoreState.TrendAvailable(
                                            ScoreTrend.SLEEP_INTERRUPTIONS,
                                            it as TimeSeries.TrendTimeSeries
                                        )
                                }
                            }
                    }

                    Trend.ACTION_SPEED -> {
                        qa.getMetricSample(context, apiKey, Trend.ACTION_SPEED, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[ScoreTrend.ACTION_SPEED]!!.value =
                                    ScoreState.TrendAvailable(
                                        ScoreTrend.ACTION_SPEED,
                                        it as TimeSeries.TrendTimeSeries
                                    )
                            }
                        }
                    }

                    Trend.TYPING_SPEED -> {
                        qa.getMetricSample(context, apiKey, Trend.TYPING_SPEED, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[ScoreTrend.TYPING_SPEED]!!.value =
                                    ScoreState.TrendAvailable(
                                        ScoreTrend.TYPING_SPEED,
                                        it as TimeSeries.TrendTimeSeries
                                    )
                            }
                        }
                    }

                    Trend.SOCIAL_SCREEN_TIME -> {
                        qa.getMetricSample(context, apiKey, Trend.SOCIAL_SCREEN_TIME, from = from)
                            .collect {
                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[ScoreTrend.SOCIAL_SCREEN_TIME]!!.value =
                                        ScoreState.TrendAvailable(
                                            ScoreTrend.SOCIAL_SCREEN_TIME,
                                            it as TimeSeries.TrendTimeSeries
                                        )
                                }
                            }
                    }

                    Trend.SOCIAL_TAPS -> {
                        qa.getMetricSample(context, apiKey, Trend.SOCIAL_TAPS, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[ScoreTrend.SOCIAL_TAPS]!!.value =
                                    ScoreState.TrendAvailable(
                                        ScoreTrend.SOCIAL_TAPS,
                                        it as TimeSeries.TrendTimeSeries
                                    )
                            }
                        }
                    }

                    Metric.COGNITIVE_FITNESS -> {
                        qa.getMetricSample(context, apiKey, Metric.COGNITIVE_FITNESS, from = from)
                            .collect {
                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[Score.COGNITIVE_FITNESS]!!.value =
                                        ScoreState.ScoreAvailable(
                                            Score.COGNITIVE_FITNESS,
                                            it as TimeSeries.DoubleTimeSeries
                                        )
                                }
                            }
                    }

                    Metric.ACTION_SPEED -> {
                        qa.getMetricSample(context, apiKey, Metric.ACTION_SPEED, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[Score.ACTION_SPEED]!!.value =
                                    ScoreState.ScoreAvailable(
                                        Score.ACTION_SPEED,
                                        it as TimeSeries.DoubleTimeSeries
                                    )
                            }
                        }
                    }

                    Metric.SOCIAL_TAPS -> {
                        qa.getMetricSample(context, apiKey, Metric.SOCIAL_TAPS, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[Score.SOCIAL_TAPS]!!.value =
                                    ScoreState.ScoreAvailable(
                                        Score.SOCIAL_TAPS,
                                        it as TimeSeries.DoubleTimeSeries
                                    )
                            }
                        }
                    }

                    Metric.TYPING_SPEED -> {
                        qa.getMetricSample(context, apiKey, Metric.TYPING_SPEED, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[Score.TYPING_SPEED]!!.value =
                                    ScoreState.ScoreAvailable(
                                        Score.TYPING_SPEED,
                                        it as TimeSeries.DoubleTimeSeries
                                    )
                            }
                        }
                    }

                    Metric.SLEEP_SCORE -> {
                        qa.getMetricSample(context, apiKey, Metric.SLEEP_SCORE, from = from).collect {

                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[Score.SLEEP_SCORE]!!.value =
                                    ScoreState.ScoreAvailable(
                                        Score.SLEEP_SCORE,
                                        it as TimeSeries.DoubleTimeSeries
                                    )
                            }
                        }
                    }

                    Metric.SOCIAL_ENGAGEMENT -> {
                        qa.getMetricSample(context, apiKey, Metric.SOCIAL_ENGAGEMENT, from = from)
                            .collect {

                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[Score.SOCIAL_ENGAGEMENT]!!.value =
                                        ScoreState.ScoreAvailable(
                                            Score.SOCIAL_ENGAGEMENT,
                                            it as TimeSeries.DoubleTimeSeries
                                        )
                                }
                            }
                    }

                    Metric.SCREEN_TIME_AGGREGATE -> {
                        qa.getMetricSample(context, apiKey, Metric.SCREEN_TIME_AGGREGATE, from = from)
                            .collect {
                                if (it.values.isNotEmpty()) {
                                    mapOfScoresStates[Score.SCREEN_TIME_AGGREGATE]!!.value =
                                        ScoreState.ScreenTimeAggregateAvailable(
                                            Score.SCREEN_TIME_AGGREGATE,
                                            it as TimeSeries.ScreenTimeAggregateTimeSeries
                                        )
                                }
                            }
                    }

                    Metric.SLEEP_SUMMARY -> {
                        qa.getMetricSample(context, apiKey, Metric.SLEEP_SUMMARY, from = from).collect {
                            if (it.values.isNotEmpty()) {
                                mapOfScoresStates[Score.SLEEP_SUMMARY]!!.value =
                                    ScoreState.SleepSummaryAvailable(
                                        Score.SLEEP_SUMMARY,
                                        it as TimeSeries.SleepSummaryTimeTimeSeries
                                    )
                                mapOfScoresStates[Score.SLEEP_INTERRUPTIONS]!!.value =
                                    ScoreState.SleepSummaryAvailable(
                                        Score.SLEEP_INTERRUPTIONS,
                                        it
                                    )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun canDraw(context: Context): Boolean {
        return qa.canDraw(context)
    }

    fun canUsage(context: Context): Boolean {
        return qa.canUsage(context)
    }

}


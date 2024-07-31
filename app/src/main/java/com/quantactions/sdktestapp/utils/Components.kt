/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.charts.AdjustableBarPlot
import com.quantactions.sdktestapp.charts.Chart
import com.quantactions.sdktestapp.charts.CumulativeBarPlot
import com.quantactions.sdktestapp.charts.InterruptedBarPlot
import com.quantactions.sdktestapp.charts.MultiShadedLineChart
import com.quantactions.sdktestapp.charts.ShadedLineChart
import com.quantactions.sdktestapp.charts.prepareAndAggregateTimeSeries
import com.quantactions.sdktestapp.core_ui.metrics.DetailMetricCircularIndicator
import com.quantactions.sdktestapp.core_ui.metrics.MetricRow
import com.quantactions.sdktestapp.core_ui.theme.Brand
import com.quantactions.sdktestapp.core_ui.theme.TP


@Composable
fun Components(
    cognitiveFitness: ScoreState,
    cognitiveFitnessTrend: ScoreState,
    sleepQuality: ScoreState,
    sleepSummary: ScoreState,
    socialEngagement: ScoreState,
    screenTimeAggregate: ScoreState,
    actionSpeed: ScoreState,
    currentChart: Chart,
) {

    val thisCognitiveFitness: TimeSeries.DoubleTimeSeries =
        when (cognitiveFitness) {
            is ScoreState.ScoreAvailable -> cognitiveFitness.timeSeries
            else -> TimeSeries.DoubleTimeSeries()
        }


    val thisScreenTime: TimeSeries.ScreenTimeAggregateTimeSeries =
        when (screenTimeAggregate) {
            is ScoreState.ScreenTimeAggregateAvailable -> screenTimeAggregate.timeSeries
            else -> TimeSeries.ScreenTimeAggregateTimeSeries()
        }
    val thisEngagementScore: TimeSeries.DoubleTimeSeries =
        when (socialEngagement) {
            is ScoreState.ScoreAvailable -> socialEngagement.timeSeries
            else -> TimeSeries.DoubleTimeSeries()
        }

    val thisSleepSummary: TimeSeries.SleepSummaryTimeTimeSeries =
        when (sleepSummary) {
            is ScoreState.SleepSummaryAvailable -> sleepSummary.timeSeries
            else -> TimeSeries.SleepSummaryTimeTimeSeries()
        }
    val thisSleepScore: TimeSeries.DoubleTimeSeries = when (sleepQuality) {
        is ScoreState.ScoreAvailable -> sleepQuality.timeSeries
        else -> TimeSeries.DoubleTimeSeries()
    }

    val thisActionSpeed: TimeSeries.DoubleTimeSeries = when (actionSpeed) {
        is ScoreState.ScoreAvailable -> actionSpeed.timeSeries
        else -> TimeSeries.DoubleTimeSeries()
    }

    val listOfScoreStates: MutableList<ScoreState.ScoreAvailable> = mutableListOf()
    listOf(
        cognitiveFitness,
        sleepQuality,
        socialEngagement
    ).forEach { localScore ->
        when (localScore) {
            is ScoreState.ScoreAvailable -> {
                if (localScore.visibility) listOfScoreStates.add(localScore)
            }

            else -> {}
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ColoredTitle(title = "Cognitive Fitness", color = Score.COGNITIVE_FITNESS.colors.color)
        MetricRow(
            metricColor = Score.COGNITIVE_FITNESS.colors,
            metricDisplayName = "Cognitive Fitness",
            scoreState = cognitiveFitness,
            scoreTrendState = cognitiveFitnessTrend,
            true,
        )

        when (cognitiveFitness) {
            is ScoreState.ScoreAvailable -> {
                DetailMetricCircularIndicator(
                    prepareAndAggregateTimeSeries(
                        cognitiveFitness.timeSeries,
                        currentChart
                    ),
                    Score.COGNITIVE_FITNESS.colors
                )
            }
            else -> {
                DetailMetricCircularIndicator(
                    0.0,
                    Score.COGNITIVE_FITNESS.colors
                )
            }
        }


        ShadedLineChart(
            thisCognitiveFitness,
            Score.COGNITIVE_FITNESS,
            currentChart,
            true,
            BasicInfo(),
            100f,
            false,
        )

        ColoredTitle(title = "Screen Time", color = Score.SOCIAL_ENGAGEMENT.colors.color)

        CumulativeBarPlot(
            thisScreenTime,
            thisEngagementScore,
            Score.SOCIAL_ENGAGEMENT,
            currentChart
        )
        ColoredTitle(title = "Sleep Summary", color = Score.SLEEP_SCORE.colors.color)

        InterruptedBarPlot(
            thisSleepSummary,
            thisSleepScore,
            Score.SLEEP_SUMMARY,
            currentChart,
        )

        ColoredTitle(title = "Action Time", color = Score.COGNITIVE_FITNESS.colors.color)

        AdjustableBarPlot(
            timeSeries = thisActionSpeed,
            scoreTimeSeries = thisCognitiveFitness,
            score = Score.ACTION_SPEED,
            chartType = currentChart,
            maxValRequested = 0f,
            adaptiveRange = true
        )

        ColoredTitle(title = "Relations", color = Brand)

        MultiShadedLineChart(
            listOfScoreStates,
            currentChart,
            true,
        )

    }
}

@Composable
fun ColoredTitle(
    title: String,
    color: Color,
) {
    Divider(color = Color.Transparent, thickness = 8.dp)
    Column(
        modifier = Modifier
            .background(color)
            .padding(vertical = 8.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = TP.regular.h2,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
    Divider(color = Color.Transparent, thickness = 8.dp)
}

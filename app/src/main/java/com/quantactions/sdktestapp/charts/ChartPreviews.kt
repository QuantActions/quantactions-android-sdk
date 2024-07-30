/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.quantactions.sdktestapp.Score
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.model.ScreenTimeAggregate
import com.quantactions.sdk.data.model.SleepSummary
import java.time.ZonedDateTime
import kotlin.random.Random


@Composable
@Preview
fun NonCumulativeBarChartPreview() {
    val nValues = 14

    val data =
        List(nValues) { Random.nextDouble() * 100 }.map { if (Random.nextDouble() > 0.9) Double.NaN else it }

    val ciH = data.map { it + Random.nextDouble() * 20 }.map { it.coerceAtMost(100.0) }
    val ciL = data.map { it - Random.nextDouble() * 20 }.map { it.coerceAtLeast(10.0) }
    val conf = data.map { it / 10 }

    val actionTimes = List(nValues) { Random.nextDouble() * 1000 + 300 }.map { if (Random.nextDouble() > 0.9) Double.NaN else it }

    val times = List(nValues) {
        ZonedDateTime.now().minusDays(it.toLong())
    }

    val timeSeriesScore = TimeSeries.DoubleTimeSeries(
        data,
        times,
        ciH,
        ciL,
        conf
    )

    val timeSeries = TimeSeries.DoubleTimeSeries(
        actionTimes,
        times,
        actionTimes,
        actionTimes,
        conf
    )

    AdjustableBarPlot(
        timeSeries,
        timeSeriesScore,
        Score.ACTION_SPEED,
        Chart.WEEK,
        0f,
        adaptiveRange = true,
    )
}

@Composable
@Preview
fun CumulativeBarChartPreview() {
    val nValues = 14

    val data =
        List(nValues) { Random.nextDouble() * 100 }.map { if (Random.nextDouble() > 0.9) Double.NaN else it }

    val ciH = data.map { it + Random.nextDouble() * 20 }.map { it.coerceAtMost(100.0) }
    val ciL = data.map { it - Random.nextDouble() * 20 }.map { it.coerceAtLeast(10.0) }
    val conf = data.map { it / 10 }

    val screenTimeList = listOf(
        ScreenTimeAggregate(13710144.0, 6068210.0),
        ScreenTimeAggregate(14434447.0, 2579185.0),
        ScreenTimeAggregate(12553347.0, 1687614.0),
        ScreenTimeAggregate(14781458.0, 2873413.0),
        ScreenTimeAggregate(8723863.0, 2200630.0),
        ScreenTimeAggregate(9572376.0, 2009445.0),
        ScreenTimeAggregate(13454547.0, 1477079.0),
        ScreenTimeAggregate(16141213.0, 3985115.0),
        ScreenTimeAggregate(10338098.0, 2551525.0),
        ScreenTimeAggregate(19022426.0, 3803534.0),
        ScreenTimeAggregate(14675841.0, 5222025.0),
        ScreenTimeAggregate(11555162.0, 3199022.0),
        ScreenTimeAggregate(11942068.0, 4366519.0),
        ScreenTimeAggregate(13958753.0, 4128044.0),
    )

    val times = List(nValues) {
        ZonedDateTime.now().minusDays(it.toLong())
    }

    val timeSeriesScore = TimeSeries.DoubleTimeSeries(
        data,
        times,
        ciH,
        ciL,
        conf
        )

    val timeSeries = TimeSeries.ScreenTimeAggregateTimeSeries(
        screenTimeList,
        times,
        screenTimeList,
        screenTimeList,
        conf
    )

    CumulativeBarPlot(
        timeSeries,
        timeSeriesScore,
        listOf(),
        Score.SOCIAL_ENGAGEMENT,
        Chart.WEEK
    )
}

@Composable
@Preview
fun BarChartPreview() {
    val nValues = 13

    val data =
        List(nValues) { Random.nextDouble() * 100 }.map { if (Random.nextDouble() > 0.9) Double.NaN else it }

    val ciH = data.map { it + Random.nextDouble() * 20 }.map { it.coerceAtMost(100.0) }
    val ciL = data.map { it - Random.nextDouble() * 20 }.map { it.coerceAtLeast(10.0) }
    val conf = data.map { it / 10 }

    val sleepStartState = listOf(
        "2023-01-20T00:00:19.403+02:00[Europe/Zurich]",
        "2023-01-20T19:49:06.181+02:00[Europe/Zurich]",
        "2023-01-22T23:39:49.956+02:00[Europe/Zurich]",
        "2023-01-23T23:08:30.384+02:00[Europe/Zurich]",
        "2023-01-24T23:57:08.012+02:00[Europe/Zurich]",
        "2023-01-25T23:18:43.557+02:00[Europe/Zurich]",
        "2023-01-27T00:38:22.838+02:00[Europe/Zurich]",
        "2023-01-28T04:00:19.403+02:00[Europe/Zurich]",
        "2023-01-29T00:49:06.181+02:00[Europe/Zurich]",
        "2023-01-30T04:32:57.924+02:00[Europe/Zurich]",
        "2023-01-30T23:50:00.000+00:00[Europe/Zurich]",
        "2023-01-31T23:08:30.384+02:00[Europe/Zurich]",
        "2023-02-01T23:57:08.012+02:00[Europe/Zurich]",
    ).map { ZonedDateTime.parse(it) }


    val sleepStopState = listOf(
        "2023-01-20T09:30:01.285+02:00[Europe/Zurich]",
        "2023-01-21T10:37:24.592+02:00[Europe/Zurich]",
        "2023-01-23T08:00:00.399+02:00[Europe/Zurich]",
        "2023-01-24T06:45:58.541+02:00[Europe/Zurich]",
        "2023-01-25T07:30:01.197+02:00[Europe/Zurich]",
        "2023-01-26T09:03:16.146+02:00[Europe/Zurich]",
        "2023-01-27T08:00:00.857+02:00[Europe/Zurich]",
        "2023-01-28T09:30:01.285+02:00[Europe/Zurich]",
        "2023-01-29T07:37:24.592+02:00[Europe/Zurich]",
        "2023-01-30T08:00:00.506+02:00[Europe/Zurich]",
        "2023-01-31T08:00:00.000+00:00[Europe/Zurich]",
        "2023-02-01T07:45:58.541+02:00[Europe/Zurich]",
        "2023-02-02T07:30:01.197+02:00[Europe/Zurich]",
    ).map { ZonedDateTime.parse(it) }

    val timeSeries = TimeSeries.DoubleTimeSeries(
        data,
        sleepStopState,
        ciL,
        ciH,
        conf
    )

    val interruptionsTime = listOf(
        listOf(),
        listOf("2023-01-21T05:37:24.592+02:00[Europe/Zurich]").map { ZonedDateTime.parse(it) },
        listOf("2023-01-23T06:00:00.506+02:00[Europe/Zurich]").map { ZonedDateTime.parse(it) },
        listOf(),
        listOf(),
        listOf(
            "2023-01-27T02:03:16.146+02:00[Europe/Zurich]",
            "2023-01-27T03:03:16.146+02:00[Europe/Zurich]",
            "2023-01-27T05:03:16.146+02:00[Europe/Zurich]",
            "2023-01-27T07:03:16.146+02:00[Europe/Zurich]",
        ).map { ZonedDateTime.parse(it) },
        listOf(),
        listOf(
            "2023-01-29T05:30:01.285+02:00[Europe/Zurich]",
            "2023-01-29T07:30:01.285+02:00[Europe/Zurich]"
        ).map { ZonedDateTime.parse(it) },
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
    )

    val sleepSummaries = List(nValues) {
        SleepSummary(
            sleepStartState[it],
            sleepStopState[it],
            interruptionsTime[it],
            interruptionsTime[it]
        )
    }

    val sleepSummary = TimeSeries.SleepSummaryTimeTimeSeries(
        sleepSummaries,
        sleepStopState,
        sleepSummaries,
        sleepSummaries,
        conf
    )

    InterruptedBarPlot(
        sleepSummary,
        timeSeries,
        Score.SLEEP_SUMMARY,
        Chart.WEEK,
    )
}

@Composable
@Preview
fun SmallChartPreview() {
    val nValues = 14
//    val data = listOf(
//        10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0,
//        80.0, 90.0, 80.0, 70.0, 60.0, 50.0, 40.0
//    )
    val data =
        List(nValues) { Random.nextDouble() * 100 }.map { if (Random.nextDouble() > 0.99) Double.NaN else it }
    val timestamps = listOf(
        "2023-01-17T04:00:19.403+02:00[Europe/Zurich]",
        "2023-01-18T04:00:19.403+02:00[Europe/Zurich]",
        "2023-01-19T23:49:06.181+02:00[Europe/Zurich]",
        "2023-01-20T04:32:57.924+02:00[Europe/Zurich]",
        "2023-01-21T23:39:49.956+02:00[Europe/Zurich]",
        "2023-01-22T23:57:08.012+02:00[Europe/Zurich]",
        "2023-01-24T23:18:43.557+02:00[Europe/Zurich]",
        "2023-01-26T00:38:22.838+02:00[Europe/Zurich]",
        "2023-01-27T04:00:19.403+02:00[Europe/Zurich]",
        "2023-01-28T23:49:06.181+02:00[Europe/Zurich]",
        "2023-01-29T23:49:06.181+02:00[Europe/Zurich]",
        "2023-01-31T23:08:30.384+02:00[Europe/Zurich]",
        "2023-02-01T23:57:08.012+02:00[Europe/Zurich]",
        "2023-02-02T23:57:08.012+02:00[Europe/Zurich]",
    ).map { ZonedDateTime.parse(it) }
    val ciH = data.map { it + Random.nextDouble() * 20 }.map { it.coerceAtMost(100.0) }
    val ciL = data.map { it - Random.nextDouble() * 20 }.map { it.coerceAtLeast(10.0) }
    val conf = data.map { it / 10 }

    val timeSeries = TimeSeries.DoubleTimeSeries(
        data,
        timestamps,
        ciL,
        ciH,
        conf
    )

    ShadedLineChart(
        timeSeries,
        Score.COGNITIVE_FITNESS,
        Chart.WEEK,
        true,
        BasicInfo(),
        100f,
        false
    )
}

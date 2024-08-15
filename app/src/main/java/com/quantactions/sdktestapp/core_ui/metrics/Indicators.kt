/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp.core_ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.Trend
import com.quantactions.sdktestapp.MetricColor
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.ScoreTrend
import com.quantactions.sdktestapp.charts.Chart
import com.quantactions.sdktestapp.charts.calculateConnectionPointsForBezierCurve
import com.quantactions.sdktestapp.charts.calculatePointsForData
import com.quantactions.sdktestapp.charts.prepareAndAggregateSleepLength
import com.quantactions.sdktestapp.charts.prepareAndAggregateSocialScreenTime
import com.quantactions.sdktestapp.charts.prepareAndAggregateTimeSeries
import com.quantactions.sdktestapp.charts.prepareAndAggregateTrend
import com.quantactions.sdktestapp.core_ui.DotsFlashing
import com.quantactions.sdktestapp.core_ui.theme.Brand
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey05
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey10
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.utils.ScoreState
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Chart with the last 7 days of data within the circular indicator in the summary view of the
 * metrics
 * @param data list of values of the metric
 * @param colors instance of [MetricColor] for the line
 * */
@Composable
fun SmallPreviewChart(
    data: List<Double>,
    colors: MetricColor,
) {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val goldenSize = screenWidth.div(3) // 140.dp
    val ratio = goldenSize / 140.dp

    val size = 76.dp.times(ratio)

    Canvas(
        modifier = Modifier
            .clip(shape = CircleShape)
            .width(size)
            .height(size)
    ) {

        val width = size
        val height = size
        val bias = size.times(0.01f)

        val pointsList = calculatePointsForData(data, width.toPx(), height.toPx())
        val connPointsList = pointsList.map { calculateConnectionPointsForBezierCurve(it) }

        // main line with spline
        val paths = pointsList.zip(connPointsList).map { (points, connPoints) ->
            Path().apply {
                moveTo(points.first().x + bias.toPx(), points.first().y)
                for (i in 1 until points.size) {
                    cubicTo(
                        connPoints.first[i - 1].x, connPoints.first[i - 1].y,
                        connPoints.second[i - 1].x, connPoints.second[i - 1].y,
                        points[i].x, points[i].y
                    )
                }
            }
        }

        // bars
        val listBars = mutableListOf<Path>()

        pointsList.zip(connPointsList).map { (points, connPoints) ->
            for (i in 1 until points.size) {
                val path2 = Path().apply {
                    moveTo(points[i - 1].x + bias.toPx(), points[i - 1].y)

                    cubicTo(
                        connPoints.first[i - 1].x, connPoints.first[i - 1].y,
                        connPoints.second[i - 1].x, connPoints.second[i - 1].y,
                        points[i].x - bias.toPx(), points[i].y
                    )

                    lineTo(points[i].x - bias.toPx(), height.toPx())
                    lineTo(points[i - 1].x + bias.toPx(), height.toPx())
                    close()
                }
                listBars.add(path2)
            }
        }


        listBars.forEach {
            drawPath(
                path = it,
                brush = Brush.verticalGradient(listOf(colors.color, Color.White))
            )
        }

        // draw line plot
        paths.forEach { path ->
            drawPath(
                path = path,
                color = colors.color,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }


        if (pointsList.isNotEmpty()) {
            // draw dot at the end of the line plot
            drawCircle(
                color = colors.color,
                center = Offset(
                    pointsList.last().last().x - bias.toPx(),
                    pointsList.last().last().y
                ),
                radius = 3.dp.toPx()
            )
        }

        // round mask for the bars
        val path3 = Path().apply {
            arcTo(
                Rect(0f, 0f, width.toPx(), height.toPx()),
                0f, 180f, false
            )
            lineTo(0f, height.toPx())
            lineTo(width.toPx(), height.toPx())
            close()
        }
        drawPath(
            path = path3,
            color = colors.lightColor
        )
    }
}

/**
 * Simple preview of the circular indicator
 * */
@Preview
@Composable
fun PrevCircularIndicator() {
    val data = listOf(30.0, 21.0, 11.0, 9.0, 8.0, 12.0, 30.0, 36.0)
    MetricCircularIndicator(
        data, 36.0,
        Score.COGNITIVE_FITNESS.colors, includeIndicator = false,
    )
}

/**
 * Circular indicator in the summary view of the metrics with small plot inside
 * @param realData list of values of the metric
 * @param colors [MetricColor] for the line,
 * */
@Composable
fun MetricCircularIndicator(
    realData: List<Double>? = null,
    lastScore: Double? = null,
    colors: MetricColor,
    includeIndicator: Boolean = true,
) {

    val data = if (includeIndicator) {
        realData
    } else {
        listOf(20.0, 25.0, 27.0, 20.0, 15.0, 20.0, 25.0, 30.0)
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val size = screenWidth.div(3) // 140.dp
    val ratio = size / 140.dp

    val ringThickness = 5.dp.times(ratio)
    val ringRadius = 50.dp.times(ratio) + ringThickness / 2 // 50.dp
    val indicatorThickness = 11.dp.times(ratio)
    val indicatorRadius = 47.dp.times(ratio) + indicatorThickness / 2
    val indicatorOffset = 12.dp.times(sqrt(2.0).toFloat()).times(ratio)

    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier
                .size(size)
                .background(Color.White)
        ) {

            if (includeIndicator) {
                // Circle 1
                drawCircle(
                    color = colors.lightColor,
                    radius = size.toPx() / 2,
                    center = Offset(x = this.size.width / 2, y = this.size.height / 2)
                )

                // Circle 2
                drawCircle(
                    color = Color.White,
                    style = Stroke(width = ringThickness.toPx()),
                    radius = ringRadius.toPx(),
                    center = Offset(x = this.size.width / 2, y = this.size.height / 2)
                )
            } else {
                drawCircle(
                    color = colors.lightColor,
                    radius = ringRadius.toPx(),
                    center = Offset(x = this.size.width / 2, y = this.size.height / 2)
                )
            }
        }

        if (includeIndicator) {
            if (data.isNullOrEmpty()) {
                ProgressWithGradientRotation(
                    size,
                    colors.color,
                    colors.lightColor,
                    indicatorThickness,
                    indicatorRadius,
                    indicatorOffset
                )
            } else {
                ProgressWithGradient(
                    lastScore ?: 75.0,
                    size,
                    colors.color,
                    colors.lightColor,
                    indicatorThickness,
                    indicatorRadius,
                    indicatorOffset,
                )
            }
        }
        data?.let {
            SmallPreviewChart(data.takeLast(8), colors)
        }

    }
}

/**
 * Bigger Circular indicator for the detail metric page with text info on the value and the change
 * from last period
 * @param scoreValueToShow Double value to show in the center of the indicator
 * @param metricColor instance of MetricColor
 * */
@Composable
fun DetailMetricCircularIndicator(
    scoreValueToShow: Double,
    metricColor: MetricColor,
) {

    val size = 232.dp
    val ringThickness = 8.dp
    val ringRadius = 83.dp + ringThickness / 2
    val indicatorThickness = 18.dp
    val indicatorRadius = 78.dp + indicatorThickness / 2
    val indicatorOffset = 20.dp.times(sqrt(2.0).toFloat())

    val bucket = bucketValue(scoreValueToShow)

    Column {
        Box(
            modifier = Modifier
                .size(size)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(size)
                    .background(Color.White)
            ) {

                // Circle 1
                drawCircle(
                    color = metricColor.lightColor,
                    radius = size.toPx() / 2,
                    center = Offset(x = this.size.width / 2, y = this.size.height / 2)
                )

                // Circle 2
                drawCircle(
                    color = Color.White,
                    style = Stroke(width = ringThickness.toPx()),
                    radius = ringRadius.toPx(),
                    center = Offset(x = this.size.width / 2, y = this.size.height / 2)
                )
            }

            ProgressWithGradient(
                scoreValueToShow,
                size,
                metricColor.color,
                metricColor.lightColor,
                indicatorThickness,
                indicatorRadius,
                indicatorOffset
            )

            // The text
            Column(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!scoreValueToShow.isNaN()) {
                    Text(
                        text = AnnotatedString(
                            text = "${scoreValueToShow.roundToInt()}",
                            spanStyle = SpanStyle(
                                fontSize = 40.sp,
                                fontFamily = TP.regular.body1.fontFamily,
                                fontWeight = TP.regular.body1.fontWeight,
                                color = ColdGrey10
                            )
                        ).plus(
                            AnnotatedString(
                                text = "/100",
                                spanStyle = TP.regular.body1.toSpanStyle(ColdGrey10)
                            )
                        )
                    )
                } else {
                    Text(
                        text = AnnotatedString(
                            text = "No data",
                            spanStyle = TP.medium.body1.toSpanStyle(ColdGrey10)
                        )
                    )
                }

                Spacer(modifier = Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = bucket),
                        style = TP.medium.h5,
                        color = ColdGrey05
                    )

                }
            }
        }
    }
}

/**
 * Green Arrow to show when the score has increased
 * */
@Composable
fun UpArrow(color: Color = Score.SLEEP_SCORE.colors.color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(shape = CircleShape)
            .background(
                color
            )
    ) {
        Canvas(
            modifier = Modifier
                .size(16.dp)
        ) {

            val path = Path().apply {
                moveTo(5.dp.toPx(), 11.dp.toPx())
                lineTo(11.dp.toPx(), 5.dp.toPx())
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            val path2 = Path().apply {
                moveTo(11.dp.toPx(), 5.dp.toPx())
                lineTo(11.dp.toPx(), 10.dp.toPx())
            }
            drawPath(
                path = path2,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            val path3 = Path().apply {
                moveTo(11.dp.toPx(), 5.dp.toPx())
                lineTo(6.dp.toPx(), 5.dp.toPx())
            }
            drawPath(
                path = path3,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
@Preview
fun PreviewMetricRowDetails() {

    val values = List(28) { Random.nextInt(10) * 3600 * 1000.0 + Random.nextInt(60) * 60 * 1000 }
    val emptyTimeSeries = TimeSeries.DoubleTimeSeries()
    emptyTimeSeries.fillMissingDays(28, inplace = true)

    val timeSeries =
        TimeSeries.DoubleTimeSeries(values, emptyTimeSeries.timestamps, values, values, values)

    val scoreState = ScoreState.ScoreAvailable(Score.SLEEP_SUMMARY, timeSeries, true)
    val scoreTrendState = ScoreState.TrendAvailable(
        ScoreTrend.SLEEP_LENGTH,
        TimeSeries.TrendTimeSeries().getRandomSample(28), true
    )

    MetricRow(
        metricColor = Score.SLEEP_SUMMARY.colors,
        metricDisplayName = stringResource(R.string.sleep_duration),
        scoreState = scoreState,
        scoreTrendState = scoreTrendState,
        false,
    )
}

@Composable
@Preview
fun PreviewMetricRowDetailsAction() {

    val values = List(28) { Random.nextDouble() * 500 }
    val emptyTimeSeries = TimeSeries.DoubleTimeSeries()
    emptyTimeSeries.fillMissingDays(28, inplace = true)

    val timeSeries =
        TimeSeries.DoubleTimeSeries(values, emptyTimeSeries.timestamps, values, values, values)
    val scoreState = ScoreState.ScoreAvailable(Score.COGNITIVE_FITNESS, timeSeries, true)

    val scoreTrendState = ScoreState.TrendAvailable(
        ScoreTrend.COGNITIVE_FITNESS,
        TimeSeries.TrendTimeSeries().getRandomSample(28), true
    )

    MetricRow(
        metricColor = Score.COGNITIVE_FITNESS.colors,
        metricDisplayName = stringResource(R.string.action_time),
        scoreState = scoreState,
        scoreTrendState = scoreTrendState,
        false,
    )
}


@Composable
fun millisecondsToHourMinutes(preAmount: Long, includeMinus: Boolean = true): String {
    val prefix = if (preAmount > 0 || !includeMinus) "" else "- "
    val amount = abs(preAmount)

    val hours = (amount / 3600 / 1000).toInt()
    val minutes = ((amount - hours * 3600 * 1000) / 60 / 1000).toInt()
    return if (hours == 0) {
        prefix + stringResource(R.string.sleep_length_minutes, minutes)
    } else {
        prefix + stringResource(R.string.sleep_length, hours, minutes)
    }
}

fun bucketValue(value: Double): Int {
    return when (value.toInt()) {
        in 0..32 -> R.string.low
        in 33..66 -> R.string.medium
        in 67..100 -> R.string.high
        else -> R.string.lifestyle_empty
    }
}

/**
 * Metric Row in the summary screen with all the metrics
 * @param scoreState of the last 7 days to render the plot
 * */
@Composable
fun MetricRow(
    metricColor: MetricColor = Score.COGNITIVE_FITNESS.colors,
    metricDisplayName: String,
    scoreState: ScoreState = ScoreState.ScoreAvailable(
        Score.COGNITIVE_FITNESS,
        TimeSeries.DoubleTimeSeries().getRandomSample(28)
    ),
    scoreTrendState: ScoreState = ScoreState.TrendAvailable(
        ScoreTrend.COGNITIVE_FITNESS,
        TimeSeries.TrendTimeSeries().getRandomSample(28)
    ),
    isScore: Boolean = true,
) {

    var gain = "--"
    var bucket = "--"

    when (scoreState) {
        is ScoreState.ScoreAvailable -> {
            val valueToShow = prepareAndAggregateTimeSeries(scoreState.timeSeries, Chart.WEEK)
            if (!valueToShow.isNaN()) {
                bucket = if (isScore) {
                    stringResource(id = bucketValue(valueToShow))
                } else {
                    stringResource(id = R.string.action_time_ms, valueToShow.roundToInt())
                }
            }
        }

        is ScoreState.SleepSummaryAvailable -> {
            val valueToShow = prepareAndAggregateSleepLength(scoreState.timeSeries, Chart.WEEK)
            if (!valueToShow.isNaN()) {
                    millisecondsToHourMinutes(valueToShow.toLong())

            }
        }

        is ScoreState.ScreenTimeAggregateAvailable -> {
            val valueToShow = prepareAndAggregateSocialScreenTime(scoreState.timeSeries, Chart.WEEK)
            if (!valueToShow.isNaN()) {
                bucket = millisecondsToHourMinutes(valueToShow.toLong())

            }
        }

        else -> {}
    }

    when (scoreTrendState) {
        is ScoreState.TrendAvailable -> {
            val value = prepareAndAggregateTrend(
                scoreTrendState.timeSeries,
                Chart.WEEK,
                ignoreSignificance = false,
                dropna = true
            )

            if (!value.isNaN()) {
                gain = if (isScore) when (value.toInt()) {
                    in Int.MIN_VALUE..-1 -> if (scoreTrendState.scoreOrTrend.id == Trend.ACTION_SPEED.id) stringResource(R.string.uptrend) else stringResource(R.string.downtrend)
                    0 -> stringResource(R.string.stable, "", "")
                    else -> if (scoreTrendState.scoreOrTrend.id == Trend.ACTION_SPEED.id) stringResource(R.string.downtrend) else stringResource(R.string.uptrend)
                } else stringResource(R.string.stable, "", "")
            }
        }
        else -> {}
    }

    Card(shape = RoundedCornerShape(8.dp),
        elevation = 10.dp,
        modifier = Modifier
            .padding(8.dp)) {
        Box {
            Row(
                modifier = Modifier
                    .padding(9.dp)
                    .fillMaxWidth()
                    .background(color = Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (scoreState) {
                    is ScoreState.ScoreAvailable -> {
                        val valueToShow =
                            prepareAndAggregateTimeSeries(scoreState.timeSeries, Chart.WEEK)
                        val dataToShow =
                            scoreState.timeSeries.fillMissingDays(14).takeLast(14).values
                        MetricCircularIndicator(dataToShow, valueToShow, metricColor, isScore)
                    }

                    is ScoreState.SleepSummaryAvailable -> {
                        val valueToShow =
                            prepareAndAggregateSleepLength(scoreState.timeSeries, Chart.WEEK)
                        val dataToShow = scoreState.timeSeries.fillMissingDays(14)
                            .takeLast(14).values.map {
                            ChronoUnit.MILLIS.between(
                                it.sleepStart,
                                it.sleepEnd
                            ).toDouble()
                        }
                        MetricCircularIndicator(dataToShow, valueToShow, metricColor, isScore)
                    }

                    is ScoreState.ScreenTimeAggregateAvailable -> {
                        val valueToShow =
                            prepareAndAggregateSocialScreenTime(scoreState.timeSeries, Chart.WEEK)
                        val dataToShow = scoreState.timeSeries.fillMissingDays(14)
                            .takeLast(14).values.map { it.socialScreenTime }
                        MetricCircularIndicator(dataToShow, valueToShow, metricColor, isScore)
                    }

                    else -> MetricCircularIndicator(null, null, metricColor, isScore)
                }

                Spacer(Modifier.width(24.dp))
                Column( // column with score name and trends
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = metricDisplayName,
                        color = ColdGrey10,
                        style = TP.regular.h6,
                    )
                    when (scoreState) {
                        is ScoreState.ScoreLoading -> {
                            BoxWithFlashingDots(TP.medium.body1.fontSize, metricColor.color)
                        }
                        else -> {
                            Row (Modifier.blur(if (isScore) 0.dp else 5.dp, BlurredEdgeTreatment.Unbounded)) {
                                Text(
                                    text = bucket,
                                    style = TP.medium.body1,
                                    color = metricColor.color
                                )
                                Text(
                                    text = " – ",
                                    style = TP.medium.body1,
                                    color = metricColor.color
                                )
                                Text(
                                    text = gain,
                                    style = TP.medium.body1,
                                    color = metricColor.color
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isScore) stringResource(R.string.score_14_days) else stringResource(
                            R.string.score_14_avg
                        ),
                        color = ColdGrey05,
                        style = TP.regular.body2
                    )
                }
            }
        }
    }
}

@Composable
fun BoxWithFlashingDots(textUnit: TextUnit, color: Color = Brand) {

    val height = with(LocalDensity.current) {
        textUnit.toDp()
    }

    Box(Modifier.height(height), contentAlignment = Alignment.CenterStart) {
        DotsFlashing(8.dp, color)
    }
}


@Preview
@Composable
fun TestGrid() {
    Row {
        Column(Modifier) {
            Text(
                text = stringResource(R.string.today_score),
                color = ColdGrey05,
                style = TP.regular.body2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.change_24h),
                color = ColdGrey05,
                style = TP.regular.body2
            )
        }
        Spacer(Modifier.width(15.dp))
        Column {
            Text(
                text = AnnotatedString(
                    text = "77",
                    spanStyle = TP.medium.body1.toSpanStyle(ColdGrey10)
                ).plus(
                    AnnotatedString(
                        text = "/100",
                        spanStyle = TP.medium.overline.toSpanStyle(ColdGrey10)
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+12",
                    color = ColdGrey10,
                    style = TP.medium.body1
                )
                Spacer(Modifier.size(6.dp))
                UpArrow()
            }
        }
    }
}

fun TextStyle.toSpanStyle(color: Color): SpanStyle {
    return SpanStyle(
        fontSize = this.fontSize,
        fontWeight = this.fontWeight,
        fontStyle = this.fontStyle,
        letterSpacing = this.letterSpacing,
        baselineShift = this.baselineShift,
        fontFamily = this.fontFamily,
        color = color
    )
}

@Composable
@Preview
private fun SweepGradientExample() {
    val colorStops = listOf(
        0.01f to Color(0x8C1339FF),
        0.25f to Color(0x8CFF13A1),
//        0.99f to Color(0x8C1339FF),
    ).toTypedArray()

    val brush = Brush.sweepGradient(
        colorStops = colorStops,
    )
    Box(
        modifier = Modifier
            .size(width = 161.dp, height = 97.dp)
            .background(brush)
    )
}



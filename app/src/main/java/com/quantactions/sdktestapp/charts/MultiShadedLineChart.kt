/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp.charts

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey01
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey04
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey07
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.utils.ScoreState
import com.quantactions.sdktestapp.utils.StringFormatter
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/**
 * Chart with the last 7 days of data within the circular indicator in the summary view of the
 * metrics
 * */
@Composable
fun MultiShadedLineChart(
    scoreStates: List<ScoreState.ScoreAvailable>,
    chartType: Chart,
    showConfidence: Boolean,
) {

    // Here I need to custom for number fo values
    val nVerticalLines = chartType.numValues
    val horizontalBias: Dp
    val times: TimeSeries<Double>

    val valuesToPlot: List<TimeSeries<Double>>

    val xLabelFormatter = DateTimeFormatter.ofPattern(StringFormatter.ChartXWeek.pattern)
    val textMeasure = rememberTextMeasurer()
    val xLabelsText: List<AnnotatedString>

    when (chartType) {
        Chart.WEEK -> {
            valuesToPlot = scoreStates.map {
                it.timeSeries.fillMissingDays(Chart.WEEK.numValues)
                    .takeLast(Chart.WEEK.numValues)
            }
            times = TimeSeries.DoubleTimeSeries().fillMissingDays(Chart.WEEK.numValues)
                .takeLast(Chart.WEEK.numValues)

            horizontalBias = 8.dp
            // x labels
            xLabelsText = times.timestamps.map { timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = if (timestamp.dayOfWeek in listOf(
                                    DayOfWeek.SATURDAY,
                                    DayOfWeek.SUNDAY
                                )
                            ) ColdGrey07 else ColdGrey04,
                            fontSize = TP.regular.overline.fontSize,
                            fontStyle = TP.regular.overline.fontStyle,
                            fontWeight = TP.regular.overline.fontWeight
                        )
                    ) {
                        append(xLabelFormatter.format(timestamp).substring(0, 2))
                    }
                }
            }
        }

        Chart.MONTH -> {
            valuesToPlot = scoreStates.map {
                it.timeSeries.fillMissingDays(Chart.MONTH.numValues * 7)
                    .extractWeeklyAverages().takeLast(Chart.MONTH.numValues)
            }

            times = TimeSeries.DoubleTimeSeries().fillMissingDays(Chart.MONTH.numValues * 7)
                .extractWeeklyAverages().takeLast(Chart.MONTH.numValues)


            horizontalBias = 20.dp
            // x labels
            xLabelsText = times.timestamps.map { timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = ColdGrey04,
                            fontSize = TP.regular.overline.fontSize,
                            fontStyle = TP.regular.overline.fontStyle,
                            fontWeight = TP.regular.overline.fontWeight
                        )
                    ) {
                        append(
                            stringResource(
                                R.string.week,
                                timestamp.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                            ))
                    }
                }
            }

        }

        Chart.YEAR -> {
            val monthlyAverages =
                scoreStates.map { it.timeSeries.fillMissingDays(366).extractMonthlyAverages() }
            valuesToPlot = monthlyAverages.map { it.takeLast(Chart.YEAR.numValues) }
            times = TimeSeries.DoubleTimeSeries().fillMissingDays(366).extractMonthlyAverages()
                .takeLast(Chart.YEAR.numValues)
            horizontalBias = 8.dp
            // x labels
            xLabelsText = times.timestamps.map { timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = ColdGrey04,
                            fontSize = TP.regular.overline.fontSize,
                            fontStyle = TP.regular.overline.fontStyle,
                            fontWeight = TP.regular.overline.fontWeight
                        )
                    ) {
                        append(
                            DateTimeFormatter.ofPattern(StringFormatter.ChartXYear.pattern)
                                .format(timestamp)
                        )
                    }
                }
            }

        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val marginLeft = 48.dp
    val marginRight = 22.dp
    val marginTopPlot = 8.dp
    val width = screenWidth - marginRight - marginLeft
    val height = 250.dp
    val nHorizontalLines = 11
    val lineStroke = 1.dp

    val stepVerticalLines =
        (width - horizontalBias.times(2) - lineStroke.times(nVerticalLines - 1)).div(nVerticalLines - 1)
    val stepHorizontalLines =
        (height - lineStroke.times(nHorizontalLines - 1)).div(nHorizontalLines - 1)
    // This is unbelievably complicated, but we need allow for multiple scores and the possibility
    // that each score has holes in it
    // It's 3 list:
    //  - level 1: the list of scores
    //  - level 2: on entry for each uninterrupted time series (interrupted by NaNs)
    //  - level 3: uninterrupted score line
    val pointsList: List<List<List<PointF>>>
    val pointsListCircles: List<List<List<PointF>>>
    val pointsListCIH: List<List<List<PointF>>>
    val pointsListCIL: List<List<List<PointF>>>
    val connPointsList: List<List<Pair<List<PointF>, List<PointF>>>>
    val connPointsListCIH: List<List<Pair<List<PointF>, List<PointF>>>>
    val connPointsListCIL: List<List<Pair<List<PointF>, List<PointF>>>>

    val pathHorizontal: Path
    val pathVertical: Path
    val paths: List<Path>
    val pathsH: List<List<Path>>

    with(LocalDensity.current) {
        // GRID
        pathHorizontal = Path().apply {
            moveTo(0f, 0f)
            for (step in 0 until nHorizontalLines) {
                lineTo(width.toPx(), (stepHorizontalLines + lineStroke).times(step).toPx())
                moveTo(0f, (stepHorizontalLines + lineStroke).times(step + 1).toPx())
            }
        }
        pathVertical = Path().apply {
            moveTo(horizontalBias.toPx(), 0f)
            for (step in 0 until nVerticalLines) {
                lineTo(
                    (stepVerticalLines + lineStroke).times(step).toPx() + horizontalBias.toPx(),
                    height.toPx()
                )
                moveTo(
                    (stepVerticalLines + lineStroke).times(step + 1).toPx() + horizontalBias.toPx(),
                    0f
                )
            }
        }


        // MAIN LINE
        pointsList = valuesToPlot.map {
            calculatePointsForDataGeneral(
                it.values,
                width.toPx(),
                height.toPx(),
                horizontalBias = horizontalBias.toPx(),
                maxVal = 100f,
                minVal = 0f
            )
        }
        pointsListCircles = valuesToPlot.map {
            calculatePointsForDataGeneral(
                it.values,
                width.toPx(),
                height.toPx(),
                horizontalBias = horizontalBias.toPx(),
                maxVal = 100f,
                minVal = 0f
            )
        }
        connPointsList = pointsList.map { thisPointsList ->
            thisPointsList.map {
                calculateConnectionPointsForBezierCurve(it)
            }
        }

        paths = pointsList.zip(connPointsList).map { (thisPointsList, thisConnPointsList) ->
            Path().apply {
                thisPointsList.zip(thisConnPointsList).map { (points, connPoints) ->
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        cubicTo(
                            connPoints.first[i - 1].x, connPoints.first[i - 1].y,
                            connPoints.second[i - 1].x, connPoints.second[i - 1].y,
                            points[i].x, points[i].y
                        )
                    }
                }
            }
        }

        // CONFIDENCE SHADING
        pointsListCIH = valuesToPlot.map {
            calculatePointsForDataGeneral(
                it.confidenceIntervalHigh,
                width.toPx(),
                height.toPx(),
                horizontalBias = horizontalBias.toPx(),
                maxVal = 100f,
                minVal = 0f
            )
        }
        pointsListCIL = valuesToPlot.map {
            calculatePointsForDataGeneral(
                it.confidenceIntervalLow,
                width.toPx(),
                height.toPx(),
                reverse = true,
                horizontalBias = horizontalBias.toPx(),
                maxVal = 100f,
                minVal = 0f
            )
        }

        connPointsListCIH = pointsListCIH.map { thisPointsListCIH ->
            thisPointsListCIH.map {
                calculateConnectionPointsForBezierCurve(it)
            }
        }
        connPointsListCIL = pointsListCIL.map { thisPointsListCIL ->
            thisPointsListCIL.map {
                calculateConnectionPointsForBezierCurve(it)
            }
        }
        pathsH = List(pointsListCIH.size) { thisIndex ->
            val thisPointsListCIH = pointsListCIH[thisIndex]
            val thisConnPointsListCIH = connPointsListCIH[thisIndex]
            val thisPointsListCIL = pointsListCIL[thisIndex]
            val thisConnPointsListCIL = connPointsListCIL[thisIndex]
            List(thisPointsListCIH.size) { segmentIndex ->
                Path().apply {
                    var points = thisPointsListCIH[segmentIndex]
                    var connPoints = thisConnPointsListCIH[segmentIndex]
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        cubicTo(
                            connPoints.first[i - 1].x, connPoints.first[i - 1].y,
                            connPoints.second[i - 1].x, connPoints.second[i - 1].y,
                            points[i].x, points[i].y
                        )
                    }

                    points = thisPointsListCIL[thisPointsListCIH.size - 1 - segmentIndex]
                    connPoints = thisConnPointsListCIL[thisPointsListCIH.size - 1 - segmentIndex]
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        cubicTo(
                            connPoints.first[i - 1].x, connPoints.first[i - 1].y,
                            connPoints.second[i - 1].x, connPoints.second[i - 1].y,
                            points[i].x, points[i].y
                        )
                    }
                }
            }
        }
    }

    val xLabelsTextLayoutResults = xLabelsText.map { textMeasure.measure(text = it) }
    val xLabelsTextSizes = xLabelsTextLayoutResults.map { it.size }

    val spaceForText = 48.dp

    Column(Modifier.padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .height(height + marginTopPlot.times(2))
                    .padding(start = 20.dp, end = 6.dp)
            ) {
                listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10, 0).forEach {
                    Text(
                        text = "$it",
                        style = TP.regular.body2,
                        color = ColdGrey04,
                        textAlign = TextAlign.Left
                    )
                }
            }
            Column(
                Modifier
                    .padding(top = marginTopPlot)
            ) {
                Box {
                    Canvas(
                        modifier = Modifier
                            .width(width)
                            .height(height)
                            .background(Color.White)
                    ) {

                        // GRID
                        drawPath(
                            path = pathHorizontal, color = ColdGrey01,
                            style = Stroke(width = lineStroke.toPx(), cap = StrokeCap.Square)
                        )
                        drawPath(
                            path = pathVertical, color = ColdGrey01,
                            style = Stroke(width = lineStroke.toPx(), cap = StrokeCap.Square)
                        )
                        // end GRID

                        scoreStates.forEachIndexed { scoreIndex, scoreState ->

                            // SHADING confidence
                            if (showConfidence) {
                                pathsH[scoreIndex].forEach { pathH ->
                                    drawPath(
                                        path = pathH,
                                        color = scoreState.scoreOrTrend.colors.color.copy(0.1f),
                                    )
                                }

                            }
                            // end SHADING

                            // main LINE
                            drawPath(
                                path = paths[scoreIndex],
                                color = scoreState.scoreOrTrend.colors.color,
                                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // DOTs

                            pointsListCircles[scoreIndex].forEachIndexed { index, singleList ->
                                singleList.forEachIndexed { i, it ->
                                    if (it.x > 0) {
                                        if (!(index == pointsListCircles[scoreIndex].lastIndex &&
                                                    i == singleList.lastIndex &&
                                                    chartType in listOf(
                                                Chart.MONTH,
                                                Chart.YEAR
                                            ))
                                        ) {
                                            drawCircle(
                                                color = scoreState.scoreOrTrend.colors.color,
                                                center = Offset(it.x, it.y),
                                                radius = 5.dp.toPx()
                                            )
                                        }
                                    }
                                }
                            }

                        }


                    }
                }
                // Canvas for the text and the journal bubbles
                Canvas(
                    modifier = Modifier
                        .width(width)
                        .height(spaceForText)
                        .background(Color.Transparent)
                ) {

                    xLabelsText.forEachIndexed { step, annotatedString ->
                        val xP = (stepVerticalLines + lineStroke).times(step)
                            .toPx() + horizontalBias.toPx()
                        // x label
                        drawText(
                            textMeasurer = textMeasure,
                            text = annotatedString,
                            topLeft = Offset(
                                (xP - xLabelsTextSizes[step].width / 2f).coerceAtMost(width.toPx() - xLabelsTextSizes[step].width),
                                16.dp.toPx()
                            )
                        )


                    }
                }
            }
        }
    }
}
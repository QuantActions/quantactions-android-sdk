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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.Score
import com.quantactions.sdk.*
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.core_ui.metrics.toSpanStyle
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey01
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey04
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey07
import com.quantactions.sdktestapp.core_ui.theme.MetricViolet
import com.quantactions.sdktestapp.utils.StringFormatter
import org.nield.kotlinstatistics.percentile
import timber.log.Timber
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields


/**
 * Chart with the last 7 days of data within the circular indicator in the summary view of the
 * metrics
 * */
@Composable
fun AdjustableBarPlot(
    timeSeries: TimeSeries.DoubleTimeSeries,
    scoreTimeSeries: TimeSeries.DoubleTimeSeries,
    score: Score,
    chartType: Chart,
    maxValRequested: Float,
    adaptiveRange: Boolean,
) {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    var selectedPoint by remember { mutableIntStateOf(-1) }

    val marginLeft = 36.dp
    val marginRight = 22.dp
    val marginTopPlot = 8.dp


    val nVerticalLines = chartType.numValues
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
    val width = screenWidth - marginRight - marginLeft
    val lineStroke = 1.dp
    val barWidth: Dp

    var valuesToPlot by remember { mutableStateOf(TimeSeries.DoubleTimeSeries()) }
    var scoreValuesToPlot by remember { mutableStateOf(TimeSeries.DoubleTimeSeries()) }
    val xLabelsText: List<AnnotatedString>
    val times: TimeSeries.DoubleTimeSeries


    when (chartType) {
        Chart.WEEK -> {

            valuesToPlot =
                timeSeries.fillMissingDays(Chart.WEEK.numValues)
                    .takeLast(Chart.WEEK.numValues)

            scoreValuesToPlot =
                scoreTimeSeries.fillMissingDays(Chart.WEEK.numValues)
                    .takeLast(Chart.WEEK.numValues)

            times = TimeSeries.DoubleTimeSeries().fillMissingDays(Chart.WEEK.numValues)
                .takeLast(Chart.WEEK.numValues)

            barWidth = 8.dp

            // x labels
            xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = if (i == selectedPoint) score.colors.color else {
                                if (timestamp.dayOfWeek in listOf(
                                        DayOfWeek.SATURDAY,
                                        DayOfWeek.SUNDAY
                                    )
                                ) ColdGrey07 else ColdGrey04
                            },
                            fontSize = TP.regular.overline.fontSize,
                            fontStyle = TP.regular.overline.fontStyle,
                            fontWeight = TP.regular.overline.fontWeight
                        )
                    ) {
                        append(formatter.format(timestamp).substring(0, 2))
                    }
                }
            }
        }

        Chart.MONTH -> {
            valuesToPlot =
                timeSeries.fillMissingDays(Chart.MONTH.numValues * 7)
                    .extractWeeklyAverages().takeLast(Chart.MONTH.numValues)


            scoreValuesToPlot =
                scoreTimeSeries.fillMissingDays(Chart.MONTH.numValues * 7)
                    .extractWeeklyAverages()
                    .takeLast(Chart.MONTH.numValues)


            times = TimeSeries.DoubleTimeSeries().fillMissingDays(Chart.MONTH.numValues * 7)
                .extractWeeklyAverages().takeLast(Chart.MONTH.numValues)


            barWidth = 20.dp
            // x labels
            xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = if (i == selectedPoint) score.colors.color else ColdGrey04,
                            fontSize = TP.regular.overline.fontSize,
                            fontStyle = TP.regular.overline.fontStyle,
                            fontWeight = TP.regular.overline.fontWeight
                        )
                    ) {
                        append(
                            String.format(
                                stringResource(id = R.string.week),
                                timestamp.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                            )
                        )
                    }
                }
            }
        }

        Chart.YEAR -> {
            // here I need to massage and extract averages over months
            valuesToPlot =
                timeSeries.fillMissingDays(366).extractMonthlyAverages()
                    .takeLast(Chart.YEAR.numValues)

            scoreValuesToPlot =
                scoreTimeSeries.fillMissingDays(366)
                    .extractMonthlyAverages()
                    .takeLast(Chart.YEAR.numValues)

            times = TimeSeries.DoubleTimeSeries().fillMissingDays(366).extractMonthlyAverages()
                .takeLast(Chart.YEAR.numValues)

            barWidth = 10.dp
            // x labels
            xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = TP.regular.overline.toSpanStyle(if (i == selectedPoint) score.colors.color else ColdGrey04)
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


    var prevSelectedPoint by remember { mutableIntStateOf(-1) }
    var showNoDataInfo by remember { mutableStateOf(false) }
    var lastXValue by remember { mutableFloatStateOf(-1.0f) }

    val scoreBubbleWidth = 8.dp
    val scoreBubbleHeight = 6.dp
    val spaceForText = 70.dp + 20.dp

    val flatPointsList: List<PointF>
    var topPointsList by remember { mutableStateOf(listOf(listOf<PointF>())) }
    val pathHorizontal: Path
    val pathVertical: Path
    val pathsTotal = mutableListOf<Path>()
    val pathsSocial = mutableListOf<Path>()
    val pathsHighlightBottom = mutableListOf<Path>()
    val linesAvg: Path
    val avgLine: List<PointF>

    val textMeasure = rememberTextMeasurer()

    val legendText = when (chartType) {
        Chart.WEEK -> {
            R.string.legend_action_time_14days
        }

        Chart.MONTH -> {
            R.string.legend_action_time_5weeks
        }

        Chart.YEAR -> {
            R.string.legend_action_time_12months
        }
    }

    var minVal = 0f
    var maxVal = maxValRequested

    if (adaptiveRange) {
        maxVal = timeSeries.values.percentile(98.0).toFloat()
        maxVal -= maxVal % 50
        maxVal += 50
        minVal = timeSeries.values.percentile(5.0).toFloat() * .95f
        minVal -= minVal % 50
    }

    val nHorizontalLines = ((maxVal - minVal) / 50 + 2).toInt()


    val height = 20.dp.times(nHorizontalLines)
    val stepVerticalLines =
        (width - barWidth.times(2) - lineStroke.times(nVerticalLines - 1)).div(nVerticalLines - 1)
    val stepHorizontalLines =
        (height - lineStroke.times(nHorizontalLines - 1)).div(nHorizontalLines - 1)

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
            moveTo(barWidth.toPx(), 0f)
            for (step in 0 until nVerticalLines) {
                lineTo(
                    (stepVerticalLines + lineStroke).times(step).toPx() + barWidth.toPx(),
                    height.toPx()
                )
                moveTo(
                    (stepVerticalLines + lineStroke).times(step + 1).toPx() + barWidth.toPx(),
                    0f
                )
            }
        }

        // MAIN LINE
        flatPointsList = calculatePointsForDataGeneralFlat(
            times.values,
            width.toPx(),
            height.toPx(),
            horizontalBias = barWidth.toPx(),
            maxVal = maxVal,
            minVal = minVal
        )
        lastXValue = flatPointsList.last().x

        topPointsList = calculatePointsForDataGeneral(
            valuesToPlot.values.map { if (it.isNaN()) minVal.toDouble() else it },
            width.toPx(),
            height.toPx(),
            maxVal = maxVal - minVal + 50,
            minVal = minVal,
            horizontalBias = barWidth.toPx(),
            includeOutOfChartLeft = false,
        )

        topPointsList.forEach { theseTops ->
            theseTops.forEach { top ->
                pathsSocial.add(
                    Path().apply {
                        moveTo(top.x - barWidth.toPx(), top.y.coerceAtLeast(0f))
                        lineTo(top.x + barWidth.toPx(), top.y.coerceAtLeast(0f))
                        lineTo(top.x + barWidth.toPx(), height.toPx())
                        lineTo(top.x - barWidth.toPx(), height.toPx())
                        close()
                    }
                )
                pathsHighlightBottom.add(Path().apply {
                    moveTo(top.x - barWidth.toPx(), 0f)
                    lineTo(top.x + barWidth.toPx(), 0f)
                    lineTo(top.x + barWidth.toPx(), spaceForText.toPx())
                    lineTo(top.x - barWidth.toPx(), spaceForText.toPx())
                    close()
                })
            }
        }

        // avg line
        avgLine = calculatePointsForDataGeneral(
            listOf(valuesToPlot.values.filter { !it.isNaN() }.average()),
            width.toPx(),
            height.toPx(),
            maxVal = maxVal - minVal + 50,
            minVal = minVal,
            horizontalBias = barWidth.toPx(),
            includeOutOfChartLeft = false,
        ).flatten()

        linesAvg = Path().apply {
            moveTo(0f, if (avgLine.isEmpty()) 0f else avgLine[0].y)
            lineTo(width.toPx(), if (avgLine.isEmpty()) 0f else avgLine[0].y)
        }

    }

    val scoreTexts = scoreValuesToPlot.values.map { value ->
        buildAnnotatedString {
            withStyle(
                style = TP.regular.subtitle1.toSpanStyle(Color.White)
            ) {
                append("${value.toInt()}")
            }
        }
    }


    val noDataText = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(stringResource(R.string.no_data))
        }
    }


    val selectedValue =
        valuesToPlot.values[selectedPoint.coerceAtLeast(0)]

    val textFirstLine = buildAnnotatedString {
        withStyle(
            style = TP.medium.h1.toSpanStyle(Color.White)
        ) {
            append(stringResource(R.string.action_time_ms, selectedValue.toInt()))
        }
    }


    val textFirstLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textFirstLine)
    val textFirstLineSize = textFirstLineLayoutResult.size

    val noDataLayoutResult: TextLayoutResult = textMeasure.measure(text = noDataText)
    val noDataLineSize = noDataLayoutResult.size

    val xLabelsTextLayoutResults = xLabelsText.map { textMeasure.measure(text = it) }
    val xLabelsTextSizes = xLabelsTextLayoutResults.map { it.size }

    val scoreTextLayoutResults = scoreTexts.map { textMeasure.measure(text = it) }
    val scoreTextSizes = scoreTextLayoutResults.map { it.size }

    fun onSelectColumn(
        tapOffset: Offset,
    ) {
        prevSelectedPoint = selectedPoint
        selectedPoint =
            findClosest(
                topPointsList
                    .flatten()
                    .map { it.x }, tapOffset.x
            ) ?: -1

        Timber.d("Offset is $tapOffset")
        Timber.d("${
            topPointsList
                .flatten()
                .map { it.x }
        }"
        )

        if (prevSelectedPoint == selectedPoint) selectedPoint = -1
        Timber.d("Select point si $selectedPoint")
        if (selectedPoint >= 0) {

            Timber.d("And values ${valuesToPlot.values}")
            showNoDataInfo = valuesToPlot.values[selectedPoint].isNaN()
        }
    }

    Column {
        Row(Modifier.padding(start = 8.dp)) {
            Column {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .height(height + marginTopPlot.times(2))
                        .width(marginLeft)
                ) {
                    List(nHorizontalLines) {
                        Text(
                            text = if (it == 0) "ms" else "${(maxVal + 50 - 50 * it).toInt()}",
                            style = TP.regular.body2,
                            color = ColdGrey04,
                            textAlign = TextAlign.Left
                        )
                    }
                }
                Spacer(Modifier.size(22.dp)) // this number is a little arbitrary
                Image(
                    painter = painterResource(R.drawable.gauge_high_solid_1),
                    contentDescription = "Sleep details",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }

            Column(
                Modifier
                    .padding(top = marginTopPlot)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .background(Color.White)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) selectedPoint = -1
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onSelectColumn(it) })
                        }
                ) {

//                    drawGrid(this, pathHorizontal, pathVertical)
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

                    // avg line
                    drawPath(
                        path = linesAvg, color = score.colors.color,
                        style = Stroke(
                            width = lineStroke.toPx(),
                            cap = StrokeCap.Square,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 10f), 5f)
                        )
                    )

                    // main BARS
                    pathsSocial.forEachIndexed { i, it ->
                        drawPath(
                            path = it,
                            color = if (i == selectedPoint) score.colors.color else {
                                if (i == pathsSocial.lastIndex && chartType != Chart.WEEK) score.colors.color.copy(
                                    0.2f
                                ) else score.colors.color.copy(
                                    0.5f
                                )
                            },
                        )
                    }

                    pathsTotal.forEachIndexed { i, it ->
                        drawPath(
                            path = it,
                            color = if (i == selectedPoint) MetricViolet else {
                                if (i == pathsTotal.lastIndex && chartType != Chart.WEEK) MetricViolet.copy(
                                    0.2f
                                ) else MetricViolet.copy(
                                    0.5f
                                )
                            },
                        )
                    }


                    if (selectedPoint >= 0 && !showNoDataInfo) {
                        val padInfo = 4.dp
                        val thisInfoHeight = textFirstLineSize.height + padInfo.times(2).toPx()
                        val thisInfoWidth = textFirstLineSize.width + padInfo.times(2).toPx()

                        val xPos = (stepVerticalLines + lineStroke).times(selectedPoint)
                            .toPx() + barWidth.toPx()
                        val yPos = (topPointsList.flatten()[selectedPoint].y - 6.dp.toPx())
                            .coerceAtLeast(thisInfoHeight)

                        val trianglePath = Path().let {
                            it.moveTo(xPos, yPos + 6.dp.toPx())
                            it.lineTo(xPos - 9.dp.div(2).toPx(), yPos - 1)
                            it.lineTo(xPos + 9.dp.div(2).toPx(), yPos - 1)
                            it.close()
                            it
                        }

                        // Bubble
                        drawRoundRect(
                            score.colors.color,
                            size = Size(thisInfoWidth, thisInfoHeight),
                            topLeft = Offset(
                                (xPos - thisInfoWidth.div(2)).coerceAtMost(width.toPx() - thisInfoWidth),
                                (yPos - thisInfoHeight).coerceAtLeast(0f)
                            ),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        // Triangle of bubble
                        drawPath(
                            path = trianglePath,
                            score.colors.color,
                        )
                        // first line
                        drawText(
                            textMeasurer = textMeasure,
                            text = textFirstLine,
                            topLeft = Offset(
                                (xPos - textFirstLineSize.width / 2f).coerceAtMost(
                                    width.toPx() - textFirstLineSize.width.div(2) - thisInfoWidth.div(
                                        2
                                    )
                                ),
                                (yPos - textFirstLineSize.height - padInfo.toPx()).coerceAtLeast(
                                    padInfo.toPx()
                                )
                            )
                        )

                    }

                    if (selectedPoint >= 0 && showNoDataInfo) {

                        val xPos = (stepVerticalLines + lineStroke).times(selectedPoint)
                            .toPx() + barWidth.toPx()
                        val yPos = height.div(2).toPx()

                        val trianglePath = Path().let {
                            it.moveTo(xPos, yPos + 6.dp.toPx())
                            it.lineTo(xPos - 9.dp.div(2).toPx(), yPos - 1)
                            it.lineTo(xPos + 9.dp.div(2).toPx(), yPos - 1)
                            it.close()
                            it
                        }

                        // Bubble
                        drawRoundRect(
                            score.colors.color,
                            size = Size(
                                noDataLineSize.width + 12.dp.toPx(),
                                noDataLineSize.height.toFloat() + 6.dp.toPx()
                            ),
                            topLeft = Offset(
                                (xPos - noDataLineSize.width / 2 - 6.dp.toPx()).coerceAtMost(width.toPx() - noDataLineSize.width),
                                yPos - noDataLineSize.height - 6.dp.toPx()
                            ),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        // Triangle of bubble
                        drawPath(
                            path = trianglePath,
                            score.colors.color,
                        )
                        drawText(
                            textMeasurer = textMeasure,
                            text = noDataText,
                            topLeft = Offset(
                                (xPos - noDataLineSize.width / 2).coerceAtMost(
                                    width.toPx() - noDataLineSize.width
                                ),
                                yPos - noDataLineSize.height - 3.dp.toPx()
                            )
                        )

                    }
                }
                Canvas(
                    modifier = Modifier
                        .width(width)
                        .height(spaceForText)
                        .background(Color.Transparent)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) selectedPoint = -1
                        }
                ) {

                    if (selectedPoint >= 0 && !showNoDataInfo) {
                        drawPath(
                            path = pathsHighlightBottom[selectedPoint],
                            color = score.colors.color.copy(0.1f),
                        )
                    }

                    for (step in 0 until valuesToPlot.size) {

                        val xP = (stepVerticalLines + lineStroke).times(step)
                            .toPx() + barWidth.toPx()

                        drawText(
                            textMeasurer = textMeasure,
                            text = xLabelsText[step],
                            topLeft = Offset(
                                (xP - xLabelsTextSizes[step].width / 2f).coerceAtMost(width.toPx() - xLabelsTextSizes[step].width),
                                48.dp.toPx() + 12.dp.toPx()
                            )
                        )

                        drawRoundRect(
                            color = if (scoreValuesToPlot.values[step].isNaN()) Color.White else score.colors.color,
                            size = Size(
                                scoreBubbleWidth.times(2).toPx(),
                                scoreBubbleHeight.times(2).toPx()
                            ),
                            topLeft = Offset(
                                xP - scoreBubbleWidth.toPx(),
                                32.dp.toPx()
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )

                        drawText(
                            textMeasurer = textMeasure,
                            text = scoreTexts[step],
                            topLeft = Offset(
                                (stepVerticalLines + lineStroke).times(step)
                                    .toPx() + barWidth.toPx() - scoreTextSizes[step].width / 2f,
                                32.dp.toPx() - 1.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        DashedLineLegend(score.colors.color, stringResource(legendText))
    }
}



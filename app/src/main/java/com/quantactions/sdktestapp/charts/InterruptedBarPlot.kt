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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.model.SleepSummary.Companion.ZonedDateTimePlaceholder
import com.quantactions.sdk.dropna
import com.quantactions.sdk.periodicMean
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.core_ui.metrics.toSpanStyle
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey01
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey04
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey07
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.utils.StringFormatter
import java.lang.Integer.max
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields


/**
 * Chart with the last 7 days of data within the circular indicator in the summary view of the
 * metrics
 * */
@Composable
fun InterruptedBarPlot(
    sleepSummary: TimeSeries.SleepSummaryTimeTimeSeries,
    sleepScore: TimeSeries.DoubleTimeSeries,
    score: Score,
    chartType: Chart,
) {

    val formatterHours = DateTimeFormatter.ofPattern("HH:mm")
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    var selectedPoint by remember { mutableIntStateOf(-1) }

    val marginLeft = 48.dp
    val marginRight = 22.dp
    val marginTopPlot = 8.dp

    val nVerticalLines = chartType.numValues
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

    val width = screenWidth - marginRight - marginLeft
    val lineStroke = 1.dp
    val barWidth: Dp

    var valuesToPlot by remember { mutableStateOf(TimeSeries.SleepSummaryTimeTimeSeries()) }
    var scoreValuesToPlot by remember { mutableStateOf(TimeSeries.DoubleTimeSeries()) }
    val xLabelsText: List<AnnotatedString>
    val times: TimeSeries.DoubleTimeSeries

    when (chartType) {
        Chart.WEEK -> {
            valuesToPlot =
                sleepSummary.fillMissingDays(Chart.WEEK.numValues)
                    .takeLast(Chart.WEEK.numValues)

            scoreValuesToPlot =
                sleepScore.fillMissingDays(Chart.WEEK.numValues)
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
                sleepSummary.fillMissingDays(Chart.MONTH.numValues * 7)
                    .extractWeeklyAverages().takeLast(Chart.MONTH.numValues)


            scoreValuesToPlot =
                sleepScore.fillMissingDays(Chart.MONTH.numValues * 7)
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
                            String.format(stringResource(id = R.string.week),
                                timestamp.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                        )
                    }
                }
            }
        }

        Chart.YEAR -> {
            valuesToPlot =
                sleepSummary.fillMissingDays(366).extractMonthlyAverages()
                    .takeLast(Chart.YEAR.numValues)

            scoreValuesToPlot =
                sleepScore.fillMissingDays(366)
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

    val interruptionWidth = 6.dp
    val interruptionHeight = 4.dp

    var prevSelectedPoint by remember { mutableIntStateOf(-1) }
    var showNoDataInfo by remember { mutableStateOf(false) }
    var lastXValue by remember { mutableFloatStateOf(-1.0f) }

    val scoreBubbleWidth = 8.dp
    val scoreBubbleHeight = 6.dp
    val spaceForText = 70.dp + 20.dp + 10.dp

    val flatPointsList: List<PointF>
    val pointsTop: List<List<PointF>>
    val pointsBottom: List<List<PointF>>
    val pointsInterruptions: List<MutableList<Float>>
    val pathHorizontal: Path
    val pathVertical: Path
    val paths = mutableListOf<Path>()
    val pathsHighlightTop = mutableListOf<Path>()
    val pathsHighlightBottom = mutableListOf<Path>()
    val pathsInterruptions = mutableListOf<Path>()
    val linesArea: Path
    val bottomTops: List<Pair<Long, Long>>
    val averageBottomTops: Pair<Long, Long>
    val minMax: List<List<PointF>>
    val legendText: Int

    val textMeasure = rememberTextMeasurer()

    // select type of chart
    when (chartType) {
        Chart.WEEK -> {
            legendText = R.string.legend_sleep_summary_14days
            pointsInterruptions = List(Chart.WEEK.numValues) { mutableListOf() }
        }

        Chart.MONTH -> {
            legendText = R.string.legend_sleep_summary_5weeks
            pointsInterruptions = List(Chart.MONTH.numValues) { mutableListOf() }
        }

        Chart.YEAR -> {
            legendText = R.string.legend_sleep_summary_12months
            pointsInterruptions = List(Chart.YEAR.numValues) { mutableListOf() }
        }
    }

    val minHour = findMinSleepStart(valuesToPlot.dropna())
    val maxHour = findMaxSleepEnd(valuesToPlot.dropna())

    val reference = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
    val minTime = reference.plusSeconds(minHour)
    val maxTime = reference.plusSeconds(maxHour)
    val nHours = ChronoUnit.HOURS.between(minTime, maxTime).toInt() + 1
    val yLabels = List(nHours) { formatterHours.format(minTime.plusHours(it.toLong())) }

    val nHorizontalLines = yLabels.size
    val height = 20.dp.times(nHorizontalLines)
    val stepVerticalLines = (width - barWidth.times(2) - lineStroke.times(nVerticalLines - 1)).div(nVerticalLines - 1)
    val stepHorizontalLines = (height - lineStroke.times(nHorizontalLines - 1)).div(nHorizontalLines - 1)

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

        bottomTops = valuesToPlot.map { sleepSummary, zonedDateTime ->
            lengthFromSleepWake(
                sleepSummary.sleepStart,
                sleepSummary.sleepEnd,
                zonedDateTime
            )
        }

        val meanStart = periodicMean(
            valuesToPlot.values.map { it.sleepStart }.dropna(),
            valuesToPlot.dropna().timestamps, LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        )

        val meanEnd = periodicMean(
            valuesToPlot.values.map { it.sleepEnd }.dropna(),
            valuesToPlot.dropna().timestamps, LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        )
        averageBottomTops = Pair(
            mapTimeToPlot(meanEnd.toLocalDateTime(), reference),
            mapTimeToPlot(meanStart.toLocalDateTime(), reference)
        )

        // MAIN LINE
        flatPointsList = calculatePointsForDataGeneralFlat(
            times.values,
            width.toPx(),
            height.toPx(),
            horizontalBias = barWidth.toPx(),
            maxVal = (maxHour - minHour).toFloat(),
            minVal = 0f
        )
        lastXValue = flatPointsList.last().x

        pointsTop = calculatePointsForDataGeneral(
            bottomTops.map { (top, _) -> top.toDouble() - minHour },
            width.toPx(),
            height.toPx(),
            maxVal = (maxHour - minHour).toFloat(),
            horizontalBias = barWidth.toPx(),
            includeOutOfChartLeft = false,
            fromTop = true,
            minVal = 0f
        )

        pointsBottom = calculatePointsForDataGeneral(
            bottomTops.map { (_, bottom) -> bottom.toDouble() - minHour },
            width.toPx(),
            height.toPx(),
            maxVal = (maxHour - minHour).toFloat(),
            horizontalBias = barWidth.toPx(),
            includeOutOfChartLeft = false,
            fromTop = true,
            minVal = 0f
        )

        pointsTop.zip(pointsBottom).forEach { (theseTops, theseBottoms) ->
            theseTops.zip(theseBottoms).forEach { (top, bottom) ->
                paths.add(
                    Path().apply {
                        moveTo(bottom.x - barWidth.toPx(), bottom.y)
                        lineTo(bottom.x + barWidth.toPx(), bottom.y)
                        lineTo(bottom.x + barWidth.toPx(), top.y)
                        lineTo(bottom.x - barWidth.toPx(), top.y)
                        close()
                    }
                )
                pathsHighlightTop.add(Path().apply {
                    moveTo(bottom.x - barWidth.toPx(), top.y)
                    lineTo(bottom.x + barWidth.toPx(), top.y)
                    lineTo(bottom.x + barWidth.toPx(), height.toPx())
                    lineTo(bottom.x - barWidth.toPx(), height.toPx())
                    close()
                })
                pathsHighlightBottom.add(Path().apply {
                    moveTo(bottom.x - barWidth.toPx(), 0f)
                    lineTo(bottom.x + barWidth.toPx(), 0f)
                    lineTo(bottom.x + barWidth.toPx(), spaceForText.toPx())
                    lineTo(bottom.x - barWidth.toPx(), spaceForText.toPx())
                    close()
                })
            }
        }

        // interruptions
        valuesToPlot.map { sleepSummary, zonedDateTime ->
            Pair(
                sleepSummary.interruptionsStart,
                zonedDateTime
            )
        }
            .forEachIndexed { i, (interruptions, t) ->
                val xDiff = (width.toPx() - barWidth.toPx() * 2) / (valuesToPlot.size - 1)
                val xP = xDiff * i + barWidth.toPx()
                calculatePointsForDataGeneral(
                    interruptions.map { mapTimeToPlot(it, t).toDouble() - minHour },
                    width.toPx(),
                    height.toPx(),
                    maxVal = (maxHour - minHour).toFloat(),
                    horizontalBias = barWidth.toPx(),
                    includeOutOfChartLeft = false,
                    fromTop = true,
                    minVal = 0f
                ).flatten().forEach {
                    pointsInterruptions[i].add(it.y)
                    pathsInterruptions.add(
                        Path().apply {
                            moveTo(xP - interruptionWidth.toPx(), it.y - interruptionHeight.toPx())
                            lineTo(xP + interruptionWidth.toPx(), it.y - interruptionHeight.toPx())
                            lineTo(xP + interruptionWidth.toPx(), it.y + interruptionHeight.toPx())
                            lineTo(xP - interruptionWidth.toPx(), it.y + interruptionHeight.toPx())
                            close()
                        }
                    )

                }

            }
        // end

        // BASELINE SHADING
        minMax = calculatePointsForDataGeneral(
            listOf(
                averageBottomTops.second.toDouble() - minHour,
                averageBottomTops.first.toDouble() - minHour,
            ),
            width.toPx(),
            height.toPx(),
            maxVal = (maxHour - minHour).toFloat(),
            horizontalBias = 8.dp.toPx(),
            includeOutOfChartLeft = false,
            fromTop = true,
            minVal = 0f
        )
        linesArea = Path().apply {
            moveTo(0f, minMax[0][1].y)
            lineTo(width.toPx(), minMax[0][1].y)
            moveTo(width.toPx(), minMax[0][0].y)
            lineTo(0f, minMax[0][0].y)
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

    val interruptionsTexts =
        valuesToPlot.values.mapIndexed { i, localSleepSummary ->
            buildAnnotatedString {
                withStyle(
                    style = TP.regular.overline.toSpanStyle(
                        if (localSleepSummary.sleepStart == ZonedDateTimePlaceholder) Color.White else {
                            if (i == selectedPoint) score.colors.color else {
                                if (localSleepSummary.interruptionsStart.isNotEmpty()) ColdGrey07 else ColdGrey04
                            }
                        }
                    )
                ) {
                    append("${localSleepSummary.interruptionsStart.size}")
                }
            }
        }

    val interruptionsTimesTexts =
        valuesToPlot.values.map { it.interruptionsStart }[selectedPoint.coerceAtLeast(
            0
        )].map { interruption ->
            buildAnnotatedString {
                withStyle(
                    style = TP.medium.caption.toSpanStyle(Color.White)
                ) {
                    append(
//                        stringResource(R.string.woke_up_at_s).format(
                        formatterHours.format(
                            interruption
                        )
//                        )
                    )
                }
            }
        }

    val textFirstLine = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(
                "${
                    formatterHours.format(
                        valuesToPlot.values.map { it.sleepStart }[selectedPoint.coerceAtLeast(
                            0
                        )]
                    )
                } - ${
                    formatterHours.format(
                        valuesToPlot.values.map { it.sleepEnd }[selectedPoint.coerceAtLeast(
                            0
                        )]
                    )
                }"
            )
        }
    }

    val noDataText = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(stringResource(R.string.no_data))
        }
    }

    val awakeAtText = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(stringResource(R.string.awake))
        }
    }

    val sleepLength = sleepLength(
        valuesToPlot.values.map { it.sleepStart }[selectedPoint.coerceAtLeast(
            0
        )], valuesToPlot.values.map { it.sleepEnd }[selectedPoint.coerceAtLeast(0)]
    )

    val textSecondLine = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(
                stringResource(
                    R.string.sleep_length_with_parenthesis,
                    sleepLength.hours,
                    sleepLength.minutes
                )
            )
        }
    }

    val awakeAtLayoutResult: TextLayoutResult = textMeasure.measure(text = awakeAtText)
    val awakeAtLineSize = awakeAtLayoutResult.size

    val textFirstLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textFirstLine)
    val textFirstLineSize = textFirstLineLayoutResult.size

    val textSecondLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textSecondLine)
    val textSecondLineSize = textSecondLineLayoutResult.size

    val noDataLayoutResult: TextLayoutResult = textMeasure.measure(text = noDataText)
    val noDataLineSize = noDataLayoutResult.size

    val xLabelsTextLayoutResults = xLabelsText.map { textMeasure.measure(text = it) }
    val xLabelsTextSizes = xLabelsTextLayoutResults.map { it.size }

    val interruptionsTimesTextLayoutResult =
        interruptionsTimesTexts.map { textMeasure.measure(text = it) }
    val interruptionsTimesTextSizes = interruptionsTimesTextLayoutResult.map { it.size }

    val scoreTextLayoutResults = scoreTexts.map { textMeasure.measure(text = it) }
    val scoreTextSizes = scoreTextLayoutResults.map { it.size }

    val interruptionsTextLayoutResults = interruptionsTexts.map { textMeasure.measure(text = it) }
    val interruptionsTextSizes = interruptionsTextLayoutResults.map { it.size }

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
                    yLabels.forEach {
                        Text(
                            text = it,
                            style = TP.regular.body2,
                            color = ColdGrey04,
                            textAlign = TextAlign.Left
                        )
                    }
                }
                Spacer(Modifier.size(9.dp))
                Image(
                    painter = painterResource(R.drawable.noun_owl_3020475_1),
                    contentDescription = "Sleep details",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.size(10.dp))
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
                            detectTapGestures(onTap = { tapOffset ->
                                prevSelectedPoint = selectedPoint
                                selectedPoint =
                                    findClosest(
                                        pointsTop
                                            .flatten()
                                            .map { it.x }, tapOffset.x
                                    ) ?: -1

                                if (prevSelectedPoint == selectedPoint) selectedPoint = -1
                                if (selectedPoint >= 0) {
                                    showNoDataInfo =
                                        valuesToPlot.values.map { it.sleepStart }[selectedPoint] == ZonedDateTimePlaceholder
                                }
                            })
                        }
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

//                    drawGrid(this, pathHorizontal, pathVertical)

                    // baseline shading
                    drawPath(
                        path = linesArea, color = score.colors.color,
                        style = Stroke(
                            width = lineStroke.toPx(),
                            cap = StrokeCap.Square,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 10f), 5f)
                        )
                    )

                    // main BARS
                    paths.forEachIndexed { i, it ->
                        drawPath(
                            path = it,
                            color = if (i == selectedPoint) score.colors.color else {
                                if (i == paths.lastIndex && chartType != Chart.WEEK) score.colors.color.copy(
                                    0.2f
                                ) else score.colors.color.copy(
                                    0.5f
                                )
                            },
                        )
                    }

                    if (chartType == Chart.WEEK) {
                        pathsInterruptions.forEach {
                            drawPath(
                                path = it,
                                color = Color.White,
                            )
                        }
                    }

                    if (selectedPoint >= 0 && !showNoDataInfo) {
                        drawPath(
                            path = pathsHighlightTop[selectedPoint],
                            color = score.colors.color.copy(0.1f),
                        )
                    }

                    if (selectedPoint >= 0 && !showNoDataInfo) {
                        val padInfo = 4.dp
                        val thisInfoHeight =
                            textFirstLineSize.height + textSecondLineSize.height + padInfo.times(2)
                                .toPx()
                        val thisInfoWidth = maxOf(
                            textFirstLineSize.width,
                            textSecondLineSize.width
                        ) + padInfo.times(2).toPx()

                        val xPos = (stepVerticalLines + lineStroke).times(selectedPoint)
                            .toPx() + barWidth.toPx()
                        val yPos = (pointsBottom.flatten()[selectedPoint].y - 6.dp.toPx())
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
                        drawText(
                            textMeasurer = textMeasure,
                            text = textFirstLine,
                            topLeft = Offset(
                                (xPos - textFirstLineSize.width / 2f).coerceAtMost(
                                    width.toPx() - textFirstLineSize.width - padInfo.toPx()
                                ),
                                (yPos - thisInfoHeight + padInfo.toPx()).coerceAtLeast(padInfo.toPx())
                            )
                        )
                        drawText(
                            textMeasurer = textMeasure,
                            text = textSecondLine,
                            topLeft = Offset(
                                (xPos - textSecondLineSize.width / 2f).coerceAtMost(
                                    width.toPx() - textSecondLineSize.width.div(2) - thisInfoWidth.div(
                                        2
                                    )
                                ),
                                (yPos - textSecondLineSize.height - padInfo.toPx()).coerceAtLeast(
                                    padInfo.times(2).toPx() + textFirstLineSize.height
                                )
                            )
                        )
//

                        if (interruptionsTimesTextSizes.isNotEmpty()) {
                            val infoHeightInt =
                                interruptionsTimesTextSizes.sumOf { it.height } + awakeAtLineSize.height + padInfo.times(
                                    2
                                ).toPx()
                            val infoWidthInt =
                                max(
                                    awakeAtLineSize.width,
                                    interruptionsTimesTextSizes.maxOf { it.width }) + padInfo.times(
                                    2
                                )
                                    .toPx()

                            val xPosInt = if (selectedPoint <= valuesToPlot.size / 2)
                                (stepVerticalLines + lineStroke).times(selectedPoint)
                                    .toPx() + barWidth.toPx().times(2) + 6.dp.toPx()
                            else (stepVerticalLines + lineStroke).times(selectedPoint)
                                .toPx() - infoWidthInt - 6.dp.toPx()

                            val yPosInt =
                                (pointsInterruptions[selectedPoint][0] - 6.dp.toPx() - interruptionsTextSizes[0].height.div(
                                    2
                                )).coerceAtLeast(thisInfoHeight)

                            if (chartType == Chart.WEEK) {
                                drawRoundRect(
                                    score.colors.color,
                                    size = Size(infoWidthInt, infoHeightInt),
                                    topLeft = Offset(
                                        (xPosInt).coerceAtMost(width.toPx() - infoWidthInt),
                                        yPosInt.coerceAtLeast(0f)
                                    ),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )

                                drawText(
                                    textMeasurer = textMeasure,
                                    text = awakeAtText,
                                    topLeft = Offset(
                                        (xPosInt + padInfo.toPx()).coerceAtMost(
                                            width.toPx() - awakeAtLineSize.width.div(2) - padInfo.toPx()
                                        ),
                                        (yPosInt + padInfo.toPx()).coerceAtLeast(
                                            padInfo.toPx()
                                        )
                                    )
                                )

                                interruptionsTimesTextSizes.forEachIndexed { ii, interruptionTimeText ->
                                    drawText(
                                        textMeasurer = textMeasure,
                                        text = interruptionsTimesTexts[ii],
                                        topLeft = Offset(
                                            (xPosInt + padInfo.toPx()).coerceAtMost(
                                                width.toPx() - interruptionTimeText.width - padInfo.toPx()
                                            ),
                                            (yPosInt + ii * interruptionTimeText.height + awakeAtLineSize.height + padInfo.toPx()).coerceAtLeast(
                                                padInfo.toPx()
                                            )
                                        )
                                    )
                                }
                            }

                        }

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
                                48.dp.toPx() + interruptionsTextSizes[step].height + 12.dp.toPx()
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
                                32.dp.toPx() + interruptionsTextSizes[step].height
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )


                        drawText(
                            textMeasurer = textMeasure,
                            text = scoreTexts[step],
                            topLeft = Offset(
                                (stepVerticalLines + lineStroke).times(step)
                                    .toPx() + barWidth.toPx() - scoreTextSizes[step].width / 2f,
                                32.dp.toPx() + interruptionsTextSizes[step].height - 1.dp.toPx()
                            )
                        )

                        drawText(
                            textMeasurer = textMeasure,
                            text = interruptionsTexts[step],
                            topLeft = Offset(
                                (stepVerticalLines + lineStroke).times(step)
                                    .toPx() + barWidth.toPx() - interruptionsTextSizes[step].width / 2f,
                                16.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(16.dp))
        DashedLineLegend(Score.SLEEP_SCORE.colors.color, stringResource(legendText))
    }
}



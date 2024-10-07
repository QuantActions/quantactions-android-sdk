/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp.charts

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.core_ui.metrics.toSpanStyle
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey01
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey04
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey07
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.utils.StringFormatter
import org.nield.kotlinstatistics.percentile
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import kotlin.math.abs

/**
 * Chart that shows lines with their confidence intervals.
 * @param timeSeries to plot
 * @param score that we are plotting
 * @param chartType from [Chart]
 * @param showConfidence whether ot not to show the confidence intervals
 * */
@Composable
fun ShadedLineChart(
    timeSeries: TimeSeries.DoubleTimeSeries,
    score: Score,
    chartType: Chart,
    showConfidence: Boolean,
    basicInfo: BasicInfo,
    maxValRequested: Float,
    adaptiveRange: Boolean,
) {
    var selectedPoint by remember {
        mutableIntStateOf(-1)
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

        val marginLeft = 48.dp
        val marginRight = 22.dp
        val marginTopPlot = 8.dp


    val nVerticalLines = chartType.numValues
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
    val width = screenWidth - marginRight - marginLeft
    val lineStroke = 1.dp

    var valuesToPlot by remember { mutableStateOf(TimeSeries.DoubleTimeSeries()) } 
    val xLabelsText: List<AnnotatedString>
    val times: TimeSeries.DoubleTimeSeries
    val horizontalBias: Dp

    // other

    when (chartType) {
        Chart.WEEK -> {

            valuesToPlot =
                timeSeries.fillMissingDays(Chart.WEEK.numValues)
                    .takeLast(Chart.WEEK.numValues)


            times = TimeSeries.DoubleTimeSeries().fillMissingDays(Chart.WEEK.numValues)
                .takeLast(Chart.WEEK.numValues)

            horizontalBias = 8.dp

            // x labels
            xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = if (i == (selectedPoint - 1)) score.colors.color else {
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


            times = TimeSeries.DoubleTimeSeries().fillMissingDays(Chart.MONTH.numValues * 7)
                .extractWeeklyAverages().takeLast(Chart.MONTH.numValues)


            horizontalBias = 20.dp
            // x labels
            xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = if (i == selectedPoint - 1) score.colors.color else ColdGrey04,
                            fontSize = TP.regular.overline.fontSize,
                            fontStyle = TP.regular.overline.fontStyle,
                            fontWeight = TP.regular.overline.fontWeight
                        )
                    ) {
                        append(
                            String.format(
                                stringResource(id = R.string.week),
                                timestamp.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
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

            times = TimeSeries.DoubleTimeSeries().fillMissingDays(366).extractMonthlyAverages()
                .takeLast(Chart.YEAR.numValues)

            horizontalBias = 8.dp
            // x labels
            xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                buildAnnotatedString {
                    withStyle(
                        style = TP.regular.overline.toSpanStyle(if (i == selectedPoint - 1) score.colors.color else ColdGrey04)
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

    var prevSelectedPoint by remember {
        mutableIntStateOf(-1)
    }

    var minVal = 0f
    var maxVal = maxValRequested

    if (adaptiveRange) {
        maxVal = timeSeries.values.percentile(80.0).toFloat()
        maxVal -= maxVal % 50
        minVal = timeSeries.values.percentile(5.0).toFloat() * .95f
        minVal -= minVal % 50
    }

    val textMeasure = rememberTextMeasurer()

    val height = 250.dp
    val nHorizontalLines = 11

    val stepVerticalLines =
        (width - horizontalBias.times(2) - lineStroke.times(nVerticalLines - 1)).div(nVerticalLines - 1)
    val stepHorizontalLines =
        (height - lineStroke.times(nHorizontalLines - 1)).div(nHorizontalLines - 1)

    val pointsList: List<List<PointF>>
    val pointsListCircles: List<List<PointF>>
    val flatPointsList: List<PointF>
    val pathHorizontal: Path
    val pathVertical: Path
    val paths: Path
    val pointsListCIH: List<List<PointF>>
    val pointsListCIL: List<List<PointF>>
    val connPointsList: List<Pair<List<PointF>, List<PointF>>>
    val connPointsListCIH: List<Pair<List<PointF>, List<PointF>>>
    val connPointsListCIL: List<Pair<List<PointF>, List<PointF>>>
    val pathsH: List<Path>
    var selectedY by remember { mutableIntStateOf(-1) }
    var selectedX by remember { mutableFloatStateOf(-1.0f) }
    var selectedTimePoint by remember { mutableIntStateOf(-1) }
    var selectedLowY by remember { mutableIntStateOf(-1) }
    var selectedHighY by remember { mutableIntStateOf(-1) }
    var lastXValue by remember { mutableFloatStateOf(-1.0f) }
    val shadedArea: Path
    val linesArea: Path

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
        pointsList = calculatePointsForDataGeneral(
            valuesToPlot.values,
            width.toPx(),
            height.toPx(),
            horizontalBias = horizontalBias.toPx(),
            maxVal = maxVal,
            minVal = minVal
        )
        pointsListCircles = calculatePointsForDataGeneral(
            valuesToPlot.values,
            width.toPx(),
            height.toPx(),
            horizontalBias = horizontalBias.toPx(),
            maxVal = maxVal,
            minVal = minVal
        )
        flatPointsList = calculatePointsForDataGeneralFlat(
            times.values,
            width.toPx(),
            height.toPx(),
            horizontalBias = horizontalBias.toPx(),
            maxVal = maxVal,
            minVal = minVal
        )
        lastXValue = flatPointsList.last().x
        connPointsList = pointsList.map {
            calculateConnectionPointsForBezierCurve(it)
        }
        paths = Path().apply {
            pointsList.zip(connPointsList).map { (points, connPoints) ->
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

        // CONFIDENCE SHADING
        pointsListCIH = calculatePointsForDataGeneral(
            valuesToPlot.confidenceIntervalHigh,
            width.toPx(),
            height.toPx(),
            horizontalBias = horizontalBias.toPx(),
            maxVal = maxVal,
            minVal = minVal
        )
        pointsListCIL = calculatePointsForDataGeneral(
            valuesToPlot.confidenceIntervalLow,
            width.toPx(),
            height.toPx(),
            reverse = true,
            horizontalBias = horizontalBias.toPx(),
            maxVal = maxVal,
            minVal = minVal
        )
        connPointsListCIH = pointsListCIH.map {
            calculateConnectionPointsForBezierCurve(it)
        }
        connPointsListCIL = pointsListCIL.map {
            calculateConnectionPointsForBezierCurve(it)
        }
        pathsH = List(pointsListCIH.size) { segmentIndex ->
            Path().apply {
                var points = pointsListCIH[segmentIndex]
                var connPoints = connPointsListCIH[segmentIndex]
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    cubicTo(
                        connPoints.first[i - 1].x, connPoints.first[i - 1].y,
                        connPoints.second[i - 1].x, connPoints.second[i - 1].y,
                        points[i].x, points[i].y
                    )
                }

                points = pointsListCIL[pointsListCIH.size - 1 - segmentIndex]
                connPoints = connPointsListCIL[pointsListCIH.size - 1 - segmentIndex]
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


        // BASELINE SHADING
        val range = score.getReferencePopulationRange(basicInfo)
        val minMax = calculatePointsForDataGeneral(
            listOf(
                range.percentile25.toDouble(),
                range.percentile75.toDouble()
            ),
            width.toPx(),
            height.toPx(),
            horizontalBias = 8.dp.toPx(),
            includeOutOfChartLeft = false,
            maxVal = maxVal,
            minVal = minVal
        )
        shadedArea = Path().apply {
            moveTo(0f, minMax[0][1].y)
            lineTo(width.toPx(), minMax[0][1].y)
            lineTo(width.toPx(), minMax[0][0].y)
            lineTo(0f, minMax[0][0].y)
            this.close()
        }
        linesArea = Path().apply {
            moveTo(0f, minMax[0][1].y)
            lineTo(width.toPx(), minMax[0][1].y)
            moveTo(width.toPx(), minMax[0][0].y)
            lineTo(0f, minMax[0][0].y)
        }
    }

    val textFirstLine = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = Color.White,
                fontSize = TP.medium.body2.fontSize,
                fontStyle = TP.medium.body2.fontStyle,
                fontWeight = TP.medium.body2.fontWeight
            )
        ) {
            append("$selectedY")
        }

    }
    val textSecondLine = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = Color.White,
                fontSize = TP.regular.caption.fontSize,
                fontStyle = TP.regular.caption.fontStyle
            )
        ) {
            append(stringResource(R.string.to, selectedLowY, selectedHighY))
        }
    }
    val textFirstLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textFirstLine)
    val textFirstLineSize = textFirstLineLayoutResult.size

    val textSecondLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textSecondLine)
    val textSecondLineSize = textSecondLineLayoutResult.size

    val xLabelsTextLayoutResults = xLabelsText.map { textMeasure.measure(text = it) }
    val xLabelsTextSizes = xLabelsTextLayoutResults.map { it.size }
    val spaceForText = 48.dp

    Column {
        Row(Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .height(height + marginTopPlot.times(2))
                    .padding(start = 20.dp, end = 6.dp)
            ) {

                List(nHorizontalLines) {
                    Text(
                        text = "${(maxVal / (nHorizontalLines - 1) * (nHorizontalLines - 1 - it) + minVal).toInt()}",
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
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) selectedPoint = -1
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        if (valuesToPlot.values.isNotEmpty()) {
                                            prevSelectedPoint = selectedPoint

                                            selectedPoint =
                                                findClosest(
                                                    flatPointsList.map { it.x },
                                                    tapOffset.x
                                                )
                                                    ?: -1

                                            if (selectedPoint > 0 && valuesToPlot.values[selectedPoint - 1].isNaN()) selectedPoint =
                                                -1
                                            if (prevSelectedPoint == selectedPoint) {
                                                selectedPoint = -1
                                                selectedY = -1
                                                selectedHighY = -1
                                                selectedLowY = -1
                                                selectedX = -1f
                                            }
                                            if (selectedPoint > 0) {
                                                selectedY =
                                                    valuesToPlot.values[selectedPoint - 1].toInt()
                                                selectedX =
                                                    flatPointsList.map { it.x }[selectedPoint]
                                                selectedHighY =
                                                    valuesToPlot.confidenceIntervalHigh[selectedPoint - 1].toInt()
                                                selectedLowY =
                                                    valuesToPlot.confidenceIntervalLow[selectedPoint - 1].toInt()

                                                if (abs(flatPointsList.map { it.y }[selectedPoint] - tapOffset.y) > height
                                                        .div(
                                                            10
                                                        )
                                                        .toPx()
                                                ) {
                                                    selectedPoint = -1
                                                    selectedY = -1
                                                    selectedHighY = -1
                                                    selectedLowY = -1
                                                    selectedX = -1f
                                                }
                                            }
                                        }
                                    }
                                )
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
//                        drawGrid(this, pathHorizontal, pathVertical)

                        // baseline shading
                        val range = score.getReferencePopulationRange(basicInfo)
                        if (range.percentile25 != 0f || range.percentile75 != 0f) {
                            drawPath(
                                path = shadedArea, color = ColdGrey04.copy(0.1f),
                            )
                            drawPath(
                                path = linesArea, color = ColdGrey04,
                                style = Stroke(
                                    width = lineStroke.toPx(),
                                    cap = StrokeCap.Square,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(10f, 10f),
                                        5f
                                    )
                                )
                            )
                        }
                        // END

                        // SHADING
                        if (showConfidence) {
                            pathsH.forEach { pathH ->
                                drawPath(
                                    path = pathH,
                                    color = score.colors.color.copy(0.1f),
                                )
                            }

                        }
                        // end SHADING

                        // main LINE
                        drawPath(
                            path = paths,
                            color = score.colors.color,
                            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // DOTs
                        pointsListCircles.forEach { singleList ->
                            singleList.forEach {
                                if ( it.x > 0) {
                                    if (!(it.x == lastXValue &&
                                                valuesToPlot.timestamps.last().dayOfWeek != DayOfWeek.SUNDAY &&
                                                chartType in listOf(
                                            Chart.MONTH,
                                            Chart.YEAR
                                        ))
                                    ) {
                                        drawCircle(
                                            color = score.colors.color,
                                            center = Offset(it.x, it.y),
                                            radius = 5.dp.toPx()
                                        )
                                        drawCircle(
                                            color = if (it.x == selectedX) score.colors.color else Color.White,
                                            center = Offset(it.x, it.y),
                                            radius = 3.dp.toPx()
                                        )
                                    }
                                }
                            }

                        }

                        if (selectedPoint > 0) {

                            val padInfo = 4.dp
                            val thisInfoHeight = if (showConfidence) textFirstLineSize.height + textSecondLineSize.height + padInfo.toPx()
                            else textFirstLineSize.height + 0f
                            val thisInfoWidth = if (showConfidence) maxOf(textFirstLineSize.width, textSecondLineSize.width) + padInfo.times(2).toPx()
                            else textFirstLineSize.width + padInfo.times(2).toPx()

                            val xPos = (stepVerticalLines + lineStroke).times(selectedPoint - 1)
                                .toPx() + horizontalBias.toPx()
                            val selectedVertical = Path().apply {
                                moveTo(xPos, 0f)
                                lineTo(xPos, height.toPx())
                            }

                            drawPath(
                                path = selectedVertical, color = score.colors.color,
                                style = Stroke(width = lineStroke.toPx(), cap = StrokeCap.Square)
                            )

                            // Info
                            if (valuesToPlot.values[selectedPoint - 1] < 70) {
                                val trianglePath = Path().let {
                                    it.moveTo(xPos, thisInfoHeight + 6.dp.toPx())
                                    it.lineTo(xPos - 9.dp.div(2).toPx(), thisInfoHeight - 1)
                                    it.lineTo(xPos + 9.dp.div(2).toPx(), thisInfoHeight - 1)
                                    it.close()
                                    it
                                }
                                drawRoundRect(
                                    score.colors.color,
                                    size = Size(thisInfoWidth, thisInfoHeight),
                                    topLeft = Offset(
                                        if (selectedPoint != valuesToPlot.size)
                                            xPos - thisInfoWidth.div(2) else
                                            width.toPx() - thisInfoWidth,
                                        0f
                                    ),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                                drawPath(
                                    path = trianglePath,
                                    score.colors.color,
                                )
                                drawText(
                                    textMeasurer = textMeasure,
                                    text = textFirstLine,
                                    topLeft = Offset(
                                        if (selectedPoint != valuesToPlot.size)
                                            xPos - textFirstLineSize.width / 2f else
                                            width.toPx() - thisInfoWidth.div(2)
                                                    - textFirstLineSize.width / 2f,
                                        0f
                                    )
                                )
                                if (showConfidence)
                                    drawText(
                                        textMeasurer = textMeasure,
                                        text = textSecondLine,
                                        topLeft = Offset(
                                            if (selectedPoint != valuesToPlot.size)
                                                xPos - textSecondLineSize.width / 2f else
                                                width.toPx() - thisInfoWidth.div(2)
                                                        - textSecondLineSize.width / 2f,
                                            textFirstLineSize.height.toFloat()
                                        )
                                    )
                            } else {
                                val refHeight =
                                    if (selectedPoint == chartType.numValues) height.div(4).times(3)
                                        .toPx() else height.toPx()
                                val trianglePath = Path().let {
                                    it.moveTo(xPos, refHeight - thisInfoHeight - 6.dp.toPx())
                                    it.lineTo(
                                        xPos - 9.dp.div(2).toPx(),
                                        refHeight - thisInfoHeight + 1
                                    )
                                    it.lineTo(
                                        xPos + 9.dp.div(2).toPx(),
                                        refHeight - thisInfoHeight + 1
                                    )
                                    it.close()
                                    it
                                }
                                drawRoundRect(
                                    score.colors.color,
                                    size = Size(thisInfoWidth, thisInfoHeight),
                                    topLeft = Offset(
                                        if (selectedPoint != valuesToPlot.size)
                                            xPos - thisInfoWidth.div(2) else
                                            width.toPx() - thisInfoWidth,
                                        refHeight - thisInfoHeight
                                    ),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                                drawPath(
                                    path = trianglePath,
                                    score.colors.color,
                                )
                                drawText(
                                    textMeasurer = textMeasure,
                                    text = textFirstLine,
                                    topLeft = Offset(
                                        if (selectedPoint != valuesToPlot.size)
                                            xPos - textFirstLineSize.width / 2f else
                                            width.toPx() - thisInfoWidth.div(2)
                                                    - textFirstLineSize.width / 2f,

                                        refHeight - thisInfoHeight
                                    )
                                )
                                if (showConfidence)
                                    drawText(
                                        textMeasurer = textMeasure,
                                        text = textSecondLine,
                                        topLeft = Offset(
                                            if (selectedPoint != valuesToPlot.size)
                                                xPos - textSecondLineSize.width / 2f else
                                                width.toPx() - thisInfoWidth.div(2)
                                                        - textSecondLineSize.width / 2f,
                                            refHeight - thisInfoHeight + textFirstLineSize.height.toFloat()
                                        )
                                    )
                            }
                            // end info
                        }
                    }
                }
                // Canvas for the text and the journal bubbles
                Canvas(
                    modifier = Modifier
                        .width(width)
                        .height(spaceForText)
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    // When the user taps on the Canvas, you can
                                    // check if the tap offset is in one of the
                                    // tracked Rects.
                                    selectedTimePoint =
                                        findClosest(flatPointsList.map { it.x }, tapOffset.x)
                                            ?: -1
                                }
                            )
                        }
                ) {

                    for (step in 0 until valuesToPlot.size) {
                        val xP = (stepVerticalLines + lineStroke).times(step)
                            .toPx() + horizontalBias.toPx()
                        // x label
                        drawText(
                            textMeasurer = textMeasure,
                            text = xLabelsText[step],
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
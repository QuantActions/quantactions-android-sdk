package com.quantactions.sdktestapp.charts


import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.core_ui.metrics.millisecondsToHourMinutes
import com.quantactions.sdktestapp.core_ui.metrics.toSpanStyle
import com.quantactions.sdktestapp.core_ui.theme.*
import com.quantactions.sdk.*
import com.quantactions.sdk.data.model.JournalEntry
import java.time.ZonedDateTime
import kotlin.math.roundToLong


/**
 * Chart with the last 7 days of data within the circular indicator in the summary view of the
 * metrics
 * */
@OptIn(ExperimentalTextApi::class)
@Composable
fun CumulativeBarPlot(
    screenTime: TimeSeries.ScreenTimeAggregateTimeSeries,
    socialEngagementScore: TimeSeries.DoubleTimeSeries,
    journal: List<JournalEntry>,
    score: Score,
    chartType: Chart,
) {

//    val nVerticalLines = chartType.numValues

//    val formatter = DateTimeFormatter.ofPattern("EEE")
    var selectedPoint by remember { mutableStateOf(-1) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val cH = BasicChart(screenTime,
        socialEngagementScore,
        score,
        selectedPoint,
        chartType,
        marginLeft = 36.dp,
        marginRight = 22.dp,
        marginTopPlot = 8.dp,
        screenWidth,
        weekString = stringResource(id = R.string.week)
    )

    var prevSelectedPoint by remember { mutableStateOf(-1) }
    var showNoDataInfo by remember { mutableStateOf(false) }
    var selectedTimePoint by remember { mutableStateOf(-1) }
    var lastXValue by remember { mutableStateOf(-1.0f) }

    val scoreBubbleWidth = 8.dp
    val scoreBubbleHeight = 6.dp
    val spaceForText = 70.dp + 20.dp

    val flatPointsList: List<PointF>
    val topSocial: List<List<PointF>>
    val topTotal: List<List<PointF>>
    val pathHorizontal: Path
    val pathVertical: Path
    val pathsTotal = mutableListOf<Path>()
    val pathsSocial = mutableListOf<Path>()
    val pathsHighlightBottom = mutableListOf<Path>()
    val linesSocialAvg: Path
    val linesTotalAvg: Path
    var avgSocialLine: List<PointF>
    var avgTotalLine: List<PointF>

    val textMeasure = rememberTextMeasurer()


    val legendText = when (chartType) {
        Chart.WEEK -> {
            R.string.legend_screen_time_14days
        }
        Chart.MONTH -> {
            R.string.legend_screen_time_5weeks
        }
        Chart.YEAR -> {
            R.string.legend_screen_time_12months
        }
    }

    val yLabels = List(11) {if (it != 10) "%02dh".format(10 - it) else "0"}

    val nHorizontalLines = yLabels.size
    val height = 20.dp.times(nHorizontalLines)

    val stepVerticalLines =
        (cH.width - cH.barWidth.times(2) - cH.lineStroke.times(cH.nVerticalLines - 1)).div(cH.nVerticalLines - 1)
    val stepHorizontalLines =
        (height - cH.lineStroke.times(nHorizontalLines - 1)).div(nHorizontalLines - 1)

    with(LocalDensity.current) {
        // GRID
        pathHorizontal = Path().apply {
            moveTo(0f, 0f)
            for (step in 0 until nHorizontalLines) {
                lineTo(cH.width.toPx(), (stepHorizontalLines + cH.lineStroke).times(step).toPx())
                moveTo(0f, (stepHorizontalLines + cH.lineStroke).times(step + 1).toPx())
            }
        }
        pathVertical = Path().apply {
            moveTo(cH.barWidth.toPx(), 0f)
            for (step in 0 until cH.nVerticalLines) {
                lineTo(
                    (stepVerticalLines + cH.lineStroke).times(step).toPx() + cH.barWidth.toPx(),
                    height.toPx()
                )
                moveTo(
                    (stepVerticalLines + cH.lineStroke).times(step + 1).toPx() + cH.barWidth.toPx(),
                    0f
                )
            }
        }

        // MAIN LINE

        flatPointsList = calculatePointsForDataGeneralFlat(
            cH.times.values,
            cH.width.toPx(),
            height.toPx(),
            horizontalBias = cH.barWidth.toPx(),
            maxVal = 10f * 1000 * 3600,
            minVal = 0f
        )
        lastXValue = flatPointsList.last().x

        topSocial = calculatePointsForDataGeneral(
            cH.valuesToPlot.map { screenTimeAggregate, _ -> screenTimeAggregate.socialScreenTime }.map { if (it.isNaN()) 0.0 else it },
            cH.width.toPx(),
            height.toPx(),
            maxVal = 10f * 1000 * 3600,
            horizontalBias = cH.barWidth.toPx(),
            includeOutOfChartLeft = false,
            minVal = 0f
        )

        topTotal = calculatePointsForDataGeneral(
            cH.valuesToPlot.map { screenTimeAggregate, _ -> screenTimeAggregate.totalScreenTime }.map { if (it.isNaN()) 0.0 else it },
            cH.width.toPx(),
            height.toPx(),
            maxVal = 10f * 1000 * 3600,
            horizontalBias = cH.barWidth.toPx(),
            includeOutOfChartLeft = false,
            minVal = 0f
        )


        topTotal.zip(topSocial).forEach { (theseTops, theseBottoms) ->
            theseTops.zip(theseBottoms).forEach { (top, bottom) ->
                pathsTotal.add(
                    Path().apply {
                        moveTo(bottom.x - cH.barWidth.toPx(), bottom.y)
                        lineTo(bottom.x + cH.barWidth.toPx(), bottom.y)
                        lineTo(bottom.x + cH.barWidth.toPx(), top.y)
                        lineTo(bottom.x - cH.barWidth.toPx(), top.y)
                        close()
                    }
                )
            }
        }

        topSocial.forEach { theseTops ->
            theseTops.forEach { top ->
                pathsSocial.add(
                    Path().apply {
                        moveTo(top.x - cH.barWidth.toPx(), top.y)
                        lineTo(top.x + cH.barWidth.toPx(), top.y)
                        lineTo(top.x + cH.barWidth.toPx(), height.toPx())
                        lineTo(top.x - cH.barWidth.toPx(), height.toPx())
                        close()
                    }
                )
                pathsHighlightBottom.add(Path().apply {
                    moveTo(top.x - cH.barWidth.toPx(), 0f)
                    lineTo(top.x + cH.barWidth.toPx(), 0f)
                    lineTo(top.x + cH.barWidth.toPx(), spaceForText.toPx())
                    lineTo(top.x - cH.barWidth.toPx(), spaceForText.toPx())
                    close()
                })
            }
        }

        // avg line
        avgSocialLine = calculatePointsForDataGeneral(
            listOf(cH.valuesToPlot.values.filter { !it.socialScreenTime.isNaN() }.map { it.socialScreenTime }.average()),
            cH.width.toPx(),
            height.toPx(),
            maxVal = 10f * 1000 * 3600,
            minVal = 0f,
            horizontalBias = cH.barWidth.toPx(),
            includeOutOfChartLeft = false,
        ).flatten()

        if (avgSocialLine.isEmpty()){
            avgSocialLine = listOf(PointF(0f, 0f))
        }

        linesSocialAvg = Path().apply {
            moveTo(0f, avgSocialLine[0].y)
            lineTo(cH.width.toPx(), avgSocialLine[0].y)
        }

        avgTotalLine = calculatePointsForDataGeneral(
            listOf(cH.valuesToPlot.values.filter { !it.totalScreenTime.isNaN() }.map { it.totalScreenTime }.average()),
            cH.width.toPx(),
            height.toPx(),
            maxVal = 10f * 1000 * 3600,
            minVal = 0f,
            horizontalBias = cH.barWidth.toPx(),
            includeOutOfChartLeft = false,
        ).flatten()

        if (avgTotalLine.isEmpty()){
            avgTotalLine = listOf(PointF(0f, 0f))
        }

        linesTotalAvg = Path().apply {
            moveTo(0f, avgTotalLine[0].y)
            lineTo(cH.width.toPx(), avgTotalLine[0].y)
        }

    }

    val scoreTexts = cH.scoreValuesToPlot.values.map { value ->
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



    val selectedScreenTime =
        cH.valuesToPlot.values.map { it.totalScreenTime }[selectedPoint.coerceAtLeast(0)]
    val selectedSocialScreenTime =
        cH.valuesToPlot.values.map { it.socialScreenTime }[selectedPoint.coerceAtLeast(0)]

    val selectedScreenTimeString = millisecondsToHourMinutes(preAmount = if (!selectedScreenTime.isNaN()) selectedScreenTime.roundToLong() else 0L)
    val selectedSocialScreenTimeString = millisecondsToHourMinutes(preAmount = if (!selectedSocialScreenTime.isNaN()) selectedSocialScreenTime.roundToLong() else 0L)

//    val selectedScreenTimeHours = (selectedScreenTime / 1000 / 3600).toInt()
//    val selectedScreenTimeMinutes = ((selectedScreenTime - selectedScreenTimeHours * 1000 * 3600) / 1000 / 60).toInt()
//    val selectedSocialScreenTimeHours = (selectedSocialScreenTime / 1000 / 3600).toInt()
//    val selectedSocialScreenTimeMinutes = ((selectedSocialScreenTime - selectedSocialScreenTimeHours * 1000 * 3600) / 1000 / 60).toInt()

    val textFirstLine = buildAnnotatedString {
        withStyle(
            style = TP.medium.h1.toSpanStyle(Color.White)
        ) {
            append(selectedSocialScreenTimeString)
        }
    }

    val textSecondLine = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(stringResource(R.string.from_a_total_of))
        }
    }

    val textThirdLine = buildAnnotatedString {
        withStyle(
            style = TP.medium.caption.toSpanStyle(Color.White)
        ) {
            append(selectedScreenTimeString)
        }
    }
    

    val textFirstLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textFirstLine)
    val textFirstLineSize = textFirstLineLayoutResult.size
    val textSecondLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textSecondLine)
    val textSecondLineSize = textSecondLineLayoutResult.size
    val textThirdLineLayoutResult: TextLayoutResult = textMeasure.measure(text = textThirdLine)
    val textThirdLineSize = textThirdLineLayoutResult.size



    val noDataLayoutResult: TextLayoutResult = textMeasure.measure(text = noDataText)
    val noDataLineSize = noDataLayoutResult.size

    val xLabelsTextLayoutResults = cH.xLabelsText.map { textMeasure.measure(text = it) }
    val xLabelsTextSizes = xLabelsTextLayoutResults.map { it.size }

    val scoreTextLayoutResults = scoreTexts.map { textMeasure.measure(text = it) }
    val scoreTextSizes = scoreTextLayoutResults.map { it.size }

    fun onSelectColumn(
        tapOffset: Offset,
    ) {
        prevSelectedPoint = selectedPoint
        selectedPoint =
            findClosest(
                topTotal
                    .flatten()
                    .map { it.x }, tapOffset.x
            ) ?: -1

        if (prevSelectedPoint == selectedPoint) selectedPoint = - 1
        if (selectedPoint >= 0) {
            showNoDataInfo = cH.valuesToPlot.values.map { it.totalScreenTime }[selectedPoint].isNaN()
        }
    }

//    fun drawBubble(
//        drawScope: DrawScope,
//
//    ) {
//
//        with(drawScope) {
//
//            val xPos = (stepVerticalLines + lineStroke).times(selectedPoint)
//                .toPx() + barWidth.toPx()
//            val yPos =
//                (pointsBottom.flatten()[selectedPoint].y - 6.dp.toPx()).coerceAtLeast(
//                    infoHeight.toPx()
//                )
//
//            val trianglePath = Path().let {
//                it.moveTo(xPos, yPos + 6.dp.toPx())
//                it.lineTo(xPos - 9.dp.div(2).toPx(), yPos - 1)
//                it.lineTo(xPos + 9.dp.div(2).toPx(), yPos - 1)
//                it.close()
//                it
//            }
//
//            // Bubble
//            drawRoundRect(
//                score.colors.color,
//                size = Size(infoWidth.toPx(), infoHeight.toPx()),
//                topLeft = Offset(
//                    (xPos - infoWidth.div(2)
//                        .toPx()).coerceAtMost(width.toPx() - infoWidth.toPx()),
//                    (yPos - infoHeight.toPx()).coerceAtLeast(0f)
//                ),
//                cornerRadius = CornerRadius(4.dp.toPx())
//            )
//            // Triangle of bubble
//            drawPath(
//                path = trianglePath,
//                score.colors.color,
//            )
//            drawText(
//                textMeasurer = textMeasure,
//                text = textFirstLine,
//                topLeft = Offset(
//                    (xPos - textFirstLineSize.width / 2f).coerceAtMost(
//                        width.toPx() - infoWidth.div(2)
//                            .toPx() - textFirstLineSize.width / 2f
//                    ),
//                    (yPos - infoHeight.toPx()).coerceAtLeast(0f)
//                )
//            )
//            drawText(
//                textMeasurer = textMeasure,
//                text = textSecondLine,
//                topLeft = Offset(
//                    (xPos - textSecondLineSize.width / 2f).coerceAtMost(
//                        width.toPx() - infoWidth.div(2)
//                            .toPx() - textSecondLineSize.width / 2f
//                    ),
//                    (yPos - infoHeight.toPx()).coerceAtLeast(0f) + textFirstLineSize.height.toFloat()
//                )
//            )
//        }
//
//    }

    Column {
        Row(Modifier.padding(start = 8.dp)) {
            Column {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .height(height + cH.marginTopPlot.times(2))
                        .width(cH.marginLeft)
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
                Spacer(Modifier.size(22.dp))
                Image(
                    painter = painterResource(R.drawable.gauge_high_solid_1),
                    contentDescription = "Sleep details",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }

            Column(
                Modifier
                    .padding(top = cH.marginTopPlot)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(cH.width)
                        .height(height)
                        .background(Color.White)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) selectedPoint = -1
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { tapOffset ->
                                onSelectColumn(tapOffset)
                            })
                        }
                ) {

                    cH.drawGrid(this, pathHorizontal, pathVertical)

                    // avg line
                    drawPath(
                        path = linesSocialAvg, color = score.colors.color,
                        style = Stroke(
                            width = cH.lineStroke.toPx(),
                            cap = StrokeCap.Square,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 10f), 5f)
                        )
                    )
                    drawPath(
                        path = linesTotalAvg, color = MetricViolet,
                        style = Stroke(
                            width = cH.lineStroke.toPx(),
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
                        val thisInfoHeight = textFirstLineSize.height +
                                             textSecondLineSize.height +
                                             textThirdLineSize.height +
                                             padInfo.times(2).toPx()
                        val thisInfoWidth = listOf(textFirstLineSize.width, textSecondLineSize.width, textThirdLineSize.width
                        ).max() + padInfo.times(2).toPx()

                        val xPos = (stepVerticalLines + cH.lineStroke).times(selectedPoint)
                            .toPx() + cH.barWidth.toPx()
                        val yPos = (topTotal.flatten()[selectedPoint].y - 6.dp.toPx())
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
                                (xPos - thisInfoWidth.div(2)).coerceAtLeast(0f).coerceAtMost(cH.width.toPx() - thisInfoWidth),
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
                                (xPos - textFirstLineSize.width / 2f)
                                    .coerceAtLeast(thisInfoWidth.div(2) - textFirstLineSize.width.div(2))
                                    .coerceAtMost(
                                    cH.width.toPx() - textFirstLineSize.width.div(2) - thisInfoWidth.div(2)
                                ),
                                (yPos - textFirstLineSize.height - textSecondLineSize.height - textThirdLineSize.height - padInfo.toPx()).coerceAtLeast(padInfo.toPx())
                            )
                        )
                        // second line
                        drawText(
                            textMeasurer = textMeasure,
                            text = textSecondLine,
                            topLeft = Offset(
                                (xPos - textSecondLineSize.width / 2f)
                                    .coerceAtLeast(thisInfoWidth.div(2) - textSecondLineSize.width.div(2))
                                    .coerceAtMost(
                                    cH.width.toPx() - textSecondLineSize.width.div(2) - thisInfoWidth.div(2)
                                ),
                                (yPos - textSecondLineSize.height - textThirdLineSize.height - padInfo.toPx()).coerceAtLeast(padInfo.toPx())
                            )
                        )
                        
                        // third line
                        drawText(
                            textMeasurer = textMeasure,
                            text = textThirdLine,
                            topLeft = Offset(
                                (xPos - textThirdLineSize.width / 2f)
                                    .coerceAtLeast(thisInfoWidth.div(2) - textThirdLineSize.width.div(2))
                                    .coerceAtMost(
                                    cH.width.toPx() - textThirdLineSize.width.div(2) - thisInfoWidth.div(2)
                                ),
                                (yPos - textThirdLineSize.height - padInfo.toPx()).coerceAtLeast(padInfo.toPx())
                            )
                        )
                        
                    }

                    if (selectedPoint >= 0 && showNoDataInfo){

                        val xPos = (stepVerticalLines + cH.lineStroke).times(selectedPoint)
                            .toPx() + cH.barWidth.toPx()
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
                            size = Size(noDataLineSize.width + 12.dp.toPx(), noDataLineSize.height.toFloat() + 6.dp.toPx()),
                            topLeft = Offset(
                                (xPos - noDataLineSize.width / 2 - 6.dp.toPx()).coerceAtMost(cH.width.toPx() - noDataLineSize.width),
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
                                    cH.width.toPx() - noDataLineSize.width),
                                yPos - noDataLineSize.height - 3.dp.toPx()
                            )
                        )

                    }
                }
                Canvas(
                    modifier = Modifier
                        .width(cH.width)
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

                    for (step in 0 until cH.valuesToPlot.size) {

                        val xP = (stepVerticalLines + cH.lineStroke).times(step)
                            .toPx() + cH.barWidth.toPx()

                        drawText(
                            textMeasurer = textMeasure,
                            text = cH.xLabelsText[step],
                            topLeft = Offset(
                                (xP - xLabelsTextSizes[step].width / 2f).coerceAtMost(cH.width.toPx() - xLabelsTextSizes[step].width),
                                48.dp.toPx() + 12.dp.toPx()
                            )
                        )


                        drawRoundRect(
                            color = if (cH.scoreValuesToPlot.values[step].isNaN()) Color.White else score.colors.color,
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
                                (stepVerticalLines + cH.lineStroke).times(step)
                                    .toPx() + cH.barWidth.toPx() - scoreTextSizes[step].width / 2f,
                                32.dp.toPx() - 1.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        ScreenTimeLegend(score.colors.color, stringResource(legendText))
    }
}



@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdktestapp.charts

import android.graphics.PointF
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantactions.sdktestapp.core_ui.theme.*
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.model.SleepSummary
import com.quantactions.sdk.dropna
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.utils.StringFormatter
import com.quantactions.sdktestapp.core_ui.metrics.toSpanStyle
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.abs


fun Long.localize(): ZonedDateTime {
    return Instant.ofEpochMilli(this * 1000)
        .atZone(ZoneId.systemDefault())
}

fun LocalDate.localize(): ZonedDateTime {
    return ZonedDateTime.of(this, LocalTime.of(0, 0), ZoneId.systemDefault())
}


/**
 * Class the holds the type of charts present in the App.
 * */
enum class Chart(val numValues: Int) {
    WEEK(14),
    MONTH(6),
    YEAR(12)
}

/**
 * Utility function for plotting. Calculates the coordinates in the canvas given a list of numbers
 * to render in the plot.
 * This function returns segments if NaN is present in data
 *
 * @param data list of numbers in the line plot
 * @param width of the canvas
 * @param height of the canvas
 * */
fun calculatePointsForData(
    data: List<Double>,
    width: Float,
    height: Float,
    reverse: Boolean = false
): List<List<PointF>> {

    var points = mutableListOf<PointF>()
    val bottomY = height.times(0.7f)
    val topY = height.times(0.35f)
    val xDiff = width / data.size

    val maxData = data.filter { !it.isNaN() }.maxOrNull()

    val segments = mutableListOf<List<PointF>>()

    val data2 = if (reverse) data.reversed() else data

    for (i in data2.indices) {
        if (!data2[i].isNaN()) {
            val y = bottomY - (data2[i] / maxData!! * bottomY) + topY
            if (reverse) {
                points.add(PointF(xDiff * (data2.lastIndex - i) + xDiff / 2, y.toFloat()))
            } else {
                points.add(PointF(xDiff * i + xDiff / 2, y.toFloat()))
            }
        } else {
            if (points.isNotEmpty()) {
                segments.add(points.toList())
                points = mutableListOf()
            }
        }
    }
    if (points.isNotEmpty()) segments.add(points) // this means no interruptions have been found

    return segments
}

/**
 * Utility function for plotting. Calculates the coordinates in the canvas given a list of numbers
 * to render in the plot.
 * This function returns segments if NaN is present in data
 *
 * @param data list of numbers in the line plot
 * @param width of the canvas
 * @param height of the canvas
 * */
fun calculatePointsForDataGeneral(
    data: List<Double>,
    width: Float,
    height: Float,
    reverse: Boolean = false,
    maxVal: Float,
    minVal: Float,
    horizontalBias: Float = 0f,
    includeOutOfChartLeft: Boolean = true,
    fromTop: Boolean = false
): List<List<PointF>> {

    var points = mutableListOf<PointF>()
    val xDiff = (width - horizontalBias * 2) / (data.size - 1)
    val segments = mutableListOf<List<PointF>>()

    val data2 = if (reverse) data.reversed() else data

    for (i in data2.indices) {
        if (!data2[i].isNaN()) {
            val y = if (fromTop)
                ((data2[i] - minVal) / maxVal * height)
            else
                height - ((data2[i] - minVal) / maxVal * height)
            if (reverse) {
                points.add(
                    PointF(
                        xDiff * (data2.lastIndex - i) + horizontalBias,
                        (y + y * 0.03).toFloat()
                    )
                )
                if (i == data2.lastIndex && includeOutOfChartLeft) points.add(
                    PointF(
                        0f,
                        y.toFloat()
                    )
                )
            } else {
                if (i == 0 && includeOutOfChartLeft) points.add(
                    PointF(
                        0f,
                        (y + y * 0.03).toFloat()
                    )
                )
                points.add(PointF(xDiff * i + horizontalBias, y.toFloat()))
            }
        } else {
            if (points.isNotEmpty()) {
                segments.add(points.toList())
                points = mutableListOf()
            }
        }
    }
    if (points.isNotEmpty()) segments.add(points) // this means no interruptions have been found

    return segments
}

/**
 * Utility function for plotting. Calculates the coordinates in the canvas given a list of numbers
 * to render in the plot.
 * This function returns segments if NaN is present in data
 *
 * @param data list of numbers in the line plot
 * @param width of the canvas
 * @param height of the canvas
 * */
fun calculatePointsForDataGeneralFlat(
    data: List<Double>,
    width: Float,
    height: Float,
    reverse: Boolean = false,
    maxVal: Float,
    minVal: Float,
    horizontalBias: Float = 0f,
    includeOutOfChartLeft: Boolean = true,
): List<PointF> {

    val points = mutableListOf<PointF>()
    val xDiff = (width - horizontalBias * 2) / (data.size - 1)

    val data2 = if (reverse) data.reversed() else data

    for (i in data2.indices) {
        val y = height - ((data2[i] - minVal) / maxVal * height)
        if (reverse) {
            points.add(
                PointF(
                    xDiff * (data2.lastIndex - i) + horizontalBias,
                    (y + y * 0.03).toFloat()
                )
            )
            if (i == data2.lastIndex && includeOutOfChartLeft) points.add(PointF(0f, y.toFloat()))
        } else {
            if (i == 0 && includeOutOfChartLeft) points.add(PointF(0f, (y + y * 0.03).toFloat()))
            points.add(PointF(xDiff * i + horizontalBias, y.toFloat()))
        }
    }
    return points
}

/**
 * Function to calculate Bezier interpolation points in the canvas for each of the points ion the
 * line plot.
 * @param points in canvas coordinates of the line plot to be rendered
 * */
fun calculateConnectionPointsForBezierCurve(points: List<PointF>): Pair<List<PointF>, List<PointF>> {
    val conPoint1 = mutableListOf<PointF>()
    val conPoint2 = mutableListOf<PointF>()
    try {
        for (i in 1 until points.size) {
            conPoint1.add(PointF((points[i].x + points[i - 1].x) / 2, points[i - 1].y))
            conPoint2.add(PointF((points[i].x + points[i - 1].x) / 2, points[i].y))
        }
    } catch (_: Exception) {
    }
    return Pair(conPoint1, conPoint2)
}

fun <T : Comparable<T>> Iterable<T>.argMin(): Int? {
    return withIndex().minByOrNull { it.value }?.index
}

fun <T : Comparable<T>> Iterable<T>.argMax(): Int? {
    return withIndex().maxByOrNull { it.value }?.index
}

fun findClosest(values: List<Float>, value: Float): Int? {
    return values.map { abs(it - value) }.argMin()
}


fun mapTimeToPlot(time: ZonedDateTime, referenceTime: ZonedDateTime): Long {
    return time.toEpochSecond() - referenceTime.toEpochSecond()
}

fun mapTimeToPlot(time: LocalDateTime, referenceTime: LocalDateTime): Long {
    return time.toEpochSecond(ZoneOffset.UTC) - referenceTime.toEpochSecond(ZoneOffset.UTC)
}

fun findMinSleepStart(sleepSummaries: TimeSeries<SleepSummary>): Long {
    // The problem here is that the reference day for a periodic mean is ill defined
    val minIndex =
        sleepSummaries.values.zip(sleepSummaries.timestamps).map { (sleepSummary, time) ->
            sleepSummary.sleepStart.toEpochSecond() - time.toEpochSecond()
        }.argMin()

    // if null I return 9pm
    return if (minIndex == null) -2L * 3600 else
        sleepSummaries.values[minIndex].sleepStart.truncatedTo(ChronoUnit.HOURS).minusHours(2)
            .toEpochSecond() - sleepSummaries.timestamps[minIndex].toEpochSecond()
}

fun findMaxSleepEnd(sleepSummaries: TimeSeries<SleepSummary>): Long {
    val maxIndex =
        sleepSummaries.values.zip(sleepSummaries.timestamps).map { (sleepSummary, time) ->
            sleepSummary.sleepEnd.toEpochSecond() - time.toEpochSecond()
        }.argMax()
    // if null I return 8am
    return if (maxIndex == null) 9L * 3600 else
        sleepSummaries.values[maxIndex].sleepEnd.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            .toEpochSecond() - sleepSummaries.timestamps[maxIndex].toEpochSecond()
}

data class SleepLength(val hours: Int, val minutes: Int)

fun sleepLength(
    sleepStartInitial: ZonedDateTime,
    sleepStop: ZonedDateTime
): SleepLength {
    // NOTE: When coming from averages for example over weeks these values have the same date so if
    // the average sleep time is 23 and wake time is 8 then the difference is -15.
    val sleepStart =
        if (sleepStartInitial > sleepStop) sleepStartInitial.minusDays(1) else sleepStartInitial
    val diffHours = ChronoUnit.HOURS.between(sleepStart, sleepStop).toInt()
    val diffMinutes = ChronoUnit.MINUTES.between(sleepStart, sleepStop).toInt() - diffHours * 60
    return SleepLength(diffHours, diffMinutes)
}

fun lengthFromSleepWake(
    sleepStart: ZonedDateTime,
    sleepStop: ZonedDateTime,
    referenceTime: ZonedDateTime
): Pair<Long, Long> {
    return Pair(mapTimeToPlot(sleepStop, referenceTime), mapTimeToPlot(sleepStart, referenceTime))
}


fun fillAndTakeValue(chartType: Chart): Int {
    return when (chartType) {
        Chart.WEEK -> Chart.WEEK.numValues
        Chart.MONTH -> Chart.MONTH.numValues * 7
        Chart.YEAR -> 366
    }
}

@JvmName("prepareTimeSeriesDouble")
fun prepareAndAggregateTimeSeries(
    timeSeries: TimeSeries.DoubleTimeSeries,
    chartType: Chart,
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    return timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake).values.dropna().average()
}

fun prepareAndAggregateTrend(
    timeSeries: TimeSeries.TrendTimeSeries,
    chartType: Chart,
    ignoreSignificance: Boolean = true,
    dropna: Boolean = false
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    val readyTrendHolder = timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake)
    val droppedTrendHolder = if (dropna) readyTrendHolder.dropna() else readyTrendHolder

    if (droppedTrendHolder.values.isEmpty()) return 0.0

    return when (chartType) {
        Chart.WEEK -> if (droppedTrendHolder.values.last().significance2Weeks != 0.0 || ignoreSignificance) droppedTrendHolder.values.last().difference2Weeks else 0.0
        Chart.MONTH -> if (droppedTrendHolder.values.last().significance6Weeks != 0.0 || ignoreSignificance) droppedTrendHolder.values.last().difference6Weeks else 0.0
        Chart.YEAR -> if (droppedTrendHolder.values.last().significance1Year != 0.0 || ignoreSignificance) droppedTrendHolder.values.last().difference1Year else 0.0
    }
}

fun prepareAndAggregateTheWave(
    timeSeries: TimeSeries.TrendTimeSeries,
    chartType: Chart,
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    val readyTrendHolder = timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake)
    return when (chartType) {
        Chart.WEEK -> readyTrendHolder.values.last().statistic2Weeks
        Chart.MONTH -> readyTrendHolder.values.last().statistic6Weeks
        Chart.YEAR -> readyTrendHolder.values.last().statistic1Year
    }
}

fun prepareAndAggregateSleepLength(
    timeSeries: TimeSeries.SleepSummaryTimeTimeSeries,
    chartType: Chart
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    return timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake)
        .dropna().values.map { ChronoUnit.MILLIS.between(it.sleepStart, it.sleepEnd).toDouble() }
        .average()
}

fun prepareAndAggregateSleepInterruptions(
    timeSeries: TimeSeries.SleepSummaryTimeTimeSeries,
    chartType: Chart
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    return timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake)
        .dropna().values.map { it.interruptionsNumberOfTaps.size }
        .average()
}

fun prepareAndAggregateTotalScreenTime(
    timeSeries: TimeSeries.ScreenTimeAggregateTimeSeries,
    chartType: Chart
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    return timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake)
        .dropna().values.map { it.totalScreenTime }.average()
}

fun prepareAndAggregateSocialScreenTime(
    timeSeries: TimeSeries.ScreenTimeAggregateTimeSeries,
    chartType: Chart
): Double {
    val fillAndTake = fillAndTakeValue(chartType)
    return timeSeries.fillMissingDays(fillAndTake).takeLast(fillAndTake)
        .dropna().values.map { it.socialScreenTime }.average()
}

class BasicChart<T>(
    val timeSeries: TimeSeries<T>,
    val scoreTimeSeries: TimeSeries.DoubleTimeSeries,
    val score: Score,
    val selectedPoint: Int,
    val chartType: Chart,
    val marginLeft: Dp,
    val marginRight: Dp,
    val marginTopPlot: Dp,
    val screenWidth: Dp,
    val highlightXLabelMinus1: Boolean = false,
    val weekString: String
) {

    val nVerticalLines = chartType.numValues
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
    private val basicDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(StringFormatter.BasicDate.pattern)
    val width = screenWidth - marginRight - marginLeft
    val lineStroke = 1.dp
    val barWidth: Dp

    val valuesToPlot: TimeSeries<T>
    val scoreValuesToPlot: TimeSeries.DoubleTimeSeries
    val xLabelsText: List<AnnotatedString>
    val times: TimeSeries.DoubleTimeSeries
    val horizontalBias: Dp

    // other
    var nHorizontalLines = 0
    var height = 0.dp
    var stepVerticalLines = 0.dp
    var stepHorizontalLines = 0.dp
    // -------------------------------

    fun setHeight(nHorizontalLines: Int, height: Dp){
        this.nHorizontalLines = nHorizontalLines
        this.height = height
        stepVerticalLines = (width - barWidth.times(2) - lineStroke.times(nVerticalLines - 1)).div(nVerticalLines - 1)
        stepHorizontalLines = (height - lineStroke.times(nHorizontalLines - 1)).div(nHorizontalLines - 1)
    }


    init {
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

                horizontalBias = 8.dp
                barWidth = 8.dp

                // x labels
                xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = if (i == (selectedPoint - if (highlightXLabelMinus1) 1 else 0)) score.colors.color else {
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
                horizontalBias = 20.dp
                // x labels
                xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = if (i == selectedPoint - if (highlightXLabelMinus1) 1 else 0) score.colors.color else ColdGrey04,
                                fontSize = TP.regular.overline.fontSize,
                                fontStyle = TP.regular.overline.fontStyle,
                                fontWeight = TP.regular.overline.fontWeight
                            )
                        ) {
                            append(
                                String.format(weekString,
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

                scoreValuesToPlot =
                    scoreTimeSeries.fillMissingDays(366)
                        .extractMonthlyAverages()
                        .takeLast(Chart.YEAR.numValues)


//                scoreValuesToPlot =
//                    scoreTimeSeries.fillMissingDays(366).extractMonthlyAverages()
//                        .takeLast(Chart.YEAR.numValues)
                times = TimeSeries.DoubleTimeSeries().fillMissingDays(366).extractMonthlyAverages()
                    .takeLast(Chart.YEAR.numValues)

                horizontalBias = 8.dp
                barWidth = 10.dp
                // x labels
                xLabelsText = valuesToPlot.timestamps.mapIndexed { i, timestamp ->
                    buildAnnotatedString {
                        withStyle(
                            style = TP.regular.overline.toSpanStyle(if (i == selectedPoint - if (highlightXLabelMinus1) 1 else 0) score.colors.color else ColdGrey04)
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
    }

    fun drawGrid(
        drawScope: DrawScope,
        pathHorizontal: Path,
        pathVertical: Path,
    ) {
        with(drawScope) {
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
        }

    }


}
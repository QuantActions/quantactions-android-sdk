package com.quantactions.sdktestapp.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.quantactions.sdktestapp.core_ui.theme.Brand
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey04
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey05
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey10
import com.quantactions.sdktestapp.core_ui.theme.MetricViolet
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.R


@Preview
@Composable
fun MetricsLegend(
    score: Score = Score.SLEEP_SCORE,
    checkedState: Boolean = true,
    showUncertainty: Boolean = true,
    changeCheckedState: (Boolean) -> Unit = {},
    showTooltipGeneralPopulation: () -> Unit = {},
    showTooltipUncertainty: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.size(36.dp))
                MetricLegendLine(score.colors.color)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.graph_legend_your_score),
                    style = TP.regular.body2,
                    color = ColdGrey10
                )
            }
            if (showUncertainty) {
                Spacer(Modifier.size(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        modifier = Modifier.size(20.dp),
                        checked = checkedState,
                        onCheckedChange = { changeCheckedState(it) },
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = Brand,
                            checkedColor = Brand
                        )
                    )
                    Spacer(Modifier.size(16.dp))
                    CILegendLine(score.colors.lightColor)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.graph_legend_uncertainty_of_your_score),
                        style = TP.regular.body2,
                        color = ColdGrey10
                    )
                    Spacer(Modifier.size(8.dp))
                    Icon(
                        painterResource(R.drawable.questionmark),
                        tint = ColdGrey05,
                        contentDescription = "",
                        modifier = Modifier.clickable { showTooltipUncertainty() }
                    )
                }
                Spacer(Modifier.size(2.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.size(36.dp))
                RangeLegendLine()
                Spacer(Modifier.size(10.dp))

                Text(
                    text = stringResource(R.string.graph_legend_general_population),
                    style = TP.regular.body2,
                    color = ColdGrey10,
                    modifier = Modifier.weight(weight = 1F, fill = false),
                )
                Spacer(Modifier.size(8.dp))
                Icon(
                    painterResource(R.drawable.questionmark),
                    tint = ColdGrey05,
                    contentDescription = "",
                    modifier = Modifier.clickable { showTooltipGeneralPopulation() }
                )
            }
        }
    }
}


@Preview
@Composable
fun ScreenTimeLegend(
    color: Color = Score.SOCIAL_ENGAGEMENT.colors.color,
    text: String = stringResource(R.string.graph_legend_general_population),
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                DoubleLegend(MetricViolet, color)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.total_screen_time),
                    style = TP.regular.body2,
                    color = ColdGrey10
                )
            }
            Spacer(Modifier.size(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxLegend(color)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.social_screen_time),
                    style = TP.regular.body2,
                    color = ColdGrey10
                )
            }
            Spacer(Modifier.size(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                RangeLegendLine()
                Spacer(Modifier.size(10.dp))
                Text(
                    text = text,
                    style = TP.regular.body2,
                    color = ColdGrey10
                )
            }

        }
    }
}

/**
 * Legend line metric
 * */
@Preview
@Composable
fun MetricLegendLine(
    color: Color = Score.SLEEP_SCORE.colors.color,
    filled: Boolean = false
) {
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .aspectRatio(1f)
    ) {

        val path2 = Path().apply {
            moveTo(0.dp.toPx(), 10.dp.toPx())
            lineTo(20.dp.toPx(), 10.dp.toPx())
        }
        drawPath(
            path = path2,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Square)
        )
        drawCircle(radius = 5.dp.toPx(), color = color)
        if (!filled)
            drawCircle(radius = 3.dp.toPx(), color = Color.White)
    }
}




@Preview
@Composable
fun DashedLineLegend(
    color: Color = Score.SLEEP_SCORE.colors.color,
    text: String = stringResource(R.string.graph_legend_general_population),
    onQuestionMarkClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
    ) {
        RangeLegendLine(color)
        Spacer(Modifier.size(10.dp))
        Text(
            text = text,
            style = TP.regular.body2,
            color = ColdGrey10
        )
        Spacer(Modifier.size(8.dp))
        onQuestionMarkClick?.let {
            Icon(
                painterResource(R.drawable.questionmark),
                tint = ColdGrey05,
                contentDescription = "",
                modifier = Modifier.clickable { it() }
            )
        }

    }
}

/**
 * Legend CI line metric
 * */
@Preview
@Composable
fun CILegendLine(color: Color = Score.SLEEP_SCORE.colors.color) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(16.dp)
            .background(color)
    ) {}
}

/**
 * Legend CI line metric
 * */
@Preview
@Composable
fun BoxLegend(color: Color = Score.SLEEP_SCORE.colors.color) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(16.dp)
            .background(color)
    ) {}
}

/**
 * Legend CI line metric
 * */
@Preview
@Composable
fun DoubleLegend(
    color1: Color = Score.SLEEP_SCORE.colors.color,
    color2: Color = Score.COGNITIVE_FITNESS.colors.color
) {
    Column {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(8.dp)
                .background(color1)
        )
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(8.dp)
                .background(color2)
        )
    }
}


/**
 * Legend line population
 * */
@Preview
@Composable
fun RangeLegendLine(color: Color = ColdGrey04) {
    
        Canvas(
            Modifier
                .size(20.dp)) {

            val path = Path().apply {
                moveTo(0f, 10.dp.toPx())
                lineTo(20.dp.toPx(), 10.dp.toPx())
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 1.dp.toPx(),
                    cap = StrokeCap.Square,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()), 0f)
                )
            )
        }

}

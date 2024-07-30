package com.quantactions.sdktestapp.core_ui.metrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantactions.sdktestapp.MetricColor
import com.quantactions.sdktestapp.R
import com.quantactions.sdktestapp.Score
import com.quantactions.sdktestapp.core_ui.theme.Brand
import com.quantactions.sdktestapp.core_ui.theme.BrandLight1
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey01
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey02
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey05
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey06
import com.quantactions.sdktestapp.core_ui.theme.ColdGrey10
import com.quantactions.sdktestapp.core_ui.theme.TP
import kotlin.math.roundToInt
import kotlin.math.sqrt


///////////////////////// COMPONENTS /////////////////////////
/**
 * The linear progress bar showing the ETA for the metric to be ready
 * @param percentCompleteReal how much in percentage the metric is ready
 * */
@Preview
@Composable
fun MetricETA(percentCompleteReal: Float = 15f) {

    val percentComplete = percentCompleteReal.coerceAtLeast(33f).coerceAtMost(99f)

    // It remembers the data usage value
    var dataUsageRemember by remember {
        mutableStateOf(-1f)
    }
    // This is to animate the foreground indicator
    val dataUsageAnimate = animateFloatAsState(
        targetValue = dataUsageRemember,
        animationSpec = tween(
            durationMillis = 1000
        )
    )
    // This is to start the animation when the activity is opened
    LaunchedEffect(Unit) {
        dataUsageRemember = percentComplete
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(23.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(15.dp))
                .height(23.dp)
                .background(ColdGrey02)
        ) {
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(15.dp))
                    .height(23.dp)
                    .background(ColdGrey05)
                    .fillMaxWidth(dataUsageAnimate.value / 100),
                contentAlignment = Alignment.Center
            ) {

            }
        }
        Row(
            modifier = Modifier.height(23.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${percentComplete.roundToInt()}%",
                color = White,
                style = TP.medium.caption,
                textAlign = TextAlign.Left,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/**
 * Circular indicator placeholder fixed at 75%.
 * */
@Preview
@Composable
fun PlaceholderCircularIndicator() {

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
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .background(White)
        ) {
            // Circle 1
            drawCircle(
                color = ColdGrey01,
                radius = size.toPx() / 2,
                center = Offset(x = this.size.width / 2, y = this.size.height / 2)
            )
            // Circle 2
            drawCircle(
                color = White,
                style = Stroke(width = ringThickness.toPx()),
                radius = ringRadius.toPx(),
                center = Offset(x = this.size.width / 2, y = this.size.height / 2)
            )
        }
        ProgressWithGradient(
            75.0, size, ColdGrey05, ColdGrey01,
            indicatorThickness, indicatorRadius, indicatorOffset
        )
    }
}

/**
 * The full row of placeholder that has the indicator the name of tye metric and ETA bar
 * @param displayName of the score
 * @param completed Percentage of completeness.
 * the metadata for the metric
 * */
@Preview
@Composable
fun MetricRowPlaceholderV2(
    displayName: String = stringResource(id = R.string.cognitive_fitness),
    colors: MetricColor = Score.COGNITIVE_FITNESS.colors,
    daysETA: Int = 3,
    completed: Float = 15f
) {

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = 10.dp,
        modifier = Modifier.padding(8.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(9.dp)
                    .background(color = White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    MetricCircularIndicator(
                        colors = colors,
                    )
                }
                Spacer(Modifier.width(24.dp))
                Column(
                    Modifier
                        .fillMaxHeight()
                        .padding(end = 16.dp), verticalArrangement = Arrangement.Center
                ) {
                    BoxWithConstraints {
                        Text(
                            text = displayName,
                            color = ColdGrey10,
                            style = TP.light.h1,
                            fontSize = if (maxWidth < 200.dp) TP.light.h1.fontSize.times(
                                0.8
                            ) else TP.light.h1.fontSize
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (daysETA > 0)
                            pluralStringResource(
                                id = R.plurals.placeholder_score_ready_in,
                                count = daysETA,
                                daysETA
                            )
                        else stringResource(R.string.we_are_almost_ready),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        color = ColdGrey06,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Left
                    )
                    Spacer(modifier = Modifier.height(11.dp))
                    MetricETA(completed)
                }
            }
        }

    }
}

/////////////////////////  SCREENS   /////////////////////////

/**
 * Placeholder Screen shown in the Relations Tab when the metrics are not ready. Has button that
 * redirects to onboarding course.
 * */
@Composable
fun LearningPhaseRelationsTab(
    isCourseStarted: Boolean,
    isCourseDone: Boolean,
    goToCourse: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(White)
                .padding(top = 32.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                stringResource(R.string.placeholder_feature_available_future),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                style = TP.regular.body1, color = ColdGrey06
            )
            if (!isCourseDone) {
                Text(
                    stringResource(R.string.placeholder_find_out_tutorial),
                    modifier = Modifier.fillMaxWidth(),
                    style = TP.regular.body1, color = ColdGrey06
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        disabledBackgroundColor = BrandLight1,
                        backgroundColor = Brand,
                        contentColor = White
                    ),
                    onClick = { goToCourse() },
                    modifier = Modifier
                        .width(250.dp)
                        .height(48.dp)
                        .padding(start = 16.dp, end = 16.dp)
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .fillMaxWidth()
                ) {
                    Text(
                        if (isCourseStarted) stringResource(R.string.placeholder_resume_the_tutorial)
                        else stringResource(R.string.placeholder_see_the_tutorial),
                        style = TP.medium.body1,
                        color = White
                    )
                }
            }
        }
    }
}

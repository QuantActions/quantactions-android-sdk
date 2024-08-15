/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp.core_ui.metrics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantactions.sdktestapp.Score
import kotlin.math.sqrt


@Composable
@Preview
fun ProgressWithGradientRotation(
    size: Dp = 232.dp,
    colorDark: Color = Score.COGNITIVE_FITNESS.colors.color,
    colorLight: Color = Score.COGNITIVE_FITNESS.colors.lightColor,
    indicatorThickness: Dp = 18.dp,
    indicatorRadius: Dp = 78.dp + indicatorThickness / 2,
    indicatorOffset: Dp = 20.dp.times(sqrt(2.0).toFloat()),
) {

    // Allow resume on rotation
    var currentRotation by remember { mutableFloatStateOf(0f) }

    val rotation = remember { Animatable(currentRotation) }


    // This is to start the animation when the activity is opened
    LaunchedEffect(Unit) {
        // Infinite repeatable rotation when is playing
        rotation.animateTo(
            targetValue = currentRotation + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        ) {
            currentRotation = value
        }
    }

    Canvas(
        modifier = Modifier
            .size(size)
            .background(Color.Transparent)
            .rotate(rotation.value)
    ) {
        val outputEnd = 98
        val outputStart = 0
        val inputEnd = 100
        val inputStart = 0
        val slope = 1.0f * (outputEnd - outputStart) / (inputEnd - inputStart)

        // Convert the dataUsage to angle
        val ratio = (outputStart + slope * (75.0f - inputStart))
        val sweepAngle = ratio * 360 / 100

        val colorStops = listOf(
            0.01f to colorLight,
            ratio / 100 to colorDark,
        ).toTypedArray()

        // Foreground indicator
        drawArc(
            brush = Brush.sweepGradient(
                colorStops = colorStops
            ),

            startAngle = 7f,
            sweepAngle = sweepAngle - 7,
            useCenter = false,
            style = Stroke(width = indicatorThickness.toPx(), cap = StrokeCap.Round),
            size = Size(
                width = (indicatorRadius * 2).toPx(),
                height = (indicatorRadius * 2).toPx()
            ),
            topLeft = Offset(
                x = indicatorOffset.toPx(),
                y = indicatorOffset.toPx()
            )
        )

    }
}

@Composable
@Preview
fun ProgressWithGradient(
    lastScore: Double = 75.0,
    size: Dp = 232.dp,
    colorDark: Color = Score.COGNITIVE_FITNESS.colors.color,
    colorLight: Color = Score.COGNITIVE_FITNESS.colors.lightColor,
    indicatorThickness: Dp = 18.dp,
    indicatorRadius: Dp = 78.dp + indicatorThickness / 2,
    indicatorOffset: Dp = 20.dp.times(sqrt(2.0).toFloat()),
) {

    // It remembers the data usage value
    var dataUsageRemember by remember {
        mutableDoubleStateOf(-1.0)
    }

    // This is to animate the foreground indicator
    val dataUsageAnimate = animateFloatAsState(
        targetValue = dataUsageRemember.toFloat(),
        animationSpec = tween(
            2000, easing = EaseOutCubic
        ), label = "loading"
    )

    // This is to start the animation when the activity is opened
    LaunchedEffect(Unit) {
        // Infinite repeatable rotation when is playing
        dataUsageRemember = if (!lastScore.isNaN() && lastScore != 75.0)
            lastScore
        else -1.0
    }

    Canvas(
        modifier = Modifier
            .size(size)
            .background(Color.Transparent)
            .rotate(-90.0f)
    ) {
        val outputEnd = 98
        val outputStart = 0
        val inputEnd = 100
        val inputStart = 0
        val slope = 1.0f * (outputEnd - outputStart) / (inputEnd - inputStart)

        // Convert the dataUsage to angle
        val ratio = (outputStart + slope * (dataUsageAnimate.value - inputStart))
        val sweepAngle = ratio * 360 / 100

        val colorStops = listOf(
            0.01f to colorLight,
            ratio / 100 to colorDark,
        ).toTypedArray()

        if (sweepAngle > 0) {
            // Foreground indicator
            drawArc(
                brush = Brush.sweepGradient(
                    colorStops = colorStops
                ),

                startAngle = 7f,
                sweepAngle = sweepAngle - 7,
                useCenter = false,
                style = Stroke(width = indicatorThickness.toPx(), cap = StrokeCap.Round),
                size = Size(
                    width = (indicatorRadius * 2).toPx(),
                    height = (indicatorRadius * 2).toPx()
                ),
                topLeft = Offset(
                    x = indicatorOffset.toPx(),
                    y = indicatorOffset.toPx()
                )
            )
        }
    }
}

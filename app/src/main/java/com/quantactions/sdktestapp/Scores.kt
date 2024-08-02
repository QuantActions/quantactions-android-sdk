/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import androidx.compose.ui.graphics.Color
import com.quantactions.sdktestapp.core_ui.theme.MetricBlue
import com.quantactions.sdktestapp.core_ui.theme.MetricLightBlue
import com.quantactions.sdktestapp.core_ui.theme.MetricLightPink
import com.quantactions.sdktestapp.core_ui.theme.MetricLightTurquoise
import com.quantactions.sdktestapp.core_ui.theme.MetricPink
import com.quantactions.sdktestapp.core_ui.theme.MetricTurquoise
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.Metric
import com.quantactions.sdk.Range

/**
 * Enumeration class that holds all of the info for the metrics
 * @property id name of the metric
 * @property code to get it from TIE (e.g. XXX-XXX-XXX-XXX)
 * @property range
 * @property colors
 * */
enum class Score(
    override val id: String,
    override val code: String,
    val eta: Int,
    override val colors: MetricColor,
    val scoreTrend: ScoreTrend,
    val getReferencePopulationRange: (BasicInfo) -> Range,
) : WatchableScoreOrTrend {
    COGNITIVE_FITNESS(
        Metric.COGNITIVE_FITNESS.id,
        Metric.COGNITIVE_FITNESS.code,
        Metric.COGNITIVE_FITNESS.eta,
        MetricColor(MetricTurquoise, MetricLightTurquoise),
        scoreTrend = ScoreTrend.COGNITIVE_FITNESS,
        getReferencePopulationRange = { basicInfo ->
            Metric.COGNITIVE_FITNESS.getReferencePopulationRange(
                basicInfo
            )
        },
    ),
    SOCIAL_ENGAGEMENT(Metric.SOCIAL_ENGAGEMENT.id,
        Metric.SOCIAL_ENGAGEMENT.code,
        Metric.SOCIAL_ENGAGEMENT.eta,
        MetricColor(MetricPink, MetricLightPink),
        scoreTrend = ScoreTrend.SOCIAL_ENGAGEMENT,
        getReferencePopulationRange = { basicInfo ->
            Metric.SOCIAL_ENGAGEMENT.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    ACTION_SPEED(
        Metric.ACTION_SPEED.id,
        Metric.ACTION_SPEED.code,
        Metric.ACTION_SPEED.eta,
        MetricColor(MetricTurquoise, MetricLightTurquoise),
        scoreTrend = ScoreTrend.ACTION_SPEED,
        getReferencePopulationRange = { basicInfo ->
            Metric.ACTION_SPEED.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    TYPING_SPEED(
        Metric.TYPING_SPEED.id,
        Metric.TYPING_SPEED.code,
        Metric.TYPING_SPEED.eta,
        MetricColor(MetricTurquoise, MetricLightTurquoise),
        scoreTrend = ScoreTrend.TYPING_SPEED,
        getReferencePopulationRange = { basicInfo ->
            Metric.ACTION_SPEED.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    SLEEP_SUMMARY(Metric.SLEEP_SUMMARY.id,
        Metric.SLEEP_SUMMARY.code,
        Metric.SLEEP_SUMMARY.eta,
        MetricColor(MetricBlue, MetricLightBlue),
        scoreTrend = ScoreTrend.SLEEP_LENGTH,
        getReferencePopulationRange = { basicInfo ->
            Metric.SLEEP_SCORE.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    SLEEP_INTERRUPTIONS(Metric.SLEEP_SUMMARY.id,
        Metric.SLEEP_SUMMARY.code,
        Metric.SLEEP_SUMMARY.eta,
        MetricColor(MetricBlue, MetricLightBlue),
        scoreTrend = ScoreTrend.SLEEP_INTERRUPTIONS,
        getReferencePopulationRange = { basicInfo ->
            Metric.SLEEP_SCORE.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    SLEEP_SCORE(Metric.SLEEP_SCORE.id,
        Metric.SLEEP_SCORE.code,
        Metric.SLEEP_SCORE.eta,
        MetricColor(MetricBlue, MetricLightBlue),
        scoreTrend = ScoreTrend.SLEEP_SCORE,
        getReferencePopulationRange = { basicInfo ->
            Metric.SLEEP_SCORE.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    SCREEN_TIME_AGGREGATE(Metric.SCREEN_TIME_AGGREGATE.id,
        Metric.SCREEN_TIME_AGGREGATE.code,
        Metric.SCREEN_TIME_AGGREGATE.eta,
        MetricColor(MetricPink, MetricLightPink),
        scoreTrend = ScoreTrend.SOCIAL_SCREEN_TIME,
        getReferencePopulationRange = { basicInfo ->
            Metric.SOCIAL_ENGAGEMENT.getReferencePopulationRange(
                basicInfo
            )
        }
    ),
    SOCIAL_TAPS(
        Metric.SOCIAL_TAPS.id,
        Metric.SOCIAL_TAPS.code,
        Metric.SOCIAL_TAPS.eta,
        MetricColor(MetricPink, MetricLightPink),
        scoreTrend = ScoreTrend.SOCIAL_TAPS,
        getReferencePopulationRange = { basicInfo ->
            Metric.SOCIAL_ENGAGEMENT.getReferencePopulationRange(
                basicInfo
            )
        }
    )
}


/**
 * Extension fun to get the Metric instance form the its id.
 * @param id id of the metric we are trying to retrieve
 * */
fun from(id: String): Score {
    return enumValues<Score>().find { it.id == id }!!
}



/**
 * Simple class that holds a color and it's light version for a metric branding.
 * */
class MetricColor(val color: Color, val lightColor: Color)

interface WatchableScoreOrTrend {
    val id: String
    val code: String
    val colors: MetricColor
}

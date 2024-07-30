package com.quantactions.sdktestapp

import com.quantactions.sdktestapp.core_ui.theme.MetricBlue
import com.quantactions.sdktestapp.core_ui.theme.MetricLightBlue
import com.quantactions.sdktestapp.core_ui.theme.MetricLightPink
import com.quantactions.sdktestapp.core_ui.theme.MetricLightTurquoise
import com.quantactions.sdktestapp.core_ui.theme.MetricPink
import com.quantactions.sdktestapp.core_ui.theme.MetricTurquoise
import com.quantactions.sdk.Trend

/**
 * Enumeration class that holds all of the info for the metrics
 * @property id name of the metric
 * @property code to get it from TIE (e.g. XXX-XXX-XXX-XXX)
 * */
enum class ScoreTrend(
    override val id: String,
    override val code: String,
    override val colors: MetricColor,
) : WatchableScoreOrTrend {
    COGNITIVE_FITNESS(
        Trend.COGNITIVE_FITNESS.id,
        Trend.COGNITIVE_FITNESS.code,
        MetricColor(MetricTurquoise, MetricLightTurquoise),
    ),
    ACTION_SPEED(
        Trend.ACTION_SPEED.id,
        Trend.ACTION_SPEED.code,
        MetricColor(MetricTurquoise, MetricLightTurquoise)
    ),
    TYPING_SPEED(
        Trend.TYPING_SPEED.id,
        Trend.TYPING_SPEED.code,
        MetricColor(MetricTurquoise, MetricLightTurquoise)
    ),
    SLEEP_LENGTH(
        Trend.SLEEP_LENGTH.id,
        Trend.SLEEP_LENGTH.code,
        MetricColor(MetricBlue, MetricLightBlue),
    ),
    SLEEP_SCORE(
        Trend.SLEEP_SCORE.id,
        Trend.SLEEP_SCORE.code,
        MetricColor(MetricBlue, MetricLightBlue),
    ),
    SLEEP_INTERRUPTIONS(
        Trend.SLEEP_INTERRUPTIONS.id,
        Trend.SLEEP_INTERRUPTIONS.code,
        MetricColor(MetricBlue, MetricLightBlue),
    ),
    SOCIAL_ENGAGEMENT(
        Trend.SOCIAL_ENGAGEMENT.id,
        Trend.SOCIAL_ENGAGEMENT.code,
        MetricColor(MetricPink, MetricLightPink),
    ),
    SOCIAL_SCREEN_TIME(
        Trend.SOCIAL_SCREEN_TIME.id,
        Trend.SOCIAL_SCREEN_TIME.code,
        MetricColor(MetricPink, MetricLightPink),
    ),
    SOCIAL_TAPS(
        Trend.SOCIAL_TAPS.id,
        Trend.SOCIAL_TAPS.code,
        MetricColor(MetricPink, MetricLightPink),
    ),
    THE_WAVE(
        Trend.THE_WAVE.id,
        Trend.THE_WAVE.code,
        MetricColor(MetricPink, MetricLightPink),
    )
}



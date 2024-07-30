/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import android.app.NotificationManager
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.navigation.NavHostController

import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics

import com.google.firebase.ktx.Firebase
import com.quantactions.sdk.QA
import com.quantactions.sdktestapp.charts.Chart
import com.quantactions.sdktestapp.core_ui.theme.TP
import com.quantactions.sdktestapp.utils.MainColumn

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Main Activity of the App that is launched when one opens the app.
 * */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var qa: QA
    lateinit var navController: NavHostController

    private val metricsViewModel: MetricsViewModel by viewModels()
    private lateinit var mNotificationManager: NotificationManager

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) prepareDebugEnvironment()

        setContent {

            val systemUiController = rememberSystemUiController()
            systemUiController.setSystemBarsColor(
                color = Color.White
            )

            val sleepScoreState by metricsViewModel.mapOfScoresStateFlows[Score.SLEEP_SCORE]!!.collectAsState()
            val cognitiveFitnessState by metricsViewModel.mapOfScoresStateFlows[Score.COGNITIVE_FITNESS]!!.collectAsState()
            val cognitiveFitnessTrendState by metricsViewModel.mapOfScoresStateFlows[ScoreTrend.COGNITIVE_FITNESS]!!.collectAsState()
            val socialEngagementScoreState by metricsViewModel.mapOfScoresStateFlows[Score.SOCIAL_ENGAGEMENT]!!.collectAsState()
            val actionSpeed by metricsViewModel.mapOfScoresStateFlows[Score.ACTION_SPEED]!!.collectAsState()
            val sleepSummaryState by metricsViewModel.mapOfScoresStateFlows[Score.SLEEP_SUMMARY]!!.collectAsState()
            val screenTimeState by metricsViewModel.mapOfScoresStateFlows[Score.SCREEN_TIME_AGGREGATE]!!.collectAsState()

            var selectedChart by remember { mutableStateOf(Chart.WEEK) }

            Column {

                ChartRadioButton(selectedChart = selectedChart) {
                    selectedChart = it
                }

                MainColumn(
                    cognitiveFitness = cognitiveFitnessState,
                    cognitiveFitnessTrend = cognitiveFitnessTrendState,
                    sleepQuality = sleepScoreState,
                    sleepSummary = sleepSummaryState,
                    socialEngagement = socialEngagementScoreState,
                    screenTimeAggregate = screenTimeState,
                    actionSpeed = actionSpeed,
                    currentChart = selectedChart
                )
            }
        }
    }

    private fun prepareDebugEnvironment(strictMode: Boolean = false) {
        Firebase.analytics.setAnalyticsCollectionEnabled(false)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
        if (strictMode) {
            try {
                Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                    .invoke(null, true)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
            StrictMode.enableDefaults()
        }
    }

}

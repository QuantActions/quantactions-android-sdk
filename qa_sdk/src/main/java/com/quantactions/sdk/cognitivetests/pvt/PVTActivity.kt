/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.pvt

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.coroutineScope
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.R
import com.quantactions.sdk.cognitivetests.theme.CognitiveTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * @hide
 */
class PVTActivity : ComponentActivity() {

    private val durations = listOf("3 minutes", "5 minutes", "10 minutes")
    private val durationsMs = listOf(3 * 60 * 1000L, 5 * 60 * 1000L, 10 * 60 * 1000L)

    private var stage by mutableStateOf(PVTUiStage.DURATION_SELECT)
    private var selectedDurationIndex by mutableIntStateOf(0)
    private var isTimerRunning by mutableStateOf(false)
    private var elapsedDisplay by mutableStateOf("0")
    private var feedbackMessage by mutableStateOf<String?>(null)
    private var medianRT by mutableDoubleStateOf(0.0)
    private var isSaved by mutableStateOf(false)

    private var handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0
    private var testStartTime: Long = 0
    private val updateInterval: Long = 10
    private val reactionTimes = mutableListOf<Triple<Long, Long, TrialType>>()
    private var currentWaitingTime: Long = 0
    private var maxTestDuration: Long = (if (BuildConfig.DEBUG) 1 else 3) * 60 * 1000L
    private var noResponseCounter: Int = 0
    private val listOfRandomTimes: MutableList<Long> = mutableListOf()

    private val viewModel: PVTViewModel by viewModels()

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val elapsedTime = System.currentTimeMillis() - startTime
                elapsedDisplay = "$elapsedTime"
                if (elapsedTime >= 9999) {
                    elapsedDisplay = getString(R.string._9999)
                    feedbackMessage = getString(R.string.too_slow)
                    noResponseCounter += 1
                    isTimerRunning = false
                    handler.postDelayed({
                        feedbackMessage = null
                        startNextTrial()
                    }, 1000)
                } else {
                    handler.postDelayed(this, updateInterval)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CognitiveTestTheme(darkTheme = true) {
                PVTScreen(
                    stage = stage,
                    selectedDurationIndex = selectedDurationIndex,
                    durations = durations,
                    onSelectDurationIndex = { selectedDurationIndex = it },
                    onConfirmDuration = {
                        maxTestDuration = durationsMs[selectedDurationIndex]
                        stage = PVTUiStage.INSTRUCTIONS
                    },
                    onStartTest = {
                        testStartTime = System.currentTimeMillis()
                        stage = PVTUiStage.RUNNING
                        startNextTrial()
                    },
                    onBack = {
                        when (stage) {
                            PVTUiStage.DURATION_SELECT -> finish()
                            PVTUiStage.INSTRUCTIONS -> stage = PVTUiStage.DURATION_SELECT
                            PVTUiStage.RUNNING -> finish()
                            PVTUiStage.SAVING_OR_FINISHED -> finish()
                        }
                    },
                    isTimerRunning = isTimerRunning,
                    elapsedDisplay = elapsedDisplay,
                    feedbackMessage = feedbackMessage,
                    onReactDown = { react() },
                    medianRT = medianRT,
                    isSaved = isSaved,
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun startNextTrial() {
        if (System.currentTimeMillis() - testStartTime >= maxTestDuration) {
            showResults()
            return
        }

        feedbackMessage = null
        currentWaitingTime = Random.nextLong(0, 3000)
        listOfRandomTimes.add(currentWaitingTime)
        handler.postDelayed({
            startTime = System.currentTimeMillis()
            isTimerRunning = true
            elapsedDisplay = "0"
            feedbackMessage = null
            handler.post(updateTimerRunnable)
        }, currentWaitingTime)
    }

    private fun showResults() {
        if (isFinishing || isDestroyed) {
            Log.w("PVTActivity", "Activity is finishing or destroyed; skipping showResults")
            return
        }

        isTimerRunning = false
        val validTimes = reactionTimes.filter { it.first != -1L && it.third != TrialType.FALSE_START }.map { it.first }
        val falseStarts = reactionTimes.count { it.third == TrialType.FALSE_START }
        medianRT = calculateMedianInt(validTimes)

        stage = PVTUiStage.SAVING_OR_FINISHED
        isSaved = false

        val res = PVTResponse(
            reactionTimes = validTimes,
            waitTimes = listOfRandomTimes,
            falseStartCount = falseStarts,
            noResponseCount = noResponseCounter,
            durationMinutes = (maxTestDuration / 1000 / 60).toInt()
        )

        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            viewModel.saveResponse(
                res,
                timestamp = System.currentTimeMillis(),
                localTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
            )
            runOnUiThread {
                isSaved = true
            }
        }
    }

    private fun calculateMedianInt(numbers: List<Long>): Double {
        if (numbers.isEmpty()) {
            Log.w("PVTActivity", "Cannot calculate median of an empty list")
            return 0.0
        }

        val sortedNumbers = numbers.sorted()
        val middle = sortedNumbers.size / 2

        return if (sortedNumbers.size % 2 == 0) {
            (sortedNumbers[middle - 1] + sortedNumbers[middle]) / 2.0
        } else {
            sortedNumbers[middle].toDouble()
        }
    }

    private fun react() {
        if (isTimerRunning) {
            val reactionTime = System.currentTimeMillis() - startTime
            elapsedDisplay = "$reactionTime"
            val trialType = when {
                reactionTime <= 150 -> TrialType.FALSE_START
                reactionTime >= 500 -> TrialType.LAPSE
                else -> TrialType.VALID
            }
            feedbackMessage = if (trialType == TrialType.FALSE_START) getString(R.string.too_fast) else null

            reactionTimes.add(Triple(reactionTime, currentWaitingTime, trialType))
            isTimerRunning = false
            handler.postDelayed({
                feedbackMessage = null
                startNextTrial()
            }, 1000)
        } else {
            feedbackMessage = getString(R.string.too_fast)
            reactionTimes.add(Triple(-1, currentWaitingTime, TrialType.FALSE_START))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private enum class TrialType {
        VALID,
        FALSE_START,
        LAPSE
    }
}

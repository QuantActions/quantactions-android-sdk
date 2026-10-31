/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.dotmemory

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
import com.quantactions.sdk.cognitivetests.theme.CognitiveTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * @hide
 */
class DotMemoryTestActivity : ComponentActivity() {

    private val numTrials = 4
    private val numFs = 10

    private var stage by mutableStateOf(DotMemoryUiStage.READY_DIALOG)
    private var currentTrial by mutableIntStateOf(0)

    // Phase 1 (Memory)
    private var presentationDots by mutableStateOf<Set<Int>>(emptySet())
    private val targetDots = mutableListOf<Int>()

    // Phase 2 (Distraction)
    private var distractionLetters by mutableStateOf<List<String>>(emptyList())
    private var selectedDistractorIndices by mutableStateOf<Set<Int>>(emptySet())
    private val positionsFs = mutableSetOf<Int>()

    // Phase 3 (Recall)
    private var placedDots by mutableStateOf<Set<Int>>(emptySet())
    private var recallStartTime: Long = 0

    // Stored metrics
    private val timeTakenList = mutableListOf<Long>()
    private val recallErrorScoreList = mutableListOf<Double>()
    private val proportionOfDistractorsList = mutableListOf<Double>()

    private var avgTimeTakenSec by mutableDoubleStateOf(0.0)
    private var avgRecallError by mutableDoubleStateOf(0.0)
    private var avgDistractorScore by mutableDoubleStateOf(0.0)
    private var isSaved by mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())
    private val viewModel: DotMemoryTestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getTestResults()

        setContent {
            CognitiveTestTheme(darkTheme = true) {
                DotMemoryTestScreen(
                    stage = stage,
                    currentTrial = currentTrial,
                    totalTrials = numTrials,
                    onStartTest = {
                        startPresentationPhase()
                    },
                    onBack = {
                        finish()
                    },
                    presentationDots = presentationDots,
                    distractionLetters = distractionLetters,
                    selectedDistractorIndices = selectedDistractorIndices,
                    onToggleDistractor = { index ->
                        selectedDistractorIndices = if (selectedDistractorIndices.contains(index)) {
                            selectedDistractorIndices - index
                        } else {
                            selectedDistractorIndices + index
                        }
                    },
                    placedDots = placedDots,
                    onTogglePlacedDot = { index ->
                        placedDots = if (placedDots.contains(index)) {
                            placedDots - index
                        } else if (placedDots.size < 3) {
                            placedDots + index
                        } else {
                            placedDots
                        }
                    },
                    onConfirmRecall = {
                        completeRecallPhase()
                    },
                    avgTimeTakenSec = avgTimeTakenSec,
                    avgRecallError = avgRecallError,
                    avgDistractorScore = avgDistractorScore,
                    isSaved = isSaved,
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun startPresentationPhase() {
        stage = DotMemoryUiStage.PRESENTATION
        val newTargetDots = mutableSetOf<Int>()
        while (newTargetDots.size < 3) {
            newTargetDots.add(Random.nextInt(0, 25))
        }
        targetDots.clear()
        targetDots.addAll(newTargetDots)
        presentationDots = newTargetDots

        handler.postDelayed({
            startDistractionPhase()
        }, 3000)
    }

    private fun startDistractionPhase() {
        stage = DotMemoryUiStage.DISTRACTION
        positionsFs.clear()
        selectedDistractorIndices = emptySet()

        while (positionsFs.size < numFs) {
            positionsFs.add(Random.nextInt(0, 40))
        }

        distractionLetters = List(40) { index ->
            if (positionsFs.contains(index)) "F" else "E"
        }

        handler.postDelayed({
            startRecallPhase()
        }, 8000)
    }

    private fun startRecallPhase() {
        stage = DotMemoryUiStage.RECALL
        placedDots = emptySet()
        recallStartTime = System.currentTimeMillis()
    }

    private fun completeRecallPhase() {
        val timeTaken = System.currentTimeMillis() - recallStartTime
        val recallError = calculateRecallErrorScore()
        val proportion = calculateProportionOfDistractors()

        timeTakenList.add(timeTaken)
        recallErrorScoreList.add(recallError)
        proportionOfDistractorsList.add(proportion)

        currentTrial++
        if (currentTrial < numTrials) {
            handler.postDelayed({
                startPresentationPhase()
            }, 1000)
        } else {
            showFinalResults()
        }
    }

    private fun calculateRecallErrorScore(): Double {
        val distances = mutableListOf<Double>()
        for (target in targetDots) {
            for (placed in placedDots) {
                val targetRow = target / 5
                val targetCol = target % 5
                val placedRow = placed / 5
                val placedCol = placed % 5
                val distance = sqrt(
                    (targetRow - placedRow).toDouble().pow(2) + (targetCol - placedCol).toDouble().pow(2)
                )
                distances.add(distance)
            }
        }
        distances.sort()
        return distances.take(3).sum()
    }

    private fun calculateProportionOfDistractors(): Double {
        val numberFsSelected = selectedDistractorIndices.intersect(positionsFs).size
        val numberEsSelected = selectedDistractorIndices.size - numberFsSelected
        return (numberFsSelected.toDouble() - numberEsSelected) / numFs
    }

    private fun showFinalResults() {
        avgTimeTakenSec = (timeTakenList.average()) / 1000.0
        avgRecallError = recallErrorScoreList.average()
        avgDistractorScore = proportionOfDistractorsList.average().coerceAtLeast(0.0)

        stage = DotMemoryUiStage.SAVING_OR_FINISHED
        isSaved = false

        val res = DotMemoryTestResponse(
            timeTaken = timeTakenList,
            recallErrorScore = recallErrorScoreList,
            proportionOfDistractors = proportionOfDistractorsList,
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
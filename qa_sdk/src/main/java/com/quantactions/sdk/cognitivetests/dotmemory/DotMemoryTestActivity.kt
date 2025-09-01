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
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quantactions.sdk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * @hide
 */
class DotMemoryTestActivity : AppCompatActivity() {

    private lateinit var presentationGrid: GridLayout
    private lateinit var distractionGrid: GridLayout
    private lateinit var recallGrid: GridLayout
    private lateinit var doneButton: Button
    private lateinit var timeTakenTextView: TextView
    private var handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0
    private var recallStartTime: Long = 0
    private var numFs = 10 // Number of Fs in the distraction phase
    private var placedDotsCount = 0 // Track the number of placed dots
    private val targetDots = mutableListOf<Int>() // Store target dot positions
    private val placedDots = mutableListOf<Int>() // Store placed dot positions
    private val positionsFs = mutableSetOf<Int>() // Store positions of Fs
    private val selectedLetters = mutableListOf<Int>()

    private val numTrials = 4 // Number of trials
    private var currentTrial = 0 // Current trial number
    private val timeTakenList = mutableListOf<Long>()
    private val recallErrorScoreList = mutableListOf<Double>()
    private val proportionOfDistractorsList = mutableListOf<Double>()

    private val viewModel: DotMemoryTestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dot_memory_test)

        presentationGrid = findViewById(R.id.presentationGrid)
        distractionGrid = findViewById(R.id.distractionGrid)
        recallGrid = findViewById(R.id.recallGrid)
        doneButton = findViewById(R.id.doneButton)
        timeTakenTextView = findViewById(R.id.timeTakenTextView)

        viewModel.getTestResults()
        showStartTestDialog()
    }

    private fun showStartTestDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Ready to Start?")
        builder.setMessage("Please ensure you are sitting comfortably and are free from distractions.")

        builder.setPositiveButton("OK, Start Test") { _, _ ->
            // Start the test
            prepareGrid(presentationGrid)
            prepareGrid(recallGrid)
            startPresentationPhase()
        }

        builder.setNegativeButton("Back") { _, _ ->
            // Close the activity
            finish()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun startPresentationPhase() {
        presentationGrid.visibility = View.VISIBLE
        placeRandomDots(presentationGrid, 3)
        handler.postDelayed({
            presentationGrid.visibility = View.GONE
            startDistractionPhase()
        }, 3000)
    }

    private fun startDistractionPhase() {
        distractionGrid.visibility = View.VISIBLE
        placeRandomLetters(distractionGrid, numFs)
        handler.postDelayed({
            distractionGrid.visibility = View.GONE
            startRecallPhase()
        }, 8000)
    }

    private fun startRecallPhase() {
        recallGrid.visibility = View.VISIBLE
        doneButton.visibility = View.VISIBLE
        recallStartTime = System.currentTimeMillis()
        setupRecallGrid(recallGrid)
        doneButton.setOnClickListener {
            val timeTaken = System.currentTimeMillis() - recallStartTime
            recallGrid.visibility = View.GONE
            doneButton.visibility = View.GONE

            // Calculate scores
            val recallErrorScore = calculateRecallErrorScore()
            val proportionOfDistractors = calculateProportionOfDistractors()

            // Store scores
            timeTakenList.add(timeTaken)
            recallErrorScoreList.add(recallErrorScore)
            proportionOfDistractorsList.add(proportionOfDistractors)

            // Proceed to next trial or show final results
            currentTrial++
            if (currentTrial < numTrials) {
                handler.postDelayed({
                    startPresentationPhase()
                }, 1000)
            } else {
                showFinalResults()
            }
        }
    }

    private fun prepareGrid(grid: GridLayout) {

        val cellSize = calculateCellSize(grid)
        Log.d("Cell size", "place random dots $cellSize")

        (0 until 25).forEach { pos ->
            val dot = View(this@DotMemoryTestActivity).apply {
                setBackgroundColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
            }
            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(pos / 5)
                columnSpec = GridLayout.spec(pos % 5)
                width = cellSize
                height = cellSize
                setMargins(3, 3, 3, 3)
            }
            grid.addView(dot, params)
        }
    }

    private fun placeRandomDots(grid: GridLayout, numDots: Int) {
        val positions = mutableSetOf<Int>()
        while (positions.size < numDots) {
            positions.add(Random.nextInt(0, 25))
        }
        targetDots.clear()
        targetDots.addAll(positions)

        val cellSize = calculateCellSize(grid)
        Log.d("Cell size", "place random dots $cellSize")

        (0 until 25).forEach { pos ->
            val dot = View(this@DotMemoryTestActivity).apply {
                if (positions.contains(pos)) {
                    setBackgroundResource(R.drawable.circle_shape)
                } else setBackgroundColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
            }
            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(pos / 5)
                columnSpec = GridLayout.spec(pos % 5)
                width = cellSize
                height = cellSize
                setMargins(3, 3, 3, 3)
            }
            grid.addView(dot, params)
        }
    }

    private fun placeRandomLetters(grid: GridLayout, numFs: Int) {
        positionsFs.clear()
        selectedLetters.clear()
        while (positionsFs.size < numFs) {
            positionsFs.add(Random.nextInt(0, 40))
        }

        val cellSize = calculateCellSize(grid)

        for (i in 0 until 40) {
            val letter = TextView(this).apply {
                text = if (positionsFs.contains(i)) "F" else "E"
                setTextColor(resources.getColor(R.color.white, context.theme))
                setBackgroundColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                tag = "unselected"
                setOnClickListener {
                    if (tag == "unselected") {
                        setBackgroundColor(resources.getColor(R.color.white, context.theme))
                        setTextColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
                        tag = "selected"
                        selectedLetters.add(i)
                    } else {
                        setBackgroundColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
                        setTextColor(resources.getColor(R.color.white, context.theme))
                        tag = "unselected"
                        selectedLetters.remove(i)
                    }
                }
            }
            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(i / 5)
                columnSpec = GridLayout.spec(i % 5)
                width = cellSize
                height = cellSize
                setMargins(3, 3, 3, 3)
            }
            grid.addView(letter, params)
        }
    }

    private fun setupRecallGrid(grid: GridLayout) {
        val cellSize = calculateCellSize(grid)
        Log.d("Cell size", "setup recall grid $cellSize")
        placedDots.clear()
        placedDotsCount = 0

        for (i in 0 until 25) {
            val cell = View(this).apply {
                setBackgroundColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
                tag = "unselected"
                setOnClickListener {
                    if (tag == "unselected" && placedDotsCount < 3) {
                        setBackgroundResource(R.drawable.circle_shape)
                        tag = "selected"
                        placedDotsCount++
                        placedDots.add(i)
                    } else if (tag == "selected") {
                        setBackgroundColor(resources.getColor(R.color.brand_cognitive_tests_background, context.theme))
                        tag = "unselected"
                        placedDotsCount--
                        placedDots.remove(i)
                    }
                }
            }
            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(i / 5)
                columnSpec = GridLayout.spec(i % 5)
                width = cellSize
                height = cellSize
                setMargins(3, 3, 3, 3)
            }
            grid.addView(cell, params)
        }
    }

    private fun calculateCellSize(grid: GridLayout): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val gridSize = min(screenWidth, screenHeight) * 0.9 - 40 // 40dp margin (20dp each side)
        return (gridSize / grid.columnCount).toInt()
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
                    (targetRow - placedRow).toDouble().pow(2) + (targetCol - placedCol).toDouble()
                        .pow(2)
                )
                distances.add(distance)
            }
        }
        distances.sort()
        return distances.take(3).sum()
    }

    private fun calculateProportionOfDistractors(): Double {
        val numberFsSelected = selectedLetters.intersect(positionsFs).size
        val numberEsSelected = selectedLetters.size - numberFsSelected
        return (numberFsSelected.toDouble() - numberEsSelected) / numFs
    }

    private fun showFinalResults() {
        val avgTimeTaken = timeTakenList.average()
        val avgRecallErrorScore = recallErrorScoreList.average()
        val avgProportionOfDistractors = proportionOfDistractorsList.average()

        timeTakenTextView.text = "Average Time Taken: ${avgTimeTaken / 1000.0} seconds\n" +
                "Average Recall Error Score: ${"%.2f".format(avgRecallErrorScore)}\n" +
                "Average Proportion of Distractors: ${"%.2f".format(avgProportionOfDistractors)}"
        timeTakenTextView.visibility = View.VISIBLE


        // save
        val res = DotMemoryTestResponse(
            timeTaken = timeTakenList,
            recallErrorScore = recallErrorScoreList,
            proportionOfDistractors = proportionOfDistractorsList,
        )

        // Here we show a loading dialog while we save the response
        // after the response has been saved we dismiss the dialog and close the activity
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Saving response")
            .setMessage("Median RT: $avgTimeTaken ms\nPlease wait...")
            .setPositiveButton("Finish") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            // button should be disable until we can go back
            .setCancelable(false)
            .create()
        loadingDialog.show()

        loadingDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            viewModel.saveResponse(
                res,
                timestamp = System.currentTimeMillis(),
                localTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
            )
            runOnUiThread {
                loadingDialog.setMessage("Median RT: $avgTimeTaken ms\nDone!")
                loadingDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }


    }
}
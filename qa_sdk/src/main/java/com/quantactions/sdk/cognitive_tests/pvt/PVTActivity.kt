/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitive_tests.pvt

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quantactions.sdk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.random.Random

class PVTActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var resultTextView: TextView
    private lateinit var brandCircle: View
    private lateinit var timerLayout: LinearLayout
    private lateinit var button: ImageButton
    private var handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0
    private var isTimerRunning = false
    private var testStartTime: Long = 0
    private val updateInterval: Long = 10 // Update interval in milliseconds
    private val reactionTimes =
        mutableListOf<Triple<Long, Long, TrialType>>() // Triple of reaction time, waiting time, and trial type
    private var currentWaitingTime: Long = 0
    private val maxTestDuration: Long = 3 * 60 * 1000 // 3 minutes in milliseconds
    private var noResponseCounter: Int = 0
    private val listOfRandomTimes: MutableList<Long> = mutableListOf()

    private val viewModel: PVTViewModel by viewModels()

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val elapsedTime = System.currentTimeMillis() - startTime
                timerTextView.text = "$elapsedTime"
                if (elapsedTime >= 9999) {
                    // Timeout after 10 seconds
                    timerTextView.text = getString(R.string._9999)
                    resultTextView.text = getString(R.string.too_slow)
                    resultTextView.visibility = View.VISIBLE
                    noResponseCounter += 1
                    isTimerRunning = false
                    handler.postDelayed({
                        timerTextView.visibility = View.INVISIBLE
                        brandCircle.visibility = View.VISIBLE
                        resultTextView.visibility = View.INVISIBLE
                        startNextTrial()
                    }, 1000)
                } else {
                    handler.postDelayed(this, updateInterval)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cognitive_test)

        button = findViewById(R.id.imageButton)
        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                react()
            }
            super.onTouchEvent(event)
        }
        timerTextView = findViewById(R.id.timerTextView)
        resultTextView = findViewById(R.id.resultTextView)
        brandCircle = findViewById(R.id.brandCircle)
        timerLayout = findViewById(R.id.timerLayout)

        // Show instructions and start test
        showStartTestDialog()
    }

    private fun showStartTestDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Ready to Start?")
        builder.setMessage("Please ensure you are sitting comfortably and are free from distractions.")

        builder.setPositiveButton("OK, Start Test") { _, _ ->
            // Start the test
            testStartTime = System.currentTimeMillis()
            startNextTrial()
        }

        builder.setNegativeButton("Back") { _, _ ->
            // Close the activity
            finish()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun startNextTrial() {
        if (System.currentTimeMillis() - testStartTime >= maxTestDuration) {
            showResults()
            return
        }

        resultTextView.text = ""
        currentWaitingTime = Random.nextLong(0, 3000)
        listOfRandomTimes.add(currentWaitingTime)
        handler.postDelayed({
            startTime = System.currentTimeMillis()
            isTimerRunning = true
            timerTextView.visibility = View.VISIBLE
            brandCircle.visibility = View.INVISIBLE
            resultTextView.visibility = View.INVISIBLE
            handler.post(updateTimerRunnable)
        }, currentWaitingTime)
    }

    private fun showResults() {
        timerLayout.visibility = LinearLayout.GONE
        resultTextView.visibility = TextView.GONE

        val validTimes =
            reactionTimes.filter { it.first != -1L && it.third != TrialType.FALSE_START }.map { it.first }
        val falseStarts = reactionTimes.count { it.third == TrialType.FALSE_START }

        // Calculate median reaction time for the valid times
        val medianRT = calculateMedianInt(validTimes)

        // save
        val res = PVTResponse(
            reactionTimes = validTimes,
            waitTimes = listOfRandomTimes,
            date = System.currentTimeMillis(),
            localTime = LocalTime.now().toString(),
            falseStartCount = falseStarts,
            noResponseCount = noResponseCounter
        )

        // Here we show a loading dialog while we save the response
        // after the response has been saved we dismiss the dialog and close the activity
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Saving response")
            .setMessage("Median RT: $medianRT ms\nPlease wait...")
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
            viewModel.saveResponse(res)
            runOnUiThread {
                loadingDialog.setMessage("Median RT: $medianRT ms\nDone!")
                loadingDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }

    private fun calculateMedianInt(numbers: List<Long>): Double {
        if (numbers.isEmpty()) {
            throw IllegalArgumentException("Cannot calculate median of an empty list")
        }

        val sortedNumbers = numbers.sorted()
        val middle = sortedNumbers.size / 2

        return if (sortedNumbers.size % 2 == 0) {
            // Even number of elements, average the two middle values
            (sortedNumbers[middle - 1] + sortedNumbers[middle]) / 2.0
        } else {
            // Odd number of elements, take the middle value
            sortedNumbers[middle].toDouble()
        }
    }

    private fun react() {
        if (isTimerRunning) {
            val reactionTime = System.currentTimeMillis() - startTime
            timerTextView.text = "$reactionTime"
            val trialType = when {
                reactionTime <= 150 -> TrialType.FALSE_START
                reactionTime >= 500 -> TrialType.LAPSE
                else -> TrialType.VALID
            }
            if (TrialType.FALSE_START == trialType) {
                resultTextView.text = getString(R.string.too_fast)
            } else {
                resultTextView.text = ""
            }

            reactionTimes.add(Triple(reactionTime, currentWaitingTime, trialType))
            isTimerRunning = false
            resultTextView.visibility = View.VISIBLE
            handler.postDelayed({
                timerTextView.visibility = View.INVISIBLE
                brandCircle.visibility = View.VISIBLE
                resultTextView.visibility = View.INVISIBLE
                startNextTrial()
            }, 1000)
        } else {
            resultTextView.text = getString(R.string.too_fast)
            reactionTimes.add(Triple(-1, currentWaitingTime, TrialType.FALSE_START))
            resultTextView.visibility = View.VISIBLE
        }

        return
    }

    // class to hold test result that can be JSON encoded
    private enum class TrialType {
        VALID,
        FALSE_START,
        LAPSE
    }

}
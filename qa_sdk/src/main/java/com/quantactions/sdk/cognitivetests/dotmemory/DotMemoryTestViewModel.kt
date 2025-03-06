/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.dotmemory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantactions.sdk.QA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import javax.inject.Inject

/**
 * This is the view model that holds the connection to the SDK and it is responsible to retrieve
 * and serve the scores from the SDK.
 * Holds also a reference to the calendar to retrieve correctly the needed data.
 * The implementation is modular in such a way that when new metrics will come in it is easy enough
 * to add them to here
 * @param application Android application
 * */

open class DotMemoryTestViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {


    private var _testResults = MutableStateFlow(listOf<DotMemoryTestResponse>())
    val testResults: StateFlow<List<DotMemoryTestResponse>> get() = _testResults

    private var _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> get() = _saving

    private val qa = QA.getInstance(application.applicationContext)

    fun getTestResults() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val results = qa.getDotMemoryTestResults()
                _testResults.value = results
            }
        }
    }

    suspend fun saveResponse(
        response: DotMemoryTestResponse,
        timestamp: Long = System.currentTimeMillis(),
        localTime: String = LocalTime.now().toString(),
    ) {
        withContext(Dispatchers.Default) {
            _saving.value = true
            qa.saveDotMemoryTestResult(response, timestamp, localTime)
            _saving.value = false
        }
    }

}


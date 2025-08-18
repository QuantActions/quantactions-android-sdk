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
import com.quantactions.sdk.cognitivetests.CognitiveTest
import com.quantactions.sdk.cognitivetests.CognitiveTestResult
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

/**
 * This is the view model for the DotMemory test.
 * @param application Android application
 * */

open class DotMemoryTestViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private var _testResults = MutableStateFlow(listOf<CognitiveTestResult<DotMemoryTestResponse>>())
    val testResults: StateFlow<List<CognitiveTestResult<DotMemoryTestResponse>>> get() = _testResults

    private var _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> get() = _saving

    private val qa = QA.getInstance(application.applicationContext)

    fun getTestResults() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                qa.getCognitiveTestResults(CognitiveTest.DotMemory).collect { results ->
                    _testResults.value = results
                }
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
            qa.saveCognitiveTestResult(CognitiveTest.DotMemory, response, timestamp, localTime)
            _saving.value = false
        }
    }

}


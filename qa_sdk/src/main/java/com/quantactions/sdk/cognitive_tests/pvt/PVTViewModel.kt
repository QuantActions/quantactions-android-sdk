/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitive_tests.pvt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantactions.sdk.QA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * This is the view model that holds the connection to the SDK and it is responsible to retrieve
 * and serve the scores from the SDK.
 * Holds also a reference to the calendar to retrieve correctly the needed data.
 * The implementation is modular in such a way that when new metrics will come in it is easy enough
 * to add them to here
 * @param application Android application
 * */

open class PVTViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {


    private var _testResults = MutableStateFlow(listOf<PVTResponse>())
    val testResults: StateFlow<List<PVTResponse>> get() = _testResults

    private var _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> get() = _saving

    private val qa = QA.getInstance(application.applicationContext)

    private fun getTestResults() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
            val studies = qa.getTestResults()
            _testResults.value = studies
            }
        }
    }

    suspend fun saveResponse(response: PVTResponse) {
            withContext(Dispatchers.Default) {
                _saving.value = true
                qa.saveTestResult(response)
                _saving.value = false
            }
    }

    init {
        getTestResults()
    }


}


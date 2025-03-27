/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
package com.quantactions.sdk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.cognitivetests.CognitiveTest
import com.quantactions.sdk.data.repository.MVPRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class PushPendingCognitiveTestsWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val pendingCognitiveTests = repository.getPendingCognitiveTests()


        val jobs = pendingCognitiveTests.map { entry ->
            async {

                val cognitiveTest = if (entry.testType == "PVT") CognitiveTest.PVT else CognitiveTest.DotMemory

                when(repository.submitPendingCognitiveTestResponse(cognitiveTest, entry)) {
                    is ApiSuccessResponse -> {
                        repository.setCognitiveTestSyncStatusTo1(entry)
                        0
                    }
                    else -> 1
                }
            }
        }

        if (jobs.awaitAll().sum() == 0) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
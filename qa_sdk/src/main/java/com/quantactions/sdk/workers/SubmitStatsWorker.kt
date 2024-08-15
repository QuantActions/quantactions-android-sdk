/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.model.DeviceStats
import com.quantactions.sdk.data.repository.MVPRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class SubmitStatsWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val extraData: DeviceStats? = inputData.getString("map")?.let {
            Json.decodeFromString(DeviceStats.serializer(), it )
        }

        when(val response = repository.submitStatistic(extraData)) {
            is ApiErrorResponse -> {
                if (response.httpStatusCode == 424){
                    Timber.e("No need to push the day again")
                    Result.failure()
                } else {
                    Timber.e(response.errorMessage)
                    Result.retry()
                }
            }
            is ApiEmptyResponse -> {
                Timber.e("empty response")
                Result.retry()
            }
            is ApiSuccessResponse -> {
                val data = Data.Builder()
                data.putString("message", response.body?.toString())
                Result.success(data.build())
            }
        }


    }
}
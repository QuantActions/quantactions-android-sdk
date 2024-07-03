/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */
package com.quantactions.sdk.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.repository.MVPRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UpdateAppsListWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendingAppCodes = repository.getPendingAppCodes()
        if (pendingAppCodes.isNotEmpty()) {

            val pendingAppList = pendingAppCodes.map { entry ->
                AppToPush(
                    entry.appName,
                    entry.id,
                )
            }

            when (val response2 = repository.updateAppList(pendingAppList)) {
                is ApiErrorResponse -> {
                    Timber.w(response2.errorMessage)
                    Result.retry()
                }

                is ApiEmptyResponse -> {
                    Result.retry()
                }

                is ApiSuccessResponse -> {
                    pendingAppCodes.forEach { app ->
                        repository.updateCodeOfAppStatus(app.id, 1)
                    }
                    Result.success()
                }
            }


        } else {
            Result.success()
        }
    }
}
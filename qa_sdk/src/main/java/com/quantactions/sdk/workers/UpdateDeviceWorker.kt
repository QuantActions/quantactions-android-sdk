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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.repository.MVPRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UpdateDeviceWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        try {
            if (repository.deviceID == ""){
                registerSpecificationsAndDevice(repository)
            }
        } catch (e: Exception) {
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
            e.printStackTrace()
        }

        when (val response = repository.updateDeviceInfo()){
            is ApiErrorResponse -> {
                Timber.e(response.errorMessage)
                Result.failure()
            }
            is ApiEmptyResponse -> {
                Result.retry()
            }
            is ApiSuccessResponse -> {
                updateIdentity()
            }
        }
    }

    private suspend fun updateIdentity(): Result {
        return when (val response = repository.updateIdentity()) {
            is ApiErrorResponse -> {
                Timber.e(response.errorMessage)
                Result.failure()
            }

            is ApiEmptyResponse -> {
                Result.retry()
            }

            is ApiSuccessResponse -> {
                response.body.let {
                    Result.success()
                }
            }
        }
    }

}
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
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.repository.MVPRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class RegisterWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        repository.checkRegisteredStatus()
        registerSpecificationsAndDevice(repository)
    }
}



suspend fun registerSpecificationsAndDevice(repository: MVPRepository): ListenableWorker.Result {
    when (val response = repository.registerDeviceSpecifications()) {
        is ApiEmptyResponse -> {
            return ListenableWorker.Result.retry()
        }
        is ApiErrorResponse -> {
            Timber.e("Register spec and device error ${response.errorMessage}")
            return ListenableWorker.Result.retry()
        }
        is ApiSuccessResponse -> {
            return if (response.body != null) {
                Timber.d("Registered device specs -> ${response.body!!.id}")
                repository.saveDeviceSpecificationsId(response.body!!.id)
                registerDevice(repository, response.body!!.id)
            } else {
                ListenableWorker.Result.failure()
            }
        }
    }
}

suspend fun registerDevice(repository: MVPRepository, id: String): ListenableWorker.Result {
    when (val response2 = repository.registerUser(id)) {
        is ApiEmptyResponse -> {
            return ListenableWorker.Result.retry()
        }
        is ApiErrorResponse -> {
            Timber.e(response2.errorMessage)
            return ListenableWorker.Result.retry()
        }
        is ApiSuccessResponse -> {
            response2.body?.let {
                Timber.d("Registered device -> ${it.id}")
                repository.setDeviceId(it.id)
            }
            return ListenableWorker.Result.success()
        }
    }
}
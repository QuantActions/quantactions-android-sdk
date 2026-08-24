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
import com.quantactions.sdk.QAPrivate
import com.quantactions.sdk.data.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class RegisterWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val deviceRepository = QAPrivate.getInstance(context).deviceRepo
    private val identityRepository = QAPrivate.getInstance(context).identityRepo

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        identityRepository.checkRegisteredStatus()
        registerSpecificationsAndDevice(deviceRepository)
    }
}



internal suspend fun registerSpecificationsAndDevice(deviceRepository: DeviceRepository): ListenableWorker.Result {
    when (val response = deviceRepository.registerDeviceSpecifications()) {
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
                deviceRepository.saveDeviceSpecificationsId(response.body!!.id)
                registerDevice(deviceRepository, response.body!!.id)
            } else {
                ListenableWorker.Result.failure()
            }
        }
    }
}

internal suspend fun registerDevice(deviceRepository: DeviceRepository, id: String): ListenableWorker.Result {
    when (val response2 = deviceRepository.registerUser(id)) {
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
                deviceRepository.setDeviceId(it.id)
            }
            return ListenableWorker.Result.success()
        }
    }
}
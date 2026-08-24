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
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.QAPrivate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdatePasswordWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = QAPrivate.getInstance(context).identityRepo
    private val preferences = ManagePref2.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

//        val newPassword = GeneratePassword.randomString()

//        when (val response = repository.updateOldPassword(newPassword)) {
//
//            is ApiSuccessResponse -> {
//                preferences.password = newPassword
                Result.success()
//            }
//
//            else -> {
//                Timber.tag("UpdatePassword").d("FAILURE")
//                Timber.tag("UpdatePassword").d(response.toString())
//                Result.failure()
//            }
//        }
    }

}
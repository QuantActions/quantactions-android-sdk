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
import com.quantactions.sdk.QAPrivate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UpdateStudiesWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val studyRepository = QAPrivate.getInstance(context).studyRepo

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            studyRepository.getSubscriptions()
            Result.success()
        } catch (e: Exception) {
            Timber.w("UpdateStudiesWorker failed: ${e.message}")
            Result.retry()
        }
    }
}

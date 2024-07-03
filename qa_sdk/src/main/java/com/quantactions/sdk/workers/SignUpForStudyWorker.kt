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
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.repository.MVPRepository
import com.quantactions.sdk.exceptions.QASDKException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SignUpForStudyWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val studyId = inputData.getString(SubscriptionConstants.COHORT_ID.name)
        val subscriptionId = inputData.getString(SubscriptionConstants.SUBSCRIPTION_ID.name)

        try {
            subscriptionId?.let { repository.registerToStudyWithParticipationId(it) }
            studyId?.let { repository.registerToStudy(it) }
        } catch (e: QASDKException) {
            // This means an error from the API so we don;t retry
            Result.failure()
        } catch (e: Exception) {
            // This means potentially a network issue so we wanna retry later
            Result.retry()
        }
        Result.success()
    }

    enum class SubscriptionConstants {
        COHORT_ID,
        SUBSCRIPTION_ID,
        SUBSCRIPTION_TOKEN,
        SUBSCRIPTION_VENDOR
    }

}
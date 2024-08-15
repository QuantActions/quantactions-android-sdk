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
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.repository.ActivityBody
import com.quantactions.sdk.data.repository.ActivityToPush
import com.quantactions.sdk.data.repository.DeviceHealthParsedToPush
import com.quantactions.sdk.data.repository.MVPRepository
import com.quantactions.sdk.data.repository.TapDataBody
import com.quantactions.sdk.data.repository.TapDataParsedToPush
import com.quantactions.sdk.literalToIntList
import com.quantactions.sdk.literalToLongList
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SubmitActivityWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val toPushTap = getPendingActivity()

        val activityBody = ActivityBody(toPushTap.second)

            val tapIdsToUpdate = toPushTap.first
            when (val response = repository.submitActivity(activityBody)) {
                is ApiSuccessResponse -> {
                    repository.updateActivitySyncStatus(tapIdsToUpdate)
                    Result.success()
                }

                is ApiErrorResponse -> {
                    Timber.e(response.errorMessage)
                    Timber.e(response.httpStatusCode.toString())

                    if (response.httpStatusCode == 400){
                        // need to take care of the wicked sessions
                        val moshi = com.squareup.moshi.Moshi.Builder().build()
                        val jsonAdapter = moshi.adapter(SessionsError::class.java)
                        val error = jsonAdapter.fromJson(response.errorMessage)
                        repository.deleteWrongTapSessions(error?.error?.details?.invalidRecords ?: listOf())

                        Result.retry()
                    } else {
                        Result.failure()
                    }

                }

                else -> Result.retry()
            }

    }

    private fun getPendingActivity(): Pair<List<Long>, List<ActivityToPush>> {
        // Here I take care of sending out also the id of the item in the DB of pending such that
        // I am sure I delete it only if the transaction is positive.
        val toSync = repository.getActivityToSync()
        val starts2delete = toSync.map { row -> row.timestamp }

        // move to new format
        val toSyncTransformed = toSync.map {
           ActivityToPush(
                it.timestamp,
                it.action,
                it.transition
            )
        }

        return Pair(starts2delete, toSyncTransformed)
    }

    @JsonClass(generateAdapter = true)
    data class SessionsError(
        val error: ErrorBody,
    )

    @JsonClass(generateAdapter = true)
    data class ErrorBody(
        val statusCode: Int,
        val name: String,
        val details: ErrorDetails
    )

    @JsonClass(generateAdapter = true)
    data class ErrorDetails(
        val invalidRecords: List<String>?,
    )


}
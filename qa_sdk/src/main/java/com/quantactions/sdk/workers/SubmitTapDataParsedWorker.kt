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
import com.quantactions.sdk.data.repository.MVPRepository
import com.quantactions.sdk.data.repository.TapDataBody
import com.quantactions.sdk.data.repository.TapDataParsedToPush
import com.quantactions.sdk.literalToIntList
import com.quantactions.sdk.literalToLongList
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SubmitTapDataParsedWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val toPushTap = getPendingTapDataParsed()

        if (toPushTap.second.isEmpty()) {
            Timber.d("No Tap data to push -> skip")
            return@withContext Result.success()
        }

        val tapHealthDataBody = TapDataBody(toPushTap.second)

            val tapIdsToUpdate = toPushTap.first
            when (val response = repository.submitTapDataParsed(tapHealthDataBody)) {
                is ApiSuccessResponse -> {
                    repository.updateTapDataParsedSyncStatus(tapIdsToUpdate)
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

                        // This is done otherwise the SDK will try to push an empty record
                        if (repository.getTapDataParsedToSync().isNotEmpty()) Result.retry()
                        else Result.success()
                    } else {
                        Result.failure()
                    }

                }

                else -> Result.retry()
            }

    }

    private fun getPendingTapDataParsed(): Pair<List<Long>, List<TapDataParsedToPush>> {
        // Here I take care of sending out also the id of the item in the DB of pending such that
        // I am sure I delete it only if the transaction is positive.
        val toSync = repository.getTapDataParsedToSync()
        val starts2delete = toSync.map { row -> row.start }

        // move to new format
        val toSyncTransformed = toSync.map {
            TapDataParsedToPush(
                it.id.toString(),
                it.taps.literalToLongList(),
                it.start,
                it.stop,
                it.orientations.literalToIntList(),
                listOf(
                    it.appIds0.literalToIntList(),
                    it.appIds1.literalToIntList(),
                    it.appIds2.literalToIntList()
                ),
                it.timeZone,
                it.inCharge.toInt()
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
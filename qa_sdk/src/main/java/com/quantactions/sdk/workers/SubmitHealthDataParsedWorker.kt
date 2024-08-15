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
import com.quantactions.sdk.data.repository.DeviceHealthParsedToPush
import com.quantactions.sdk.data.repository.HealthDataBody
import com.quantactions.sdk.data.repository.MVPRepository
import com.quantactions.sdk.data.repository.TapDataParsedToPush
import com.quantactions.sdk.literalToIntList
import com.quantactions.sdk.literalToLongList
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SubmitHealthDataParsedWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val toPushHealth = pushPendingDeviceHealthParsed()

        val tapHealthDataBody = HealthDataBody(toPushHealth.second)

            val healthIdsToUpdate = toPushHealth.first
            when (val response = repository.submitHealthDataParsed(tapHealthDataBody)) {
                is ApiSuccessResponse -> {
                    repository.updateDeviceHealthParsedSyncStatus(healthIdsToUpdate)
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
                        repository.deleteWrongHealthSessions(error?.error?.details?.invalidRecords ?: listOf())

                        // This is done otherwise the SDK will try to push an empty record
                        if (repository.getDeviceHealthParsedToSync().isNotEmpty()) Result.retry()
                        else Result.success()
                    } else {
                        Result.failure()
                    }

                }

                else -> Result.retry()
            }

    }

    private fun pushPendingDeviceHealthParsed(): Pair<List<Long>, List<DeviceHealthParsedToPush>> {
        // Here I take care of sending out also the id of the item in the DB of pending such that
        // I am sure I delete it only if the transaction is positive.
        val toSync = repository.getDeviceHealthParsedToSync()
        val starts2delete = toSync.map { row -> row.start }

        // move to new format
        val toSyncTransformed = toSync.map {
            DeviceHealthParsedToPush(
                it.timestamps.replace("[", "").replace("]", "").toLong(),
                it.charge.replace("[", "").replace("]", "").toInt(),
                it.id.toString()
            )
        }
        return Pair(starts2delete, toSyncTransformed)
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
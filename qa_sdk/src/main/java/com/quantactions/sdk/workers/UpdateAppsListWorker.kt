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
                    response2.body?.forEach { app ->
                        repository.updateCodeOfApp(app.`package`, 1, app.categoryMain)
                    }
                    updateAppCategories()
                    Result.success()
                }
            }

        } else {
            updateAppCategories()
            Result.success()
        }
    }

    suspend fun updateAppCategories() {
        // Also try to update categories of apps that were not pending but might have changed
        val appsWithPendingCategory = repository.getAppsWithPendingCategory()

        if (appsWithPendingCategory.isNotEmpty()) {

            val pendingAppList = appsWithPendingCategory.map { entry ->
                AppToPush(
                    entry.appName,
                    entry.id,
                )
            }

            Timber.d("Trying to update categories of $pendingAppList")

            when (val response3 = repository.updateAppList(pendingAppList)) {
                is ApiErrorResponse -> {
                    Timber.w(response3.errorMessage)
                }

                is ApiEmptyResponse -> {
                    Timber.w("Empty response when updating categories")
                }

                is ApiSuccessResponse -> {
                    response3.body?.forEach { app ->
                        Timber.d("Category of ${app.`package`} updated to ${app.categoryMain}")
                        repository.updateCodeOfApp(app.`package`, 1, app.categoryMain)
                    }
                }
            }
        } else {
            Timber.d("No apps with pending category to update")
        }
    }
}
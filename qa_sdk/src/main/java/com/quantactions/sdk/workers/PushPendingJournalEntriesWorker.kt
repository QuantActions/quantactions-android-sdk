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
import com.quantactions.sdk.data.model.JournalEntryBody
import com.quantactions.sdk.data.model.JournalEventBody
import com.quantactions.sdk.data.repository.MVPRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PushPendingJournalEntriesWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val repository = MVPRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val pendingEntries = repository.getPendingJournalEntries()
        val pendingEntriesToDelete = repository.getPendingJournalEntriesToDelete()

        val job = pendingEntries.map { localEntry ->
            async {
                // build the body
                val eventsForEntry = repository.getJournalEventsOfJournalEntry(localEntry.id)

                // what I save is (created.toEpochMilli() / 1000).toString(),
                val loadCreated = Instant.ofEpochMilli(localEntry.created.toLong())

                // I also try to sync
                val journalEntryBody =
                    JournalEntryBody(
                        localEntry.id,
                        localEntry.description,
                        loadCreated.atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_DATE_TIME),
                    )

                val journalEventsToPost = eventsForEntry.map {
                    JournalEventBody(
                        it.journal_event_id,
                        if (it.rating == -1) null else it.rating,
                    )
                }

                when(val response = repository.journalEntrySubmit(journalEntryBody)) {

                        is ApiSuccessResponse, is ApiEmptyResponse -> {

                            repository.updateJournalEntry(localEntry.id, 1)

                            when (val response2 = repository.journalEventsSubmit(
                                journalEntryBody.id,
                                journalEventsToPost,
                            )
                            ) {
                                is ApiSuccessResponse, is ApiEmptyResponse -> {
                                    // Here I need to push the single stuff
                                    Timber.d("API success response: Journal event was successfully posted")
                                    // no need to update the sync status as that is dependent on the entry
                                    0
                                }
                                is ApiErrorResponse -> {
                                    Timber.e("API ERROR response:: ${response2.errorMessage}")
                                    1
                                }

                            }
                        }
                        is ApiErrorResponse -> {
                            Timber.e("API ERROR response:: ${response.errorMessage}")
                            1
                        }
                    }
            }
        }

        val deleteJob = pendingEntriesToDelete.map { localEntry ->
            async {

                Timber.w("CALLING DELETE")
                when (val response = repository.simplyDeleteJournalEntry(
                    localEntry.id
                )) {

                    is ApiSuccessResponse, is ApiEmptyResponse -> {
                        Timber.tag("API RESPONSE")
                        repository.updateJournalEntry(localEntry.id, 1)
                        0
                    }
                    is ApiErrorResponse -> {
                        Timber.e("ERROR API (DELETE) $response")
                        1
                    }
                }
            }
        }

        if (job.awaitAll().sum() == 0) {
            if (deleteJob.awaitAll().sum() == 0) {
                Result.success()
            } else {
                Result.retry()
            }
        } else {
            Result.retry()
        }
    }
}
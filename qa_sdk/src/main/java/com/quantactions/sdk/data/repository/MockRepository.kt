/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral", "unused")

package com.quantactions.sdk.data.repository

import android.content.Context
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_core.Resource
import com.hadiyarajesh.flower_core.networkResource
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.CanReturnCompiledTimeSeries
import com.quantactions.sdk.Metric
import com.quantactions.sdk.MockPref
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.TokenApi
import com.quantactions.sdk.data.api.TokenAuthenticator
import com.quantactions.sdk.data.api.getBasicAuthHeader
import com.quantactions.sdk.data.entity.JournalEntryEntity
import com.quantactions.sdk.data.entity.JournalEntryJoinsJournalEventEntity
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.model.JournalEntryEvent
import com.quantactions.sdk.data.repository.MVPRoomDatabase.Companion.getDatabase
import com.quantactions.sdk.data.stringify
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


class MockRepository @Inject constructor(
    context: Context, private val preferences: MockPref, apiKey: String? = null
) {

    companion object {
        @Volatile
        private var INSTANCE: MockRepository? = null

        fun getInstance(context: Context, apiKey: String? = null): MockRepository {
            val preferences = MockPref.getInstance(context)
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = MockRepository(
                        context, preferences, apiKey ?: preferences.apiKey
                    )
                    INSTANCE = instance
                }
                instance.reInit(apiKey ?: preferences.apiKey)
                return instance
            }
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    val identityId: String
        get() = preferences.identityId

    val password: String?
        get() = preferences.password

    private lateinit var apiService: ApiService
    internal val mvpDao = getDatabase(context).mvpDao()

    var iamParticipationId: String = BuildConfig.QA_SAMPLE_PART_ID
    private lateinit var tokenApi: TokenApi

    private var wereJournalEventsCached = false
    private var wereHealthyRangesCached = false

    init {
        reInit(apiKey ?: preferences.apiKey)
    }

    private suspend fun checkLoginStatus() {
        if (preferences.accessToken == null || preferences.accessToken == "") {
            tokenApi.login(getBasicAuthHeader(preferences))
        }
    }

    fun reInit(apiKey: String) {
        val cookieJar = ApiService.UvCookieJar(preferences, "TokenApi")
        tokenApi = TokenApi.buildTokenApi(apiKey, cookieJar)
        val tokenAuthenticator = TokenAuthenticator(tokenApi, preferences)
        apiService = ApiService.create(
            apiKey, tokenAuthenticator, cookieJar
        )
        // This is not really needed but it is to test other journal/ranges calls
        if (!wereHealthyRangesCached) cacheHealthyRanges()
    }

    @ExperimentalCoroutinesApi
    fun <P : TimestampedEntity, T> getStat(
        metricOrTrend: CanReturnCompiledTimeSeries<P, T>, from: Long, to: Long, refresh: Boolean
    ): Flow<TimeSeries<T>> {

        runBlocking { checkLoginStatus() }

        val resources = networkResource(
            makeNetworkRequest = {
                val thisFrom = if (refresh) "1970-01" else Instant.now().minus(60, ChronoUnit.DAYS)
                    .atZone(ZoneId.systemDefault()).format(
                        DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                    )
                val thisTo = Instant.ofEpochMilli(to).atZone(ZoneId.systemDefault()).format(
                    DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                )
                metricOrTrend.getStat(
                    apiService, identityId, iamParticipationId, thisFrom, thisTo
                )
            },
            onNetworkRequestFailed = { _, _ -> }
        ).flowOn(Dispatchers.IO)

        return flow {
            resources.collect {
                when (val status = it.status) {
                    is Resource.Status.Loading, is Resource.Status.EmptySuccess -> {}

                    is Resource.Status.Success -> {
                        emit(metricOrTrend.prepareReturnData(status.data, from / 1000, to / 1000))
                    }

                    is Resource.Status.Error -> {
                        if (status.data != null) {
                            emit(
                                metricOrTrend.prepareReturnData(
                                    status.data!!, from / 1000, to / 1000
                                )
                            )
                        }
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun cacheHealthyRanges() {
        wereHealthyRangesCached = true
        scope.launch {
            Metric.SLEEP_SCORE.cacheHealthyRanges(apiService, preferences, identityId)
            Metric.COGNITIVE_FITNESS.cacheHealthyRanges(apiService, preferences, identityId)
            Metric.SOCIAL_ENGAGEMENT.cacheHealthyRanges(apiService, preferences, identityId)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun cacheJournalEvents() {
        scope.launch {

            val filter = mutableMapOf<String, Any>()
            filter["limit"] = "20"

            when (val jEvents = apiService.getJournalEventTypes(identityId, filter.stringify())) {
                is ApiSuccessResponse -> {
                    wereJournalEventsCached = true
                    Timber.d("[QA API CALL]:[SUC]:Journal events were successfully fetched from API")
                    mvpDao.insertOrUpdateEvents(jEvents.body!!)
                }

                else -> Timber.e("[QA API CALL]:[FAIL]:JOURNAL API something wrong $jEvents")
            }
            cacheJournal()
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun cacheJournal(): List<JournalEntry> {

        val filter = mutableMapOf<String, Any>()

        filter["limit"] = 100
        filter["include"] = listOf(
            mutableMapOf<String, Any>().apply {
                put("relation", "journalEvents")
                put("scope", mutableMapOf<String, Any>().apply {
                    put("where", mutableMapOf<String, Any?>().apply {
                        put("deleted", null)
                    })
                })
            })


        return when (val response = apiService.getJournalEntries(identityId, filter.stringify())) {
            is ApiSuccessResponse -> {
                val journal = mutableListOf<JournalEntry>()
                val entriesToCommit = mutableListOf<JournalEntryEntity>()
                val joinsToCommit = mutableListOf<JournalEntryJoinsJournalEventEntity>()



                response.body!!.forEach { entry ->
                    val entryToAdd = JournalEntry(
                        entry.id, LocalDate.from(
                            DateTimeFormatter.ISO_DATE_TIME.parse(entry.created)
                        ), entry.description
                    )

                    val currentSyncStatus = mvpDao.getEntry(entry.id)?.sync
                    val currentDeleteStatus = mvpDao.getEntry(entry.id)?.deleted

                    entry.journalEvents?.forEach { event ->
                        val referenceEvent = mvpDao.getEvent(event.journalEventTypeId)

                        joinsToCommit.add(
                            JournalEntryJoinsJournalEventEntity(
                                event.id,
                                event.journalId,
                                event.journalEventTypeId,
                                event.rating ?: -1,
                            )
                        )
                        if (referenceEvent != null) entryToAdd.events.add(
                            JournalEntryEvent(
                                event.id,
                                referenceEvent.id,
                                referenceEvent.name,
                                referenceEvent.icon,
                                event.rating
                            )
                        )
                    }

                    entriesToCommit.add(
                        JournalEntryEntity(
                            entry.id,
                            entry.description,
                            entry.identityId,
                            LocalDateTime.from(
                                DateTimeFormatter.ISO_DATE_TIME.parse(entry.created)
                            ).toInstant(ZoneOffset.UTC).toEpochMilli().toString(),
                            LocalDateTime.from(
                                DateTimeFormatter.ISO_DATE_TIME.parse(entry.created)
                            ).toInstant(ZoneOffset.UTC).toEpochMilli().toString(),
                            // if from remote I keep the sync status if exists
                            currentSyncStatus ?: 1,
                            // if from remote I keep the delete status if exists
                            currentDeleteStatus ?: 0,
                            ""
                        )
                    )
                    journal.add(entryToAdd)
                }
                return journal
            }

            else -> listOf()
        }

    }
}
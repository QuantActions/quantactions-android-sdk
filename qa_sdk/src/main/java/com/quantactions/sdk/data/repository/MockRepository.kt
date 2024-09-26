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
import androidx.work.WorkManager
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_core.Resource
import com.hadiyarajesh.flower_core.flow.dbBoundResourceFlow
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.CanReturnCompiledTimeSeries
import com.quantactions.sdk.MockPref
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.TokenApi
import com.quantactions.sdk.data.api.TokenAuthenticator
import com.quantactions.sdk.data.api.getBasicAuthHeader
import com.quantactions.sdk.data.entity.JournalEntryJoinsJournalEventEntity
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.model.JournalEntryEvent
import com.quantactions.sdk.data.repository.MVPRoomDatabase.Companion.getDatabase
import com.quantactions.sdk.data.stringify
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.CountDownLatch
import javax.inject.Inject


class MockRepository @Inject private constructor(
    context: Context,
    private val preferences: MockPref,
    apiKey: String? = null
) {

    private var latch = CountDownLatch(1)

    companion object {
        @Volatile
        private var INSTANCE: MockRepository? = null

        fun getInstance(context: Context, apiKey: String? = null): MockRepository {
            val preferences = MockPref.getInstance(context)
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = MockRepository(
                        context,
                        preferences,
                        apiKey ?: preferences.apiKey
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


    private lateinit var apiService: ApiService//.create(preferences, apiKey ?: preferences.apiKey)
    internal val mvpDao = getDatabase(context).mvpDao()
    val workManager = WorkManager.getInstance(context)

    private var iamParticipationId: String = BuildConfig.QA_SAMPLE_PART_ID
    private lateinit var tokenApi: TokenApi

    private val Boolean.intValue
        get() = if (this) 1 else 0

    init {
        reInit(apiKey ?: preferences.apiKey)
    }

    private var wereJournalEventsCached = false
    private var wasPartIdRequested = false

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
            apiKey,
            tokenAuthenticator,
            cookieJar
        )
    }


    @ExperimentalCoroutinesApi
    fun <P : TimestampedEntity, T> getStat(
        metricOrTrend: CanReturnCompiledTimeSeries<P, T>,
        from: Long,
        to: Long,
        refresh: Boolean
    ): Flow<TimeSeries<T>> {

        runBlocking { checkLoginStatus() }

        val resources = dbBoundResourceFlow(
            fetchFromLocal = {
                metricOrTrend.getMetric(mvpDao)
            },
            shouldMakeNetworkRequest = {
                (refresh && iamParticipationId != "") || ((iamParticipationId != "") && (
                        it.isNullOrEmpty() || it[0].timestamp < (Instant.now()
                            .toEpochMilli() / 1000 - 3 * 3600)))

            },
            makeNetworkRequest = {
                // I would say we always fetch the last 2 months unless forced
                val thisFrom = if (refresh) "1970-01" else Instant.now().minus(60, ChronoUnit.DAYS)
                    .atZone(ZoneId.systemDefault()).format(
                        DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                    )
                val thisTo = Instant.ofEpochMilli(to).atZone(ZoneId.systemDefault()).format(
                    DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                )

                metricOrTrend.getStat(
                    apiService,
                    identityId,
                    iamParticipationId,
                    thisFrom,
                    thisTo
                )
            },
            processNetworkResponse = {
            },
            saveResponseData = { statistics ->
                statistics.chunked(500).forEach { chunk ->
                    metricOrTrend.insertOrUpdateMetric(mvpDao, chunk)
                }
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
                                    status.data!!,
                                    from / 1000,
                                    to / 1000
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun cacheJournalEvents() {
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
        }
    }

    fun getAndResolveJournalEntry(journalEntryId: String): JournalEntry? {
        val resolvedEvents = mvpDao.getResolvedEventsFromEntry(journalEntryId)
        var journalEntry: JournalEntry? = null
        resolvedEvents.forEachIndexed { index, entry ->
            if (index == 0) journalEntry =
                JournalEntry(
                    entry.standaloneEntryId,
                    Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                    entry.note
                )

            entry.journalEventId?.let {
                val referenceEvent = mvpDao.getEvent(entry.journalEventId)
                if (referenceEvent != null)
                    journalEntry?.events?.add(
                        JournalEntryEvent(
                            entry.journalEventId,
                            referenceEvent.id,
                            referenceEvent.name,
                            referenceEvent.icon,
                            entry.rating
                        )
                    )
            }
        }
        return journalEntry
    }

    private fun getJournalEventsAssociatedWithEntry(entryId: String): Flow<Resource<List<JournalEntryJoinsJournalEventEntity>>> {
        return dbBoundResourceFlow(
            fetchFromLocal = {
                mvpDao.getJournalEventsFromJournalEntry(entryId)
            },
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {

                val filter = mutableMapOf(
                    "where" to mutableMapOf(
                        "coreJournalId" to entryId
                    )
                )

                apiService.getJournalEvents(
                    filter.stringify()
                )
            },
            processNetworkResponse = {},
            saveResponseData = {
                mvpDao.insertOrUpdateJournalEntryJoinsJournalEventEntity(it.map { e ->
                    JournalEntryJoinsJournalEventEntity(
                        e.id,
                        e.coreJournalId,
                        e.coreJournalEventTypeId,
                        e.rating
                    )
                })
            },
            onNetworkRequestFailed = { _, _ -> }
        ).flowOn(Dispatchers.IO)
    }

    fun journalEventKinds(): List<JournalEventEntity> {
        return mvpDao.getEvents()
    }

    private val bearer
        get() = mapOf("x-authorization" to "Bearer ${preferences.accessToken}")

}
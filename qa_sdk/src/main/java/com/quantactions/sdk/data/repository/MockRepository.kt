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
import com.hadiyarajesh.flower_core.Resource
import com.hadiyarajesh.flower_core.dbBoundResource
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.CanReturnCompiledTimeSeries
import com.quantactions.sdk.MockPref
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.TokenApi
import com.quantactions.sdk.data.api.TokenAuthenticator
import com.quantactions.sdk.data.api.getBasicAuthHeader
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.repository.MVPRoomDatabase.Companion.getDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject


class MockRepository @Inject private constructor(
    context: Context,
    private val preferences: MockPref,
    apiKey: String? = null
) {

    companion object {
        @Volatile
        private var INSTANCE: MockRepository? = null
        private val a = LocalDate.now()

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

    val identityId: String
        get() = preferences.identityId

    val password: String?
        get() = preferences.password

    private lateinit var apiService: ApiService
    internal val mvpDao = getDatabase(context).mvpDao()

    var iamParticipationId: String = BuildConfig.QA_SAMPLE_PART_ID
    private lateinit var tokenApi: TokenApi

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

        val resources = dbBoundResource(
            fetchFromLocal = {
                metricOrTrend.getMetric(mvpDao)
            },
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
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
            processNetworkResponse = { },
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
}
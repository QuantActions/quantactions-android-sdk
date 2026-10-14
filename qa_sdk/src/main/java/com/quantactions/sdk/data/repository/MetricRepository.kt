/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import com.hadiyarajesh.flower_core.Resource
import com.hadiyarajesh.flower_core.dbBoundResource
import com.quantactions.sdk.CanReturnCompiledTimeSeries
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.stringify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Repository for managing metrics and time series data.
 */
internal class MetricRepository(
    private val identityRepository: IdentityRepository,
    private val networkManager: NetworkManager,
    private val mvpDao: MVPDao
) {
    @ExperimentalCoroutinesApi
    fun <P : TimestampedEntity, T> getStat(
        metricOrTrend: CanReturnCompiledTimeSeries<P, T>,
        iamParticipationId: String,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli(),
        refresh: Boolean = false
    ): Flow<TimeSeries<T>> {

        val resources = dbBoundResource(
            fetchFromLocal = {
                metricOrTrend.getMetric(mvpDao, from / 1000, to / 1000)
            },
            shouldMakeNetworkRequest = {
                (refresh && iamParticipationId != "") || ((iamParticipationId != "") && (
                        it.isNullOrEmpty() || it[0].timestamp < (Instant.now()
                            .toEpochMilli() / 1000 - 3 * 3600)))
            },
            makeNetworkRequest = {
                val thisFrom = if (refresh) "1970-01" else Instant.now().minus(60, ChronoUnit.DAYS)
                    .atZone(ZoneId.systemDefault()).format(
                        DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                    )
                val thisTo = Instant.ofEpochMilli(to).atZone(ZoneId.systemDefault()).format(
                    DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                )

                metricOrTrend.getStat(
                    networkManager.apiService,
                    identityRepository.identityId,
                    iamParticipationId,
                    thisFrom,
                    thisTo
                )
            },
            saveResponseData = { statistics ->
                statistics.chunked(500).forEach { chunk ->
                    metricOrTrend.insertOrUpdateMetric(mvpDao, chunk)
                }
            }
        ).flowOn(Dispatchers.IO)

        return flow {
            resources.collect {
                when (val status = it.status) {
                    is Resource.Status.Loading, is Resource.Status.EmptySuccess -> {}
                    is Resource.Status.Success -> {
                        emit(metricOrTrend.prepareReturnData(status.data!!, from / 1000, to / 1000))
                    }
                    is Resource.Status.Error -> {
                        if (status.data != null) {
                            emit(metricOrTrend.prepareReturnData(status.data!!, from / 1000, to / 1000))
                        }
                    }
                }
            }
        }
    }
}

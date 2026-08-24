/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import com.google.gson.Gson
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.cognitivetests.CognitiveTest
import com.quantactions.sdk.cognitivetests.CognitiveTestResult
import com.quantactions.sdk.cognitivetests.dotmemory.DotMemoryTestResponse
import com.quantactions.sdk.cognitivetests.pvt.PVTResponse
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.entity.CognitiveTestEntity
import com.quantactions.sdk.exceptions.QASDKException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Repository for managing cognitive tests results and submissions.
 */
internal class CognitiveTestRepository(
    private val identityRepository: IdentityRepository,
    private val networkManager: NetworkManager,
    private val cognitiveTestDao: CognitiveTestDao
) {
    @Suppress("UNCHECKED_CAST")
    fun <T> getCognitiveTestResults(testType: CognitiveTest<T>): Flow<List<CognitiveTestResult<T>>> {
        val gson = Gson()
        return cognitiveTestDao.getResultsForType(testType.id).map { entities ->
            when (testType) {
                is CognitiveTest.PVT -> {
                    entities.map { entity ->
                        CognitiveTestResult(
                            cognitiveTest = CognitiveTest.PVT.id,
                            result = gson.fromJson(entity.results, PVTResponse::class.java) as T,
                            timestamp = entity.timestamp,
                            localTime = entity.localTime
                        )
                    }
                }

                is CognitiveTest.DotMemory -> {
                    entities.map { entity ->
                        CognitiveTestResult(
                            cognitiveTest = CognitiveTest.DotMemory.id,
                            result = gson.fromJson(entity.results, DotMemoryTestResponse::class.java) as T,
                            timestamp = entity.timestamp,
                            localTime = entity.localTime
                        )
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun <T> submitCognitiveTestResponse(
        cognitiveTest: CognitiveTest<T>,
        answer: CognitiveTestEntity
    ) {
        val idToDelete = cognitiveTestDao.insertOrUpdateCognitiveTestResult(answer)
        val toPush = CognitiveTestEntity.toBody(answer, cognitiveTest)

        val apiResponse = networkManager.apiService.submitCognitiveTestResponse(
            identityRepository.identityId, toPush
        )

        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let {
                    cognitiveTestDao.setSyncStatusTo1(idToDelete)
                }
            }
            is ApiErrorResponse -> {
                Timber.e("API Error [submitCognitiveTestResponse()]: ${apiResponse.errorMessage} || ${apiResponse.httpStatusCode}")
            }
            is ApiEmptyResponse -> {
                throw QASDKException("Response Body to sendQuestionnaire is empty, this should not happen, file a bug report!")
            }
        }
    }

    suspend fun <T> submitPendingCognitiveTestResponse(
        cognitiveTest: CognitiveTest<T>,
        answer: CognitiveTestEntity
    ): ApiResponse<ApiService.IdResponse> {
        val toPush = CognitiveTestEntity.toBody(answer, cognitiveTest)
        return networkManager.apiService.submitCognitiveTestResponse(
            identityRepository.identityId, toPush
        )
    }

    fun setCognitiveTestSyncStatusTo1(answer: CognitiveTestEntity) {
        cognitiveTestDao.setSyncStatusTo1(answer.id.toLong())
    }

    fun getPendingCognitiveTests(): List<CognitiveTestEntity> {
        return cognitiveTestDao.getPendingCognitiveTests()
    }
}

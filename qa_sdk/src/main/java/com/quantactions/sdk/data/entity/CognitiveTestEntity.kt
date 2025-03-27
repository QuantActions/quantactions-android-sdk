/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.quantactions.sdk.cognitivetests.CognitiveTest
import com.quantactions.sdk.cognitivetests.dotmemory.DotMemoryTestResponse
import com.quantactions.sdk.cognitivetests.pvt.PVTResponse
import com.quantactions.sdk.data.api.ApiService.CognitiveTestResponseBody

@Entity(tableName = "cognitive_test_results")
data class CognitiveTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testType: String, // Discriminator: "memory", "attention", etc.
    val results: String, // The JSON string of the test result
    val timestamp: Long,
    val localTime: String,
    val sync: Int,
){
    companion object {
        fun <T>toBody(cognitiveTestEntity: CognitiveTestEntity, cognitiveTest: CognitiveTest<T>): CognitiveTestResponseBody {
            val gson = Gson()
            val result = when (cognitiveTest) {
                is CognitiveTest.PVT -> gson.fromJson(cognitiveTestEntity.results, PVTResponse::class.java)
                is CognitiveTest.DotMemory -> gson.fromJson(cognitiveTestEntity.results, DotMemoryTestResponse::class.java)
            }
            return CognitiveTestResponseBody(
                    testType = cognitiveTestEntity.testType,
                    localTime = cognitiveTestEntity.localTime,
                    timestamp = cognitiveTestEntity.timestamp,
                    results = result
                )
        }
    }
}
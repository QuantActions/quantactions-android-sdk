/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests

import com.google.gson.Gson
import com.quantactions.sdk.cognitivetests.dotmemory.DotMemoryTestResponse
import com.quantactions.sdk.cognitivetests.pvt.PVTResponse

/**
 * @hide
 */
sealed class CognitiveTest<T>(val id: String, internal val responseClass: Class<T>) {
    data object PVT : CognitiveTest<PVTResponse>("PVT", PVTResponse::class.java)
    data object DotMemory : CognitiveTest<DotMemoryTestResponse>("DotMemory", DotMemoryTestResponse::class.java)

    internal fun parseResult(json: String, gson: Gson): T = gson.fromJson(json, responseClass)
}

/**
 * @hide
 */
class CognitiveTestResult<T>(
    val cognitiveTest: String,
    val result: T,
    val timestamp: Long,
    val localTime: String,
)

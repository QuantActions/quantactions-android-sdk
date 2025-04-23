/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests

import com.quantactions.sdk.cognitivetests.dotmemory.DotMemoryTestResponse
import com.quantactions.sdk.cognitivetests.pvt.PVTResponse


sealed class CognitiveTest<T>(val id: String) {
    data object PVT : CognitiveTest<PVTResponse>("PVT")
    data object DotMemory : CognitiveTest<DotMemoryTestResponse>("DotMemory")
}

class CognitiveTestResult<T>(
    val cognitiveTest: String,
    val result: T,
    val timestamp: Long,
    val localTime: String,
)

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

@Entity(tableName = "cognitive_test_results")
data class CognitiveTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testType: String, // Discriminator: "memory", "attention", etc.
    val resultJson: String, // The JSON string of the test result
    val sync: Int
)
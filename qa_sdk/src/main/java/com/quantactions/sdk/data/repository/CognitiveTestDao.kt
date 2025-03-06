/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantactions.sdk.data.entity.CognitiveTestEntity

@Dao
interface CognitiveTestDao {
    @Insert
    suspend fun insert(cognitiveTestEntity: CognitiveTestEntity)

    @Query("SELECT * FROM cognitive_test_results")
    suspend fun getAllResults(): List<CognitiveTestEntity>

    @Query("SELECT * FROM cognitive_test_results WHERE testType = :testType")
    suspend fun getResultsForType(testType: String): List<CognitiveTestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCognitiveTestResult(action: CognitiveTestEntity)

    @Query("DELETE FROM cognitive_test_results where id = :id")
    fun deleteCognitiveTestResult(id: Int)

}
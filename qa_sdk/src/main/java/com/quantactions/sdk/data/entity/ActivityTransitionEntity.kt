/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * When subscribing to a study with a participation ID or a study ID, the call will return an
 * object of this class which contains all information related to the study including necessary
 * permissions and privacy policy.
 * @suppress
 */
@Entity(tableName = "activity_transition_table")
@JsonClass(generateAdapter = true)
@Keep
data class ActivityTransitionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "transition") val transition: Int,
    @ColumnInfo(name = "sync") val sync: Int
)
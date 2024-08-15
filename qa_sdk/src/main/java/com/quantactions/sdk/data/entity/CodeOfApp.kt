/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
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
@Entity(tableName = "code_of_app")
@JsonClass(generateAdapter = true)
@Keep
data class CodeOfApp(
    /** Identification for row */
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    /** App package name */
    @ColumnInfo(name = "app_name")
    val appName: String,

    /** Is synced */
    @ColumnInfo(name = "sync")
    val sync: Int,
)
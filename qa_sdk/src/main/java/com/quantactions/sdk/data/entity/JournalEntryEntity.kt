/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Entity of the table journal entries.
 * @hide
 */
@Keep
@Entity(tableName = "journal_entry")
@JsonClass(generateAdapter = true)
data class JournalEntryEntity(
    @PrimaryKey
    val id: String,

    @Keep
    @ColumnInfo(name = "note")
    val description: String,

    @Keep
    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @Keep
    @ColumnInfo(name = "created")
    val created: String,

    @Keep
    @ColumnInfo(name = "modified")
    val modified: String,

    @Keep
    @ColumnInfo(name = "sync")
    val sync: Int = 0,

    @Keep
    @ColumnInfo(name = "deleted")
    val deleted: Int = 0,

    @Keep
    @ColumnInfo(name = "old_id")
    val oldId: String = "",

    )
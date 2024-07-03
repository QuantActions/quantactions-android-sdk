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
 * Class containing information about the events that one can log in the journal, only a set of
 * predefined events can be assigned to a journal event, since this list may change in the future,
 * be sure pull this list from remote every so often to refresh it.
 * @hide
 */
@Keep
@Entity(tableName = "journal_event")
@JsonClass(generateAdapter = true)
data class JournalEventEntity(
    /** UUID of the event */
    @PrimaryKey
    val id: String,

    /** Public name (in english) of the event (e.g. Food) */
    @Keep
    @ColumnInfo(name = "public_name")
    val name: String,

    /** Icon name - refers to the icon names from FontAwesome */
    @Keep
    @ColumnInfo(name = "icon_name")
    val icon: String,

    /** UNIX timestamp of when the event was first created */
    @Keep
    @ColumnInfo(name = "created")
    val created: String,

    /** UNIX timestamp of when the event was last edited (e.g. in case we change the public name) */
    @Keep
    @ColumnInfo(name = "modified")
    val modified: String,
)
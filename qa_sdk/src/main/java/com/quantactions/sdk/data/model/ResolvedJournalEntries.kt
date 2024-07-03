/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import androidx.room.ColumnInfo
import com.squareup.moshi.JsonClass

/**
 * @hide
 * Entity of the table journal entries.
 */

@JsonClass(generateAdapter = true)
data class ResolvedJournalEntries(

    @ColumnInfo(name = "standalone_event_id")
    val eventId: String?,

    @ColumnInfo(name = "journal_event_id")
    val journalEventId: String?,

    @ColumnInfo(name = "journal_entry_id")
    val journalEntryId: String?,

    @ColumnInfo(name = "created")
    val timestamp: Long,

    val rating: Int?,

    val note: String,

    @ColumnInfo(name = "public_name")
    val publicName: String?,

    @ColumnInfo(name = "icon_name")
    val iconName: String?,

    @ColumnInfo(name = "standalone_id")
    val standaloneEntryId: String,

    @ColumnInfo(name = "sleep_score_value")
    val sleepScore: Float? = 0f,

    @ColumnInfo(name = "cog_value")
    val cogScore: Float? = 0f,

    @ColumnInfo(name = "social_eng_value")
    val socScore: Float? = 0f,

)
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
 * Entity of the table questionnaires containing all information about a questionnaire.
 * @param id UUID of the event connected to the journal entry
 * @param journal_entry_id UUID of the journal entry
 * @param journal_event_id UUID of the general event
 * @param rating a value between 1 and 5
 * @suppress
 */
@Keep
@Entity(tableName = "journal_entry_joins_journal_event")
@JsonClass(generateAdapter = true)
data class JournalEntryJoinsJournalEventEntity(
    @Keep
    @PrimaryKey
    val id: String,

    @Keep
    @ColumnInfo(name = "journal_entry_id")
    val journal_entry_id: String,

    @Keep
    @ColumnInfo(name = "journal_event_id")
    val journal_event_id: String,

    @Keep
    @ColumnInfo(name = "rating")
    val rating: Int,

)
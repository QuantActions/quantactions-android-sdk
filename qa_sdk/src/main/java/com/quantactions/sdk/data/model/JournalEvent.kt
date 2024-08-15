/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import com.quantactions.sdk.data.entity.JournalEventEntity
import com.squareup.moshi.JsonClass

/**
 * @hide
 * Entity of the table journal entries.
 */
@JsonClass(generateAdapter = true)
data class JournalEvent (
    val id: String,
    val coreJournalEventTypeId: String,
    val coreJournalId: String,
    val rating: Int
)

@JsonClass(generateAdapter = true)
data class JournalEventWithRating (
    val eventType: JournalEventEntity,
    val rating: Int? = null,
    val id: String? = null
    )
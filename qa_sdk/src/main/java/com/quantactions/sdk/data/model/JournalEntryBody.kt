/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
@Serializable
class JournalEntryDeleteBody(
    val journal_entry_id: String
)
/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
@Serializable
class JournalEntryBody(
    val id: String,
    val description: String,
    val created: String,
)
/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
@Serializable
class JournalEventBody(
    val journalEventTypeId: String,
    val rating: Int? = null,
)
/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
@Serializable
class JournalEventEnterResponse(
    val journalEventTypeId: String,
    val journalId: String,
    val rating: Int? = -1,
    val id: String
)
/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
@Serializable
class EventBodyToPost(
    val id: String,
    val journal_event_id: String,
    val rating: Int
)
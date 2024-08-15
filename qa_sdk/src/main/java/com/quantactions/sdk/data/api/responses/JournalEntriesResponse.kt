/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.responses

import com.quantactions.sdk.data.api.SkipSerialization
import com.squareup.moshi.JsonClass

/**
 * @hide
 * This data class represent the body response for the API call [com.quantactions.sdk.data.api.ApiService.getJournalEntries]
 * @property id UUID for the entry
 * @property created UNIX timestamp of the creation date
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class JournalEntriesResponse (
    @SkipSerialization
    val id: String,
    val created: String,
    var description: String,
    val identityId: String,
    var journalEvents: List<JournalEventsResponse>?
)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class JournalEventsResponse (
    @SkipSerialization
    val journalEventTypeId: String,
    val journalId: String,
    val id: String,
    val rating: Int? = null,
    val deleted: String?
)

/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import com.squareup.moshi.JsonClass

/**
 * @hide
 * Entity of the table journal entries.
 */
@JsonClass(generateAdapter = true)
data class ResolvedJournalEvent (
    val id: String,
    val journalEventId: String,
    val publicName: String,
    val iconName: String
)
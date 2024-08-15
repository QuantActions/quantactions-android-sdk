/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import androidx.annotation.Keep
import java.time.LocalDate

/**
 * @hide
 *
 * @property id
 * @property date
 * @property note
 * @property events
 * @property scores
 */
@Keep
data class JournalEntry(
    val id: String? = null,
    val date: LocalDate = LocalDate.ofEpochDay(0),
    val note: String = "",
    val events: MutableList<JournalEntryEvent> = mutableListOf(),
    var scores: MutableMap<String, Int> = mutableMapOf()
)
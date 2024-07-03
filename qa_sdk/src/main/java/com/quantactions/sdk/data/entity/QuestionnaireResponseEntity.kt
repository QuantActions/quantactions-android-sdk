/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Entity of the table questionnaires_response containing all information about a questionnaire response.
 * The fullID is defined as the concatenation (with ':') between the study_id and the questionnaire code.
 * The response is a JSON encoded hash-map with key-value pairs where the keys are the questions code
 * and the values and the numeric values of the response.
 * @suppress
 */
@Entity(tableName = "questionnaire_responses")
@JsonClass(generateAdapter = true)
data class QuestionnaireResponseEntity(
    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "qFullID")
    val qFullID: String,

    @ColumnInfo(name = "qName")
    val qName: String,

    @ColumnInfo(name = "qCode")
    val qCode: String,

    @ColumnInfo(name = "qDate")
    val qDate: Long,

    @ColumnInfo(name = "qResponse")
    val qResponse: String
)
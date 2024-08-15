/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Class containing all information about a questionnaire that the user can fill in.
 */
@Keep
@Entity(tableName = "questionnaires")
@JsonClass(generateAdapter = true)
data class Questionnaire(
    /** UUID of the questionnaire */
    @PrimaryKey
    val id: String,

    /** Name of the questionnaire */
    @Keep
    @ColumnInfo(name = "qName")
    val questionnaireName: String,

    /** Informal description of the questionnaire */
    @Keep
    @ColumnInfo(name = "qDescription")
    val questionnaireDescription: String,

    /** Code - only used internally */
    @Keep
    @ColumnInfo(name = "qCode")
    val questionnaireCode: String,

    /** Cohort ID to which this questionnaire is bound */
    @Keep
    @ColumnInfo(name = "qStudy")
    val questionnaireCohort: String,

    /** Body of the questionnaire in JSON format */
    @Keep
    @ColumnInfo(name = "qBody")
    val questionnaireBody: String
)
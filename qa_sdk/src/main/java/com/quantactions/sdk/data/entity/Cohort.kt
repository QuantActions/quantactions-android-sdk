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
 * When subscribing to a cohort with a cohort ID or a subscription ID, the call will return an
 * object of this class which contains all information related to the cohort including necessary
 * permissions and privacy policy.
 */
@Entity(tableName = "studies")
@JsonClass(generateAdapter = true)
@Keep
data class Cohort(
    /** Identification UUID fo the cohort */
    @PrimaryKey
    @ColumnInfo(name = "studyId")
    val cohortId: String,

    /** Privacy policy */
    @ColumnInfo(name = "privacyPolicy")
    val privacyPolicy: String?,

    /** Human readable title */
    @ColumnInfo(name = "studyTitle")
    val cohortName: String?,

    /** Data pattern - disregard, this is only used internally */
    @ColumnInfo(name = "dataPattern")
    val dataPattern: String?,

    /** Deprecated - we do not use gps in newer versions */
    @ColumnInfo(name = "gpsResolution")
    val gpsResolution: Int,

    /** It is 1 if the device is allowed to withdraw, if 0 only the cohort manager can withdraw
     * the device  */
    @ColumnInfo(name = "canWithdraw")
    val canWithdraw: Int,

    /** Deprecated - we do not use this functionality anymore */
    @ColumnInfo(name = "syncOnScreenOff")
    val syncOnScreenOff: Int?,

    /** Deprecated - we do not use this functionality anymore */
    @ColumnInfo(name = "perimeterCheck")
    val perimeterCheck: Int?,

    /** Is app id permission necessary for this cohort */
    @ColumnInfo(name = "permAppId")
    val permAppId: Int?,

    /** Is draw over permission necessary for this cohort */
    @ColumnInfo(name = "permDrawOver")
    val permDrawOver: Int?,

    /** Deprecated - we do not use location anymore */
    @ColumnInfo(name = "permLocation")
    val permLocation: Int?,

    /** Deprecated - we do not use this functionality anymore */
    @ColumnInfo(name = "permContact")
    val permContact: Int?
)
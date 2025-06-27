/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.responses

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass



/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
class Questionnaire {
    var id: String? = null
    var title: String? = null
    var description: String? = null
    var definition: String? = null
    var created: String? = null
    var modified: String? = null
    var completionTimeMinutes: Int? = null
}

/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
class Study {
    var id: String? = null
    var title: String? = null
    var description: String? = null
    var emailTemplate: String? = null
    var privacyPolicy: String? = null
    var privacyPolicyDate: String? = null
    var dataPattern: String? = null
    var canWithdraw = 0
    var includeDeviceNotes = 0
    var rawDataAccess = 0
    var deviceIdAccess = 0
    var perimeterCheck = 0
    var syncOnScreenOff = 0
    var gpsResolution = 0
    var permAppId = 0
    var permDrawOver = 0
    var permLocation = 0
    var permContact = 0
    var studyIdSignUpAccess = 0
    var created: String? = null
    var modified: String? = null
    var userId: String? = null
    var questionnaires: List<Questionnaire>? = null
    var premiumFeaturesTTL: Int? = null
    var enableCognitiveTests: Boolean? = false
}





/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
class StudySignupResponse {
    var participationId: String? = null
    var privacyPolicy: String? = null
    var privacyPolicyDate: String? = null
    var studyId: String? = null
    var study: Study? = null
    var studyTitle: String? = null
    var dataPattern: String? = null
    var perimeterCheck = 0
    var syncOnScreenOff = 0
    var gpsResolution = 0
    var canWithdraw = 0
    var permAppId = 0
    var permDrawOver = 0
    var permLocation = 0
    var permContact = 0
    var deviceParticipationId: String? = null
    var participationModified: Long? = null
    var participationTtlInMillis: Long? = null
    var deviceParticipations: List<DeviceParticipation>? = null
}






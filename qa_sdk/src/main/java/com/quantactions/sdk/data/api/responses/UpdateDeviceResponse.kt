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
class Participations {
    var participations: List<DeviceParticipation>? = null
}

/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
class DeviceParticipation {
    var id: String? = null
    var participationId: String? = null
    var tapDeviceId: String? = null
    var privacyPolicyDate: String? = null
    var dates: String? = null
    var lastCheckInDate: String? = null
    var lastCheckOutDate: String? = null
    var center: String? = null
    var created: Long? = null
    var modified: String? = null
    var participation: Participation? = null
}

/**
 * @suppress
 */
@Keep
@JsonClass(generateAdapter = true)
class Participation {
    var id: String? = null
    var studyId: String? = null
    var study: Study? = null
    var modified: Long? = null
    var ttlInMillis: Long? = null
    var deviceParticipations: List<DeviceParticipation>? = null
}
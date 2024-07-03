/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.responses

import androidx.annotation.Keep

/**
 * Generic response for a call to the TapCloud API.
 * @suppress
 */
@Keep
data class PushTapDataResponse(
    val invalidTapsRecordingSessionIds: List<String>,
    val invalidHealthRecordingSessionIds: List<String>
)
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

@JsonClass(generateAdapter = true)
@Serializable
/**
 * @hide
 * Data structure to submit the health data. The health data should be submitted as a string with
 * one entry per line. Each line is formatted as [event, battery, UNIX timestamp] e.g.:
 * OK 65 1652966999515
 * The events can be:
 * OK: normal check
 * RS: The background service was not running and has been RESTARTED.
 * RB: The phone has been rebooted.
 */
data class DeviceHealth(
    val healthData: String,         // see above
    val type: String = "Uptime"
)

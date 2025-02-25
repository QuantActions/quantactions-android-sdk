/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import android.os.Build
import com.quantactions.sdk.BuildConfig
import com.squareup.moshi.JsonClass
import java.util.Locale

/**
 * @hide
 */
@JsonClass(generateAdapter = true)
data class DeviceRegistration(

    val deviceSpecificationId: String? = null,
    val enableAppIdAccess: Boolean,
    val enableDrawOverAccess: Boolean,
    val enableLocationAccess: Boolean,
    val enablePushNotification: Boolean = true,
    val firebaseTokenId: String? = null,
    val language: String = Locale.getDefault().language,
    val osType: String = "android",
    val osVersion: String = Build.VERSION.RELEASE,
    val sdkVersion: String = BuildConfig.VERSION_NAME,
    val packageUsingSdk: String,
    val hardwareVersion: String = Build.MODEL,
    val hardwareVendor: String = Build.MANUFACTURER,

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class DeviceResponse(

    val deviceSpecificationId: String? = null,
    val enableAppIdAccess: Boolean,
    val enableDrawOverAccess: Boolean,
    // FIXME: We'll keep the name for now to avoid breaking changes, but is should be activityRecognition
    val enableLocationAccess: Boolean,
    val enablePushNotification: Boolean = true,
    val firebaseTokenId: String? = null,
    val id: String,
    val language: String = Locale.getDefault().language,
    val osType: String = "android",
    val osVersion: String = Build.VERSION.RELEASE,
    val sdkVersion: String = BuildConfig.VERSION_NAME,
    val hardwareVersion: String = Build.MODEL,
    val hardwareVendor: String = Build.MANUFACTURER,

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class DevicePatch(

    val enableAppIdAccess: Boolean,
    val enableDrawOverAccess: Boolean,
    val enableLocationAccess: Boolean = false,
    val enablePushNotification: Boolean = true,
    val firebaseTokenId: String? = null,
    val language: String = Locale.getDefault().language,
    val osType: String = "android",
    val osVersion: String = Build.VERSION.RELEASE,
    val sdkVersion: String = BuildConfig.VERSION_NAME,
    val packageUsingSdk: String,
    val hardwareVersion: String = Build.MODEL,
    val hardwareVendor: String = Build.MANUFACTURER,

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class DeviceStatus(

    val state: String = "ACTIVE",
    val reason: List<String> = listOf(),

)

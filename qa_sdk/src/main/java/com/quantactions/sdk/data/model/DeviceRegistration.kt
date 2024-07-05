/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import android.os.Build
import com.quantactions.sdk.BuildConfig
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * @hide
 */
@JsonClass(generateAdapter = true)
data class DeviceRegistration(

    val deviceSpecificationId: String? = null,
    val enableAppIdAccess: Boolean,
    val enableDrawOverAccess: Boolean,
    val enableLocationAccess: Boolean = false,
    val enablePushNotification: Boolean = true,
    val firebaseTokenId: String? = null,
    val language: String = Locale.getDefault().language,
    val osType: String = "android",
    val osVersion: String = Build.VERSION.RELEASE,
    val sdkVersion: String = BuildConfig.VERSION_NAME,
    val packageUsingSdk: String

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class DeviceResponse(

    val deviceSpecificationId: String? = null,
    val enableAppIdAccess: Boolean,
    val enableDrawOverAccess: Boolean,
    val enableLocationAccess: Boolean = false,
    val enablePushNotification: Boolean = true,
    val firebaseTokenId: String? = null,
    val id: String,
    val language: String = Locale.getDefault().language,
    val osType: String = "android",
    val osVersion: String = Build.VERSION.RELEASE,
    val sdkVersion: String = BuildConfig.VERSION_NAME

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
    val packageUsingSdk: String

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class DeviceStatus(

    val state: String = "ACTIVE",
    val reason: List<String> = listOf(),

)
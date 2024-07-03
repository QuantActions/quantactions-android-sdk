/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * This object hold the information returned by the call [QA.getSubscriptionId]. It returns the
 * subscriptionId, the list of deviceIds connected to this subscription and the cohortId relative to
 * the subscription.
 * */
@Keep
@JsonClass(generateAdapter = true)
class Subscription(
    /** Of the form `138e...28eb` */
    val subscriptionId: String,
    /** List of UUIDs */
    val deviceIds: List<String>,
    /** Of the form `aef3...de19` */
    val cohortId: String,
    val cohortName: String,
    val premiumFeaturesTTL: Long,
    val token: String?
    )
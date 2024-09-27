/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

/**
 * Interface to manage the preferences of the SDK.
 * @suppress
 */
interface GenericPreferences {

    var gender: QA.Gender

    var yearOfBirth: Int

    var selfDeclaredHealthy: Boolean

    var identityId: String

    var password: String?

    val accessToken: String?

    val refreshToken: String?

    var areCredentialsRegistered: Boolean

    var isOauthActivated: Boolean

    fun saveAccessTokens(accessToken: String? = null, refreshToken: String? = null)

}
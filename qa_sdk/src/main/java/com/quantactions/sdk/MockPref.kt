/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral", "unused")

package com.quantactions.sdk

import android.content.Context


/**
 * @hide
 */
class MockPref private constructor(context: Context) : GenericPreferences {

    private var sharedPref = context.getSharedPreferences("mock_preferences", Context.MODE_PRIVATE)

    override var gender: QA.Gender = QA.Gender.UNKNOWN

    override var yearOfBirth: Int = 1985

    override var selfDeclaredHealthy: Boolean = true

    var apiKey: String
        get() = sharedPref.getString(API_KEY, "")!!
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putString(API_KEY, newVal)
            editor.apply()
        }

    override var identityId: String = BuildConfig.QA_SAMPLE_ID

    override var password: String? = BuildConfig.QA_SAMPLE_PASSWORD

    override val accessToken: String?
        get() = sharedPref.getString(ACCESS_TOKEN, null)

    override val refreshToken: String?
        get() = sharedPref.getString(REFRESH_TOKEN, null)

    override var areCredentialsRegistered: Boolean = true

    override var isOauthActivated: Boolean = true

    override fun saveAccessTokens(accessToken: String?, refreshToken: String?) {
        val editor = sharedPref.edit()
        accessToken?.let{ editor.putString(ACCESS_TOKEN, accessToken) }
        refreshToken?.let{ editor.putString(REFRESH_TOKEN, refreshToken) }
        editor.apply()
    }


    companion object : SingletonHolder<MockPref, Context>(::MockPref){
        const val API_KEY                        = "api_key"
        const val ACCESS_TOKEN                   = "access_token"
        const val REFRESH_TOKEN                  = "refresh_token"
    }
}

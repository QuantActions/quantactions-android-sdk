/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api

import android.content.Context
import com.quantactions.sdk.CapabilitiesManager
import com.quantactions.sdk.ManagePref2
import timber.log.Timber

/**
 * Manages the lifecycle of API services and their dependencies.
 */
internal class NetworkManager private constructor(
    private val preferences: ManagePref2,
    val capabilitiesManager: CapabilitiesManager
) {
    private var _apiService: ApiService? = null
    private var _tokenApi: TokenApi? = null
    private var currentApiKey: String? = null

    val apiService: ApiService
        get() = _apiService ?: synchronized(this) {
            _apiService ?: initServices(preferences.apiKey).let { _apiService!! }
        }

    val tokenApi: TokenApi
        get() = _tokenApi ?: synchronized(this) {
            _tokenApi ?: initServices(preferences.apiKey).let { _tokenApi!! }
        }

    @Synchronized
    fun initServices(apiKey: String): NetworkManager {
        if (apiKey == currentApiKey && _apiService != null && _tokenApi != null) {
            return this
        }

        Timber.d("Initializing Network Services with API Key: ${apiKey.take(4)}...")
        
        val cookieJar = ApiService.UvCookieJar(preferences, "NetworkManager")
        val tokenApi = TokenApi.buildTokenApi(apiKey, cookieJar)
        val authenticator = TokenAuthenticator(tokenApi, preferences, capabilitiesManager)
        
        _tokenApi = tokenApi
        _apiService = ApiService.create(apiKey, authenticator, cookieJar)
        currentApiKey = apiKey
        
        return this
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkManager? = null

        fun getInstance(
            context: Context,
            preferences: ManagePref2 = ManagePref2.getInstance(context),
            capabilitiesManager: CapabilitiesManager = CapabilitiesManager(context, preferences)
        ): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkManager(preferences, capabilitiesManager).also { INSTANCE = it }
            }
        }
    }
}

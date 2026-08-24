/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.QA
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.TokenApi
import com.quantactions.sdk.data.api.getBasicAuthHeader
import timber.log.Timber

/**
 * Repository for managing user identity and registration status.
 */
internal class IdentityRepository(
    private val preferences: ManagePref2,
    private val networkManager: NetworkManager
) {
    val identityId: String get() = preferences.identityId
    val deviceID: String get() = preferences.deviceID
    val password: String? get() = preferences.password

    suspend fun checkRegisteredStatus(): Boolean {
        if (preferences.areCredentialsRegistered && preferences.isOauthActivated) {
            Timber.d("Identity is registered -> I continue")
            return true
        }

        if (preferences.identityId == "") return false

        val tokenApi = networkManager.tokenApi

        if (preferences.areCredentialsRegistered && !preferences.isOauthActivated) {
            return when (val response = tokenApi.enableOauth(getBasicAuthHeader(preferences))) {
                is ApiEmptyResponse, is ApiSuccessResponse -> {
                    preferences.isOauthActivated = true
                    when (val response2 = tokenApi.login(getBasicAuthHeader(preferences))) {
                        is ApiEmptyResponse, is ApiSuccessResponse -> true
                        is ApiErrorResponse -> {
                            Timber.e("intercept CheckOauthStatus : $response2")
                            false
                        }
                    }
                }
                is ApiErrorResponse -> {
                    Timber.e("intercept CheckOauthStatus : $response")
                    false
                }
            }
        }

        val identityRegistration = TokenApi.IdentityRegistration(
            id = preferences.identityId,
            password = preferences.password,
            gender = preferences.gender.code,
            yearOfBirth = if (preferences.yearOfBirth != 0) preferences.yearOfBirth else null,
            selfDeclaredHealthy = preferences.selfDeclaredHealthy
        )

        when (val response = tokenApi.registerIdentity(identityRegistration)) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                preferences.areCredentialsRegistered = true
                when (val response2 = tokenApi.enableOauth(getBasicAuthHeader(preferences))) {
                    is ApiEmptyResponse, is ApiSuccessResponse -> {
                        preferences.isOauthActivated = true
                    }
                    is ApiErrorResponse -> {
                        Timber.e("intercept CheckOauthStatus : $response2")
                        return false
                    }
                }
            }
            is ApiErrorResponse -> {
                Timber.e("CheckRegisteredStatus : $response")
                return false
            }
        }
        return true
    }

    suspend fun linkIdentities(idToLink: String): ApiResponse<TokenApi.Identity> {
        return networkManager.apiService.linkIdentities(preferences.identityId, ApiService.AuthLinkBody(idToLink))
    }

    suspend fun updateIdentity(): ApiResponse<TokenApi.Identity> {
        val patch = ApiService.IdentityPatch(
            gender = preferences.gender.code!!,
            yearOfBirth = preferences.yearOfBirth,
            selfDeclaredHealthy = preferences.selfDeclaredHealthy
        )
        return networkManager.apiService.patchIdentity(preferences.identityId, patch)
    }

    suspend fun updateOldPassword(newPassword: String): ApiResponse<TokenApi.Identity> {
        return networkManager.apiService.patchCredentials(
            identityId,
            ApiService.CredentialsRegistration(
                identityId = identityId,
                password = newPassword,
            )
        )
    }

    suspend fun getIdentity(context: String): TokenApi.Identity {
        return when (val response = networkManager.apiService.getIdentity(preferences.identityId)) {
            is ApiSuccessResponse -> response.body!!
            else -> throw Exception("Failed to get identity for $context")
        }
    }

    fun updateBasicInfo(yearOfBirth: Int, gender: QA.Gender, selfDeclaredHealthy: Boolean) {
        preferences.yearOfBirth = yearOfBirth
        preferences.gender = gender
        preferences.selfDeclaredHealthy = selfDeclaredHealthy
    }

    fun getBasicInfo(): BasicInfo {
        return BasicInfo(
            preferences.yearOfBirth,
            preferences.gender,
            preferences.selfDeclaredHealthy
        )
    }
}

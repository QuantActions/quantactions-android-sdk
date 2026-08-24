/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import android.content.Context
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.TapsStats
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.responses.RegistrationResponse
import com.quantactions.sdk.data.entity.ActivityTransitionEntity
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.model.AppWithCategory
import com.quantactions.sdk.data.model.DevicePatch
import com.quantactions.sdk.data.model.DeviceRegistration
import com.quantactions.sdk.data.model.DeviceSpecifications
import com.quantactions.sdk.data.model.DeviceSpecificationsResponse
import com.quantactions.sdk.data.model.DeviceStats
import com.quantactions.sdk.data.model.DeviceStatsResponse
import com.quantactions.sdk.data.model.Note
import com.quantactions.sdk.data.model.TapDataParsed
import com.quantactions.sdk.exceptions.QASDKException

/**
 * Repository for managing device registration, specifications, and data submission.
 */
internal class DeviceRepository(
    private val context: Context,
    private val preferences: ManagePref2,
    private val identityRepository: IdentityRepository,
    private val networkManager: NetworkManager,
    internal val mvpDao: MVPDao
) {
    private val packageName: String = context.packageName
    private val firebaseToken: String get() = preferences.getFBCode()

    suspend fun registerUser(coreDeviceSpecificationId: String): ApiResponse<RegistrationResponse> {
        val deviceRegistration = DeviceRegistration(
            deviceSpecificationId = coreDeviceSpecificationId,
            enableAppIdAccess = preferences.canUsage(context),
            enableDrawOverAccess = preferences.canDraw(context),
            enableLocationAccess = preferences.canActivity(context),
            firebaseTokenId = firebaseToken,
            packageUsingSdk = packageName
        )
        return networkManager.apiService.registerDevice(identityRepository.identityId, deviceRegistration)
    }

    suspend fun registerDeviceSpecifications(): ApiResponse<DeviceSpecificationsResponse> {
        return networkManager.apiService.registerDeviceSpecifications(identityRepository.identityId, DeviceSpecifications())
    }

    suspend fun updateDeviceInfo(): ApiResponse<DeviceRegistration> {
        val deviceInfo = DevicePatch(
            enableAppIdAccess = preferences.canUsage(context),
            enableDrawOverAccess = preferences.canDraw(context),
            enableLocationAccess = preferences.canActivity(context),
            firebaseTokenId = firebaseToken,
            packageUsingSdk = packageName
        )
        return networkManager.apiService.updateDevice(identityRepository.identityId, identityRepository.deviceID, deviceInfo)
    }

    suspend fun submitStatistic(deviceStats: DeviceStats? = null): ApiResponse<DeviceStatsResponse> {
        return networkManager.apiService.submitStatistic(
            identityRepository.identityId,
            identityRepository.deviceID,
            deviceStats ?: TapsStats(mvpDao).toDeviceStats()
        )
    }

    suspend fun submitNote(note: String, fakeDeviceID: String? = null) {
        val apiResponse = networkManager.apiService.submitNote(
            fakeDeviceID ?: identityRepository.identityId,
            Note(note)
        )
        when (apiResponse) {
            is ApiSuccessResponse -> {}
            is ApiErrorResponse -> throw QASDKException("API Error [submitNote()]: ${apiResponse.errorMessage}")
            is ApiEmptyResponse -> throw QASDKException("Response Body to submitNote is empty")
        }
    }

    suspend fun updateAppList(listOfApps: List<AppToPush>): ApiResponse<List<AppWithCategory>> {
        return networkManager.apiService.updateAppList(identityRepository.identityId, identityRepository.deviceID, listOfApps)
    }

    suspend fun submitActivity(activityBody: ActivityBody): ApiResponse<ActivityBody> {
        return networkManager.apiService.submitActivity(identityRepository.identityId, identityRepository.deviceID, activityBody)
    }

    suspend fun submitTapDataParsed(tapDataBody: TapDataBody): ApiResponse<TapDataBody> {
        return networkManager.apiService.submitTap(identityRepository.identityId, identityRepository.deviceID, tapDataBody)
    }

    suspend fun submitHealthDataParsed(healthDataBody: HealthDataBody): ApiResponse<HealthDataBody> {
        return networkManager.apiService.submitHealth(identityRepository.identityId, identityRepository.deviceID, healthDataBody)
    }

    fun setDeviceId(id: String) { preferences.deviceID = id }
    fun saveDeviceSpecificationsId(id: String) { preferences.deviceSpecificationsId = id }

    suspend fun getConnectedDevices(): List<String> {
        return when (val response = networkManager.apiService.getConnectedDevices(identityRepository.identityId)) {
            is ApiSuccessResponse -> response.body?.map { it.id } ?: listOf()
            else -> listOf()
        }
    }

    // Local DAO delegations
    fun getLatestTaps(rollBackDate: String): List<HourlyTapsEntity> = mvpDao.getLatestTaps(rollBackDate)
    fun getTapsInTimeWindow(start: Long, end: Long): List<TapDataParsed> = mvpDao.getTapsInTimeWindow(start, end)
    fun getAppCodesByIds(ids: List<Int>): List<CodeOfApp> = mvpDao.getAppCodesByIds(ids)
    fun insertOrUpdateHourlyTapsEntity(entities: List<HourlyTapsEntity>) = mvpDao.insertOrUpdateHourlyTapsEntity(entities)
    fun insertOrUpdateAppCode(entities: List<CodeOfApp>) = mvpDao.insertOrUpdateAppCode(entities)
    fun getPendingAppCodes(): List<CodeOfApp> = mvpDao.getPendingAppCodes()
    fun getAppsWithPendingCategory(): List<CodeOfApp> = mvpDao.getAppsWithPendingCategory()
    fun updateCodeOfApp(name: String, isSync: Int, category: String?) = mvpDao.updateCodeOfApp(name, isSync, category)
    fun resetAppCodesSyncStatus() = mvpDao.resetAppCodesSyncStatus()
    fun getTapDataParsedToSync(): List<TapDataParsed> = mvpDao.getTapDataParsedToSync()
    fun updateTapDataParsedSyncStatus(ids: List<Long>) = mvpDao.updateTapDataParsedSyncStatus(ids)
    fun removeInvalidTapSessions(ids: List<Int>) = mvpDao.removeInvalidTapSessions(ids)
    fun removeInvalidTapSessionsFromStart(ids: List<Long>) = mvpDao.removeInvalidTapSessionsFromStart(ids)
    fun getDeviceHealthParsedToSync(): List<DeviceHealthParsed> = mvpDao.getDeviceHealthParsedToSync()
    fun updateDeviceHealthParsedSyncStatus(ids: List<Long>) = mvpDao.updateDeviceHealthParsedSyncStatus(ids)
    fun getActivityToSync(): List<ActivityTransitionEntity> = mvpDao.getActivityToSync()
    fun updateActivitySyncStatus(ids: List<Long>) = mvpDao.updateActivitySyncStatus(ids)
    fun deleteWrongHealthSessions(ids: List<String>) = mvpDao.deleteWrongHealthSessions(ids)
    fun deleteWrongTapSessions(ids: List<String>) = mvpDao.deleteWrongTapSessions(ids)
    fun deleteLocalStudies() = mvpDao.deleteStudies()
}

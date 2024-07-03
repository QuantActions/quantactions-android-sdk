/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral", "unused")

package com.quantactions.sdk

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * @hide
 */
class ManagePref2 private constructor(context: Context) {

    private var sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val databaseHelper = DatabaseHelper.getInstance(context)

    private val sqLiteDatabase = databaseHelper.writableDatabase

    var apiKey: String
        get() = sharedPref.getString(API_KEY, "")!!
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putString(API_KEY, newVal)
            editor.apply()
        }

    var deviceSpecificationsId: String
        get() = sharedPref.getString(DEVICE_SPECS, "")!!
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putString(DEVICE_SPECS, newVal)
            editor.apply()
        }

    var isDataCollectionPaused: Boolean
        get() = sharedPref.getBoolean(IS_DATA_COLLECTION_PAUSED, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(IS_DATA_COLLECTION_PAUSED, newVal)
            editor.apply()
        }

    var oldToNewDBMigrationDone: Boolean
        get() = sharedPref.getBoolean(OLD_TO_NEW_DB_MIGRATION_DONE, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(OLD_TO_NEW_DB_MIGRATION_DONE, newVal)
            editor.apply()
        }

    var oldToNewAPIMigrationDone: Boolean
        get() = sharedPref.getBoolean(OLD_TO_NEW_API_MIGRATION_DONE, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(OLD_TO_NEW_API_MIGRATION_DONE, newVal)
            editor.apply()
        }

    var gender: QA.Gender
        get() = QA.Gender.fromInt(sharedPref.getInt(GENDER, 0)) ?: QA.Gender.UNKNOWN
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putInt(GENDER, newVal.id)
            editor.apply()
        }

    var yearOfBirth: Int
        get() = sharedPref.getInt(YEAR_OF_BIRTH, 0)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putInt(YEAR_OF_BIRTH, newVal)
            editor.apply()
        }

    var selfDeclaredHealthy: Boolean
        get() = sharedPref.getBoolean(SELF_DECLARED_HEALTHY, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(SELF_DECLARED_HEALTHY, newVal)
            editor.apply()
        }

    var shouldRestartDataCollection: Boolean
        get() = sharedPref.getBoolean(SHOULD_RESTART_DATA_COLLECTION, true)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(SHOULD_RESTART_DATA_COLLECTION, newVal)
            editor.apply()
        }

    var deviceID: String
        get() = sharedPref.getString(DEVICE_ID, "")!!
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putString(DEVICE_ID, newVal)
            editor.apply()
        }

    var identityId: String
        get() = sharedPref.getString(IAM_IDENTITY_ID, "")!!
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putString(IAM_IDENTITY_ID, newVal)
            editor.apply()
        }

    val date: Int
        get() {
            val calendar = Calendar.getInstance()
            val date = calendar.time
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return Integer.parseInt(sdf.format(date))
        }

    var isDeviceRegistered: Boolean
        get() = sharedPref.getBoolean(REGISTERED_STATUS, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(REGISTERED_STATUS, newVal)
            editor.apply()
        }

    var areCredentialsRegistered: Boolean
        get() = sharedPref.getBoolean(CREDENTIALS_STATUS, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(CREDENTIALS_STATUS, newVal)
            editor.apply()
        }

    var isOauthActivated: Boolean
        get() = sharedPref.getBoolean(OAUTH_STATUS, false)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putBoolean(OAUTH_STATUS, newVal)
            editor.apply()
        }

    var password: String?
        get() = sharedPref.getString(PASSWORD, null)
        set(newVal) {
            val editor = sharedPref.edit()
            editor.putString(PASSWORD, newVal)
            editor.apply()
        }

    val accessToken: String?
        get() = sharedPref.getString(ACCESS_TOKEN, null)

    val refreshToken: String?
        get() = sharedPref.getString(REFRESH_TOKEN, null)

    fun saveAccessTokens(accessToken: String? = null, refreshToken: String? = null) {
        val editor = sharedPref.edit()
        accessToken?.let{ editor.putString(ACCESS_TOKEN, accessToken) }
        refreshToken?.let{ editor.putString(REFRESH_TOKEN, refreshToken) }
        editor.apply()
    }

    fun getDebugMode(): Boolean {
        return sharedPref.getBoolean(DEBUG_MODE, false)
    }

    fun setDebugMode(status: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(DEBUG_MODE, status)
        editor.apply()
    }

    fun getPendingFirebaseToken(): Boolean {
        return sharedPref.getBoolean(PENDING_FB, false)
    }

    fun setPendingFirebaseToken(status: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(PENDING_FB, status)
        editor.apply()
    }

    fun getHasFinishedOnboarding(): Boolean {
        return sharedPref.getBoolean(ONBOARDING_FINISHED, false)
    }

    fun setHasFinishedOnboarding(status: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(ONBOARDING_FINISHED, status)
        editor.apply()
    }

    fun setGPSResolution(resolution: Int) {
        val editor = sharedPref.edit()
        editor.putInt(GPS_RESOLUTION, resolution)
        editor.apply()
    }

    fun getVerbose(): Int {
        return sharedPref.getInt(SAVED_VERBOSE, 0)
    }

    fun setVerbose(verbose: Int) {
        val editor = sharedPref.edit()
        editor.putInt(SAVED_VERBOSE, verbose)
        editor.apply()
    }

    fun getCheckIn(): Boolean {
        return sharedPref.getBoolean(SAVED_CHECK_IN_STATUS, false)
    }

    internal fun setCheckIn(checkIn: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(SAVED_CHECK_IN_STATUS, checkIn)
        editor.apply()
    }

    fun getSavedPermissions(): String {
        return sharedPref.getString(SAVED_PERM, "-1-1-1-1")!!
    }

    internal fun setSavedPermissions(context: Context){
        val editor = sharedPref.edit()
        editor.putString(SAVED_PERM, getPermissionsStatus(context))
        editor.apply()
    }

    fun getSavedAppsCount(): Int {
        return sharedPref.getInt(SAVED_APP_COUNT, 0)
    }

    internal fun setSavedAppsCount() {
        val editor = sharedPref.edit()
        editor.putInt(SAVED_APP_COUNT, getAppCount())
        editor.apply()
    }

    fun getSavedDate(): Int {
        return sharedPref.getInt(SAVED_DATE, 0)
    }

    internal fun setSavedDate() {
        val editor = sharedPref.edit()
        editor.putInt(SAVED_DATE, date)
        editor.apply()
    }

    internal fun setCenterPending(centerPending: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(CENTER_PENDING, centerPending)
        editor.apply()
    }

    fun getCenterPending(): Boolean {
        return sharedPref.getBoolean(CENTER_PENDING, false)
    }

    internal fun getDrawNeeded(): Boolean {
        return sharedPref.getBoolean(DRAW_NEEDED, true)
    }

    internal fun setDrawNeeded(draw: Boolean){
        val editor = sharedPref.edit()
        editor.putBoolean(DRAW_NEEDED, draw)
        editor.apply()
    }

    internal fun getContactsNeeded(): Boolean {
        return sharedPref.getBoolean(CONTACTS_NEEDED, false)
    }

    internal fun setContactsNeeded(contacts: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(CONTACTS_NEEDED, contacts)
        editor.apply()
    }

    internal fun getAppIdNeeded(): Boolean {
        return sharedPref.getBoolean(APP_ID_NEEDED, true)
    }

    internal fun setAppIdNeeded(appId: Boolean){
        val editor = sharedPref.edit()
        editor.putBoolean(APP_ID_NEEDED, appId)
        editor.apply()
    }

    internal fun getLocationNeeded(): Boolean {
        return sharedPref.getBoolean(LOCATION_NEEDED, false)
    }

    internal fun setLocationNeeded(location: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean(LOCATION_NEEDED, location)
        editor.apply()
    }

    internal fun setPendingSignUp(signUp: String) {
        val editor = sharedPref.edit()
        editor.putString(PENDING_SIGN_UP, signUp)
        editor.apply()
    }

    internal fun getPendingSignUp(): String {
        return sharedPref.getString(PENDING_SIGN_UP, "")!!
    }

    fun getPermissionsStatus(context: Context): String {
        val ret = StringBuilder()
        ret.append(canUsage(context)).append(0).append(canDraw(context)).append(0)
        return ret.toString()
    }

    fun getAppCount(): Int {

        var count = 0
        val cursor = sqLiteDatabase.query(LookUp.TABLE_APP_CODE, null, null, null, null, null, null)

        if (null != cursor) {
            count = cursor.count
            cursor.close()
        }
        return count
    }

    fun getAuthCode(): String {
        return sharedPref.getString(AUTH_CODE, "")!!
    }

    fun setAuthCode(authCode: String){
        val editor = sharedPref.edit()
        editor.putString(AUTH_CODE, authCode)
        editor.apply()
    }

    fun getFBCode(): String {
        return sharedPref.getString(FB_CODE, "no_token")!!
    }

    fun setFBCode(fbCode: String){
        val editor = sharedPref.edit()
        editor.putString(FB_CODE, fbCode)
        editor.apply()
    }

    fun canDraw(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    fun canUsage(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid,
                applicationInfo.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    companion object : SingletonHolder<ManagePref2, Context>(::ManagePref2){
        const val API_KEY                        = "api_key"
        const val APP_ID_NEEDED                  = "app_id_needed"
        const val AUTH_CODE                      = "auth_code"
        const val CENTER_PENDING                 = "center_pending"
        const val CONTACTS_NEEDED                = "contacts_needed"
        const val CREDENTIALS_STATUS             = "credentials_status"
        const val DEBUG_MODE                     = "debug_mode"
        const val DEVICE_ID                      = "device_id"
        const val DRAW_NEEDED                    = "draw_needed"
        const val FB_CODE                        = "fb_code"
        const val GENDER                         = "gender"
        const val GPS_RESOLUTION                 = "gps_resolution"
        const val IAM_IDENTITY_ID                = "iam_identity_id"
        const val IS_DATA_COLLECTION_PAUSED      = "is_data_collection_paused"
        const val LOCATION_NEEDED                = "location_needed"
        const val OAUTH_STATUS                   = "oauth_status"
        const val OLD_TO_NEW_DB_MIGRATION_DONE   = "old_to_new_db_migration_done"
        const val ONBOARDING_FINISHED            = "onboarding_finished"
        const val PENDING_FB                     = "pending_fb"
        const val PENDING_SIGN_UP                = "pending_sign_up"
        const val SAVED_APP_COUNT                = "saved_app_count"
        const val SAVED_CHECK_IN_STATUS          = "is_check_in"
        const val SAVED_DATE                     = "saved_date"
        const val SAVED_PERM                     = "saved_perm"
        const val SAVED_VERBOSE                  = "verbose"
        const val SELF_DECLARED_HEALTHY          = "self_declared_healthy"
        const val SHOULD_RESTART_DATA_COLLECTION = "should_restart_data_collection"
        const val YEAR_OF_BIRTH                  = "year_of_birth"
        const val ACCESS_TOKEN                   = "access_token"
        const val REFRESH_TOKEN                  = "refresh_token"
        const val REGISTERED_STATUS              = "registered_status"
        const val PASSWORD                       = "password"
        const val DEVICE_SPECS                   = "device_specs"
        const val OLD_TO_NEW_API_MIGRATION_DONE  = "old_to_new_api_migration_done"
    }
}

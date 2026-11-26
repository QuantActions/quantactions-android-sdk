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

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * @hide
 */
class ManagePref2 private constructor(val context: Context) : GenericPreferences {

    private var sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val databaseHelper = DatabaseHelper.getInstance(context)

    private val sqLiteDatabase = databaseHelper.writableDatabase

    var apiKey: String
        get() = sharedPref.getString(API_KEY, "") ?: ""
        set(newVal) {
            sharedPref.edit {
                putString(API_KEY, newVal)
            }
        }

    var deviceSpecificationsId: String
        get() = sharedPref.getString(DEVICE_SPECS, "") ?: ""
        set(newVal) {
            sharedPref.edit {
                putString(DEVICE_SPECS, newVal)
            }
        }

    var isDataCollectionPaused: Boolean
        get() = sharedPref.getBoolean(IS_DATA_COLLECTION_PAUSED, false)
        set(newVal) {
            sharedPref.edit {
                putBoolean(IS_DATA_COLLECTION_PAUSED, newVal)
            }
        }

    var oldToNewDBMigrationDone: Boolean
        get() = sharedPref.getBoolean(OLD_TO_NEW_DB_MIGRATION_DONE, true)
        set(newVal) {
            sharedPref.edit {
                putBoolean(OLD_TO_NEW_DB_MIGRATION_DONE, newVal)
            }
        }

    var oldToNewAPIMigrationDone: Boolean
        get() = sharedPref.getBoolean(OLD_TO_NEW_API_MIGRATION_DONE, true)
        set(newVal) {
            sharedPref.edit {
                putBoolean(OLD_TO_NEW_API_MIGRATION_DONE, newVal)
            }
        }

    override var gender: QA.Gender
        get() = QA.Gender.fromInt(sharedPref.getInt(GENDER, 0)) ?: QA.Gender.UNKNOWN
        set(newVal) {
            sharedPref.edit {
                putInt(GENDER, newVal.id)
            }
        }

    override var yearOfBirth: Int
        get() = sharedPref.getInt(YEAR_OF_BIRTH, 0)
        set(newVal) {
            sharedPref.edit {
                putInt(YEAR_OF_BIRTH, newVal)
            }
        }

    override var selfDeclaredHealthy: Boolean
        get() = sharedPref.getBoolean(SELF_DECLARED_HEALTHY, false)
        set(newVal) {
            sharedPref.edit {
                putBoolean(SELF_DECLARED_HEALTHY, newVal)
            }
        }

    var shouldRestartDataCollection: Boolean
        get() = sharedPref.getBoolean(SHOULD_RESTART_DATA_COLLECTION, true)
        set(newVal) {
            sharedPref.edit {
                putBoolean(SHOULD_RESTART_DATA_COLLECTION, newVal)
            }
        }

    var deviceID: String
        get() = sharedPref.getString(DEVICE_ID, "") ?: ""
        set(newVal) {
            sharedPref.edit {
                putString(DEVICE_ID, newVal)
            }
        }

    override var identityId: String
        get() = sharedPref.getString(IAM_IDENTITY_ID, "") ?: ""
        set(newVal) {
            sharedPref.edit {
                putString(IAM_IDENTITY_ID, newVal)
            }
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
            sharedPref.edit {
                putBoolean(REGISTERED_STATUS, newVal)
            }
        }

    override var areCredentialsRegistered: Boolean
        get() = sharedPref.getBoolean(CREDENTIALS_STATUS, false)
        set(newVal) {
            sharedPref.edit {
                putBoolean(CREDENTIALS_STATUS, newVal)
            }
        }

    override var isOauthActivated: Boolean
        get() = sharedPref.getBoolean(OAUTH_STATUS, false)
        set(newVal) {
            sharedPref.edit {
                putBoolean(OAUTH_STATUS, newVal)
            }
        }

    override var password: String
        get() = getEncryptedOrMigrate(PASSWORD, "") ?: ""
        set(newVal) {
            putEncrypted(PASSWORD, newVal)
        }

    override var participationId: String
        get() = sharedPref.getString(PARTICIPATION_ID, "") ?: ""
        set(newVal) {
            sharedPref.edit {
                putString(PARTICIPATION_ID, newVal)
            }
        }

    override val accessToken: String?
        get() = getEncryptedOrMigrate(ACCESS_TOKEN, null)

    override val refreshToken: String?
        get() = getEncryptedOrMigrate(REFRESH_TOKEN, null)

    override fun saveAccessTokens(accessToken: String?, refreshToken: String?) {
        accessToken?.let { putEncrypted(ACCESS_TOKEN, it) }
        refreshToken?.let { putEncrypted(REFRESH_TOKEN, it) }
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existingKey = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existingKey != null) {
                return existingKey.secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            null
        }
    }

    private fun encryptValue(plainText: String): String {
        if (plainText.isEmpty()) return plainText
        val secretKey = getOrCreateSecretKey() ?: return plainText
        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(1 + iv.size + cipherText.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherText, 0, combined, 1 + iv.size, cipherText.size)
            ENCRYPTED_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            plainText
        }
    }

    private fun decryptValue(stored: String): String? {
        if (!stored.startsWith(ENCRYPTED_PREFIX)) return stored
        val secretKey = getOrCreateSecretKey() ?: return null
        return try {
            val raw = Base64.decode(stored.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
            val ivSize = raw[0].toInt() and 0xFF
            val iv = raw.copyOfRange(1, 1 + ivSize)
            val cipherText = raw.copyOfRange(1 + ivSize, raw.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun putEncrypted(key: String, value: String?) {
        sharedPref.edit {
            if (value == null) {
                remove(key)
            } else {
                putString(key, encryptValue(value))
            }
        }
    }

    private fun getEncryptedOrMigrate(key: String, defaultValue: String?): String? {
        val raw = sharedPref.getString(key, null) ?: return defaultValue
        if (raw.isEmpty()) return raw
        if (raw.startsWith(ENCRYPTED_PREFIX)) {
            return decryptValue(raw) ?: defaultValue
        }
        // Transparently migrate legacy plaintext credential to encrypted storage if Keystore is available
        val encrypted = encryptValue(raw)
        if (encrypted.startsWith(ENCRYPTED_PREFIX)) {
            sharedPref.edit { putString(key, encrypted) }
        }
        return raw
    }

    fun getDebugMode(): Boolean {
        return sharedPref.getBoolean(DEBUG_MODE, false)
    }

    fun setDebugMode(status: Boolean) {
        sharedPref.edit {
            putBoolean(DEBUG_MODE, status)
        }
    }

    fun getPendingFirebaseToken(): Boolean {
        return sharedPref.getBoolean(PENDING_FB, false)
    }

    fun setPendingFirebaseToken(status: Boolean) {
        sharedPref.edit {
            putBoolean(PENDING_FB, status)
        }
    }

    fun getHasFinishedOnboarding(): Boolean {
        return sharedPref.getBoolean(ONBOARDING_FINISHED, false)
    }

    fun setHasFinishedOnboarding(status: Boolean) {
        sharedPref.edit {
            putBoolean(ONBOARDING_FINISHED, status)
        }
    }

    fun setGPSResolution(resolution: Int) {
        sharedPref.edit {
            putInt(GPS_RESOLUTION, resolution)
        }
    }

    fun getVerbose(): Int {
        return sharedPref.getInt(SAVED_VERBOSE, 0)
    }

    fun setVerbose(verbose: Int) {
        sharedPref.edit {
            putInt(SAVED_VERBOSE, verbose)
        }
    }

    fun getCheckIn(): Boolean {
        return sharedPref.getBoolean(SAVED_CHECK_IN_STATUS, false)
    }

    internal fun setCheckIn(checkIn: Boolean) {
        sharedPref.edit {
            putBoolean(SAVED_CHECK_IN_STATUS, checkIn)
        }
    }

    fun getSavedPermissions(): String {
        return sharedPref.getString(SAVED_PERM, "-1-1-1-1") ?: "-1-1-1-1"
    }

    internal fun setSavedPermissions(context: Context){
        sharedPref.edit {
            putString(SAVED_PERM, getPermissionsStatus(context))
        }
    }

    fun getSavedAppsCount(): Int {
        return sharedPref.getInt(SAVED_APP_COUNT, 0)
    }

    internal fun setSavedAppsCount() {
        sharedPref.edit {
            putInt(SAVED_APP_COUNT, getAppCount())
        }
    }

    fun getSavedDate(): Int {
        return sharedPref.getInt(SAVED_DATE, 0)
    }

    internal fun setSavedDate() {
        sharedPref.edit {
            putInt(SAVED_DATE, date)
        }
    }

    internal fun setCenterPending(centerPending: Boolean) {
        sharedPref.edit {
            putBoolean(CENTER_PENDING, centerPending)
        }
    }

    fun getCenterPending(): Boolean {
        return sharedPref.getBoolean(CENTER_PENDING, false)
    }

    internal fun getDrawNeeded(): Boolean {
        return sharedPref.getBoolean(DRAW_NEEDED, true)
    }

    internal fun setDrawNeeded(draw: Boolean){
        sharedPref.edit {
            putBoolean(DRAW_NEEDED, draw)
        }
    }

    internal fun getContactsNeeded(): Boolean {
        return sharedPref.getBoolean(CONTACTS_NEEDED, false)
    }

    internal fun setContactsNeeded(contacts: Boolean) {
        sharedPref.edit {
            putBoolean(CONTACTS_NEEDED, contacts)
        }
    }

    internal fun getAppIdNeeded(): Boolean {
        return sharedPref.getBoolean(APP_ID_NEEDED, true)
    }

    internal fun setAppIdNeeded(appId: Boolean){
        sharedPref.edit {
            putBoolean(APP_ID_NEEDED, appId)
        }
    }

    internal fun getLocationNeeded(): Boolean {
        return sharedPref.getBoolean(LOCATION_NEEDED, false)
    }

    internal fun setLocationNeeded(location: Boolean) {
        sharedPref.edit {
            putBoolean(LOCATION_NEEDED, location)
        }
    }

    internal fun setPendingSignUp(signUp: String) {
        sharedPref.edit {
            putString(PENDING_SIGN_UP, signUp)
        }
    }

    internal fun getPendingSignUp(): String {
        return sharedPref.getString(PENDING_SIGN_UP, "") ?: ""
    }

    fun getPermissionsStatus(context: Context): String {
        val ret = StringBuilder()
        ret.append(canUsage(context)).append(0).append(canDraw(context)).append(0)
        return ret.toString()
    }

    fun getAppCount(): Int {

        val cursor = sqLiteDatabase.query(LookUp.TABLE_APP_CODE, null, null, null, null, null, null)

        val count = cursor.count
        cursor.close()
        return count
    }

    fun getAuthCode(): String {
        return sharedPref.getString(AUTH_CODE, "") ?: ""
    }

    fun setAuthCode(authCode: String){
        sharedPref.edit {
            putString(AUTH_CODE, authCode)
        }
    }

    fun getFBCode(): String {
        return sharedPref.getString(FB_CODE, "no_token") ?: "no_token"
    }

    fun setFBCode(fbCode: String){
        sharedPref.edit {
            putString(FB_CODE, fbCode)
        }
    }

    fun canActivity(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canDraw(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
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

    override fun saveHealthyRanges(code: String, ranges: PopulationRange) {
        sharedPref.edit {
            putString(
                "${HEALTHY_RANGES}_$code",
                Json.encodeToString(PopulationRange.serializer(), ranges)
            )
        }
    }

    override fun getHealthyRanges(code: String): PopulationRange {
        val ranges = sharedPref.getString("${HEALTHY_RANGES}_$code", null)
        return if (null != ranges) {
            Json.decodeFromString(PopulationRange.serializer(), ranges)
        } else {
            PopulationRange()
        }
    }

    override var capabilitiesToken: String?
        get() = sharedPref.getString(CAPABILITIES_TOKEN, null)
        set(newVal) {
            sharedPref.edit {
                putString(CAPABILITIES_TOKEN, newVal)
            }
        }

    var lastRecordedExitTimestamp: Long
        get() = sharedPref.getLong(LAST_RECORDED_EXIT_TIMESTAMP, 0L)
        set(newVal) {
            sharedPref.edit {
                putLong(LAST_RECORDED_EXIT_TIMESTAMP, newVal)
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
        const val HEALTHY_RANGES                 = "healthy_ranges"
        const val CAPABILITIES_TOKEN             = "capabilities_token"
        const val PARTICIPATION_ID               = "participation_id"
        const val LAST_RECORDED_EXIT_TIMESTAMP   = "last_recorded_exit_timestamp"
        private const val ANDROID_KEYSTORE       = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS         = "qa_sdk_prefs_aes_key"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ENCRYPTED_PREFIX       = "enc:v1:"
    }
}

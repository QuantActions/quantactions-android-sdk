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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.Keep
import androidx.work.WorkManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.quantactions.sdk.cognitivetests.CognitiveTest
import com.quantactions.sdk.cognitivetests.dotmemory.DotMemoryTestActivity
import com.quantactions.sdk.cognitivetests.dotmemory.DotMemoryTestResponse
import com.quantactions.sdk.cognitivetests.pvt.PVTActivity
import com.quantactions.sdk.cognitivetests.pvt.PVTResponse
import com.quantactions.sdk.data.api.adapters.SubscriptionWithQuestionnaires
import com.quantactions.sdk.data.entity.*
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.exceptions.QASDKException
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * This is the main element one needs to use to access all the functionality of the QA SDK.
 * As a singleton it can be called easily from anywhere in the code and gives access to all the
 * possible interactions with the QA backend, as well as some functions to retrieve user metrics.
 * Since most of the calls are asynchronous server interactions, they return  flows.
 *
 * @author <a href="mailto:support@quantactions.com">Enea Ceolini</a>
 * */
@Keep
class QA private constructor(
    private val qaPrivate: QAPrivate
) {

    /**
     * Used to retrieve the default singleton instance of [QA].
     */
    @Keep
    companion object {
        @Volatile
        private var INSTANCE: QA? = null

        /**
         * Retrieves the default singleton instance of [QA].
         *
         * @param context A Context for on-demand initialization.
         * @return The singleton instance of [QA].
         */
        @Keep
        fun getInstance(context: Context): QA {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    val qaPrivate = QAPrivate.getInstance(context)
                    Timber.d("Retrieving FB token! ==> ${qaPrivate.getFirebaseToken()}")
                    instance = QA(qaPrivate)
                    INSTANCE = instance
                    WorkManager.getInstance(context)
                }
                return instance
            }
        }
    }

    /** ID of the device */
    val deviceID: String
        get() = qaPrivate.deviceID
    val identityId: String
        get() = qaPrivate.identityId
    val password: String?
        get() = qaPrivate.password
    /** [BasicInfo] for the current user */
    val basicInfo: BasicInfo
        get() = qaPrivate.basicInfo
    /** Firebase token for communication */
    val firebaseToken: String?
        get() = qaPrivate.getFirebaseToken()

    var updater: DataCollectionNotification = UpdateTaps(this)

    fun setActivityPermissionNotification(activityPermissionNotification: ActivityPermissionNotification) {
        qaPrivate.activityPermissionNotification = activityPermissionNotification
    }

    /**
     * Pause the data collection.
     * @param context Android application context
     * */
    fun pauseDataCollection(context: Context) {
        qaPrivate.stopReadingService(context)
    }

    /**
     * This function check that the data collection is currently running.
     * @param context Android application context
     * @return a boolean indicating if the data collection is running or not
     * */
    fun isDataCollectionRunning(context: Context): Boolean {
        return qaPrivate.isDataCollectionRunning(context)
    }

    /**
     * Restart the data collection after it has been purposely paused.
     * @param context Android application context
     * */
    fun resumeDataCollection(context: Context) {
        return qaPrivate.resumeCollectionRunning(context)
    }

    /**
     * This internal class holds the string that identify requests for permissions.
     * @suppress
     */
    object Permissions {
        const val QA_ALREADY_GRANTED = QAStrings.QA_ALREADY_GRANTED
        const val QA_PERMISSION_REQUEST_OVERLAY = QAStrings.QA_PERMISSION_REQUEST_OVERLAY
        const val QA_PERMISSION_REQUEST_USAGE = QAStrings.QA_PERMISSION_REQUEST_USAGE
    }

    /**
     * This internal class holds the string that identify requests and notifications.
     * @suppress
     */
    object Strings {
        /**Flag used when extending [QAFirebaseMessagingService].
         * Use this flag to trigger a notification for the lack of the DRAW OVER permission.  */
        const val NOTIFY_DRAW = QAStrings.NOTIFY_DRAW

        /**Flag used when extending [QAFirebaseMessagingService].
         * Use this flag to trigger a notification for the lack of the USAGE over permission.  */
        const val NOTIFY_USAGE = QAStrings.NOTIFY_USAGE

        /**Flag used when extending [QAFirebaseMessagingService].
         * Use this flag to trigger a simple notification.  */
        const val NOTIFY_VANILLA = QAStrings.NOTIFY_VANILLA

        /**Flag used when extending [QAFirebaseMessagingService].
         * Use this flag to trigger a simple notification.  */
        const val NOTIFY_QUESTIONNAIRE = QAStrings.NOTIFY_QUESTIONNAIRE

        /**Flag used when extending [QAFirebaseMessagingService].
         * Use this flag to trigger a notification when an update is available.  */
        const val NOTIFY_UPDATE = QAStrings.NOTIFY_UPDATE
    }

    /**
     * This internal class holds the string that identify the flags for requesting the number of
     * taps in a certain period with [QA.getLastTaps]
     */
    enum class Flag(val timePoints: Int) {
        /** Flag to request taps in the last 24 hours  */
        DAY(24),

        /** Flag to request taps in the last 7 days  */
        WEEK(7),

        /** Flag to request taps in the last 30 days  */
        MONTH(30)
    }

    /**
     * Enumeration class containing the available Genders for the registration of the device.
     * */
    @Keep
    enum class Gender(val id: Int, val code: String) {
        UNKNOWN(0, "U"),
        MALE(1, "M"),
        FEMALE(2, "F"),
        OTHER(3, "O");

        @Keep
        companion object {
            private val map = entries.associateBy(Gender::id)
            private val mapStrings = entries.associateBy(Gender::code)

            @Keep
            fun fromInt(type: Int?) = map[type]

            @Keep
            fun fromString(type: String?) = mapStrings[type]
        }
    }

    /**
     * The first time you use the QA SDK in the code you should initialize it, this allows the SDK
     * to create a unique identifier and initiate server transactions and workflows.
     * Most of the functionality will not work if you have never initialized the singleton before.
     * The function is synchronous and return a flow with the status of the registration of the
     * device to the server. NOTE: do not use this function without collecting the flow otherwise
     * the function will not be called at all.
     * @param context Android application context
     * @param apiKey Authorization code which is provided by QA (api key).
     * @return whether or not is the first time init is called.
     */
    @Throws(QASDKException::class)
    suspend fun init(
        context: Context,
        apiKey: String,
        basicInfo: BasicInfo,
        identityId: String? = null,
        password: String? = null
    ): Boolean {
        try {
            ProviderInstaller.installIfNeeded(context)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
        }

        return qaPrivate.init(context, apiKey, basicInfo, identityId, password)
    }

    /**
     * This function checks if the device is already registered to the QA backend.
     * @return boolean, whether or not the device is registered.
     */
    fun isDeviceRegistered(): Boolean {
        return qaPrivate.isDeviceRegistered()
    }

    /**
     * Use this function to subscribe the device to your(one of your) cohort(s).
     * @param cohortId UUID of the cohort to subscribe tom will be assigned a random subscription ID
     * @param subscriptionId UUID of the subscription, this subscription must already exist in the cohort.
     * @return Object containing the [SubscriptionWithQuestionnaires] object.
     */
    @Throws(QASDKException::class)
    suspend fun subscribe(
        cohortId: String? = null,
        subscriptionId: String? = null
    ): SubscriptionWithQuestionnaires {
        return qaPrivate.signUpForStudy(cohortId, subscriptionId)
    }


    /**
     * Utility function to sync all the local data with the server. Due to the complexity of the work,
     * it spawns a Worker and return its UUID. The status of the worker can be observed to check its
     * status of SUCCESS/FAILURE.
     */
    @Throws(SDKNotInitialisedException::class)
    suspend fun syncData(context: Context) {
        qaPrivate.syncData(context)
    }

    /**
     * Use this to withdraw the device from a particular cohort.
     * @param cohortId cohort subscription Id
     * @return Object containing the status of the response and the message from the API.
     */
    @Throws(QASDKException::class)
    suspend fun leaveCohort(subscriptionId: String, cohortId: String) {
        qaPrivate.leaveStudy(subscriptionId, cohortId)
    }

    /**
     * Use this to re-subscribe the user to a cohort from which they were withdrawn.
     * @param subscriptionId of the subscription to re-enable.
     */
    @Throws(QASDKException::class)
    suspend fun reSubscribe(subscriptionId: String) {
        qaPrivate.reSubscribe(subscriptionId)
    }

    /**
     * @suppress
     * Use this to link the identities of two devices. This should be
     * used only in special cases, the correct way to link devices to the same identity is to use
     * init() with relevant identityId and password.
     * @param idToLink the identityId of the device to link to the current identity.
     * @return boolean indicating the success of the operation.
     */
    suspend fun linkIdentities(idToLink: String): Boolean {
        return qaPrivate.linkIdentities(idToLink)
    }

    /**
     * This function checks if the overlay permission has been granted, if not opens the
     * corresponding settings activity to prompt the user to grant this permission.
     * @param context Android application context
     * @return status of the request
     */
    fun requestOverlayPermission(context: Context): Int {
        if (!canDraw(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (context as Activity).startActivityForResult(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName())
                    ), QAStrings.QA_PERMISSION_REQUEST_OVERLAY
                )
                return QAStrings.QA_PERMISSION_REQUEST_OVERLAY
            }
        }
        return QAStrings.QA_ALREADY_GRANTED
    }

    /**
     * This function checks if the usage permission has been granted, if not open the
     * corresponding settings activity to prompt the user to grant this permission.
     * @param context Android application context
     * @return status of the request
     */
    fun requestUsagePermission(context: Context): Int {
        if (!canUsage(context)) {
            (context as Activity).startActivityForResult(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                QAStrings.QA_PERMISSION_REQUEST_USAGE
            )
        }
        return QAStrings.QA_ALREADY_GRANTED
    }

    /**
     * Returns whether or not the `activity recognition` permission has been granted
     * @param context Android application context
     * @return status of activity Recognition permission
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun canActivity(context: Context): Boolean {
        return qaPrivate.canActivity(context)
    }

    /**
     * Returns whether or not the `draw over other apps` permission has been granted
     * @param context Android application context
     * @return status of Draw Over permission
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun canDraw(context: Context): Boolean {
        return qaPrivate.canDraw(context)
    }

    /**
     * Returns whether or not the `usage` permission has been granted
     * @param context Android application context
     * @return status of usage permission
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun canUsage(context: Context): Boolean {
        return qaPrivate.canUsage(context)
    }

    /**
     * Retrieves the list of studies the device is currently registered for.
     * @return The list of studies the device is subscribed to
     * @see Cohort
     */
    suspend fun getCohortList(): List<Cohort> {
        return qaPrivate.getStudyList()
    }

    suspend fun subscriptions(studyId: String? = null): List<Subscription> {
        return qaPrivate.subscription(studyId)
    }

    @Throws(QASDKException::class)
    suspend fun getConnectedDevices(): List<String> {
        return qaPrivate.getConnectedDevices()
    }
    /**
     * Saves simple text note.
     * @param text simple text
     */
    suspend fun sendNote(text: String) {
        qaPrivate.submitNote(text)
    }

    /**
     * Retrieves taps in the last day, week or month depending on the provided flag.
     * Please also [QATaps] shows the provided result structure.
     * Alpha feature, try setting flag as the number of days you would like to gat back.
     * @param flag from [QA.Flag]
     * @return last taps
     */
    fun getLastTaps(flag: Flag): QATaps {
        return qaPrivate.getLastTaps(flag)
    }

    /**
     * Some manufacturers kill apps that are running for too long, on those devices it is better
     * to ask the user to prevent the OS from killing the app running this SDK. Call this function
     * to retrieve the Intent that redirects the user to the correct setting for disabling battery
     * optimization of the current manufacturer.
     * The functions returns a list, choose the first intent, in an empty list is returned,
     * the current manufacturer does not have specific battery optimization options, thus the
     * request is not needed and the SDk should run safely.
     * @param context Android application context
     */
    fun retrieveBatteryOptimizationIntentForCurrentManufacturer(context: Context): Intent {
        return ManufacturerBatteryOptimization.getAvailableIntents(context)
    }

    /**
     * Saves a questionnaire response.
     * @param name Name of the questionnaire (one gets this from the Questionnaire Entity)
     * @param code Code of the questionnaire (one gets this from the Questionnaire Entity)
     * @param date UNIX Timestamp in milliseconds of the time of completion (e.g. System.currentTimeMillis())
     * @param fullID Full ID id of the questionnaire = cohort_id+questionnaire_code
     * @param response JSON encoded string with the response to the questionnaire, i.e. key-value map
     * where key == question key , value == numeric answer
     */
    suspend fun recordQuestionnaireResponse(
        name: String,
        code: String,
        date: Long,
        fullID: String,
        response: Map<String, Any>
    ) {
       qaPrivate.recordQuestionnaireResponse(name, code, date, fullID, response)
    }

    /**
     * Get a list of all the questionnaires available to complete (across all the studies to which
     * a device is subscribed to).
     * @return a list of [Questionnaire]
     */
    suspend fun getQuestionnairesList(): List<Questionnaire> {
        return qaPrivate.getQuestionnairesList()
    }

    /**
     * Get a QA metric or trend relative to the device in use. Check the the list of available metrics
     * from [Metric] and [Trend]. The function returns an object of type [TimeSeries] which contains timestamps
     * and values of the requested metric. The call is asynchronous ans returns a flow.
     * @param score Object of type [Metric] indicating which metric to retrieve
     * @param from first Timestamp (in ms) to included in the returned metric [TimeSeries]
     * @param to last Timestamp (in ms) to included in the returned metric [TimeSeries]
     * @return A flow wrapping a [TimeSeries]
     */
    @Throws(SDKNotInitialisedException::class)
    fun <P : TimestampedEntity, T> getMetric(
        score: Metric<P, T>,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli(),
        refresh: Boolean = false
    ): Flow<TimeSeries<T>> {
        return qaPrivate.getStat(score, from, to, refresh = refresh)
    }

    /**
     * Get a QA metric relative to a fictitious test device. Check the the list of available metrics
     * from [Metric]. The function returns an object of type [TimeSeries] which contains timestamps
     * and values of the requested metric. The call is asynchronous ans returns a flow. You can use
     * this function to test your data workflow and visualization.
     * @param context Android application context
     * @param score Object of type [Metric] indicating which metric to retrieve
     * @param from first Timestamp (in ms) to included in the returned metric [TimeSeries]
     * @param to last Timestamp (in ms) to included in the returned metric [TimeSeries]
     * @return A flow wrapping a [TimeSeries]
     */
    @Throws(SDKNotInitialisedException::class)
    fun <P : TimestampedEntity, T> getMetricSample(
        context: Context,
        apiKey: String,
        score: Metric<P, T>,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli()
    ):
            Flow<TimeSeries<T>> {
        return qaPrivate.getStatSample(context, apiKey, score, from, to)
    }


    /**
     * Use this utility function to create or edit a journal entry. In case you want to edit a note
     * you will need to pass the ID of the entity to edit. The function returns an asynchronous flow
     * with the response of the action. The response is mostly to trigger UI/UX events, in case of
     * failure the SDK will take care internally of retrying.
     * @return the created journal entry
     */
    @Throws(SDKNotInitialisedException::class)
    suspend fun saveJournalEntry(
        journalEntry: JournalEntry
    ): JournalEntry {
        return qaPrivate.saveJournalEntry(
            journalEntry
        )
    }

    /**
     * This functions returns the full journal of the device, meaning all entries with the
     * corresponding events. Checkout [JournalEntry] for a complete description of how the
     * journal entries are organized.
     * @return A flow with a list of [JournalEntry]
     * @throws [SDKNotInitialisedException] if the SDk was not initialised before.
     */
    @Throws(SDKNotInitialisedException::class)
    fun journalEntries(): Flow<List<JournalEntry>> {
        return qaPrivate.getJournal()
    }

    /**
     * This functions returns a fictitious journal and can be used for test/display purposes,
     * Checkout [JournalEntry] for a complete description of how the journal entries are
     * organized.
     * @param context Android application context
     * @return A flow with a list of [JournalEntry]
     * @throws [SDKNotInitialisedException] if the SDk was not initialised before.
     */
    @Throws(SDKNotInitialisedException::class)
    suspend fun getJournalSample(context: Context, apiKey: String): List<JournalEntry> {
        return qaPrivate.getJournalSample(context, apiKey)
    }

    /**
     * Retrieves the Journal events, meaning the events that one can log together with a journal
     * entry. The events come from a fixed set which may be updated in the future, this function
     * return the latest update to the [JournalEventEntity].
     * @return A list of [JournalEventEntity]
     * @throws [SDKNotInitialisedException] if the SDk was not initialised before.
     */
    suspend fun journalEventKinds(): List<JournalEventEntity> {
        return qaPrivate.journalEventKinds()
    }

    /**
     * Use this function to delete a journal entry. You need to provide the id of the entry you
     * want to delete, checkout [journalEntries] and [JournalEntry] to see how to retrieve
     * the id of the entry to delete.
     */
    @Throws(QASDKException::class)
    suspend fun deleteJournalEntry(id: String) {
        qaPrivate.deleteJournalEntry(id)
    }

    /**
     * Use this function to retrieve a particular journal entry. You need to provide the id of the
     * entry you want to retrieve, checkout [journalEntries] and [JournalEntry] to see how to
     * retrieve the id of the entry.
     * @return an optional [JournalEntry]
     */
    fun getJournalEntry(
        journalEntryId: String
    ): JournalEntry? {
        return qaPrivate.getJournalEntry(journalEntryId)
    }

    /**
     * Use this function to update the basic info of a user. You can call the function with one or
     * parameters, the missing ones will be considered unaltered.
     * @param newYearOfBirth new value for the year of birth
     * @param newGender new value for the gender [QA.Gender]
     * @param newSelfDeclaredHealthy new value for the self declaration of healthiness
     * */
    fun update(
        newYearOfBirth: Int = basicInfo.yearOfBirth,
        newGender: Gender = basicInfo.gender,
        newSelfDeclaredHealthy: Boolean = basicInfo.selfDeclaredHealthy
    ) {
        qaPrivate.updateBasicInfo(newYearOfBirth, newGender, newSelfDeclaredHealthy)
    }

    suspend fun savePVTResult(
        testResult: PVTResponse,
        timestamp: Long = System.currentTimeMillis(),
        localTime: String = Instant.now().toString()
    ) {
        qaPrivate.saveCognitiveTestResult(testResult, timestamp, localTime)
    }

    suspend fun getPVTResults(): List<PVTResponse> {
        return qaPrivate.getPVTResults()
    }

    suspend fun saveDotMemoryTestResult(
        testResult: DotMemoryTestResponse,
        timestamp: Long = System.currentTimeMillis(),
        localTime: String = Instant.now().toString()
    ) {
        qaPrivate.saveDotMemoryTestResult(testResult, timestamp, localTime)
    }

    suspend fun getDotMemoryTestResults(): List<DotMemoryTestResponse> {
        return qaPrivate.getDotMemoryTestResults()
    }

    fun startCognitiveTest(context: Context, cognitiveTest: CognitiveTest) {
        when (cognitiveTest) {
            CognitiveTest.PVT -> {
                val intent = Intent(context, PVTActivity::class.java)
                context.startActivity(intent)
            }

            CognitiveTest.DotMemory -> {
                val intent = Intent(context, DotMemoryTestActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}
/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.cognitive_tests.PVTResponse
import com.quantactions.sdk.data.api.adapters.SubscriptionWithQuestionnaires
import com.quantactions.sdk.data.entity.*
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.repository.*
import com.quantactions.sdk.exceptions.QASDKException
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import com.quantactions.sdk.workers.RegisterWorker
import com.quantactions.sdk.workers.UpdateDeviceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by enea on 12/11/16.
 * Contact: enea.ceolini@quantactions.com
 */
internal class QAPrivate private constructor(
    private var repository: MVPRepository,
    private val preferences: ManagePref2,
    private var firebaseToken: String? = null
) {

    companion object {
        @Volatile
        private var INSTANCE: QAPrivate? = null

        fun getInstance(context: Context): QAPrivate {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    val preferences = ManagePref2.getInstance(context)
                    instance = QAPrivate(
                        MVPRepository.getInstance(context),
                        preferences
                    )
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

    var activityPermissionNotification: ActivityPermissionNotification = ActivityPermissionNotificationImpl()
    val deviceID: String
        get() = repository.deviceID
    val identityId: String
        get() = repository.identityId
    val password: String?
        get() = repository.password
    val basicInfo: BasicInfo
        get() = repository.basicInfo

    fun isDeviceRegistered(): Boolean {
        return repository.isDeviceRegistered()
    }

//    fun isDeviceRegistered(context: Context): Boolean {
//        val keyFile = Utils.optionalKey(context)
//        val isPresent = keyFile != null
//        keyFile?.close()
//        return isPresent
//    }

    private fun registerDeviceAsync() {
        val registerWork = OneTimeWorkRequest.Builder(RegisterWorker::class.java)
            .addTag("registerUser")
            .build()
        val updateDeviceWork = OneTimeWorkRequest.Builder(UpdateDeviceWorker::class.java)
            .addTag("updateDevice")
            .build()
        repository.workManager.beginUniqueWork(
            "registerUser",
            ExistingWorkPolicy.KEEP,
            registerWork
        ).then(updateDeviceWork).enqueue()
    }

    @Throws(QASDKException::class)
    suspend fun init(
        context: Context,
        authCode: String,
        basicInfo: BasicInfo = BasicInfo(),
        identityId: String? = null,
        password: String? = null
    ): Boolean {

        preferences.apiKey = authCode
        // I refresh the repository instance cause before the api was invalid
        repository = MVPRepository.getInstance(context, preferences.apiKey)

        if (preferences.identityId == "") {
            preferences.identityId = identityId ?: UUID.randomUUID().toString()
            preferences.password = password ?: GeneratePassword.randomString()
            // if iamIdentity is not null, the identity already exists so we should just login
            if (identityId != null){
                try {
                    val existingIdentity = repository.getIdentity("qa init")
                    preferences.areCredentialsRegistered = true
                    preferences.isOauthActivated = true
                    preferences.selfDeclaredHealthy = existingIdentity.selfDeclaredHealthy == true
                    preferences.gender = QA.Gender.fromString(existingIdentity.gender) ?: QA.Gender.UNKNOWN
                    preferences.yearOfBirth = existingIdentity.yearOfBirth ?: 0
                } catch (e: Exception){
                    preferences.identityId = ""
                    preferences.password = ""
                    throw QASDKException("Identity not found, please register first")
                }

            } else {
                preferences.selfDeclaredHealthy = basicInfo.selfDeclaredHealthy
                preferences.gender = basicInfo.gender
                preferences.yearOfBirth = basicInfo.yearOfBirth
            }

            preferences.oldToNewDBMigrationDone = true
            preferences.oldToNewAPIMigrationDone = true
            // I only register the user is it is the first launch, if this fails it will be retried upon sync
            registerSpecificationsAndDevice(context)
            checkAndRunService(context)
            return true

        } else {

            // as we transition away from the old DB this should happen only 1 time
            if (preferences.apiKey == "") {
                Timber.e("apiKey was never set, setting it now to $authCode")
                preferences.apiKey = authCode
            }

            // not first time, I check for running
            checkAndRunService(context)
            settingUpSomeWorkers(context)
            return false
        }
    }

    private fun checkAndRunService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (repository.canDraw(context)) {
                makeServiceForeground(context)
                Timber.i("Service NOT running, launching")
            } else {
                Timber.i("Overlay not granted, try to launch anyway")
                makeServiceForeground(context)
            }
        } else {
            makeServiceForeground(context)
            if (ManagePref2.getInstance(context)
                    .getVerbose() > 0
            ) Timber.i("Service NOT running, launching!")
        }
    }

    private fun settingUpSomeWorkers(context: Context) {
        val alarm = QABroadcastReceiver()
        alarm.setAlarm(context)

        // instantiate the worker
        val constraints = Constraints.Builder()
            .build()

        // Runs every hour
        val syncRequest = PeriodicWorkRequest.Builder(SyncWorker::class.java, 1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.MINUTES)
            .addTag(context.packageName + ":com.quantactions.sdk.SyncWorker")
            .build()
        val relaunchRequest =
            PeriodicWorkRequest.Builder(RelaunchWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(2, TimeUnit.MINUTES)
                .addTag(context.packageName + ":com.quantactions.sdk.RelaunchWorker")
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            context.packageName + ":com.quantactions.sdk.SyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            context.packageName + ":com.quantactions.sdk.RelaunchWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            relaunchRequest
        )
    }

    @Throws(QASDKException::class)
    private suspend fun registerSpecificationsAndDevice(context: Context) {

        repository.checkRegisteredStatus()

        when (val response = repository.registerDeviceSpecifications()) {
            is ApiEmptyResponse -> {
                throw QASDKException("Device registration failed, this should, never happen")
            }

            is ApiErrorResponse -> {
                Timber.e("Register spec and device error ${response.errorMessage}")
                registerDeviceAsync()
                throw QASDKException("Device registration failed, retrying later")
            }

            is ApiSuccessResponse -> {
                if (response.body != null) {
                    Timber.d("Registered device specs -> ${response.body!!.id}")
                    repository.saveDeviceSpecificationsId(response.body!!.id)
                    registerDevice(context, response.body!!.id)
                } else {
                    throw QASDKException("Device registration failed, this should, never happen")                }
            }
        }

        // I could not do this before cause I needed a login, could be called also somewhere else
        repository.cacheJournalEvents()
    }

    @Throws(QASDKException::class)
    private suspend fun registerDevice(context: Context, id: String) {
        when (val response2 = repository.registerUser(context, id)) {
            is ApiEmptyResponse -> {
                throw QASDKException("Device registration failed, this should, never happen")
            }

            is ApiErrorResponse -> {
                Timber.e(response2.errorMessage)
                throw QASDKException("Device registration failed, this should, never happen")
            }

            is ApiSuccessResponse -> {
                preferences.isDeviceRegistered = true
                response2.body?.let {
                    Timber.d("Registered device -> ${it.id}")
                    repository.setDeviceId(it.id)
                }
            }
        }
    }

    suspend fun linkIdentities(idToLink: String): Boolean {
        return when (repository.linkIdentities(idToLink)) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                true
            }
            else -> {
                false
            }
        }
    }

    fun getFirebaseToken(): String? {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w("Fetching FCM registration token failed", task.exception)
                    preferences.getFBCode()
                } else {
                    // Get new FCM registration token
                    firebaseToken = task.result
                    preferences.setFBCode(task.result)
                    Timber.d("TOKEN is :: $firebaseToken")
                }
            }
        } catch (e: Exception) {
            Timber.w("Your app does not integrate Firebase, cannot send crash!")
        }
        return firebaseToken
    }

    @Throws(QASDKException::class)
    suspend fun leaveStudy(participationId: String, studyId: String) {
        return repository.withdraw(participationId, studyId)
    }

    @Throws(QASDKException::class)
    suspend fun reSubscribe(participationId: String) {
        return repository.reSubscribe(participationId)
    }

    @Throws(QASDKException::class)
    suspend fun signUpForStudy(
        studyId: String? = null,
        subscriptionId: String? = null
    ): SubscriptionWithQuestionnaires {
        if (studyId == null && subscriptionId == null) {
            throw QASDKException("You need to provide either a studyId or a subscriptionId")
        }
        return if (subscriptionId != null){
            repository.registerToStudyWithParticipationId(subscriptionId)
        } else {
            repository.registerToStudy(studyId!!)
        }
    }

    suspend fun syncData(context: Context): Boolean {
        return SyncHelper(context).syncAll()
    }

    suspend fun submitNote(text: String) {
        repository.submitNote(text)
    }

    fun canWithdraw(studyId: String): Boolean {
        val study = repository.getStudiesSingle().find { it.cohortId == studyId }
        return study != null && study.canWithdraw == 1
    }

    fun getLastTaps(flag: QA.Flag): QATaps {
        val taps: HashMap<String, Int>
        val speed: HashMap<String, Float>
        val calendar = Calendar.getInstance()
        val justOneDay = Calendar.getInstance()
        val ca: Calendar
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hours = SimpleDateFormat("HH", Locale.getDefault())
        val rollBackDate: String
        val currentHour: String
        when (flag) {
            QA.Flag.DAY -> {
                ca = Calendar.getInstance()
                taps = LinkedHashMap()
                speed = LinkedHashMap()
                currentHour = hours.format(ca.timeInMillis)
                taps[currentHour] = 0
                speed[currentHour] = 0f
                var i = 1
                while (i < flag.timePoints) {
                    ca.add(Calendar.HOUR_OF_DAY, -1)
                    taps[hours.format(ca.timeInMillis)] = 0
                    speed[hours.format(ca.timeInMillis)] = 0f
                    i++
                }
                calendar.add(Calendar.DAY_OF_MONTH, -2)
                justOneDay.add(Calendar.DAY_OF_MONTH, -1)
                rollBackDate = sdf.format(calendar.timeInMillis)

                val latestTaps = repository.getLatestTaps(rollBackDate)

                latestTaps.forEach {
                    taps["%02d".format(it.hour)] = it.taps
                    speed["%02d".format(it.hour)] = it.speed
                }

            }

            QA.Flag.WEEK, QA.Flag.MONTH -> {
                ca = Calendar.getInstance()
                taps = LinkedHashMap()
                speed = LinkedHashMap()
                taps[sdf.format(ca.timeInMillis)] = 0
                speed[sdf.format(ca.timeInMillis)] = 0f
                var i = 1
                while (i < flag.timePoints) {
                    ca.add(Calendar.DAY_OF_YEAR, -1)
                    taps[sdf.format(ca.timeInMillis)] = 0
                    speed[sdf.format(ca.timeInMillis)] = 0f
                    i++
                }
                calendar.add(Calendar.DAY_OF_MONTH, -flag.timePoints)
                rollBackDate = sdf.format(calendar.timeInMillis)

                val latestTaps = repository.getLatestTaps(rollBackDate)

                latestTaps.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.taps } }.forEach {
                    taps[it.key] = it.value
                }
                latestTaps.groupBy { it.date }.mapValues { (_, v) -> v.maxOf { it.speed } }
                    .forEach {
                        speed[it.key] = it.value
                    }
            }
        }
        return QATaps(
            taps.toSortedMap().toList().reversed().map { it.second },
            taps.map { it.value }.sum(),
            speed.toSortedMap().toList().reversed().map { it.second })
    }

    suspend fun getStudyList(): List<Cohort> {
        return repository.getStudies()
    }

    @Throws(QASDKException::class)
    suspend fun recordQuestionnaireResponse(
        name: String,
        code: String,
        date: Long,
        fullID: String,
        response: Map<String, Any>
    ) {
        val resp = QuestionnaireResponseEntity(
                0,
        fullID,
        name,
        code,
        date,
        JSONObject(response as Map<*, *>).toString()
        )
        return repository.sendQuestionnaireResponse(resp)
    }

    suspend fun getQuestionnairesList(): List<Questionnaire> {
        return repository.getQuestionnaires()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Throws(SDKNotInitialisedException::class)
    fun <P : TimestampedEntity, T> getStat(
        metricOrTrend: CanReturnCompiledTimeSeries<P, T>,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli(),
        refresh: Boolean = false
    ): Flow<TimeSeries<T>> {
        return repository.getStat(metricOrTrend, from, to, refresh = refresh)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <P : TimestampedEntity, T> getStatSample(
        context: Context,
        apiKey: String,
        score: Metric<P, T>,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli(),
    ): Flow<TimeSeries<T>> {
        val mockRepository = MockRepository.getInstance(
            context,
            apiKey
        )

        return mockRepository.getStat(
            score,
            from,
            to,
            refresh = true
        )
    }


    @Throws(QASDKException::class)
    suspend fun saveJournalEntry(
        journalEntry: JournalEntry
    ): JournalEntry {
        return repository.saveJournalEntry(journalEntry)
    }


    fun getJournal(): Flow<List<JournalEntry>> {
        return repository.getJournalFromDAO()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getJournalSample(
        context: Context,
        apiKey: String
    ): List<JournalEntry> {
        val fakeRepository = MVPRepository.getInstance(
            context,
            apiKey
        )
        fakeRepository.cacheJournalEvents()
        return fakeRepository.cacheJournal("f87984d2-3606-4225-b355-ceaad2304b6d")
    }

    @Throws(SDKNotInitialisedException::class)
    suspend fun journalEventKinds(): List<JournalEventEntity> {
        return repository.journalEventKinds()
    }

    @Throws(QASDKException::class)
    suspend fun deleteJournalEntry(id: String) {
        repository.deleteJournalEntry(id)
    }

    fun getJournalEntry(
        journalEntryId: String
    ): JournalEntry? {
        return repository.getAndResolveJournalEntry(journalEntryId)
    }

    suspend fun subscription(studyId: String? = null): List<Subscription> {
        return if (isDeviceRegistered()) {
            repository.getParticipations(studyId)
        } else listOf()
    }

    fun stopReadingService(context: Context) {
        val intent = Intent(context, ReadingsService::class.java)
        intent.putExtra("killSignal", true)
        makeServiceForeground(context, intent)
        preferences.isDataCollectionPaused = true
    }

    fun isDataCollectionRunning(context: Context): Boolean {
        return !QABroadcastReceiver.isMyServiceRunning(ReadingsService::class.java, context)
    }

    fun resumeCollectionRunning(context: Context) {
        preferences.isDataCollectionPaused = false
        makeServiceForeground(context)
    }

    fun canActivity(context: Context): Boolean {
        return repository.canActivity(context)
    }

    fun canDraw(context: Context): Boolean {
        return repository.canDraw(context)
    }

    fun canUsage(context: Context): Boolean {
        return repository.canUsage(context)
    }


    fun makeServiceForeground(
        context: Context,
        intent: Intent = Intent(context, ReadingsService::class.java)
    ): Boolean {
        return try {
            if (!preferences.isDataCollectionPaused && isDeviceRegistered()) {
                val result: Int =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        PackageManager.PERMISSION_GRANTED
                    }
                if (result == PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    Timber.tag("RESTARTER").d( "PERMISSION IS NOT ENABLED WE NEED TO ASK")
                    // Send notification to user to enable the permission
                    val notification = activityPermissionNotification.createNotification(
                        context,
                        context.getString(R.string.notification_channel_id_qa)
                    )
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.notify(1, notification)
                }

            }
            true
        } catch (e: Exception) {
            // This means that the foreground service cannot be started from background, either turn
            // off the battery optimization, or send a notification to reopen the app so that the
            // foreground can start again. The only problem is that this is via the SDK and not via
            // the app so it is problematic for customization.
            Firebase.crashlytics.setUserId(deviceID)
            Firebase.crashlytics.setCustomKeys {
                key("location", "QAPrivate")
                key("method", "makeServiceForeground")
                key("canDraw", canDraw(context))
                key("canUsage", canUsage(context))
            }
            Firebase.crashlytics.recordException(e)
            false
        }
    }

    fun updateBasicInfo(
        newYearOfBirth: Int,
        newGender: QA.Gender,
        newSelfDeclaredHealthy: Boolean
    ) {
        repository.updateBasicInfo(newYearOfBirth, newGender, newSelfDeclaredHealthy)
    }

    fun executeOldToNewAPIMigration(scope: CoroutineScope) {
        // If password is not null it means I was born with the new API, so no need to make the migration
        if (!preferences.oldToNewAPIMigrationDone && preferences.password == null) {
            preferences.oldToNewAPIMigrationDone = true
            preferences.password = BuildConfig.QA_SAMPLE_PASSWORD
            // this can be done because the deviceID is the same as the IAM identity
            preferences.identityId = preferences.deviceID

            // this assumes the migration has ben done and the new DB is aware of this device
            // Note: There could be some cases when a device has registered with the old API and
            // then updated the app before the migration was done. This is a rare case since we are
            // going to run the migration daily and we don't have that many devices yet. And since
            // this would apply only to new devices, if we have to tell them to just wait for the
            // new app to drop for them then it's not a lot of data lost.
            preferences.isDeviceRegistered = true
            preferences.areCredentialsRegistered = true
//            preferences.isOauthActivated = true
            // auth needs to be activate so to create the jwk, this mean that the device upon first
            // call to the API will receive a 401 and will proceed to login and.

            // this will allow us to re-sync all apps from the phone without the need to do the
            // manual migration for them

            scope.launch {
                repository.deleteLocalStudies()
                repository.checkRegisteredStatus()
                try {
                    val associatedIdentity = repository.getIdentity("OldToNewAPIMigration")
                    Timber.d("ID $associatedIdentity")
                    associatedIdentity.identityId?.let {
                        preferences.identityId = it
                        preferences.isOauthActivated = false
                        repository.checkRegisteredStatus()
                    }

                    repository.resetAppCodesSyncStatus()
                    repository.resetJournalSyncStatus()
                } catch (e: QASDKException) {
                    Firebase.crashlytics.recordException(e)
                    Timber.e(e.toString())
                    Timber.e( "Device is updating but migration was not yet done for this device")
                }
            }
        }
    }

    fun executeOldToNewDBMigration(
        databaseHelper: DatabaseHelper,
        scope: CoroutineScope
    ) {

        val sqLiteDatabase = databaseHelper.writableDatabase
        var cursor: Cursor

        Timber.tag("BroadCastReceiver")
            .i("preferences.oldToNewDBMigrationDone ${preferences.oldToNewDBMigrationDone}")

        if (!preferences.oldToNewDBMigrationDone) {

            cursor = sqLiteDatabase.query(
                LookUp.TABLE_TAPS, null, null, null, null, null, null
            )
            val hourlyTapsEntities = mutableListOf<HourlyTapsEntity>()
            while (cursor.moveToNext()) {
                hourlyTapsEntities.add(
                    HourlyTapsEntity(
                        0,
                        cursor.getString(cursor.getColumnIndexOrThrow(LookUp.COLUMN_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(LookUp.COLUMN_HOUR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(LookUp.NUMBER_TAPS)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(LookUp.COLUMN_SPEED)),
                    )
                )
            }
            cursor.close()
            Timber.tag("BroadCastReceiver").i("Inserting into taps (${hourlyTapsEntities.size})")
            scope.launch { repository.insertOrUpdateHourlyTapsEntity(hourlyTapsEntities) }
            cursor = sqLiteDatabase.query(
                LookUp.TABLE_APP_CODE, null, null, null, null, null, null
            )
            val appCodes = mutableListOf<CodeOfApp>()
            while (cursor.moveToNext()) {
                appCodes.add(
                    CodeOfApp(
                        cursor.getInt(cursor.getColumnIndexOrThrow("_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow(LookUp.COLUMN_APP_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(LookUp.COLUMN_IS_SYNC)),
                    )
                )
            }
            cursor.close()
            Timber.tag("BroadCastReceiver").i("Moving (${appCodes.size}) apps")
            scope.launch { repository.insertOrUpdateAppCode(appCodes) }

            cursor = sqLiteDatabase.query(
                LookUp.TABLE_RECORDS,
                arrayOf(
                    LookUp.COLUMN_UUI,
                    LookUp.AGE,
                    LookUp.GENDER,
                    LookUp.SELF_DECLARED_HEALTHY,
                    LookUp.COLUMN_AUTH_CODE
                ),
                null,
                null,
                null,
                null,
                null
            )

            if (cursor.moveToNext()) {
                if (cursor.getColumnIndex(LookUp.COLUMN_AUTH_CODE) >= 0) {
                    preferences.apiKey =
                        cursor.getString(cursor.getColumnIndexOrThrow(LookUp.COLUMN_AUTH_CODE))
                }

                if (cursor.getColumnIndex(LookUp.COLUMN_UUI) >= 0) {
                    preferences.deviceID =
                        cursor.getString(cursor.getColumnIndexOrThrow(LookUp.COLUMN_UUI))
                }
                if (cursor.getColumnIndex(LookUp.AGE) >= 0) {
                    preferences.yearOfBirth =
                        cursor.getString(cursor.getColumnIndexOrThrow(LookUp.AGE))
                            .toInt()
                }
                if (cursor.getColumnIndex(LookUp.GENDER) >= 0) {
                    preferences.gender =
                        QA.Gender.fromInt(cursor.getInt(cursor.getColumnIndexOrThrow(LookUp.GENDER)))
                            ?: QA.Gender.UNKNOWN
                }
                if (cursor.getColumnIndex(LookUp.SELF_DECLARED_HEALTHY) >= 0) {
                    preferences.selfDeclaredHealthy =
                        cursor.getInt(cursor.getColumnIndexOrThrow(LookUp.SELF_DECLARED_HEALTHY)) == 1
                }
            }
            cursor.close()
            preferences.oldToNewDBMigrationDone = true
        }
    }

    @Throws(QASDKException::class)
    suspend fun getConnectedDevices(): List<String> {
        return repository.getConnectedDevices()
    }

    suspend fun saveTestResult(tesType: String, testResult: PVTResponse) {
        repository.saveTestResult(tesType, testResult)
    }

    suspend fun getTestResults(): List<PVTResponse> {
        return repository.getTestResults()
    }
}

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
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.messaging.FirebaseMessaging
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.cognitivetests.CognitiveTest
import com.quantactions.sdk.cognitivetests.CognitiveTestResult
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.adapters.SubscriptionWithQuestionnaires
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.entity.QuestionnaireWithCohortName
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.local.MVPRoomDatabase.Companion.getDatabase
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.repository.CognitiveTestRepository
import com.quantactions.sdk.data.repository.DeviceRepository
import com.quantactions.sdk.data.repository.IdentityRepository
import com.quantactions.sdk.data.repository.JournalRepository
import com.quantactions.sdk.data.repository.MetricRepository
import com.quantactions.sdk.data.repository.MockRepository
import com.quantactions.sdk.data.repository.StudyRepository
import com.quantactions.sdk.data.stringify
import com.quantactions.sdk.exceptions.QASDKException
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import com.quantactions.sdk.workers.RegisterWorker
import com.quantactions.sdk.workers.UpdateDeviceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Created by enea on 12/11/16.
 * Contact: enea.ceolini@quantactions.com
 */
internal class QAPrivate private constructor(
    private val identityRepository: IdentityRepository,
    private val studyRepository: StudyRepository,
    private val journalRepository: JournalRepository,
    private val deviceRepository: DeviceRepository,
    private val cognitiveTestRepository: CognitiveTestRepository,
    private val metricRepository: MetricRepository,
    private val preferences: ManagePref2,
    private var firebaseToken: String? = null
) {

    internal val identityRepo = identityRepository
    internal val studyRepo = studyRepository
    internal val journalRepo = journalRepository
    internal val deviceRepo = deviceRepository
    internal val cognitiveRepo = cognitiveTestRepository
    internal val metricRepo = metricRepository

    companion object {
        @Volatile
        private var INSTANCE: QAPrivate? = null

        fun getInstance(context: Context): QAPrivate {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    val preferences = ManagePref2.getInstance(context)
                    val networkManager = NetworkManager.getInstance(context, preferences)
                    val identityRepo = IdentityRepository(preferences, networkManager)
                    val journalRepo = JournalRepository(identityRepo, networkManager, getDatabase(context).mvpDao())
                    val studyRepo = StudyRepository(preferences, identityRepo, networkManager, getDatabase(context).mvpDao(), WorkManager.getInstance(context))
                    val cognitiveRepo = CognitiveTestRepository(identityRepo, networkManager, getDatabase(context).cognitiveTestDao())
                    val deviceRepo = DeviceRepository(context, preferences, identityRepo, networkManager, getDatabase(context).mvpDao())
                    val metricRepo = MetricRepository(identityRepo, networkManager, getDatabase(context).mvpDao())
                    
                    instance = QAPrivate(
                        identityRepo, studyRepo, journalRepo, deviceRepo, cognitiveRepo, metricRepo,
                        preferences,
                    )
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

    var activityPermissionNotification: ActivityPermissionNotification = ActivityPermissionNotificationImpl()
    var restartedRequiredNotification: RestartedRequiredNotification = RestartedRequiredNotificationImpl()
    val deviceID: String
        get() = identityRepository.deviceID
    val identityId: String
        get() = identityRepository.identityId
    val password: String?
        get() = identityRepository.password
    val basicInfo: BasicInfo
        get() = identityRepository.getBasicInfo()

    fun getQuestionnaireResponses() = studyRepository.getQuestionnaireResponses()
    fun getPendingCognitiveTests() = cognitiveTestRepository.getPendingCognitiveTests()
    fun getPendingJournalEntries() = journalRepository.getPendingJournalEntries()
    fun getPendingJournalEntriesToDelete() = journalRepository.getPendingJournalEntriesToDelete()
    fun getTapDataParsedToSync() = deviceRepository.getTapDataParsedToSync()
    fun getDeviceHealthParsedToSync() = deviceRepository.getDeviceHealthParsedToSync()
    fun getActivityToSync() = deviceRepository.getActivityToSync()
    internal val mvpDao = deviceRepository.mvpDao

    suspend fun checkRegisteredStatus(): Boolean = identityRepository.checkRegisteredStatus()

    fun isDeviceRegistered(): Boolean {
        return identityRepository.deviceID != ""
    }

    private fun registerDeviceAsync(context: Context) {
        val registerWork = OneTimeWorkRequest.Builder(RegisterWorker::class.java)
            .addTag("registerUser")
            .build()
        val updateDeviceWork = OneTimeWorkRequest.Builder(UpdateDeviceWorker::class.java)
            .addTag("updateDevice")
            .build()
        WorkManager.getInstance(context).beginUniqueWork(
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
        NetworkManager.getInstance(context).initServices(authCode)

        if (preferences.identityId == "") {
            preferences.identityId = identityId ?: UUID.randomUUID().toString()
            preferences.password = password ?: GeneratePassword.randomString()
            // if iamIdentity is not null, the identity already exists so we should just login
            if (identityId != null){
                try {
                    val existingIdentity = identityRepository.getIdentity("qa init")
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
        if (preferences.canDraw(context)) {
            makeServiceForeground(context)
            Timber.d("Service NOT running, launching")
        } else {
            Timber.d("Overlay not granted, try to launch anyway")
            makeServiceForeground(context)
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

        identityRepository.checkRegisteredStatus()

        when (val response = deviceRepository.registerDeviceSpecifications()) {
            is ApiEmptyResponse -> {
                throw QASDKException("Device registration failed, this should, never happen")
            }

            is ApiErrorResponse -> {
                Timber.e("Register spec and device error ${response.errorMessage}")
                registerDeviceAsync(context)
                throw QASDKException("Device registration failed, retrying later")
            }

            is ApiSuccessResponse -> {
                if (response.body != null) {
                    Timber.d("Registered device specs -> ${response.body!!.id}")
                    deviceRepository.saveDeviceSpecificationsId(response.body!!.id)
                    registerDevice(context, response.body!!.id)
                } else {
                    throw QASDKException("Device registration failed, this should, never happen")                }
            }
        }

        // I could not do this before cause I needed a login, could be called also somewhere else
        journalRepository.cacheJournalEvents()
    }

    @Throws(QASDKException::class)
    private suspend fun registerDevice(context: Context, id: String) {
        when (val response2 = deviceRepository.registerUser(id)) {
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
                    deviceRepository.setDeviceId(it.id)
                }
            }
        }
    }

    suspend fun linkIdentities(idToLink: String): Boolean {
        return when (identityRepository.linkIdentities(idToLink)) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                true
            }
            is ApiErrorResponse -> {
                false
            }
        }
    }

    fun hasFeature(feature: String): Boolean {
        return NetworkManager.getInstance(preferences.context).capabilitiesManager.hasFeature(feature)
    }

    fun getFirebaseToken(): String? {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w("Fetching FCM registration token failed ${task.exception}")
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
        return studyRepository.withdraw(participationId, studyId)
    }

    @Throws(QASDKException::class)
    suspend fun reSubscribe(participationId: String) {
        return studyRepository.reSubscribe(participationId)
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
            studyRepository.registerToStudyWithParticipationId(subscriptionId)
        } else {
            studyRepository.registerToStudy(studyId!!)
        }
    }

    suspend fun syncData(context: Context): Boolean {
        return SyncHelper(context).syncAll()
    }

    suspend fun submitNote(text: String) {
        deviceRepository.submitNote(text)
    }

    fun canWithdraw(studyId: String): Boolean {
        val study = runBlocking(Dispatchers.IO) { studyRepository.getStudiesSingle().find { it.cohortId == studyId } }
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

                val latestTaps = deviceRepository.getLatestTaps(rollBackDate)

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

                val latestTaps = deviceRepository.getLatestTaps(rollBackDate)

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

    /**
     * Get taps in a precise time window
     * @param startTimestamp start of the time window in milliseconds since epoch
     * @param stopTimestamp end of the time window in milliseconds since epoch
     * @return object containing the list of taps and the list of apps
     * @throws SDKNotInitialisedException if the SDK is not initialised
     * @throws QASDKException if there is an error retrieving the data
     */
    fun getTapsAndAppsInTimeWindow(
        startTimestamp: Long,
        stopTimestamp: Long,
    ): QA.TapsAndApps {

        if (!hasFeature("raw")){
            throw QASDKException("You do not have access to `raw` feature. Get in contact with QuantActions to enable it for your project.")
        }

        // Retrieve taps in the time window
        val taps = deviceRepository.getTapsInTimeWindow(startTimestamp, stopTimestamp)

        // Extract all unique app IDs from the retrieved taps
        val ids = mutableSetOf<Int>()
        taps.forEach { session ->
            session.appIds0.literalToIntList().map { ids.add(it) }
        }

        Timber.d("Retrieved ${taps.size} tap sessions with app IDs: $ids")

        // Retrieve app codes for the extracted IDs
        val apps = deviceRepository.getAppCodesByIds(ids.toList())

        return QA.TapsAndApps(taps, apps)
    }

    fun getStudyList(): List<Cohort> {
        return runBlocking(Dispatchers.IO) { studyRepository.getStudies() }
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
        return studyRepository.sendQuestionnaireResponse(resp)
    }

    suspend fun getQuestionnairesList(): List<QuestionnaireWithCohortName> {
        return studyRepository.getQuestionnaires()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Throws(SDKNotInitialisedException::class)
    fun <P : TimestampedEntity, T> getStat(
        metricOrTrend: CanReturnCompiledTimeSeries<P, T>,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli(),
        refresh: Boolean = false
    ): Flow<TimeSeries<T>> {
        // Need to get participation ID. This might be problematic if not yet cached.
        val participationId = runBlocking(Dispatchers.IO) { studyRepository.getStudiesSingle().firstOrNull()?.cohortId ?: "" }
        return metricRepository.getStat(metricOrTrend, participationId, from, to, refresh = refresh)
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
        return journalRepository.saveJournalEntry(journalEntry)
    }


    fun getJournal(): Flow<List<JournalEntry>> {
        return journalRepository.getJournalFromDAO()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getJournalSample(
        context: Context,
        apiKey: String
    ): List<JournalEntry> {
        val mockRepository = MockRepository.getInstance(
            context,
            apiKey
        )
        mockRepository.cacheJournalEvents()
        return mockRepository.cacheJournal()
    }

    @Throws(SDKNotInitialisedException::class)
    suspend fun journalEventKinds(): List<JournalEventEntity> {
        return journalRepository.journalEventKinds()
    }

    @Throws(QASDKException::class)
    suspend fun deleteJournalEntry(id: String) {
        journalRepository.deleteJournalEntry(id)
    }

    fun getJournalEntry(
        journalEntryId: String
    ): JournalEntry? {
        return journalRepository.getAndResolveJournalEntry(journalEntryId)
    }

    suspend fun subscription(studyId: String? = null): List<Subscription> {
        return if (isDeviceRegistered()) {
            val filter = mutableMapOf<String, Any>()
            filter["where"] = mapOf("deleted" to null, "withdraw" to null)
            filter["include"] = listOf(mapOf("relation" to "study", "scope" to mapOf("where" to mapOf("deleted" to null))))
            
            val response = NetworkManager.getInstance(preferences.context).apiService.getParticipations(identityId, filter.stringify())
            if (response is ApiSuccessResponse) {
                val results = response.body!!
                val items = if (studyId != null) results.filter { it.studyId == studyId } else results
                items.map { p ->
                    Subscription(p.id, listOf(), p.studyId!!, p.study?.name ?: "",
                        if (p.ttl != null) ZonedDateTime.parse(p.ttl).toInstant().toEpochMilli() else -1, p.token)
                }
            } else listOf()
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
        return preferences.canActivity(context)
    }

    fun canDraw(context: Context): Boolean {
        return preferences.canDraw(context)
    }

    fun canUsage(context: Context): Boolean {
        return preferences.canUsage(context)
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
            Log.e("QAPrivate", "${e.localizedMessage}")
            val notification = restartedRequiredNotification.createNotification(
                context,
                context.getString(R.string.notification_channel_id_qa)
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(1, notification)
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
        identityRepository.updateBasicInfo(newYearOfBirth, newGender, newSelfDeclaredHealthy)
    }

    suspend fun updateOldPassword(newPassword: String) = identityRepository.updateOldPassword(newPassword)

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
                deviceRepository.deleteLocalStudies()
                identityRepository.checkRegisteredStatus()
                try {
                    val associatedIdentity = identityRepository.getIdentity("OldToNewAPIMigration")
                    Timber.d("ID $associatedIdentity")
                    associatedIdentity.identityId?.let {
                        preferences.identityId = it
                        preferences.isOauthActivated = false
                        identityRepository.checkRegisteredStatus()
                    }

                    deviceRepository.resetAppCodesSyncStatus()
                    journalRepository.resetJournalSyncStatus()
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
            scope.launch { deviceRepository.insertOrUpdateHourlyTapsEntity(hourlyTapsEntities) }
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
            scope.launch { deviceRepository.insertOrUpdateAppCode(appCodes) }

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
        return deviceRepository.getConnectedDevices()
    }

    fun <T>getCognitiveTestResults(testType: CognitiveTest<T>): Flow<List<CognitiveTestResult<T>>> {
        return cognitiveTestRepository.getCognitiveTestResults(testType)
    }

    suspend fun <T>saveCognitiveTestResult(
        testType: CognitiveTest<T>,
        testResult: T,
        timestamp: Long = System.currentTimeMillis(),
        localTime: String = Instant.now().toString()
    ) {
        val gson = com.google.gson.Gson()
        val resultJson = gson.toJson(testResult)
        val entity = com.quantactions.sdk.data.entity.CognitiveTestEntity(
            testType = testType.id,
            results = resultJson,
            timestamp = timestamp,
            localTime = localTime,
            sync = 0
        )
        return cognitiveTestRepository.submitCognitiveTestResponse(testType, entity)
    }
}

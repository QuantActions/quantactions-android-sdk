/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral", "unused")

package com.quantactions.sdk.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_core.Resource
import com.hadiyarajesh.flower_core.flow.dbBoundResourceFlow
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.CanReturnCompiledTimeSeries
import com.quantactions.sdk.GeneratePassword
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.Metric
import com.quantactions.sdk.QA
import com.quantactions.sdk.R
import com.quantactions.sdk.Subscription
import com.quantactions.sdk.TapsStats
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.TokenApi
import com.quantactions.sdk.data.api.TokenAuthenticator
import com.quantactions.sdk.data.api.adapters.SubscriptionWithQuestionnaires
import com.quantactions.sdk.data.api.getBasicAuthHeader
import com.quantactions.sdk.data.api.responses.JournalEntriesResponse
import com.quantactions.sdk.data.api.responses.RegistrationResponse
import com.quantactions.sdk.data.entity.ActivityTransitionEntity
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.entity.JournalEntryEntity
import com.quantactions.sdk.data.entity.JournalEntryJoinsJournalEventEntity
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.entity.Questionnaire
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.model.DevicePatch
import com.quantactions.sdk.data.model.DeviceRegistration
import com.quantactions.sdk.data.model.DeviceSpecifications
import com.quantactions.sdk.data.model.DeviceSpecificationsResponse
import com.quantactions.sdk.data.model.DeviceStats
import com.quantactions.sdk.data.model.DeviceStatsResponse
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.model.JournalEntryEvent
import com.quantactions.sdk.data.model.JournalEntryBody
import com.quantactions.sdk.data.model.JournalEventBody
import com.quantactions.sdk.data.model.JournalEventEnterResponse
import com.quantactions.sdk.data.model.Note
import com.quantactions.sdk.data.model.QuestionnaireResponse
import com.quantactions.sdk.data.repository.MVPRoomDatabase.Companion.getDatabase
import com.quantactions.sdk.data.stringify
import com.quantactions.sdk.exceptions.QASDKException
import com.quantactions.sdk.workers.SignUpForStudyWorker
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import timber.log.Timber
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.math.roundToInt


class MVPRepository @Inject private constructor(
    context: Context,
    private val preferences: ManagePref2,
    apiKey: String? = null
) {

    private var latch = CountDownLatch(1)

    companion object {
        @Volatile
        private var INSTANCE: MVPRepository? = null

        fun getInstance(context: Context, apiKey: String? = null): MVPRepository {
            val preferences = ManagePref2.getInstance(context)
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = MVPRepository(
                        context,
                        preferences,
                        apiKey ?: preferences.apiKey
                    )
                    INSTANCE = instance
                }
                instance.reInit(apiKey ?: preferences.apiKey)
                return instance
            }
        }
    }


    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val packageName: String = context.packageName

    val deviceID: String
        get() = preferences.deviceID

    val identityId: String
        get() = preferences.identityId

    val password: String?
        get() = preferences.password

    val basicInfo: BasicInfo
        get() = getABasicInfo()

    fun isDeviceRegistered(): Boolean {
        return deviceID != ""
    }

//    private var participationId: String = "enea-test-id"

    private fun getABasicInfo(): BasicInfo {
        return BasicInfo(
            preferences.yearOfBirth,
            preferences.gender,
            preferences.selfDeclaredHealthy
        )
    }

    private val isTablet = context.resources.getBoolean(R.bool.isTablet)

    private val firebaseToken: String
        get() = preferences.getFBCode()
    private lateinit var apiService: ApiService//.create(preferences, apiKey ?: preferences.apiKey)
    internal val mvpDao = getDatabase(context).mvpDao()
    val workManager = WorkManager.getInstance(context)
    private var canActivity = preferences.canActivity(context)
    private var canDraw = preferences.canDraw(context)
    private var canUsage = preferences.canUsage(context)

    private var iamParticipationId: String = "" // ""138e8ff6b05d6b3c48339e2fd40f2fa8854328eb"
    private lateinit var tokenApi: TokenApi

    private val Boolean.intValue
        get() = if (this) 1 else 0

    suspend fun checkRegisteredStatus(): Boolean {

        if (preferences.areCredentialsRegistered && preferences.isOauthActivated) {
            Timber.i("Identity is registered -> I continue")
            return true
        }

        if (preferences.identityId == "") return false

        if (preferences.areCredentialsRegistered && !preferences.isOauthActivated) {
            return when (val response = tokenApi.enableOauth(getBasicAuthHeader(preferences))) {
                is ApiEmptyResponse, is ApiSuccessResponse -> {
                    preferences.isOauthActivated = true
                    // then I login for this time and let the 401 happen another time
                    when (val response2 = tokenApi.login(getBasicAuthHeader(preferences))) {
                        is ApiEmptyResponse, is ApiSuccessResponse -> {
                            true
                        }

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
                // chain oath activation
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

    init {
        reInit(apiKey ?: preferences.apiKey)
    }

    private var wereJournalEventsCached = false
    private var wasPartIdRequested = false

    fun reInit(apiKey: String) {

        val cookieJar = ApiService.UvCookieJar(preferences, "TokenApi")
        tokenApi = TokenApi.buildTokenApi(apiKey, cookieJar)
        val tokenAuthenticator = TokenAuthenticator(tokenApi, preferences)
        apiService = ApiService.create(
            apiKey,
            tokenAuthenticator,
            cookieJar
        )

        if (preferences.isOauthActivated) {
            if (iamParticipationId == "" && !wasPartIdRequested) {
                wasPartIdRequested = true
                getParticipationsId()
            } else {
                latch.countDown()
            }

            cacheJournalEvents()

        } else {
            latch.countDown()
        }

    }

    private fun updatePWD() {
        scope.launch {
            val newPassword = GeneratePassword.randomString()
            updateOldPassword(newPassword)
        }
    }

    private fun getParticipationsId() {
        synchronized(iamParticipationId) {
            scope.launch {
                if (!checkRegisteredStatus()) return@launch
                getParticipations()
            }
        }
    }

    fun canActivity(context: Context): Boolean {
        canActivity = preferences.canActivity(context)
        return canActivity
    }

    fun canDraw(context: Context): Boolean {
        canDraw = preferences.canDraw(context)
        return canDraw
    }

    suspend fun linkIdentities(idToLink: String): ApiResponse<TokenApi.Identity> {
        return apiService.linkIdentities(
            identityId,
            ApiService.AuthLinkBody(idToLink),
        )
    }

    fun canUsage(context: Context): Boolean {
        canUsage = preferences.canUsage(context)
        return canUsage
    }

    @ExperimentalCoroutinesApi
    fun <P : TimestampedEntity, T> getStat(
        metricOrTrend: CanReturnCompiledTimeSeries<P, T>,
        from: Long = Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli(),
        to: Long = Instant.now().toEpochMilli(),
        sampleParticipationId: String? = null,
        refresh: Boolean = false
    ): Flow<TimeSeries<T>> {

        if (sampleParticipationId == null) {
            Timber.w("I am waiting for the partId with latch")
            latch.await()
        }

        Timber.w("Latch has been released: $iamParticipationId")

        val resources = dbBoundResourceFlow(
            fetchFromLocal = {
                metricOrTrend.getMetric(mvpDao)
            },
            shouldMakeNetworkRequest = {
                (refresh && iamParticipationId != "") || ((iamParticipationId != "" || sampleParticipationId != null) && (
                        it.isNullOrEmpty() || it[0].timestamp < (Instant.now()
                            .toEpochMilli() / 1000 - 3 * 3600)))

            },
            makeNetworkRequest = {
                // I would say we always fetch the last 2 months unless forced
                val thisFrom = if (refresh) "1970-01" else Instant.now().minus(60, ChronoUnit.DAYS)
                    .atZone(ZoneId.systemDefault()).format(
                        DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                    )
                val thisTo = Instant.ofEpochMilli(to).atZone(ZoneId.systemDefault()).format(
                    DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
                )

                if (sampleParticipationId != null){
                    runBlocking { tokenApi.login(getBasicAuthHeader(BuildConfig.QA_SAMPLE_ID, BuildConfig.QA_SAMPLE_PASSWORD))  }
                }

                metricOrTrend.getStat(
                    apiService,
                    if (sampleParticipationId != null) BuildConfig.QA_SAMPLE_ID else identityId,
                    sampleParticipationId ?: iamParticipationId,
                    thisFrom,
                    thisTo
                )
            },
            processNetworkResponse = {
            },
            saveResponseData = { statistics ->
                statistics.chunked(500).forEach { chunk ->
                    metricOrTrend.insertOrUpdateMetric(mvpDao, chunk)
                }
            },
            onNetworkRequestFailed = { _, _ -> }
        ).flowOn(Dispatchers.IO)

        return flow {
            resources.collect {
                when (val status = it.status) {
                    is Resource.Status.Loading, is Resource.Status.EmptySuccess -> {}

                    is Resource.Status.Success -> {
                        emit(metricOrTrend.prepareReturnData(status.data, from / 1000, to / 1000))
                    }

                    is Resource.Status.Error -> {
                        if (status.data != null) {
                            emit(
                                metricOrTrend.prepareReturnData(
                                    status.data!!,
                                    from / 1000,
                                    to / 1000
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun getJournalFromDAO(): Flow<List<JournalEntry>> {
        return mvpDao.getJournal().map { journalFromDao ->
            val journal = mutableMapOf<String, JournalEntry>()

            journalFromDao.forEach { entry ->
                journal.putIfAbsent(
                    entry.standaloneEntryId,
                    JournalEntry(
                        entry.standaloneEntryId,
                        Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalDate(),
                        entry.note
                    )
                )
                entry.journalEventId?.let {
                    journal[entry.standaloneEntryId]?.events?.add(
                        JournalEntryEvent(
                            entry.standaloneEntryId,
                            entry.journalEventId,
                            entry.publicName!!,
                            entry.iconName!!,
                            entry.rating
                        )
                    )
                }

                entry.sleepScore?.let { it1 ->
                    journal[entry.standaloneEntryId]?.scores?.set(
                        Metric.SLEEP_SCORE.code,
                        it1.roundToInt()
                    )
                }
                entry.cogScore?.let { it1 ->
                    journal[entry.standaloneEntryId]?.scores?.set(
                        Metric.COGNITIVE_FITNESS.code,
                        it1.roundToInt()
                    )
                }
                entry.socScore?.let { it1 ->
                    journal[entry.standaloneEntryId]?.scores?.set(
                        Metric.SOCIAL_ENGAGEMENT.code,
                        it1.roundToInt()
                    )
                }
            }
            journal.values.toList()
        }


    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun cacheJournalEvents() {
        scope.launch {

            val filter = mutableMapOf<String, Any>()
            filter["limit"] = "20"

            when (val jEvents = apiService.getJournalEventTypes(identityId, filter.stringify())) {
                is ApiSuccessResponse -> {
                    wereJournalEventsCached = true
                    Timber.d("[QA API CALL]:[SUC]:Journal events were successfully fetched from API")
                    mvpDao.insertOrUpdateEvents(jEvents.body!!)
                }

                else -> Timber.e("[QA API CALL]:[FAIL]:JOURNAL API something wrong $jEvents")
            }
            cacheJournal()
        }
    }


    fun getAndResolveJournalEntry(journalEntryId: String): JournalEntry? {
        val resolvedEvents = mvpDao.getResolvedEventsFromEntry(journalEntryId)
        var journalEntry: JournalEntry? = null
        resolvedEvents.forEachIndexed { index, entry ->
            if (index == 0) journalEntry =
                JournalEntry(
                    entry.standaloneEntryId,
                    Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                    entry.note
                )

            entry.journalEventId?.let {
                val referenceEvent = mvpDao.getEvent(entry.journalEventId)
                if (referenceEvent != null)
                    journalEntry?.events?.add(
                        JournalEntryEvent(
                            entry.journalEventId,
                            referenceEvent.id,
                            referenceEvent.name,
                            referenceEvent.icon,
                            entry.rating
                        )
                    )
            }
        }
        return journalEntry
    }


    @ExperimentalCoroutinesApi
    suspend fun cacheJournal(fakeDeviceID: String? = null): List<JournalEntry> {

        return if (isDeviceRegistered()) {

            val filter = mutableMapOf<String, Any>()

            filter["limit"] = 100
            filter["include"] = listOf(
                mutableMapOf<String, Any>().apply {
                    put("relation", "journalEvents")
                    put("scope", mutableMapOf<String, Any>().apply {
                        put("where", mutableMapOf<String, Any?>().apply {
                            put("deleted", null)
                        })
                    })
                }
            )


            when (val response = apiService.getJournalEntries(identityId, filter.stringify())) {
                is ApiSuccessResponse -> {
                    val journal = mutableListOf<JournalEntry>()
                    val entriesToCommit = mutableListOf<JournalEntryEntity>()
                    val joinsToCommit = mutableListOf<JournalEntryJoinsJournalEventEntity>()



                    response.body!!.forEach { entry ->
                        val entryToAdd = JournalEntry(
                            entry.id,
                            LocalDate.from(
                                DateTimeFormatter.ISO_DATE_TIME.parse(entry.created)
                            ),
                            entry.description
                        )

                        val currentSyncStatus = mvpDao.getEntry(entry.id)?.sync
                        val currentDeleteStatus = mvpDao.getEntry(entry.id)?.deleted

                        entry.journalEvents?.forEach { event ->
                            val referenceEvent = mvpDao.getEvent(event.journalEventTypeId)

                            joinsToCommit.add(
                                JournalEntryJoinsJournalEventEntity(
                                    event.id,
                                    event.journalId,
                                    event.journalEventTypeId,
                                    event.rating ?: -1,
                                )
                            )
                            if (referenceEvent != null)
                                entryToAdd.events.add(
                                    JournalEntryEvent(
                                        event.id,
                                        referenceEvent.id,
                                        referenceEvent.name,
                                        referenceEvent.icon,
                                        event.rating
                                    )
                                )
                        }

                        entriesToCommit.add(
                            JournalEntryEntity(
                                entry.id,
                                entry.description,
                                entry.identityId,
                                LocalDateTime.from(
                                    DateTimeFormatter.ISO_DATE_TIME.parse(entry.created)
                                ).toInstant(ZoneOffset.UTC).toEpochMilli().toString(),
                                LocalDateTime.from(
                                    DateTimeFormatter.ISO_DATE_TIME.parse(entry.created)
                                ).toInstant(ZoneOffset.UTC).toEpochMilli().toString(),
                                // if from remote I keep the sync status if exists
                                currentSyncStatus ?: 1,
                                // if from remote I keep the delete status if exists
                                currentDeleteStatus ?: 0,
                                ""
                            )
                        )
                        journal.add(entryToAdd)
                    }

                    if (fakeDeviceID == null) {
                        // if it's a sample I don't cache it
                        mvpDao.insertOrUpdateEntries(entriesToCommit)
                        mvpDao.insertOrUpdateJournalEntryJoinsJournalEventEntity(joinsToCommit)
                    }


                    return journal
                }

                else -> listOf()
            }
        } else listOf()
    }

    private fun getJournalEventsAssociatedWithEntry(entryId: String): Flow<Resource<List<JournalEntryJoinsJournalEventEntity>>> {
        return dbBoundResourceFlow(
            fetchFromLocal = {
                mvpDao.getJournalEventsFromJournalEntry(entryId)
            },
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {

                val filter = mutableMapOf(
                    "where" to mutableMapOf(
                        "coreJournalId" to entryId
                    )
                )

                apiService.getJournalEvents(
                    filter.stringify()
                )
            },
            processNetworkResponse = {},
            saveResponseData = {
                mvpDao.insertOrUpdateJournalEntryJoinsJournalEventEntity(it.map { e ->
                    JournalEntryJoinsJournalEventEntity(
                        e.id,
                        e.coreJournalId,
                        e.coreJournalEventTypeId,
                        e.rating
                    )
                })
            },
            onNetworkRequestFailed = { _, _ -> }
        ).flowOn(Dispatchers.IO)
    }

    suspend fun journalEventKinds(): List<JournalEventEntity> {
        return mvpDao.getEvents()
    }

    suspend fun saveJournalEntry(journalEntry: JournalEntry): JournalEntry {

        val newJournalEntryId = UUID.randomUUID().toString()
        // I also try to sync
        val journalEntryBody =
            JournalEntryBody(
                newJournalEntryId, // I am submitting so I pass the ID
                journalEntry.note,
                journalEntry.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_DATE_TIME),
            )

        val journalEntryEntity = JournalEntryEntity(
            newJournalEntryId,
            journalEntry.note,
            identityId,
            journalEntry.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                .toString(),
            journalEntry.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                .toString(),
            0,
            0,
            ""
        )

        mvpDao.insertOrUpdateEntry(journalEntryEntity)

        // submit the entry to get the ID
        val journalEntryToReturn = when (val responseSubmitJournalEntry =
            apiService.journalEntrySubmit(identityId, journalEntryBody)) {
            is ApiSuccessResponse -> {
                // Here I need to push the single stuff
                Timber.d("API success response: Journal Entry was successfully posted")
                mvpDao.updateJournalEntry(newJournalEntryId, 1)

                journalEntry.copy(id = newJournalEntryId)
            }

            is ApiErrorResponse -> {
                Timber.e("API ERROR response: ${responseSubmitJournalEntry.errorMessage}")
                throw QASDKException("API ERROR response: ${responseSubmitJournalEntry.errorMessage}")
            }

            else -> {
                throw QASDKException()
            }
        }

        // submit the events
        val journalEventsBody = journalEntry.events.map {
            JournalEventBody(
                it.eventKindID,
                if (it.rating == -1) null else it.rating,
            )
        }

        val journalEventsEntity = journalEntry.events.map { event ->
            JournalEntryJoinsJournalEventEntity(
                UUID.randomUUID().toString(),
                journalEntryEntity.id,
                event.eventKindID,
                event.rating ?: -1
            )
        }

        mvpDao.insertOrUpdateJournalEntryJoinsJournalEventEntity(journalEventsEntity)

        submitJournalEvents(journalEntryToReturn.id!!, journalEventsBody)

        if (journalEntry.id != null) { // this was an edit request
            deleteJournalEntry(journalEntry.id)
        }

        return journalEntryToReturn
    }

    suspend fun journalEntrySubmit(journalEntryBody: JournalEntryBody): ApiResponse<JournalEntriesResponse> {
        return apiService.journalEntrySubmit(identityId, journalEntryBody)
    }

    suspend fun journalEventsSubmit(
        journalEntryId: String,
        journalEventsToPost: List<JournalEventBody>
    ): ApiResponse<List<JournalEventEnterResponse>> {
        return apiService.journalEventsSubmit(identityId, journalEntryId, journalEventsToPost)
    }

    private suspend fun deleteJournalEvents(journalId: String, idsToDelete: List<String>) {
        idsToDelete.map { deleteJournalEvent(journalId, it) }
    }

    private suspend fun deleteJournalEvent(
        journalId: String,
        idToDelete: String
    ) {
        when (val response = apiService.journalEventDelete(identityId, journalId, idToDelete)
        ) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                // Here I need to push the single stuff
                Timber.d("API success response: Journal event was successfully DELETED")
                // no need to update the sync status as that is dependent on the entry

            }

            is ApiErrorResponse -> {
                Timber.e("API ERROR response: ${response.errorMessage}")
                throw QASDKException("API ERROR response: ${response.errorMessage}")
            }
        }
    }

    @kotlin.jvm.Throws(QASDKException::class)
    private suspend fun submitJournalEvents(
        coreJournalId: String,
        journalEventsToPost: List<JournalEventBody>
    ) {
        when (val response2 = apiService.journalEventsSubmit(
            identityId,
            coreJournalId,
            journalEventsToPost
        )
        ) {
            is ApiSuccessResponse -> {
                // Here I need to push the single stuff
                Timber.d("API success response: Journal events was successfully posted")
                val updatedEvents = response2.body!!
                updatedEvents.forEach { updatedEvent ->
                    mvpDao.updateJournalEntryJoinsJournalEvent(
                        updatedEvent.id,
                        coreJournalId,
                        updatedEvent.journalEventTypeId
                    )
                }

                // no need to update the sync status as that is dependent on the entry
            }

            is ApiErrorResponse -> {
                Timber.e("API ERROR response: ${response2.errorMessage}")
                throw QASDKException("API ERROR response: ${response2.errorMessage}")
            }

            else -> {
                throw QASDKException("Response Body to journal entry is empty, this should not happen, file a bug report!")
            }
        }
    }

    suspend fun simplyDeleteJournalEntry(journalId: String): ApiResponse<String> {
        return apiService.journalEntryDelete(identityId, journalId)
    }

    @Throws(QASDKException::class)
    suspend fun deleteJournalEntry(id: String) {
        mvpDao.deleteJournalEntry(id)

        val eventsToDelete = mvpDao.getJournalEventsFromJournalEntryAsync(id)

        deleteJournalEvents(id, eventsToDelete.map { it.id })

        when (val response = apiService.journalEntryDelete(identityId, id)) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                mvpDao.updateJournalEntry(id, 1)
            }

            is ApiErrorResponse -> {
                throw QASDKException("API Error [deleteJournalEntry()]: ${response.errorMessage} || ${response.httpStatusCode}")
            }
        }
    }

    suspend fun registerUser(coreDeviceSpecificationId: String): ApiResponse<RegistrationResponse> {
        val deviceRegistration = DeviceRegistration(
            deviceSpecificationId = coreDeviceSpecificationId,
            enableAppIdAccess = canUsage,
            enableDrawOverAccess = canDraw,
            firebaseTokenId = firebaseToken,
            packageUsingSdk = packageName
        )
        return apiService.registerDevice(identityId, deviceRegistration)
    }

    suspend fun registerDeviceSpecifications(): ApiResponse<DeviceSpecificationsResponse> {
        return apiService.registerDeviceSpecifications(identityId, DeviceSpecifications())
    }

    @Throws(QASDKException::class)
    suspend fun registerToStudy(
        studyId: String
    ): SubscriptionWithQuestionnaires {

        val apiResponse = apiService.registerToStudy(
            identityId,
            ApiService.SubscribeWithStudyIdBody(
                studyId = studyId
            )
        )

        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let {
                    Timber.d("SUCCESS while signup $it")
                    // here I should retrieve the info about the study
                    val cohort =
                        getStudyInfo(it.studyId!!) ?: throw QASDKException("Cohort is null")

                    val questionnaires = getQuestionnaires(it.studyId!!)
                    mvpDao.insertOrUpdateStudy(cohort)
                    return SubscriptionWithQuestionnaires(
                        cohort,
                        questionnaires, it.id, listOf(),
                        if (it.ttl != null) ZonedDateTime.parse(it.ttl).toInstant()
                            .toEpochMilli() else -1
                    )

                }
                throw QASDKException("Response Body to subscription is empty, this should not happen, file a bug report!")
            }

            is ApiErrorResponse -> {
                // Might be because the device is not yet registered, in this case we
                // Should be retried by app
                // launch a background task to be retried later on.

                Timber.e("ERROR while signup ${apiResponse.errorMessage}")
                if (apiResponse.httpStatusCode in listOf(424, 404)) {
                    // simply pasted the wrong one
                    throw QASDKException("ParticipationID is invalid")
                } else { // Network problem
                    Timber.e("ERROR ${apiResponse.errorMessage} : while signup Launching job (2 minutes)")
                    val data = Data.Builder()
                    data.putString(
                        SignUpForStudyWorker.SubscriptionConstants.COHORT_ID.name,
                        studyId
                    )
                    val signUpWork =
                        OneTimeWorkRequest.Builder(SignUpForStudyWorker::class.java)
                            .addTag("signupForStudy")
                            .setInitialDelay(2, TimeUnit.MINUTES)
                            .setInputData(data.build())
                            .build()
                    workManager.enqueueUniqueWork(
                        "signupForStudy",
                        ExistingWorkPolicy.KEEP,
                        signUpWork
                    )
                    throw QASDKException("Network issues, we will retry later.")
                }
            }

            is ApiEmptyResponse -> {
                throw QASDKException()
            }
        }
    }


    suspend fun registerToStudyWithParticipationId(
        participationId: String
    ): SubscriptionWithQuestionnaires {

        val apiResponse = apiService.registerToStudyWithParticipationId(
            identityId,
            participationId,
            ApiService.SubscribeWithParticipationIdBody(
                identityId = identityId
            )
        )

        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let {
                    Timber.d("SUCCESS while signup $it")
                    // re-login for good measure
                    tokenApi.login(getBasicAuthHeader(preferences))
                    // here I should retrieve the info about the study
                    val cohort = getStudyInfoWithParticipationId(participationId)
                        ?: throw QASDKException("Cohort is null")
                    val questionnaires = getQuestionnaires(cohort.cohortId)
                    mvpDao.insertOrUpdateStudy(cohort)
                    return SubscriptionWithQuestionnaires(
                        cohort,
                        questionnaires, it.id, listOf(),
                        if (it.ttl != null) ZonedDateTime.parse(it.ttl).toInstant()
                            .toEpochMilli() else -1
                    )
                }
                throw QASDKException("Body is null")
            }

            is ApiErrorResponse -> {
                // Might be because the device is not yet registered, in this case we
                // launch a background task to be retried later on.

                Timber.e("ERROR while signup ${apiResponse.errorMessage}")
                if (apiResponse.httpStatusCode in listOf(424, 404)) {
                    // simply pasted the wrong one
                    throw QASDKException("ParticipationID is invalid")
                } else { // Network problem
                    Timber.e("ERROR while signup Launching job (2 minutes)")
                    val data = Data.Builder()
                    data.putString(
                        SignUpForStudyWorker.SubscriptionConstants.SUBSCRIPTION_ID.name,
                        participationId
                    )
                    val signUpWork =
                        OneTimeWorkRequest.Builder(SignUpForStudyWorker::class.java)
                            .addTag("signupForStudy")
                            .setInitialDelay(2, TimeUnit.MINUTES)
                            .setInputData(data.build())
                            .build()
                    workManager.enqueueUniqueWork(
                        "signupForStudy",
                        ExistingWorkPolicy.KEEP,
                        signUpWork
                    )
                    throw QASDKException("Network issues! Retry again later")
                }
            }

            is ApiEmptyResponse -> {
                throw QASDKException()
            }
        }
    }

    private suspend fun getQuestionnaires(studyId: String): List<Questionnaire> {

        Timber.d("Now getting questionnaires")

        val header = mutableMapOf<String, String>()
        header["x-authorization"] = "Bearer ${preferences.accessToken}"
        header["cache-control"] = "no-cache"

        return when (val response = apiService.getQuestionnaires(identityId, studyId)) {
            is ApiSuccessResponse -> {
                Timber.d("GOT questionnaires info")

                val questionnaires = response.body!!
                if (questionnaires.isEmpty()) {
                    return listOf()
                }

                val mappedQuestionnaires = questionnaires.map {
                    Questionnaire(
                        it.id,
                        it.title,
                        it.description,
                        it.code,
                        studyId,
                        it.definition.toMutableMap().stringify()
                    )
                }

                mvpDao.insertOrUpdateQuestionnaire(mappedQuestionnaires)
                return mappedQuestionnaires
            }

            is ApiEmptyResponse -> listOf()
            is ApiErrorResponse -> {
                Timber.e("Error while getting questionnaires: ${response.errorMessage}")
                listOf()
            }
        }
    }

    private suspend fun getStudyInfo(studyId: String): Cohort? {

        Timber.d("Now getting study info")
        val filter = mutableMapOf<String, Any>()
        filter["where"] = mutableMapOf<String, Any>().apply {
            put("studyId", studyId)
//            put("identityId", identityId)
        }
        filter["include"] = listOf(
            mutableMapOf<String, Any>().apply {
                put("relation", "study")
                put("scope", mutableMapOf<String, Any>().apply {
                    put("where", mutableMapOf<String, Any?>().apply {
                        put("deleted", null)
                    })
                })
            }
        )

        val header = mutableMapOf<String, String>()
        header["x-authorization"] = "Bearer ${preferences.accessToken}"
        header["cache-control"] = "no-cache"

        return when (val response = apiService.getParticipations(
            identityId,
            filter.stringify()
        )) {
            is ApiSuccessResponse -> {
                Timber.d("GOT study info 2 ${response.body}")
                val studyInfo = response.body!!.first { it.studyId == studyId }.study!!
                // here I should retrieve the info about the study
                val cohort = Cohort(
                    studyInfo.id,
                    studyInfo.privacyPolicy,
                    studyInfo.name,
                    null,
                    0,
                    if (studyInfo.enableWithdraw) 1 else 0,
                    if (studyInfo.enableSyncOnScreenOff) 1 else 0,
                    if (studyInfo.enableAppIdAccess) 1 else 0,
                    if (studyInfo.enableSyncOnScreenOff) 1 else 0,
                    if (studyInfo.enableDrawOverAccess) 1 else 0,
                    0,
                    0
                )
                mvpDao.insertOrUpdateStudy(cohort)

                return cohort
            }

            is ApiEmptyResponse -> null
            is ApiErrorResponse -> {
                Timber.e("Error while getting study info: ${response.errorMessage}")
                null
            }
        }
    }

    private suspend fun getStudyInfoWithParticipationId(participationId: String): Cohort? {

        Timber.d("Now getting study info")
        val filter = mutableMapOf<String, Any>()
        filter["include"] = listOf(
            mutableMapOf<String, Any>().apply {
                put("relation", "study")
                put("scope", mutableMapOf<String, Any>().apply {
                    put("where", mutableMapOf<String, Any?>().apply {
                        put("deleted", null)
                    })
                })
            }
        )

        return when (val response = apiService.getParticipation(
            identityId,
            participationId,
            filter.stringify()
        )) {
            is ApiSuccessResponse -> {
                Timber.d("GOT study info 1 ${response.body}")
                val studyInfo = response.body!!.study!!
                // here I should retrieve the info about the study
                val cohort = Cohort(
                    studyInfo.id,
                    studyInfo.privacyPolicy,
                    studyInfo.name,
                    null,
                    0,
                    if (studyInfo.enableWithdraw) 1 else 0,
                    if (studyInfo.enableSyncOnScreenOff) 1 else 0,
                    if (studyInfo.enableAppIdAccess) 1 else 0,
                    if (studyInfo.enableSyncOnScreenOff) 1 else 0,
                    if (studyInfo.enableDrawOverAccess) 1 else 0,
                    0,
                    0
                )
                mvpDao.insertOrUpdateStudy(cohort)

                return cohort
            }

            is ApiEmptyResponse -> null
            is ApiErrorResponse -> {
                Timber.e("Error while getting study info: ${response.errorMessage}")
                null
            }
        }
    }


    suspend fun getParticipations(studyId: String? = null): List<Subscription> {

        // re-login for good measure
        tokenApi.login(getBasicAuthHeader(preferences))

        if (deviceID == "") {
            return listOf()
        }

        val filter = mutableMapOf<String, Any>()
        filter["where"] = mutableMapOf<String, Any?>().apply {
            put("deleted", null)
            put("withdraw", null)
        }

        filter["include"] = listOf(
            mutableMapOf<String, Any>().apply {
                put("relation", "study")
                put("scope", mutableMapOf<String, Any>().apply {
                    put("where", mutableMapOf<String, Any?>().apply {
                        put("deleted", null)
                    })
                })
            }
        )

        filter["limit"] = 10

        val header = mutableMapOf<String, String>()
        header["x-authorization"] = "Bearer ${preferences.accessToken}"
        header["cache-control"] = "no-cache"

        // check the validity of the ID..otherwise abort
        val apiResponse = apiService.getParticipations(
            identityId,
            filter.stringify(),
        )
        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let { studyRegistrationResponse ->

                    if (studyRegistrationResponse.isEmpty()) {
                        latch.countDown()
                        return listOf()
                    }

                    if (studyId != null) {
                        val filtered =
                            studyRegistrationResponse.filter { it.studyId == studyId }
                        if (filtered.isEmpty()) {
                            latch.countDown()
                            return listOf()
                        } else {
                            val participation = filtered[0]
                            iamParticipationId = participation.id
                            latch.countDown()

                            // here I cache the study info anyway
                            getStudyInfo(participation.studyId!!)

                            return listOf(
                                Subscription(
                                    participation.id,
                                    listOf(),
                                    participation.studyId!!,
                                    participation.study?.name ?: "",
                                    if (participation.ttl != null) ZonedDateTime.parse(participation.ttl)
                                        .toInstant()
                                        .toEpochMilli() else -1,
                                    participation.token
                                )
                            )
                        }
                    }

                    // here I cache the study info anyway
                    studyRegistrationResponse.forEach { participation ->
                        getStudyInfo(participation.studyId!!)
                        getQuestionnaires(participation.studyId!!)
                    }

                    Timber.i("ParticipationIds: ${studyRegistrationResponse.map { it.id }}")
                    Timber.i("Tokens: ${studyRegistrationResponse.map { it.token }}")
                    iamParticipationId = studyRegistrationResponse[0].id
                    latch.countDown()

                    return studyRegistrationResponse.map { participation ->
                        Subscription(
                            participation.id,
                            listOf(),
                            participation.studyId!!,
                            participation.study?.name ?: "",
                            if (participation.ttl != null) ZonedDateTime.parse(
                                participation.ttl
                            ).toInstant()
                                .toEpochMilli() else -1,
                            participation.token
                        )
                    }

                }
                latch.countDown()
                wasPartIdRequested = true
                return listOf()
            }

            is ApiErrorResponse -> {
                // Might be because the device is not yet registered, in this case we
                // launch a background task to be retried later on.
                latch.countDown()
                wasPartIdRequested = false
                Timber.e("ERROR while getting participations ${apiResponse.errorMessage}")
                return listOf()
            }

            is ApiEmptyResponse -> {
                latch.countDown()
                wasPartIdRequested = false
                return listOf()
            }
        }

    }


    fun getStudiesSingle(): List<Cohort> {
        return mvpDao.getStudiesSingle()
    }

    suspend fun getStudies(): List<Cohort> {
        return mvpDao.getStudies()
    }


    suspend fun getQuestionnaires(): List<Questionnaire> {
        return mvpDao.getQuestionnaires()
    }

    suspend fun reSubscribe(
        participationId: String,
    ) {

        val apiResponse = apiService.withdrawParticipation(
            identityId,
            participationId,
            ApiService.WithdrawBody(withdraw = null)
        )

        when (apiResponse) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                Timber.i("ReSub success")
            }

            is ApiErrorResponse -> {
                Timber.e("ReSub error")
                throw QASDKException("API Error [reSubscribe()]: ${apiResponse.errorMessage} || ${apiResponse.httpStatusCode}")
            }
        }

    }


    suspend fun withdraw(
        participationId: String,
        studyId: String
    ) {

        val created = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)

        val apiResponse = apiService.withdrawParticipation(
            identityId,
            participationId,
            ApiService.WithdrawBody(withdraw = created)
        )

        when (apiResponse) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                mvpDao.deleteStudy(studyId)
                mvpDao.deleteQuestionnaires(studyId)
            }

            is ApiErrorResponse -> {
                Timber.e("Delete error")
                throw QASDKException("API Error [withdraw()]: ${apiResponse.errorMessage} || ${apiResponse.httpStatusCode}")
            }
        }

    }

    // test -> testSubmitStatistic
    suspend fun submitStatistic(
        deviceStats: DeviceStats? = null
    ): ApiResponse<DeviceStatsResponse> {
        return apiService.submitStatistic(
            identityId,
            deviceID,
            deviceStats ?: TapsStats(mvpDao).toDeviceStats()
        )
    }

    // test -> testSendNote
    suspend fun submitNote(
        note: String,
        fakeDeviceID: String? = null
    ) {
        val apiResponse = apiService.submitNote(
            fakeDeviceID ?: identityId,
            Note(note)
        )
        when (apiResponse) {

            is ApiSuccessResponse -> {}

            is ApiErrorResponse -> {
                throw QASDKException("API Error [submitNote()]: ${apiResponse.errorMessage} || ${apiResponse.httpStatusCode}")
            }

            is ApiEmptyResponse -> {
                throw QASDKException("Response Body to submitNote is empty, this should not happen, file a bug report!")
            }
        }
    }

    // test -> testUpdateAppList
    suspend fun updateAppList(
        listOfApps: List<AppToPush>
    ): ApiResponse<List<AppToPush>> {
        return apiService.updateAppList(identityId, deviceID, listOfApps)
    }

    // test -> testSubmitQResponse
    suspend fun sendQuestionnaireResponse(
        answer: QuestionnaireResponseEntity
    ) {

        val daoResponse = mvpDao.insertQuestionnaireResponse(answer)

        val studyId = answer.qFullID.split(":")[0]
        val questionnaireId = answer.qFullID.split(":")[1]

        val toPush = prepareQuestionnaireResponseSubmit(answer)

        val apiResponse = apiService.submitQuestionnaireAnswer(
            identityId, studyId, questionnaireId,
            toPush
        )

        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let {
                    mvpDao.deleteQuestionnaireResponse(daoResponse)
                }
            }

            is ApiErrorResponse -> {
                throw QASDKException("API Error [sendQuestionnaireResponse()]: ${apiResponse.errorMessage} || ${apiResponse.httpStatusCode}")
            }

            is ApiEmptyResponse -> {
                throw QASDKException("Response Body to sendQuestionnaire is empty, this should not happen, file a bug report!")
            }
        }
    }

    private fun prepareQuestionnaireResponseSubmit(answer: QuestionnaireResponseEntity): QuestionnaireResponse {

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(Any::class.java)
        val jsonStructure = adapter.fromJson(answer.qResponse)
        val responseObject = jsonStructure as Map<String, Any>?
        val date = Instant.ofEpochMilli(answer.qDate).atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_DATE_TIME)
        return QuestionnaireResponse(
            date,
            responseObject
        )
    }

    // test -> testSubmitQResponse
    suspend fun sendPendingQuestionnaireResponse(
        answer: QuestionnaireResponseEntity
    ): ApiResponse<QuestionnaireResponse> {

        val studyId = answer.qFullID.split(":")[0]
        val questionnaireId = answer.qFullID.split(":")[1]
        val toPush = prepareQuestionnaireResponseSubmit(answer)

        return apiService.submitQuestionnaireAnswer(identityId, studyId, questionnaireId, toPush)
    }

    suspend fun sendPendingJournalEntry(
        journalEntrySubmitBody: JournalEntryBody,
    ): ApiResponse<JournalEntriesResponse> {

        return apiService.journalEntrySubmit(
            identityId,
            journalEntrySubmitBody
        )
    }

    suspend fun deleteJournalEntryAsync(
        journalId: String
    )
            : ApiResponse<String> {
        return apiService.journalEntryDelete(
            identityId,
            journalId
        )
    }

    suspend fun updateDeviceInfo(): ApiResponse<DeviceRegistration> {

        val deviceInfo = DevicePatch(
            enableAppIdAccess = canUsage,
            enableDrawOverAccess = canDraw,
            firebaseTokenId = firebaseToken,
            packageUsingSdk = packageName
        )
        return apiService.updateDevice(identityId, deviceID, deviceInfo)
    }

    suspend fun updateIdentity(): ApiResponse<TokenApi.Identity> {

        val newIdentity = ApiService.IdentityPatch(
            gender = basicInfo.gender.code,
            yearOfBirth = if (basicInfo.yearOfBirth != 0) basicInfo.yearOfBirth else null,
            selfDeclaredHealthy = basicInfo.selfDeclaredHealthy
        )

        return apiService.patchIdentity(
            identityId,
            newIdentity
        )
    }

    suspend fun submitActivity(
        activityBody: ActivityBody,
    ): ApiResponse<ActivityBody> {
        return apiService.submitActivity(
            identityId,
            deviceID,
            activityBody
        )
    }

    suspend fun submitTapDataParsed(
        tapHealthDataBody: TapDataBody,
    ): ApiResponse<TapDataBody> {
        return apiService.submitTap(
            identityId,
            deviceID,
            tapHealthDataBody
        )
    }

    suspend fun submitHealthDataParsed(
        tapHealthDataBody: HealthDataBody
    ): ApiResponse<HealthDataBody> {
        return apiService.submitHealth(
            identityId,
            deviceID,
            tapHealthDataBody
        )
    }

    suspend fun resetAppCodesSyncStatus() {
        mvpDao.resetAppCodesSyncStatus()
    }

    suspend fun resetJournalSyncStatus() {
        mvpDao.resetJournalSyncStatus()
    }


    private fun permanentlyDeleteJournalEntry(journalEntryId: String) {
        mvpDao.permanentlyDeleteJournalEntry(journalEntryId)
    }

    fun permanentlyDeleteJournalEvents(journalEventIds: List<String>) {
        mvpDao.permanentlyDeleteJournalEvents(journalEventIds)
    }

    fun getPendingAppCodes(): List<CodeOfApp> {
        return mvpDao.getPendingAppCodes()
    }

    fun updateCodeOfAppStatus(appId: Int, syncStatus: Int) {
        mvpDao.updateCodeOfAppStatus(appId, syncStatus)
    }

    fun deleteWrongHealthSessions(idsToDelete: List<String>) {
        mvpDao.deleteWrongHealthSessions(idsToDelete)
    }

    fun deleteWrongTapSessions(idsToDelete: List<String>) {
        mvpDao.deleteWrongTapSessions(idsToDelete)
    }

    fun getPendingJournalEntries(): List<JournalEntryEntity> {
        return mvpDao.getPendingJournalEntries()

    }

    fun getPendingJournalEntriesToDelete(): List<JournalEntryEntity> {
        return mvpDao.getPendingJournalEntriesToDelete()
    }

    fun getJournalEventsOfJournalEntry(journalEntryId: String): List<JournalEntryJoinsJournalEventEntity> {
        return mvpDao.getJournalEventsOfJournalEntry(journalEntryId)
    }

    fun updateJournalEntryOldId(localId: String, oldId: String) {
        mvpDao.updateJournalEntryOldId(localId, oldId)
    }

    fun updateJournalEntry(localId: String, syncStatus: Int) {
        mvpDao.updateJournalEntry(localId, syncStatus)
    }

    fun insertOrUpdateStudy(cohort: Cohort) {
        mvpDao.insertOrUpdateStudy(cohort)
    }

    fun insertOrUpdateQuestionnaire(listQ: List<Questionnaire>) {
        mvpDao.insertOrUpdateQuestionnaire(listQ)
    }

    fun updateDeviceHealthParsedSyncStatus(startsToUpdate: List<Long>) {
        mvpDao.updateDeviceHealthParsedSyncStatus(startsToUpdate)
    }

    fun getDeviceHealthParsedToSync(): List<DeviceHealthParsed> {
        return mvpDao.getDeviceHealthParsedToSync()
    }

    fun getQuestionnaireResponses(): List<QuestionnaireResponseEntity> {
        return mvpDao.getQuestionnaireResponses()
    }

    fun deleteQuestionnaireResponse(id: Long) {
        mvpDao.deleteQuestionnaireResponse(id)
    }

    fun updateTapDataParsedSyncStatus(startsToUpdate: List<Long>) {
        mvpDao.updateTapDataParsedSyncStatus(startsToUpdate)
    }

    fun updateActivitySyncStatus(startsToUpdate: List<Long>) {
        mvpDao.updateActivitySyncStatus(startsToUpdate)
    }

    fun removeInvalidTapSessions(sessionsToRemove: List<Int>) {
        mvpDao.removeInvalidTapSessions(sessionsToRemove)
    }

    fun removeInvalidTapSessionsFromStart(startsToRemove: List<Long>) {
        mvpDao.removeInvalidTapSessionsFromStart(startsToRemove)
    }

    fun getTapDataParsedToSync(): List<TapDataParsed> {
        return mvpDao.getTapDataParsedToSync()
    }

    fun getActivityToSync(): List<ActivityTransitionEntity> {
        return mvpDao.getActivityToSync()
    }

    fun getLatestTaps(rollBackDate: String): List<HourlyTapsEntity> {
        return mvpDao.getLatestTaps(rollBackDate)
    }

    suspend fun submitQuestionnaireAnswer(
        studyId: String,
        questionnaireId: String,
        questionnaireResponse: QuestionnaireResponse,
    ): ApiResponse<QuestionnaireResponse> {
        return apiService.submitQuestionnaireAnswer(
            identityId,
            studyId,
            questionnaireId,
            questionnaireResponse
        )
    }

    suspend fun updateOldPassword(newPassword: String): ApiResponse<TokenApi.Identity> {
        return apiService.patchCredentials(
            identityId,
            ApiService.CredentialsRegistration(
                identityId = identityId,
                password = newPassword,
            )
        )
    }

    fun updateBasicInfo(
        newYearOfBirth: Int,
        newGender: QA.Gender,
        newSelfDeclaredHealthy: Boolean
    ) {
        preferences.yearOfBirth = newYearOfBirth
        preferences.gender = newGender
        preferences.selfDeclaredHealthy = newSelfDeclaredHealthy
    }

    suspend fun insertOrUpdateHourlyTapsEntity(hourlyTapsEntities: MutableList<HourlyTapsEntity>) {
        mvpDao.insertOrUpdateHourlyTapsEntity(hourlyTapsEntities)
    }

    suspend fun insertOrUpdateAppCode(appCodes: MutableList<CodeOfApp>) {
        mvpDao.insertOrUpdateAppCode(appCodes)
    }

    fun setDeviceId(deviceId: String) {
        preferences.deviceID = deviceId
    }

    fun saveDeviceSpecificationsId(id: String) {
        preferences.deviceSpecificationsId = id
    }

    suspend fun getConnectedDevices(): List<String> {

        if (!checkRegisteredStatus()) {
            throw QASDKException("Device is not registered")
        }


        val filter = mutableMapOf<String, Any>()
        filter["where"] = mutableMapOf<String, Any>().apply {
            put("identityId", identityId)
        }
        filter["limit"] = "20"

        val response = apiService.getConnectedDevices(
            identityId
        )

        return when (response) {
            is ApiSuccessResponse -> {
                // Here I need to push the single stuff
                Timber.d("API success response: Connected devices were successfully retrieved")
                response.body!!.map { it.id }
            }

            is ApiErrorResponse -> {
                Timber.e("API ERROR response: ${response.errorMessage}")
                throw QASDKException("API Error [getConnectedDevices()]: ${response.errorMessage} || ${response.httpStatusCode}")
            }

            else -> {
                listOf()
            }
        }

    }

    @Throws(QASDKException::class)
    suspend fun getIdentity(ctx: String): TokenApi.Identity {
        when (val identityResponse = apiService.getIdentity(identityId)) {
            is ApiSuccessResponse -> {
                identityResponse.body?.let {
                    return it
                }
                throw QASDKException("Response Body to identity is empty, this should not happen, file a bug report!")
            }

            is ApiErrorResponse -> {
                throw QASDKException("API Error [getIdentity()] [ctx: $ctx]: ${identityResponse.errorMessage} || ${identityResponse.httpStatusCode}")
            }

            is ApiEmptyResponse -> {
                throw QASDKException("Response Body to identity is empty, this should not happen, file a bug report!")
            }
        }
    }

    suspend fun deleteLocalStudies() {
        mvpDao.deleteStudies()
    }

    private val bearer
        get() = mapOf("x-authorization" to "Bearer ${preferences.accessToken}")

}
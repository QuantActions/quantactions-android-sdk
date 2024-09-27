/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.data.api

import android.util.Base64
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_retrofit.FlowerCallAdapterFactory
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.GenericPreferences
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.adapters.SleepSummaryAdapter
import com.quantactions.sdk.data.api.adapters.StatisticAdapter
import com.quantactions.sdk.data.api.adapters.StatisticStringAdapter
import com.quantactions.sdk.data.api.adapters.TrendAdapter
import com.quantactions.sdk.data.api.responses.*
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.entity.SleepSummaryEntity
import com.quantactions.sdk.data.entity.StatisticEntity
import com.quantactions.sdk.data.entity.StatisticStringEntity
import com.quantactions.sdk.data.entity.TrendEntity
import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.model.DevicePatch
import com.quantactions.sdk.data.model.DeviceRegistration
import com.quantactions.sdk.data.model.DeviceResponse
import com.quantactions.sdk.data.model.DeviceSpecifications
import com.quantactions.sdk.data.model.DeviceSpecificationsResponse
import com.quantactions.sdk.data.model.DeviceStats
import com.quantactions.sdk.data.model.DeviceStatsResponse
import com.quantactions.sdk.data.model.JournalEntryBody
import com.quantactions.sdk.data.model.JournalEvent
import com.quantactions.sdk.data.model.JournalEventBody
import com.quantactions.sdk.data.model.JournalEventEnterResponse
import com.quantactions.sdk.data.model.Note
import com.quantactions.sdk.data.model.QuestionnaireResponse
import com.quantactions.sdk.data.repository.ActivityBody
import com.quantactions.sdk.data.repository.HealthDataBody
import com.quantactions.sdk.data.repository.TapDataBody
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import okhttp3.Authenticator
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.inject.Inject


interface ApiService {

    /**
     * This endpoint is used to get stats from TapCloud relative to the device in use.
     * Use this function to get any stat that is numeric i.e. a score value per day/hour.
     * @param identityId
     * @param participationId
     * @param filter
     * @return A flow wrapping a list of [com.quantactions.sdk.data.entity.StatisticEntity]
     */
    @GET("flows/identities/{identityId}/participations/{participationId}/analyses")
    fun getStatStatisticEntity(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Query("filter") filter: String,
        @Query("containerId") containerId: String
    ): Flow<ApiResponse<List<StatisticEntity>>>

    @GET("flows/identities/{identityId}/participations/{participationId}/analyses")
    fun getStatStatisticStringEntity(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Query("filter") filter: String,
        @Query("containerId") containerId: String
    ): Flow<ApiResponse<List<StatisticStringEntity>>>

    @GET("flows/identities/{identityId}/participations/{participationId}/analyses")
    fun getStatSleepSummaryEntity(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Query("filter") filter: String,
        @Query("containerId") containerId: String
    ): Flow<ApiResponse<List<SleepSummaryEntity>>>

    @GET("flows/identities/{identityId}/participations/{participationId}/analyses")
    fun getTrendEntity(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Query("filter") filter: String,
        @Query("containerId") containerId: String
    ): Flow<ApiResponse<List<TrendEntity>>>

    /**
     * Registers the device with the backend.
     */
    @POST("flows/identities/{id}/devices")
    suspend fun registerDevice(
        @Path("id") identityId: String,
        @Body deviceRegistration: DeviceRegistration
    ): ApiResponse<RegistrationResponse>

    /**
     */
    @POST("flows/identities/{identityId}/devices/specifications")
    suspend fun registerDeviceSpecifications(
        @Path("identityId") identityId: String,
        @Body deviceSpecifications: DeviceSpecifications
    ): ApiResponse<DeviceSpecificationsResponse>


    /**
     * Updates information about the device, for example when a new version of the app has been
     * downloaded, there is an update of the OS or there is a change in system permissions.
     * See [com.quantactions.sdk.data.model.DeviceRegistration] for how to format this info.
     */
    @PATCH("flows/identities/{identityId}/devices/{deviceId}")
    suspend fun updateDevice(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Body devicePatch: DevicePatch
    ): ApiResponse<DeviceRegistration>

    /**
     * Registers the device to a cohort also known as "study". See [com.quantactions.sdk.data.model.SignUpForStudy]
     * for how to format the data in the body of the call. Upon successful registration, returns all
     * the information about the study and questionnaires relative to the study if any. See
     * [com.quantactions.sdk.workers.SignUpForStudyWorker] for an example on how to handle the
     * response of this call.
     */

    /**
     * This endpoint is used to submit some daily statistics of the TapData, these statistics are
     * not really used in the processing but they help debug and check at a glance is anything
     * is out of the ordinary. See [com.quantactions.sdk.data.model.DeviceStats] for info on how to
     * format the body of this call.
     */
    @POST("flows/identities/{identityId}/devices/{deviceId}/recordings/summaries")
    suspend fun submitStatistic(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Body deviceStats: DeviceStats
    ): ApiResponse<DeviceStatsResponse>

    @DELETE("flows/identities/{identityId}/devices/{deviceId}/recordings/summaries/{deviceSummaryId}")
    suspend fun removeStatistic(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Path("deviceSummaryId") deviceSummaryId: String,
    ): ApiResponse<Void>

    // OK
    @POST("flows/identities/{identityId}/devices/{deviceId}/recordings")
    suspend fun submitActivity(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Body activityBody: ActivityBody
    ): ApiResponse<ActivityBody>

    @POST("flows/identities/{identityId}/devices/{deviceId}/recordings")
    suspend fun submitTap(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Body tapHealthDataBody: TapDataBody
    ): ApiResponse<TapDataBody>

    @POST("flows/identities/{identityId}/devices/{deviceId}/recordings")
    suspend fun submitHealth(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Body tapHealthDataBody: HealthDataBody
    ): ApiResponse<HealthDataBody>

    // OK
    /**
     * Use this endpoint to submit a list of apps with their package name and corresponding ID.
     * See [com.quantactions.sdk.data.model.AppToPush] for details on how to format the list of apps.
     */
    @POST("flows/identities/{identityId}/devices/{deviceId}/applications/catalogues")
    suspend fun updateAppList(
        @Path("identityId") identityId: String,
        @Path("deviceId") deviceId: String,
        @Body appsList: List<AppToPush>
    ): ApiResponse<List<AppToPush>>

    /**
     * Submits a simple text note to the backend.
     */
    @POST("flows/identities/{identityId}/notes")
    suspend fun submitNote(
        @Path("identityId") identityId: String,
        @Body note: Note
    ): ApiResponse<Note>

    /**
     * Use this endpoint to submit the answer to a questionnaire. See
     * [com.quantactions.sdk.data.model.QuestionnaireResponse] for info on how to format the body.
     */
    @POST("flows/identities/{identityId}/studies/{studyId}/questionnaires/{questionnaireId}/responses")
    suspend fun submitQuestionnaireAnswer(
        @Path("identityId") identityId: String,
        @Path("studyId") studyId: String,
        @Path("questionnaireId") questionnaireId: String,
        @Body questionnaireResponse: QuestionnaireResponse
    ): ApiResponse<QuestionnaireResponse>

    /////////////////////////////
    ///// JOURNAL ENTRIES ///////
    ////////////////////////////
    /**
     * This endpoint gets the journal events present in the database. These journal events will not
     * change very often, this endpoint can be called once every day (or even less often).
     *
     * @return a list of [com.quantactions.sdk.data.entity.JournalEventEntity]
     * */
    @GET("flows/identities/{identityId}/journals/events/types")
    suspend fun getJournalEventTypes(
        @Path("identityId") identityId: String,
        @Query("filter") filter: String
    ): ApiResponse<List<JournalEventEntity>>

    /**
     * This endpoint pulls the entries from the remote database. This should be only useful when
     * multiple devices are on the same participationId so it synchronizes entries across devices.
     * Generally we can assume local repository as source of truth.
     *
     * @return a list of [com.quantactions.sdk.data.api.responses.JournalEntriesResponse]
     * */
    @GET("flows/identities/{identityId}/journals")
    suspend fun getJournalEntries(
        @Path("identityId") identityId: String,
        @Query("filter") filter: String
    ): ApiResponse<List<JournalEntriesResponse>>


    @GET("flows/identities/{identityId}/journals/{journalId}/events")
    fun getJournalEvents(
        @Query("filter") filter: String
    ): Flow<ApiResponse<List<JournalEvent>>>

    /**
     * Use this endpoint to submit the newly created journal entries. See
     * [com.quantactions.sdk.data.model.JournalEntryBody] for info on how to format the body.
     */
    @POST("flows/identities/{identityId}/journals")
    suspend fun journalEntrySubmit(
        @Path("identityId") identityId: String,
        @Body journalEntryBody: JournalEntryBody
    ): ApiResponse<JournalEntriesResponse>

    @POST("flows/identities/{identityId}/journals/{journalId}/events")
    suspend fun journalEventsSubmit(
        @Path("identityId") identityId: String,
        @Path("journalId") journalId: String,
        @Body journalEventBody: List<JournalEventBody>
    ): ApiResponse<List<JournalEventEnterResponse>>

    @DELETE("flows/identities/{identityId}/journals/{journalId}")
    suspend fun journalEntryDelete(
        @Path("identityId") identityId: String,
        @Path("journalId") journalId: String
    ): ApiResponse<String>

    @DELETE("flows/identities/{identityId}/journals/{journalId}/events/{journalEventId}")
    suspend fun journalEventDelete(
        @Path("identityId") identityId: String,
        @Path("journalId") journalId: String,
        @Path("journalEventId") eventId: String
    ): ApiResponse<String>

    @GET("flows/identities/{identityId}/studies/{studyId}/questionnaires")
    suspend fun getQuestionnaires(
        @Path("identityId") identityId: String,
        @Path("studyId") studyId: String,
    ): ApiResponse<List<Questionnaire>>

    @GET("flows/identities/{identityId}/devices")
    suspend fun getConnectedDevices(
        @Path("identityId") identityId: String,
    ): ApiResponse<List<DeviceResponse>>

    @JsonClass(generateAdapter = true)
    @Serializable
    data class Questionnaire(
        val id: String,
        val code: String,
        val title: String,
        val description: String,
        val language: String,
        val definition: Map<String, @Contextual  Any>,
    )

    @POST("flows/identities/{identityId}/links")
    suspend fun linkIdentities(
        @Path("identityId") identityId: String,
        @Body authLinkBody: AuthLinkBody,
    ): ApiResponse<TokenApi.Identity>


    @PATCH("flows/credentials/{identityId}")
    suspend fun patchCredentials(
        @Path("identityId") identityId: String,
        @Body credentialsRegistration: CredentialsRegistration
    ): ApiResponse<TokenApi.Identity>

    @PATCH("flows/identities/{identityId}")
    suspend fun patchIdentity(
        @Path("identityId") identityId: String,
        @Body identityPatch: IdentityPatch
    ): ApiResponse<TokenApi.Identity>

    @GET("flows/identities/{identityId}")
    suspend fun getIdentity(
        @Path("identityId") identityId: String,
    ): ApiResponse<TokenApi.Identity>

    @POST("flows/identities/{identityId}/participations")
    suspend fun registerToStudy(
        @Path("identityId") identityId: String,
        @Body subscriptionBody: SubscribeWithStudyIdBody
    ): ApiResponse<StudyRegistrationResponse>

    @GET("flows/identities/{identityId}/participations")
    suspend fun getParticipations(
        @Path("identityId") identityId: String,
        @Query("filter") filter: String,
    ): ApiResponse<List<StudyRegistrationResponse>>

    @GET("flows/identities/{identityId}/participations/{participationId}")
    suspend fun getParticipation(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Query("filter") filter: String,
    ): ApiResponse<StudyRegistrationResponse>

    @PATCH("flows/identities/{identityId}/participations/{participationId}")
    suspend fun registerToStudyWithParticipationId(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Body subscriptionBody: SubscribeWithParticipationIdBody
    ): ApiResponse<StudyRegistrationResponse>


    @PATCH("flows/identities/{identityId}/participations/{participationId}")
    suspend fun withdrawParticipation(
        @Path("identityId") identityId: String,
        @Path("participationId") participationId: String,
        @Body withdrawBody: WithdrawBody
    ): ApiResponse<StudyRegistrationResponse>


    @JsonClass(generateAdapter = true)
    @Serializable
    data class AuthLinkBody(
        val id: String
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class SubscribeWithStudyIdBody(
        var studyId: String? = null
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class SubscribeWithParticipationIdBody(
        val identityId: String
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class WithdrawBody(
        var withdraw: String? = null
    )

    enum class SubscriptionVendor {
        GOOGLE,
        ORGANISATION,
    }


    @JsonClass(generateAdapter = true)
    @Serializable
    data class StudyRegistrationResponse(
        var studyId: String?,
        var id: String,
        var study: StudyResponse?,
        var ttl: String?,
        var token: String?
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class StudyResponse(
        var enableAppIdAccess: Boolean,
        var enableDeviceIdAccess: Boolean,
        var enableDeviceNotes: Boolean,
        var enableDrawOverAccess: Boolean,
        var enableHighGpsResolution: Boolean,
        var enableLocationAccess: Boolean,
        var enableRawDataAccess: Boolean,
        var enableStudyIdSignUp: Boolean,
        var enableSyncOnScreenOff: Boolean,
        var enableWithdraw: Boolean,
        var organisationId: String,
        var id: String,
        var name: String,
        var privacyPolicy: String,
        var privacyPolicyDate: String,
        var premiumFeaturesTtlInDays: Int?,
        var created: String
    )



    @JsonClass(generateAdapter = true)
    @Serializable
    data class IdentityPatch(
        val gender: String,
        val yearOfBirth: Int?,
        val selfDeclaredHealthy: Boolean,
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class CredentialsRegistration(
        val identityId: String,
        var password: String?,
    )

    companion object {
        private const val BASE_URL = BuildConfig.QA_API_ROUTE

        fun create(
            apiKey: String,
            tokenAuthenticator: TokenAuthenticator,
            cookieJar: UvCookieJar,
        ): ApiService {
            val logger =
//                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
//                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }

            val client = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .authenticator(tokenAuthenticator)
                .addInterceptor(logger)
                .addInterceptor { chain ->
                    val original = chain.request()
                    // Request customization: add request headers
                    val requestBuilder = original.newBuilder()
                        .header("Content-Type", "application/json; charset=utf8")
                        .header("x-api-key", apiKey)
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .build()

            val customMoshi = Moshi.Builder()
                .add(StatisticAdapter())
                .add(StatisticStringAdapter())
                .add(SleepSummaryAdapter())
                .add(StatisticStringAdapter())
                .add(QuestionnaireAdapter())
                .add(TrendAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addCallAdapterFactory(FlowerCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(customMoshi).withNullSerialization())
                .build()
                .create(ApiService::class.java)
        }
    }

    class UvCookieJar(private val preferences: GenericPreferences, val context: String) : CookieJar {

        private val cookies = mutableListOf<Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach {
                Timber.i("[$context] Saving cookie: ${it.name}")
                if (it.name == "accessToken") preferences.saveAccessTokens(
                    accessToken = it.value,
                )
                if (it.name == "refreshToken") preferences.saveAccessTokens(
                    refreshToken = it.value
                )
            }
            if (
                "accessToken" in cookies.map { it.name }.toList() ||
                "refreshToken" in cookies.map { it.name }.toList()
                ) {
                this.cookies.clear()
                this.cookies.addAll(cookies)
            }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            if (cookies.isEmpty()) {
                preferences.accessToken?.let {
                    Timber.i("[$context] Loading access token for request")
                    cookies.add(
                        createNonPersistentCookie(
                            "accessToken",
                            it
                        )
                    )
                }
                preferences.refreshToken?.let {
                    Timber.i("Loading refresh token for request")
                    cookies.add(
                        createNonPersistentCookie(
                            "refreshToken",
                            it
                        )
                    )
                }
            }
            return cookies
        }
    }
}


class TokenAuthenticator @Inject constructor(
    private val tokenApi: TokenApi,
    private val preferences: GenericPreferences
) : Authenticator {

    override fun authenticate(route: Route?, response: okhttp3.Response): Request {

        Timber.e("Got a 401 : $response")

        // authenticator is called when the call returns a 401
        // 3 scenarios:
        // - the user has never logged in -> accessToken is null -> the device logs in -> saves the tokens -> continues
        // - accessToken has expired -> call the refresh token -> get the new access token -> continues
        // - refresh token has expired -> login -> saves the tokens -> continues

        return runBlocking {


            if (preferences.identityId == "") {
                throw IOException()
            }

            if (!preferences.areCredentialsRegistered){

                val identityRegistration = TokenApi.IdentityRegistration(
                    id = preferences.identityId,
                    password = preferences.password,
                    gender = preferences.gender.code,
                    yearOfBirth = if (preferences.yearOfBirth != 0) preferences.yearOfBirth else null,
                    selfDeclaredHealthy = preferences.selfDeclaredHealthy
                )

                val responseToRegistration = tokenApi.registerIdentity(identityRegistration)

                when (responseToRegistration) {
                    is ApiSuccessResponse, is ApiEmptyResponse -> {
                        Timber.d("Registration successful")
                        preferences.areCredentialsRegistered = true
                    }
                    is ApiErrorResponse -> {
                        Timber.e("Registration failed")
                        if(responseToRegistration.httpStatusCode == 409){
                            Timber.e("Identity already exists")
                            preferences.areCredentialsRegistered = true
                        } else {
                            throw IOException()
                        }
                    }
                }
            }

            if (!preferences.isOauthActivated) {
                tokenApi.enableOauth(getBasicAuthHeader(preferences))
            }

            val iamEntityJWT: ApiResponse<Void> =
                if (preferences.accessToken == null) {
                    Timber.w("SO I login")
                    login()
                } else {
                    Timber.w("SO I refresh")
                    refreshToken()
                }

            when (iamEntityJWT) {
                is ApiSuccessResponse, is ApiEmptyResponse -> {
                    response.request.newBuilder()
                        .addHeader("Set-Cookie",
                           "accessToken=${preferences.accessToken}"
                        )
                        .build()
                }

                is ApiErrorResponse -> {
                    Timber.e("I tried to login/refresh but : $iamEntityJWT")


                    // if refresh fails I need to login again
                    when (val reLogin: ApiResponse<Void> = login()) {
                        is ApiSuccessResponse, is ApiEmptyResponse -> {
                            Timber.d("Got my new tokens let's roll")
                            response.request.newBuilder()
                                .addHeader("Set-Cookie",
                                    listOf("accessToken=${preferences.accessToken}").toString()
                                )
                                .build()
                        }
                        is ApiErrorResponse -> {
                            Timber.w("ReLogin returned an error, so I'll crash $reLogin")
                            throw IOException()
                            // it could be that I receive a 424 when I try to push something but the device
                            // has not been registered
//                            if (reLogin.httpStatusCode == 424 && !preferences.isOauthActivated) {
//                                    tokenApi.enableOauth(getBasicAuthHeader(preferences))
//
//                            } else {
//                                throw IOException()
//                            }

                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshToken(): ApiResponse<Void> {
        return tokenApi.refreshToken()
    }

    private suspend fun login(): ApiResponse<Void> {
        return tokenApi.login(getBasicAuthHeader(preferences))
    }
}

fun getBasicAuthHeader(preferences: GenericPreferences): Map<String, String> {
    val encryptedBasic =
        "${preferences.identityId}:${preferences.password}"
    val base64Basic = Base64.encodeToString(
        encryptedBasic.toByteArray(Charset.forName("UTF-8")),
        Base64.NO_WRAP
    )
    return mapOf("x-authorization" to "basic $base64Basic")
}

fun createNonPersistentCookie(name: String, value: String): Cookie {
    return Cookie.Builder()
        .domain("quantactions.com")
        .path("/")
        .name(name)
        .value(value)
        .httpOnly()
        .secure()
        .build()
}


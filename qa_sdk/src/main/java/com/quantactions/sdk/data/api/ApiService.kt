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
import com.quantactions.sdk.CapabilitiesManager
import com.quantactions.sdk.GenericPreferences
import com.quantactions.sdk.data.api.adapters.QuestionnaireAdapter
import com.quantactions.sdk.data.api.responses.HealthyRangesResponse
import com.quantactions.sdk.data.api.responses.JournalEntriesResponse
import com.quantactions.sdk.data.api.responses.RegistrationResponse
import com.quantactions.sdk.data.api.responses.StatisticResponse
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.model.AppWithCategory
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
import com.quantactions.sdk.data.model.ActivityBody
import com.quantactions.sdk.data.model.HealthDataBody
import com.quantactions.sdk.data.model.TapDataBody
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
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


interface ApiService {

    @GET("healthyranges")
    suspend fun getHealthyRanges(
        @Query("code") code: String? = null,
        @Query("codes") codes: String? = null,
    ): ApiResponse<List<HealthyRangesResponse>>

    /**
     * This endpoint is used to get analysis documents (metrics and trends) from TapCloud relative
     * to the participation in use. The container is automatically derived from the metric code prefix on the server.
     * @param participationId
     * @param code
     * @param start
     * @param end
     * @return An [ApiResponse] wrapping a list of [StatisticResponse]
     */
    @GET("participations/{participationId}/analyses")
    suspend fun getAnalyses(
        @Path("participationId") participationId: String,
        @Query("code") code: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
    ): ApiResponse<List<StatisticResponse>>

    /**
     * Registers the device with the backend.
     */
    @POST("devices")
    suspend fun registerDevice(
        @Body deviceRegistration: DeviceRegistration
    ): ApiResponse<RegistrationResponse>

    /**
     * Updates information about the device, for example when a new version of the app has been
     * downloaded, there is an update of the OS or there is a change in system permissions.
     * See [com.quantactions.sdk.data.model.DeviceRegistration] for how to format this info.
     */
    @PATCH("devices/{deviceId}")
    suspend fun updateDevice(
        @Path("deviceId") deviceId: String,
        @Body devicePatch: DevicePatch
    ): ApiResponse<DeviceRegistration>

    /**
     * This endpoint is used to submit some daily statistics of the TapData, these statistics are
     * not really used in the processing but they help debug and check at a glance is anything
     * is out of the ordinary. See [com.quantactions.sdk.data.model.DeviceStats] for info on how to
     * format the body of this call.
     */
    @POST("devices/{deviceId}/recordings/summaries")
    suspend fun submitStatistic(
        @Path("deviceId") deviceId: String,
        @Body deviceStats: DeviceStats
    ): ApiResponse<DeviceStatsResponse>

    @DELETE("devices/{deviceId}/recordings/summaries/{deviceSummaryId}")
    suspend fun removeStatistic(
        @Path("deviceId") deviceId: String,
        @Path("deviceSummaryId") deviceSummaryId: String,
    ): ApiResponse<Void>

    @POST("devices/{deviceId}/recordings")
    suspend fun submitActivity(
        @Path("deviceId") deviceId: String,
        @Body activityBody: ActivityBody
    ): ApiResponse<ActivityBody>

    @POST("devices/{deviceId}/recordings")
    suspend fun submitTap(
        @Path("deviceId") deviceId: String,
        @Body tapHealthDataBody: TapDataBody
    ): ApiResponse<TapDataBody>

    @POST("devices/{deviceId}/recordings")
    suspend fun submitHealth(
        @Path("deviceId") deviceId: String,
        @Body tapHealthDataBody: HealthDataBody
    ): ApiResponse<HealthDataBody>

    /**
     * Use this endpoint to submit a list of apps with their package name and corresponding ID.
     * See [com.quantactions.sdk.data.model.AppToPush] for details on how to format the list of apps.
     */
    @POST("devices/{deviceId}/applications/catalogues")
    suspend fun updateAppList(
        @Path("deviceId") deviceId: String,
        @Body appsList: List<AppToPush>
    ): ApiResponse<List<AppWithCategory>>

    /**
     * Submits a simple text note to the backend.
     */
    @POST("me/notes")
    suspend fun submitNote(
        @Body note: Note
    ): ApiResponse<Note>

    /**
     * Use this endpoint to submit the answer to a questionnaire. See
     * [com.quantactions.sdk.data.model.QuestionnaireResponse] for info on how to format the body.
     */
    @POST("studies/{studyId}/questionnaires/{questionnaireId}/responses")
    suspend fun submitQuestionnaireAnswer(
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
    @GET("me/journals/events/types")
    suspend fun getJournalEventTypes(): ApiResponse<List<JournalEventEntity>>

    /**
     * This endpoint pulls the entries from the remote database. This should be only useful when
     * multiple devices are on the same participationId so it synchronizes entries across devices.
     * Generally we can assume local repository as source of truth.
     *
     * @return a list of [com.quantactions.sdk.data.api.responses.JournalEntriesResponse]
     * */
    @GET("me/journals")
    suspend fun getJournalEntries(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): ApiResponse<List<JournalEntriesResponse>>

    @GET("me/journals/{journalId}/events")
    fun getJournalEvents(
        @Path("journalId") journalId: String
    ): Flow<ApiResponse<List<JournalEvent>>>

    /**
     * Use this endpoint to submit the newly created journal entries. See
     * [com.quantactions.sdk.data.model.JournalEntryBody] for info on how to format the body.
     */
    @POST("me/journals")
    suspend fun journalEntrySubmit(
        @Body journalEntryBody: JournalEntryBody
    ): ApiResponse<JournalEntriesResponse>

    @POST("me/journals/{journalId}/events")
    suspend fun journalEventsSubmit(
        @Path("journalId") journalId: String,
        @Body journalEventBody: List<JournalEventBody>
    ): ApiResponse<List<JournalEventEnterResponse>>

    @DELETE("me/journals/{journalId}")
    suspend fun journalEntryDelete(
        @Path("journalId") journalId: String
    ): ApiResponse<String>

    @DELETE("me/journals/{journalId}/events/{journalEventId}")
    suspend fun journalEventDelete(
        @Path("journalId") journalId: String,
        @Path("journalEventId") eventId: String
    ): ApiResponse<String>

    @GET("studies/{studyId}/questionnaires")
    suspend fun getQuestionnaires(
        @Path("studyId") studyId: String,
    ): ApiResponse<List<Questionnaire>>

    @GET("devices")
    suspend fun getConnectedDevices(): ApiResponse<List<DeviceResponse>>

    @JsonClass(generateAdapter = true)
    @Serializable
    data class Questionnaire(
        val id: String,
        val code: String,
        val title: String,
        val description: String,
        val language: String,
        val definition: Map<String, @Contextual Any>,
        val completionTimeMinutes: Int?,
    )

    @POST("me/links")
    suspend fun linkIdentities(
        @Body authLinkBody: AuthLinkBody,
    ): ApiResponse<TokenApi.Identity>

    @PATCH("me/credentials")
    suspend fun patchCredentials(
        @Body credentialsRegistration: CredentialsRegistration
    ): ApiResponse<TokenApi.Identity>

    @PATCH("me")
    suspend fun patchIdentity(
        @Body identityPatch: IdentityPatch
    ): ApiResponse<TokenApi.Identity>

    @GET("me")
    suspend fun getIdentity(): ApiResponse<TokenApi.Identity>

    @POST("me/participations")
    suspend fun registerToStudy(
        @Body subscriptionBody: SubscribeWithStudyIdBody
    ): ApiResponse<StudyRegistrationResponse>

    @GET("me/participations")
    suspend fun getParticipations(
        @Query("studyId") studyId: String? = null,
    ): ApiResponse<List<StudyRegistrationResponse>>

    @GET("me/participations/{participationId}")
    suspend fun getParticipation(
        @Path("participationId") participationId: String,
    ): ApiResponse<StudyRegistrationResponse>

    @PATCH("me/participations/{participationId}")
    suspend fun registerToStudyWithParticipationId(
        @Path("participationId") participationId: String,
        @Body subscriptionBody: SubscribeWithParticipationIdBody
    ): ApiResponse<StudyRegistrationResponse>

    @PATCH("me/participations/{participationId}")
    suspend fun withdrawParticipation(
        @Path("participationId") participationId: String,
        @Body withdrawBody: WithdrawBody
    ): ApiResponse<StudyRegistrationResponse>

    /**
     * This endpoint pushes a cognitive test result to the backend scoped to an active study.
     */
    @POST("studies/{studyId}/cognitivetests")
    suspend fun submitStudyCognitiveTestResponse(
        @Path("studyId") studyId: String,
        @Body response: CognitiveTestResponseBody
    ): ApiResponse<IdResponse>

    /**
     * This endpoint pushes a cognitive test result to the backend. The result is a JSON object
     *
     * @return ID of the save test result
     * */
    @POST("me/cognitivetests")
    suspend fun submitCognitiveTestResponse(
        @Body response: CognitiveTestResponseBody
    ): ApiResponse<IdResponse>

    @Serializable
    data class IdResponse(
        val id: String,
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class CognitiveTestResponseBody(
        val testType: String,
        val localTime: String,
        val timestamp: Long,
        @Contextual
        val results: Any
    )

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
        var created: String,
        var enableCognitiveTests: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    @Serializable
    data class CapabilitiesResponse(
        val token: String,
        val expiresIn: Long
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
        val identityId: String? = null,
        var password: String?,
        val verificationCode: String? = null,
    )

    companion object {
        private const val BASE_URL = BuildConfig.QA_API_ROUTE

        fun create(
            apiKey: String,
            tokenAuthenticator: TokenAuthenticator,
            preferences: GenericPreferences,
        ): ApiService {
            val logger =
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                }

            val client = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .authenticator(tokenAuthenticator)
                .addInterceptor(logger)
                .addInterceptor { chain ->
                    val original = chain.request()
                    // Request customization: add request headers
                    val requestBuilder = original.newBuilder()
                        .header("Content-Type", "application/json; charset=utf8")
                        .header("x-api-key", apiKey)
                    val accessToken = preferences.accessToken
                    if (!accessToken.isNullOrEmpty() && original.header("x-authorization") == null) {
                        requestBuilder.header("x-authorization", "Bearer $accessToken")
                    }
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .addInterceptor(GzipRequestInterceptor())
                .build()

            val customMoshi = Moshi.Builder()
                .add(QuestionnaireAdapter())
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
}

class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val body = originalRequest.body
        if (
            body == null ||
            originalRequest.header("Content-Encoding") != null ||
            originalRequest.method != "POST" ||
            !originalRequest.url.encodedPath.endsWith("/recordings")
        ) {
            return chain.proceed(originalRequest)
        }

        val compressedRequest = originalRequest.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(originalRequest.method, gzip(body))
            .build()
        return chain.proceed(compressedRequest)
    }

    private fun gzip(body: RequestBody): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? = body.contentType()

            override fun contentLength(): Long = -1L

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink).buffer()
                body.writeTo(gzipSink)
                gzipSink.close()
            }
        }
    }
}

class TokenAuthenticator @Inject constructor(
    private val tokenApi: TokenApi,
    private val preferences: GenericPreferences,
    private val capabilitiesManager: CapabilitiesManager
) : Authenticator {

    private val authLock = Any()

    override fun authenticate(route: Route?, response: Response): Request {

        Timber.e("Got a 401 [${route}] : $response")

        if (responseCount(response) >= 2) {
            Timber.e("Authentication failed after retry, giving up")
            throw IOException("Max authentication retries exceeded")
        }

        // authenticator is called when the call returns a 401
        // 4 scenarios:
        // - the user has never logged in -> accessToken is null -> the device logs in -> saves the tokens -> continues
        // - accessToken has expired -> call the refresh token -> get the new access token -> continues
        // - refresh token has expired -> login -> saves the tokens -> continues
        // - password is wrong -> reset it

        return synchronized(authLock) {
            runBlocking {

                if (preferences.identityId == "") {
                    throw IOException()
                }

                // If another concurrent request already refreshed the access token while we waited for the lock, reuse it
                val currentAccessToken = preferences.accessToken
                val requestAuthHeader = response.request.header("x-authorization")
                if (!currentAccessToken.isNullOrEmpty() && requestAuthHeader != null && requestAuthHeader != "Bearer $currentAccessToken") {
                    return@runBlocking buildNewRequestWithAccessToken(response, preferences)
                }

                if (!preferences.areCredentialsRegistered) {

                    val identityRegistration = TokenApi.IdentityRegistration(
                        id = preferences.identityId,
                        password = preferences.password,
                        gender = preferences.gender.code,
                        yearOfBirth = if (preferences.yearOfBirth >= 1900) preferences.yearOfBirth else null,
                        selfDeclaredHealthy = preferences.selfDeclaredHealthy
                    )

                    val responseToRegistration = tokenApi.registerIdentity(identityRegistration)

                    when (responseToRegistration) {
                        is ApiSuccessResponse -> {
                            Timber.d("Registration successful")
                            preferences.areCredentialsRegistered = true
                            if (!responseToRegistration.body?.accessToken.isNullOrEmpty()) {
                                preferences.saveAccessTokens(
                                    accessToken = responseToRegistration.body?.accessToken,
                                    refreshToken = responseToRegistration.body?.refreshToken
                                )
                                return@runBlocking buildNewRequestWithAccessToken(response, preferences)
                            }
                        }
                        is ApiEmptyResponse -> {
                            Timber.d("Registration successful")
                            preferences.areCredentialsRegistered = true
                        }
                        is ApiErrorResponse -> {
                            Timber.e("Registration failed")
                            if (responseToRegistration.httpStatusCode == 409) {
                                Timber.e("Identity already exists")
                                preferences.areCredentialsRegistered = true
                            } else {
                                throw IOException()
                            }
                        }
                    }
                }

                val iamEntityJWT: ApiResponse<TokenApi.AuthResponse> =
                    if (preferences.accessToken == null) login() else refreshToken()

                when (iamEntityJWT) {
                    is ApiSuccessResponse, is ApiEmptyResponse -> {
                        buildNewRequestWithAccessToken(response, preferences)
                    }

                    is ApiErrorResponse -> {
                        Timber.e("I tried to login/refresh but : $iamEntityJWT")
                        // if refresh fails I need to login again
                        when (val reLogin: ApiResponse<TokenApi.AuthResponse> = login()) {
                            is ApiSuccessResponse, is ApiEmptyResponse -> {
                                Timber.d("Got my new tokens let's roll")
                                buildNewRequestWithAccessToken(response, preferences)
                            }
                            is ApiErrorResponse -> {
                                Timber.w("Login returned an error, $reLogin")
                                if (reLogin.httpStatusCode == 401) { // password mismatch
                                    Timber.e("Password mismatch: $reLogin")
                                    resetPasswordWorkflow(response)
                                } else throw IOException()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private suspend fun resetPasswordWorkflow(response: Response): Request {
        // reset password
        val passwordResetBody = TokenApi.PasswordResetBody(
            id = preferences.identityId,
            password = preferences.password!!,
            verificationCode = BuildConfig.QA_VERIFICATION_CODE
        )

        when (val responseToPasswordReset = tokenApi.resetPassword(passwordResetBody)) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                Timber.d("Password reset successful")
                when (val treLogin: ApiResponse<TokenApi.AuthResponse> = login()) {
                    is ApiSuccessResponse, is ApiEmptyResponse -> {
                        Timber.d("Got my new tokens let's roll")
                        return buildNewRequestWithAccessToken(response, preferences)
                    }
                    is ApiErrorResponse -> {
                        Timber.w("TreLogin returned an error, so I'll crash $treLogin")
                        throw IOException()
                    }
                }
            }
            is ApiErrorResponse -> {
                Timber.e("Password reset failed")
                // add a catch for 409
                if (responseToPasswordReset.httpStatusCode == 409) {
                    Timber.e("Password was already changed: ${responseToPasswordReset.errorMessage}")
                    return buildNewRequestWithAccessToken(response, preferences)
                } else {
                    Timber.e(responseToPasswordReset.errorMessage)
                    throw IOException()
                }
            }
        }
    }

    private suspend fun refreshToken(): ApiResponse<TokenApi.AuthResponse> {
        Timber.d("I refresh")
        val headers = preferences.refreshToken?.takeIf { it.isNotEmpty() }
            ?.let { mapOf("x-authorization" to "Bearer $it") }
            ?: emptyMap()
        val result = tokenApi.refreshToken(headers)
        if (result is ApiSuccessResponse && !result.body?.accessToken.isNullOrEmpty()) {
            preferences.saveAccessTokens(
                accessToken = result.body?.accessToken,
                refreshToken = result.body?.refreshToken
            )
        }
        return result
    }

    private suspend fun login(): ApiResponse<TokenApi.AuthResponse> {
        Timber.d("I login")
        val result = tokenApi.login(getBasicAuthHeader(preferences))
        if (result is ApiSuccessResponse && !result.body?.accessToken.isNullOrEmpty()) {
            preferences.saveAccessTokens(
                accessToken = result.body?.accessToken,
                refreshToken = result.body?.refreshToken
            )
        }
        return result
    }
}

private fun buildNewRequestWithAccessToken(response: Response, preferences: GenericPreferences): Request {
    return response.request.newBuilder()
        .header("x-authorization", "Bearer ${preferences.accessToken ?: ""}")
        .build()
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


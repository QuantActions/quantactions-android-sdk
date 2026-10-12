/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.Subscription
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.adapters.SubscriptionWithQuestionnaires
import com.quantactions.sdk.data.api.getBasicAuthHeader
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.Questionnaire
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.entity.QuestionnaireWithCohortName
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.model.QuestionnaireResponse
import com.quantactions.sdk.exceptions.QASDKException
import com.quantactions.sdk.data.stringify
import com.quantactions.sdk.workers.SignUpForStudyWorker
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Repository for managing studies, questionnaires, and participation.
 */
internal class StudyRepository(
    private val preferences: ManagePref2,
    private val identityRepository: IdentityRepository,
    private val networkManager: NetworkManager,
    private val mvpDao: MVPDao,
    private val workManager: WorkManager
) {
    @Throws(QASDKException::class)
    suspend fun registerToStudy(studyId: String): SubscriptionWithQuestionnaires {
        val apiResponse = networkManager.apiService.registerToStudy(
            identityRepository.identityId,
            ApiService.SubscribeWithStudyIdBody(studyId = studyId)
        )

        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let {
                    Timber.d("SUCCESS while signup $it")
                    val cohort = getStudyInfo(it.studyId!!) ?: throw QASDKException("Cohort is null")
                    val questionnaires = getQuestionnaires(it.studyId!!)
                    mvpDao.insertOrUpdateStudy(cohort)
                    return SubscriptionWithQuestionnaires(
                        cohort, questionnaires, it.id, listOf(),
                        if (it.ttl != null) ZonedDateTime.parse(it.ttl).toInstant().toEpochMilli() else -1
                    )
                }
                throw QASDKException("Response Body to subscription is empty")
            }
            is ApiErrorResponse -> {
                Timber.e("ERROR while signup ${apiResponse.errorMessage}")
                if (apiResponse.httpStatusCode in listOf(424, 404, 403)) {
                    throw QASDKException("ParticipationID is invalid")
                } else {
                    val data = Data.Builder()
                    data.putString(SignUpForStudyWorker.SubscriptionConstants.COHORT_ID.name, studyId)
                    val signUpWork = OneTimeWorkRequest.Builder(SignUpForStudyWorker::class.java)
                        .addTag("signupForStudy")
                        .setInitialDelay(2, TimeUnit.MINUTES)
                        .setInputData(data.build())
                        .build()
                    workManager.enqueueUniqueWork("signupForStudy", ExistingWorkPolicy.KEEP, signUpWork)
                    throw QASDKException("Network issues, we will retry later.")
                }
            }
            is ApiEmptyResponse -> throw QASDKException()
        }
    }

    suspend fun registerToStudyWithParticipationId(participationId: String): SubscriptionWithQuestionnaires {
        val identityId = identityRepository.identityId
        val apiResponse = networkManager.apiService.registerToStudyWithParticipationId(
            identityId,
            participationId,
            ApiService.SubscribeWithParticipationIdBody(identityId = identityId)
        )

        when (apiResponse) {
            is ApiSuccessResponse -> {
                apiResponse.body?.let {
                    Timber.d("SUCCESS while signup $it")
                    networkManager.tokenApi.login(getBasicAuthHeader(preferences))
                    val cohort = getStudyInfoWithParticipationId(participationId) ?: throw QASDKException("Cohort is null")
                    val questionnaires = getQuestionnaires(cohort.cohortId)
                    mvpDao.insertOrUpdateStudy(cohort)
                    return SubscriptionWithQuestionnaires(
                        cohort, questionnaires, it.id, listOf(),
                        if (it.ttl != null) ZonedDateTime.parse(it.ttl).toInstant().toEpochMilli() else -1
                    )
                }
                throw QASDKException("Body is null")
            }
            is ApiErrorResponse -> {
                Timber.e("ERROR while signup ${apiResponse.errorMessage}")
                if (apiResponse.httpStatusCode in listOf(424, 404, 403)) {
                    throw QASDKException("ParticipationID is invalid")
                } else {
                    val data = Data.Builder()
                    data.putString(SignUpForStudyWorker.SubscriptionConstants.SUBSCRIPTION_ID.name, participationId)
                    val signUpWork = OneTimeWorkRequest.Builder(SignUpForStudyWorker::class.java)
                        .addTag("signupForStudy")
                        .setInitialDelay(2, TimeUnit.MINUTES)
                        .setInputData(data.build())
                        .build()
                    workManager.enqueueUniqueWork("signupForStudy", ExistingWorkPolicy.KEEP, signUpWork)
                    throw QASDKException("Network issues! Retry again later")
                }
            }
            is ApiEmptyResponse -> throw QASDKException()
        }
    }

    suspend fun getQuestionnaires(studyId: String): List<Questionnaire> {
        return when (val response = networkManager.apiService.getQuestionnaires(identityRepository.identityId, studyId)) {
            is ApiSuccessResponse -> {
                val mapped = response.body!!.map {
                    Questionnaire(it.id, it.title, it.description, it.code, studyId, it.definition.toMutableMap().stringify(), it.completionTimeMinutes ?: 5)
                }
                mvpDao.insertOrUpdateQuestionnaire(mapped)
                mapped
            }
            else -> listOf()
        }
    }

    suspend fun getStudyInfo(studyId: String): Cohort? {
        val filter = mutableMapOf<String, Any>()
        filter["where"] = mapOf("studyId" to studyId)
        filter["include"] = listOf(mapOf("relation" to "study", "scope" to mapOf("where" to mapOf("deleted" to null))))

        return when (val response = networkManager.apiService.getParticipations(identityRepository.identityId, filter.stringify())) {
            is ApiSuccessResponse -> {
                val studyInfo = response.body!!.first { it.studyId == studyId }.study!!
                val cohort = Cohort(studyInfo.id, studyInfo.privacyPolicy, studyInfo.name, null, 0, if (studyInfo.enableWithdraw) 1 else 0, if (studyInfo.enableSyncOnScreenOff) 1 else 0, if (studyInfo.enableAppIdAccess) 1 else 0, if (studyInfo.enableSyncOnScreenOff) 1 else 0, if (studyInfo.enableDrawOverAccess) 1 else 0, 0, 0, studyInfo.enableCognitiveTests ?: false)
                mvpDao.insertOrUpdateStudy(cohort)
                cohort
            }
            else -> null
        }
    }

    suspend fun getStudyInfoWithParticipationId(participationId: String): Cohort? {
        return when (val response = networkManager.apiService.getParticipation(identityRepository.identityId, participationId, "{\"include\":[{\"relation\":\"study\",\"scope\":{\"where\":{\"deleted\":null}}}]}")) {
            is ApiSuccessResponse -> {
                val studyInfo = response.body!!.study!!
                val cohort = Cohort(studyInfo.id, studyInfo.privacyPolicy, studyInfo.name, null, 0, if (studyInfo.enableWithdraw) 1 else 0, if (studyInfo.enableSyncOnScreenOff) 1 else 0, if (studyInfo.enableAppIdAccess) 1 else 0, if (studyInfo.enableSyncOnScreenOff) 1 else 0, if (studyInfo.enableDrawOverAccess) 1 else 0, 0, 0, studyInfo.enableCognitiveTests ?: false)
                mvpDao.insertOrUpdateStudy(cohort)
                cohort
            }
            else -> null
        }
    }

    fun getStudies(): List<Cohort> = mvpDao.getStudies()
    fun getStudiesSingle(): List<Cohort> = mvpDao.getStudiesSingle()
    fun getQuestionnaires(): List<QuestionnaireWithCohortName> = mvpDao.getQuestionnaires()

    suspend fun getSubscriptions(studyId: String? = null): List<Subscription> {
        val filter = mutableMapOf<String, Any>()
        filter["where"] = mapOf("deleted" to null, "withdraw" to null)
        filter["include"] = listOf(mapOf("relation" to "study", "scope" to mapOf("where" to mapOf("deleted" to null))))

        val response = networkManager.apiService.getParticipations(identityRepository.identityId, filter.stringify())
        return if (response is ApiSuccessResponse) {
            val results = response.body!!
            val items = if (studyId != null) results.filter { it.studyId == studyId } else results
            items.map { p ->
                Subscription(
                    p.id,
                    listOf(),
                    p.studyId!!,
                    p.study?.name ?: "",
                    if (p.ttl != null) ZonedDateTime.parse(p.ttl).toInstant().toEpochMilli() else -1,
                    p.token
                )
            }
        } else {
            emptyList()
        }
    }

    suspend fun withdraw(participationId: String, studyId: String) {
        val body = ApiService.WithdrawBody(withdraw = "voluntary")
        when (val response = networkManager.apiService.withdrawParticipation(identityRepository.identityId, participationId, body)) {
            is ApiSuccessResponse -> mvpDao.deleteStudy(studyId)
            else -> Timber.e("Withdraw failed")
        }
    }

    suspend fun reSubscribe(participationId: String) {
        val body = ApiService.SubscribeWithParticipationIdBody(identityId = identityRepository.identityId)
        networkManager.apiService.registerToStudyWithParticipationId(identityRepository.identityId, participationId, body)
    }

    private fun prepareQuestionnaireResponseSubmit(answer: QuestionnaireResponseEntity): QuestionnaireResponse {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(Any::class.java)
        val jsonStructure = adapter.fromJson(answer.qResponse)
        @Suppress("UNCHECKED_CAST") val responseObject = jsonStructure as Map<String, Any>?
        val date = Instant.ofEpochMilli(answer.qDate).atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_DATE_TIME)
        return QuestionnaireResponse(date, responseObject)
    }

    suspend fun sendQuestionnaireResponse(answer: QuestionnaireResponseEntity) {
        val daoResponse = mvpDao.insertQuestionnaireResponse(answer)
        val studyId = answer.qFullID.split(":")[0]
        val questionnaireId = answer.qFullID.split(":")[1]
        val toPush = prepareQuestionnaireResponseSubmit(answer)

        val apiResponse = networkManager.apiService.submitQuestionnaireAnswer(
            identityRepository.identityId,
            studyId,
            questionnaireId,
            toPush
        )
        if (apiResponse is ApiSuccessResponse) {
            mvpDao.deleteQuestionnaireResponse(daoResponse)
        }
    }

    suspend fun sendPendingQuestionnaireResponse(answer: QuestionnaireResponseEntity): ApiResponse<QuestionnaireResponse> {
        val studyId = answer.qFullID.split(":")[0]
        val questionnaireId = answer.qFullID.split(":")[1]
        val toPush = prepareQuestionnaireResponseSubmit(answer)
        return networkManager.apiService.submitQuestionnaireAnswer(
            identityRepository.identityId,
            studyId,
            questionnaireId,
            toPush
        )
    }

    fun getQuestionnaireResponses(): List<QuestionnaireResponseEntity> = mvpDao.getQuestionnaireResponses()
    fun deleteQuestionnaireResponse(id: Long) = mvpDao.deleteQuestionnaireResponse(id)
}

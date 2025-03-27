/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.adapters

import androidx.annotation.Keep
import com.quantactions.sdk.data.api.responses.StudySignupResponse
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.Questionnaire
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson

/**
 * Class containing the information about a study and all of its questionnaires.
 * @param cohort the [Cohort]  (a.k.a. study)
 * @param listOfQuestionnaires the list of [Questionnaire]s associated with the study
 * @param subscriptionId the id of the subscription (a.k.a. participationID)
 * @param tapDeviceIds the list of tap device ids associated with the subscription
 * @param premiumFeaturesTTL the time to live of the premium features associated with the subscription
 *
 * TODO: we need to rethink return the premium features TTL, it's not really a property of the SDk
 * as we do not sell subscriptions, BUT we could have features in the SDk that are premium e.g.
 * the different metrics.
 */
@Keep
@JsonClass(generateAdapter = true)
data class SubscriptionWithQuestionnaires(
    val cohort: Cohort,
    val listOfQuestionnaires: List<Questionnaire>,
    val subscriptionId: String,
    val tapDeviceIds: List<String>,
    val premiumFeaturesTTL: Long,
    val token: String? = null
)


/**
 * Api adapter to correctly format the response to the signupForStudy endpoint which returns also all
 * the subscriptions (studies) associated with the device. see [StudySignupResponse] for the raw
 * response to the endpoint. Produces a list of [SubscriptionWithQuestionnaires] which is the entity
 * used by the SDK user (app) to access study data.
 */
@Keep
class StudyAdapter {
    @FromJson
    fun fromJson(response: StudySignupResponse): SubscriptionWithQuestionnaires {

            val s = response.study!!
            val tapDeviceIds = response.deviceParticipations!!.map { it.tapDeviceId!! }

            val cohort = Cohort(
                s.id!!,
                s.privacyPolicy,
                s.title,
                s.dataPattern,
                s.gpsResolution,
                s.canWithdraw,
                s.syncOnScreenOff,
                s.perimeterCheck,
                s.permAppId,
                s.permDrawOver,
                s.permLocation,
                s.permContact,
                s.enableCognitiveTests ?: false
            )


            val questionnaireEntities: MutableList<Questionnaire> = mutableListOf()

            s.questionnaires?.forEach { questionnaire ->
                questionnaireEntities.add(
                    Questionnaire(
                        s.id + ":" + questionnaire.id,
                        questionnaire.title!!,
                        questionnaire.description!!,
                        questionnaire.id!!,
                        s.id!!,
                        questionnaire.definition!!
                    )
                )
            }

            return SubscriptionWithQuestionnaires(
                cohort,
                questionnaireEntities,
                response.participationId!!,
                tapDeviceIds,
                response.participationTtlInMillis ?: 0L
            )
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") value: Pair<Cohort, List<Questionnaire>>): StudySignupResponse {
        throw UnsupportedOperationException()
    }
}
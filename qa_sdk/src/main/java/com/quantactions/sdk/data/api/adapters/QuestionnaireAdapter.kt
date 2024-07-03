/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.adapters

import androidx.annotation.Keep
import com.quantactions.sdk.data.api.responses.Participations
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.Questionnaire
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant

/**
 * Api adapter to correctly format the response to the updateDevice endpoint which returns also all
 * the subscriptions (studies) associated with the device. see [Participations] for the raw response
 * to the endpoint. Produces a list of [SubscriptionWithQuestionnaires] which is the entity used by
 * the SDK user to access study data.
 *
 */
@Keep
class QuestionnaireAdapter {
    @FromJson
    fun fromJson(response: Participations): List<SubscriptionWithQuestionnaires> {

        val ret: MutableList<SubscriptionWithQuestionnaires> = mutableListOf()
        // Here I need to update the studies as well:
        // 1. Some study PP might have changed
        // 2. It could be the transition phase


        response.participations?.forEach{ devPart ->
            val s = devPart.participation?.study!!

            val cohort = Cohort(s.id!!, s.privacyPolicy, s.title, s.dataPattern, s.gpsResolution,
                s.canWithdraw, s.syncOnScreenOff, s.perimeterCheck, s.permAppId, s.permDrawOver, s.permLocation, s.permContact)

            val questionnaireEntities: MutableList<Questionnaire> = mutableListOf()

            val tapDeviceIds = mutableListOf<String>()

            devPart.participation?.study?.questionnaires?.forEach { questionnaire ->
                questionnaireEntities.add(
                    Questionnaire(s.id + ":" + questionnaire.id,
                        questionnaire.title!!,
                        questionnaire.description!!, questionnaire.id!!, s.id!!, questionnaire.definition!!)
                )
            }

            devPart.participation?.deviceParticipations?.forEach{
                tapDeviceIds.add(it.tapDeviceId!!)
            }


            ret.add(SubscriptionWithQuestionnaires(
                cohort,
                questionnaireEntities,
                devPart.participationId!!,
                tapDeviceIds,
                Instant.now().toEpochMilli()
            ))
        }
        return ret
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") value: List<Questionnaire>): Participations {
        throw UnsupportedOperationException()
    }
}
/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalUnit

/**
 * Serialization adapter for ZonedDateTime, useful for flutter plugin.
 * @hide
 *
 * @property sleepStart
 * @property sleepEnd
 * @property interruptionsStart
 * @property interruptionsEnd
 * @property interruptionsNumberOfTaps
 */
@JsonClass(generateAdapter = true)
@Serializable
data class SerializableSleepSummary(
    val sleepStart: String,
    val sleepEnd: String ,
    val interruptionsStart: List<String> = listOf(),
    val interruptionsEnd: List<String> = listOf(),
    val interruptionsNumberOfTaps: List<Int> = listOf(),
)

/**
 * This data class hold detailed information about a sleep session (or sleep episode).
 * See also [com.quantactions.sdk.Metric.SLEEP_SUMMARY] for a Time series of these episodes (generally one per day/night).
 *
 * @property sleepStart zoned date time of the bed time
 * @property sleepEnd zoned date time of the wake up time
 * @property interruptionsStart list of zoned date times of the beginning on interruptions of the sleep episode
 * @property interruptionsEnd list of zoned date times of the end on interruptions of the sleep episode
 * @property interruptionsNumberOfTaps number of taps in each sleep interruption
 */
@Keep
data class SleepSummary (
    val sleepStart: ZonedDateTime = ZonedDateTimePlaceholder,
    val sleepEnd: ZonedDateTime = ZonedDateTimePlaceholder,
    val interruptionsStart: List<ZonedDateTime> = listOf(),
    val interruptionsEnd: List<ZonedDateTime> = listOf(),
    val interruptionsNumberOfTaps: List<Int> = listOf(),
) {
    /**
     * Checks if the sleep episode is empty and thus just a placeholder
     *
     * @return a boolean, true if the object is only a placeholder
     */
    @Keep
    fun isEmpty(): Boolean {
        return sleepStart == ZonedDateTimePlaceholder
    }

    @Keep
    companion object {
        /** Date considered a placeholder */
        @Keep
        val ZonedDateTimePlaceholder: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("UTC"))
    }

    fun serialize() : SerializableSleepSummary {
        return SerializableSleepSummary(
            sleepStart.toEpochSecond().toString() + "=" + sleepStart.zone.id,
            sleepEnd.toEpochSecond().toString() + "=" + sleepEnd.zone.id,
            interruptionsStart.map { it.toEpochSecond().toString() + "=" + sleepStart.zone.id },
            interruptionsEnd.map { it.toEpochSecond().toString() + "=" + sleepStart.zone.id },
            interruptionsNumberOfTaps
        )
    }

}



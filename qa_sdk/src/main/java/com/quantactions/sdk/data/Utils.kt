/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data

import android.util.Base64
import com.quantactions.sdk.ManagePref2
import org.json.JSONObject
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * @suppress
 */
fun getAuthHeader(preferences: ManagePref2): Map<String, String> {
    val encryptedBasic =
        "${preferences.identityId}:${preferences.password}"
    val base64Basic = Base64.encodeToString(
        encryptedBasic.toByteArray(Charset.forName("UTF-8")),
        Base64.NO_WRAP
    )
    return mapOf("x-authorization" to "basic $base64Basic")
}

/**
 * @suppress
 */
fun <K, V> MutableMap<K, V>.stringify(): String {
    return JSONObject(this.toMap()).toString()
}

/**
 * @suppress
 */
private fun addDash(str: String, index: Int): String {
    return str.substring(0, index) + "-" + str.substring(index)
}

/**
 * @suppress
 */
fun specialIDtoUUID(id: String): String {
    var st = id.substring(4, id.length - 4)
    st = addDash(st, 8)
    st = addDash(st, 13)
    st = addDash(st, 18)
    st = addDash(st, 23)
    return st
}

/**
 * @suppress
 */
@Deprecated("Additional strings have been removed only use UUIDs now", replaceWith = ReplaceWith("Nothing"))
fun isPartIdValid(partId: String): Boolean {
    return Pattern.matches("aef3[a-zA-Z\\d._-]+de19", partId) ||
            Pattern.matches("138e[a-zA-Z\\d._-]+28eb", partId)
}

/**
 * @suppress
 */
private fun parseDateFromPostgres(created: String): Long {
    return LocalDateTime.from(
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .parse(created)
    ).toInstant(ZoneOffset.UTC).toEpochMilli()
}

private val GMT_OFFSET_REGEX: Pattern = Pattern.compile("^(?:[A-Za-z]+)?([+-])(\\d{1,2}):(\\d{1,2})$")

/**
 * Robustly parses a timezone string that may come from legacy systems, Android, or backend
 * in non-standard formats (such as "GMT+1:0", "GMT-5:0", "+1:0", etc.) as well as standard
 * IANA identifiers ("Europe/Zurich") and standard offsets ("GMT+01:00", "+01:00", "UTC").
 */
fun parseZoneId(timeZone: String?): java.time.ZoneId {
    if (timeZone.isNullOrBlank()) return java.time.ZoneId.of("UTC")
    val trimmed = timeZone.trim()
    try {
        return java.time.ZoneId.of(trimmed)
    } catch (_: Exception) {}

    // Check for formats like GMT+1:0, GMT-5:0, +1:0, UTC+2:0
    val matcher = GMT_OFFSET_REGEX.matcher(trimmed)
    if (matcher.matches()) {
        val sign = matcher.group(1) ?: "+"
        val hours = matcher.group(2)?.toIntOrNull() ?: 0
        val minutes = matcher.group(3)?.toIntOrNull() ?: 0
        val normalized = String.format(java.util.Locale.ROOT, "GMT%s%02d:%02d", sign, hours, minutes)
        try {
            return java.time.ZoneId.of(normalized)
        } catch (_: Exception) {}
    }

    // Fallback using java.util.TimeZone which parses custom legacy formats
    try {
        val tz = java.util.TimeZone.getTimeZone(trimmed)
        if (tz != null && (tz.id != "GMT" || trimmed.startsWith("GMT") || trimmed == "UTC")) {
            return tz.toZoneId()
        }
    } catch (_: Exception) {}

    return java.time.ZoneId.of("UTC")
}

/**
 * Robustly parses date strings in various ISO formats (with or without milliseconds,
 * or date-only) without throwing DateTimeParseException.
 */
fun parseFlexDateTime(dateStr: String?): LocalDateTime {
    if (dateStr.isNullOrBlank()) return LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
    val trimmed = dateStr.trim()
    return try {
        java.time.Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDateTime()
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME)
        } catch (_: Exception) {
            try {
                java.time.LocalDate.parse(trimmed).atStartOfDay()
            } catch (_: Exception) {
                LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
            }
        }
    }
}
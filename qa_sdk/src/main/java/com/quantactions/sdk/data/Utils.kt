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
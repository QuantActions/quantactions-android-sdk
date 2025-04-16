/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import androidx.room.TypeConverter
import com.google.gson.Gson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringToListLong(value: String): List<Long> {
        return Json.decodeFromString(ListSerializer(Long.serializer()), value)
    }

    @TypeConverter
    fun fromStringToListInt(value: String): List<Int> {
        return Json.decodeFromString(ListSerializer(Int.serializer()), value)
    }

    @TypeConverter
    fun fromListToLong(list: List<Long>): String {
        return Json.encodeToString(ListSerializer(Long.serializer()), list)
    }

    @TypeConverter
    fun fromListToInt(list: List<Int>): String {
        return Json.encodeToString(ListSerializer(Int.serializer()), list)
    }

    private val gson = Gson()

    @TypeConverter
    fun fromJson(value: String): Map<String, Any> {
        return gson.fromJson(value, Map::class.java) as Map<String, Any>
    }

    @TypeConverter
    fun toJson(value: Map<String, Any>): String {
        return gson.toJson(value)
    }

}
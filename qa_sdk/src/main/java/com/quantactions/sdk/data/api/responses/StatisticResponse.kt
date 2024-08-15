/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api.responses

import com.quantactions.sdk.data.api.SkipSerialization
import com.squareup.moshi.JsonClass

/**
 * This is the data class that represent the body response for the API calls
 * getStatStatisticEntity...
 * @suppress
 * @property code code of the metric or trend
 * @property extras extra information about the metric or trend, it's a JSON string but we don;t parse it
 * @property participationId id of the participation
 * @property id of the call to the API, ignore it
 * @property metrics the actual data, see [StatisticCore] for more information
 * @property publicName name of the metric or trend (e.g. "Sleep Duration")
 * @property timestamp UNIX timestamp of the creation date, ignore it
 *
 */
@JsonClass(generateAdapter = true)
data class StatisticResponse (
    @SkipSerialization
    val code: String,
    val extras: String?,
    val participationId: String,
    val id: String,
    val metrics: StatisticCore,
    val publicName: String,
    val timestamp: String

)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class StatisticData (
    @SkipSerialization
    val params: Map<String, Any>,
    val metrics: StatisticCore,
)

/**
 * @suppress
 */
@JsonClass(generateAdapter = true)
data class StatisticCore (
    @SkipSerialization
    val schema: DataFrameSchema,
    val data: List<Map<String, Any?>>,
)

/**
 * This is the data class that represent the schema of the DataFrame, if familiar with python's pandas
 * it is mostly useful to know the type and name of each column.
 * @suppress
 * @param fields list of fields, meaning columns, of the DataFrame, with name and type
 * @param primaryKey list of columns that are the primary key of the DataFrame, generally this will be `["index"]` or `["date"]`
 * @param pandas_version version of pandas used to generate the DataFrame, can be ignored
 */
@JsonClass(generateAdapter = true)
data class DataFrameSchema (
    @SkipSerialization
    val fields: List<Map<String, String>>,
    val primaryKey: List<String>,
    val pandas_version: String,
)

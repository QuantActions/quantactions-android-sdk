/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk

import com.quantactions.sdk.data.model.AppToPush
import com.quantactions.sdk.data.repository.DeviceHealthParsedToPush
import com.quantactions.sdk.data.repository.TapDataParsedToPush

class Payloads(
    val tapDataParsedPayload: List<TapDataParsedToPush> =
        listOf(
            TapDataParsedToPush(
                "1", listOf(1672731645051L, 1672731649550L, 1672731652042L),
                1672731642882,
                1672731905648,
                listOf(1, 1, 1),
                listOf(listOf(202, 202, 202), listOf(203, 203, 203), listOf(204, 204, 204)),
                "Europe/Zurich",
                0
            ),
            TapDataParsedToPush(
                "1", listOf(),
                1672731642883,
                1672731905649,
                listOf(),
                listOf(listOf(), listOf(), listOf()),
                "Europe/Zurich",
                0
            )
        ),
    val deviceHealthParsedPayload: List<DeviceHealthParsedToPush> = listOf(
        DeviceHealthParsedToPush(1672731931972, 29, "1"),
        DeviceHealthParsedToPush(1672733139415, 30, "2"),
        DeviceHealthParsedToPush(1672734019955, 31, "3"),
    ),

    val appsToPush: List<AppToPush> = listOf(
        AppToPush("com.weird.app1", 202),
        AppToPush("com.weird.app2", 203),
        AppToPush("com.weird.app3", 204),
    )
)


/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.api

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes


class SkipSerializedStrategy {

    companion object {
        fun getStrategy(): ExclusionStrategy {
            return object : ExclusionStrategy {
                override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                    return false
                }

                override fun shouldSkipField(field: FieldAttributes): Boolean {
                    return field.getAnnotation(SkipSerialization::class.java) != null
                }
            }
        }
    }
}
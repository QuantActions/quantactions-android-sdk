/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import java.util.Random

/**
 * @suppress
 */
object GeneratePassword {
    /**
     * Genera una password RANDOM
     */
    const val DATA1 = "0123456789"
    const val DATA2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val DATA3 = "abcdefghijklmnopqrstuvwxyz"
    const val DATA4 = "_~$@*()"

    var RANDOM = Random()
    fun randomString(): String {
        val len = 28
        val sb = StringBuilder(len)
        for (i in 0 until len) {
            when (i % 4) {
                0 -> {sb.append(DATA1[RANDOM.nextInt(DATA1.length)])}
                1 -> {sb.append(DATA2[RANDOM.nextInt(DATA2.length)])}
                2 -> {sb.append(DATA3[RANDOM.nextInt(DATA3.length)])}
                3 -> {sb.append(DATA4[RANDOM.nextInt(DATA4.length)])}
            }

        }
        return sb.toString()
    }
}
/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

/**
 * This class will contain the result to the call [QA.getLastTaps]
 * The length of taps and speed depends on the number of days requested.
 * The taps array is an integer array while the speed array is float array
 */
class QATaps(var taps: List<Int>, var totalTaps: Int, var speed: List<Float>)
//public class QATaps(var total_taps: Int)

/**
 * @suppress
 */
fun String.literalToLongList(): List<Long> {
    return this.filter { it !in listOf('[', ']') }.split(',').filter { it != "" }.map{it.trim().toLong()}
}

/**
 * @suppress
 */
fun String.literalToIntList(): List<Int> {
    return this.filter { it !in listOf('[', ']') }.split(',').filter { it != "" }.map{it.trim().toInt()}
}

/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActuatorSpeedTest {

    @Test
    fun testCalculateSpeedEmptyAndSingleTap() {
        assertEquals(0f, Actuator.calculateSpeed(emptyList()), 0.001f)
        assertEquals(0f, Actuator.calculateSpeed(listOf(1000L)), 0.001f)
    }

    @Test
    fun testCalculateSpeedUniformIntervals() {
        // 5 taps, each separated by 200ms -> 5 taps/sec
        val taps = listOf(1000L, 1200L, 1400L, 1600L, 1800L)
        val speed = Actuator.calculateSpeed(taps)
        assertEquals(5.0f, speed, 0.001f)
    }

    @Test
    fun testCalculateSpeedIgnoresZeroDifferenceTaps() {
        // Simultaneous touches (e.g. 0ms difference) should be ignored without causing Infinity
        val taps = listOf(1000L, 1000L, 1200L)
        val speed = Actuator.calculateSpeed(taps)
        assertTrue("Speed must be finite", speed.isFinite())
        assertEquals(5.0f, speed, 0.001f)
    }

    @Test
    fun testCalculateSpeedAllZeroDifferenceReturnsZero() {
        // If all touches have 0ms interval, result must be 0f (not Infinity or NaN)
        val taps = listOf(1000L, 1000L, 1000L)
        val speed = Actuator.calculateSpeed(taps)
        assertTrue("Speed must be finite", speed.isFinite())
        assertEquals(0f, speed, 0.001f)
    }

    @Test
    fun testCalculateSpeedIgnoresSub50MsJitter() {
        // Intervals < 50ms are considered multi-touch/digitizer jitter
        val taps = listOf(1000L, 1020L, 1040L)
        val speed = Actuator.calculateSpeed(taps)
        assertEquals(0f, speed, 0.001f)
    }

    @Test
    fun testCalculateSpeedIgnoresPausesAbove3000Ms() {
        // Intervals > 3000ms are reading pauses, not active typing
        val taps = listOf(1000L, 5000L, 10000L)
        val speed = Actuator.calculateSpeed(taps)
        assertEquals(0f, speed, 0.001f)
    }

    @Test
    fun testCalculateSpeedMixedRealisticSession() {
        // Taps:
        // - 1000 to 1000: 0ms -> ignored (multi-touch)
        // - 1000 to 1250: 250ms -> valid
        // - 1250 to 1500: 250ms -> valid
        // - 1500 to 7000: 5500ms -> ignored (pause)
        // - 7000 to 7250: 250ms -> valid
        // Total valid intervals: 3 * 250ms = 750ms. Speed = (1000 * 3) / 750 = 4.0 taps/sec
        val taps = listOf(1000L, 1000L, 1250L, 1500L, 7000L, 7250L)
        val speed = Actuator.calculateSpeed(taps)
        assertEquals(4.0f, speed, 0.001f)
    }

    @Test
    fun testCalculateCombinedSpeedWeightedAverage() {
        // Session 1: 50 taps at 2.0f
        // Session 2: 50 taps at 4.0f
        // Combined: (50*2 + 50*4) / 100 = 3.0f
        val speedEqual = Actuator.calculateCombinedSpeed(
            currentSpeed = 2.0f,
            currentTaps = 50,
            newSpeed = 4.0f,
            newTaps = 50
        )
        assertEquals(3.0f, speedEqual, 0.001f)

        // Unequal taps: 20 taps at 2.0f, 80 taps at 4.0f -> (40 + 320) / 100 = 3.6f
        val speedUnequal = Actuator.calculateCombinedSpeed(
            currentSpeed = 2.0f,
            currentTaps = 20,
            newSpeed = 4.0f,
            newTaps = 80
        )
        assertEquals(3.6f, speedUnequal, 0.001f)
    }

    @Test
    fun testCalculateCombinedSpeedHealsCorruptedInfinity() {
        // If currentSpeed in DB was poisoned with Infinity, heal to new valid speed
        val speed = Actuator.calculateCombinedSpeed(
            currentSpeed = Float.POSITIVE_INFINITY,
            currentTaps = 100,
            newSpeed = 3.5f,
            newTaps = 50
        )
        assertTrue("Speed must be finite", speed.isFinite())
        assertEquals(3.5f, speed, 0.001f)
    }

    @Test
    fun testCalculateCombinedSpeedPreservesValidWhenNewIsZero() {
        // If new session has 0 speed (e.g. single tap), keep existing valid speed
        val speed = Actuator.calculateCombinedSpeed(
            currentSpeed = 3.0f,
            currentTaps = 50,
            newSpeed = 0f,
            newTaps = 1
        )
        assertEquals(3.0f, speed, 0.001f)
    }

    @Test
    fun testCalculateCombinedSpeedHandlesBothInvalid() {
        val speed = Actuator.calculateCombinedSpeed(
            currentSpeed = Float.NaN,
            currentTaps = 10,
            newSpeed = Float.POSITIVE_INFINITY,
            newTaps = 10
        )
        assertEquals(0f, speed, 0.001f)
    }
}

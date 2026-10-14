/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.CapabilitiesManager
import com.quantactions.sdk.ManagePref2
import com.quantactions.sdk.QA
import com.quantactions.sdk.ReadingsService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Proxy

/**
 * End-to-End Instrumented Test for data collection in the QuantActions SDK.
 * Verifies the full background ingestion lifecycle:
 * 1. Permissions & Overlay attachment
 * 2. Visual touches enabled & MainActivity launched on screen
 * 3. Screen ON session start
 * 4. UI Automator simulated tap gestures (visible on screen)
 * 5. Physical Screen OFF (display sleeps/locks) triggering Actuator.saveSession() and Room persistence
 * 6. Physical Screen ON (display wakes/unlocks)
 * 7. SDK query via getTapsAndAppsInTimeWindow
 */
@RunWith(AndroidJUnit4::class)
class DataCollectionE2ETest {

    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var qa: QA
    private lateinit var preferences: ManagePref2

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
        context = instrumentation.targetContext

        // Grant required permissions using shell commands via UI Automator
        val packageName = context.packageName
        instrumentation.uiAutomation.executeShellCommand("appops set $packageName SYSTEM_ALERT_WINDOW allow")
        instrumentation.uiAutomation.executeShellCommand("appops set $packageName GET_USAGE_STATS allow")

        // Enable visual touch circles on the screen so simulated taps are visible to the user
        instrumentation.uiAutomation.executeShellCommand("settings put system show_touches 1")

        // Ensure device is awake and keyguard is dismissed before starting
        device.wakeUp()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")

        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        } catch (_: Exception) {}

        // Pre-configure preferences to avoid remote registration network calls during test
        preferences = ManagePref2.getInstance(context)
        preferences.apiKey = "test_api_key"
        preferences.identityId = "test_identity_id"
        preferences.deviceID = "test_device_id"
        preferences.password = "test_password"
        preferences.isDeviceRegistered = true
        preferences.areCredentialsRegistered = true
        preferences.isOauthActivated = true

        // Inject simulated "raw" capability so qa.getTapsAndAppsInTimeWindow is permitted
        injectRawCapability(context)

        // Initialize SDK (with pre-set identityId, it starts the background service without remote call)
        qa = QA.getInstance(context)
        runBlocking {
            qa.init(
                context = context,
                apiKey = "test_api_key",
                basicInfo = BasicInfo(1990, QA.Gender.MALE, true)
            )
        }

        // Ensure ReadingsService is explicitly started
        val serviceIntent = Intent(context, ReadingsService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        SystemClock.sleep(1000)
    }

    @After
    fun tearDown() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Reset show_touches
        instrumentation.uiAutomation.executeShellCommand("settings put system show_touches 0")
        try {
            device.wakeUp()
            instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")
        } catch (_: Exception) {}
    }

    @Test
    fun testDataCollectionAndGetTapsAndAppsInTimeWindow() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // 1. Wake up device, dismiss keyguard, and launch the test app on screen
        device.wakeUp()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 5000)
        }
        SystemClock.sleep(1000)

        // Trigger Screen ON to initialize Actuator session
        val screenOnIntent = Intent(context, ReadingsService::class.java).apply {
            putExtra("screen_state", false)
        }
        context.startService(screenOnIntent)
        SystemClock.sleep(500)

        val startTime = System.currentTimeMillis()

        // 2. Perform visible tap gestures on the screen using UI Automator
        val displayWidth = device.displayWidth
        val displayHeight = device.displayHeight
        val centerX = displayWidth / 2
        val centerY = displayHeight / 2

        val expectedTapCount = 5
        for (i in 0 until expectedTapCount) {
            val x = centerX + (i - 2) * 60
            val y = centerY + (i - 2) * 60
            device.click(x, y)
            SystemClock.sleep(400) // Paced so touches are clearly visible on screen
        }

        val stopTime = System.currentTimeMillis() + 500

        // 3. Physically sleep the screen (screen turns off/black), triggering ACTION_SCREEN_OFF and saveSession()
        device.sleep()
        SystemClock.sleep(1500)

        // Ensure service receives screen_state true
        val screenOffIntent = Intent(context, ReadingsService::class.java).apply {
            putExtra("screen_state", true)
        }
        context.startService(screenOffIntent)

        // 4. Physically wake up the device and unlock the screen
        device.wakeUp()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")
        SystemClock.sleep(1000)

        // 5. Poll database until session is saved (avoiding fixed flaky sleeps)
        var tapsAndApps: QA.TapsAndApps? = null
        val deadline = System.currentTimeMillis() + 6000
        while (System.currentTimeMillis() < deadline) {
            val result = qa.getTapsAndAppsInTimeWindow(startTime - 2000, stopTime + 2000)
            if (result.taps.isNotEmpty()) {
                tapsAndApps = result
                break
            }
            SystemClock.sleep(250)
        }

        // 6. Assertions
        assertNotNull("TapsAndApps result should not be null", tapsAndApps)
        assertTrue("Captured taps list should not be empty", tapsAndApps!!.taps.isNotEmpty())
        
        val recordedTaps = tapsAndApps.taps.sumOf { it.tapsSession }
        assertTrue(
            "Expected at least $expectedTapCount taps to be captured, but found $recordedTaps",
            recordedTaps >= expectedTapCount
        )
    }

    /**
     * Injects "raw" feature capability into CapabilitiesManager using dynamic reflection proxy,
     * enabling getTapsAndAppsInTimeWindow to execute without requiring live RSA token verification.
     */
    private fun injectRawCapability(context: Context) {
        try {
            val decodedJwtClass = Class.forName("com.auth0.jwt.interfaces.DecodedJWT")
            val claimClass = Class.forName("com.auth0.jwt.interfaces.Claim")

            val claimProxy = Proxy.newProxyInstance(
                claimClass.classLoader,
                arrayOf(claimClass)
            ) { _, method, _ ->
                if (method.name == "asList") {
                    listOf("raw", "export")
                } else null
            }

            val jwtProxy = Proxy.newProxyInstance(
                decodedJwtClass.classLoader,
                arrayOf(decodedJwtClass)
            ) { _, method, args ->
                if (method.name == "getClaim" && args?.getOrNull(0) == "features") {
                    claimProxy
                } else null
            }

            val networkManagerClass = Class.forName("com.quantactions.sdk.data.api.NetworkManager")
            val companionField = networkManagerClass.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companion = companionField.get(null)
            val getInstanceMethod = companion.javaClass.getMethod("getInstance", Context::class.java, ManagePref2::class.java, CapabilitiesManager::class.java)
            val preferences = ManagePref2.getInstance(context)
            val defaultCapManager = CapabilitiesManager(context, preferences)
            val networkManager = getInstanceMethod.invoke(companion, context, preferences, defaultCapManager)
            val getCapabilitiesManagerMethod = networkManagerClass.getMethod("getCapabilitiesManager")
            val capabilitiesManager = getCapabilitiesManagerMethod.invoke(networkManager)

            val field = CapabilitiesManager::class.java.getDeclaredField("currentDecodedJWT")
            field.isAccessible = true
            field.set(capabilitiesManager, jwtProxy)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

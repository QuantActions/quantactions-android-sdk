/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.Keep
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.entity.TapDataParsed
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.local.MVPRoomDatabase.Companion.getDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Vector

/**
 * This private class is called from the public ReadingService.class
 * Created by enea on 12/11/16.
 * Contact: enea.ceolini@quantactions.com
 */
internal class Actuator private constructor() {
    private var layerView: CustomTouchView? = null
    private var logs: Vector<EntryLog> = Vector()
    private var startTime: Long = 0
    private lateinit var mvpDao: MVPDao
    private lateinit var usm: UsageStatsManager
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    var added: Boolean = false
    @Volatile
    private var pendingServiceStart: Boolean = false
    @Volatile
    private var pendingServiceIntent: Intent? = null

    @Keep
    companion object {
        @Volatile
        private var INSTANCE: Actuator? = null

        /**
         * Retrieves the default singleton instance of [QA].
         *
         * @param context A Context for on-demand initialization.
         * @return The singleton instance of [QA].
         */
        @Keep
        fun getInstance(context: Context): Actuator {
            val appContext = context.applicationContext
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Actuator()
                    INSTANCE = instance
                    instance.logs = Vector()
                    instance.mvpDao = getDatabase(appContext).mvpDao()
                    instance.usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                    instance.addView(appContext)
                } else {
                    instance.addView(appContext)
                }
                return instance
            }
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                10, 10,  //Arbitrary size
                // Had to switch from TYPE_PHONE to TYPE_TOAST when changing API from 21 to 23, for permission reasons
                // API: 21 -> 23 = TYPE_PHONE -> TYPE_TOAST
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // Prevent the view to get focused
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,  // Listen to the touches
                // that happen outside of its layout limits
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                10, 10,  //Arbitrary size
                // THIS SHOULD BE TYPE_APPLICATION_OVERLAY but needs to be tested!
                // Also this is from API 26+ so it needs to be checked
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // Prevent the view to get focused
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,  // Listen to the touches
                // that happen outside of its layout limits
                PixelFormat.TRANSLUCENT
            )
        }
    }

    private fun addViewOnMainThread(appContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(appContext)) {
            added = false
            return
        }
        val currentView = layerView
        if (added && currentView != null && currentView.isAttachedToWindow) {
            Timber.d("View already added and attached")
            return
        }
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val viewToAdd = currentView ?: CustomTouchView(appContext).also { layerView = it }
        try {
            if (!viewToAdd.isAttachedToWindow) {
                wm.addView(viewToAdd, buildLayoutParams())
            }
            added = true
            Timber.d("Adding view")
            Log.d("Actuator", "Adding view")
        } catch (e: IllegalStateException) {
            // View is already added to WindowManager
            added = true
            Timber.d("View already added in WindowManager")
        } catch (e: Exception) {
            added = false
            Timber.e(e, "Failed to add overlay view")
        }
    }

    fun addView(context: Context) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            addViewOnMainThread(appContext)
        } else {
            mainHandler.post { addViewOnMainThread(appContext) }
        }
    }

    /**
     * Ensures the TYPE_APPLICATION_OVERLAY view is attached and visible before starting ReadingsService.
     * On Android 15+ (API 35+), holding SYSTEM_ALERT_WINDOW only grants a background FGS start exemption
     * when the overlay window is actively attached and View.VISIBLE.
     * - If the screen is ON: attaches the overlay and starts ReadingsService as soon as windowVisibility == View.VISIBLE.
     * - If the screen is OFF: attaches the overlay and arms it so ReadingsService starts the instant the screen wakes.
     */
    fun ensureOverlayAndStartService(
        context: Context,
        intent: Intent = Intent(context.applicationContext, ReadingsService::class.java)
    ) {
        val appContext = context.applicationContext
        pendingServiceIntent = intent
        pendingServiceStart = true

        val action = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(appContext)) {
                pendingServiceStart = false
                pendingServiceIntent = null
                QAPrivate.getInstance(appContext).startForegroundServiceDirect(appContext, intent)
                return@Runnable
            }

            addViewOnMainThread(appContext)

            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenInteractive = pm?.isInteractive ?: true
            val view = layerView

            if (isScreenInteractive && view != null && view.isAttachedToWindow && view.windowVisibility == View.VISIBLE) {
                pendingServiceStart = false
                pendingServiceIntent = null
                QAPrivate.getInstance(appContext).startForegroundServiceDirect(appContext, intent)
            } else if (isScreenInteractive) {
                // View was just added; WindowManager will dispatch onWindowVisibilityChanged(View.VISIBLE) on next frame.
                // Schedule a short fallback check in case visibility is already set without a transition callback.
                mainHandler.postDelayed({
                    if (pendingServiceStart && layerView?.windowVisibility == View.VISIBLE) {
                        val startIntent = pendingServiceIntent ?: Intent(appContext, ReadingsService::class.java)
                        pendingServiceStart = false
                        pendingServiceIntent = null
                        QAPrivate.getInstance(appContext).startForegroundServiceDirect(appContext, startIntent)
                    }
                }, 200L)
            } else {
                Timber.d("Screen is OFF; overlay attached and armed to start ReadingsService on next screen wake")
                Log.i("Actuator", "Screen is OFF; overlay armed to start ReadingsService on next screen wake")
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    fun removeView(context: Context) {
        val appContext = context.applicationContext
        pendingServiceStart = false
        pendingServiceIntent = null
        try {
            val removeAction = Runnable {
                try {
                    val view = layerView
                    if (view != null) {
                        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        if (view.isAttachedToWindow) {
                            wm.removeView(view)
                        }
                        layerView = null
                    }
                    added = false
                    Timber.d("Removing view")
                    Log.d("Actuator", "Removing view")
                } catch (e: Exception) {
                    added = false
                    layerView = null
                    Timber.e(e, "Exception during view removal on main thread")
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                removeAction.run()
            } else {
                mainHandler.post(removeAction)
            }
        } catch (e: Exception) {
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex2: Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
            Timber.e(context.getString(R.string.TAG), "Exception during removal")
        }
    }

    fun getViewVisibility(): Int? {
        return layerView?.windowVisibility
    }

    fun startSession(time: Long) {
        startTime = time
        logs = Vector()
    }

    class AppUsageEvent(var packageName: String, var eventTime: Long)

    @DelicateCoroutinesApi
    suspend fun saveSession(timeStop: Long, context: Context) {

        val tic = Instant.now().toEpochMilli()

        // query usage events from 1 second before start to 30 ms after stop
        val usageEvents: UsageEvents = usm.queryEvents(startTime - 1000, timeStop + 30)

        val recentAppUsages: MutableList<AppUsageEvent> =
            java.util.ArrayList<AppUsageEvent>()

        // go through every event and filter in only the activity resumed event i.e. app becoming
        // visible
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                recentAppUsages.add(
                    AppUsageEvent(
                        event.packageName,
                        event.timeStamp
                    )
                )
            }
        }

        // add the stop event to make sure the taps after the last activity resume event can be
        // assigned correctly
        recentAppUsages.add(AppUsageEvent(
            "NULL",
            timeStop
        ))

        // Sort the list by event time
        recentAppUsages.sortWith { o1, o2 ->
            o1.eventTime.compareTo(o2.eventTime)
        }

        val nEvents = recentAppUsages.size

        var tapsCounter = 0
        val nTaps = logs.size

        for (i in 0 until nEvents - 1) {
            val thisEvent = recentAppUsages[i]
            val nextEventTime = recentAppUsages[i + 1].eventTime

            while (tapsCounter < nTaps && logs[tapsCounter].timeStamp < nextEventTime) {
                logs[tapsCounter].top3[0] = thisEvent.packageName
                tapsCounter++
            }
        }

        //BATTERY
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        // Are we charging / charged?
        var status = -1
        if (batteryStatus != null) {
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        }
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // How are we charging?
        var charging = 0
        if (null != batteryStatus) {
            val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
            if (isCharging && usbCharge) charging = 1
            if (isCharging && acCharge) charging = 2
        }

        try {

            val currentList = mvpDao.getListOfApps().toMutableList()

            val allAppIds0 = ArrayList<Int>()
            val allAppIds1 = ArrayList<Int>()
            val allAppIds2 = ArrayList<Int>()
            val allTaps = ArrayList<Long>()
            val allOrient = ArrayList<Int>()

            // this logs!! is unsafe
            logs.map { entry ->
                val currentCodes = entry.top3.map { app ->
                    val appId = currentList.find { it.appName == app }?.id
                    if (appId != null) {
                        appId
                    } else {
                        val newId = putAppInDB(app)
                        currentList.add(CodeOfApp(newId, app, 0))
                        newId
                    }
                }
                allAppIds0.add(currentCodes[0])
                allAppIds1.add(currentCodes[1])
                allAppIds2.add(currentCodes[2])
                allTaps.add(entry.timeStamp)
                allOrient.add(if (entry.orientation == "P") 1 else 0)
            }

            val tapsSession = allTaps.size

            val startTime2 = if (startTime == 0L) {
                if (allTaps.size > 0) {
                    allTaps[0]
                } else {
                    timeStop
                }
            } else startTime

            mvpDao.insertOrUpdateTapDataParsed(
                TapDataParsed(
                    0,
                    allTaps.toString(),
                    startTime2,
                    timeStop,
                    allOrient.toString(),
                    allAppIds0.toString(),
                    allAppIds1.toString(),
                    allAppIds2.toString(),
                    tapsSession + 0L,
                    if (tapsSession > 1) allTaps.last() - allTaps.first() else 0L,
                    timeZone,
                    "$charging",
                    0
                )
            )

            addTapsDB(allTaps, allTaps.speed())
            logs = Vector()
        } catch (e: Exception) {
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: java.lang.Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
            e.printStackTrace()
        }
        Log.i("ReadingService","Saving session: ${Instant.now().toEpochMilli() - tic} ms")
    }

    private val timeZone: String
        get() {
            val timezone = TimeZone.getDefault()
            return timezone.id
        }

    @SuppressLint("LogNotTimber")
    private fun addTapsDB(allTaps: List<Long>, speed: Float) {
        if (allTaps.isEmpty()) return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Group tap timestamps by their actual local date and hour
        val tapsByHour = allTaps.groupBy { tapTs ->
            calendar.timeInMillis = tapTs
            val dateStr = sdf.format(calendar.time)
            val hourInt = calendar.get(Calendar.HOUR_OF_DAY)
            dateStr to hourInt
        }

        for ((dateHour, tapsInHour) in tapsByHour) {
            val (currentDate, hour) = dateHour
            val effectiveTAPs = tapsInHour.size
            val effectiveSpeed = if (tapsByHour.size == 1) speed else tapsInHour.speed()

            if (effectiveSpeed > 0) Log.i("ReadingService", "Saving session: $effectiveTAPs taps at $currentDate $hour:00")
            else Log.i("ReadingService", "Saving session: $effectiveTAPs taps at $currentDate $hour:00")

            val currentTaps = mvpDao.getTapsInDateAndHour(currentDate, hour)

            if (currentTaps != null) {
                val updatedEntry: HourlyTapsEntity = if (effectiveSpeed > currentTaps.speed) {
                    HourlyTapsEntity(
                        currentTaps.id,
                        currentDate,
                        hour,
                        currentTaps.taps + effectiveTAPs,
                        effectiveSpeed
                    )
                } else {
                    HourlyTapsEntity(
                        currentTaps.id,
                        currentDate,
                        hour,
                        currentTaps.taps + effectiveTAPs,
                        currentTaps.speed
                    )
                }
                mvpDao.insertOrUpdateHourlyTapsEntity(listOf(updatedEntry))
            } else {
                // New hour / day!
                mvpDao.insertOrUpdateHourlyTapsEntity(
                    listOf(HourlyTapsEntity(0, currentDate, hour, effectiveTAPs, effectiveSpeed))
                )
            }
        }
    }

    private fun List<Long>.speed(): Float {
        val diff = mutableListOf<Long>()
        return if (this.size > 1) {
            for (i in 1 .. this.indices.last) {
                diff.add(this[i] - this[i - 1])
            }
            1.0f / (diff.minOrNull() ?: 1)
        } else 0f
    }

    private fun putAppInDB(appName: String): Int {
        // I save in the database the name of all the Apps used. In this way I can save in the file
        // only the code of the app and add the name corresponding to the code to the HEADER of the Data sent
        // This is just a way to compress the recordings data.
        return mvpDao.insertOrUpdateAppCode(listOf(CodeOfApp(0, appName, 0)))[0].toInt()
    }

    private inner class CustomTouchView  // This is the special view that listen for all the taps happening on the screen.
    // Starting from Honey Comb is not possible to get information about position,size,etc of a
    // tap that happened outside of the view itself. This is a security measure to avoid Tap-jacking.
    // that is why we save this information only if the phone is old enough.
        (context: Context?) : View(context) {

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            if (visibility == View.VISIBLE) {
                val appContext = context?.applicationContext ?: return
                if (pendingServiceStart || !ReadingsService.isServiceRunning) {
                    val startIntent = pendingServiceIntent ?: Intent(appContext, ReadingsService::class.java)
                    pendingServiceStart = false
                    pendingServiceIntent = null
                    Timber.d("Overlay window is VISIBLE; starting ReadingsService in foreground")
                    Log.i("Actuator", "Overlay window is VISIBLE; starting ReadingsService in foreground")
                    QAPrivate.getInstance(appContext).startForegroundServiceDirect(appContext, startIntent)
                }
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            added = false
            Timber.d("Overlay view detached from WindowManager")
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            super.onTouchEvent(event)
            val time = Instant.now().toEpochMilli()
            logs.add(
                EntryLog(
                    time, arrayOf("NULL", "NULL", "NULL"), context
                )
            )
            if (!ReadingsService.isServiceRunning) {
                context?.applicationContext?.let { appContext ->
                    pendingServiceStart = false
                    pendingServiceIntent = null
                    QAPrivate.getInstance(appContext).startForegroundServiceDirect(appContext)
                }
            }
            return false // Return false for other touch events
        }
    }

    private inner class EntryLog(
        val timeStamp: Long,
        var top3: Array<String>,
        context: Context
    ) {
        var orientation: String = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "L"
            Configuration.ORIENTATION_PORTRAIT -> "P"
            else -> "-"
        }
    }

}
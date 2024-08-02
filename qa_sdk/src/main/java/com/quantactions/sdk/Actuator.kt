/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.repository.MVPDao
import com.quantactions.sdk.data.repository.MVPRoomDatabase.Companion.getDatabase
import com.quantactions.sdk.data.repository.TapDataParsed
import kotlinx.coroutines.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

/**
 * This private class is called from the public ReadingService.class
 * Created by enea on 12/11/16.
 * Contact: enea.ceolini@quantactions.com
 */
internal class Actuator(service: ReadingsService) {
    private var layerView: CustomTouchView? = null
    private val context: Context = service
    private var logs: Vector<EntryLog> = Vector()
    private var startTime: Long = 0
    private val mvpDao: MVPDao

    fun addView() {
        val wParams: WindowManager.LayoutParams =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layerView = CustomTouchView(context)

        // Add layout to window manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                wm.addView(layerView, wParams)
                (context as ReadingsService).added = true
                Timber.i("Adding view")
            } else {
                (context as ReadingsService).added = false
            }
        } else {
            wm.addView(layerView, wParams)
            (context as ReadingsService).added = true
            Timber.i("Adding view")
        }
    }

    fun removeView() {
        try {
            if (null != layerView) {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(layerView)
                (context as ReadingsService).added = false
                Timber.i("Removing view")
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

    fun startSession(time: Long) {
        startTime = time
        logs = Vector()
    }

    @DelicateCoroutinesApi
    suspend fun saveSession(timeStop: Long) {

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

            addTapsDB(allTaps.size, allTaps.speed())
            logs = Vector()
        } catch (e: Exception) {
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: java.lang.Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
            e.printStackTrace()
        }
    }

    private val timeZone: String
        get() {
            val timezone = TimeZone.getDefault()
            return timezone.id
        }

    @SuppressLint("LogNotTimber")
    private fun addTapsDB(effectiveTAPs: Int, speed: Float) {
        // Adding count of taps per day every
        // get day from last time step
        val calendar = Calendar.getInstance()
        val date = calendar.time
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = sdf.format(date)

        // there is something we should update
        // check speed
        if (speed > 0) Log.i("ReadingService","Saving session: $effectiveTAPs taps")
        else Log.i("ReadingService","Saving session: $effectiveTAPs taps")

        val currentTaps = mvpDao.getTapsInDateAndHour(currentDate, hour)

        if (currentTaps != null) {

            val updatedEntry: HourlyTapsEntity = if (speed > currentTaps.speed) {
                HourlyTapsEntity(
                    currentTaps.id,
                    currentDate,
                    hour,
                    currentTaps.taps + effectiveTAPs,
                    speed
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
            // New day!
            mvpDao.insertOrUpdateHourlyTapsEntity(
                listOf(HourlyTapsEntity(0, currentDate, hour, effectiveTAPs, speed))
            )
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

    // HERE I collected only 2 columns so they are ordered

    private fun printTop3Task(): Array<String> {
        val currentApps = arrayOf("NULL", "NULL", "NULL")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = Instant.now().toEpochMilli()
            val appList =
                usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 86400000, time)

            if (appList != null && appList.size > 0) {
                val sortedList = appList.sortedBy { it.lastTimeUsed }.reversed()
                if (sortedList.isNotEmpty()) currentApps[0] = sortedList[0].packageName
                if (sortedList.size > 1) currentApps[1] = sortedList[1].packageName
                if (sortedList.size > 2) currentApps[2] = sortedList[2].packageName
                return currentApps
            }
        } else {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks: List<RunningAppProcessInfo> = am.runningAppProcesses
            if (tasks.isNotEmpty()) currentApps[0] = tasks[0].processName
            if (tasks.size > 1) currentApps[1] = tasks[1].processName
            if (tasks.size > 2) currentApps[2] = tasks[2].processName
        }
        return currentApps
    }

    private inner class CustomTouchView  // This is the special view that listen for all the taps happening on the screen.
    // Starting from Honey Comb is not possible to get information about position,size,etc of a
    // tap that happened outside of the view itself. This is a security measure to avoid Tap-jacking.
    // that is why we save this information only if the phone is old enough.
        (context: Context?) : View(context) {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            super.onTouchEvent(event)
            logs.add(
                EntryLog(
                    Instant.now().toEpochMilli(), printTop3Task()
                )
            )
            return false // Return false for other touch events
        }
    }

    private inner class EntryLog(
        val timeStamp: Long,
        var top3: Array<String>
    ) {
        var orientation: String = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "L"
            Configuration.ORIENTATION_PORTRAIT -> "P"
            else -> "-"
        }
    }

    init {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        logs = Vector()

        // This is the what allows us to detect TAPS on the screen in every moment.
        // We create a View which is Transparent, in the middle of the screen, of size 10X10 pixels
        // which detects touches but does not disturb the user
        addView()
        mvpDao = getDatabase(context).mvpDao()
    }
}
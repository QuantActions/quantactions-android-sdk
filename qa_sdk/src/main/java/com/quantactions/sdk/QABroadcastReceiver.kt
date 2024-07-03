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

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.work.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.quantactions.sdk.data.repository.DeviceHealthParsed
import com.quantactions.sdk.data.repository.MVPDao
import com.quantactions.sdk.data.repository.MVPRoomDatabase.Companion.getDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * General broadcast receiver for SDK actions.
 * Created by Enea Ceolini on 11/16.
 * Contact: enea.ceolini@quantactions.com
 * @hide
 */
class QABroadcastReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
//    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        val mvpDao = getDatabase(context).mvpDao()
        val qaPrivate = QAPrivate.getInstance(context)
        val batteryLevel = getBatteryPercentage(context)

        val i: Intent
        if (null != intent.action) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> Toast.makeText(
                    context,
                    "The device is charging",
                    Toast.LENGTH_SHORT
                ).show()
                Intent.ACTION_SHUTDOWN -> try {
                    insertHealthRow(mvpDao, Instant.now(), batteryLevel, "SH")
                } catch (e: Exception) {
                    try {
                        FirebaseCrashlytics.getInstance().recordException(e)
                    } catch (ex: Exception) {
                        Timber.e("App does not integrate Firebase, cannot send crash!")
                    }
                    e.printStackTrace()
                }
                "YouwillNeverKillMe" -> qaPrivate.makeServiceForeground(context)
                Intent.ACTION_SCREEN_ON -> {
                    i = Intent(context, ReadingsService::class.java)
                    i.putExtra("screen_state", false)
                    qaPrivate.makeServiceForeground(context, i)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    i = Intent(context, ReadingsService::class.java)
                    i.putExtra("screen_state", true)
                    qaPrivate.makeServiceForeground(context, i)
                }
                Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    Timber.tag("BroadCastReceiver").i("NEW ___ SETTING UP 1 MINUTE __ NEW")

                    // delay 1 minute start service to make sure
                    val delayRequest = OneTimeWorkRequest.Builder(ServiceStarter::class.java)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                        .addTag("startLater")
                        .build()
                    WorkManager.getInstance(context).enqueue(delayRequest)

                    val databaseHelper = DatabaseHelper.getInstance(context)

                    qaPrivate.executeOldToNewDBMigration(databaseHelper, scope)

                    qaPrivate.executeOldToNewAPIMigration(scope)

                    val constraints = Constraints.Builder()
                        .build()

                    val syncRequest = PeriodicWorkRequest.Builder(
                        SyncWorker::class.java, 1, TimeUnit.HOURS
                    )
                        .setConstraints(constraints)
                        .addTag(context.packageName + ":com.quantactions.sdk.SyncWorker")
                        .setInitialDelay(5, TimeUnit.MINUTES)
                        .build()

                    val relaunchRequest = PeriodicWorkRequest.Builder(
                        RelaunchWorker::class.java, 15, TimeUnit.MINUTES
                    )
                        .setConstraints(constraints)
                        .addTag(context.packageName + ":com.quantactions.sdk.RelaunchWorker")
                        .setInitialDelay(2, TimeUnit.MINUTES)
                        .build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        context.packageName + ":com.quantactions.sdk.SyncWorker",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        syncRequest
                    )
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        context.packageName + ":com.quantactions.sdk.RelaunchWorker",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        relaunchRequest
                    )
                    setAlarm(context)
                    if (intent.action == "android.intent.action.BOOT_COMPLETED") {
                        insertHealthRow(mvpDao, Instant.now(), batteryLevel, "RB")
                    } else {
                        insertHealthRow(mvpDao, Instant.now(), batteryLevel, "UP")
                    }
                }

                QA_ALARM_ACTION -> fireAlarm(context, mvpDao, batteryLevel, qaPrivate)

                "pauseCollection" -> {
                    Timber.d("Signal to pause received")
                    val newIntent = Intent(context, ReadingsService::class.java)
                    newIntent.putExtra("pauseSignal", true)
                    qaPrivate.makeServiceForeground(context, newIntent)
                }
                "resumeCollection" -> qaPrivate.makeServiceForeground(context)
                else -> {}
            }
        }
    }

    private fun fireAlarm(
        context: Context,
        mvpDao: MVPDao,
        batteryLevel: Int,
        qaPrivate: QAPrivate
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    if (isMyServiceRunning(ReadingsService::class.java, context)) {
                        qaPrivate.makeServiceForeground(context)
                        insertHealthRow(mvpDao, Instant.now(), batteryLevel, "RS")
                    } else {
                        qaPrivate.makeServiceForeground(context)
                        insertHealthRow(mvpDao, Instant.now(), batteryLevel, "OK")
                    }
                } else {
                    qaPrivate.makeServiceForeground(context)
                    insertHealthRow(mvpDao, Instant.now(), batteryLevel, "OK")
                }
            } else {
                if (isMyServiceRunning(ReadingsService::class.java, context)) {
                    qaPrivate.makeServiceForeground(context)
                    insertHealthRow(mvpDao, Instant.now(), batteryLevel, "RS")
                } else {
                    qaPrivate.makeServiceForeground(context)
                    insertHealthRow(mvpDao, Instant.now(), batteryLevel, "OK")
                }
            }
        } catch (e: Exception) {
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: Exception) {
                Timber.e("App does not integrate Firebase, cannot send crash!")
            }
            e.printStackTrace()
        }
    }

    fun setAlarm(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QABroadcastReceiver::class.java)
        intent.action = QA_ALARM_ACTION
        val alarmIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val calendar = Calendar.getInstance()
        // Set the alarm's trigger time to 16:00 a.m.
        if (Instant.now().toEpochMilli() > calendar.timeInMillis) calendar.add(
            Calendar.DAY_OF_YEAR,
            1
        )

        alarmMgr.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 600000,
            600000, alarmIntent
        )
        val receiver = ComponentName(context, QABroadcastReceiver::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    companion object {
        private const val QA_ALARM_ACTION = "com.quantactions.sdk.ALARM"
        fun getBatteryPercentage(context: Context): Int {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, iFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level / scale.toFloat()
            return (batteryPct * 100).toInt()
        }

        fun isMyServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return false
                }
            }
            return true
        }
    }

    private fun insertHealthRow(
        mvpDao: MVPDao,
        timestamp: Instant,
        batteryLevel: Int,
        event: String
    ) {
        scope.launch {
            mvpDao.insertOrUpdateDeviceHealthParsed(
                DeviceHealthParsed(
                    0, "${timestamp.toEpochMilli()}",
                    "$batteryLevel", event,
                    timestamp.toEpochMilli(), timestamp.toEpochMilli(), 0
                )
            )
        }
    }

}
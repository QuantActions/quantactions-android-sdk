/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.quantactions.sdk.QA.Companion.getInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Arrays

/**
 * Background service (forced to be foreground for SDK >= Android O) that allows QA to function
 * properly. Receives notifications of screen on / screen off to start / save current tap session.
 * @hide
 */
class ReadingsService : Service() {
    private val mBinder: IBinder = LocalBinder()
    private var mReceiver: BroadcastReceiver? = null
    private lateinit var actuator: Actuator
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {

        // Register the filter to be told when screen goes on and off
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        mReceiver = QABroadcastReceiver()
        registerReceiver(mReceiver, filter)
        actuator = Actuator.getInstance(this@ReadingsService)
        getInstance(this@ReadingsService).updater.updateNotification()

        // Necessary for handling foreground task
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, ReadingsService::class.java)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel()
            // This is necessary for handleMessage error on Samsung, does not really happen to other phones
            startForeground(
                QAStrings.QA_FOREGROUND_SERVICE_ID, getInstance(this@ReadingsService).updater.createNotification(
                    applicationContext, channelId
                )
            )
        }

        val result: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        if (result == PackageManager.PERMISSION_GRANTED) {
            setupActivityRecognition()
        }


    }

    private fun setupActivityRecognition() {
        // ACTION RECON

        val transitionList = ArrayList<ActivityTransition>()
        val activities: ArrayList<Int> = ArrayList(
            Arrays.asList(
                DetectedActivity.STILL,
                DetectedActivity.WALKING,
                DetectedActivity.ON_FOOT,
                DetectedActivity.RUNNING,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.IN_VEHICLE
            )
        )
        for (activity in activities) {
            transitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build()
            )
            transitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
            )
        }

        val request = ActivityTransitionRequest(transitionList)

        val intent = Intent(this, DetectedActivityReceiver::class.java)
        intent.action = DetectedActivityReceiver.INTENT_ACTION

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )

        val task = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        } else {
            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, pendingIntent)
        }
        ActivityRecognition.getClient(this)
            .requestActivityTransitionUpdates(request, pendingIntent)

        task.addOnSuccessListener {
            Timber.d("SUCCESS")
            Timber.d("Transitions Api registered with success")
        }
        /*
        task.addOnFailureListener() {
            Log.d("REQUEST", it.toString())
        }
        */
        task.addOnFailureListener { e: Exception ->
            Log.d(
                "DetectedActivityReceivr",
                "Transitions Api could NOT be registered ${e.localizedMessage}"
            )
        }

        // END
    }

    /**
     * Creates a notification channel, this is necessary from Android O on.
     *
     * @return String defining the channel ID.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = getString(R.string.notification_channel_id_qa)
        val name: CharSequence = getString(R.string.notification_channel_name_qa)
        val description = getString(R.string.notification_channel_desc_qa)
        val chan = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_NONE)
        chan.description = description
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    /**
     * This is triggered when service starts but also when the screen goes on and off.
     *
     * @param intent  That started the service
     * @param flags   additional flags
     * @param startId additional Id
     * @return status
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        getInstance(this@ReadingsService).updater.updateNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel()
            // This is necessary for handleMessage error on Samsung, does not really happen to other phones
            startForeground(
                QAStrings.QA_FOREGROUND_SERVICE_ID, getInstance(this@ReadingsService).updater.createNotification(
                    applicationContext,
                    channelId,
//                    intent?.hasExtra("pauseSignal") ?: false
                )
            )
        }

        // Check if is added view
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(applicationContext)) addView()
            //This function is launched when an On/Off event is triggered
        }

        if (null != intent) {
            val screenOff: Boolean
            if (intent.hasExtra("screen_state")) {
                screenOff = intent.getBooleanExtra("screen_state", false)
                if (!screenOff) { // If screen is on I register the accelerometer that start recording
                    startSession(System.currentTimeMillis())
                } else { // otherwise (if the screen goes off) I analyze the data recorded in the last session
                    scope.launch {
                        actuator.saveSession(System.currentTimeMillis(), this@ReadingsService)
                    }
                }
            }

            if (intent.hasExtra("killSignal")) {
                stopMe()
                removeView()
            }
            if (intent.hasExtra("pauseSignal")) {
                Timber.d("Pause Signal received")
                pauseMe()
                removeView()
            }
            return START_STICKY
        } else {
            return START_STICKY_COMPATIBILITY
        }
    }

    /**
     * Adding the tap collector view to the screen
     */
    private fun addView() {
        if (!actuator.added) {
            actuator.addView(this@ReadingsService)
        }
    }

    /**
     * Removing the view from the screen, this is necessary in some cases
     */
    private fun removeView() {
        actuator.removeView(this@ReadingsService)
    }

    override fun onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
        }
        sendBroadcast(Intent("YouWillNeverKillMe"))
        super.onDestroy()
    }

    /**
     * This is deprecated from Android O on.
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        sendBroadcast(Intent("YouWillNeverKillMe"))
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    /**
     * Start recording taps.
     *
     * @param time when the screen was turned on.
     */
    private fun startSession(time: Long) {
        actuator.startSession(time)
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: ReadingsService
            get() =// Return this instance of LocalService so clients can call public methods
                this@ReadingsService
    }

    private fun stopMe() {
        Timber.w("QA service is being asked to shut down!")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun pauseMe() {
        Timber.w("QA service is being asked to pause!")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(false)
        }
        val channelId = getString(R.string.notification_channel_id_qa)
        getInstance(this@ReadingsService).updater.createNotification(
            applicationContext, channelId

        )
    }
}
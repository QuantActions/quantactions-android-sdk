/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.app.*
import android.content.*
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.quantactions.sdk.QA.Companion.getInstance
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

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
    var added = false

    override fun onCreate() {

        // Register the filter to be told when screen goes on and off
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        mReceiver = QABroadcastReceiver()
        registerReceiver(mReceiver, filter)
        actuator = Actuator(this@ReadingsService)
        val preferences = ManagePref2.getInstance(this@ReadingsService)

        // Necessary for handling foreground task
        ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, ReadingsService::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel()
            // This is necessary for handleMessage error on Samsung, does not really happen to other phones
            startForeground(
                QAStrings.QA_FOREGROUND_SERVICE_ID, createNotification(
                    applicationContext, channelId, false
                )
            )
        }

        // Not necessary but for precaution we recall
//        try {
//            getInstance(applicationContext).init(
//                applicationContext, preferences.apiKey, preferences.yearOfBirth, preferences.gender, preferences.selfDeclaredHealthy
//            )
//        } catch (e: SDKNotInitialisedException) {
//            FirebaseCrashlytics.getInstance().recordException(e)
//            Timber.e("NOT STARTING QA NOT INIT")
//        }
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
        val chan = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_MIN)
        chan.description = description
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    /**
     * Creates the notification body **important** icon is necessary otherwise the systems puts its
     * own notification and does not show our text.
     *
     * @param context   Android application context
     * @param channelID Name on the notification channel in which publish the notification
     * @return Notification channel
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createNotification(
        context: Context,
        channelID: String,
        showResume: Boolean
    ): Notification {

        val pauseIntent = Intent(context, QABroadcastReceiver::class.java)
        pauseIntent.action = "pauseCollection"

//        val pausePendingIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent(context, QABroadcastReceiver::class.java)
        resumeIntent.action = "resumeCollection"

//        val resumePendingIntent = PendingIntent.getBroadcast(this, 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val mBuilder = NotificationCompat.Builder(context, channelID)
        mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        mBuilder.setSmallIcon(R.drawable.ic_equalizer_black_24dp)
        mBuilder.color = context.resources.getColor(R.color.brand_background_icon_color)
        mBuilder.setWhen(0)
        mBuilder.setOngoing(true)
//        if (showResume) {
//            mBuilder.addAction(R.drawable.ic_debug_foreground, "Resume", resumePendingIntent)
//        } else {
//            mBuilder.addAction(R.drawable.ic_debug_foreground, "Pause", pausePendingIntent)
//        }
        return mBuilder.build()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel()
            // This is necessary for handleMessage error on Samsung, does not really happen to other phones
            startForeground(
                QAStrings.QA_FOREGROUND_SERVICE_ID, createNotification(
                    applicationContext,
                    channelId,
                    intent?.hasExtra("pauseSignal") ?: false
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
                        actuator.saveSession(System.currentTimeMillis())
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
        if (!added) {
            actuator.addView()
            added = true
        }
    }

    /**
     * Removing the view from the screen, this is necessary in some cases
     */
    private fun removeView() {
        actuator.removeView()
        added = false
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
        createNotification(
            applicationContext, channelId, true
        )
    }
}
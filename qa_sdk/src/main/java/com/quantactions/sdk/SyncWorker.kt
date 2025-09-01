/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

//import com.quantactions.sdk.QA.Companion.getInstance
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * @hide
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val isInit: Boolean
    private val isDebug: Boolean
    private val syncHelper: SyncHelper
    private var mNotificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private val qaPrivate: QAPrivate
    private val preferences: ManagePref2
    private val databaseHelper: DatabaseHelper
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    init {
        qaPrivate = QAPrivate.getInstance(context)
        preferences = ManagePref2.getInstance(context)
        databaseHelper = DatabaseHelper.getInstance(context)
        isInit = qaPrivate.isDeviceRegistered()
        isDebug = preferences.getDebugMode()
        syncHelper = SyncHelper(context)

        // more
        if (isDebug) {
            mNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = context.getString(R.string.notification_channel_id_qa)

            // Android O requires a Notification Channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name: CharSequence = context.getString(R.string.notification_channel_name_qa)
                val chan = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_MIN)
                mNotificationManager!!.createNotificationChannel(chan)
            }
            notification = getNotification(context, channelId)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            doSync()
        } catch (e: SDKNotInitialisedException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            e.printStackTrace()
        }
        if (isDebug) {
            mNotificationManager!!.notify(NOTIFICATION_ID, notification)
        }
        Result.success()
    }

    @Throws(SDKNotInitialisedException::class)
    private suspend fun doSync() {
        if (isInit) {
            syncHelper.syncAll()
        } else {
            // here instead of throwing I will try to recover the device in case something was wrong
            qaPrivate.executeOldToNewDBMigration(
                databaseHelper,
                scope)
//            throw SDKNotInitialisedException()
        }
    }

    /**
     * Returns the [NotificationCompat] used as part of the foreground service.
     */
    private fun getNotification(context: Context, channelID: String): Notification {

        // Set the Channel ID for Android O.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = NotificationCompat.Builder(context, channelID)
                .setContentTitle(
                    "Data Uploaded: " + DateFormat.getDateTimeInstance()
                        .format(Date())
                )
                .setSmallIcon(R.drawable.ic_debug_foreground)
                .setVibrate(longArrayOf(0L))
                .setWhen(System.currentTimeMillis())
            builder.setChannelId(channelID) // Channel ID
            builder.build()
        } else {
            @Suppress("DEPRECATION") val notificationBuilder = NotificationCompat.Builder(applicationContext)
                .setContentTitle(
                    "Data Uploaded: " + DateFormat.getDateTimeInstance()
                        .format(Date())
                )
                .setSmallIcon(R.mipmap.ic_debug)
                .setVibrate(longArrayOf(0L))
                .setWhen(System.currentTimeMillis())
            notificationBuilder.build()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 77777
    }
}
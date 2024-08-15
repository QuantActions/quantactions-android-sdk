/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import android.content.*
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

import com.quantactions.sdk.data.entity.ActivityTransitionEntity
import com.quantactions.sdk.data.repository.MVPDao
import com.quantactions.sdk.data.repository.MVPRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import kotlin.coroutines.CoroutineContext

class DetectedActivityReceiver : BroadcastReceiver(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var mapDao: MVPDao
//    private var mService: LocationUpdatesService? = null

    // Tracks the bound state of the service.
    private var mBound = false


//    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName, service: IBinder) {
//            val binder = service as LocationUpdatesService.LocalBinder
//            mService = binder.service
//            mBound = true
//        }
//
//        override fun onServiceDisconnected(name: ComponentName) {
//            mService = null
//            mBound = false
//        }
//    }

    override fun onReceive(context: Context?, intent: Intent) {

        if (context != null) {
            Timber.d("context not null")
            mapDao = MVPRoomDatabase.getDatabase(context).mvpDao()


            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                for (event in result!!.transitionEvents) {
                    Timber.d("ActivityTransitionResult.hasResult")
                    val activity = activityType(event.activityType)
                    val transition = transitionType(event.transitionType)
                    val message = "Transition: $activity ($transition)"
                    Timber.d(message)

                    val action = ActivityTransitionEntity(
                        0, Instant.now().toEpochMilli(),
                        activityType(event.activityType),
                        event.transitionType, 0)

                    launch(Dispatchers.IO) { mapDao.insertOrUpdateActivityTransition(action)
                        Timber.i("Action: $action")
                    }

                }

//                Toast.makeText(context, message, LENGTH_LONG).show()

//                if (context != null) {
//                    // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
//                    val builder =
//                        NotificationCompat.Builder(context, "qa_channel_01")
//                            .setSmallIcon(R.drawable.ic_equalizer_black_24dp)
//                            .setContentText("Transition: $activity ($transition)")
//                            .setVibrate(longArrayOf(0L))
//                            .setWhen(System.currentTimeMillis())
//
//                    // Set the Channel ID for Android O.
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        builder.setChannelId("qa_channel_01") // Channel ID
//                    }
//                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
//                        Random().nextInt(100) + 1, builder.build())
            } else {
                Timber.d("ActivityTransitionResult NO Result")
            }
        } else {
            Timber.d("context NULL")
        }
    }


    private fun transitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }

    private fun activityType(activity: Int): String {
        return when (activity) {
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.ON_FOOT -> "ON_FOOT"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            else -> "UNKNOWN"
        }
    }

    companion object {
        const val INTENT_ACTION = "com.mypackage.ACTION_PROCESS_ACTIVITY_TRANSITIONS"
    }
}
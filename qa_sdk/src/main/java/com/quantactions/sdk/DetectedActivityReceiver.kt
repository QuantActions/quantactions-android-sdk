/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

/**
 * BroadcastReceiver to handle activity transition updates.
 * It receives the activity transition events and logs them.
 * It also saves the events to the local database.
 * @Suppress("Dokka")
 */
class DetectedActivityReceiver : BroadcastReceiver(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var mapDao: MVPDao

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
                        Timber.d("Action: $action")
                    }

                }
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
        const val INTENT_ACTION = "com.quantactions.sdk.ACTION_PROCESS_ACTIVITY_TRANSITIONS"
    }
}
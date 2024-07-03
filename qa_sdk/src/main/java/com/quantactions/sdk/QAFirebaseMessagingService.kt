/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.os.Handler
import android.os.Looper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.exceptions.SDKNotInitialisedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * This basic service handles Firebase messaging, thus it handles what happens when a notification
 * is received from the cloud (from tap admin). This service simply handles the basic
 * functionality provided by the web interface that is forcing the sync of data. You can easily
 * create your own responses to custom cloud notification by extending this service and adding
 * it to your application.
 * @suppress
 * Created by Enea Ceolini on 11/02/17.
 * Contact: enea.ceolini@quantactions.com
 */
open class QAFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        setFirebaseToken(token)
    }

    /**
     * When a new firebase token is received (refresh token) we save it.
     * @param newFirebaseToken self explanatory.
     */
    private fun setFirebaseToken(newFirebaseToken: String) {
        val preferences = ManagePref2.getInstance(applicationContext)
        preferences.setFBCode(newFirebaseToken)
    }

    /**
     * Called when message is received.
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val ioScope = CoroutineScope(Dispatchers.IO + Job())
        val qa = QAPrivate.getInstance(applicationContext)
        if (remoteMessage.data.isNotEmpty()) {
            Timber.i(getString(R.string.TAG), "Message data payload: " + remoteMessage.data)
            val data: Map<*, *> = remoteMessage.data
            val handler = Handler(Looper.getMainLooper())
            try {
                val j = JSONObject(data["payload"] as String)
                if (j.has("code")) {
                    when (j.getString("code")) {
                        "sync" -> ioScope.launch { qa.syncData(applicationContext) }
                        "init" -> ioScope.launch { qa.init(applicationContext, j.getString("apiKey")) }
                        "relaunch" -> qa.makeServiceForeground(applicationContext)
                        "pcheck" -> {
                            if (!qa.canDraw(applicationContext)) sendNotification(
                                "Please provide the permission",
                                QAStrings.NOTIFY_DRAW
                            )
                            if (!qa.canUsage(applicationContext)) sendNotification(
                                "Please provide the permission",
                                QAStrings.NOTIFY_USAGE
                            )
                        }
                        "signup" -> handler.post {
                            try {
                                ioScope.launch {
                                    qa.signUpForStudy(
                                        subscriptionId = j.getString("partId"),
                                    )
                                }
                                
                            } catch (e: SDKNotInitialisedException) {
                                e.printStackTrace()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        "withd" -> handler.post {
                            try {
                                ioScope.launch {
                                    qa.leaveStudy(
                                        j.getString("participationId"),
                                        j.getString("studyId")
                                    )
                                }
                                
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        "update" -> {
                            sendNotification(
                                "TapCounter has a newer version! Please update the App in the PlayStore in order to have all the latest features!",
                                QAStrings.NOTIFY_UPDATE
                            )
                            Timber.i(getString(R.string.TAG), "Ask for update: SUCCESS!")
                        }
                        "fillQuest" -> sendNotification(
                            j.getString("message"),
                            QAStrings.NOTIFY_QUESTIONNAIRE
                        )
                        "verbose-on" -> {
                            Timber.d("REMOTE OPTS", "VERBOSE 1000")
//                            qa.setVerboseLevel(applicationContext, QAStrings.VERBOSE_HIGH)
//                            qa.log(applicationContext, "Turning on HIGH verbose")
                        }
                        "verbose-off" -> {
                            Timber.d("REMOTE OPTS", "VERBOSE 1")
//                            qa.setVerboseLevel(applicationContext, QAStrings.VERBOSE_LOW)
//                            qa.log(applicationContext, "Turning off HIGH verbose")
                        }
                        else -> sendNotification(j.getString("message"), QAStrings.NOTIFY_VANILLA)
                    }
                } else {
                    sendNotification(j.getString("message"), QAStrings.NOTIFY_VANILLA)
                }
            } catch (e: Exception) {
                try {
                    FirebaseCrashlytics.getInstance().recordException(e);
                } catch (ex: Exception) {
                    Timber.e("App does not integrate Firebase, cannot send crash!");
                }
                e.printStackTrace()
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    open fun sendNotification(messageBody: String, code: Int) {
        if (ManagePref2.getInstance(applicationContext)
                .getVerbose() > 0
        ) Timber.i(getString(R.string.TAG), "Received: $messageBody")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
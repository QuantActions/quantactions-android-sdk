/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk

/**
 * Created by enea on 30/01/17.
 * Contact: enea.ceolini@quantactions.com
 */

import android.app.AppOpsManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.quantactions.sdk.data.repository.MVPRepository
import com.quantactions.sdk.workers.PushPendingCognitiveTestsWorker
import com.quantactions.sdk.workers.PushPendingJournalEntriesWorker
import com.quantactions.sdk.workers.PushPendingQuestionnairesWorker
import com.quantactions.sdk.workers.RegisterWorker
import com.quantactions.sdk.workers.SubmitActivityWorker
import com.quantactions.sdk.workers.SubmitHealthDataParsedWorker
import com.quantactions.sdk.workers.SubmitStatsWorker
import com.quantactions.sdk.workers.SubmitTapDataParsedWorker
import com.quantactions.sdk.workers.UpdateAppsListWorker
import com.quantactions.sdk.workers.UpdateDeviceWorker
import timber.log.Timber

internal class SyncHelper(context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private var pendingSignUp: String? = null
    private var pushPermissions = true
    private var pushFirebaseToken = false
    private var appOps: AppOpsManager? = null
    private val managePref = ManagePref2.getInstance(context)
    private val statusNow = managePref.getPermissionsStatus(context)
    private val repository = MVPRepository.getInstance(context)
    private var pushStats = true

    init {
        appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        pendingSignUp = managePref.getPendingSignUp()
        pushStats = managePref.getSavedDate() != ManagePref2.getInstance(context).date
        if (pushStats) managePref.setSavedDate()
        val statusSaved = managePref.getSavedPermissions()
        pushPermissions = statusNow != statusSaved
        pushFirebaseToken = true // managePref.getPendingFirebaseToken()
    }

    suspend fun syncAll(): Boolean {
        // I could check everything here and update if necessary


        if (managePref.deviceID != "") {

            // always update device info (can catch a new android version)
            workManager.enqueueUniqueWork(
                "updateDevice",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(UpdateDeviceWorker::class.java)
                    .addTag("updateDevice")
                    .build()
            )

            // push taps stats
            if (pushStats) {

                workManager.enqueueUniqueWork(
                    "submitStats",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(SubmitStatsWorker::class.java)
                        .addTag("submitStats")
                        .build()
                )
            }

            // pending quest
            val pendingQuestionnaires = repository.getQuestionnaireResponses()
            if (pendingQuestionnaires.isNotEmpty()) {
                workManager.enqueueUniqueWork(
                    "submitPendingQuest",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(PushPendingQuestionnairesWorker::class.java)
                        .addTag("submitPendingQuest")
                        .build()
                )
            } else {
                Timber.i("No pending questionnaires -> not running")
            }

            // pending cog test
            val pendingCognitiveTest = repository.getPendingCognitiveTests()
            if (pendingCognitiveTest.isNotEmpty()) {
                workManager.enqueueUniqueWork(
                    "submitPendingCogTest",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(PushPendingCognitiveTestsWorker::class.java)
                        .addTag("submitPendingCogTest")
                        .build()
                )
            } else {
                Timber.i("No pending cog tests -> not running")
            }

            // pending journal entries
            val pendingEntries = repository.getPendingJournalEntries()
            val pendingEntriesToDelete = repository.getPendingJournalEntriesToDelete()
            if (pendingEntries.isNotEmpty() or pendingEntriesToDelete.isNotEmpty()) {
                workManager.enqueueUniqueWork(
                    "submitPendingJournal",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(PushPendingJournalEntriesWorker::class.java)
                        .addTag("submitPendingJournal")
                        .build()
                )
            } else {
                Timber.i("No pending journal entries -> not running")
            }

            // update list of apps
            updateAppList()

            if (repository.getTapDataParsedToSync().isNotEmpty()) {
                workManager.enqueueUniqueWork(
                    "submitTapDataParsed",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(SubmitTapDataParsedWorker::class.java)
                        .addTag("submitTapDataParsed")
                        .build()
                )
            }

            if (repository.getDeviceHealthParsedToSync().isNotEmpty()) {
                workManager.enqueueUniqueWork(
                    "submitHealthDataParsed",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(SubmitHealthDataParsedWorker::class.java)
                        .addTag("submitHealthDataParsed")
                        .build()
                )
            }

            if (repository.getActivityToSync().isNotEmpty()) {
                workManager.enqueueUniqueWork(
                    "activityRecognition",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(SubmitActivityWorker::class.java)
                        .addTag("activityRecognition")
                        .build()
                )
            }

        } else { // if not we still sync the pending syncs
            Timber.w("ID is not present I will try to register the user unless the task is already there")
            workManager.enqueueUniqueWork(
                "registerUser",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(RegisterWorker::class.java)
                    .addTag("registerUser")
                    .build()
            )
            return false
        }
        return true
    }

    private fun updateAppList() {
        workManager.enqueueUniqueWork(
            "updateAppsList",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.Builder(UpdateAppsListWorker::class.java)
                .addTag("updateAppsList")
                .build()
        )
    }
}

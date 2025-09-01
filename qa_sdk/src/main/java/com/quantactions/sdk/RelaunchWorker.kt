/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.quantactions.sdk.QAPrivate.Companion.getInstance

/**
 * @suppress
 */
class RelaunchWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val qa: QAPrivate = getInstance(context)
    private val actuator = Actuator.getInstance(context)

    override fun doWork(): Result {
        Log.i("RelaunchWorker", "Relaunching service: ${actuator.getViewVisibility()}")
        actuator.addView(applicationContext)
        qa.makeServiceForeground(applicationContext)
        return Result.success()
    }
}
/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */
package com.quantactions.sdk

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.quantactions.sdk.QAPrivate.Companion.getInstance

/**
 * @suppress
 */
class ServiceStarter(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        getInstance(applicationContext).makeServiceForeground(applicationContext)
        return Result.success()
    }
}
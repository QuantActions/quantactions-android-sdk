/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import android.os.Bundle
import android.os.StrictMode
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.quantactions.sdk.BasicInfo
import com.quantactions.sdk.QA
import com.quantactions.sdktestapp.BuildConfig.QA_API_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var mainTextView: TextView
    private lateinit var buttonDraw: Button
    private lateinit var buttonId: Button
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = MainViewModel(application)

        mainTextView = findViewById(R.id.mainTextView)
        buttonDraw = findViewById(R.id.button1)
        buttonId = findViewById(R.id.button2)

        if (BuildConfig.DEBUG) prepareDebugEnvironment()

        // Setup QA SDK
        val qa = QA.getInstance(this@MainActivity)

        lifecycle.coroutineScope.launch(Dispatchers.IO) {
//            qa.init(this@MainActivity, QA_API_KEY, BasicInfo(0, QA.Gender.UNKNOWN, false))
//            Timber.d("MainActivity-QA-SDK", "ID :: " + qa.deviceID)
//            qa.syncData(this@MainActivity)

        }

        Timber.d("ID :: " + qa.deviceID)

//        qa.updateBasicInfo(2000, QA.Gender.FEMALE, true)
//        qa.syncData()
//        qa.linkIdentities()
//
        buttonDraw.setOnClickListener { qa.requestOverlayPermission(this) }
        buttonId.setOnClickListener { qa.requestUsagePermission(this) }

        // Retrieve scores
//        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.getStatSample(QA_API_KEY).observe(
            this
        ) {
            if (it.values.isNotEmpty())
                mainTextView.text = "QA Score : ${it.values.last()} @ ${it.timestamps.last()}"
        }

    }

    private fun prepareDebugEnvironment(strictMode: Boolean = false) {
        Timber.plant(Timber.DebugTree())
        if (strictMode) {
            try {
                Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                    .invoke(null, true)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
            StrictMode.enableDefaults()
        }
    }

}

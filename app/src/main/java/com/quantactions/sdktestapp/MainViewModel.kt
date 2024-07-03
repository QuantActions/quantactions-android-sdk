/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quantactions.sdk.Metric
import com.quantactions.sdk.QA
import com.quantactions.sdk.TimeSeries
import com.quantactions.sdk.data.model.ScreenTimeAggregate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * This is the main view model
 * */
class MainViewModel(application: Application) :
    AndroidViewModel(application) {

    private val qa = QA.getInstance(application.applicationContext)

//    @ExperimentalCoroutinesApi
//    fun getStat(statCode: String): LiveData<Resource<StatStreamResponse>> {
//
//        return QA.getStat(getApplication(), statCode).map {
//            when (it.status) {
//                Resource.Status.LOADING -> {
//                    Resource.loading(null)
//                }
//                Resource.Status.SUCCESS -> {
//                    val sortedList = it.data!!.sortedBy { statistic -> statistic.timestamp }
//
//                    Resource.success(
//                        StatStreamResponse(
//                            sortedList.map { statistic -> statistic.value },
//                            sortedList.map { statistic -> statistic.timestamp },
//                            sortedList.map { statistic -> statistic.reset })
//                    )
//                }
//                Resource.Status.ERROR -> {
//                    Resource.error(it.message!!, null)
//                }
//            }
//        }.asLiveData(viewModelScope.coroutineContext)
//    }

    @ExperimentalCoroutinesApi
    fun getStatSample(apiKey: String): LiveData<TimeSeries<ScreenTimeAggregate>> {
        return qa.getMetricSample(getApplication(), apiKey, Metric.SCREEN_TIME_AGGREGATE, from=0, to=Instant.now().toEpochMilli()).asLiveData(viewModelScope.coroutineContext)
    }

    data class StatStreamResponse(
        var evolution: List<Double>,
        var timestamps: List<Long>,
        var reset: List<Int>
    )

    data class StatStreamStringResponse(
        var evolution: List<String>,
        var timestamps: List<Long>,
        var reset: List<Int>
    )

}
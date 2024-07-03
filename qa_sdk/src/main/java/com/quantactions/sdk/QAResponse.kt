/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import androidx.annotation.Keep


/**
 * Wrapper class to hold asynchronous responses to the SDK calls. Check the type of the response
 * when needed and fetch the data only when present. Note that in some cases the data is present
 * even if the call fails, for example if the network is not available but the app requests the
 * latest metrics, the SDK will respond with the latest `cached` metrics thus returning an Error
 * response due to the missing network connection but still with some data.
 * @property data object of any kind return by the call
 * @property message in case of failed response
 * */
//@Keep
//sealed class QAResponse<out T>(
//    val data: T?,
//    val message: String?
//) {
//    @Keep
//    class QASuccessResponse<T>(data: T)                : QAResponse<T>(data, null)
//    @Keep
//    class QAErrorResponse<T>(data: T? = null, message: String) : QAResponse<T>(data, message)
//    @Keep
//    class QALoadingResponse<T>(data: T?)                : QAResponse<T>(data, null)
//}

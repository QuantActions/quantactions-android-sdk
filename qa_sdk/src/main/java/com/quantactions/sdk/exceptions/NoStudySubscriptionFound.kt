/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.exceptions

import androidx.annotation.Keep

/**
 * This exception is thrown sometimes but it should be deprecated
 * @suppress
 * */
@Keep
class NoStudySubscriptionFound: Exception("Could not find any study subscription please check retry participating\n" +
        "This can happen if the latestStudyNeedsGPS was called for th wrong reason.")

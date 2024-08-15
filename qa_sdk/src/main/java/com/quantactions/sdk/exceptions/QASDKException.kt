/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.exceptions

import androidx.annotation.Keep

/**
 * Exception launched when SDK functionalities are called before the SDK was initialized.
 */
@Keep
class QASDKException(errorMessage: String? = "this should not happen, file a bug report!"): Exception(errorMessage)

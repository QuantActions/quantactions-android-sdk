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
 * Data class only basic demographic information of the user associated with the device.
 * */
@Keep
class BasicInfo (
    /** Year of birth e.g. 1985 */
    val yearOfBirth: Int = 0,
    /** Gender of the user, see available settings in [QA.Gender] */
    val gender: QA.Gender = QA.Gender.UNKNOWN,
    /** Whether or not the user declares themselves as being healthy, this can be left blank if unknown */
    val selfDeclaredHealthy: Boolean = false
)

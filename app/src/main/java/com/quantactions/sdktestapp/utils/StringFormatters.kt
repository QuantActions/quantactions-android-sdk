/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdktestapp.utils

sealed class StringFormatter(val pattern: String){
    data object ChartXWeek : StringFormatter("EEE")
    data object ChartXYear : StringFormatter("MMM")
    data object BasicDate : StringFormatter("dd/MM/yyyy")
}

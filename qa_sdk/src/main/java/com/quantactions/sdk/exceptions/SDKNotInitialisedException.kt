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
 * Exception launched when SDK functionalities are called before the SDK was initialized.
 */
@Keep
class SDKNotInitialisedException: Exception("SDK Not Initialised! In order to communicate with the server call" +
        "\n" +
        "QA qa = QA.getInstance();\n" +
        "qa.init(context, your_api_key, 0, QA.Gender.UNKNOWN, 0);\n" +
        "\n")

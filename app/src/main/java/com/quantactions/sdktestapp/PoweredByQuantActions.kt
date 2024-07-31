/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quantactions.sdktestapp.core_ui.theme.TP

@Preview
@Composable
fun PoweredByQuantActions() {
    Row(
//        modifier = Modifier
//            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Powered by",
            style = TP.regular.body1,
            modifier = Modifier.padding(end = 2.dp)
        )

        // image from xml resources
         Image(
             painter = painterResource(id = R.drawable.qa_black_logo),
             contentDescription = "QuantActions Logo",
//             contentScale = ContentScale.Inside,
             modifier = Modifier.height(16.dp)
         )

    }
}
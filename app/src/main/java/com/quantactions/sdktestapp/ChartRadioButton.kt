/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdktestapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantactions.sdktestapp.charts.Chart
import com.quantactions.sdktestapp.core_ui.theme.TP

@Composable
fun ChartRadioButton(
    selectedChart: Chart,
    selectedChartChange: (Chart) -> Unit
) {
    Row(
        modifier = Modifier.padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = (Chart.WEEK == selectedChart),
                onClick = { selectedChartChange(Chart.WEEK) }
            )
            Text(
                text = "14 Days",
                style = TP.regular.body1,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = (Chart.MONTH == selectedChart),
                onClick ={ selectedChartChange(Chart.MONTH) }
            )
            Text(
                text = "6 weeks",
                style = TP.regular.body1,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = (Chart.YEAR == selectedChart),
                onClick = { selectedChartChange(Chart.YEAR) }
            )
            Text(
                text = "12 Months",
                style = TP.regular.body1,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

    }
}
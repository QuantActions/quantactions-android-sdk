/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.pvt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantactions.sdk.R
import com.quantactions.sdk.cognitivetests.theme.QuantBlueContainer
import com.quantactions.sdk.cognitivetests.theme.QuantBlueDark
import com.quantactions.sdk.cognitivetests.theme.QuantBlueLight
import com.quantactions.sdk.cognitivetests.theme.QuantBlueLighter
import com.quantactions.sdk.cognitivetests.theme.StatusActiveGreen
import com.quantactions.sdk.cognitivetests.theme.StatusActiveGreenContainer
import com.quantactions.sdk.cognitivetests.theme.StatusAmberContainer
import com.quantactions.sdk.cognitivetests.theme.StatusAmberWarning
import com.quantactions.sdk.cognitivetests.theme.StatusErrorContainer
import com.quantactions.sdk.cognitivetests.theme.StatusErrorRed
import com.quantactions.sdk.cognitivetests.theme.TestCanvasBackground
import com.quantactions.sdk.cognitivetests.theme.TestCanvasSurface
import com.quantactions.sdk.cognitivetests.theme.TestCanvasSurfaceBorder
import com.quantactions.sdk.cognitivetests.theme.TestTextPrimary
import com.quantactions.sdk.cognitivetests.theme.TestTextSecondary

enum class PVTUiStage {
    DURATION_SELECT,
    INSTRUCTIONS,
    RUNNING,
    SAVING_OR_FINISHED
}

@Composable
fun PVTScreen(
    stage: PVTUiStage,
    selectedDurationIndex: Int,
    durations: List<String>,
    onSelectDurationIndex: (Int) -> Unit,
    onConfirmDuration: () -> Unit,
    onStartTest: () -> Unit,
    onBack: () -> Unit,
    // Test execution state
    isTimerRunning: Boolean,
    elapsedDisplay: String,
    feedbackMessage: String?,
    onReactDown: () -> Unit,
    // Saving & Result state
    medianRT: Double,
    isSaved: Boolean,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TestCanvasBackground
    ) {
        when (stage) {
            PVTUiStage.DURATION_SELECT -> {
                PVTDurationSelectDialog(
                    selectedDurationIndex = selectedDurationIndex,
                    durations = durations,
                    onSelect = onSelectDurationIndex,
                    onConfirm = onConfirmDuration,
                    onBack = onBack
                )
            }
            PVTUiStage.INSTRUCTIONS -> {
                PVTInstructionsStep(
                    onStart = onStartTest,
                    onBack = onBack
                )
            }
            PVTUiStage.RUNNING -> {
                PVTRunningTest(
                    isTimerRunning = isTimerRunning,
                    elapsedDisplay = elapsedDisplay,
                    feedbackMessage = feedbackMessage,
                    onReactDown = onReactDown,
                    onExit = onBack
                )
            }
            PVTUiStage.SAVING_OR_FINISHED -> {
                PVTSavingOrResult(
                    medianRT = medianRT,
                    isSaved = isSaved,
                    onFinish = onFinish
                )
            }
        }
    }
}

@Composable
private fun PVTDurationSelectDialog(
    selectedDurationIndex: Int,
    durations: List<String>,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TestCanvasSurface),
            border = BorderStroke(1.5.dp, TestCanvasSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = QuantBlueDark,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Reaction Time (PVT)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TestTextPrimary
                        )
                        Text(
                            text = "Select test duration",
                            fontSize = 13.sp,
                            color = TestTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                durations.forEachIndexed { index, label ->
                    val isSelected = selectedDurationIndex == index
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) QuantBlueDark.copy(alpha = 0.25f) else Color.Transparent,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) QuantBlueLight else TestCanvasSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(index) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(index) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = QuantBlueLight,
                                    unselectedColor = TestTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = TestTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Cancel", color = TestTextPrimary)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuantBlueDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Next", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PVTInstructionsStep(
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TestTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reaction Time Test",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TestTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TestCanvasSurface),
            border = BorderStroke(1.5.dp, TestCanvasSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Instructions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuantBlueLight
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Sit comfortably in a quiet area free of distractions.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "2. Look at the center button. Numbers (reaction timer) will start counting randomly.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "3. Tap the center button as quickly as possible the moment the numbers appear.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "4. Avoid tapping early before numbers appear—that counts as a false start.",
                    fontSize = 15.sp,
                    color = StatusAmberWarning,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QuantBlueDark),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Test",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PVTRunningTest(
    isTimerRunning: Boolean,
    elapsedDisplay: String,
    feedbackMessage: String?,
    onReactDown: () -> Unit,
    onExit: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "buttonScale")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reaction Time Test",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TestTextSecondary
            )
            Text(
                text = "Tap when numbers appear",
                fontSize = 13.sp,
                color = QuantBlueLight
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Timer Display Box
        Box(
            modifier = Modifier
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isTimerRunning) {
                Text(
                    text = elapsedDisplay,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = QuantBlueDark.copy(alpha = 0.4f),
                    border = BorderStroke(2.dp, QuantBlueLight),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = QuantBlueLight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Feedback Text (e.g. Too fast! / Too slow!)
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!feedbackMessage.isNullOrEmpty()) {
                val isError = feedbackMessage.contains("fast") || feedbackMessage.contains("slow")
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isError) StatusErrorContainer else StatusActiveGreenContainer
                ) {
                    Text(
                        text = feedbackMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) StatusErrorRed else StatusActiveGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Action React Target
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(buttonScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onReactDown()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isTimerRunning) QuantBlueLight else QuantBlueDark,
                border = BorderStroke(4.dp, if (isTimerRunning) Color.White else QuantBlueLighter),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Tap",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "TAP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Powered by QuantActions",
            fontSize = 12.sp,
            color = TestTextSecondary
        )
    }
}

@Composable
private fun PVTSavingOrResult(
    medianRT: Double,
    isSaved: Boolean,
    onFinish: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TestCanvasSurface),
            border = BorderStroke(1.5.dp, TestCanvasSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSaved) StatusActiveGreen else QuantBlueDark,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSaved) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isSaved) "Test Completed!" else "Saving Test Response",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TestTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Median Reaction Time: ${medianRT.toInt()} ms",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuantBlueLight
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSaved) "Your results have been securely recorded." else "Please wait while data is processed...",
                    fontSize = 14.sp,
                    color = TestTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = onFinish,
                    enabled = isSaved,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantBlueDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Finish & Return",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

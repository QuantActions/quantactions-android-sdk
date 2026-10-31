/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.dotmemory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantactions.sdk.cognitivetests.theme.QuantBlueContainer
import com.quantactions.sdk.cognitivetests.theme.QuantBlueDark
import com.quantactions.sdk.cognitivetests.theme.QuantBlueLight
import com.quantactions.sdk.cognitivetests.theme.QuantBlueLighter
import com.quantactions.sdk.cognitivetests.theme.StatusActiveGreen
import com.quantactions.sdk.cognitivetests.theme.StatusAmberContainer
import com.quantactions.sdk.cognitivetests.theme.StatusAmberWarning
import com.quantactions.sdk.cognitivetests.theme.TestCanvasBackground
import com.quantactions.sdk.cognitivetests.theme.TestCanvasSurface
import com.quantactions.sdk.cognitivetests.theme.TestCanvasSurfaceBorder
import com.quantactions.sdk.cognitivetests.theme.TestTextPrimary
import com.quantactions.sdk.cognitivetests.theme.TestTextSecondary

enum class DotMemoryUiStage {
    READY_DIALOG,
    PRESENTATION,
    DISTRACTION,
    RECALL,
    SAVING_OR_FINISHED
}

@Composable
fun DotMemoryTestScreen(
    stage: DotMemoryUiStage,
    currentTrial: Int,
    totalTrials: Int,
    onStartTest: () -> Unit,
    onBack: () -> Unit,
    // Phase 1: Presentation (5x5 grid, 3 target dots)
    presentationDots: Set<Int>,
    // Phase 2: Distraction (8x5 grid, 40 cells, letters F & E)
    distractionLetters: List<String>,
    selectedDistractorIndices: Set<Int>,
    onToggleDistractor: (Int) -> Unit,
    // Phase 3: Recall (5x5 grid, user places up to 3 dots)
    placedDots: Set<Int>,
    onTogglePlacedDot: (Int) -> Unit,
    onConfirmRecall: () -> Unit,
    // Final Summary
    avgTimeTakenSec: Double,
    avgRecallError: Double,
    avgDistractorScore: Double,
    isSaved: Boolean,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TestCanvasBackground
    ) {
        when (stage) {
            DotMemoryUiStage.READY_DIALOG -> {
                DotMemoryReadyDialog(
                    onStart = onStartTest,
                    onBack = onBack
                )
            }
            DotMemoryUiStage.PRESENTATION -> {
                DotMemoryPresentationView(
                    trial = currentTrial + 1,
                    totalTrials = totalTrials,
                    targetDots = presentationDots
                )
            }
            DotMemoryUiStage.DISTRACTION -> {
                DotMemoryDistractionView(
                    trial = currentTrial + 1,
                    totalTrials = totalTrials,
                    distractionLetters = distractionLetters,
                    selectedIndices = selectedDistractorIndices,
                    onToggleDistractor = onToggleDistractor
                )
            }
            DotMemoryUiStage.RECALL -> {
                DotMemoryRecallView(
                    trial = currentTrial + 1,
                    totalTrials = totalTrials,
                    placedDots = placedDots,
                    onTogglePlacedDot = onTogglePlacedDot,
                    onConfirmRecall = onConfirmRecall
                )
            }
            DotMemoryUiStage.SAVING_OR_FINISHED -> {
                DotMemoryFinalSummary(
                    avgTimeTakenSec = avgTimeTakenSec,
                    avgRecallError = avgRecallError,
                    avgDistractorScore = avgDistractorScore,
                    isSaved = isSaved,
                    onFinish = onFinish
                )
            }
        }
    }
}

@Composable
private fun DotMemoryReadyDialog(
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
                text = "Dot Memory Test",
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
                    text = "Ready to Start?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuantBlueLight
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Please ensure you are sitting comfortably and free from distractions.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "2. Phase 1 (Memory): 3 blue dots will appear on a grid for 3 seconds. Memorize their positions.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "3. Phase 2 (Distraction): A grid of letters will appear for 8 seconds. Tap all the 'F' letters as fast as you can.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "4. Phase 3 (Recall): Tap the 3 grid cells where you remember seeing the original dots, then tap Done.",
                    fontSize = 15.sp,
                    color = TestTextPrimary,
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
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "OK, Start Test",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DotMemoryPresentationView(
    trial: Int,
    totalTrials: Int,
    targetDots: Set<Int>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trial $trial of $totalTrials",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = QuantBlueLight
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = QuantBlueContainer
            ) {
                Text(
                    text = "Memorize the 3 dots",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuantBlueDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val gridSide = maxWidth.coerceAtMost(maxHeight) * 0.92f

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TestCanvasSurface),
                border = BorderStroke(2.dp, TestCanvasSurfaceBorder),
                modifier = Modifier.size(gridSide)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 5) {
                                val index = row * 5 + col
                                val isDot = targetDots.contains(index)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDot) QuantBlueDark else TestCanvasSurfaceBorder.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDot) {
                                        Surface(
                                            shape = CircleShape,
                                            color = QuantBlueLighter,
                                            modifier = Modifier.size(24.dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "3 seconds to memorize...",
            fontSize = 14.sp,
            color = TestTextSecondary
        )
    }
}

@Composable
private fun DotMemoryDistractionView(
    trial: Int,
    totalTrials: Int,
    distractionLetters: List<String>,
    selectedIndices: Set<Int>,
    onToggleDistractor: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trial $trial of $totalTrials",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = QuantBlueLight
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = StatusAmberContainer
            ) {
                Text(
                    text = "Tap all 'F' letters",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusAmberWarning,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val gridSideW = maxWidth.coerceAtMost(360.dp)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TestCanvasSurface),
                border = BorderStroke(1.5.dp, TestCanvasSurfaceBorder),
                modifier = Modifier
                    .width(gridSideW)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0 until 8) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0 until 5) {
                                val index = row * 5 + col
                                val letter = distractionLetters.getOrNull(index) ?: "E"
                                val isSelected = selectedIndices.contains(index)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White else TestCanvasSurfaceBorder.copy(alpha = 0.35f))
                                        .clickable { onToggleDistractor(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) TestCanvasBackground else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tap as many 'F's as you can before time expires",
            fontSize = 13.sp,
            color = TestTextSecondary
        )
    }
}

@Composable
private fun DotMemoryRecallView(
    trial: Int,
    totalTrials: Int,
    placedDots: Set<Int>,
    onTogglePlacedDot: (Int) -> Unit,
    onConfirmRecall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trial $trial of $totalTrials",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = QuantBlueLight
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = QuantBlueContainer
            ) {
                Text(
                    text = "Place the 3 dots (${placedDots.size}/3)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuantBlueDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val gridSide = maxWidth.coerceAtMost(maxHeight) * 0.92f

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TestCanvasSurface),
                border = BorderStroke(2.dp, TestCanvasSurfaceBorder),
                modifier = Modifier.size(gridSide)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 5) {
                                val index = row * 5 + col
                                val isSelected = placedDots.contains(index)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) QuantBlueDark else TestCanvasSurfaceBorder.copy(alpha = 0.4f))
                                        .clickable { onTogglePlacedDot(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = QuantBlueLighter,
                                            modifier = Modifier.size(24.dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfirmRecall,
            enabled = placedDots.size == 3,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QuantBlueDark),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Done (${placedDots.size}/3)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DotMemoryFinalSummary(
    avgTimeTakenSec: Double,
    avgRecallError: Double,
    avgDistractorScore: Double,
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

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TestCanvasBackground, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Average Recall Time:", fontSize = 14.sp, color = TestTextSecondary)
                        Text(text = "${"%.1f".format(avgTimeTakenSec)} s", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TestTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Recall Error Score:", fontSize = 14.sp, color = TestTextSecondary)
                        Text(text = "%.2f".format(avgRecallError), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = QuantBlueLight)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Distractor Accuracy:", fontSize = 14.sp, color = TestTextSecondary)
                        Text(text = "${(avgDistractorScore * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TestTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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

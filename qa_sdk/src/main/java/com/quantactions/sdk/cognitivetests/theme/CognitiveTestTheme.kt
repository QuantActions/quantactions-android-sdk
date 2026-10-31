/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * *******************************************************************************
 */

package com.quantactions.sdk.cognitivetests.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// QuantActions Brand Colors (Aligned with TapCounter App)
val QuantBlueDark = Color(0xFF06697D)
val QuantBlueLight = Color(0xFF0C8BA6)
val QuantBlueLighter = Color(0xFF35A2B8)
val QuantBlueContainer = Color(0xFFD4EFF5)
val QuantBlueOnContainer = Color(0xFF003641)

// Neutral & Clinical Surface Colors
val SurfaceLight = Color(0xFFF9FBFC)
val OnSurfaceLight = Color(0xFF131D20)
val SurfaceVariantLight = Color(0xFFE5ECEF)
val OnSurfaceVariantLight = Color(0xFF384347)
val OutlineLight = Color(0xFF6B787D)

// Status & Cognitive Clinical Signals
val StatusActiveGreen = Color(0xFF1B6B2F)
val StatusActiveGreenContainer = Color(0xFFD6F5DB)
val StatusAmberWarning = Color(0xFF8C5800)
val StatusAmberContainer = Color(0xFFFFF0D4)
val StatusErrorRed = Color(0xFFB00020)
val StatusErrorContainer = Color(0xFFFFDAD6)

// Dark Clinical Test Surfaces
val TestCanvasBackground = Color(0xFF0D1417)
val TestCanvasSurface = Color(0xFF1A2327)
val TestCanvasSurfaceBorder = Color(0xFF2C393F)
val TestTextPrimary = Color(0xFFF0F4F6)
val TestTextSecondary = Color(0xFF9EADB4)

private val LightColorScheme = lightColorScheme(
    primary = QuantBlueDark,
    onPrimary = Color.White,
    primaryContainer = QuantBlueContainer,
    onPrimaryContainer = QuantBlueOnContainer,
    secondary = QuantBlueLight,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = Color.White,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = QuantBlueLight,
    onPrimary = QuantBlueOnContainer,
    primaryContainer = QuantBlueDark,
    onPrimaryContainer = Color.White,
    secondary = QuantBlueLighter,
    onSecondary = QuantBlueOnContainer,
    background = TestCanvasBackground,
    onBackground = TestTextPrimary,
    surface = TestCanvasSurface,
    onSurface = TestTextPrimary,
    surfaceVariant = TestCanvasSurfaceBorder,
    onSurfaceVariant = TestTextSecondary,
    outline = TestCanvasSurfaceBorder
)

@Composable
fun CognitiveTestTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}

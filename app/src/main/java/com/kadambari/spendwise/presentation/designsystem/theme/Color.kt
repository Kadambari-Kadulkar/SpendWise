package com.kadambari.spendwise.presentation.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * SpendWise's raw colour tokens.
 *
 * The palette follows the agreed product direction: deep blue as the brand
 * colour, neutral surfaces, green for positive financial states, red for
 * negative financial states, and amber for warnings.
 *
 * These values describe the product palette only. Material 3 roles such as
 * primary, onPrimary, and surfaceVariant must be mapped in [Theme.kt] rather
 * than used directly by screens or reusable components.
 */

// Brand colours
val PrimaryBlue = Color(0xFF0F4C81)
val PrimaryBlueContainer = Color(0xFFD6E8FF)
val SecondaryBlue = Color(0xFF4B6584)
val SecondaryBlueContainer = Color(0xFFE7EDF5)

// Neutral colours
val Background = Color(0xFFF7F8FA)
val Surface = Color(0xFFFFFFFF)
val PrimaryText = Color(0xFF1B1B1B)
val DarkGray = Color(0xFF616161)
val MediumGray = Color(0xFF9E9E9E)
val LightGray = Color(0xFFE0E0E0)
val White = Color(0xFFFFFFFF)

// Semantic colours
val Success = Color(0xFF2E7D32)
val Warning = Color(0xFFED6C02)
val Error = Color(0xFFD32F2F)

// Financial colours
val Income = Color(0xFF2E7D32)
val Expense = Color(0xFFC62828)

// Dark-theme base colours
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)

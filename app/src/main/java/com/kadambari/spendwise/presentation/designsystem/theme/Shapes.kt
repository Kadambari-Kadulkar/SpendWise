package com.kadambari.spendwise.presentation.designsystem.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shared corner-radius tokens for the SpendWise design system.
 *
 * The values are intentionally restrained so cards and containers feel
 * polished without making the financial interface look overly playful.
 */
object SpendWiseCornerRadius {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
}

/**
 * Material 3 shape scale used across SpendWise.
 *
 * Feature screens should use [SpendWiseShapes] through
 * `MaterialTheme.shapes` instead of creating ad-hoc rounded shapes.
 */
val SpendWiseShapes = Shapes(
    extraSmall = RoundedCornerShape(SpendWiseCornerRadius.extraSmall),
    small = RoundedCornerShape(SpendWiseCornerRadius.small),
    medium = RoundedCornerShape(SpendWiseCornerRadius.medium),
    large = RoundedCornerShape(SpendWiseCornerRadius.large),
    extraLarge = RoundedCornerShape(SpendWiseCornerRadius.extraLarge)
)


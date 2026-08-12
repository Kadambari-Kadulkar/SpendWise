package com.kadambari.spendwise.presentation.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing tokens for the SpendWise design system.
 *
 * Values follow a 4dp rhythm and support the calm, uncluttered layout defined
 * by the project UI guidelines. Prefer these tokens over screen-specific magic
 * numbers so spacing remains consistent as the application grows.
 */
object SpendWiseDimens {
    // Spacing scale
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space24 = 24.dp
    val space32 = 32.dp

    // Layout spacing
    val screenHorizontalPadding = 16.dp
    val screenVerticalPadding = 24.dp
    val sectionSpacing = 24.dp
    val cardPadding = 16.dp
    val listItemVerticalPadding = 12.dp

    // Interactive and icon sizing. Interactive controls should not be smaller
    // than the minimum touch target, even when their visual content is small.
    val minimumTouchTarget = 48.dp
    val iconSmall = 20.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp

    // Small structural dimensions
    val dividerThickness = 1.dp
}


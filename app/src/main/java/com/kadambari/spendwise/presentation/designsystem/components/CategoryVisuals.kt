package com.kadambari.spendwise.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kadambari.spendwise.domain.model.TransactionType

/**
 * Visual representation helper for the existing SpendWise category catalogue.
 *
 * This provides visual categorization (icon and subtle container tint) for
 * transaction rows and forms without altering the domain category model.
 */
object CategoryVisuals {

    fun iconFor(categoryId: String?, transactionType: TransactionType? = null): ImageVector {
        return when (categoryId) {
            "income_salary" -> Icons.Outlined.AccountBalance
            "income_freelance" -> Icons.Outlined.WorkOutline
            "income_bonus" -> Icons.Outlined.CardGiftcard
            "income_investment" -> Icons.AutoMirrored.Outlined.TrendingUp
            "income_other" -> Icons.Outlined.Savings
            "expense_food" -> Icons.Outlined.Restaurant
            "expense_shopping" -> Icons.Outlined.ShoppingBag
            "expense_bills" -> Icons.AutoMirrored.Outlined.ReceiptLong
            "expense_transport" -> Icons.Outlined.DirectionsBus
            "expense_entertainment" -> Icons.Outlined.Movie
            "expense_health" -> Icons.Outlined.MedicalServices
            "expense_travel" -> Icons.Outlined.Flight
            "expense_education" -> Icons.Outlined.School
            "expense_other" -> Icons.Outlined.MoreHoriz
            else -> when (transactionType) {
                TransactionType.INCOME -> Icons.Outlined.ArrowUpward
                TransactionType.EXPENSE -> Icons.Outlined.ArrowDownward
                null -> Icons.Outlined.MoreHoriz
            }
        }
    }
}

/**
 * A clean squircle icon badge for transaction categories.
 *
 * Decorative only; contentDescription is null to prevent redundant TalkBack
 * announcements when placed inside a merged-semantics row.
 */
@Composable
fun CategoryIconBadge(
    categoryId: String?,
    transactionType: TransactionType? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    val isDark = isSystemInDarkTheme()
    val isIncome = transactionType == TransactionType.INCOME || categoryId?.startsWith("income") == true

    val containerColor = if (isDark) {
        if (isIncome) Color(0xFF193220) else Color(0xFF2E2628)
    } else {
        if (isIncome) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    }

    val iconTint = if (isDark) {
        if (isIncome) Color(0xFF81C784) else Color(0xFFE0E0E0)
    } else {
        if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CategoryVisuals.iconFor(categoryId, transactionType),
            contentDescription = null, // Decorative icon within accessible container
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

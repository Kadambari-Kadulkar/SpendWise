package com.kadambari.spendwise.presentation.analytics

import com.kadambari.spendwise.domain.model.AnalyticsPeriod

/**
 * User interactions and lifecycle events for the SpendWise Analytics feature.
 */
sealed interface AnalyticsUiEvent {
    data class PeriodSelected(val period: AnalyticsPeriod) : AnalyticsUiEvent
    data object Retry : AnalyticsUiEvent
}

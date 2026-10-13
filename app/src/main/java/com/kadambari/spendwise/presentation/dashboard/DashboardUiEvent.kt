package com.kadambari.spendwise.presentation.dashboard

import java.time.YearMonth

/**
 * User-driven and lifecycle events supported by the Dashboard.
 */
sealed interface DashboardUiEvent {
    /**
     * Triggered when the user selects a different calendar month on the Dashboard.
     */
    data class MonthChanged(val month: YearMonth) : DashboardUiEvent

    /**
     * Triggered to retry loading dashboard data after a failure.
     */
    data object Retry : DashboardUiEvent

    /**
     * Triggered when the dashboard is displayed or resumed from background.
     */
    data object Resume : DashboardUiEvent
}

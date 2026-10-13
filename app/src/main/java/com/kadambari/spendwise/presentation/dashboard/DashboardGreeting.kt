package com.kadambari.spendwise.presentation.dashboard

import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Domain-aware presentation helper for calculating SpendWise's local-time-based dashboard greeting.
 *
 * Greeting boundaries (based on device local wall-clock time):
 * - 05:00–11:59: "Good morning"
 * - 12:00–16:59: "Good afternoon"
 * - 17:00–20:59: "Good evening"
 * - 21:00–04:59: "Welcome back"
 */
object DashboardGreeting {
    const val GOOD_MORNING = "Good morning"
    const val GOOD_AFTERNOON = "Good afternoon"
    const val GOOD_EVENING = "Good evening"
    const val WELCOME_BACK = "Welcome back"

    /**
     * Resolves the greeting string based on the given local wall-clock [time].
     */
    fun forTime(time: LocalTime): String = when (time.hour) {
        in 5..11 -> GOOD_MORNING
        in 12..16 -> GOOD_AFTERNOON
        in 17..20 -> GOOD_EVENING
        else -> WELCOME_BACK
    }

    /**
     * Resolves the greeting string using the supplied [clock] and [zoneId].
     */
    fun forClock(clock: Clock, zoneId: ZoneId): String {
        val localTime = clock.instant().atZone(zoneId).toLocalTime()
        return forTime(localTime)
    }

    /**
     * Computes the next boundary [ZonedDateTime] when the greeting will transition.
     */
    fun nextBoundary(currentDateTime: ZonedDateTime): ZonedDateTime {
        val date = currentDateTime.toLocalDate()
        val time = currentDateTime.toLocalTime()
        return when {
            time.isBefore(LocalTime.of(5, 0)) -> date.atTime(5, 0).atZone(currentDateTime.zone)
            time.isBefore(LocalTime.of(12, 0)) -> date.atTime(12, 0).atZone(currentDateTime.zone)
            time.isBefore(LocalTime.of(17, 0)) -> date.atTime(17, 0).atZone(currentDateTime.zone)
            time.isBefore(LocalTime.of(21, 0)) -> date.atTime(21, 0).atZone(currentDateTime.zone)
            else -> date.plusDays(1).atTime(5, 0).atZone(currentDateTime.zone)
        }
    }

    /**
     * Computes the duration until the next greeting boundary.
     */
    fun durationUntilNextBoundary(currentDateTime: ZonedDateTime): Duration {
        val boundary = nextBoundary(currentDateTime)
        return Duration.between(currentDateTime, boundary)
    }
}

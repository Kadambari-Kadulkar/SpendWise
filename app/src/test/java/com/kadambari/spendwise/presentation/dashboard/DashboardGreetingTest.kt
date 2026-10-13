package com.kadambari.spendwise.presentation.dashboard

import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardGreetingTest {

    private val zoneLondon = ZoneId.of("Europe/London")

    @Test
    fun `boundary 04 59 returns Welcome back`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(4, 59))
        assertEquals(DashboardGreeting.WELCOME_BACK, greeting)
    }

    @Test
    fun `boundary 05 00 returns Good morning`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(5, 0))
        assertEquals(DashboardGreeting.GOOD_MORNING, greeting)
    }

    @Test
    fun `boundary 11 59 returns Good morning`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(11, 59))
        assertEquals(DashboardGreeting.GOOD_MORNING, greeting)
    }

    @Test
    fun `boundary 12 00 returns Good afternoon`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(12, 0))
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, greeting)
    }

    @Test
    fun `boundary 16 59 returns Good afternoon`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(16, 59))
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, greeting)
    }

    @Test
    fun `boundary 17 00 returns Good evening`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(17, 0))
        assertEquals(DashboardGreeting.GOOD_EVENING, greeting)
    }

    @Test
    fun `boundary 20 59 returns Good evening`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(20, 59))
        assertEquals(DashboardGreeting.GOOD_EVENING, greeting)
    }

    @Test
    fun `boundary 21 00 returns Welcome back`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(21, 0))
        assertEquals(DashboardGreeting.WELCOME_BACK, greeting)
    }

    @Test
    fun `midnight 00 00 returns Welcome back`() {
        val greeting = DashboardGreeting.forTime(LocalTime.of(0, 0))
        assertEquals(DashboardGreeting.WELCOME_BACK, greeting)
    }

    @Test
    fun `mid range times return expected greetings`() {
        assertEquals(DashboardGreeting.GOOD_MORNING, DashboardGreeting.forTime(LocalTime.of(8, 30)))
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, DashboardGreeting.forTime(LocalTime.of(14, 15)))
        assertEquals(DashboardGreeting.GOOD_EVENING, DashboardGreeting.forTime(LocalTime.of(19, 45)))
        assertEquals(DashboardGreeting.WELCOME_BACK, DashboardGreeting.forTime(LocalTime.of(22, 30)))
        assertEquals(DashboardGreeting.WELCOME_BACK, DashboardGreeting.forTime(LocalTime.of(2, 15)))
    }

    @Test
    fun `forClock resolves greeting correctly using injected clock and zone`() {
        // 2026-10-07T21:15:00Z in London (BST, UTC+1) is 22:15 -> "Welcome back"
        val clockNight = Clock.fixed(Instant.parse("2026-10-07T21:15:00Z"), zoneLondon)
        assertEquals(DashboardGreeting.WELCOME_BACK, DashboardGreeting.forClock(clockNight, zoneLondon))

        // 2026-10-07T07:30:00Z in London (BST, UTC+1) is 08:30 -> "Good morning"
        val clockMorning = Clock.fixed(Instant.parse("2026-10-07T07:30:00Z"), zoneLondon)
        assertEquals(DashboardGreeting.GOOD_MORNING, DashboardGreeting.forClock(clockMorning, zoneLondon))

        // 2026-10-07T12:30:00Z in London (BST, UTC+1) is 13:30 -> "Good afternoon"
        val clockAfternoon = Clock.fixed(Instant.parse("2026-10-07T12:30:00Z"), zoneLondon)
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, DashboardGreeting.forClock(clockAfternoon, zoneLondon))

        // 2026-10-07T17:00:00Z in London (BST, UTC+1) is 18:00 -> "Good evening"
        val clockEvening = Clock.fixed(Instant.parse("2026-10-07T17:00:00Z"), zoneLondon)
        assertEquals(DashboardGreeting.GOOD_EVENING, DashboardGreeting.forClock(clockEvening, zoneLondon))
    }

    @Test
    fun `nextBoundary computes correct transition targets`() {
        val dtBeforeMorning = ZonedDateTime.of(2026, 10, 7, 4, 59, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 5, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtBeforeMorning),
        )

        val dtMorning = ZonedDateTime.of(2026, 10, 7, 5, 0, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 12, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtMorning),
        )

        val dtBeforeAfternoon = ZonedDateTime.of(2026, 10, 7, 11, 59, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 12, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtBeforeAfternoon),
        )

        val dtAfternoon = ZonedDateTime.of(2026, 10, 7, 12, 0, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 17, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtAfternoon),
        )

        val dtEvening = ZonedDateTime.of(2026, 10, 7, 17, 0, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 21, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtEvening),
        )

        val dtNight = ZonedDateTime.of(2026, 10, 7, 21, 0, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 8, 5, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtNight),
        )

        val dtMidnight = ZonedDateTime.of(2026, 10, 7, 0, 0, 0, 0, zoneLondon)
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 5, 0, 0, 0, zoneLondon),
            DashboardGreeting.nextBoundary(dtMidnight),
        )
    }
}

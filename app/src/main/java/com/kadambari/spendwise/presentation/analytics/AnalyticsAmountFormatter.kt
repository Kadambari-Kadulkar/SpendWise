package com.kadambari.spendwise.presentation.analytics

import com.kadambari.spendwise.domain.model.CurrencyCatalogue
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats monetary amounts, percentages, deltas, and date ranges for SpendWise Analytics.
 *
 * All monetary amounts are formatted using exact [BigDecimal] representations and integer [Long]
 * minor units. Floating point conversions (Float/Double) are strictly avoided.
 */
object AnalyticsAmountFormatter {

    private const val EN_DASH = "–"

    /**
     * Formats net cash flow.
     * - Positive cash flow: "+£1,250.00"
     * - Negative cash flow: "-£45.50"
     * - Zero cash flow: "£0.00" (neutral, no sign)
     */
    fun formatNetCashFlow(
        amountMinorUnits: Long,
        currencyCode: CurrencyCode,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val formattedNumber = formatAbsoluteAmount(amountMinorUnits, currency.fractionDigits, locale)

        return when {
            amountMinorUnits > 0L -> "+${currency.symbol}$formattedNumber"
            amountMinorUnits < 0L -> "-${currency.symbol}$formattedNumber"
            else -> "${currency.symbol}$formattedNumber"
        }
    }

    /**
     * Formats signed income or expense totals with explicit sign (+ or -) and currency symbol.
     * - Income: "+£3,500.00"
     * - Expense: "-£2,250.00"
     */
    fun formatSignedTotal(
        amountMinorUnits: Long,
        currencyCode: CurrencyCode,
        type: TransactionType,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val formattedNumber = formatAbsoluteAmount(amountMinorUnits, currency.fractionDigits, locale)
        val sign = when (type) {
            TransactionType.INCOME -> "+"
            TransactionType.EXPENSE -> "-"
        }
        return "$sign${currency.symbol}$formattedNumber"
    }

    /**
     * Formats signed expense difference against a comparison baseline.
     * - Positive difference (expenses increased): "+£45.50"
     * - Negative difference (expenses decreased): "-£120.00"
     * - Zero difference (spending unchanged): "£0.00" (neutral)
     */
    fun formatExpenseDifference(
        differenceMinorUnits: Long,
        currencyCode: CurrencyCode,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val formattedNumber = formatAbsoluteAmount(differenceMinorUnits, currency.fractionDigits, locale)

        return when {
            differenceMinorUnits > 0L -> "+${currency.symbol}$formattedNumber"
            differenceMinorUnits < 0L -> "-${currency.symbol}$formattedNumber"
            else -> "${currency.symbol}$formattedNumber"
        }
    }

    /**
     * Formats standard unsigned monetary amounts (e.g. for category rows: "£620.00").
     */
    fun formatAmount(
        amountMinorUnits: Long,
        currencyCode: CurrencyCode,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val formattedNumber = formatAbsoluteAmount(amountMinorUnits, currency.fractionDigits, locale)
        return "${currency.symbol}$formattedNumber"
    }

    /**
     * Formats a category spending proportion as a percentage with 1 decimal place (e.g. "27.6%").
     */
    fun formatPercentage(
        percentage: BigDecimal,
        locale: Locale = Locale.UK,
    ): String {
        val decimalFormat = DecimalFormat(
            "#,##0.0",
            DecimalFormatSymbols.getInstance(locale),
        ).apply {
            roundingMode = RoundingMode.HALF_UP
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return "${decimalFormat.format(percentage)}%"
    }

    /**
     * Formats a comparison percentage delta.
     * - Positive delta: "+12.5%"
     * - Negative delta: "-8.0%"
     * - Zero delta: "0.0%"
     * - Null delta (zero baseline in comparison period): returns null (does not invent a percentage).
     */
    fun formatPercentageDelta(
        delta: BigDecimal?,
        locale: Locale = Locale.UK,
    ): String? {
        if (delta == null) return null

        val decimalFormat = DecimalFormat(
            "#,##0.0",
            DecimalFormatSymbols.getInstance(locale),
        ).apply {
            roundingMode = RoundingMode.HALF_UP
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }

        val signum = delta.signum()
        val formattedAbs = decimalFormat.format(delta.abs())

        return when {
            signum > 0 -> "+$formattedAbs%"
            signum < 0 -> "-$formattedAbs%"
            else -> "0.0%"
        }
    }

    /**
     * Formats a period date range according to UK locale standards.
     * - Same day: "1 Oct 2026"
     * - Date span: "1 Oct 2026 – 16 Oct 2026"
     */
    fun formatDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        locale: Locale = Locale.UK,
    ): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        val formattedStart = startDate.format(formatter)
        val formattedEnd = endDate.format(formatter)

        return if (startDate == endDate) {
            formattedStart
        } else {
            "$formattedStart $EN_DASH $formattedEnd"
        }
    }

    private fun formatAbsoluteAmount(
        minorUnits: Long,
        fractionDigits: Int,
        locale: Locale,
    ): String {
        val amount = BigDecimal.valueOf(minorUnits, fractionDigits).abs()
        val decimalFormat = DecimalFormat(
            patternFor(fractionDigits),
            DecimalFormatSymbols.getInstance(locale),
        ).apply {
            roundingMode = RoundingMode.UNNECESSARY
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
            isGroupingUsed = true
        }
        return decimalFormat.format(amount)
    }

    private fun patternFor(fractionDigits: Int): String = buildString {
        append("#,##0")
        if (fractionDigits > 0) {
            append('.')
            repeat(fractionDigits) { append('0') }
        }
    }
}

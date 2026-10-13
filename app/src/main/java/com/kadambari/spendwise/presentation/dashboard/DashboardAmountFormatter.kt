package com.kadambari.spendwise.presentation.dashboard

import com.kadambari.spendwise.domain.model.CurrencyCatalogue
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Formats monetary amounts for the SpendWise Dashboard.
 *
 * All amounts are formatted using exact BigDecimal representation and Long minor units.
 * Floating point conversions (Float/Double) are strictly avoided.
 */
object DashboardAmountFormatter {

    /**
     * Formats overall balance. Positive amounts and zero are formatted as e.g. £2,480.50,
     * while negative amounts are prefixed with a minus sign e.g. -£50.00.
     */
    fun formatBalance(
        amountMinorUnits: Long,
        currencyCode: CurrencyCode,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val isNegative = amountMinorUnits < 0
        val positiveMinorUnits = abs(amountMinorUnits)
        val amount = BigDecimal.valueOf(
            positiveMinorUnits,
            currency.fractionDigits,
        )
        val decimalFormat = createDecimalFormat(currency.fractionDigits, locale)
        val formattedNumber = decimalFormat.format(amount)

        return if (isNegative) {
            "-${currency.symbol}$formattedNumber"
        } else {
            "${currency.symbol}$formattedNumber"
        }
    }

    /**
     * Formats monthly income or expenses with explicit sign (+ or -) and currency symbol.
     */
    fun formatMonthlyTotal(
        amountMinorUnits: Long,
        currencyCode: CurrencyCode,
        type: TransactionType,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val amount = BigDecimal.valueOf(
            abs(amountMinorUnits),
            currency.fractionDigits,
        )
        val decimalFormat = createDecimalFormat(currency.fractionDigits, locale)
        val sign = when (type) {
            TransactionType.INCOME -> "+"
            TransactionType.EXPENSE -> "-"
        }

        return "$sign${currency.symbol}${decimalFormat.format(amount)}"
    }

    private fun createDecimalFormat(fractionDigits: Int, locale: Locale): DecimalFormat =
        DecimalFormat(
            patternFor(fractionDigits),
            DecimalFormatSymbols.getInstance(locale),
        ).apply {
            roundingMode = RoundingMode.UNNECESSARY
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
            isGroupingUsed = true
        }

    private fun patternFor(fractionDigits: Int): String = buildString {
        append("#,##0")
        if (fractionDigits > 0) {
            append('.')
            repeat(fractionDigits) { append('0') }
        }
    }
}

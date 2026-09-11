package com.kadambari.spendwise.presentation.transaction

import com.kadambari.spendwise.domain.model.CurrencyCatalogue
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats persisted transaction amounts for the transaction feature.
 *
 * Domain amounts remain positive minor units. The transaction type supplies
 * the presentation sign, while currency metadata supplies the symbol and
 * fraction digits.
 */
object TransactionAmountFormatter {

    fun format(
        amountMinorUnits: Long,
        currencyCode: CurrencyCode,
        type: TransactionType,
        locale: Locale = Locale.UK,
    ): String {
        val currency = CurrencyCatalogue.requireSupported(currencyCode)
        val amount = BigDecimal.valueOf(
            amountMinorUnits,
            currency.fractionDigits,
        )
        val decimalFormat = DecimalFormat(
            patternFor(currency.fractionDigits),
            DecimalFormatSymbols.getInstance(locale),
        ).apply {
            roundingMode = RoundingMode.UNNECESSARY
            minimumFractionDigits = currency.fractionDigits
            maximumFractionDigits = currency.fractionDigits
            isGroupingUsed = true
        }
        val sign = when (type) {
            TransactionType.INCOME -> "+"
            TransactionType.EXPENSE -> "-"
        }

        return "$sign${currency.symbol}${decimalFormat.format(amount)}"
    }

    private fun patternFor(fractionDigits: Int): String = buildString {
        append("#,##0")
        if (fractionDigits > 0) {
            append('.')
            repeat(fractionDigits) { append('0') }
        }
    }
}

package com.kadambari.spendwise.domain.model

import java.math.BigDecimal

/**
 * A positive monetary amount stored in the smallest unit of its currency.
 *
 * For GBP, 1000 minor units represents £10.00. The currency is part of the
 * value so that amounts from different currencies cannot be confused.
 */
data class Money private constructor(
    val minorUnits: Long,
    val currencyCode: CurrencyCode
) {
    companion object {
        fun fromMinorUnits(
            minorUnits: Long,
            currencyCode: CurrencyCode
        ): Money {
            CurrencyCatalogue.requireSupported(currencyCode)

            if (minorUnits <= 0) {
                throw DomainValidationException("Money amount must be greater than zero.")
            }

            return Money(
                minorUnits = minorUnits,
                currencyCode = currencyCode
            )
        }

        /**
         * Converts an exact decimal amount to minor units without rounding.
         */
        fun fromDecimal(
            amount: BigDecimal,
            currencyCode: CurrencyCode
        ): Money {
            val definition = CurrencyCatalogue.requireSupported(currencyCode)

            if (amount.signum() <= 0) {
                throw DomainValidationException("Money amount must be greater than zero.")
            }

            if (amount.scale() > definition.fractionDigits) {
                throw DomainValidationException(
                    "${definition.code.value} amounts support at most " +
                        "${definition.fractionDigits} fractional digits."
                )
            }

            val minorUnits = try {
                amount
                    .movePointRight(definition.fractionDigits)
                    .longValueExact()
            } catch (_: ArithmeticException) {
                throw DomainValidationException(
                    "Money amount is outside the supported range."
                )
            }

            if (minorUnits <= 0) {
                throw DomainValidationException("Money amount must be greater than zero.")
            }

            return Money(
                minorUnits = minorUnits,
                currencyCode = currencyCode
            )
        }
    }
}

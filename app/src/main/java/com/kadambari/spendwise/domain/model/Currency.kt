package com.kadambari.spendwise.domain.model

import java.util.Locale

/**
 * An ISO 4217 alpha-3 currency code.
 *
 * The value object validates the shape of a code. Whether a syntactically
 * valid code is currently supported by SpendWise is determined by
 * [CurrencyCatalogue].
 */
@JvmInline
value class CurrencyCode private constructor(val value: String) {
    companion object {
        val GBP: CurrencyCode = CurrencyCode("GBP")

        fun of(rawValue: String): CurrencyCode {
            val normalisedValue = rawValue.trim().uppercase(Locale.ROOT)

            if (!normalisedValue.matches(ISO_4217_ALPHA_3_PATTERN)) {
                throw DomainValidationException(
                    "Currency code must be a three-letter ISO 4217 code."
                )
            }

            return CurrencyCode(normalisedValue)
        }

        private val ISO_4217_ALPHA_3_PATTERN = Regex("[A-Z]{3}")
    }

    override fun toString(): String = value
}

data class CurrencyDefinition(
    val code: CurrencyCode,
    val displayName: String,
    val symbol: String,
    val fractionDigits: Int
) {
    init {
        if (displayName.isBlank()) {
            throw DomainValidationException("Currency display name must not be blank.")
        }
        if (symbol.isBlank()) {
            throw DomainValidationException("Currency symbol must not be blank.")
        }
        if (fractionDigits !in 0..9) {
            throw DomainValidationException(
                "Currency fraction digits must be between 0 and 9."
            )
        }
    }
}

/**
 * Application-supported currencies.
 *
 * GBP is intentionally the only supported currency at this stage. Adding a
 * currency here later does not require changing the transaction model.
 */
object CurrencyCatalogue {
    val GBP = CurrencyDefinition(
        code = CurrencyCode.GBP,
        displayName = "British Pound",
        symbol = "£",
        fractionDigits = 2
    )

    private val definitions = mapOf(GBP.code to GBP)

    val supported: List<CurrencyDefinition>
        get() = definitions.values.toList()

    fun find(code: CurrencyCode): CurrencyDefinition? = definitions[code]

    fun requireSupported(code: CurrencyCode): CurrencyDefinition =
        find(code)
            ?: throw DomainValidationException(
                "Currency ${code.value} is not supported by SpendWise."
            )
}

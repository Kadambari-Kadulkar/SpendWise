package com.kadambari.spendwise.domain.model

/**
 * Raised when a domain model cannot be created because it violates a
 * SpendWise business rule.
 */
class DomainValidationException(message: String) : IllegalArgumentException(message)

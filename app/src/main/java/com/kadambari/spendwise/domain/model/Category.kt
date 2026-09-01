package com.kadambari.spendwise.domain.model

import java.util.Locale

/**
 * Stable identifier for a transaction category. Display labels are kept
 * separately so they can change or be localised without changing records.
 */
@JvmInline
value class CategoryId private constructor(val value: String) {
    companion object {
        fun of(rawValue: String): CategoryId {
            val normalisedValue = rawValue.trim().lowercase(Locale.ROOT)

            if (!normalisedValue.matches(CATEGORY_ID_PATTERN)) {
                throw DomainValidationException(
                    "Category ID must contain only lowercase letters, numbers, " +
                        "underscores, or hyphens."
                )
            }

            return CategoryId(normalisedValue)
        }

        private val CATEGORY_ID_PATTERN = Regex("[a-z0-9][a-z0-9_-]*")
    }

    override fun toString(): String = value
}

data class CategoryDefinition(
    val id: CategoryId,
    val transactionType: TransactionType,
    val displayName: String
) {
    init {
        if (displayName.isBlank()) {
            throw DomainValidationException("Category display name must not be blank.")
        }
    }
}

/**
 * Predefined categories for the first Transactions feature.
 *
 * Categories are deliberately not persisted as an enum or database entity at
 * this stage. Their stable IDs allow a future custom-category table to be
 * introduced without changing Transaction.categoryId.
 */
object CategoryCatalogue {
    private val definitions = listOf(
        CategoryDefinition(
            id = CategoryId.of("income_salary"),
            transactionType = TransactionType.INCOME,
            displayName = "Salary"
        ),
        CategoryDefinition(
            id = CategoryId.of("income_freelance"),
            transactionType = TransactionType.INCOME,
            displayName = "Freelance"
        ),
        CategoryDefinition(
            id = CategoryId.of("income_bonus"),
            transactionType = TransactionType.INCOME,
            displayName = "Bonus"
        ),
        CategoryDefinition(
            id = CategoryId.of("income_investment"),
            transactionType = TransactionType.INCOME,
            displayName = "Investment"
        ),
        CategoryDefinition(
            id = CategoryId.of("income_other"),
            transactionType = TransactionType.INCOME,
            displayName = "Other"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_food"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Food"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_shopping"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Shopping"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_bills"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Bills"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_transport"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Transport"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_entertainment"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Entertainment"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_health"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Health"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_travel"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Travel"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_education"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Education"
        ),
        CategoryDefinition(
            id = CategoryId.of("expense_other"),
            transactionType = TransactionType.EXPENSE,
            displayName = "Other"
        )
    )

    private val definitionsById = definitions.associateBy { it.id }

    val all: List<CategoryDefinition>
        get() = definitions

    fun forType(type: TransactionType): List<CategoryDefinition> =
        definitions.filter { it.transactionType == type }

    fun find(id: CategoryId): CategoryDefinition? = definitionsById[id]

    fun requireCategory(id: CategoryId): CategoryDefinition =
        find(id)
            ?: throw DomainValidationException(
                "Category ${id.value} does not exist."
            )
}

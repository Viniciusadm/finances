package com.archieapps.finances.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(
    val message: String? = null,
    val body: T? = null,
    val error: String? = null,
    val success: Boolean = false
)

@Serializable
data class LoginDto(val token: String? = null)

@Serializable
data class AccountDto(
    val id: Int,
    val name: String,
    val kind: String,
    val kindLabel: String,
    val initialBalanceCents: Long,
    val balanceCents: Long? = null,
    val currency: String = "BRL",
    val color: String? = null,
    val colorToken: String? = null,
    val icon: String? = null,
    val position: Int = 0,
    val archived: Boolean = false
)

@Serializable
data class CardDto(
    val id: Int,
    val name: String,
    val brand: String? = null,
    val lastDigits: String? = null,
    val accountId: Int? = null,
    val accountName: String? = null,
    val limitCents: Long? = null,
    val usedCents: Long? = null,
    val closingDay: Int,
    val dueDay: Int,
    val color: String? = null,
    val colorToken: String? = null,
    val icon: String? = null,
    val position: Int = 0,
    val archived: Boolean = false
)

@Serializable
data class CategoryDto(
    val id: Int,
    val parentId: Int? = null,
    val name: String,
    val slug: String,
    val kind: String,
    val kindLabel: String,
    val color: String? = null,
    val colorToken: String? = null,
    val icon: String? = null,
    val position: Int = 0,
    val active: Boolean = true,
    val isDefault: Boolean = false,
    val entryCount: Int? = null
)

@Serializable
data class TagDto(
    val id: Int,
    val name: String,
    val slug: String,
    val color: String? = null,
    val colorToken: String? = null,
    val usageCount: Int? = null
)

@Serializable
data class PageInfo(
    val limit: Int = 0,
    val current: Int = 1,
    val last: Int = 1,
    val count: Int = 0,
    val total: Int = 0
)

@Serializable
data class PagedEnvelope<T>(
    val message: String? = null,
    val body: List<T> = emptyList(),
    val pagination: PageInfo? = null,
    val error: String? = null,
    val success: Boolean = false
)

@Serializable
data class EntryDto(
    val id: Int,
    val kind: String,
    val kindLabel: String,
    val description: String,
    val amountCents: Long,
    val signedAmountCents: Long,
    val date: String,
    val paid: Boolean = true,
    val accountId: Int? = null,
    val accountName: String? = null,
    val counterAccountId: Int? = null,
    val counterAccountName: String? = null,
    val cardId: Int? = null,
    val cardName: String? = null,
    val paymentMethod: String? = null,
    val paymentMethodLabel: String? = null,
    val statementId: Int? = null,
    val paysStatementId: Int? = null,
    val installmentPlanId: Int? = null,
    val installmentNumber: Int? = null,
    val installmentsTotal: Int? = null,
    val recurrenceId: Int? = null,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val personId: Int? = null,
    val personName: String? = null,
    val personRole: String? = null,
    val personRoleLabel: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    val color: String? = null,
    val colorToken: String? = null,
    val items: List<EntryItemDto>? = null,
    val itemsTotalCents: Long? = null
)

@Serializable
data class EntryItemDto(
    val id: Int,
    val entryId: Int,
    val productId: Int? = null,
    val productName: String? = null,
    val name: String,
    val quantity: Double = 1.0,
    val unitPriceCents: Long = 0,
    val totalCents: Long = 0
)

@Serializable
data class MonthTotalsDto(
    val expenseCents: Long = 0,
    val expenseAccountCents: Long = 0,
    val expenseCardCents: Long = 0,
    val incomeCents: Long = 0,
    val netCents: Long = 0,
    val cardDueReference: String? = null
)

@Serializable
data class CategorySliceDto(
    val categoryId: Int? = null,
    val name: String,
    val color: String? = null,
    val colorToken: String? = null,
    val totalCents: Long = 0
)

@Serializable
data class OverviewDto(
    val month: String,
    val totalCents: Long = 0,
    val committedCents: Long = 0,
    val freeCents: Long = 0,
    val accounts: List<AccountDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val monthTotals: MonthTotalsDto = MonthTotalsDto(),
    val previousTotals: MonthTotalsDto = MonthTotalsDto(),
    val byCategory: List<CategorySliceDto> = emptyList(),
    val unpaidCents: Long = 0,
    val openStatements: List<OpenStatementDto> = emptyList(),
    val upcoming: List<EntryDto> = emptyList(),
    val recent: List<EntryDto> = emptyList()
)

@Serializable
data class PersonBalanceDto(
    val id: Int,
    val name: String,
    val nickname: String? = null,
    val balanceCents: Long = 0,
    val direction: String = "settled"
)

@Serializable
data class StatementDto(
    val id: Int,
    val cardId: Int,
    val cardName: String? = null,
    val reference: String,
    val startsOn: String,
    val closesOn: String,
    val dueOn: String,
    val totalCents: Long = 0,
    val closed: Boolean = false,
    val daysToClose: Int = 0,
    val daysToDue: Int = 0,
    val paid: Boolean = false,
    val paidCents: Long? = null,
    val paidOn: String? = null,
    val paymentEntryId: Int? = null,
    val entries: List<EntryDto> = emptyList()
)

@Serializable
data class OpenStatementDto(
    val cardId: Int,
    val cardName: String,
    val color: String? = null,
    val reference: String,
    val startsOn: String,
    val closesOn: String,
    val dueOn: String,
    val totalCents: Long = 0,
    val closed: Boolean = false,
    val paid: Boolean = false,
    val statementId: Int? = null
)

@Serializable
data class InstallmentPlanDto(
    val id: Int,
    val description: String,
    val totalAmountCents: Long = 0,
    val installmentsTotal: Int = 1,
    val firstInstallmentNumber: Int = 1,
    val firstDueDate: String? = null,
    val purchasedAt: String? = null,
    val cardId: Int? = null,
    val cardName: String? = null,
    val accountId: Int? = null,
    val accountName: String? = null,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val personId: Int? = null,
    val personName: String? = null,
    val generatedCount: Int = 0,
    val paidCount: Int = 0,
    val paidCents: Long = 0,
    val remainingCents: Long = 0,
    val nextDate: String? = null,
    val entries: List<EntryDto> = emptyList()
)

@Serializable
data class RecurrenceDto(
    val id: Int,
    val description: String,
    val kind: String = "expense",
    val kindLabel: String = "Gasto",
    val amountCents: Long = 0,
    val recurrenceRule: String,
    val startsOn: String? = null,
    val endsOn: String? = null,
    val cardId: Int? = null,
    val cardName: String? = null,
    val accountId: Int? = null,
    val accountName: String? = null,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val personId: Int? = null,
    val personName: String? = null,
    val active: Boolean = true,
    val materializedThrough: String? = null
)

@Serializable
data class UpcomingOccurrenceDto(
    val recurrenceId: Int,
    val description: String,
    val kind: String = "expense",
    val amountCents: Long = 0,
    val date: String,
    val cardName: String? = null,
    val accountName: String? = null,
    val categoryName: String? = null,
    val materialized: Boolean = false,
    val entryId: Int? = null
)

@Serializable
data class ProductDto(
    val id: Int,
    val name: String,
    val brand: String? = null,
    val unit: String? = null,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val barcode: String? = null,
    val notes: String? = null,
    val archived: Boolean = false,
    val tags: List<String>? = null,
    val lastPaidCents: Long? = null,
    val lastPaidOn: String? = null
)

@Serializable
data class ProductCategoryDto(
    val id: Int,
    val parentId: Int? = null,
    val name: String,
    val slug: String,
    val position: Int = 0,
    val active: Boolean = true,
    val productCount: Int? = null
)

@Serializable
data class PriceHistoryDto(
    val itemId: Int,
    val entryId: Int,
    val date: String? = null,
    val description: String? = null,
    val quantity: Double = 1.0,
    val unitPriceCents: Long = 0,
    val totalCents: Long = 0
)

@Serializable
data class CategoryReportRowDto(
    val categoryId: Int? = null,
    val name: String,
    val color: String? = null,
    val totalCents: Long = 0,
    val accountCents: Long = 0,
    val cardCents: Long = 0,
    val entries: Int = 0,
    val share: Double = 0.0
)

@Serializable
data class TagReportRowDto(
    val tagId: Int,
    val name: String,
    val color: String? = null,
    val totalCents: Long = 0,
    val accountCents: Long = 0,
    val cardCents: Long = 0,
    val entries: Int = 0,
    val share: Double = 0.0
)

@Serializable
data class MethodReportRowDto(
    val method: String,
    val name: String,
    val totalCents: Long = 0,
    val entries: Int = 0,
    val share: Double = 0.0
)

@Serializable
data class MonthReportRowDto(
    val month: String,
    val expenseCents: Long = 0,
    val expenseAccountCents: Long = 0,
    val expenseCardCents: Long = 0,
    val incomeCents: Long = 0,
    val netCents: Long = 0
)

@Serializable
data class MonthReportMiscDto(
    val source: String? = null,
    val averageExpenseCents: Long = 0,
    val maxExpenseCents: Long = 0,
    val totalExpenseCents: Long = 0,
    val totalIncomeCents: Long = 0,
    val totalExpenseAccountCents: Long = 0,
    val totalExpenseCardCents: Long = 0
)

@Serializable
data class ReportWindowDto(
    val from: String? = null,
    val to: String? = null,
    val kind: String? = null,
    val source: String? = null,
    val totalCents: Long = 0,
    val accountCents: Long = 0,
    val cardCents: Long = 0
)

@Serializable
data class MiscEnvelope<B, M>(
    val message: String? = null,
    val body: B? = null,
    val misc: M? = null,
    val error: String? = null,
    val success: Boolean = false
)

@Serializable
data class PersonDto(
    val id: Int,
    val name: String,
    val nickname: String? = null
)

@Serializable
data class EntryDraftDto(
    val type: String = "entry",
    val kind: String = "expense",
    val amountCents: Long? = null,
    val description: String = "",
    val date: String = "",
    val paid: Boolean = true,
    val paymentHint: String? = null,
    val accountId: Int? = null,
    val counterAccountId: Int? = null,
    val cardId: Int? = null,
    val categoryId: Int? = null,
    val newCategory: DraftCategoryDto? = null,
    val personId: Int? = null,
    val personRole: String? = null,
    val notes: String? = null,
    val tags: List<DraftTagDto> = emptyList(),
    val installment: DraftInstallmentDto? = null,
    val recurrence: DraftRecurrenceDto? = null,
    val items: List<DraftItemDto> = emptyList(),
    val text: String = "",
    val warnings: List<String> = emptyList()
)

@Serializable
data class DraftCategoryDto(
    val name: String,
    val kind: String = "expense"
)

@Serializable
data class DraftTagDto(
    val name: String,
    val id: Int? = null,
    val isNew: Boolean = false
)

@Serializable
data class DraftInstallmentDto(
    val total: Int = 2,
    val firstInstallmentNumber: Int = 1,
    val amountIs: String = "total",
    val firstDueDate: String? = null,
    val backfill: Boolean = false
)

@Serializable
data class DraftRecurrenceDto(
    val frequency: String = "monthly",
    val startsOn: String? = null,
    val endsOn: String? = null
)

@Serializable
data class DraftItemDto(
    val name: String,
    val quantity: Double = 1.0,
    val unitPriceCents: Long = 0,
    val totalCents: Long = 0,
    val productId: Int? = null
)

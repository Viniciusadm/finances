package com.archieapps.finances.ui.screen.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.PersonDto
import com.archieapps.finances.data.net.RecentChoices
import com.archieapps.finances.data.net.SessionStore
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

enum class Target { Account, Card }

private val InstallmentSuffix = Regex(" \\(\\d+/\\d+\\)$")

val PaymentMethods = listOf(
    "pix" to "Pix",
    "debit" to "Débito",
    "cash" to "Dinheiro"
)

private val StorablePaymentMethods = PaymentMethods.map { it.first }

data class ComposerState(
    val editingId: Int? = null,
    val kind: String = "expense",
    val integerDigits: String = "",
    val centsDigits: String = "",
    val typingCents: Boolean = false,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val categoryId: Int? = null,
    val target: Target = Target.Account,
    val accountId: Int? = null,
    val counterAccountId: Int? = null,
    val cardId: Int? = null,
    val paymentMethod: String? = null,
    val paid: Boolean = true,
    val notes: String = "",
    val tags: String = "",
    val personId: Int? = null,
    val personRole: String? = null,
    val installmentPlanId: Int? = null,
    val installmentNumber: Int? = null,
    val installmentsTotal: Int? = null,
    val recurrenceId: Int? = null,
    val applyToPlan: Boolean = false,
    val expanded: Boolean = false,
    val accounts: List<AccountDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val people: List<PersonDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
) {
    val isInstallment: Boolean get() = installmentPlanId != null && installmentNumber != null

    val isOccurrence: Boolean get() = recurrenceId != null

    val amountCents: Long get() = Money.fromTyped(integerDigits, centsDigits)

    val hasAmount: Boolean get() = integerDigits.isNotEmpty() || centsDigits.isNotEmpty()

    val isTransfer: Boolean get() = kind == "transfer"

    val visibleCategories: List<CategoryDto>
        get() = categories.filter { it.kind == kind || it.kind == "both" }

    val showsPaymentMethod: Boolean
        get() = !isTransfer && target == Target.Account && accountId != null

    fun methodFor(accountId: Int?, fallback: String?): String? {
        if (accounts.firstOrNull { it.id == accountId }?.kind == "cash") {
            return "cash"
        }

        return fallback?.takeIf { it in StorablePaymentMethods }
    }

    val canSave: Boolean
        get() = amountCents > 0L && when {
            isTransfer -> accountId != null && counterAccountId != null && accountId != counterAccountId
            else -> true
        }
}

class ComposerViewModel(
    private val repository: FinanceRepository,
    private val sessionStore: SessionStore,
    private val editingId: Int?
) : ViewModel() {

    private val _state = MutableStateFlow(ComposerState(editingId = editingId))
    val state: StateFlow<ComposerState> = _state.asStateFlow()

    private var clientToken: String = UUID.randomUUID().toString()

    private var deleted = false

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val accounts = repository.accounts()
            val cards = repository.cards()
            val categories = repository.categories()
            val people = repository.people()
            val recent = sessionStore.recent.first()

            val failure = listOf(accounts, cards, categories)
                .filterIsInstance<ApiResult.Failure>()
                .firstOrNull()

            _state.update { current ->
                current.copy(
                    accounts = (accounts as? ApiResult.Ok)?.value ?: emptyList(),
                    cards = (cards as? ApiResult.Ok)?.value ?: emptyList(),
                    categories = (categories as? ApiResult.Ok)?.value ?: emptyList(),
                    people = (people as? ApiResult.Ok)?.value ?: emptyList(),
                    loading = false,
                    error = failure?.message
                )
            }

            if (editingId == null) {
                applyRecent(recent)
            } else {
                loadEntry(editingId)
            }
        }
    }

    private fun applyRecent(recent: RecentChoices) {
        _state.update { current ->
            val account = recent.accountId?.takeIf { id -> current.accounts.any { it.id == id } }
                ?: current.accounts.firstOrNull()?.id
            val card = recent.cardId?.takeIf { id -> current.cards.any { it.id == id } }
            val category = recent.categoryId?.takeIf { id -> current.categories.any { it.id == id } }

            current.copy(
                kind = recent.kind.takeIf { it != "transfer" } ?: "expense",
                accountId = account,
                cardId = card,
                categoryId = category,
                paymentMethod = current.methodFor(account, recent.paymentMethod)
            )
        }
    }

    private suspend fun loadEntry(id: Int) {
        when (val result = repository.entry(id)) {
            is ApiResult.Ok -> {
                val entry = result.value

                _state.update { current ->
                    current.copy(
                        kind = entry.kind,
                        integerDigits = (entry.amountCents / 100).toString(),
                        centsDigits = (entry.amountCents % 100).toString().padStart(2, '0'),
                        typingCents = false,
                        description = entry.description,
                        date = runCatching { LocalDate.parse(entry.date) }.getOrDefault(LocalDate.now()),
                        categoryId = entry.categoryId,
                        target = if (entry.cardId != null) Target.Card else Target.Account,
                        accountId = entry.accountId,
                        counterAccountId = entry.counterAccountId,
                        cardId = entry.cardId,
                        paymentMethod = entry.paymentMethod.takeIf { it in StorablePaymentMethods },
                        paid = entry.paid,
                        notes = entry.notes.orEmpty(),
                        tags = entry.tags?.joinToString(", ").orEmpty(),
                        personId = entry.personId,
                        personRole = entry.personRole,
                        installmentPlanId = entry.installmentPlanId,
                        installmentNumber = entry.installmentNumber,
                        installmentsTotal = entry.installmentsTotal,
                        recurrenceId = entry.recurrenceId,
                        expanded = entry.notes != null || !entry.tags.isNullOrEmpty() || entry.personId != null
                    )
                }
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
        }
    }

    fun acknowledgeSaved() {
        clientToken = UUID.randomUUID().toString()

        if (editingId != null) {
            _state.update { it.copy(done = false, applyToPlan = false, error = null) }

            if (!deleted) {
                viewModelScope.launch { loadEntry(editingId) }
            }

            return
        }

        _state.update { current ->
            current.copy(
                done = false,
                applyToPlan = false,
                integerDigits = "",
                centsDigits = "",
                typingCents = false,
                description = "",
                notes = "",
                tags = "",
                personId = null,
                personRole = null,
                date = LocalDate.now(),
                expanded = false,
                error = null
            )
        }
    }

    fun onDigit(digit: Char) = _state.update { current ->
        if (current.typingCents) {
            if (current.centsDigits.length >= 2) {
                current
            } else {
                current.copy(centsDigits = current.centsDigits + digit, error = null)
            }
        } else {
            val next = (current.integerDigits + digit).trimStart('0')
            current.copy(integerDigits = next.take(9), error = null)
        }
    }

    fun onCents() = _state.update { it.copy(typingCents = true, error = null) }

    fun onBackspace() = _state.update { current ->
        when {
            current.centsDigits.isNotEmpty() -> current.copy(centsDigits = current.centsDigits.dropLast(1), error = null)
            current.typingCents -> current.copy(typingCents = false, error = null)
            else -> current.copy(integerDigits = current.integerDigits.dropLast(1), error = null)
        }
    }

    fun onKind(kind: String) = _state.update { current ->
        current.copy(
            kind = kind,
            categoryId = if (kind == "transfer") null else current.categoryId,
            target = if (kind == "transfer") Target.Account else current.target,
            error = null
        )
    }

    fun onDescription(value: String) = _state.update { it.copy(description = value) }

    fun onDate(value: LocalDate) = _state.update { it.copy(date = value) }

    fun onCategory(id: Int?) = _state.update { it.copy(categoryId = id) }

    fun onTarget(target: Target) = _state.update { current ->
        current.copy(
            target = target,
            cardId = if (target == Target.Card) current.cardId ?: current.cards.firstOrNull()?.id else null,
            accountId = if (target == Target.Account) current.accountId ?: current.accounts.firstOrNull()?.id else null
        )
    }

    fun onAccount(id: Int?) = _state.update {
        it.copy(accountId = id, paymentMethod = it.methodFor(id, it.paymentMethod))
    }

    fun onPaymentMethod(value: String) = _state.update { it.copy(paymentMethod = value) }

    fun onCounterAccount(id: Int?) = _state.update { it.copy(counterAccountId = id) }

    fun onCard(id: Int?) = _state.update { it.copy(cardId = id) }

    fun onPaid(paid: Boolean) = _state.update { it.copy(paid = paid) }

    fun onNotes(value: String) = _state.update { it.copy(notes = value) }

    fun onPerson(id: Int?) = _state.update { current ->
        current.copy(
            personId = if (current.personId == id) null else id,
            personRole = if (current.personId == id) null else current.personRole
        )
    }

    fun onPersonRole(role: String?) = _state.update { current ->
        current.copy(personRole = if (current.personRole == role) null else role)
    }

    fun onTags(value: String) = _state.update { it.copy(tags = value) }

    fun toggleExpanded() = _state.update { it.copy(expanded = !it.expanded) }

    fun toggleApplyToPlan() = _state.update { it.copy(applyToPlan = !it.applyToPlan) }

    fun save() {
        val current = _state.value
        if (!current.canSave || current.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val tags = current.tags.split(',').map(String::trim).filter(String::isNotEmpty)

            val body = jsonBody {
                put("amount", current.amountCents)
                put("kind", current.kind)
                put("date", current.date.toString())
                put("paid", current.paid)
                current.description.trim().takeIf { it.isNotEmpty() }?.let { put("description", it) }

                if (current.isTransfer) {
                    current.accountId?.let { put("accountId", it) }
                    current.counterAccountId?.let { put("counterAccountId", it) }
                } else {
                    current.categoryId?.let { put("categoryId", it) }

                    if (current.target == Target.Card) {
                        current.cardId?.let { put("cardId", it) }
                    } else {
                        current.accountId?.let { put("accountId", it) }
                        current.paymentMethod?.let { put("paymentMethod", it) }
                    }
                }

                current.notes.trim().takeIf { it.isNotEmpty() }?.let { put("notes", it) }
                current.personId?.let { put("personId", it) }
                current.personRole?.let { put("personRole", it) }

                if (tags.isNotEmpty()) {
                    put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
                }

                if (current.editingId == null) {
                    put("clientToken", clientToken)
                }
            }

            val result = when {
                current.editingId != null -> repository.updateEntry(current.editingId, body)
                current.isTransfer -> repository.createTransfer(body)
                else -> repository.createEntry(body)
            }

            when (result) {
                is ApiResult.Ok -> {
                    sessionStore.rememberChoices(
                        RecentChoices(
                            kind = current.kind,
                            categoryId = current.categoryId,
                            accountId = current.accountId,
                            cardId = if (current.target == Target.Card) current.cardId else null,
                            paymentMethod = current.paymentMethod
                        )
                    )

                    val planFailure = spreadOverPlan(current, tags)

                    if (planFailure != null) {
                        _state.update { it.copy(saving = false, error = planFailure) }
                    } else {
                        _state.update { it.copy(saving = false, done = true) }
                    }
                }
                is ApiResult.Failure -> {
                    clientToken = UUID.randomUUID().toString()
                    _state.update { it.copy(saving = false, error = result.message) }
                }
            }
        }
    }

    private suspend fun spreadOverPlan(current: ComposerState, tags: List<String>): String? {
        val planId = current.installmentPlanId

        if (!current.applyToPlan || planId == null) {
            return null
        }

        val planDescription = current.description.trim().replace(InstallmentSuffix, "").trim()

        val body = jsonBody {
            planDescription.takeIf { it.isNotEmpty() }?.let { put("description", it) }
            current.categoryId?.let { put("categoryId", it) }

            if (current.target == Target.Card) {
                current.cardId?.let { put("cardId", it) }
            } else {
                current.accountId?.let { put("accountId", it) }
            }

            if (tags.isNotEmpty()) {
                put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
            }
        }

        return (repository.updateInstallmentPlan(planId, body) as? ApiResult.Failure)?.message
    }

    fun delete(scope: DeleteScope) {
        val current = _state.value
        val id = current.editingId ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val result = when (scope) {
                DeleteScope.Entry -> repository.deleteEntry(id)
                DeleteScope.Plan -> current.installmentPlanId
                    ?.let { repository.deleteInstallmentPlan(it) }
                    ?: repository.deleteEntry(id)
                DeleteScope.Recurrence -> current.recurrenceId
                    ?.let { repository.deleteRecurrence(it, keepEntries = false) }
                    ?: repository.deleteEntry(id)
            }

            when (result) {
                is ApiResult.Ok -> {
                    deleted = true
                    _state.update { it.copy(saving = false, done = true) }
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}

enum class DeleteScope { Entry, Plan, Recurrence }

package com.archieapps.finances.ui.screen.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.ui.components.paymentAccounts
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.EstablishmentDto
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

enum class Target { Account, Card }

private val InstallmentSuffix = Regex(" \\(\\d+/\\d+\\)$")

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
    val statementShift: Int = 0,
    val paid: Boolean = true,
    val notes: String = "",
    val placeName: String = "",
    val placeUnit: String = "",
    val establishments: List<EstablishmentDto> = emptyList(),
    val addItemsAfter: Boolean = false,
    val createdId: Int? = null,
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
    val loading: Boolean = false,
    /** Enquanto busca o lançamento a editar ou copiar, a tela mostra só o carregando (sem formulário pela metade). */
    val preparing: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
) {
    val isInstallment: Boolean get() = installmentPlanId != null && installmentNumber != null

    val isOccurrence: Boolean get() = recurrenceId != null

    val amountCents: Long get() = Money.fromTyped(integerDigits, centsDigits)

    val hasAmount: Boolean get() = integerDigits.isNotEmpty() || centsDigits.isNotEmpty()

    val isTransfer: Boolean get() = kind == "transfer"

    val isYield: Boolean get() = kind == "yield"

    val visibleCategories: List<CategoryDto>
        get() = if (isYield) emptyList() else categories.filter { it.kind == kind || it.kind == "both" }

    /** Contas que podem pagar um gasto ou receber um ganho. */
    val payingAccounts: List<AccountDto> get() = paymentAccounts(accounts)

    val canSave: Boolean
        get() = amountCents > 0L && when {
            isTransfer -> accountId != null && counterAccountId != null && accountId != counterAccountId
            isYield -> target == Target.Account && accountId != null
            target == Target.Card -> cardId != null
            else -> accountId != null && payingAccounts.any { it.id == accountId }
        }
}

class ComposerViewModel(
    private val repository: FinanceRepository,
    private val sessionStore: SessionStore,
    private val reference: com.archieapps.finances.data.repo.ReferenceData,
    private val editingId: Int?,
    private val copyFromId: Int? = null
) : ViewModel() {

    /** Itens do lançamento duplicado: gravados no novo depois de salvar. */
    private var copiedItems: List<com.archieapps.finances.data.dto.EntryItemDto> = emptyList()

    private val _state = MutableStateFlow(
        reference.snapshot.value.let { snapshot ->
            ComposerState(
                editingId = editingId,
                accounts = snapshot.accounts,
                cards = snapshot.cards,
                categories = snapshot.categories,
                people = snapshot.people,
                establishments = snapshot.establishments
            )
        }
    )
    val state: StateFlow<ComposerState> = _state.asStateFlow()

    private var clientToken: String = UUID.randomUUID().toString()

    private var deleted = false

    init {
        load()
    }

    private fun load() {
        // As listas vêm do aparelho na hora; a rede só atualiza por baixo.
        viewModelScope.launch {
            reference.snapshot.collect { snapshot ->
                _state.update { current ->
                    current.copy(
                        accounts = snapshot.accounts,
                        cards = snapshot.cards,
                        categories = snapshot.categories,
                        people = snapshot.people,
                        establishments = snapshot.establishments
                    )
                }
            }
        }

        viewModelScope.launch { reference.refresh() }

        viewModelScope.launch {
            val recent = sessionStore.recent.first()

            when {
                editingId != null -> loadEntry(editingId)
                copyFromId != null -> loadEntry(copyFromId, copy = true)
                else -> applyRecent(recent)
            }

            _state.update { it.copy(preparing = false) }
        }
    }

    private fun applyRecent(recent: RecentChoices) {
        _state.update { current ->
            val account = recent.accountId?.takeIf { id -> current.payingAccounts.any { it.id == id } }
                ?: current.payingAccounts.firstOrNull()?.id
            val card = recent.cardId?.takeIf { id -> current.cards.any { it.id == id } }
            val category = recent.categoryId?.takeIf { id -> current.categories.any { it.id == id } }

            current.copy(
                kind = recent.kind.takeIf { it != "transfer" && it != "yield" } ?: "expense",
                accountId = account,
                cardId = card,
                categoryId = category,
            )
        }
    }

    private suspend fun loadEntry(id: Int, copy: Boolean = false) {
        when (val result = repository.entry(id)) {
            is ApiResult.Ok -> {
                val entry = result.value

                if (copy) copiedItems = entry.items.orEmpty()

                _state.update { current ->
                    current.copy(
                        kind = entry.kind,
                        integerDigits = (entry.amountCents / 100).toString(),
                        centsDigits = (entry.amountCents % 100).toString().padStart(2, '0'),
                        typingCents = false,
                        description = entry.description,
                        date = if (copy) LocalDate.now() else runCatching { LocalDate.parse(entry.date) }.getOrDefault(LocalDate.now()),
                        categoryId = entry.categoryId,
                        target = if (entry.cardId != null) Target.Card else Target.Account,
                        accountId = entry.accountId,
                        counterAccountId = entry.counterAccountId,
                        cardId = entry.cardId,
                        statementShift = if (copy) 0 else entry.statementShift,
                        paid = entry.paid,
                        notes = entry.notes.orEmpty(),
                        placeName = entry.establishmentName.orEmpty(),
                        placeUnit = entry.placeName.orEmpty(),
                        tags = entry.tags?.joinToString(", ").orEmpty(),
                        personId = entry.personId,
                        personRole = entry.personRole,
                        installmentPlanId = if (copy) null else entry.installmentPlanId,
                        installmentNumber = if (copy) null else entry.installmentNumber,
                        installmentsTotal = if (copy) null else entry.installmentsTotal,
                        recurrenceId = if (copy) null else entry.recurrenceId,
                        expanded = entry.notes != null || entry.placeId != null || !entry.tags.isNullOrEmpty() || entry.personId != null
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

    fun onReais(value: String) = _state.update { it.copy(integerDigits = value.filter(Char::isDigit).trimStart('0').take(9), typingCents = false, error = null) }

    fun onCentavos(value: String) = _state.update { it.copy(centsDigits = value.filter(Char::isDigit).take(2), error = null) }

    fun onCents() = _state.update { it.copy(typingCents = true, error = null) }

    fun onBackspace() = _state.update { current ->
        when {
            current.centsDigits.isNotEmpty() -> current.copy(centsDigits = current.centsDigits.dropLast(1), error = null)
            current.typingCents -> current.copy(typingCents = false, error = null)
            else -> current.copy(integerDigits = current.integerDigits.dropLast(1), error = null)
        }
    }

    fun onKind(kind: String) = _state.update { current ->
        val accountOnly = kind == "transfer" || kind == "yield"
        val paying = current.payingAccounts

        val accountId = when (kind) {
            // Rendimento: conta de investimento ou poupança.
            "yield" -> current.accounts.firstOrNull { it.kind == "investment" || it.kind == "savings" }?.id
            // Transferência: qualquer conta.
            "transfer" -> current.accountId ?: current.accounts.firstOrNull()?.id
            // Gasto e ganho: só conta corrente.
            else -> current.accountId?.takeIf { id -> paying.any { it.id == id } } ?: paying.firstOrNull()?.id
        }

        current.copy(
            kind = kind,
            categoryId = if (accountOnly) null else current.categoryId,
            target = if (accountOnly) Target.Account else current.target,
            cardId = if (accountOnly) null else current.cardId,
            accountId = accountId,
            paid = if (kind == "yield") true else current.paid,
            personRole = if (kind == "yield") null else current.personRole,
            error = null
        )
    }

    fun onDescription(value: String) = _state.update { it.copy(description = value) }

    fun onDate(value: LocalDate) = _state.update { it.copy(date = value) }

    fun onCategory(id: Int?) = _state.update { it.copy(categoryId = id) }

    fun onTarget(target: Target) = _state.update { current ->
        current.copy(
            target = target,
            statementShift = if (target == Target.Card) current.statementShift else 0,
            cardId = if (target == Target.Card) current.cardId ?: current.cards.firstOrNull()?.id else null,
            accountId = if (target == Target.Account) {
                current.accountId?.takeIf { id -> current.payingAccounts.any { it.id == id } } ?: current.payingAccounts.firstOrNull()?.id
            } else {
                null
            }
        )
    }

    fun onAccount(id: Int?) = _state.update { it.copy(accountId = id) }

    fun onCounterAccount(id: Int?) = _state.update { it.copy(counterAccountId = id) }

    fun onCard(id: Int?) = _state.update { it.copy(cardId = id, statementShift = if (id == it.cardId) it.statementShift else 0) }

    fun onStatementShift(value: Int) = _state.update { it.copy(statementShift = value) }

    fun onPaid(paid: Boolean) = _state.update { it.copy(paid = paid) }

    fun onNotes(value: String) = _state.update { it.copy(notes = value) }

    fun onPlaceName(value: String) = _state.update { it.copy(placeName = value, placeUnit = if (value.isBlank()) "" else it.placeUnit) }

    fun onPlaceUnit(value: String) = _state.update { it.copy(placeUnit = value) }

    fun toggleItemsAfter() = _state.update { it.copy(addItemsAfter = !it.addItemsAfter) }

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
                } else if (current.isYield) {
                    current.accountId?.let { put("accountId", it) }
                } else {
                    current.categoryId?.let { put("categoryId", it) }

                    if (current.target == Target.Card) {
                        current.cardId?.let { put("cardId", it) }
                        put("statementShift", current.statementShift)
                    } else {
                        current.accountId?.let { put("accountId", it) }
                    }
                }

                if (!current.isTransfer && !current.isYield) {
                    if (current.placeName.isNotBlank()) {
                        put("place", buildJsonObject {
                            put("establishment", current.placeName.trim())
                            if (current.placeUnit.isNotBlank()) put("unit", current.placeUnit.trim()) else put("unit", JsonNull)
                        })
                    } else {
                        put("placeId", JsonNull)
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
                            paymentMethod = null
                        )
                    )

                    if (current.editingId == null && copiedItems.isNotEmpty()) {
                        (result.value as? EntryDto)?.id?.let { newId -> saveCopiedItems(newId) }
                    }

                    val planFailure = spreadOverPlan(current, tags)

                    if (planFailure != null) {
                        _state.update { it.copy(saving = false, error = planFailure) }
                    } else {
                        _state.update { it.copy(saving = false, done = true, createdId = (result.value as? EntryDto)?.id) }
                    }
                }
                is ApiResult.Failure -> {
                    clientToken = UUID.randomUUID().toString()
                    _state.update { it.copy(saving = false, error = result.message) }
                }
            }
        }
    }

    private suspend fun saveCopiedItems(entryId: Int) {
        val body = kotlinx.serialization.json.buildJsonObject {
            put("items", kotlinx.serialization.json.buildJsonArray {
                copiedItems.forEach { item ->
                    add(kotlinx.serialization.json.buildJsonObject {
                        item.productId?.let { put("productId", it) }
                        put("name", item.name)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPriceCents)
                    })
                }
            })
        }

        repository.replaceEntryItems(entryId, body)
        copiedItems = emptyList()
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

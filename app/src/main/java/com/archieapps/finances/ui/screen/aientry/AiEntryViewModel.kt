package com.archieapps.finances.ui.screen.aientry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.DraftCategoryDto
import com.archieapps.finances.data.dto.DraftItemDto
import com.archieapps.finances.data.dto.EntryDraftDto
import com.archieapps.finances.data.dto.PersonDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import com.archieapps.finances.ui.components.moneyCents
import com.archieapps.finances.ui.screen.composer.Target
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.util.UUID

enum class AiPhase { Writing, Review }

data class TagChoice(val name: String, val isNew: Boolean, val selected: Boolean = true)

data class AiEntryState(
    val phase: AiPhase = AiPhase.Writing,
    val text: String = "",
    val parsing: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val saved: Boolean = false,

    val type: String = "entry",
    val kind: String = "expense",
    val reais: String = "",
    val centavos: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val paid: Boolean = true,
    val target: Target = Target.Account,
    val accountId: Int? = null,
    val counterAccountId: Int? = null,
    val cardId: Int? = null,
    val categoryId: Int? = null,
    val newCategory: DraftCategoryDto? = null,
    val createCategory: Boolean = true,
    val personId: Int? = null,
    val personRole: String? = null,
    val notes: String = "",
    val tags: List<TagChoice> = emptyList(),
    val installments: String = "",
    val amountIs: String = "total",
    val firstDueDate: LocalDate = LocalDate.now(),
    val frequency: String = "monthly",
    val startsOn: LocalDate = LocalDate.now(),
    val items: List<DraftItemDto> = emptyList(),

    val accounts: List<AccountDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val people: List<PersonDto> = emptyList()
) {
    val amountCents: Long get() = moneyCents(reais, centavos)

    val isTransfer: Boolean get() = type == "transfer"

    val isInstallment: Boolean get() = type == "installment"

    val isRecurrence: Boolean get() = type == "recurrence"

    val installmentsTotal: Int get() = installments.toIntOrNull() ?: 0

    val visibleCategories: List<CategoryDto>
        get() = categories.filter { it.kind == kind || it.kind == "both" }

    val canParse: Boolean get() = text.trim().length >= 3 && !parsing

    val canSave: Boolean
        get() = amountCents > 0L &&
            !saving &&
            (!isInstallment || installmentsTotal >= 2) &&
            (!isTransfer || (accountId != null && counterAccountId != null && accountId != counterAccountId))
}

class AiEntryViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(AiEntryState())
    val state: StateFlow<AiEntryState> = _state.asStateFlow()

    private var clientToken: String = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            val accounts = repository.accounts()
            val cards = repository.cards()
            val categories = repository.categories()
            val people = repository.people()

            _state.update { current ->
                current.copy(
                    accounts = (accounts as? ApiResult.Ok)?.value ?: emptyList(),
                    cards = (cards as? ApiResult.Ok)?.value ?: emptyList(),
                    categories = (categories as? ApiResult.Ok)?.value ?: emptyList(),
                    people = (people as? ApiResult.Ok)?.value ?: emptyList()
                )
            }
        }
    }

    fun onText(value: String) = _state.update { it.copy(text = value, error = null) }

    fun parse() {
        val current = _state.value
        if (!current.canParse) return

        viewModelScope.launch {
            _state.update { it.copy(parsing = true, error = null) }

            when (val result = repository.parseEntryDraft(current.text.trim())) {
                is ApiResult.Ok -> _state.update { it.applyDraft(result.value) }
                is ApiResult.Failure -> _state.update { it.copy(parsing = false, error = result.message) }
            }
        }
    }

    fun backToWriting() = _state.update { it.copy(phase = AiPhase.Writing, error = null) }

    fun onKind(value: String) = _state.update { current ->
        current.copy(
            kind = value,
            type = if (value == "transfer") "transfer" else if (current.isTransfer) "entry" else current.type,
            categoryId = if (value == "transfer") null else current.categoryId,
            error = null
        )
    }

    fun onReais(value: String) = _state.update { it.copy(reais = value, error = null) }

    fun onCentavos(value: String) = _state.update { it.copy(centavos = value, error = null) }

    fun onDescription(value: String) = _state.update { it.copy(description = value) }

    fun onDate(value: LocalDate) = _state.update { it.copy(date = value) }

    fun onPaid(value: Boolean) = _state.update { it.copy(paid = value) }

    fun onAccount(id: Int) = _state.update { it.copy(target = Target.Account, accountId = id, cardId = null) }

    fun onCounterAccount(id: Int) = _state.update { it.copy(counterAccountId = id) }

    fun onCard(id: Int) = _state.update { it.copy(target = Target.Card, cardId = id, accountId = null) }

    fun onCategory(id: Int) = _state.update { current ->
        current.copy(categoryId = if (current.categoryId == id) null else id)
    }

    fun toggleCreateCategory() = _state.update { it.copy(createCategory = !it.createCategory) }

    fun onPerson(id: Int) = _state.update { current ->
        val clearing = current.personId == id
        current.copy(
            personId = if (clearing) null else id,
            personRole = if (clearing) null else current.personRole
        )
    }

    fun onPersonRole(role: String) = _state.update { current ->
        current.copy(personRole = if (current.personRole == role) null else role)
    }

    fun onNotes(value: String) = _state.update { it.copy(notes = value) }

    fun toggleTag(name: String) = _state.update { current ->
        current.copy(tags = current.tags.map { tag ->
            if (tag.name == name) tag.copy(selected = !tag.selected) else tag
        })
    }

    fun onInstallments(value: String) = _state.update { it.copy(installments = value) }

    fun onAmountIs(value: String) = _state.update { it.copy(amountIs = value) }

    fun onFirstDueDate(value: LocalDate) = _state.update { it.copy(firstDueDate = value) }

    fun onFrequency(value: String) = _state.update { it.copy(frequency = value) }

    fun onStartsOn(value: LocalDate) = _state.update { it.copy(startsOn = value) }

    fun removeItem(index: Int) = _state.update { current ->
        current.copy(items = current.items.filterIndexed { position, _ -> position != index })
    }

    fun acknowledgeSaved() {
        clientToken = UUID.randomUUID().toString()
        _state.update { current -> AiEntryState(accounts = current.accounts, cards = current.cards, categories = current.categories, people = current.people) }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.commitEntryDraft(current.toBody(clientToken))) {
                is ApiResult.Ok -> _state.update { it.copy(saving = false, saved = true) }
                is ApiResult.Failure -> {
                    clientToken = UUID.randomUUID().toString()
                    _state.update { it.copy(saving = false, error = result.message) }
                }
            }
        }
    }
}

private fun AiEntryState.applyDraft(draft: EntryDraftDto): AiEntryState {
    val cents = draft.amountCents ?: 0L
    val parsedDate = runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now())

    return copy(
        phase = AiPhase.Review,
        parsing = false,
        error = null,
        warnings = draft.warnings,
        type = draft.type,
        kind = draft.kind,
        reais = if (cents > 0L) (cents / 100).toString() else "",
        centavos = if (cents > 0L) (cents % 100).toString().padStart(2, '0') else "",
        description = draft.description,
        date = parsedDate,
        paid = draft.paid,
        target = if (draft.cardId != null) Target.Card else Target.Account,
        accountId = draft.accountId,
        counterAccountId = draft.counterAccountId,
        cardId = draft.cardId,
        categoryId = draft.categoryId,
        newCategory = draft.newCategory,
        createCategory = draft.newCategory != null,
        personId = draft.personId,
        personRole = draft.personRole,
        notes = draft.notes.orEmpty(),
        tags = draft.tags.map { TagChoice(name = it.name, isNew = it.isNew) },
        installments = draft.installment?.total?.toString().orEmpty(),
        amountIs = draft.installment?.amountIs ?: "total",
        firstDueDate = draft.installment?.firstDueDate?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        } ?: parsedDate,
        frequency = draft.recurrence?.frequency ?: "monthly",
        startsOn = draft.recurrence?.startsOn?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        } ?: parsedDate,
        items = draft.items
    )
}

private fun AiEntryState.toBody(clientToken: String) = jsonBody {
    put("clientToken", clientToken)
    put("text", text.trim())
    put("type", type)
    put("kind", kind)
    put("amountCents", amountCents)
    put("date", date.toString())
    put("paid", paid)
    description.trim().takeIf { it.isNotEmpty() }?.let { put("description", it) }
    notes.trim().takeIf { it.isNotEmpty() }?.let { put("notes", it) }

    if (isTransfer) {
        accountId?.let { put("accountId", it) }
        counterAccountId?.let { put("counterAccountId", it) }
    } else {
        if (target == Target.Card) {
            cardId?.let { put("cardId", it) }
        } else {
            accountId?.let { put("accountId", it) }
        }

        if (categoryId != null) {
            put("categoryId", categoryId)
        } else if (createCategory && newCategory != null) {
            putJsonObject("newCategory") {
                put("name", newCategory.name)
                put("kind", newCategory.kind)
            }
        }

        personId?.let { put("personId", it) }
        personRole?.let { put("personRole", it) }
    }

    val selectedTags = tags.filter { it.selected }.map { it.name }

    if (selectedTags.isNotEmpty()) {
        put("tags", JsonArray(selectedTags.map { JsonPrimitive(it) }))
    }

    if (isInstallment) {
        putJsonObject("installment") {
            put("total", installmentsTotal)
            put("firstInstallmentNumber", 1)
            put("amountIs", amountIs)
            put("firstDueDate", firstDueDate.toString())
            put("backfill", false)
        }
    }

    if (isRecurrence) {
        putJsonObject("recurrence") {
            put("frequency", frequency)
            put("startsOn", startsOn.toString())
        }
    }

    if (items.isNotEmpty() && type == "entry") {
        putJsonArray("items") {
            items.forEach { item ->
                addJsonObject {
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unitPriceCents", item.unitPriceCents)
                    put("totalCents", item.totalCents)
                    item.productId?.let { put("productId", it) }
                }
            }
        }
    }
}

package com.archieapps.finances.ui.screen.recurrences

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.RecurrenceDto
import com.archieapps.finances.data.dto.UpcomingOccurrenceDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ColorDot
import com.archieapps.finances.ui.components.DateStepper
import com.archieapps.finances.ui.components.ConfirmChoice
import com.archieapps.finances.ui.components.ConfirmDialog
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val DayMonth = DateTimeFormatter.ofPattern("dd/MM", PtBr)

@Composable
fun RecurrencesScreen(onBack: () -> Unit) {
    var confirmingDelete by remember { mutableStateOf<RecurrenceDto?>(null) }

    val model = appViewModel { container -> RecurrencesViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Recorrências",
            subtitle = "Assinaturas, salário, mensalidade",
            onBack = onBack,
            action = { TextAction("Nova", model::startCreate) }
        )

        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Chip(
                text = if (state.onlyActive) "Só ativas" else "Todas",
                active = state.onlyActive,
                onClick = model::toggleActive
            )
        }

        Spacer(Modifier.height(8.dp))
        ErrorBanner(state.error)

        if (state.notice != null) {
            Text(
                text = state.notice.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Positive,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        if (state.loading && state.recurrences.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.recurrences.isEmpty()) {
            EmptyState(
                headline = "Nenhuma recorrência",
                hint = "Cadastre o que repete todo mês. O app lança sozinho na data, e você vê o que vem antes de acontecer.",
                action = { QuietAction("Criar recorrência", model::startCreate) }
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (state.recurrences.isNotEmpty()) {
                item { SectionHeader("Cadastradas") }

                items(state.recurrences, key = { "rec-${it.id}" }) { recurrence ->
                    RecurrenceLine(recurrence = recurrence, onClick = { model.startEdit(recurrence) })
                    HairlineDivider(inset = 20)
                }
            }

            if (state.upcoming.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Próximos 60 dias")
                }

                items(state.upcoming, key = { "up-${it.recurrenceId}-${it.date}" }) { occurrence ->
                    OccurrenceLine(occurrence)
                    HairlineDivider(inset = 20)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(
            title = if (form.id == null) "Nova recorrência" else "Editar recorrência",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "O que é",
                value = form.description,
                onValueChange = { value -> model.editForm { it.copy(description = value) } },
                placeholder = "Netflix, aluguel, salário…"
            )

            MoneyInput(
                label = "Valor",
                reais = form.reais,
                centavos = form.centavos,
                onReaisChange = { value -> model.editForm { it.copy(reais = value) } },
                onCentavosChange = { value -> model.editForm { it.copy(centavos = value) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    text = "Gasto",
                    active = form.kind == "expense",
                    onClick = { model.editForm { it.copy(kind = "expense") } }
                )
                Chip(
                    text = "Ganho",
                    active = form.kind == "income",
                    onClick = { model.editForm { it.copy(kind = "income", useCard = false) } }
                )
            }

            Column {
                Eyebrow("Com que frequência")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cadence.entries.forEach { cadence ->
                        Chip(
                            text = cadence.label,
                            active = form.cadence == cadence,
                            onClick = { model.editForm { it.copy(cadence = cadence) } }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cadenceHint(form),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            DateStepper(
                label = "Primeira vez",
                value = form.startsOn,
                onChange = { value -> model.editForm { it.copy(startsOn = value) } }
            )

            Column {
                Eyebrow("Onde cai")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (form.kind == "expense") {
                        state.cards.forEach { card ->
                            Chip(
                                text = card.name,
                                active = form.useCard && form.cardId == card.id,
                                onClick = { model.editForm { it.copy(useCard = true, cardId = card.id) } },
                                leading = { ColorDot(card.color) }
                            )
                        }
                    }
                    state.accounts.forEach { account ->
                        Chip(
                            text = account.name,
                            active = !form.useCard && form.accountId == account.id,
                            onClick = { model.editForm { it.copy(useCard = false, accountId = account.id) } },
                            leading = { ColorDot(account.color) }
                        )
                    }
                }
            }

            Column {
                Eyebrow("Categoria")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.categories
                        .filter { it.kind == form.kind || it.kind == "both" }
                        .forEach { category ->
                            Chip(
                                text = category.name,
                                active = form.categoryId == category.id,
                                onClick = {
                                    model.editForm {
                                        it.copy(categoryId = if (it.categoryId == category.id) null else category.id)
                                    }
                                },
                                leading = { ColorDot(category.color) }
                            )
                        }
                }
            }

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.id == null) "Criar recorrência" else "Salvar",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )

            if (form.id != null) {
                val current = state.recurrences.firstOrNull { it.id == form.id }

                if (current != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuietAction(
                            label = "Lançar até hoje",
                            onClick = { model.materialize(current) }
                        )
                        QuietAction(
                            label = if (current.active) "Pausar" else "Retomar",
                            onClick = { model.setActive(current, !current.active) }
                        )
                    }

                    QuietAction(
                        label = "Excluir recorrência",
                        onClick = { confirmingDelete = current },
                        destructive = true
                    )
                }
            }
        }
    }

    val doomed = confirmingDelete

    if (doomed != null) {
        ConfirmDialog(
            title = "Excluir \"${doomed.description}\"?",
            message = "A regra para de gerar novos lançamentos.",
            choices = listOf(
                ConfirmChoice(
                    label = "Excluir, manter lançamentos",
                    hint = "Os que já foram lançados continuam no histórico",
                    onPick = { model.delete(doomed, true) }
                ),
                ConfirmChoice(
                    label = "Excluir com os lançamentos",
                    hint = "Apaga também tudo o que ela já gerou",
                    onPick = { model.delete(doomed, false) }
                )
            ),
            onDismiss = { confirmingDelete = null }
        )
    }
}

@Composable
private fun RecurrenceLine(recurrence: RecurrenceDto, onClick: () -> Unit) {
    LedgerRow(
        title = recurrence.description,
        subtitle = recurrenceSubtitle(recurrence),
        showMarker = false,
        navigates = true,
        dimmed = !recurrence.active,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = recurrence.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = if (recurrence.kind == "income") Positive else TextStrong
            )
        }
    )
}

@Composable
private fun OccurrenceLine(occurrence: UpcomingOccurrenceDto) {
    LedgerRow(
        title = occurrence.description,
        subtitle = listOfNotNull(
            shortDate(occurrence.date),
            occurrence.cardName ?: occurrence.accountName,
            if (occurrence.materialized) "lançada" else "prevista"
        ).joinToString(" · "),
        showMarker = false,
        dimmed = !occurrence.materialized,
        trailing = {
            MoneyText(
                cents = occurrence.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = if (occurrence.materialized) TextStrong else TextFaint
            )
        }
    )
}

private fun recurrenceSubtitle(recurrence: RecurrenceDto): String {
    val cadence = ruleLabel(recurrence.recurrenceRule, recurrence.startsOn)
    val where = recurrence.cardName ?: recurrence.accountName
    val paused = if (recurrence.active) null else "pausada"

    return listOfNotNull(cadence, where, paused).joinToString(" · ")
}

private fun ruleLabel(rule: String, startsOn: String?): String {
    val day = startsOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    return when {
        rule.contains("FREQ=WEEKLY") -> "toda ${weekdayName(day)}"
        rule.contains("FREQ=YEARLY") -> "todo ano em ${day?.format(DayMonth) ?: "?"}"
        rule.contains("FREQ=DAILY") -> "todo dia"
        else -> "todo dia ${day?.dayOfMonth ?: "?"}"
    }
}

private fun weekdayName(date: LocalDate?): String = when (date?.dayOfWeek?.value) {
    1 -> "segunda"
    2 -> "terça"
    3 -> "quarta"
    4 -> "quinta"
    5 -> "sexta"
    6 -> "sábado"
    7 -> "domingo"
    else -> "semana"
}

private fun cadenceHint(form: RecurrenceForm): String = when (form.cadence) {
    Cadence.Monthly -> "Todo dia ${form.startsOn.dayOfMonth}."
    Cadence.Weekly -> "Toda ${weekdayName(form.startsOn)}."
    Cadence.Yearly -> "Todo ano em ${form.startsOn.format(DayMonth)}."
}

private fun shortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(DayMonth) }.getOrDefault(date)

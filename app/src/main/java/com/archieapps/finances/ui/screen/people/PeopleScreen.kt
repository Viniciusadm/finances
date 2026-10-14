package com.archieapps.finances.ui.screen.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.PersonBalanceDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM/yy", PtBr)

@Composable
fun PeopleScreen(onBack: () -> Unit, onOpenEntry: (Int) -> Unit = {}) {
    val model = appViewModel { container -> PeopleViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Pessoas",
            subtitle = "Quem te deve e a quem você deve",
            onBack = onBack,
            action = { TextAction("Atualizar", model::refresh) }
        )

        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Chip(
                text = if (state.includeSettled) "Todas" else "Só em aberto",
                active = !state.includeSettled,
                onClick = model::toggleSettled
            )
        }

        if (state.balances.isNotEmpty()) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column {
                    Eyebrow("Te devem")
                    Spacer(Modifier.height(4.dp))
                    MoneyText(
                        cents = state.owedToYou,
                        size = MoneySize.Section,
                        showSymbol = false,
                        color = Positive
                    )
                }
                Column {
                    Eyebrow("Você deve")
                    Spacer(Modifier.height(4.dp))
                    MoneyText(
                        cents = state.youOwe,
                        size = MoneySize.Section,
                        showSymbol = false,
                        color = Negative
                    )
                }
            }
        }

        ErrorBanner(state.error)

        if (state.notice != null) {
            Text(
                text = state.notice.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Positive,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        if (state.loading && state.balances.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.balances.isEmpty()) {
            EmptyState(
                headline = "Nenhuma conta com ninguém",
                hint = "Ao lançar, marque a pessoa e diga se emprestou ou pegou emprestado. O saldo com cada um aparece aqui."
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.balances, key = { it.id }) { person ->
                PersonLine(person = person, onClick = { model.openDetail(person) })
                HairlineDivider(inset = 20)
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    val detail = state.detail

    if (detail != null) {
        EditorSheet(title = detail.person.name, onDismiss = model::dismissDetail) {
            Text(
                text = balanceLine(detail.person),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    detail.person.balanceCents > 0 -> Positive
                    detail.person.balanceCents < 0 -> Negative
                    else -> TextMuted
                }
            )

            if (detail.person.balanceCents != 0L) {
                HairlineDivider()

                Column {
                    Eyebrow("Pagar com")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.accounts.forEach { account ->
                            Chip(
                                text = account.name,
                                active = detail.settleAccountId == account.id,
                                onClick = { model.editDetail { it.copy(settleAccountId = account.id) } }
                            )
                        }
                    }
                }

                MoneyInput(
                    label = "Valor do acerto",
                    reais = detail.settleReais,
                    centavos = detail.settleCentavos,
                    onReaisChange = { value -> model.editDetail { it.copy(settleReais = value) } },
                    onCentavosChange = { value -> model.editDetail { it.copy(settleCentavos = value) } }
                )

                Text(
                    text = "Em branco quita o saldo inteiro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextFaint
                )

                PrimaryAction(
                    label = settleLabel(detail),
                    onClick = model::settle,
                    enabled = detail.canSettle,
                    loading = state.working
                )
            }

            HairlineDivider()

            if (detail.loading) {
                LoadingRow()
            } else if (detail.entries.isEmpty()) {
                Text(
                    text = "Nenhum lançamento com essa pessoa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                Eyebrow("Histórico")

                detail.entries.forEach { entry ->
                    EntryLine(
                        entry = entry,
                        onClick = {
                            model.dismissDetail()
                            onOpenEntry(entry.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonLine(person: PersonBalanceDto, onClick: () -> Unit) {
    LedgerRow(
        title = person.nickname?.takeIf { it.isNotBlank() } ?: person.name,
        subtitle = directionLabel(person),
        showMarker = false,
        navigates = true,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = kotlin.math.abs(person.balanceCents),
                size = MoneySize.Row,
                showSymbol = false,
                color = when {
                    person.balanceCents > 0 -> Positive
                    person.balanceCents < 0 -> Negative
                    else -> TextMuted
                }
            )
        }
    )
}

@Composable
private fun EntryLine(entry: EntryDto, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextStrong
            )
            Text(
                text = listOfNotNull(
                    shortDate(entry.date),
                    entry.personRoleLabel,
                    entry.accountName ?: entry.cardName
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        MoneyText(
            cents = entry.amountCents,
            size = MoneySize.Row,
            showSymbol = false,
            color = if (entry.kind == "income") Positive else TextStrong
        )
    }
}

private fun directionLabel(person: PersonBalanceDto): String = when {
    person.balanceCents > 0 -> "te deve"
    person.balanceCents < 0 -> "você deve"
    else -> "quitado"
}

private fun balanceLine(person: PersonBalanceDto): String {
    val amount = com.archieapps.finances.core.Money.full(kotlin.math.abs(person.balanceCents))

    return when {
        person.balanceCents > 0 -> "${person.name} te deve R$ $amount."
        person.balanceCents < 0 -> "Você deve R$ $amount para ${person.name}."
        else -> "Está quitado com ${person.name}."
    }
}

private fun settleLabel(detail: PersonDetail): String {
    val amount = com.archieapps.finances.core.Money.full(detail.settleCents)

    return if (detail.person.balanceCents > 0) {
        "Recebi R$ $amount"
    } else {
        "Paguei R$ $amount"
    }
}

private fun shortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(ShortDate) }.getOrDefault(date)

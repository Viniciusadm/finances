package com.archieapps.finances.ui.screen.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ConfirmChoice
import com.archieapps.finances.ui.components.ConfirmDialog
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.screen.composer.statementFor
import com.archieapps.finances.ui.screen.holdings.formatQuantity
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LongDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))

private fun longDate(value: String): String = runCatching { LocalDate.parse(value).format(LongDate) }.getOrDefault(value)

@Composable
fun EntryDetailScreen(
    entryId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onOpenItems: (Int) -> Unit,
    onChanged: () -> Unit,
    refreshKey: Int = 0
) {
    val model = appViewModel(key = "entry-detail-$entryId-$refreshKey") { container ->
        EntryDetailViewModel(container.repository, entryId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val entry = state.entry
    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    if (confirmingDelete && entry != null) {
        DeleteChoices(entry, onDismiss = { confirmingDelete = false }) { scope ->
            model.delete(scope, onChanged)
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = entry?.description ?: "Lançamento",
            subtitle = entry?.placeLabel,
            onBack = onBack,
            action = {
                if (entry != null) {
                    IconAction(AppIcons.Delete, onClick = { confirmingDelete = true }, contentDescription = "Excluir", tint = Negative)
                    IconAction(AppIcons.Edit, onClick = { onEdit(entry.id) }, contentDescription = "Editar")
                }
            }
        )

        ErrorBanner(state.error)

        if (state.loading && entry == null) {
            LoadingRow()
        }

        if (entry != null) {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "hero") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Eyebrow(entry.kindLabel)
                        Spacer(Modifier.height(4.dp))
                        MoneyText(
                            cents = entry.amountCents,
                            size = MoneySize.Hero,
                            color = when {
                                entry.kind == "income" || entry.kind == "yield" -> Positive
                                else -> TextStrong
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (entry.status) {
                                "done" -> "Pago"
                                "overdue" -> "Atrasado"
                                else -> "Previsto"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = when (entry.status) {
                                "done" -> Positive
                                "overdue" -> Warning
                                else -> TextMuted
                            }
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Chip(text = "Editar", active = false, icon = AppIcons.Edit, onClick = { onEdit(entry.id) })
                            if (entry.kind != "transfer" && entry.kind != "yield") {
                                Chip(text = "Itens", active = false, icon = AppIcons.Cart, onClick = { onOpenItems(entry.id) })
                            }
                            if (entry.paysStatementId == null && entry.kind != "transfer") {
                                Chip(
                                    text = if (entry.paid) "Desfazer pagamento" else "Marcar como pago",
                                    active = false,
                                    icon = AppIcons.Check,
                                    onClick = { model.togglePaid(onChanged) }
                                )
                            }
                        }
                    }
                }

                item(key = "info") {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Fact("Data", longDate(entry.date))
                        entry.time?.takeIf { it.isNotBlank() }?.let { Fact("Hora", it.take(5)) }
                        Fact("Pagamento", paymentLine(entry))
                        statementLine(entry, state.card)?.let { Fact("Fatura", it) }
                        entry.categoryName?.let { Fact("Categoria", it) }
                        entry.placeLabel?.let { Fact("Local", it) }
                        entry.personName?.let { person ->
                            Fact("Pessoa", listOfNotNull(person, entry.personRoleLabel).joinToString(" · "))
                        }
                        entry.installmentNumber?.let { Fact("Parcela", "$it de ${entry.installmentsTotal ?: "?"}") }
                        if (entry.recurrenceId != null) Fact("Origem", "Recorrência")
                        entry.tags?.takeIf { it.isNotEmpty() }?.let { Fact("Tags", it.joinToString(", ")) }
                    }
                }

                entry.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item(key = "notes-header") { SectionHeader("Observação") }
                    item(key = "notes") {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextStrong,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                val items = entry.items.orEmpty()

                if (items.isNotEmpty()) {
                    item(key = "items-header") { SectionHeader("Itens") }

                    itemsIndexed(items, key = { _, item -> "item-${item.id}" }) { index, item ->
                        Column(Modifier.grouped(index, items.size)) {
                            LedgerRow(
                                title = item.name,
                                subtitle = "${formatQuantity(item.quantity.toString())} × R$ ${Money.full(item.unitPriceCents)}",
                                showMarker = false,
                                trailing = {
                                    MoneyText(cents = item.totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                                }
                            )
                        }
                    }

                    item(key = "items-total") {
                        Column(
                            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HairlineDivider()
                            val total = items.sumOf { it.totalCents }
                            Fact("Soma dos itens", "R$ ${Money.full(total)}")
                            val difference = entry.amountCents - total
                            if (difference != 0L) {
                                Fact("Diferença para o valor", "R$ ${Money.full(difference)}")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextStrong,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun paymentLine(entry: EntryDto): String = when {
    entry.kind == "transfer" -> listOfNotNull(entry.accountName, entry.counterAccountName).joinToString(" → ")
    entry.cardName != null -> "Cartão ${entry.cardName}"
    entry.accountName != null -> entry.accountName
    entry.paysStatementId != null -> "Pagamento de fatura"
    else -> "Sem conta"
}

/** Fatura em que o lançamento cai, calculada pelo fechamento e vencimento do cartão. */
private fun statementLine(entry: EntryDto, card: com.archieapps.finances.data.dto.CardDto?): String? {
    if (entry.cardId == null || card == null) return null

    val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return null
    val label = statementFor(card.closingDay, card.dueDay, date, entry.statementShift).label()

    return when {
        entry.statementShift > 0 -> "$label (fatura seguinte)"
        entry.statementShift < 0 -> "$label (fatura anterior)"
        else -> label
    }
}

@Composable
private fun DeleteChoices(entry: EntryDto, onDismiss: () -> Unit, onPick: (DeleteScope) -> Unit) {
    val installment = entry.installmentPlanId != null && entry.installmentNumber != null
    val occurrence = entry.recurrenceId != null

    val choices = when {
        installment -> listOf(
            ConfirmChoice("Excluir só esta parcela", "As outras parcelas continuam") { onPick(DeleteScope.Entry) },
            ConfirmChoice("Excluir as ${entry.installmentsTotal ?: 0} parcelas", "Apaga o parcelamento inteiro, inclusive as já pagas") { onPick(DeleteScope.Plan) }
        )
        occurrence -> listOf(
            ConfirmChoice("Excluir só este lançamento", "A recorrência continua gerando os próximos") { onPick(DeleteScope.Entry) },
            ConfirmChoice("Excluir a recorrência inteira", "Apaga a regra e todos os lançamentos que ela gerou") { onPick(DeleteScope.Recurrence) }
        )
        else -> listOf(ConfirmChoice("Excluir") { onPick(DeleteScope.Entry) })
    }

    ConfirmDialog(
        title = if (installment) "Excluir a parcela ${entry.installmentNumber}?" else "Excluir lançamento?",
        message = entry.description,
        choices = choices,
        onDismiss = onDismiss
    )
}

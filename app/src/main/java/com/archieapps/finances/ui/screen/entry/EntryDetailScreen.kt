package com.archieapps.finances.ui.screen.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.archieapps.finances.ui.components.SurfaceCard
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.SurfaceLift
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
    onDuplicate: (Int) -> Unit = {},
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
                }
            }
        )

        ErrorBanner(state.error)

        if (state.loading && entry == null) {
            LoadingRow()
        }

        if (entry != null) {
            val items = entry.items.orEmpty()
            val statusColor = when (entry.status) {
                "done" -> Positive
                "overdue" -> Warning
                else -> TextMuted
            }
            val statusLabel = when (entry.status) {
                "done" -> if (entry.kind == "income") "Recebido" else "Pago"
                "overdue" -> "Atrasado"
                else -> "Previsto"
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "hero") {
                    SurfaceCard(contentPadding = PaddingValues(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow(entry.kindLabel, Modifier.weight(1f))
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(statusColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColor
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        MoneyText(
                            cents = entry.amountCents,
                            size = MoneySize.Hero,
                            color = if (entry.kind == "income" || entry.kind == "yield") Positive else TextStrong
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = listOfNotNull(longDate(entry.date), entry.time?.takeIf { it.isNotBlank() }?.take(5)).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                item(key = "actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionTile("Editar", AppIcons.Edit, Modifier.weight(1f)) { onEdit(entry.id) }
                            if (entry.paysStatementId == null) {
                                ActionTile("Duplicar", AppIcons.Layers, Modifier.weight(1f)) { onDuplicate(entry.id) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (entry.kind != "transfer" && entry.kind != "yield") {
                                ActionTile("Itens", AppIcons.Cart, Modifier.weight(1f)) { onOpenItems(entry.id) }
                            }
                            if (entry.paysStatementId == null && entry.kind != "transfer") {
                                ActionTile(
                                    label = if (entry.paid) "Desfazer pagamento" else "Marcar como pago",
                                    icon = AppIcons.Check,
                                    modifier = Modifier.weight(1f),
                                    onClick = { model.togglePaid(onChanged) }
                                )
                            }
                        }
                    }
                }

                item(key = "info") {
                    SurfaceCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
                        val rows = listOfNotNull(
                            "Pagamento" to paymentLine(entry),
                            statementLine(entry, state.card)?.let { "Fatura" to it },
                            entry.categoryName?.let { "Categoria" to it },
                            entry.placeLabel?.let { "Local" to it },
                            entry.personName?.let { person -> "Pessoa" to listOfNotNull(person, entry.personRoleLabel).joinToString(" · ") },
                            entry.installmentNumber?.let { "Parcela" to "$it de ${entry.installmentsTotal ?: "?"}" },
                            if (entry.recurrenceId != null) "Origem" to "Recorrência" else null,
                            entry.tags?.takeIf { it.isNotEmpty() }?.let { "Tags" to it.joinToString(", ") }
                        )

                        rows.forEachIndexed { index, (label, value) ->
                            if (index > 0) HairlineDivider()
                            Fact(label, value)
                        }
                    }
                }

                entry.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item(key = "notes") {
                        SurfaceCard(contentPadding = PaddingValues(20.dp)) {
                            Eyebrow("Observação")
                            Spacer(Modifier.height(8.dp))
                            Text(text = notes, style = MaterialTheme.typography.bodyLarge, color = TextStrong)
                        }
                    }
                }

                if (items.isNotEmpty()) {
                    item(key = "items") {
                        SurfaceCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
                            Eyebrow("Itens")
                            Spacer(Modifier.height(6.dp))

                            items.forEachIndexed { index, item ->
                                if (index > 0) HairlineDivider()

                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyLarge, color = TextStrong)
                                        Text(
                                            text = "${formatQuantity(item.quantity.toString())} × R$ ${Money.full(item.unitPriceCents)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted
                                        )
                                    }
                                    MoneyText(cents = item.totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                                }
                            }

                            val total = items.sumOf { it.totalCents }
                            val difference = entry.amountCents - total

                            if (items.size > 1 || difference != 0L) {
                                HairlineDivider()
                                Fact("Soma dos itens", "R$ ${Money.full(total)}")
                            }
                            if (difference != 0L) {
                                Fact("Diferença para o valor", "R$ ${Money.full(difference)}")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ActionTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLift)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextStrong, maxLines = 1)
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
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
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
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

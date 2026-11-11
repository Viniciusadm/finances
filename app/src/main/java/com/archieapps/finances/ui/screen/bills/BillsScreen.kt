package com.archieapps.finances.ui.screen.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.archieapps.finances.ui.components.NoticeBanner
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.StatTile
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.soft
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.BillDto
import com.archieapps.finances.ui.components.AccountChips
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
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SettleButton
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM", PtBr)

@Composable
fun BillsScreen(onBack: (() -> Unit)?, onChanged: () -> Unit = {}) {
    val model = appViewModel { container -> BillsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    val totals = state.totals

    LaunchedEffect(state.changes) {
        if (state.changes > 0) onChanged()
    }

    Refreshable(refreshing = state.loading && state.bills.isNotEmpty(), onRefresh = model::refresh) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                ScreenHeader(
                    title = "Contas",
                    subtitle = "A pagar e a receber",
                    onBack = onBack
                )
            }

            item {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillFilter.entries.forEach { filter ->
                        Chip(text = filter.label, active = state.filter == filter, onClick = { model.select(filter) })
                    }
                }
            }

            if (totals != null && state.filter != BillFilter.Done) {
                item {
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile("A pagar", totals.toPayCents, Negative, AppIcons.ArrowUp, Modifier.weight(1f))
                        StatTile("A receber", totals.toReceiveCents, Positive, AppIcons.ArrowDown, Modifier.weight(1f))
                    }
                }

                if (totals.overdueCount > 0) {
                    item {
                        Row(
                            Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Warning.soft(0.12f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.Warning, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (totals.overdueCount == 1) "1 conta vencida" else "${totals.overdueCount} contas vencidas",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                                color = TextStrong
                            )
                        }
                    }
                }
            }

            item { ErrorBanner(state.error) }
            item { NoticeBanner(state.notice) }

            if (state.loading && state.bills.isEmpty()) {
                item { LoadingRow() }
            }

            if (!state.loading && state.bills.isEmpty()) {
                item {
                    EmptyState(
                        headline = if (state.filter == BillFilter.Done) "Nada realizado ainda" else "Nada em aberto",
                        hint = "Cadastre recorrências (aluguel, assinatura, salário) e elas aparecem aqui na data. Um toque confirma o pagamento e já atualiza o saldo.",
                        icon = AppIcons.Receipt
                    )
                }
            }

            if (state.bills.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
            }

            itemsIndexed(state.bills, key = { _, bill -> bill.key }) { index, bill ->
                Column(Modifier.grouped(index, state.bills.size)) {
                    BillLine(
                        bill = bill,
                        onSettle = { model.quickSettle(bill) },
                        onOpen = if (bill.status == "done") null else ({ model.openSettle(bill) })
                    )
                }
            }

            item { Spacer(Modifier.height(110.dp)) }
        }
    }

    val settle = state.settle

    if (settle != null) {
        EditorSheet(
            title = settle.bill.description,
            onDismiss = model::dismissSettle
        ) {
            Text(
                text = listOfNotNull(
                    dueLabel(settle.bill),
                    settle.bill.personName?.let { (if (settle.receiving) "de " else "para ") + it }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )

            AccountChips(
                label = if (settle.receiving) "Entrou em" else "Saiu de",
                accounts = state.accounts,
                selectedId = settle.accountId,
                onSelect = { id -> model.editSettle { it.copy(accountId = id) } }
            )

            MoneyInput(
                label = "Valor",
                reais = settle.reais,
                centavos = settle.centavos,
                onReaisChange = { value -> model.editSettle { it.copy(reais = value) } },
                onCentavosChange = { value -> model.editSettle { it.copy(centavos = value) } }
            )

            Text(
                text = "Em branco usa o valor previsto, R$ ${Money.full(settle.bill.amountCents)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextFaint
            )

            ErrorBanner(state.error)

            PrimaryAction(
                icon = AppIcons.Check,
                label = (if (settle.receiving) "Recebi R$ " else "Paguei R$ ") + Money.full(settle.amountCents),
                onClick = model::confirmSettle,
                enabled = settle.valid,
                loading = state.working
            )

            if (settle.bill.recurrenceId != null) {
                QuietAction(label = "Pular esta vez", onClick = model::skip, icon = AppIcons.ChevronRight, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BillLine(bill: BillDto, onSettle: () -> Unit, onOpen: (() -> Unit)?) {
    val done = bill.status == "done"

    LedgerRow(
        title = bill.description,
        subtitle = listOfNotNull(
            dueLabel(bill),
            bill.installmentNumber?.let { number -> bill.installmentsTotal?.let { "$number/$it" } ?: "parcela $number" },
            bill.personName,
            bill.accountName
        ).joinToString(" · "),
        icon = when {
            done -> AppIcons.Check
            bill.status == "overdue" -> AppIcons.Warning
            bill.kind == "income" -> AppIcons.ArrowDown
            bill.recurrenceId != null -> AppIcons.Repeat
            bill.installmentNumber != null -> AppIcons.Layers
            else -> AppIcons.Receipt
        },
        markerHex = when {
            done -> "#9AA4B5"
            bill.status == "overdue" -> "#FBBF24"
            bill.kind == "income" -> "#34D399"
            else -> "#5B8CFF"
        },
        dimmed = done,
        onClick = onOpen,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyText(
                    cents = bill.amountCents,
                    size = MoneySize.Row,
                    showSymbol = false,
                    color = when {
                        done -> TextMuted
                        bill.kind == "income" -> Positive
                        else -> TextStrong
                    }
                )
                if (!done) {
                    SettleButton(receiving = bill.kind == "income", onClick = onSettle)
                }
            }
        }
    )
}

private fun dueLabel(bill: BillDto): String {
    val date = runCatching { LocalDate.parse(bill.date) }.getOrNull() ?: return bill.date
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val formatted = date.format(ShortDate)

    return when (bill.status) {
        "done" -> if (bill.kind == "income") "recebido · $formatted" else "pago · $formatted"
        "overdue" -> if (days == -1L) "venceu ontem" else "venceu há ${-days} dias · $formatted"
        else -> when (days) {
            0L -> "vence hoje"
            1L -> "vence amanhã"
            else -> "vence $formatted"
        }
    }
}

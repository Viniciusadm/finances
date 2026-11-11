package com.archieapps.finances.ui.screen.bills

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
fun BillsScreen(onBack: () -> Unit, onChanged: () -> Unit = {}) {
    val model = appViewModel { container -> BillsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    val totals = state.totals

    LaunchedEffect(state.changes) {
        if (state.changes > 0) onChanged()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Contas a pagar e receber",
            subtitle = "Recorrências, parcelas e previstos",
            onBack = onBack,
            action = { TextAction("Atualizar", model::refresh) }
        )

        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BillFilter.entries.forEach { filter ->
                Chip(text = filter.label, active = state.filter == filter, onClick = { model.select(filter) })
            }
        }

        if (totals != null && state.filter != BillFilter.Done) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column {
                    Eyebrow("A pagar")
                    Spacer(Modifier.height(4.dp))
                    MoneyText(cents = totals.toPayCents, size = MoneySize.Section, showSymbol = false, color = Negative)
                }
                Column {
                    Eyebrow("A receber")
                    Spacer(Modifier.height(4.dp))
                    MoneyText(cents = totals.toReceiveCents, size = MoneySize.Section, showSymbol = false, color = Positive)
                }
                if (totals.overdueCount > 0) {
                    Column {
                        Eyebrow("Vencidas", color = Warning)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${totals.overdueCount}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Warning
                        )
                    }
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

        if (state.loading && state.bills.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.bills.isEmpty()) {
            EmptyState(
                headline = if (state.filter == BillFilter.Done) "Nada realizado ainda" else "Nada em aberto",
                hint = "Cadastre recorrências (aluguel, assinatura, salário) e elas aparecem aqui na data. Um toque confirma o pagamento e já atualiza o saldo."
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.bills, key = { it.key }) { bill ->
                BillLine(
                    bill = bill,
                    onSettle = { model.quickSettle(bill) },
                    onOpen = if (bill.status == "done") null else ({ model.openSettle(bill) })
                )
                HairlineDivider(inset = 20)
            }

            item { Spacer(Modifier.height(40.dp)) }
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
                label = (if (settle.receiving) "Recebi R$ " else "Paguei R$ ") + Money.full(settle.amountCents),
                onClick = model::confirmSettle,
                enabled = settle.valid,
                loading = state.working
            )

            if (settle.bill.recurrenceId != null) {
                QuietAction(label = "Pular esta vez", onClick = model::skip)
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
        showMarker = false,
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

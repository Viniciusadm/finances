package com.archieapps.finances.ui.screen.accounts

import androidx.compose.foundation.lazy.itemsIndexed
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Primary
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountFlowDto
import com.archieapps.finances.data.dto.AccountMonthFlowDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EditorSheet
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
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM", PtBr)

@Composable
fun AccountDetailScreen(
    accountId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onOpenEntry: (Int) -> Unit,
    refreshKey: Int = 0
) {
    val model = appViewModel(key = "account-$accountId-$refreshKey") { container ->
        AccountDetailViewModel(container.repository, accountId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val history = state.history
    val account = history?.account

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = account?.name ?: "Conta",
            subtitle = listOfNotNull(account?.institution, account?.kindLabel).joinToString(" · ").ifBlank { null },
            onBack = onBack,
            action = { IconAction(AppIcons.Edit, onClick = { onEdit(accountId) }, contentDescription = "Editar conta") }
        )

        ErrorBanner(state.error)

        if (state.notice != null) {
            Text(
                text = state.notice.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Positive,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        if (state.loading && history == null) {
            LoadingRow()
        }

        if (history != null) {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "balance") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Eyebrow("Saldo agora")
                        Spacer(Modifier.height(4.dp))
                        MoneyText(
                            cents = history.account.balanceCents ?: history.account.initialBalanceCents,
                            size = MoneySize.Hero,
                            color = TextStrong
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Chip(
                                text = "Atualizar saldo",
                                active = false,
                                onClick = { model.open(AccountSheet.Reconcile) }
                            )
                            Chip(
                                text = "Lançar rendimento",
                                active = false,
                                onClick = { model.open(AccountSheet.Yield) }
                            )
                        }
                    }
                }

                item(key = "composition") {
                    SectionHeader("De onde veio o saldo")
                    Composition(history.allTime, history.account.initialBalanceCents)
                }

                if (history.months.any { it.changeCents != 0L || it.depositsCents != 0L || it.withdrawalsCents != 0L }) {
                    item(key = "months-header") { SectionHeader("Mês a mês") }

                    itemsIndexed(history.months.reversed(), key = { _, it -> "month-${it.month}" }) { index, month ->
                        Column(Modifier.grouped(index, history.months.reversed().size)) {
                            MonthLine(month)
                        }
                    }
                }

                item(key = "entries-header") { SectionHeader("Movimentações") }

                if (history.entries.isEmpty()) {
                    item(key = "entries-empty") {
                        Text(
                            text = "Nada movimentou esta conta ainda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                itemsIndexed(history.entries, key = { _, it -> "entry-${it.id}" }) { index, entry ->
                    Column(Modifier.grouped(index, history.entries.size)) {
                        EntryLine(entry = entry, onClick = { onOpenEntry(entry.id) })
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(
            title = if (form.sheet == AccountSheet.Reconcile) "Atualizar saldo" else "Lançar rendimento",
            onDismiss = model::dismiss
        ) {
            Text(
                text = if (form.sheet == AccountSheet.Reconcile) {
                    "Diga quanto a conta tem agora. A diferença para o saldo calculado vira rendimento, com a data de hoje."
                } else {
                    "Quanto rendeu desde a última vez. Fica registrado com a data de hoje e não conta como ganho."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            MoneyInput(
                label = if (form.sheet == AccountSheet.Reconcile) "Saldo atual" else "Rendimento",
                reais = form.reais,
                centavos = form.centavos,
                onReaisChange = { value -> model.edit { it.copy(reais = value, confirmLoss = false) } },
                onCentavosChange = { value -> model.edit { it.copy(centavos = value, confirmLoss = false) } }
            )

            val preview = form.preview

            if (form.sheet == AccountSheet.Reconcile && preview != null) {
                Text(
                    text = previewLine(preview.differenceCents),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        preview.differenceCents > 0 -> Positive
                        preview.differenceCents < 0 -> Warning
                        else -> TextMuted
                    }
                )

                if (form.needsLossConfirmation) {
                    Chip(
                        text = "Confirmo o ajuste de R$ ${Money.full(-preview.differenceCents)}",
                        active = form.confirmLoss,
                        onClick = { model.edit { it.copy(confirmLoss = !it.confirmLoss) } }
                    )
                }
            }

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.sheet == AccountSheet.Reconcile) "Atualizar" else "Registrar rendimento",
                onClick = model::submit,
                enabled = form.valid,
                loading = state.working
            )

            QuietAction(label = "Cancelar", onClick = model::dismiss)
        }
    }
}

@Composable
private fun Composition(flow: AccountFlowDto, initialCents: Long) {
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowLine("Saldo inicial", initialCents, TextStrong)
        FlowLine("Depósitos e entradas", flow.depositsCents, Positive, "+")
        FlowLine("Rendimentos", flow.yieldCents, Positive, "+")
        FlowLine("Saídas e retiradas", flow.withdrawalsCents, Negative, "−")
        HairlineDivider()
        FlowLine("Saldo", flow.closingCents, TextStrong)
    }
}

@Composable
private fun FlowLine(label: String, cents: Long, color: Color, prefix: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = MaterialTheme.typography.bodyLarge, color = color)
        }
        MoneyText(cents = cents, size = MoneySize.Row, showSymbol = false, color = color)
    }
}

@Composable
private fun MonthLine(month: AccountMonthFlowDto) {
    LedgerRow(
        title = monthLabel(month.month),
        subtitle = listOfNotNull(
            month.depositsCents.takeIf { it > 0 }?.let { "+${Money.full(it)} depósitos" },
            month.yieldCents.takeIf { it > 0 }?.let { "+${Money.full(it)} rendimento" },
            month.withdrawalsCents.takeIf { it > 0 }?.let { "−${Money.full(it)} saídas" }
        ).joinToString(" · ").ifBlank { "sem movimento" },
        showMarker = false,
        trailing = {
            MoneyText(
                cents = month.closingCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = TextStrong
            )
        }
    )
}

@Composable
private fun EntryLine(entry: EntryDto, onClick: () -> Unit) {
    val signed = entry.accountSignedAmountCents ?: entry.signedAmountCents

    LedgerRow(
        title = entry.description,
        subtitle = listOfNotNull(
            shortDate(entry.date),
            entry.kindLabel,
            if (entry.paid) null else "previsto"
        ).joinToString(" · "),
        markerHex = entry.color,
        dimmed = !entry.paid,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = signed,
                size = MoneySize.Row,
                showSymbol = false,
                color = when {
                    entry.kind == "yield" -> Positive
                    signed > 0 -> Positive
                    else -> TextStrong
                }
            )
        }
    )
}

private fun previewLine(differenceCents: Long): String = when {
    differenceCents > 0 -> "Rendeu R$ ${Money.full(differenceCents)}."
    differenceCents < 0 -> "Está R$ ${Money.full(-differenceCents)} abaixo do calculado."
    else -> "Igual ao saldo calculado."
}

private fun monthLabel(value: String): String = runCatching {
    val month = YearMonth.parse(value)
    month.month.getDisplayName(TextStyle.FULL, PtBr).replaceFirstChar { it.titlecase(PtBr) } + " ${month.year}"
}.getOrDefault(value)

private fun shortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(ShortDate) }.getOrDefault(date)

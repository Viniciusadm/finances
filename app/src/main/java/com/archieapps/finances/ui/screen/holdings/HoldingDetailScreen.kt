package com.archieapps.finances.ui.screen.holdings

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.HoldingDto
import com.archieapps.finances.data.dto.HoldingPreviewDto
import com.archieapps.finances.ui.components.AccountChips
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.DateChips
import com.archieapps.finances.ui.components.ConfirmChoice
import com.archieapps.finances.ui.components.ConfirmDialog
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.IconAction
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
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ShortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun date(value: String): String = runCatching { LocalDate.parse(value).format(ShortDate) }.getOrDefault(value)

private fun tone(value: Long): Color = when {
    value > 0 -> Positive
    value < 0 -> Negative
    else -> TextMuted
}

@Composable
fun HoldingDetailScreen(
    holdingId: Int,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val model = appViewModel(key = "holding-$holdingId") { container ->
        HoldingDetailViewModel(container.repository, holdingId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val holding = state.holding
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = holding?.symbol ?: "Ativo",
            subtitle = holding?.name,
            onBack = onBack,
            action = {
                if (holding != null) {
                    IconAction(AppIcons.Delete, onClick = { confirmDelete = true }, contentDescription = "Remover ativo")
                }
            }
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

        if (state.loading && holding == null) {
            LoadingRow()
        }

        if (holding != null) {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "hero") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Eyebrow("Valor da sua posição")
                        Spacer(Modifier.height(4.dp))
                        MoneyText(cents = holding.valueCents, size = MoneySize.Hero, color = TextStrong)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatQuantity(holding.quantity)} × R$ ${formatPrice(holding.price)} (preço atual)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Chip(text = "Comprar", active = false, onClick = { model.open(HoldingSheet.Buy) })
                            Chip(text = "Vender", active = false, onClick = { model.open(HoldingSheet.Sell) })
                            Chip(text = "Atualizar preço", active = false, onClick = { model.open(HoldingSheet.Price) })
                            Chip(text = "Dividendo", active = false, onClick = { model.open(HoldingSheet.Dividend) })
                        }
                    }
                }

                item(key = "summary") {
                    SectionHeader("Resumo")
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryLine("Quantidade", formatQuantity(holding.quantity))
                        SummaryLine("Preço médio que você pagou", "R$ ${formatPrice(holding.avgPrice)}")
                        SummaryLine("Preço de hoje", "R$ ${formatPrice(holding.price)}")
                        SummaryLine("Quanto você pagou no total", "R$ ${Money.full(holding.costCents)}")
                        SummaryLine("Dividendos recebidos", "R$ ${Money.full(holding.dividendsCents)}", color = if (holding.dividendsCents > 0) Positive else TextMuted)
                        SummaryLine(
                            label = "Lucro ou prejuízo",
                            value = "${formatSigned(holding.profitCents)} (${formatPercent(holding.profitPercent)})",
                            color = tone(holding.profitCents)
                        )
                    }
                }

                item(key = "prices-header") { SectionHeader("Histórico de preço") }

                if (holding.prices.isEmpty()) {
                    item(key = "prices-empty") { Muted("Nenhum preço registrado ainda.") }
                }

                itemsIndexed(holding.prices, key = { _, point -> "price-${point.id}" }) { index, point ->
                    Column(Modifier.grouped(index, holding.prices.size)) {
                        LedgerRow(
                            title = "R$ ${formatPrice(point.price)}",
                            subtitle = date(point.recordedOn),
                            showMarker = false,
                            trailing = {
                                val change = point.changePercent

                                if (change != null && change != 0.0) {
                                    Text(
                                        text = (if (change > 0) "▲ " else "▼ ") + formatPercent(change),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (change > 0) Positive else Negative
                                    )
                                }
                            }
                        )
                    }
                }

                item(key = "trades-header") { SectionHeader("Compras e vendas") }

                if (holding.trades.isEmpty()) {
                    item(key = "trades-empty") { Muted("Nenhuma compra ou venda registrada ainda.") }
                }

                itemsIndexed(holding.trades, key = { _, trade -> "trade-${trade.id}" }) { index, trade ->
                    Column(Modifier.grouped(index, holding.trades.size)) {
                        LedgerRow(
                            title = (if (trade.side == "buy") "Compra" else "Venda") + " de ${formatQuantity(trade.quantity)}",
                            subtitle = "${date(trade.date)} · a R$ ${formatPrice(trade.unitPrice)} cada",
                            showMarker = false,
                            trailing = {
                                MoneyText(
                                    cents = trade.totalCents,
                                    size = MoneySize.Row,
                                    showSymbol = false,
                                    color = if (trade.side == "buy") TextStrong else Positive
                                )
                            }
                        )
                    }
                }

                item(key = "dividends-header") { SectionHeader("Dividendos") }

                if (holding.dividends.isEmpty()) {
                    item(key = "dividends-empty") { Muted("Nenhum dividendo registrado ainda.") }
                }

                itemsIndexed(holding.dividends, key = { _, dividend -> "dividend-${dividend.id}" }) { index, dividend ->
                    Column(Modifier.grouped(index, holding.dividends.size)) {
                        LedgerRow(
                            title = "Dividendo",
                            subtitle = "${date(dividend.date)} · caiu na conta ${dividend.accountName ?: "corrente"}",
                            showMarker = false,
                            trailing = {
                                MoneyText(cents = dividend.amountCents, size = MoneySize.Row, showSymbol = false, color = Positive)
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    val form = state.form

    if (form != null && holding != null) {
        TradeSheet(
            holding = holding,
            form = form,
            accounts = state.accounts.filter { it.id != holding.accountId },
            dividendAccountName = model.dividendAccount(state)?.name,
            working = state.working,
            error = state.error,
            onEdit = model::edit,
            onSubmit = { model.submit(onChanged) },
            onDismiss = model::dismiss
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Remover ${holding?.symbol.orEmpty()}?",
            message = "As compras, vendas e variações que já aparecem no extrato continuam lá. Só o ativo some da lista.",
            choices = listOf(ConfirmChoice(label = "Remover ativo") {
                confirmDelete = false
                model.delete(onChanged)
            }),
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun TradeSheet(
    holding: HoldingDto,
    form: HoldingForm,
    accounts: List<com.archieapps.finances.data.dto.AccountDto>,
    dividendAccountName: String?,
    working: Boolean,
    error: String?,
    onEdit: ((HoldingForm) -> HoldingForm) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (form.sheet) {
        HoldingSheet.Buy -> "Comprar ${holding.symbol}"
        HoldingSheet.Sell -> "Vender ${holding.symbol}"
        HoldingSheet.Price -> "Atualizar preço de ${holding.symbol}"
        HoldingSheet.Dividend -> "Dividendo de ${holding.symbol}"
    }

    EditorSheet(title = title, onDismiss = onDismiss) {

        if (form.sheet == HoldingSheet.Dividend) {
            MoneyInput(
                label = "Valor recebido",
                reais = form.reais,
                centavos = form.centavos,
                onReaisChange = { value -> onEdit { it.copy(reais = value) } },
                onCentavosChange = { value -> onEdit { it.copy(centavos = value) } }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Data do recebimento")
                DateChips(date = form.date, onChange = { value -> onEdit { it.copy(date = value) } })
            }

            Text(
                text = if (dividendAccountName != null) {
                    "O dinheiro cai direto na conta $dividendAccountName e aparece como ganho (Dividendo ${holding.symbol})."
                } else {
                    "Não achei a conta corrente do mesmo banco. Informe o banco da conta de investimento e cadastre uma corrente dele."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (dividendAccountName != null) Positive else Negative
            )

            ErrorBanner(error)

            PrimaryAction(
                label = "Registrar dividendo",
                onClick = onSubmit,
                enabled = form.valid && dividendAccountName != null,
                loading = working
            )

            QuietAction(label = "Cancelar", onClick = onDismiss)
            return@EditorSheet
        }

        if (form.sheet != HoldingSheet.Price) {
            LabeledField(
                label = if (form.sheet == HoldingSheet.Buy) "Quantidade comprada" else "Quantidade vendida",
                value = form.quantity,
                onValueChange = { value -> onEdit { it.copy(quantity = value) } },
                placeholder = if (form.sheet == HoldingSheet.Sell) "Você tem ${formatQuantity(holding.quantity)}" else "Ex.: 10",
                keyboardType = KeyboardType.Decimal
            )
        }

        LabeledField(
            label = if (form.sheet == HoldingSheet.Price) "Preço atual por unidade (R$)" else "Preço por unidade (R$)",
            value = form.price,
            onValueChange = { value -> onEdit { it.copy(price = value) } },
            placeholder = "Ex.: 35,20",
            keyboardType = KeyboardType.Decimal
        )

        if (form.sheet != HoldingSheet.Price) {
            AccountChips(
                label = if (form.sheet == HoldingSheet.Buy) "Conta de origem" else "Conta de destino",
                accounts = accounts,
                selectedId = form.otherAccountId,
                onSelect = { id -> onEdit { it.copy(otherAccountId = id) } }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(
                when (form.sheet) {
                    HoldingSheet.Buy -> "Data da compra"
                    HoldingSheet.Sell -> "Data da venda"
                    HoldingSheet.Price -> "Data do preço"
                    HoldingSheet.Dividend -> "Data do recebimento"
                }
            )
            DateChips(date = form.date, onChange = { value -> onEdit { it.copy(date = value) } })
        }

        PreviewBox(holding = holding, form = form)

        ErrorBanner(error ?: form.previewError)

        PrimaryAction(
            label = when (form.sheet) {
                HoldingSheet.Buy -> "Registrar compra"
                HoldingSheet.Sell -> "Registrar venda"
                HoldingSheet.Price -> "Atualizar preço"
                HoldingSheet.Dividend -> "Registrar dividendo"
            },
            onClick = onSubmit,
            enabled = form.valid,
            loading = working
        )


        QuietAction(label = "Cancelar", onClick = onDismiss)
    }
}

/** Mostra, em frases simples, o que vai acontecer antes de confirmar. */
@Composable
private fun PreviewBox(holding: HoldingDto, form: HoldingForm) {
    val lines = previewLines(holding, form, form.preview)

    if (lines.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow("Resultado")
        lines.forEach { (text, color) ->
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

private fun previewLines(holding: HoldingDto, form: HoldingForm, preview: HoldingPreviewDto?): List<Pair<String, Color>> {
    val lines = mutableListOf<Pair<String, Color>>()
    val total = valueCents(form.quantity, form.price)

    when (form.sheet) {
        HoldingSheet.Buy -> {
            if (total != null && isPositive(form.quantity) && isPositive(form.price)) {
                lines += "${formatQuantity(form.quantity)} × R$ ${formatPrice(form.price)} = R$ ${Money.full(total)}" to TextStrong
            }
            if (preview?.newAvgPrice != null) {
                lines += "Seu preço médio vai de R$ ${formatPrice(preview.oldAvgPrice ?: holding.avgPrice)} para R$ ${formatPrice(preview.newAvgPrice)}." to TextStrong
                preview.newQuantity?.let { lines += "Você passa a ter ${formatQuantity(it)} unidades." to TextMuted }
            }
        }
        HoldingSheet.Sell -> {
            if (total != null && isPositive(form.quantity) && isPositive(form.price)) {
                lines += "Você recebe R$ ${Money.full(total)}." to TextStrong
            }
            val result = preview?.resultCents
            if (result != null) {
                lines += "Resultado da venda: ${formatSigned(result)} (${formatPercent(preview.resultPercent)}) em relação ao preço médio." to tone(result)
                preview.remainingQuantity?.let { lines += "Sobram ${formatQuantity(it)} unidades." to TextMuted }
            }
        }
        HoldingSheet.Price, HoldingSheet.Dividend -> Unit
    }

    if (preview != null && preview.oldPrice != null && preview.changeCents != 0L) {
        val verb = if (preview.changeCents > 0) "subiu" else "caiu"
        lines += "O preço $verb ${formatPercent(preview.changePercent).removePrefix("+").removePrefix("-")} " +
            "(de R$ ${formatPrice(preview.oldPrice)} para R$ ${formatPrice(form.price)}). " +
            "Sua posição de ${formatQuantity(holding.quantity)} unidades ${if (preview.changeCents > 0) "sobe" else "cai"} " +
            "R$ ${Money.full(kotlin.math.abs(preview.changeCents))}." to tone(preview.changeCents)
    }

    return lines
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color = TextStrong) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun Muted(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

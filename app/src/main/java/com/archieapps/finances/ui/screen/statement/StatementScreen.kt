package com.archieapps.finances.ui.screen.statement

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
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
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val MonthFormat = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM", PtBr)

@Composable
fun StatementScreen(
    cardId: Int,
    onBack: () -> Unit,
    onOpenEntry: (Int) -> Unit = {},
    refreshKey: Int = 0
) {
    val model = appViewModel(key = "statement-$cardId") { container ->
        StatementViewModel(container.repository, cardId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val statement = state.statement

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) model.load()
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = statement?.cardName ?: "Fatura",
                subtitle = "Fatura do cartão",
                onBack = onBack,
                action = { TextAction("Atualizar", model::load) }
            )
        }

        if (statement != null) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextAction("‹", model::previousMonth, color = TextMuted)
                    Text(
                        text = monthLabel(statement.reference),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextStrong,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    TextAction("›", model::nextMonth, color = TextMuted)
                }
            }

            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Eyebrow(if (statement.paid) "Fatura paga" else "Total da fatura")
                    Spacer(Modifier.height(10.dp))
                    MoneyText(
                        cents = statement.totalCents,
                        size = MoneySize.Hero,
                        color = TextStrong,
                        weight = FontWeight.W700
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = statusLine(statement),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            statement.paid -> Positive
                            statement.closed -> Warning
                            else -> TextMuted
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Compras de ${shortDate(statement.startsOn)} a ${shortDate(statement.closesOn)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextFaint
                    )
                }
            }

            item { ErrorBanner(state.error) }

            if (state.canPay) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Eyebrow("Pagar com")
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.accounts.forEach { account ->
                                Chip(
                                    text = account.name,
                                    active = state.payAccountId == account.id,
                                    onClick = { model.onPayAccount(account.id) }
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryAction(
                            label = "Pagar fatura",
                            onClick = model::pay,
                            enabled = state.payAccountId != null,
                            loading = state.working
                        )
                    }
                }
            }

            if (statement.paid) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        QuietAction(
                            label = "Desfazer pagamento",
                            onClick = model::unpay,
                            destructive = true
                        )
                    }
                }
            }

            if (state.byCategory.isNotEmpty()) {
                item { SectionHeader("Por categoria") }

                items(state.byCategory, key = { "cat-${it.first}" }) { (name, total) ->
                    LedgerRow(
                        title = name,
                        showMarker = false,
                        trailing = {
                            MoneyText(cents = total, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                        }
                    )
                    HairlineDivider(inset = 20)
                }
            }

            if (statement.entries.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Compras")
                }

                items(statement.entries, key = { "entry-${it.id}" }) { entry ->
                    ItemLine(entry, onClick = { onOpenEntry(entry.id) })
                    HairlineDivider(inset = 20)
                }
            } else {
                item {
                    Text(
                        text = "Nenhuma compra nesta fatura.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }

        if (state.loading && statement == null) {
            item { LoadingRow() }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun ItemLine(entry: EntryDto, onClick: () -> Unit) {
    LedgerRow(
        onClick = onClick,
        title = entry.description,
        subtitle = listOfNotNull(
            shortDate(entry.date),
            entry.categoryName?.takeIf { it != entry.description }
        ).joinToString(" · "),
        markerHex = entry.color,
        trailing = {
            MoneyText(cents = entry.amountCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
        }
    )
}

private fun monthLabel(reference: String): String {
    val parsed = runCatching { YearMonth.parse(reference) }.getOrNull() ?: return reference

    return parsed.atDay(1).format(MonthFormat).replaceFirstChar { it.titlecase(PtBr) }
}

private fun shortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(ShortDate) }.getOrDefault(date)

private fun statusLine(statement: com.archieapps.finances.data.dto.StatementDto): String {
    if (statement.paid) {
        return "Paga em ${shortDate(statement.paidOn.orEmpty())}."
    }

    val due = shortDate(statement.dueOn)

    return when {
        statement.closed && statement.daysToDue < 0 -> "Fechada e vencida em $due."
        statement.closed -> "Fechada. Vence $due (${dayCount(statement.daysToDue)})."
        else -> "Fecha ${shortDate(statement.closesOn)} (${dayCount(statement.daysToClose)}). Vence $due."
    }
}

private fun dayCount(days: Int): String = when {
    days == 0 -> "hoje"
    days == 1 -> "em 1 dia"
    days > 1 -> "em $days dias"
    days == -1 -> "há 1 dia"
    else -> "há ${-days} dias"
}

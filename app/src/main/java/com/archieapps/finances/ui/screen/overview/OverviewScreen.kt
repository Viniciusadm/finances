package com.archieapps.finances.ui.screen.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.OpenStatementDto
import com.archieapps.finances.data.dto.CategorySliceDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.MonthTotalsDto
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.components.parseHex
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

private val PtBr = Locale.forLanguageTag("pt-BR")
private val MonthFormat = DateTimeFormatter.ofPattern("MMMM", PtBr)
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM", PtBr)

@Composable
fun OverviewScreen(
    onOpenEntry: (Int) -> Unit,
    onOpenStatement: (Int) -> Unit,
    onOpenReports: () -> Unit
) {
    val model = appViewModel { container -> OverviewViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    val data = state.data

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = "Resumo",
                action = { TextAction("Atualizar", model::refresh) }
            )
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Eyebrow("Saldo em contas")
                Spacer(Modifier.height(10.dp))
                MoneyText(
                    cents = data?.totalCents ?: 0,
                    size = MoneySize.Hero,
                    color = TextStrong,
                    weight = FontWeight.W700
                )

                if (data != null && data.committedCents > 0) {
                    Spacer(Modifier.height(8.dp))
                    FreeBalanceLine(freeCents = data.freeCents, committedCents = data.committedCents)
                }

                Spacer(Modifier.height(22.dp))
            }
        }

        item { ErrorBanner(state.error) }

        if (state.loading && data == null) {
            item { LoadingRow() }
        }

        if (data != null) {
            item {
                MonthBand(
                    monthLabel = monthLabel(data.month),
                    month = data.month,
                    totals = data.monthTotals,
                    deltaCents = state.expenseDelta,
                    unpaidCents = data.unpaidCents
                )
            }

            if (data.byCategory.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "No que foi",
                        action = { TextAction("Relatórios", onOpenReports) }
                    )
                }

                items(data.byCategory.take(6), key = { "slice-${it.categoryId ?: 0}" }) { slice ->
                    CategoryBar(slice = slice, maxCents = state.topCategoryCents)
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            item { SectionHeader("Contas") }

            items(data.accounts, key = { "account-${it.id}" }) { account ->
                AccountLine(account)
                HairlineDivider(inset = 20)
            }

            if (data.openStatements.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Faturas abertas")
                }

                items(data.openStatements, key = { "statement-${it.cardId}" }) { statement ->
                    StatementLine(statement = statement, onClick = { onOpenStatement(statement.cardId) })
                    HairlineDivider(inset = 20)
                }
            }

            if (data.upcoming.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Próximas contas")
                }

                items(data.upcoming, key = { "next-${it.id}" }) { entry ->
                    UpcomingLine(entry = entry, onClick = { onOpenEntry(entry.id) })
                    HairlineDivider(inset = 20)
                }
            }

            if (data.recent.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Últimos lançamentos")
                }

                items(data.recent, key = { "recent-${it.id}" }) { entry ->
                    RecentLine(entry = entry, onClick = { onOpenEntry(entry.id) })
                    HairlineDivider(inset = 20)
                }
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun FreeBalanceLine(freeCents: Long, committedCents: Long) {
    Row {
        MoneyText(
            cents = freeCents.absoluteValue,
            size = MoneySize.Inline,
            color = if (freeCents < 0) Negative else TextMuted,
            modifier = Modifier.alignByBaseline()
        )
        Caption(if (freeCents < 0) " a descoberto · " else " livres · ")
        MoneyText(
            cents = committedCents,
            size = MoneySize.Inline,
            color = Warning,
            modifier = Modifier.alignByBaseline()
        )
        Caption(" em fatura")
    }
}

@Composable
private fun RowScope.Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextFaint,
        modifier = Modifier.alignByBaseline()
    )
}

@Composable
private fun MonthBand(
    monthLabel: String,
    month: String,
    totals: MonthTotalsDto,
    deltaCents: Long,
    unpaidCents: Long
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Eyebrow(monthLabel)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column {
                Text("Gastei", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                MoneyText(cents = totals.expenseCents, size = MoneySize.Section, color = Negative, showSymbol = false)
            }
            Column {
                Text("Entrou", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                MoneyText(cents = totals.incomeCents, size = MoneySize.Section, color = Positive, showSymbol = false)
            }
        }

        if (totals.expenseCardCents > 0) {
            Spacer(Modifier.height(10.dp))

            Row {
                Caption("conta ")
                MoneyText(
                    cents = totals.expenseAccountCents,
                    size = MoneySize.Inline,
                    color = Negative,
                    modifier = Modifier.alignByBaseline()
                )
                Caption(" · cartão ")
                MoneyText(
                    cents = totals.expenseCardCents,
                    size = MoneySize.Inline,
                    color = Warning,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = comparisonText(deltaCents),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        if (totals.expenseCardCents > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = dueText(totals.expenseCardCents, totals.cardDueReference, month),
                style = MaterialTheme.typography.bodyMedium,
                color = TextFaint
            )
        }

        if (unpaidCents > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "R$ ${Money.full(unpaidCents)} previsto e ainda não pago.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextFaint
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CategoryBar(slice: CategorySliceDto, maxCents: Long) {
    val fraction = if (maxCents <= 0) 0f else (slice.totalCents.toFloat() / maxCents).coerceIn(0.02f, 1f)
    val color = parseHex(slice.color) ?: Primary

    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = slice.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextStrong,
                modifier = Modifier.weight(1f)
            )
            MoneyText(cents = slice.totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
        }

        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Hairline, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun AccountLine(account: AccountDto) {
    LedgerRow(
        title = account.name,
        subtitle = account.kindLabel.takeIf { it != account.name },
        markerHex = account.color,
        trailing = {
            MoneyText(
                cents = account.balanceCents ?: account.initialBalanceCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = TextStrong
            )
        }
    )
}

@Composable
private fun StatementLine(statement: OpenStatementDto, onClick: () -> Unit) {
    LedgerRow(
        title = statement.cardName,
        subtitle = statementSubtitle(statement),
        markerHex = statement.color,
        navigates = true,
        onClick = onClick,
        trailing = {
            if (statement.totalCents > 0) {
                MoneyText(
                    cents = statement.totalCents,
                    size = MoneySize.Row,
                    showSymbol = false,
                    color = TextStrong
                )
            } else {
                Text("sem uso", style = MaterialTheme.typography.bodyMedium, color = TextFaint)
            }
        }
    )
}

private fun statementSubtitle(statement: OpenStatementDto): String {
    val due = shortDate(statement.dueOn)

    return when {
        statement.paid -> "Paga · vence $due"
        statement.closed -> "Fechada · vence $due"
        else -> "Fecha ${shortDate(statement.closesOn)} · vence $due"
    }
}

private fun shortDate(date: String): String =
    runCatching { java.time.LocalDate.parse(date).format(ShortDate) }.getOrDefault(date)

@Composable
private fun UpcomingLine(entry: EntryDto, onClick: () -> Unit) {
    LedgerRow(
        onClick = onClick,
        title = entry.description,
        subtitle = listOfNotNull(
            shortDate(entry.date),
            entry.cardName ?: entry.accountName
        ).joinToString(" · "),
        markerHex = entry.color,
        dimmed = true,
        trailing = {
            MoneyText(
                cents = entry.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = if (entry.kind == "income") Positive.copy(alpha = 0.7f) else TextFaint
            )
        }
    )
}

@Composable
private fun RecentLine(entry: EntryDto, onClick: () -> Unit) {
    LedgerRow(
        title = entry.description,
        subtitle = listOfNotNull(
            entry.categoryName?.takeIf { it != entry.description },
            entry.cardName ?: entry.accountName
        ).joinToString(" · "),
        markerHex = entry.color,
        dimmed = !entry.paid,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = entry.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = when {
                    !entry.paid -> TextFaint
                    entry.kind == "income" -> Positive
                    else -> TextStrong
                }
            )
        }
    )
}

private fun monthLabel(month: String): String {
    val parsed = runCatching { YearMonth.parse(month) }.getOrNull() ?: return month

    return parsed.atDay(1).format(MonthFormat).replaceFirstChar { it.titlecase(PtBr) }
}

private fun dueText(cardCents: Long, reference: String?, month: String): String {
    val amount = "R$ ${Money.full(cardCents)}"
    val due = reference?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: return "$amount sai nas próximas faturas."
    val current = runCatching { YearMonth.parse(month) }.getOrNull()
    val name = monthLabel(reference).lowercase(PtBr)
    val label = if (current != null && current.year != due.year) "$name de ${due.year}" else name

    return "$amount sai na fatura de $label."
}

private fun comparisonText(deltaCents: Long): String = when {
    deltaCents == 0L -> "Mesmo gasto do mês passado."
    deltaCents > 0 -> "R$ ${Money.full(deltaCents)} mais que o mês passado."
    else -> "R$ ${Money.full(deltaCents.absoluteValue)} menos que o mês passado."
}

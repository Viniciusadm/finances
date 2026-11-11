package com.archieapps.finances.ui.screen.statement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.archieapps.finances.ui.components.CardShape
import com.archieapps.finances.ui.components.ColorDot
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.ProgressTrack
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.SurfaceCard
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.HeroBrush
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
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

    Refreshable(refreshing = state.loading && statement != null, onRefresh = model::load) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = statement?.cardName ?: "Fatura",
                subtitle = "Fatura do cartão",
                onBack = onBack
            )
        }

        if (statement != null) {
            item {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(Surface)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconAction(AppIcons.ChevronLeft, onClick = model::previousMonth, contentDescription = "Fatura anterior", size = 40)
                    Text(
                        text = monthLabel(statement.reference),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
                        color = TextStrong,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconAction(AppIcons.ChevronRight, onClick = model::nextMonth, contentDescription = "Próxima fatura", size = 40)
                }
            }

            item {
                val statusColor = when {
                    statement.paid -> Positive
                    statement.closed -> Warning
                    else -> Primary
                }

                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(HeroBrush)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.CreditCard, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (statement.paid) "Fatura paga" else "Total da fatura",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = when {
                                statement.paid -> "Paga"
                                statement.closed -> "Fechada"
                                else -> "Aberta"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    MoneyText(
                        cents = statement.totalCents,
                        size = MoneySize.Hero,
                        color = Color.White,
                        symbolColor = Color.White.copy(alpha = 0.7f),
                        weight = FontWeight.W800
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = statusLine(statement),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Compras de ${shortDate(statement.startsOn)} a ${shortDate(statement.closesOn)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            item { ErrorBanner(state.error) }

            if (state.canPay) {
                item {
                    SurfaceCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
                                    onClick = { model.onPayAccount(account.id) },
                                    leading = { ColorDot(account.color) }
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryAction(
                            label = "Pagar fatura",
                            icon = AppIcons.Check,
                            onClick = model::pay,
                            enabled = state.payAccountId != null,
                            loading = state.working
                        )
                    }
                }
            }

            if (statement.paid) {
                item {
                    QuietAction(
                        label = "Desfazer pagamento",
                        onClick = model::unpay,
                        destructive = true,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                    )
                }
            }

            if (state.byCategory.isNotEmpty()) {
                item { SectionHeader("Por categoria") }

                val top = state.byCategory.maxOf { it.second }.coerceAtLeast(1)

                item {
                    SurfaceCard(Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp)) {
                        state.byCategory.forEachIndexed { index, (name, total) ->
                            if (index > 0) Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                                    color = TextStrong,
                                    modifier = Modifier.weight(1f)
                                )
                                MoneyText(cents = total, size = MoneySize.Inline, showSymbol = false, color = TextStrong)
                            }
                            Spacer(Modifier.height(6.dp))
                            ProgressTrack(fraction = total.toFloat() / top)
                        }
                    }
                }
            }

            if (statement.entries.isNotEmpty()) {
                item { SectionHeader("Compras") }

                itemsIndexed(statement.entries, key = { _, entry -> "entry-${entry.id}" }) { index, entry ->
                    Column(Modifier.grouped(index, statement.entries.size)) {
                        ItemLine(entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            } else {
                item {
                    EmptyState(
                        headline = "Fatura vazia",
                        hint = "Nenhuma compra nesta fatura.",
                        icon = AppIcons.CreditCard
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

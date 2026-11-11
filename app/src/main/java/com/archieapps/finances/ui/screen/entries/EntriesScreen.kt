package com.archieapps.finances.ui.screen.entries

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
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.archieapps.finances.ui.components.CardShape
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.StatTile
import com.archieapps.finances.ui.components.cardPadding
import com.archieapps.finances.ui.components.entryIcon
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.Accent
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val MonthFormat = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val DayFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr)

@Composable
fun EntriesScreen(onEdit: (Int) -> Unit) {
    val model = appViewModel { container -> EntriesViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Refreshable(refreshing = state.loading && state.entries.isNotEmpty(), onRefresh = model::refresh) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { ScreenHeader(title = "Extrato") }

            if (!state.allTime) {
                item {
                    MonthStepper(
                        month = state.month,
                        onPrevious = model::previousMonth,
                        onNext = model::nextMonth
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip("Todo período", state.allTime, icon = AppIcons.Calendar, onClick = model::toggleAllTime)
                    Chip("Gastos", state.kind == "expense", icon = AppIcons.ArrowUp, activeColor = Negative, onClick = {
                        model.setKind(if (state.kind == "expense") null else "expense")
                    })
                    Chip("Ganhos", state.kind == "income", icon = AppIcons.ArrowDown, activeColor = Positive, onClick = {
                        model.setKind(if (state.kind == "income") null else "income")
                    })
                    Chip("Transferências", state.kind == "transfer", icon = AppIcons.Swap, onClick = {
                        model.setKind(if (state.kind == "transfer") null else "transfer")
                    })
                    Chip("Rendimentos", state.kind == "yield", icon = AppIcons.TrendingUp, activeColor = Accent, onClick = {
                        model.setKind(if (state.kind == "yield") null else "yield")
                    })
                    Chip("Só previstos", state.onlyUnpaid, icon = AppIcons.Schedule, activeColor = Warning, onClick = model::toggleUnpaid)
                }
            }

            item { Totals(expense = state.expenseCents, income = state.incomeCents) }

            item { ErrorBanner(state.error) }

            if (state.loading && state.entries.isEmpty()) {
                item { LoadingRow() }
            }

            if (!state.loading && state.entries.isEmpty()) {
                item {
                    EmptyState(
                        headline = "Nada lançado aqui",
                        hint = "Toque no + para lançar um gasto. Leva três toques: valor, conta, salvar.",
                        icon = AppIcons.List
                    )
                }
            }

            state.grouped.forEach { (date, entries) ->
                item(key = "day-$date") {
                    DayHeader(date = date, totalCents = model.dayTotal(entries))
                }

                itemsIndexed(entries, key = { _, entry -> "entry-${entry.id}" }) { index, entry ->
                    Column(Modifier.grouped(index, entries.size)) {
                        EntryLine(entry = entry, onClick = { onEdit(entry.id) })
                    }
                }
            }

            if (state.hasMore) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.loadingMore) {
                            LoadingRow()
                        } else {
                            QuietAction(label = "Carregar mais", onClick = model::loadMore, icon = AppIcons.ExpandMore)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(110.dp)) }
        }
    }
}

@Composable
private fun MonthStepper(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .cardPadding()
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconAction(AppIcons.ChevronLeft, onClick = onPrevious, contentDescription = "Mês anterior", size = 40)
        Text(
            text = month.atDay(1).format(MonthFormat).replaceFirstChar { it.titlecase(PtBr) },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
            color = TextStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconAction(AppIcons.ChevronRight, onClick = onNext, contentDescription = "Próximo mês", size = 40)
    }
}

@Composable
private fun Totals(expense: Long, income: Long) {
    Row(
        Modifier
            .cardPadding()
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile("Saiu", expense, Negative, AppIcons.ArrowUp, Modifier.weight(1f))
        StatTile("Entrou", income, Positive, AppIcons.ArrowDown, Modifier.weight(1f))
    }
}

@Composable
private fun DayHeader(date: String, totalCents: Long) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val label = when (parsed) {
        LocalDate.now() -> "Hoje"
        LocalDate.now().minusDays(1) -> "Ontem"
        null -> date
        else -> parsed.format(DayFormat).replaceFirstChar { it.titlecase(PtBr) }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            cents = totalCents,
            size = MoneySize.Inline,
            showSymbol = false,
            color = if (totalCents >= 0) Positive else TextMuted
        )
    }
}

@Composable
private fun EntryLine(entry: EntryDto, onClick: () -> Unit) {
    LedgerRow(
        title = entry.description,
        subtitle = subtitleFor(entry),
        markerHex = entry.color,
        icon = entryIcon(entry.kind),
        dimmed = !entry.paid,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = entry.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = when {
                    !entry.paid -> TextFaint
                    entry.kind == "income" || entry.kind == "yield" -> Positive
                    entry.kind == "transfer" -> TextMuted
                    else -> TextStrong
                }
            )
        }
    )
}

private fun subtitleFor(entry: EntryDto): String {
    val where = when {
        entry.kind == "transfer" -> listOfNotNull(entry.accountName, entry.counterAccountName).joinToString(" → ")
        entry.cardName != null -> entry.cardName
        entry.accountName != null -> entry.accountName
        else -> null
    }

    val parts = listOfNotNull(
        entry.categoryName?.takeIf { it != entry.description },
        where,
        entry.personName,
        if (entry.paid) null else "previsto"
    )

    return parts.joinToString(" · ")
}

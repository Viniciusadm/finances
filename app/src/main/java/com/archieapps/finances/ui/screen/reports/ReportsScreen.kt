package com.archieapps.finances.ui.screen.reports

import androidx.compose.foundation.lazy.itemsIndexed
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.AppIcons
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.archieapps.finances.ui.components.CardShape
import com.archieapps.finances.ui.components.ProgressTrack
import com.archieapps.finances.ui.components.SurfaceCard
import com.archieapps.finances.ui.theme.HeroBrush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.archieapps.finances.data.dto.CategoryReportRowDto
import com.archieapps.finances.data.dto.MethodReportRowDto
import com.archieapps.finances.data.dto.MonthReportRowDto
import com.archieapps.finances.data.dto.TagReportRowDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
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
import com.archieapps.finances.ui.theme.Warning
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortMonth = DateTimeFormatter.ofPattern("MMM", PtBr)

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val model = appViewModel { container -> ReportsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Refreshable(refreshing = state.loading && state.byCategory.isNotEmpty(), onRefresh = model::refresh) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                ScreenHeader(
                    title = "Relatórios",
                    subtitle = "No que o dinheiro foi",
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
                    ReportRange.entries.forEach { range ->
                        Chip(
                            text = range.label,
                            active = state.range == range,
                            onClick = { model.setRange(range) }
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip("Gastos", state.kind == "expense", icon = AppIcons.ArrowUp, activeColor = Negative, onClick = { model.setKind("expense") })
                    Chip("Ganhos", state.kind == "income", icon = AppIcons.ArrowDown, activeColor = Positive, onClick = { model.setKind("income") })
                }
            }

            item {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportSource.entries.forEach { source ->
                        Chip(
                            text = source.label,
                            active = state.source == source,
                            onClick = { model.setSource(source) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(state.error)
            }

            if (state.loading && state.byCategory.isEmpty()) {
                item { LoadingRow() }
            }

            val total = state.categoryWindow?.totalCents ?: 0

            if (total > 0) {
                item {
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(HeroBrush)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = if (state.kind == "income") "Entrou no período" else "Gastei no período",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(10.dp))
                        MoneyText(
                            cents = total,
                            size = MoneySize.Hero,
                            color = Color.White,
                            symbolColor = Color.White.copy(alpha = 0.7f),
                            weight = FontWeight.W800
                        )
                    }
                }
            }

            if (!state.loading && state.byCategory.isEmpty()) {
                item {
                    EmptyState(
                        headline = "Nada no período",
                        hint = "Troque o período acima ou lance alguma coisa primeiro."
                    )
                }
            }

            if (state.byCategory.isNotEmpty()) {
                item { SectionHeader("Por categoria") }

                itemsIndexed(state.byCategory, key = { _, it -> "cat-${it.categoryId ?: 0}" }) { index, row ->
                    Column(Modifier.grouped(index, state.byCategory.size)) {
                        CategoryRow(row = row, maxCents = state.topCategoryCents)
                    }
                }
            }

            if (state.byTag.isNotEmpty()) {
                item { SectionHeader("Por tag") }

                itemsIndexed(state.byTag, key = { _, it -> "tag-${it.tagId}" }) { index, row ->
                    Column(Modifier.grouped(index, state.byTag.size)) {
                        TagRow(row = row, maxCents = state.topTagCents)
                    }
                }
            }

            if (state.byMethod.isNotEmpty()) {
                item { SectionHeader("Por forma de pagamento") }

                itemsIndexed(state.byMethod, key = { _, it -> "method-${it.method}" }) { index, row ->
                    Column(Modifier.grouped(index, state.byMethod.size)) {
                        MethodRow(row = row, maxCents = state.topMethodCents)
                    }
                }
            }

            if (state.byInstitution.size > 1 || state.byInstitution.any { it.institution != null }) {
                item { SectionHeader("Por banco") }

                itemsIndexed(state.byInstitution, key = { _, it -> "inst-${it.institution ?: "none"}" }) { index, row ->
                    Column(Modifier.grouped(index, state.byInstitution.size)) {
                        Bar(
                            name = row.name,
                            totalCents = row.totalCents,
                            share = row.share,
                            entries = row.entries,
                            color = Primary,
                            fraction = fractionOf(row.totalCents, state.topInstitutionCents)
                        )
                    }
                }
            }

            if (state.byAccount.isNotEmpty()) {
                item { SectionHeader(if (state.kind == "income") "Por conta" else "Por conta (débito, pix, dinheiro)") }

                itemsIndexed(state.byAccount, key = { _, it -> "account-${it.accountId}" }) { index, row ->
                    Column(Modifier.grouped(index, state.byAccount.size)) {
                        Bar(
                            name = listOfNotNull(row.name, row.institution?.takeIf { !row.name.contains(it, ignoreCase = true) })
                                .joinToString(" · "),
                            totalCents = row.totalCents,
                            share = row.share,
                            entries = row.entries,
                            color = parseHex(row.color) ?: Primary,
                            fraction = fractionOf(row.totalCents, state.topAccountCents)
                        )
                        if (row.byMethod.size > 1) {
                            Text(
                                text = row.byMethod.joinToString(" · ") { "${it.name} ${Money.full(it.totalCents)}" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }

            if (state.byCard.isNotEmpty()) {
                item { SectionHeader("Por cartão (crédito)") }

                itemsIndexed(state.byCard, key = { _, it -> "card-${it.cardId}" }) { index, row ->
                    Column(Modifier.grouped(index, state.byCard.size)) {
                        Bar(
                            name = row.name,
                            totalCents = row.totalCents,
                            share = row.share,
                            entries = row.entries,
                            color = parseHex(row.color) ?: Warning,
                            fraction = fractionOf(row.totalCents, state.topCardCents)
                        )
                    }
                }
            }

            if (state.byMonth.isNotEmpty()) {
                item { SectionHeader("Últimos 12 meses") }

                item {
                    SurfaceCard(Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                        MonthChart(
                            rows = state.byMonth,
                            maxCents = state.maxMonthCents,
                            averageCents = state.monthMisc?.averageExpenseCents ?: 0
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun CategoryRow(row: CategoryReportRowDto, maxCents: Long) {
    Bar(
        name = row.name,
        totalCents = row.totalCents,
        share = row.share,
        entries = row.entries,
        color = parseHex(row.color) ?: Primary,
        fraction = fractionOf(row.totalCents, maxCents)
    )
}

@Composable
private fun TagRow(row: TagReportRowDto, maxCents: Long) {
    Bar(
        name = row.name,
        totalCents = row.totalCents,
        share = row.share,
        entries = row.entries,
        color = parseHex(row.color) ?: Primary,
        fraction = fractionOf(row.totalCents, maxCents)
    )
}

@Composable
private fun MethodRow(row: MethodReportRowDto, maxCents: Long) {
    Bar(
        name = row.name,
        totalCents = row.totalCents,
        share = row.share,
        entries = row.entries,
        color = methodColor(row.method),
        fraction = fractionOf(row.totalCents, maxCents)
    )
}

private fun methodColor(method: String): androidx.compose.ui.graphics.Color = when (method) {
    "credit" -> Warning
    "transfer" -> Positive
    "unknown" -> TextFaint
    else -> Primary
}

@Composable
private fun Bar(
    name: String,
    totalCents: Long,
    share: Double,
    entries: Int,
    color: androidx.compose.ui.graphics.Color,
    fraction: Float
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = TextStrong,
                modifier = Modifier.weight(1f)
            )
            MoneyText(cents = totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
        }

        Spacer(Modifier.height(6.dp))

        ProgressTrack(fraction = fraction, color = color, height = 8)

        Spacer(Modifier.height(5.dp))

        Text(
            text = "${percent(share)} · ${entriesLabel(entries)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextFaint
        )
    }
}

@Composable
private fun MonthChart(rows: List<MonthReportRowDto>, maxCents: Long, averageCents: Long) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            rows.forEach { row ->
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(barHeight(row.expenseCents, maxCents))
                            .background(Negative.copy(alpha = 0.85f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(barHeight(row.incomeCents, maxCents))
                            .background(Positive.copy(alpha = 0.7f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Text(
                    text = monthLabel(row.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextFaint,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Legend(color = Negative, label = "saiu")
            Legend(color = Positive, label = "entrou")
        }

        if (averageCents > 0) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Média de gasto por mês: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                MoneyText(cents = averageCents, size = MoneySize.Inline, showSymbol = false, color = TextMuted)
            }
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(14.dp)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

private fun fractionOf(value: Long, max: Long): Float =
    if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0.02f, 1f)

private fun barHeight(value: Long, max: Long): androidx.compose.ui.unit.Dp {
    if (max <= 0L || value <= 0L) return 2.dp

    return (((value.toFloat() / max) * 130f).coerceIn(2f, 130f)).dp
}

private fun percent(share: Double): String =
    String.format(PtBr, "%.1f%%", share * 100)

private fun entriesLabel(entries: Int): String = when (entries) {
    1 -> "1 lançamento"
    else -> "$entries lançamentos"
}

private fun monthLabel(month: String): String {
    val parsed = runCatching { YearMonth.parse(month) }.getOrNull() ?: return month

    return parsed.atDay(1).format(ShortMonth).take(3)
}

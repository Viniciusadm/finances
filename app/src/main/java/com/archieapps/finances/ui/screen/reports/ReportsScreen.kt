package com.archieapps.finances.ui.screen.reports

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

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = "Relatórios",
                subtitle = "No que o dinheiro foi",
                onBack = onBack,
                action = { TextAction("Atualizar", model::refresh) }
            )
        }

        item {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
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
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("Gastos", state.kind == "expense", onClick = { model.setKind("expense") })
                Chip("Ganhos", state.kind == "income", onClick = { model.setKind("income") })
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
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
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Eyebrow(if (state.kind == "income") "Entrou no período" else "Gastei no período")
                    Spacer(Modifier.height(10.dp))
                    MoneyText(
                        cents = total,
                        size = MoneySize.Hero,
                        color = TextStrong,
                        weight = FontWeight.W700
                    )
                    Spacer(Modifier.height(16.dp))
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

            items(state.byCategory, key = { "cat-${it.categoryId ?: 0}" }) { row ->
                CategoryRow(row = row, maxCents = state.topCategoryCents)
            }
        }

        if (state.byTag.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Por tag")
            }

            items(state.byTag, key = { "tag-${it.tagId}" }) { row ->
                TagRow(row = row, maxCents = state.topTagCents)
            }
        }

        if (state.byMethod.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Por forma de pagamento")
            }

            items(state.byMethod, key = { "method-${it.method}" }) { row ->
                MethodRow(row = row, maxCents = state.topMethodCents)
            }
        }

        if (state.byMonth.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Últimos 12 meses")
            }

            item {
                MonthChart(
                    rows = state.byMonth,
                    maxCents = state.maxMonthCents,
                    averageCents = state.monthMisc?.averageExpenseCents ?: 0
                )
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
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
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextStrong,
                modifier = Modifier.weight(1f)
            )
            MoneyText(cents = totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
        }

        Spacer(Modifier.height(6.dp))

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
    Column(Modifier.padding(horizontal = 20.dp)) {
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

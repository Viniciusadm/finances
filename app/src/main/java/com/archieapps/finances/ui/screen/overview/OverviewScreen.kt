package com.archieapps.finances.ui.screen.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.BillsSummaryDto
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
import com.archieapps.finances.ui.components.SettleButton
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.archieapps.finances.data.dto.InstitutionGroupDto
import com.archieapps.finances.ui.components.CardShape
import com.archieapps.finances.ui.components.IconBadge
import com.archieapps.finances.ui.components.NoticeBanner
import com.archieapps.finances.ui.components.ProgressTrack
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.StatTile
import com.archieapps.finances.ui.components.SurfaceCard
import com.archieapps.finances.ui.components.accountIcon
import com.archieapps.finances.ui.components.cardPadding
import com.archieapps.finances.ui.components.entryIcon
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.Accent
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.HeroBrush
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
    onOpenReports: () -> Unit,
    onOpenBills: () -> Unit = {},
    onOpenAccount: (Int) -> Unit = {},
    refreshKey: Int = 0
) {
    val model = appViewModel { container -> OverviewViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) model.refresh()
    }
    val data = state.data

    Refreshable(refreshing = state.loading && data != null, onRefresh = model::refresh) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                ScreenHeader(title = greeting(), subtitle = data?.let { "Resumo de " + monthLabel(it.month).lowercase(PtBr) })
            }

            item {
                HeroCard(
                    totalCents = data?.totalCents ?: 0,
                    freeCents = data?.freeCents ?: 0,
                    committedCents = data?.committedCents ?: 0
                )
            }

            item { ErrorBanner(state.error) }
            item { NoticeBanner(state.notice) }

            if (state.loading && data == null) {
                item { LoadingRow() }
            }

            if (data != null) {
                item { SectionHeader(monthLabel(data.month)) }

                item {
                    MonthBand(
                        month = data.month,
                        totals = data.monthTotals,
                        deltaCents = state.expenseDelta,
                        unpaidCents = data.unpaidCents
                    )
                }

                item {
                    SectionHeader(
                        title = "Contas a pagar e receber",
                        action = { TextAction("Ver todas", onOpenBills) }
                    )
                }

                item { BillsSummary(data.bills, onClick = onOpenBills) }

                if (data.upcoming.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)) }
                }

                itemsIndexed(data.upcoming, key = { _, entry -> "next-${entry.id}" }) { index, entry ->
                    Column(Modifier.grouped(index, data.upcoming.size)) {
                        UpcomingLine(
                            entry = entry,
                            settling = state.settlingId == entry.id,
                            onClick = { onOpenEntry(entry.id) },
                            onSettle = if (entry.accountId != null) ({ model.settle(entry) }) else onOpenBills
                        )
                    }
                }

                if (data.byCategory.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "No que foi o dinheiro",
                            action = { TextAction("Relatórios", onOpenReports) }
                        )
                    }

                    item {
                        SurfaceCard(Modifier.cardPadding(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            data.byCategory.take(6).forEach { slice ->
                                CategoryBar(slice = slice, maxCents = state.topCategoryCents, totalCents = data.monthTotals.expenseCents)
                            }
                        }
                    }
                }

                item { SectionHeader("Onde o dinheiro está") }

                val accountsById = data.accounts.associateBy { it.id }
                val groups = if (data.institutions.isEmpty()) {
                    listOf(InstitutionGroupDto(institution = null, totalCents = data.totalCents, accountIds = data.accounts.map { it.id }))
                } else {
                    data.institutions
                }
                val labelled = data.institutions.size > 1 || data.institutions.any { it.institution != null }

                groups.forEach { group ->
                    val members = group.accountIds.mapNotNull { accountsById[it] }

                    if (labelled) {
                        item(key = "institution-${group.institution?.lowercase() ?: "none"}") {
                            InstitutionLine(name = group.institution ?: "Sem banco", totalCents = group.totalCents)
                        }
                    }

                    itemsIndexed(members, key = { _, account -> "account-${account.id}" }) { index, account ->
                        Column(Modifier.grouped(index, members.size)) {
                            AccountLine(account, onClick = { onOpenAccount(account.id) })
                        }
                    }
                }

                if (data.openStatements.isNotEmpty()) {
                    item { SectionHeader("Faturas abertas") }

                    itemsIndexed(data.openStatements, key = { _, statement -> "statement-${statement.cardId}" }) { index, statement ->
                        Column(Modifier.grouped(index, data.openStatements.size)) {
                            StatementLine(statement = statement, onClick = { onOpenStatement(statement.cardId) })
                        }
                    }
                }

                if (data.recent.isNotEmpty()) {
                    item { SectionHeader("Últimos lançamentos") }

                    itemsIndexed(data.recent, key = { _, entry -> "recent-${entry.id}" }) { index, entry ->
                        Column(Modifier.grouped(index, data.recent.size)) {
                            RecentLine(entry = entry, onClick = { onOpenEntry(entry.id) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(110.dp)) }
        }
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

@Composable
private fun HeroCard(totalCents: Long, freeCents: Long, committedCents: Long) {
    Column(
        Modifier
            .cardPadding()
            .padding(top = 4.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(HeroBrush)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Wallet, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Patrimônio", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(10.dp))
        MoneyText(
            cents = totalCents,
            size = MoneySize.Hero,
            color = Color.White,
            symbolColor = Color.White.copy(alpha = 0.7f),
            weight = FontWeight.W800
        )

        if (committedCents > 0) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroPill(
                    label = if (freeCents < 0) "A descoberto" else "Livre",
                    cents = freeCents.absoluteValue,
                    modifier = Modifier.weight(1f)
                )
                HeroPill(label = "Em fatura", cents = committedCents, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroPill(label: String, cents: Long, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(2.dp))
        MoneyText(cents = cents, size = MoneySize.Row, color = Color.White, symbolColor = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun MonthBand(
    month: String,
    totals: MonthTotalsDto,
    deltaCents: Long,
    unpaidCents: Long
) {
    Column(Modifier.cardPadding()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Gastei", totals.expenseCents, Negative, AppIcons.ArrowUp, Modifier.weight(1f))
            StatTile("Entrou", totals.incomeCents, Positive, AppIcons.ArrowDown, Modifier.weight(1f))
            if (totals.yieldCents > 0) {
                StatTile("Rendeu", totals.yieldCents, Accent, AppIcons.TrendingUp, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))

        SurfaceCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            InsightLine(
                icon = if (deltaCents > 0) AppIcons.TrendingUp else AppIcons.TrendingDown,
                color = if (deltaCents > 0) Negative else Positive,
                text = comparisonText(deltaCents)
            )

            if (totals.expenseCardCents > 0) {
                Spacer(Modifier.height(10.dp))
                InsightLine(
                    icon = AppIcons.CreditCard,
                    color = Warning,
                    text = "Conta R$ ${Money.full(totals.expenseAccountCents)} · cartão R$ ${Money.full(totals.expenseCardCents)}. " +
                        dueText(totals.expenseCardCents, totals.cardDueReference, month)
                )
            }

            if (unpaidCents > 0) {
                Spacer(Modifier.height(10.dp))
                InsightLine(
                    icon = AppIcons.Schedule,
                    color = TextMuted,
                    text = "R$ ${Money.full(unpaidCents)} previsto e ainda não pago."
                )
            }
        }
    }
}

@Composable
private fun InsightLine(icon: ImageVector, color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, color = color, size = 30)
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
private fun CategoryBar(slice: CategorySliceDto, maxCents: Long, totalCents: Long) {
    val fraction = if (maxCents <= 0) 0f else (slice.totalCents.toFloat() / maxCents).coerceIn(0.02f, 1f)
    val share = if (totalCents <= 0) 0 else ((slice.totalCents * 100) / totalCents).toInt()
    val color = parseHex(slice.color) ?: Primary

    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(color = color, label = slice.name, size = 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = slice.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                    color = TextStrong,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                MoneyText(cents = slice.totalCents, size = MoneySize.Inline, showSymbol = false, color = TextStrong)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressTrack(fraction = fraction, color = color, modifier = Modifier.weight(1f))
                Text(
                    text = "$share%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun BillsSummary(bills: BillsSummaryDto, onClick: () -> Unit) {
    Row(
        Modifier.cardPadding(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BillTile(
            label = if (bills.overdueCount == 1) "1 vencida" else "${bills.overdueCount} vencidas",
            cents = bills.overdueCents,
            color = if (bills.overdueCount > 0) Warning else TextMuted,
            icon = AppIcons.Warning,
            onClick = onClick,
            modifier = Modifier.weight(1f)
        )
        BillTile(label = "A pagar", cents = bills.toPayCents, color = Negative, icon = AppIcons.ArrowUp, onClick = onClick, modifier = Modifier.weight(1f))
        BillTile(label = "A receber", cents = bills.toReceiveCents, color = Positive, icon = AppIcons.ArrowDown, onClick = onClick, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BillTile(label: String, cents: Long, color: Color, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        MoneyText(cents = cents, size = MoneySize.Inline, showSymbol = false, color = if (cents > 0) TextStrong else TextFaint)
    }
}

@Composable
private fun InstitutionLine(name: String, totalCents: Long) {
    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.Bank, contentDescription = null, tint = TextFaint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Eyebrow(name, Modifier.weight(1f))
        MoneyText(cents = totalCents, size = MoneySize.Inline, showSymbol = false, color = TextMuted)
    }
}

@Composable
private fun AccountLine(account: AccountDto, onClick: () -> Unit) {
    LedgerRow(
        title = account.name,
        subtitle = account.kindLabel.takeIf { it != account.name },
        markerHex = account.color,
        icon = accountIcon(account.kind),
        navigates = true,
        onClick = onClick,
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
        icon = AppIcons.CreditCard,
        navigates = true,
        onClick = onClick,
        trailing = {
            if (statement.totalCents > 0) {
                MoneyText(
                    cents = statement.totalCents,
                    size = MoneySize.Row,
                    showSymbol = false,
                    color = if (statement.paid) Positive else TextStrong
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
private fun UpcomingLine(entry: EntryDto, settling: Boolean, onClick: () -> Unit, onSettle: () -> Unit) {
    val overdue = entry.status == "overdue"

    LedgerRow(
        onClick = onClick,
        title = entry.description,
        subtitle = listOfNotNull(
            if (overdue) "venceu ${shortDate(entry.date)}" else shortDate(entry.date),
            entry.personName,
            entry.accountName
        ).joinToString(" · "),
        markerHex = if (overdue) "#FBBF24" else entry.color,
        icon = if (overdue) AppIcons.Warning else null,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyText(
                    cents = entry.amountCents,
                    size = MoneySize.Row,
                    showSymbol = false,
                    color = when {
                        overdue -> Warning
                        entry.kind == "income" -> Positive
                        else -> TextStrong
                    }
                )
                if (settling) {
                    Text("…", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                } else {
                    SettleButton(receiving = entry.kind == "income", onClick = onSettle)
                }
            }
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
        icon = entryIcon(entry.kind),
        trailing = {
            MoneyText(
                cents = entry.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = when {
                    !entry.paid -> TextFaint
                    entry.kind == "income" || entry.kind == "yield" -> Positive
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

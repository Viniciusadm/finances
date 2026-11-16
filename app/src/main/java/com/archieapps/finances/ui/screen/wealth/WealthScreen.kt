package com.archieapps.finances.ui.screen.wealth

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CommittedItemDto
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.screen.accounts.lockLabel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning

@Composable
fun WealthScreen(
    section: String,
    onBack: () -> Unit,
    onOpenAccount: (Int) -> Unit,
    onOpenStatement: (Int) -> Unit = {}
) {
    val model = appViewModel(key = "wealth-$section") { container -> WealthViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    val breakdown = state.breakdown

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = when (section) {
                "free" -> if ((breakdown?.freeCents ?: 0) < 0) "Faltando" else "Livre"
                "committed" -> "Em fatura"
                else -> "Patrimônio"
            },
            subtitle = when (section) {
                "free" -> "Quanto sobra depois das faturas"
                "committed" -> "Faturas de cartão ainda não pagas"
                else -> "O que é seu, em conta ou renda fixa"
            },
            onBack = onBack
        )

        ErrorBanner(state.error)

        if (state.loading && breakdown == null) {
            LoadingRow()
        }

        if (breakdown != null) {
            LazyColumn(Modifier.fillMaxSize()) {
                when (section) {
                    "free" -> freeSection(breakdown, onOpenAccount)
                    "committed" -> committedSection(breakdown, onOpenStatement)
                    else -> patrimonySection(breakdown, onOpenAccount)
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.patrimonySection(
    breakdown: WealthBreakdown,
    onOpenAccount: (Int) -> Unit
) {
    item(key = "total") {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            MoneyText(cents = breakdown.totalCents, size = MoneySize.Hero, color = TextStrong)
        }
    }

    if (breakdown.accounts.isNotEmpty()) {
        item(key = "accounts-header") { SectionHeader("Contas") }
        accountRows(breakdown.accounts, "account", onOpenAccount)
    }

    if (breakdown.fixedIncome.isNotEmpty()) {
        item(key = "fixed-header") { SectionHeader("Renda fixa") }
        accountRows(breakdown.fixedIncome, "fixed", onOpenAccount)
    }

    if (breakdown.separatedCents > 0 || breakdown.hasVariable) {
        item(key = "out-header") { SectionHeader("Fica de fora") }

        item(key = "out") {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (breakdown.separatedCents > 0) {
                    Line("Dinheiro separado", breakdown.separatedCents, TextMuted)
                }
                if (breakdown.hasVariable) {
                    Text(
                        text = "Ações e cripto ficam de fora.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.freeSection(
    breakdown: WealthBreakdown,
    onOpenAccount: (Int) -> Unit
) {
    val availableFixed = breakdown.fixedIncome.filterNot { it.locked }
    val accountsCents = breakdown.accountsCents
    val availableFixedCents = availableFixed.sumOf { it.balanceCents ?: it.initialBalanceCents }
    val available = accountsCents + availableFixedCents
    val negative = breakdown.freeCents < 0

    item(key = "free-total") {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            MoneyText(
                cents = kotlin.math.abs(breakdown.freeCents),
                size = MoneySize.Hero,
                color = if (negative) Negative else Positive
            )
        }
    }

    // 1. O que está disponível
    item(key = "available-header") {
        SectionHeader(
            title = "Disponível",
            action = { MoneyText(cents = available, size = MoneySize.Inline, color = TextMuted) }
        )
    }

    if (breakdown.accounts.isNotEmpty()) {
        item(key = "accounts-label") { SubLabel("Contas") }
        accountRows(breakdown.accounts, "free-account", onOpenAccount)
    }

    if (availableFixed.isNotEmpty()) {
        item(key = "fixed-label") { SubLabel("Renda fixa com resgate livre") }
        accountRows(availableFixed, "free-fixed", onOpenAccount)
    }

    // 2. O que ainda vai sair
    item(key = "committed-header") {
        SectionHeader(
            title = "Faturas em aberto",
            action = { MoneyText(cents = breakdown.committedCents, size = MoneySize.Inline, color = Negative) }
        )
    }

    if (breakdown.committedItems.isEmpty()) {
        item(key = "committed-empty") { Muted("Nenhuma fatura em aberto.") }
    } else {
        committedRows(breakdown.committedItems, "free-committed", {})
    }

    // 3. O que está preso
    if (breakdown.lockedAccounts.isNotEmpty()) {
        item(key = "locked-header") {
            SectionHeader(
                title = "CDB com resgate em data futura",
                action = { MoneyText(cents = breakdown.lockedCents, size = MoneySize.Inline, color = Warning) }
            )
        }
        accountRows(breakdown.lockedAccounts, "free-locked", onOpenAccount)
    }

    // 4. A conta
    item(key = "equation-header") { SectionHeader("A conta") }

    item(key = "equation") {
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Line("Patrimônio", breakdown.totalCents, TextStrong)
            if (breakdown.lockedCents > 0) {
                Line("CDB com resgate em data futura", breakdown.lockedCents, Warning, "−")
            }
            Line("Disponível agora", available, TextStrong)
            if (breakdown.committedCents > 0) {
                Line("Faturas em aberto", breakdown.committedCents, Negative, "−")
            }
            HairlineDivider()
            Line(
                label = if (negative) "Faltando" else "Livre",
                cents = kotlin.math.abs(breakdown.freeCents),
                color = if (negative) Negative else Positive,
                prefix = if (negative) "−" else ""
            )
        }
    }

    // 5. O que não entra
    if (breakdown.separatedCents > 0 || breakdown.hasVariable) {
        item(key = "free-out-header") { SectionHeader("Fora da conta") }

        item(key = "free-out") {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (breakdown.separatedCents > 0) {
                    Line("Dinheiro separado", breakdown.separatedCents, TextMuted)
                }
                if (breakdown.hasVariable) {
                    Text(
                        text = "Ações e cripto ficam de fora.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
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

private fun androidx.compose.foundation.lazy.LazyListScope.committedSection(
    breakdown: WealthBreakdown,
    onOpenStatement: (Int) -> Unit
) {
    item(key = "committed-total") {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            MoneyText(cents = breakdown.committedCents, size = MoneySize.Hero, color = TextStrong)
        }
    }

    val current = breakdown.committedItems.filter { it.current }
    val earlier = breakdown.committedItems.filterNot { it.current }

    if (current.isNotEmpty()) {
        item(key = "current-header") { SectionHeader("Faturas atuais") }
        committedRows(current, "current", onOpenStatement)
    }

    if (earlier.isNotEmpty()) {
        item(key = "earlier-header") { SectionHeader("Anteriores sem pagamento") }
        committedRows(earlier, "earlier", onOpenStatement)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.committedRows(
    rows: List<CommittedItemDto>,
    prefix: String,
    onOpenStatement: (Int) -> Unit
) {
    itemsIndexed(rows, key = { index, row -> "$prefix-${row.cardId}-${row.reference}-$index" }) { index, row ->
        Column(Modifier.grouped(index, rows.size)) {
            LedgerRow(
                title = row.cardName ?: "Cartão",
                subtitle = listOfNotNull(referenceLabel(row.reference), row.dueOn?.let { "vence ${shortDue(it)}" }).joinToString(" · "),
                showMarker = false,
                icon = com.archieapps.finances.ui.theme.AppIcons.CreditCard,
                navigates = true,
                onClick = { onOpenStatement(row.cardId) },
                trailing = {
                    MoneyText(cents = row.totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                }
            )
        }
    }
}

private val DueDay = java.time.format.DateTimeFormatter.ofPattern("dd/MM")

private fun shortDue(value: String): String =
    runCatching { java.time.LocalDate.parse(value).format(DueDay) }.getOrDefault(value)

private fun referenceLabel(reference: String): String {
    val months = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    val parts = reference.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return reference

    return "fatura de ${months.getOrElse(month - 1) { reference }}/${parts[0]}"
}

private fun androidx.compose.foundation.lazy.LazyListScope.accountRows(
    accounts: List<AccountDto>,
    prefix: String,
    onOpen: (Int) -> Unit
) {
    itemsIndexed(accounts, key = { _, account -> "$prefix-${account.id}" }) { index, account ->
        Column(Modifier.grouped(index, accounts.size)) {
            LedgerRow(
                title = account.name,
                subtitle = listOfNotNull(account.institution, lockLabel(account)).joinToString(" · ").ifBlank { null },
                markerHex = account.color,
                navigates = true,
                onClick = { onOpen(account.id) },
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
    }
}

@Composable
private fun Line(label: String, cents: Long, color: Color, prefix: String = "") {
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

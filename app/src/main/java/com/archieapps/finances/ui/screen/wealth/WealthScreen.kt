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
fun WealthScreen(onBack: () -> Unit, onOpenAccount: (Int) -> Unit) {
    val model = appViewModel { container -> WealthViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    val breakdown = state.breakdown

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Seu dinheiro",
            subtitle = "Como o patrimônio e o livre são calculados",
            onBack = onBack
        )

        ErrorBanner(state.error)

        if (state.loading && breakdown == null) {
            LoadingRow()
        }

        if (breakdown != null) {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "total") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text("Patrimônio", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        Spacer(Modifier.height(4.dp))
                        MoneyText(cents = breakdown.totalCents, size = MoneySize.Hero, color = TextStrong)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "É o que é seu e está em conta ou em renda fixa. Ações e cripto não entram.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
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

                item(key = "free-header") { SectionHeader("Quanto está livre") }

                item(key = "free") {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Line("Patrimônio", breakdown.totalCents, TextStrong)
                        if (breakdown.committedCents > 0) {
                            Line("Faturas de cartão em aberto", breakdown.committedCents, Negative, "−")
                        }
                        if (breakdown.lockedCents > 0) {
                            Line("CDB com resgate em data futura", breakdown.lockedCents, Warning, "−")
                        }
                        HairlineDivider()
                        Line(
                            label = if (breakdown.freeCents < 0) "A descoberto" else "Livre",
                            cents = kotlin.math.abs(breakdown.freeCents),
                            color = if (breakdown.freeCents < 0) Negative else Positive,
                            prefix = if (breakdown.freeCents < 0) "−" else ""
                        )
                        Text(
                            text = "Livre é quanto sobraria se você pagasse hoje todas as faturas abertas, sem mexer em CDB que só resgata numa data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                if (breakdown.separatedCents > 0 || breakdown.hasVariable) {
                    item(key = "out-header") { SectionHeader("Fica de fora") }

                    item(key = "out") {
                        Column(
                            Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (breakdown.separatedCents > 0) {
                                Column {
                                    Line("Dinheiro separado", breakdown.separatedCents, TextMuted)
                                    Text(
                                        text = "Está nas suas contas, mas não é seu.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                }
                            }
                            if (breakdown.hasVariable) {
                                Text(
                                    text = "Ações e cripto: o valor oscila, então não entram no patrimônio nem no livre. Para ver, abra Contas ou toque em \"Ações e cripto\" na tela inicial.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
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

package com.archieapps.finances.ui.screen.accounts

import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Primary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ChoiceChips
import com.archieapps.finances.ui.components.ColorChoice
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.MoveControls
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

private val Kinds = listOf(
    "checking" to "Corrente",
    "savings" to "Caixinha",
    "investment" to "Investimento",
    "cash" to "Dinheiro",
    "wallet" to "Digital"
)

private val InvestmentTypes = listOf(
    "fixed_income" to "Renda fixa (CDB)",
    "stock" to "Ações",
    "crypto" to "Cripto"
)

private val Ownership = listOf(
    "own" to "Meu dinheiro",
    "separated" to "Separado (não é meu)"
)

private val LockDay = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun lockLabel(account: AccountDto): String? {
    val until = account.lockedUntil ?: return null
    val text = runCatching { java.time.LocalDate.parse(until).format(LockDay) }.getOrDefault(until)

    return if (account.locked) "Resgate só em $text" else null
}

private fun accountSubtitle(account: AccountDto): String? {
    val base = account.investmentTypeLabel?.let { "${account.kindLabel} · $it" } ?: account.kindLabel
    val kind = listOfNotNull(base, lockLabel(account)).joinToString(" · ")

    return when {
        account.archived -> "$kind · arquivada"
        account.kindLabel == account.name && account.investmentTypeLabel == null -> null
        else -> kind
    }
}

@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onOpenAccount: (Int) -> Unit = {},
    editAccountId: Int? = null
) {
    val model = appViewModel { container -> AccountsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    var pendingEdit by remember(editAccountId) { mutableStateOf(editAccountId) }

    LaunchedEffect(state.accounts, pendingEdit) {
        val id = pendingEdit ?: return@LaunchedEffect
        if (model.startEditById(id)) pendingEdit = null
    }

    val totalCents = patrimonyCents(state.accounts)
    val separatedTotal = separatedCents(state.accounts)
    val separated = separatedAccounts(state.accounts)
    val variable = variableAccounts(state.accounts)
    val variableTotal = variableCents(state.accounts)
    var showVariable by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Contas",
            subtitle = "Onde o dinheiro fica",
            onBack = onBack,
            action = { IconAction(AppIcons.Add, onClick = model::startCreate, contentDescription = "Nova", tint = Primary) }
        )

        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip(
                text = if (state.reordering) "Concluir ordem" else "Reordenar",
                active = state.reordering,
                onClick = model::toggleReordering
            )
            if (!state.reordering) {
                Chip(
                    text = "Arquivadas",
                    active = state.includeArchived,
                    onClick = model::toggleArchived
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        ErrorBanner(state.error)

        if (state.loading && state.accounts.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.accounts.isEmpty()) {
            EmptyState(
                headline = "Nenhuma conta ainda",
                                action = { QuietAction("Criar a primeira", model::startCreate) }
            )
        }

        if (state.reordering) {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(state.accounts, key = { _, account -> account.id }) { index, account ->
                    Column(Modifier.grouped(index, state.accounts.size)) {
                        LedgerRow(
                            title = account.name,
                            subtitle = account.institution ?: accountSubtitle(account),
                            markerHex = account.color,
                            dimmed = account.archived,
                            trailing = {
                                MoveControls(
                                    canMoveUp = index > 0,
                                    canMoveDown = index < state.accounts.lastIndex,
                                    onMoveUp = { model.move(index, index - 1) },
                                    onMoveDown = { model.move(index, index + 1) }
                                )
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.accounts.isNotEmpty()) {
                    item(key = "patrimony") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Eyebrow("Patrimônio")
                            MoneyText(cents = totalCents, size = MoneySize.Section, color = TextStrong)
                        }
                    }
                }

                groupByInstitution(ownAccounts(state.accounts)).forEach { group ->
                    item(key = "group-${group.institution?.lowercase() ?: "none"}") {
                        SectionHeader(
                            title = group.institution ?: "Sem banco",
                            action = {
                                MoneyText(
                                    cents = group.totalCents,
                                    size = MoneySize.Inline,
                                    color = TextMuted
                                )
                            }
                        )
                    }

                    itemsIndexed(group.accounts, key = { _, account -> account.id }) { index, account ->
                        Column(Modifier.grouped(index, group.accounts.size)) {
                            LedgerRow(
                                title = account.name,
                                subtitle = accountSubtitle(account),
                                markerHex = account.color,
                                dimmed = account.archived,
                                navigates = true,
                                onClick = { onOpenAccount(account.id) },
                                trailing = {
                                    MoneyText(
                                        cents = account.balanceCents ?: account.initialBalanceCents,
                                        size = MoneySize.Row,
                                        showSymbol = false,
                                        color = if (account.archived) TextMuted else TextStrong
                                    )
                                }
                            )
                        }
                    }
                }

                if (separated.isNotEmpty()) {
                    item(key = "separated-header") {
                        SectionHeader(
                            title = "Dinheiro separado",
                            action = { MoneyText(cents = separatedTotal, size = MoneySize.Inline, color = TextMuted) }
                        )
                    }

                    itemsIndexed(separated, key = { _, account -> account.id }) { index, account ->
                        Column(Modifier.grouped(index, separated.size)) {
                            LedgerRow(
                                title = account.name,
                                subtitle = listOfNotNull(account.institution, accountSubtitle(account)).joinToString(" · ").ifBlank { null },
                                markerHex = account.color,
                                dimmed = account.archived,
                                navigates = true,
                                onClick = { onOpenAccount(account.id) },
                                trailing = {
                                    MoneyText(
                                        cents = account.currentCents(),
                                        size = MoneySize.Row,
                                        showSymbol = false,
                                        color = if (account.archived) TextMuted else TextStrong
                                    )
                                }
                            )
                        }
                    }
                }

                if (variable.isNotEmpty()) {
                    item(key = "variable-header") {
                        androidx.compose.foundation.layout.Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showVariable = !showVariable }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showVariable) "Ações e cripto" else "Ações e cripto",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            if (showVariable) {
                                MoneyText(cents = variableTotal, size = MoneySize.Inline, color = TextMuted)
                            }
                        }
                    }

                    if (showVariable) {
                        itemsIndexed(variable, key = { _, account -> account.id }) { index, account ->
                            Column(Modifier.grouped(index, variable.size)) {
                                LedgerRow(
                                    title = account.name,
                                    subtitle = listOfNotNull(account.institution, accountSubtitle(account)).joinToString(" · ").ifBlank { null },
                                    markerHex = account.color,
                                    dimmed = true,
                                    navigates = true,
                                    onClick = { onOpenAccount(account.id) },
                                    trailing = {
                                        MoneyText(
                                            cents = account.currentCents(),
                                            size = MoneySize.Row,
                                            showSymbol = false,
                                            color = TextMuted
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(
            title = if (form.id == null) "Nova conta" else "Editar conta",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "Nome",
                value = form.name,
                onValueChange = { value -> model.editForm { it.copy(name = value) } },
                placeholder = "Conta corrente, caixinha de viagens, CDB…"
            )

            LabeledField(
                label = "Banco",
                value = form.institution,
                onValueChange = { value -> model.editForm { it.copy(institution = value) } },
                placeholder = "Nubank, Inter… (vazio para dinheiro em espécie)"
            )

            ChoiceChips(
                label = "Tipo",
                options = Kinds,
                selected = form.kind,
                onSelect = { value -> model.editForm { it.copy(kind = value) } }
            )

            if (form.kind == "investment") {
                ChoiceChips(
                    label = "Tipo de investimento",
                    options = InvestmentTypes,
                    selected = form.investmentType,
                    onSelect = { value -> model.editForm { it.copy(investmentType = value) } }
                )

                if (form.investmentType == "fixed_income") {
                    ChoiceChips(
                        label = "Resgate",
                        options = listOf("free" to "Quando eu quiser", "date" to "Só em uma data"),
                        selected = if (form.lockedUntil == null) "free" else "date",
                        onSelect = { value ->
                            model.editForm {
                                it.copy(lockedUntil = if (value == "free") null else (it.lockedUntil ?: java.time.LocalDate.now().plusMonths(6)))
                            }
                        }
                    )

                    if (form.lockedUntil != null) {
                        com.archieapps.finances.ui.components.DateChips(
                            date = form.lockedUntil,
                            onChange = { value -> model.editForm { it.copy(lockedUntil = value) } }
                        )
                    }
                }

            }

            ChoiceChips(
                label = "De quem é esse dinheiro?",
                options = Ownership,
                selected = if (form.separated) "separated" else "own",
                onSelect = { value -> model.editForm { it.copy(separated = value == "separated") } }
            )

            if (form.separated) {
            }

            MoneyInput(
                label = if (form.kind == "investment" && form.investmentType != "fixed_income") "Saldo em dinheiro (sem os ativos)" else "Saldo inicial",
                reais = form.reais,
                centavos = form.centavos,
                onReaisChange = { value -> model.editForm { it.copy(reais = value) } },
                onCentavosChange = { value -> model.editForm { it.copy(centavos = value) } }
            )

            ColorChoice(
                selected = form.color,
                onSelect = { value -> model.editForm { it.copy(color = value) } }
            )

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.id == null) "Criar conta" else "Salvar",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )

            if (form.id != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuietAction(
                        label = if (form.archived) "Reativar" else "Arquivar",
                        onClick = { model.setArchived(!form.archived) }
                    )
                    QuietAction(
                        label = "Excluir",
                        onClick = model::delete,
                        destructive = true
                    )
                }
            }
        }
    }
}

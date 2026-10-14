package com.archieapps.finances.ui.screen.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ChoiceChips
import com.archieapps.finances.ui.components.ColorChoice
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
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
    "cash" to "Dinheiro",
    "wallet" to "Digital",
    "savings" to "Poupança",
    "investment" to "Investimento"
)

private fun accountSubtitle(account: com.archieapps.finances.data.dto.AccountDto): String? = when {
    account.archived -> "${account.kindLabel} · arquivada"
    account.kindLabel == account.name -> null
    else -> account.kindLabel
}

@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val model = appViewModel { container -> AccountsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Contas",
            subtitle = "Onde o dinheiro fica",
            onBack = onBack,
            action = { TextAction("Nova", model::startCreate) }
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
                hint = "Cadastre onde o seu dinheiro fica: conta do banco, dinheiro na carteira, carteira digital.",
                action = { QuietAction("Criar a primeira", model::startCreate) }
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(state.accounts, key = { _, account -> account.id }) { index, account ->
                LedgerRow(
                    title = account.name,
                    subtitle = accountSubtitle(account),
                    markerHex = account.color,
                    dimmed = account.archived,
                    onClick = if (state.reordering) null else ({ model.startEdit(account) }),
                    trailing = {
                        if (state.reordering) {
                            MoveControls(
                                canMoveUp = index > 0,
                                canMoveDown = index < state.accounts.lastIndex,
                                onMoveUp = { model.move(index, index - 1) },
                                onMoveDown = { model.move(index, index + 1) }
                            )
                        } else {
                            MoneyText(
                                cents = account.balanceCents ?: account.initialBalanceCents,
                                size = MoneySize.Row,
                                showSymbol = false,
                                color = if (account.archived) TextMuted else TextStrong
                            )
                        }
                    }
                )
                HairlineDivider(inset = 20)
            }

            item { Spacer(Modifier.height(40.dp)) }
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
                placeholder = "Nubank, carteira, poupança…"
            )

            ChoiceChips(
                label = "Tipo",
                options = Kinds,
                selected = form.kind,
                onSelect = { value -> model.editForm { it.copy(kind = value) } }
            )

            MoneyInput(
                label = "Saldo inicial",
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
                Text(
                    text = "Arquivar esconde a conta das listas sem apagar o histórico.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

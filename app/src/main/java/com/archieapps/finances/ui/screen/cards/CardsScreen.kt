package com.archieapps.finances.ui.screen.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ColorChoice
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.components.parseHex
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun CardsScreen(onOpenStatement: (Int) -> Unit) {
    val model = appViewModel { container -> CardsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Cartões",
            subtitle = "Toque no cartão para ver a fatura",
            action = { TextAction("Novo", model::startCreate) }
        )

        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Chip(
                text = "Arquivados",
                active = state.includeArchived,
                onClick = model::toggleArchived
            )
        }

        Spacer(Modifier.height(8.dp))
        ErrorBanner(state.error)

        if (state.loading && state.cards.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.cards.isEmpty()) {
            EmptyState(
                headline = "Nenhum cartão",
                hint = "Cadastre o cartão com o dia de fechamento e de vencimento. As compras vão para a fatura certa sozinhas.",
                action = { QuietAction("Cadastrar cartão", model::startCreate) }
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.cards, key = { it.id }) { card ->
                CardTile(
                    card = card,
                    onClick = { onOpenStatement(card.id) },
                    onEdit = { model.startEdit(card) }
                )
            }
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(
            title = if (form.id == null) "Novo cartão" else "Editar cartão",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "Nome",
                value = form.name,
                onValueChange = { value -> model.editForm { it.copy(name = value) } },
                placeholder = "Nubank, Inter, Itaú…"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    label = "Fecha dia",
                    value = form.closingDay,
                    onValueChange = { value ->
                        model.editForm { it.copy(closingDay = value.filter(Char::isDigit).take(2)) }
                    },
                    placeholder = "28",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                LabeledField(
                    label = "Vence dia",
                    value = form.dueDay,
                    onValueChange = { value ->
                        model.editForm { it.copy(dueDay = value.filter(Char::isDigit).take(2)) }
                    },
                    placeholder = "5",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            MoneyInput(
                label = "Limite",
                reais = form.limitReais,
                centavos = form.limitCentavos,
                onReaisChange = { value -> model.editForm { it.copy(limitReais = value) } },
                onCentavosChange = { value -> model.editForm { it.copy(limitCentavos = value) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    label = "Bandeira",
                    value = form.brand,
                    onValueChange = { value -> model.editForm { it.copy(brand = value) } },
                    placeholder = "visa",
                    modifier = Modifier.weight(1f)
                )
                LabeledField(
                    label = "4 dígitos",
                    value = form.lastDigits,
                    onValueChange = { value ->
                        model.editForm { it.copy(lastDigits = value.filter(Char::isDigit).take(4)) }
                    },
                    placeholder = "1234",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.accounts.isNotEmpty()) {
                Column {
                    Eyebrow("Conta que paga a fatura")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(
                            text = "Nenhuma",
                            active = form.accountId == null,
                            onClick = { model.editForm { it.copy(accountId = null) } }
                        )
                        state.accounts.take(3).forEach { account ->
                            Chip(
                                text = account.name,
                                active = form.accountId == account.id,
                                onClick = { model.editForm { it.copy(accountId = account.id) } }
                            )
                        }
                    }
                }
            }

            ColorChoice(
                selected = form.color,
                onSelect = { value -> model.editForm { it.copy(color = value) } }
            )

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.id == null) "Criar cartão" else "Salvar",
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
                    QuietAction(label = "Excluir", onClick = model::delete, destructive = true)
                }
            }
        }
    }
}

@Composable
private fun CardTile(card: CardDto, onClick: () -> Unit, onEdit: () -> Unit) {
    val tint = parseHex(card.color) ?: Primary

    Column(
        Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = if (card.archived) 0.05f else 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleLarge,
                color = if (card.archived) TextMuted else TextStrong,
                modifier = Modifier.weight(1f)
            )
            if (card.lastDigits != null) {
                Text(
                    text = "•••• ${card.lastDigits}",
                    style = TextStyle(fontFamily = Figures, fontSize = 13.sp, fontWeight = FontWeight.W500),
                    color = TextFaint,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }

            TextAction("Editar", onEdit)
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Fecha ${card.closingDay} · vence ${card.dueDay}")
                if (card.archived) {
                    Spacer(Modifier.height(4.dp))
                    Text("Arquivado", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }

            val limit = card.limitCents
            val used = card.usedCents

            if (used != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Eyebrow("Fatura atual")
                    Spacer(Modifier.height(4.dp))
                    MoneyText(cents = used, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                    if (limit != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "de R$ ${Money.full(limit)} de limite",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextFaint
                        )
                    }
                }
            } else if (limit != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Eyebrow("Limite")
                    Spacer(Modifier.height(4.dp))
                    MoneyText(cents = limit, size = MoneySize.Row, showSymbol = false, color = TextMuted)
                }
            }
        }
    }
}

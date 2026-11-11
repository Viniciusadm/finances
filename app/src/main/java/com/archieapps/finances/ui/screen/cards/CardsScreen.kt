package com.archieapps.finances.ui.screen.cards

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.ProgressTrack
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Ink
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

    Refreshable(refreshing = state.loading && state.cards.isNotEmpty(), onRefresh = model::refresh) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Cartões",
                    subtitle = "Toque no cartão para ver a fatura",
                    action = { IconAction(AppIcons.Add, onClick = model::startCreate, contentDescription = "Novo cartão", tint = Primary) }
                )
            }

            item {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    Chip(
                        text = "Mostrar arquivados",
                        active = state.includeArchived,
                        icon = AppIcons.Archive,
                        onClick = model::toggleArchived
                    )
                }
            }

            item { ErrorBanner(state.error) }

            if (state.loading && state.cards.isEmpty()) {
                item { LoadingRow() }
            }

            if (!state.loading && state.cards.isEmpty()) {
                item {
                    EmptyState(
                        headline = "Nenhum cartão",
                        hint = "Cadastre o cartão com o dia de fechamento e de vencimento. As compras vão para a fatura certa sozinhas.",
                        icon = AppIcons.CreditCard,
                        action = { QuietAction("Cadastrar cartão", model::startCreate, icon = AppIcons.Add) }
                    )
                }
            }

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

            LabeledField(
                label = "Banco",
                value = form.institution,
                onValueChange = { value -> model.editForm { it.copy(institution = value) } },
                placeholder = "Nubank, Inter…",
                supporting = "Ajuda a entender \"no crédito do Nubank\" no lançamento por texto."
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(
                            text = "Nenhuma",
                            active = form.accountId == null,
                            onClick = { model.editForm { it.copy(accountId = null) } }
                        )
                        state.accounts.forEach { account ->
                            Chip(
                                text = account.name,
                                active = form.accountId == account.id,
                                onClick = { model.onAccountForCard(account.id) }
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
                icon = AppIcons.Check,
                label = if (form.id == null) "Criar cartão" else "Salvar",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )

            if (form.id != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuietAction(
                        label = if (form.archived) "Reativar" else "Arquivar",
                        onClick = { model.setArchived(!form.archived) },
                        icon = AppIcons.Archive,
                        modifier = Modifier.weight(1f)
                    )
                    QuietAction(label = "Excluir", onClick = model::delete, destructive = true, icon = AppIcons.Delete, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CardTile(card: CardDto, onClick: () -> Unit, onEdit: () -> Unit) {
    val tint = parseHex(card.color) ?: Primary
    val surface = lerp(tint, Ink, 0.35f)
    val limit = card.limitCents
    val used = card.usedCents

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .alpha(if (card.archived) 0.5f else 1f)
            .clip(RoundedCornerShape(24.dp))
            .background(surface)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                val caption = listOfNotNull(card.institution?.takeIf { it.isNotBlank() && it != card.name }, card.brand?.uppercase())
                if (caption.isNotEmpty()) {
                    Text(caption.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                }
            }
            IconAction(
                icon = AppIcons.Edit,
                onClick = onEdit,
                contentDescription = "Editar cartão",
                tint = Color.White,
                container = Color.White.copy(alpha = 0.16f),
                size = 38
            )
        }

        Spacer(Modifier.height(22.dp))

        Text("Fatura atual", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.height(2.dp))
        MoneyText(
            cents = used ?: 0,
            size = MoneySize.Section,
            color = Color.White,
            symbolColor = Color.White.copy(alpha = 0.7f),
            weight = FontWeight.W800
        )

        if (limit != null && limit > 0) {
            Spacer(Modifier.height(12.dp))
            ProgressTrack(
                fraction = ((used ?: 0).toFloat() / limit),
                color = Color.White,
                track = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "R$ ${Money.full((limit - (used ?: 0)).coerceAtLeast(0))} disponível de R$ ${Money.full(limit)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Fecha dia ${card.closingDay} · vence dia ${card.dueDay}" + if (card.archived) " · arquivado" else "",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            if (card.lastDigits != null) {
                Text(
                    text = "•••• ${card.lastDigits}",
                    style = TextStyle(fontFamily = Figures, fontSize = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 1.sp),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

package com.archieapps.finances.ui.screen.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.PageScaffold
import com.archieapps.finances.ui.components.SurfaceCard
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Warning
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun ItemsSheet(entryId: Int, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val model = appViewModel(key = "items-$entryId") { container ->
        ItemsViewModel(container.repository, entryId)
    }
    val state by model.state.collectAsStateWithLifecycle()

    PageScaffold(
        title = "Itens da compra",
        onBack = onDismiss,
        footer = {
            PrimaryAction(
                label = "Salvar itens",
                onClick = { model.save(onSaved) },
                enabled = !state.saving,
                loading = state.saving
            )
        }
    ) {
        BalanceCard(state)
        ItemsCard(state, model)
        NewItemCard(state, model)
        ErrorBanner(state.error)
    }
}

@Composable
private fun BalanceCard(state: ItemsState) {
    val statusColor = when {
        state.items.isEmpty() -> TextMuted
        state.difference == 0L -> Positive
        else -> Warning
    }

    SurfaceCard(contentPadding = PaddingValues(20.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Soma dos itens")
                Spacer(Modifier.height(6.dp))
                MoneyText(cents = state.itemsTotal, size = MoneySize.Section, color = TextStrong)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Eyebrow("Lançamento")
                Spacer(Modifier.height(6.dp))
                MoneyText(cents = state.entryAmount, size = MoneySize.Section, color = TextMuted)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(statusColor.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = when {
                    state.items.isEmpty() -> "Nenhum item ainda"
                    state.difference == 0L -> "Fecha com o lançamento"
                    state.difference > 0 -> "R$ ${Money.full(state.difference)} acima do lançamento"
                    else -> "Faltam R$ ${Money.full(-state.difference)}"
                },
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}

@Composable
private fun ItemsCard(state: ItemsState, model: ItemsViewModel) {
    if (state.items.isEmpty()) return

    SurfaceCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        state.items.forEachIndexed { index, item ->
            if (index > 0) HairlineDivider()

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifBlank { "Item sem nome" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextStrong
                    )
                    Text(
                        text = itemLine(item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                MoneyText(cents = item.totalCents, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                IconAction(
                    icon = AppIcons.Close,
                    onClick = { model.removeAt(index) },
                    contentDescription = "Tirar item",
                    tint = Negative,
                    size = 36,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun NewItemCard(state: ItemsState, model: ItemsViewModel) {
    SurfaceCard(contentPadding = PaddingValues(20.dp)) {
        Eyebrow("Novo item")
        Spacer(Modifier.height(12.dp))

        LabeledField(
            label = "Produto ou nome",
            value = state.draftName,
            onValueChange = model::onDraftName,
            placeholder = "arroz, detergente…"
        )

        if (state.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.suggestions.take(5).forEach { product ->
                    Chip(
                        text = product.name,
                        active = state.draftProductId == product.id,
                        onClick = { model.pickProduct(product) }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            LabeledField(
                label = "Qtd.",
                value = state.draftQuantity,
                onValueChange = model::onDraftQuantity,
                placeholder = "1",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                modifier = Modifier.weight(0.35f)
            )
            MoneyInput(
                label = "Preço por unidade",
                reais = state.draftReais,
                centavos = state.draftCentavos,
                onReaisChange = model::onDraftReais,
                onCentavosChange = model::onDraftCentavos,
                modifier = Modifier.weight(0.65f)
            )
        }

        Spacer(Modifier.height(16.dp))

        QuietAction(
            label = "Adicionar à lista",
            onClick = model::addDraft,
            icon = AppIcons.Add,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun itemLine(item: DraftItem): String {
    val quantity = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()

    val product = item.productName?.takeIf { it != item.name }

    return "${quantity}x R$ ${Money.full(item.unitPriceCents)}" + (product?.let { " · $it" } ?: "")
}

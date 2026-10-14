package com.archieapps.finances.ui.screen.composer

import androidx.compose.foundation.layout.Arrangement
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

    EditorSheet(title = "Itens da compra", onDismiss = onDismiss) {
        Text(
            text = balanceLine(state),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                state.difference == 0L -> Positive
                else -> TextMuted
            }
        )

        HairlineDivider()

        state.items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        cents = item.totalCents,
                        size = MoneySize.Row,
                        showSymbol = false,
                        color = TextStrong
                    )
                    QuietAction(label = "Tirar", onClick = { model.removeAt(index) }, destructive = true)
                }
            }
            HairlineDivider()
        }

        Eyebrow("Adicionar item")

        LabeledField(
            label = "Produto ou nome",
            value = state.draftName,
            onValueChange = model::onDraftName,
            placeholder = "arroz, detergente…"
        )

        if (state.suggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.suggestions.take(3).forEach { product ->
                    Chip(
                        text = product.name,
                        active = state.draftProductId == product.id,
                        onClick = { model.pickProduct(product) }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(
                label = "Quantidade",
                value = state.draftQuantity,
                onValueChange = model::onDraftQuantity,
                placeholder = "1",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }

        MoneyInput(
            label = "Preço por unidade",
            reais = state.draftReais,
            centavos = state.draftCentavos,
            onReaisChange = model::onDraftReais,
            onCentavosChange = model::onDraftCentavos
        )

        QuietAction(
            label = "Adicionar à lista",
            onClick = model::addDraft
        )

        ErrorBanner(state.error)

        Spacer(Modifier.height(2.dp))

        PrimaryAction(
            label = "Salvar itens",
            onClick = { model.save(onSaved) },
            enabled = !state.saving,
            loading = state.saving
        )
    }
}

private fun balanceLine(state: ItemsState): String {
    val items = "Itens R$ ${Money.full(state.itemsTotal)}"
    val entry = "lançamento R$ ${Money.full(state.entryAmount)}"

    return when {
        state.items.isEmpty() -> "Nenhum item. O lançamento vale R$ ${Money.full(state.entryAmount)}."
        state.difference == 0L -> "$items · fecha com o $entry."
        state.difference > 0 -> "$items · R$ ${Money.full(state.difference)} acima do $entry."
        else -> "$items · faltam R$ ${Money.full(-state.difference)} para o $entry."
    }
}

private fun itemLine(item: DraftItem): String {
    val quantity = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()

    val product = item.productName?.takeIf { it != item.name }

    return "${quantity}x R$ ${Money.full(item.unitPriceCents)}" + (product?.let { " · $it" } ?: "")
}

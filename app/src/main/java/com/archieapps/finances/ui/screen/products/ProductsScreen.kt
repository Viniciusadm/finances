package com.archieapps.finances.ui.screen.products

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.PriceHistoryDto
import com.archieapps.finances.data.dto.ProductDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM/yy", PtBr)

@Composable
fun ProductsScreen(onBack: () -> Unit) {
    val model = appViewModel { container -> ProductsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Produtos",
            subtitle = "O preço vem das compras",
            onBack = onBack,
            action = { TextAction("Novo", model::startCreate) }
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            LabeledField(
                label = "Buscar",
                value = state.search,
                onValueChange = model::onSearch,
                placeholder = "arroz, café, código de barras…"
            )
        }

        if (state.categories.isNotEmpty()) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categories.forEach { category ->
                    Chip(
                        text = category.name,
                        active = state.categoryFilter == category.id,
                        onClick = { model.setCategory(category.id) }
                    )
                }
            }
        }

        ErrorBanner(state.error)

        if (state.loading && state.products.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.products.isEmpty()) {
            EmptyState(
                headline = if (state.search.isBlank()) "Nenhum produto" else "Nada com esse nome",
                hint = "Produtos servem para saber quanto você pagou da última vez — e, no caso de remédio, quanto custa cada dose.",
                action = { QuietAction("Criar produto", model::startCreate) }
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.products, key = { it.id }) { product ->
                ProductLine(product = product, onClick = { model.openDetail(product) })
                HairlineDivider(inset = 20)
            }

            if (state.hasMore) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QuietAction(label = "Carregar mais", onClick = model::loadMore)
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    val detail = state.detail

    if (detail != null) {
        EditorSheet(title = detail.product.name, onDismiss = model::dismissDetail) {
            Text(
                text = detailSummary(detail.product),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            if (detail.history.isEmpty()) {
                Text(
                    text = "Nenhuma compra registrada com este produto ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextFaint
                )
            } else {
                HairlineDivider()
                Eyebrow("Histórico de preço")

                detail.history.forEachIndexed { index, entry ->
                    HistoryRow(
                        entry = entry,
                        previous = detail.history.getOrNull(index + 1)
                    )
                }
            }

            HairlineDivider()

            QuietAction(label = "Editar produto", onClick = { model.startEdit(detail.product) })
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(
            title = if (form.id == null) "Novo produto" else "Editar produto",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "Nome",
                value = form.name,
                onValueChange = { value -> model.editForm { it.copy(name = value) } },
                placeholder = "Arroz Tio João 5kg"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    label = "Marca",
                    value = form.brand,
                    onValueChange = { value -> model.editForm { it.copy(brand = value) } },
                    placeholder = "Tio João",
                    modifier = Modifier.weight(1f)
                )
                LabeledField(
                    label = "Unidade",
                    value = form.unit,
                    onValueChange = { value -> model.editForm { it.copy(unit = value) } },
                    placeholder = "un, kg, cx",
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.categories.isNotEmpty()) {
                Column {
                    Eyebrow("Categoria do produto")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.categories.forEach { category ->
                            Chip(
                                text = category.name,
                                active = form.categoryId == category.id,
                                onClick = {
                                    model.editForm {
                                        it.copy(categoryId = if (it.categoryId == category.id) null else category.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            LabeledField(
                label = "Código de barras",
                value = form.barcode,
                onValueChange = { value -> model.editForm { it.copy(barcode = value) } },
                placeholder = "opcional"
            )

            LabeledField(
                label = "Tags",
                value = form.tags,
                onValueChange = { value -> model.editForm { it.copy(tags = value) } },
                placeholder = "básico, importado"
            )

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.id == null) "Criar produto" else "Salvar",
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
                Text(
                    text = "Excluir mantém o nome nos itens já comprados; só o vínculo se desfaz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun ProductLine(product: ProductDto, onClick: () -> Unit) {
    LedgerRow(
        title = product.name,
        subtitle = listOfNotNull(
            product.brand,
            product.categoryName,
            product.lastPaidOn?.let { "última em ${shortDate(it)}" }
        ).joinToString(" · ").takeIf { it.isNotBlank() },
        showMarker = false,
        navigates = true,
        dimmed = product.archived,
        onClick = onClick,
        trailing = {
            val last = product.lastPaidCents

            if (last != null) {
                MoneyText(cents = last, size = MoneySize.Row, showSymbol = false, color = TextStrong)
            } else {
                Text("sem compra", style = MaterialTheme.typography.bodyMedium, color = TextFaint)
            }
        }
    )
}

@Composable
private fun HistoryRow(entry: PriceHistoryDto, previous: PriceHistoryDto?) {
    val delta = previous?.let { entry.unitPriceCents - it.unitPriceCents }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.description ?: "Compra",
                style = MaterialTheme.typography.bodyLarge,
                color = TextStrong
            )
            Text(
                text = listOfNotNull(
                    entry.date?.let { shortDate(it) },
                    quantityLabel(entry.quantity)
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                cents = entry.unitPriceCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = TextStrong
            )
            if (delta != null && delta != 0L) {
                Text(
                    text = deltaLabel(delta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (delta > 0) Negative else Positive
                )
            }
        }
    }
}

private fun detailSummary(product: ProductDto): String {
    val parts = listOfNotNull(
        product.brand,
        product.unit?.let { "por $it" },
        product.categoryName,
        product.barcode?.let { "cód. $it" }
    )

    return parts.joinToString(" · ").ifBlank { "Sem marca nem categoria." }
}

private fun quantityLabel(quantity: Double): String {
    val rounded = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()

    return "${rounded}x"
}

private fun deltaLabel(delta: Long): String {
    val sign = if (delta > 0) "+" else "−"
    val absolute = if (delta < 0) -delta else delta

    return "$sign${com.archieapps.finances.core.Money.full(absolute)}"
}

private fun shortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(ShortDate) }.getOrDefault(date)

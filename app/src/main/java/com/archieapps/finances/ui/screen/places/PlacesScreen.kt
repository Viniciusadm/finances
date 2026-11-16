package com.archieapps.finances.ui.screen.places

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.EstablishmentDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.Refreshable
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun PlacesScreen(onBack: () -> Unit) {
    val model = appViewModel { container -> PlacesViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Locais",
            subtitle = "Loja e unidade onde você comprou",
            onBack = onBack,
            action = { IconAction(AppIcons.Add, onClick = model::startCreate, contentDescription = "Novo", tint = Primary) }
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            LabeledField(
                label = "Buscar",
                value = state.search,
                onValueChange = model::onSearch,
                placeholder = "Burger King, Pague Menos…"
            )
        }

        Spacer(Modifier.height(12.dp))
        ErrorBanner(state.error)

        if (state.loading && state.establishments.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.establishments.isEmpty()) {
            EmptyState(
                headline = if (state.search.isBlank()) "Nenhum local" else "Nada com esse nome",
                action = { QuietAction("Criar local", model::startCreate) }
            )
        }

        Refreshable(refreshing = state.loading && state.establishments.isNotEmpty(), onRefresh = model::refresh) {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(state.establishments, key = { _, it -> it.id }) { index, establishment ->
                    Column(Modifier.grouped(index, state.establishments.size)) {
                        LedgerRow(
                            title = establishment.name,
                            subtitle = subtitle(establishment),
                            showMarker = false,
                            icon = AppIcons.Place,
                            onClick = { model.startEdit(establishment) },
                            trailing = {
                                val total = establishment.totalCents

                                if (total != null && total > 0) {
                                    MoneyText(cents = total, size = MoneySize.Row, showSymbol = false, color = TextStrong)
                                }
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    val form = state.form

    if (form != null) {
        val current = state.establishments.firstOrNull { it.id == form.id }

        EditorSheet(
            title = if (form.id == null) "Novo local" else "Editar local",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "Estabelecimento",
                value = form.name,
                onValueChange = { value -> model.editForm { it.copy(name = value) } },
                placeholder = "Burger King"
            )

            if (form.id == null) {
                LabeledField(
                    label = "Unidade (opcional)",
                    value = form.newUnit,
                    onValueChange = { value -> model.editForm { it.copy(newUnit = value) } },
                    placeholder = "Águas Claras Shopping"
                )
            }

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.id == null) "Criar local" else "Salvar",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )

            if (form.id != null && current != null) {
                Column {
                    Eyebrow("Unidades")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        current.places.forEach { place ->
                            Chip(
                                text = (place.name ?: "Única") + (place.entries?.let { " · $it" } ?: ""),
                                active = false,
                                onClick = { model.removeUnit(place.id) },
                                icon = AppIcons.Close
                            )
                        }
                    }
                    Text(
                        text = "Toque numa unidade para removê-la.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                LabeledField(
                    label = "Nova unidade",
                    value = form.newUnit,
                    onValueChange = { value -> model.editForm { it.copy(newUnit = value) } },
                    placeholder = "Asa Sul"
                )
                QuietAction(label = "Adicionar unidade", onClick = model::addUnit)

                val others = state.establishments.filter { it.id != form.id }

                if (others.isNotEmpty()) {
                    Column {
                        Eyebrow("Unir a outro local")
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            others.forEach { other ->
                                Chip(other.name, form.mergeInto == other.id, onClick = {
                                    model.editForm { it.copy(mergeInto = other.id) }
                                })
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (form.mergeInto != null) {
                        QuietAction(label = "Unir agora", onClick = model::merge)
                    }
                    QuietAction(label = "Excluir", onClick = model::delete, destructive = true)
                }
            }
        }
    }
}

private fun subtitle(establishment: EstablishmentDto): String? {
    val units = establishment.places.mapNotNull { it.name }
    val entries = establishment.entries ?: 0

    return listOfNotNull(
        units.takeIf { it.isNotEmpty() }?.joinToString(", "),
        when (entries) {
            0 -> null
            1 -> "1 compra"
            else -> "$entries compras"
        }
    ).joinToString(" · ").ifBlank { null }
}

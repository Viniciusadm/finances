package com.archieapps.finances.ui.screen.tags

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ColorChoice
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted

@Composable
fun TagsScreen(onBack: () -> Unit) {
    val model = appViewModel { container -> TagsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Tags",
            subtitle = "Marcações livres, além da categoria",
            onBack = onBack,
            action = { TextAction("Nova", model::startCreate) }
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            LabeledField(
                label = "Buscar",
                value = state.search,
                onValueChange = model::onSearch,
                placeholder = "viagem, presente…"
            )
        }

        Spacer(Modifier.height(12.dp))
        ErrorBanner(state.error)

        if (state.loading && state.tags.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.tags.isEmpty()) {
            EmptyState(
                headline = if (state.search.isBlank()) "Nenhuma tag" else "Nada com esse nome",
                hint = "Tags marcam um gasto por fora da categoria: uma viagem, um presente, uma reforma.",
                action = { QuietAction("Criar tag", model::startCreate) }
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.tags, key = { it.id }) { tag ->
                LedgerRow(
                    title = tag.name,
                    markerHex = tag.color,
                    onClick = { model.startEdit(tag) },
                    trailing = {
                        val count = tag.usageCount
                        if (count != null) {
                            Text(
                                text = usageLabel(count),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (count == 0) TextFaint else TextMuted
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
            title = if (form.id == null) "Nova tag" else "Editar tag",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "Nome",
                value = form.name,
                onValueChange = { value -> model.editForm { it.copy(name = value) } },
                placeholder = "viagem sp"
            )

            ColorChoice(
                selected = form.color,
                onSelect = { value -> model.editForm { it.copy(color = value) } }
            )

            ErrorBanner(state.error)

            PrimaryAction(
                label = if (form.id == null) "Criar tag" else "Salvar",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )

            if (form.id != null) {
                val others = state.tags.filter { it.id != form.id }

                if (others.isNotEmpty()) {
                    Column {
                        Eyebrow("Unir a outra tag")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Move as marcações para a tag escolhida e apaga esta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
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

private fun usageLabel(count: Int): String = when (count) {
    0 -> "sem uso"
    1 -> "1 uso"
    else -> "$count usos"
}

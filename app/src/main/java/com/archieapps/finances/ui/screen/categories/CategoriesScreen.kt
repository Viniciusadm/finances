package com.archieapps.finances.ui.screen.categories

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ChoiceChips
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
import com.archieapps.finances.ui.theme.TextMuted

private val KindOptions = listOf(
    "expense" to "Gasto",
    "income" to "Ganho",
    "both" to "Ambos"
)

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val model = appViewModel { container -> CategoriesViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Categorias",
            subtitle = "Um nível de subcategoria",
            onBack = onBack,
            action = { TextAction("Nova", model::startCreate) }
        )

        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Todas", state.kindFilter == null, onClick = { model.setKindFilter(null) })
            Chip("Gastos", state.kindFilter == "expense", onClick = { model.setKindFilter("expense") })
            Chip("Ganhos", state.kindFilter == "income", onClick = { model.setKindFilter("income") })
            Chip("Arquivadas", state.includeArchived, onClick = { model.toggleArchived() })
        }

        Spacer(Modifier.height(8.dp))
        ErrorBanner(state.error)

        if (state.loading && state.categories.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.visible.isEmpty()) {
            EmptyState(
                headline = "Nada nesse filtro",
                hint = "Troque o filtro acima ou crie uma categoria nova."
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            state.roots.forEach { root ->
                item(key = "root-${root.id}") {
                    CategoryLine(root, indented = false) { model.startEdit(root) }
                    HairlineDivider(inset = 20)
                }

                state.childrenOf(root.id).forEach { child ->
                    item(key = "child-${child.id}") {
                        CategoryLine(child, indented = true) { model.startEdit(child) }
                        HairlineDivider(inset = 20)
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(
            title = if (form.id == null) "Nova categoria" else "Editar categoria",
            onDismiss = model::dismissForm
        ) {
            LabeledField(
                label = "Nome",
                value = form.name,
                onValueChange = { value -> model.editForm { it.copy(name = value) } },
                placeholder = "Mercado, aluguel, salário…"
            )

            ChoiceChips(
                label = "Serve para",
                options = KindOptions,
                selected = form.kind,
                onSelect = { value -> model.editForm { it.copy(kind = value) } }
            )

            val parents = state.parentOptions.filter { it.id != form.id }

            if (parents.isNotEmpty()) {
                Column {
                    Eyebrow("Dentro de")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("Nenhuma", form.parentId == null, onClick = {
                            model.editForm { it.copy(parentId = null) }
                        })
                        parents.forEach { parent ->
                            Chip(parent.name, form.parentId == parent.id, onClick = {
                                model.editForm { it.copy(parentId = parent.id) }
                            })
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
                label = if (form.id == null) "Criar categoria" else "Salvar",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )

            if (form.id != null && !form.isDefault) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuietAction(
                        label = if (form.active) "Arquivar" else "Reativar",
                        onClick = { model.setActive(!form.active) }
                    )
                    QuietAction(label = "Excluir", onClick = model::delete, destructive = true)
                }
            }

            if (form.isDefault) {
                Text(
                    text = "Esta é a categoria padrão. Ela não pode ser arquivada nem removida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun CategoryLine(category: CategoryDto, indented: Boolean, onClick: () -> Unit) {
    LedgerRow(
        title = category.name,
        subtitle = subtitleFor(category),
        markerHex = category.color,
        dimmed = !category.active,
        onClick = onClick,
        modifier = if (indented) Modifier.padding(start = 22.dp) else Modifier
    )
}

private fun subtitleFor(category: CategoryDto): String? {
    val kind = when (category.kind) {
        "income" -> "Ganho"
        "both" -> "Gasto e ganho"
        else -> null
    }

    if (category.active) {
        return kind
    }

    return listOfNotNull(kind, "arquivada").joinToString(" · ")
}

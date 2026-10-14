package com.archieapps.finances.ui.screen.aientry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ColorDot
import com.archieapps.finances.ui.components.CountField
import com.archieapps.finances.ui.components.DateStepper
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.screen.composer.Target
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val DayFormat = DateTimeFormatter.ofPattern("d 'de' MMM", PtBr)

private val Examples = listOf(
    "Comprei uma TV de 3200 no cartão em 10x",
    "Almoço de 42,90 no pix",
    "Recebi 4500 de salário ontem",
    "Todo dia 10 pago 89,90 da Netflix"
)

private val KindOptions = listOf(
    "expense" to "Gasto",
    "income" to "Ganho",
    "transfer" to "Transferência"
)

private val FrequencyOptions = listOf(
    "daily" to "Todo dia",
    "weekly" to "Toda semana",
    "monthly" to "Todo mês",
    "yearly" to "Todo ano"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiEntrySheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val model = appViewModel(key = "ai-entry") { container -> AiEntryViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.saved) {
        if (!state.saved) return@LaunchedEffect
        model.acknowledgeSaved()
        onSaved()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = TextStrong,
        dragHandle = null
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 18.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.1f)
                    .height(4.dp)
                    .background(Hairline, RoundedCornerShape(2.dp))
            )

            Spacer(Modifier.height(16.dp))

            when (state.phase) {
                AiPhase.Writing -> WritingPhase(state, model)
                AiPhase.Review -> ReviewPhase(state, model, onDismiss)
            }
        }
    }
}

@Composable
private fun ColumnScope.WritingPhase(state: AiEntryState, model: AiEntryViewModel) {
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
        Text(
            text = "Descreva a compra",
            style = MaterialTheme.typography.titleLarge,
            color = TextStrong
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Escreva do seu jeito. Diga o valor, como pagou e se parcelou.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .background(Surface, RoundedCornerShape(14.dp))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            if (state.text.isEmpty()) {
                Text(
                    text = "Comprei uma TV de 3200 no cartão Nubank em 10x",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextFaint
                )
            }
            BasicTextField(
                value = state.text,
                onValueChange = model::onText,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge.copy(color = TextStrong)
                ),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        Eyebrow("Exemplos")
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Examples.forEach { example ->
                Chip(text = example, active = false, onClick = { model.onText(example) })
            }
        }

        Spacer(Modifier.height(16.dp))
        ErrorBanner(state.error)
    }

    Spacer(Modifier.height(12.dp))

    PrimaryAction(
        label = "Interpretar",
        onClick = model::parse,
        enabled = state.canParse,
        loading = state.parsing
    )
}

@Composable
private fun ColumnScope.ReviewPhase(state: AiEntryState, model: AiEntryViewModel, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Confira antes de salvar",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextStrong
                )
                Spacer(Modifier.height(4.dp))
                Eyebrow(state.headline())
            }
            QuietAction(label = "Reescrever", onClick = model::backToWriting)
        }

        Spacer(Modifier.height(16.dp))

        WarningList(state.warnings)
        ErrorBanner(state.error)

        Spacer(Modifier.height(4.dp))

        MoneyInput(
            label = if (state.isInstallment && state.amountIs == "installment") "Valor da parcela" else "Valor",
            reais = state.reais,
            centavos = state.centavos,
            onReaisChange = model::onReais,
            onCentavosChange = model::onCentavos
        )

        if (!state.isInstallment) {
            Field("Tipo") {
                ChipRow {
                    KindOptions.forEach { (value, label) ->
                        Chip(text = label, active = state.kind == value, onClick = { model.onKind(value) })
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        LabeledField(
            label = "Descrição",
            value = state.description,
            onValueChange = model::onDescription,
            placeholder = "Sem descrição"
        )

        Field(if (state.isRecurrence) "Primeira cobrança" else "Data") {
            if (state.isRecurrence) {
                ChipRow {
                    Chip(text = state.startsOn.format(DayFormat), active = true, onClick = {})
                    Chip(text = "‹", active = false, onClick = { model.onStartsOn(state.startsOn.minusDays(1)) })
                    Chip(text = "›", active = false, onClick = { model.onStartsOn(state.startsOn.plusDays(1)) })
                }
            } else {
                DateChips(state.date, model::onDate)
            }
        }

        if (!state.isRecurrence) {
            Field("Situação") {
                ChipRow {
                    Chip(text = "Já pago", active = state.paid, onClick = { model.onPaid(true) })
                    Chip(text = "Só previsto", active = !state.paid, onClick = { model.onPaid(false) })
                }
            }
        }

        if (state.isTransfer) {
            Field("De") {
                ChipRow {
                    state.accounts.forEach { account ->
                        Chip(
                            text = account.name,
                            active = state.accountId == account.id,
                            onClick = { model.onAccount(account.id) },
                            leading = { ColorDot(account.color) }
                        )
                    }
                }
            }
            Field("Para") {
                ChipRow {
                    state.accounts.filter { it.id != state.accountId }.forEach { account ->
                        Chip(
                            text = account.name,
                            active = state.counterAccountId == account.id,
                            onClick = { model.onCounterAccount(account.id) },
                            leading = { ColorDot(account.color) }
                        )
                    }
                }
            }
        } else {
            Field("Conta ou cartão") {
                ChipRow {
                    state.accounts.forEach { account ->
                        Chip(
                            text = account.name,
                            active = state.target == Target.Account && state.accountId == account.id,
                            onClick = { model.onAccount(account.id) },
                            leading = { ColorDot(account.color) }
                        )
                    }
                    state.cards.forEach { card ->
                        Chip(
                            text = card.name,
                            active = state.target == Target.Card && state.cardId == card.id,
                            onClick = { model.onCard(card.id) },
                            leading = { ColorDot(card.color) }
                        )
                    }
                }
            }

            Field("Categoria") {
                val suggestion = state.newCategory

                if (suggestion != null && state.categoryId == null) {
                    Chip(
                        text = "${suggestion.name} · nova",
                        active = state.createCategory,
                        onClick = model::toggleCreateCategory
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.createCategory) {
                            "Vou criar essa categoria ao salvar. Toque para não criar."
                        } else {
                            "Não vou criar. Escolha uma categoria existente abaixo."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextFaint
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ChipRow {
                    state.visibleCategories.forEach { category ->
                        Chip(
                            text = category.name,
                            active = state.categoryId == category.id,
                            onClick = { model.onCategory(category.id) },
                            leading = { ColorDot(category.color) }
                        )
                    }
                }
            }
        }

        if (state.isInstallment) {
            Spacer(Modifier.height(18.dp))
            CountField(
                label = "Parcelas",
                value = state.installments,
                onValueChange = model::onInstallments,
                placeholder = "10"
            )
            Field("O valor acima é") {
                ChipRow {
                    Chip(text = "o total", active = state.amountIs == "total", onClick = { model.onAmountIs("total") })
                    Chip(text = "de cada parcela", active = state.amountIs == "installment", onClick = { model.onAmountIs("installment") })
                }
            }
            Spacer(Modifier.height(18.dp))
            DateStepper(
                label = "Primeira parcela",
                value = state.firstDueDate,
                onChange = model::onFirstDueDate
            )
        }

        if (state.isRecurrence) {
            Field("Se repete") {
                ChipRow {
                    FrequencyOptions.forEach { (value, label) ->
                        Chip(text = label, active = state.frequency == value, onClick = { model.onFrequency(value) })
                    }
                }
            }
        }

        val personId = state.personId

        if (personId != null) {
            val person = state.people.firstOrNull { it.id == personId }

            Field("Pessoa") {
                ChipRow {
                    Chip(
                        text = person?.name ?: "Pessoa #$personId",
                        active = true,
                        onClick = { model.onPerson(personId) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                ChipRow {
                    Chip(text = "Emprestei", active = state.personRole == "lent", onClick = { model.onPersonRole("lent") })
                    Chip(text = "Peguei", active = state.personRole == "borrowed", onClick = { model.onPersonRole("borrowed") })
                    Chip(text = "Acerto", active = state.personRole == "settle", onClick = { model.onPersonRole("settle") })
                }
            }
        }

        if (state.tags.isNotEmpty() && !state.isRecurrence) {
            Field("Tags") {
                ChipRow {
                    state.tags.forEach { tag ->
                        Chip(
                            text = if (tag.isNew) "${tag.name} · nova" else tag.name,
                            active = tag.selected,
                            onClick = { model.toggleTag(tag.name) }
                        )
                    }
                }
            }
        }

        if (state.items.isNotEmpty() && state.type == "entry") {
            Field("Itens") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.items.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextStrong,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = quantityLabel(item.quantity) +
                                        (if (item.productId != null) " · no catálogo" else ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextFaint
                                )
                            }
                            MoneyText(cents = item.totalCents, size = MoneySize.Row, color = TextMuted)
                            Text(
                                text = "  ✕",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextFaint,
                                modifier = Modifier.clickable { model.removeItem(index) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        LabeledField(
            label = "Observação",
            value = state.notes,
            onValueChange = model::onNotes,
            placeholder = "Opcional"
        )

        Spacer(Modifier.height(20.dp))

        QuietAction(label = "Descartar", onClick = onDismiss, destructive = true)

        Spacer(Modifier.height(12.dp))
    }

    Spacer(Modifier.height(12.dp))

    PrimaryAction(
        label = "Salvar",
        onClick = model::save,
        enabled = state.canSave,
        loading = state.saving
    )
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Eyebrow(label)
    Spacer(Modifier.height(10.dp))
    content()
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun DateChips(date: LocalDate, onChange: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    ChipRow {
        Chip(text = "Hoje", active = date == today, onClick = { onChange(today) })
        Chip(text = "Ontem", active = date == yesterday, onClick = { onChange(yesterday) })

        if (date != today && date != yesterday) {
            Chip(text = date.format(DayFormat), active = true, onClick = { onChange(today) })
        }

        Chip(text = "‹", active = false, onClick = { onChange(date.minusDays(1)) })
        Chip(text = "›", active = false, onClick = { onChange(date.plusDays(1)) })
    }
}

@Composable
private fun WarningList(warnings: List<String>) {
    if (warnings.isEmpty()) return

    Column(
        Modifier
            .fillMaxWidth()
            .background(Warning.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        warnings.forEach { warning ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = Warning)
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextStrong
                )
            }
        }
    }
}

private fun quantityLabel(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) {
        "${quantity.toLong()} un"
    } else {
        String.format(PtBr, "%.3f un", quantity)
    }

private fun AiEntryState.headline(): String = when {
    isInstallment -> "Parcelamento em ${installmentsTotal}x"
    isRecurrence -> FrequencyOptions.firstOrNull { it.first == frequency }?.second ?: "Recorrência"
    isTransfer -> "Transferência"
    kind == "income" -> "Ganho"
    else -> "Gasto"
}

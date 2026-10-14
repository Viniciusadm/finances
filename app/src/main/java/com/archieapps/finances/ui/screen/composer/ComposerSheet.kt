package com.archieapps.finances.ui.screen.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.ui.components.AmountKeypad
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ColorDot
import com.archieapps.finances.ui.components.ConfirmChoice
import com.archieapps.finances.ui.components.ConfirmDialog
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.TabularFigures
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val KindOptions = listOf(
    "expense" to "Gasto",
    "income" to "Ganho",
    "transfer" to "Transferência"
)

private val DayFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerSheet(
    editingId: Int? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onOpenItems: (Int) -> Unit = {}
) {
    val model = appViewModel(key = "composer-${editingId ?: "new"}") { container ->
        ComposerViewModel(container.repository, container.sessionStore, editingId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (!state.done) return@LaunchedEffect
        model.acknowledgeSaved()
        onSaved()
    }

    if (confirmingDelete) {
        DeleteDialog(
            state = state,
            onDismiss = { confirmingDelete = false },
            onPick = model::delete
        )
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

            Spacer(Modifier.height(14.dp))

            if (state.editingId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = editingLabel(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextFaint,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Excluir",
                        style = MaterialTheme.typography.labelLarge,
                        color = Negative,
                        modifier = Modifier
                            .clickable { confirmingDelete = true }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            if (state.expanded) {
                CompactSummary(state)
                Spacer(Modifier.height(12.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindOptions.forEach { (value, label) ->
                        Chip(
                            text = label,
                            active = state.kind == value,
                            onClick = { model.onKind(value) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                TypedAmount(state)

                Spacer(Modifier.height(12.dp))
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

            if (!state.expanded) {
                InlineField(
                    value = state.description,
                    onValueChange = model::onDescription,
                    placeholder = descriptionHint(state.kind)
                )

                Spacer(Modifier.height(14.dp))

                if (state.isTransfer) {
                    TransferPickers(state, model)
                } else {
                    SourcePickers(state, model)
                    Spacer(Modifier.height(8.dp))
                    CategoryPickers(state, model)
                }

                Spacer(Modifier.height(8.dp))
                DatePickers(state, model)
            }

            Spacer(Modifier.height(12.dp))


            if (state.expanded) {

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        text = if (state.paid) "Já pago" else "Só previsto",
                        active = !state.paid,
                        onClick = { model.onPaid(!state.paid) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Eyebrow("Tags")
                Spacer(Modifier.height(6.dp))
                InlineField(
                    value = state.tags,
                    onValueChange = model::onTags,
                    placeholder = "viagem, presente"
                )

                if (state.people.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Eyebrow("Com quem")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.people.forEach { person ->
                            Chip(
                                text = person.nickname?.takeIf { it.isNotBlank() } ?: person.name,
                                active = state.personId == person.id,
                                onClick = { model.onPerson(person.id) }
                            )
                        }
                    }

                    if (state.personId != null) {
                        Spacer(Modifier.height(10.dp))
                        Eyebrow("Isso é")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(
                                text = "Só marcação",
                                active = state.personRole == null,
                                onClick = { model.onPersonRole(state.personRole) }
                            )
                            Chip(
                                text = "Emprestei",
                                active = state.personRole == "lent",
                                onClick = { model.onPersonRole("lent") }
                            )
                            Chip(
                                text = "Peguei",
                                active = state.personRole == "borrowed",
                                onClick = { model.onPersonRole("borrowed") }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = roleHint(state.personRole),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Eyebrow("Observação")
                Spacer(Modifier.height(6.dp))
                InlineField(
                    value = state.notes,
                    onValueChange = model::onNotes,
                    placeholder = "Detalhe que você vai querer lembrar"
                )
            }

            }

            ErrorBanner(state.error, Modifier.padding(top = 12.dp))

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    text = if (state.expanded) "Menos detalhes" else "Mais detalhes",
                    active = state.expanded,
                    onClick = model::toggleExpanded
                )

                val editing = state.editingId

                if (editing != null) {
                    Chip(
                        text = "Itens",
                        active = false,
                        onClick = { onOpenItems(editing) }
                    )
                }

                if (state.isInstallment) {
                    Chip(
                        text = "Todas as parcelas",
                        active = state.applyToPlan,
                        onClick = model::toggleApplyToPlan
                    )
                }

                if (!state.paid) {
                    Text(
                        text = "Só previsto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.expanded) {
                PrimaryAction(
                    label = if (state.editingId == null) "Salvar" else "Salvar alterações",
                    onClick = model::save,
                    enabled = state.canSave,
                    loading = state.saving
                )
            } else {
                AmountKeypad(
                    onDigit = model::onDigit,
                    onCents = model::onCents,
                    onBackspace = model::onBackspace,
                    onConfirm = model::save,
                    confirmLabel = if (state.editingId == null) "Salvar" else "Salvar alterações",
                    confirmEnabled = state.canSave,
                    confirming = state.saving,
                    centsActive = state.typingCents
                )
            }
        }
    }
}

@Composable
private fun CompactSummary(state: ComposerState) {
    val kind = KindOptions.firstOrNull { it.first == state.kind }?.second ?: "Gasto"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = kind.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TextFaint
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "R$ " + Money.full(state.amountCents),
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 20.sp,
                fontWeight = FontWeight.W700
            ),
            color = TextStrong,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = state.description.trim().ifEmpty { "sem descrição" },
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun TypedAmount(state: ComposerState) {
    val integer = Money.groupedDigits(state.integerDigits.ifEmpty { "0" })
    val cents = state.centsDigits.padEnd(2, '0')
    val idle = !state.hasAmount

    Row {
        Text(
            text = "R$",
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500
            ),
            color = TextFaint,
            modifier = Modifier.alignByBaseline()
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = integer,
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 40.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.5).sp
            ),
            color = if (idle) TextFaint else TextStrong,
            modifier = Modifier.alignByBaseline()
        )
        Text(
            text = ",$cents",
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 19.sp,
                fontWeight = FontWeight.W700
            ),
            color = when {
                state.typingCents -> Primary
                idle -> TextFaint
                else -> TextStrong.copy(alpha = 0.72f)
            },
            modifier = Modifier.alignByBaseline()
        )
    }
}

@Composable
private fun SourcePickers(state: ComposerState, model: ComposerViewModel) {
    val labelled = state.accounts.isNotEmpty() && state.cards.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.accounts.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (labelled) {
                    GroupLabel("Saiu da conta")
                }

                state.accounts.forEach { account ->
                    Chip(
                        text = account.name,
                        active = state.target == Target.Account && state.accountId == account.id,
                        onClick = {
                            model.onTarget(Target.Account)
                            model.onAccount(account.id)
                        },
                        leading = { ColorDot(account.color) }
                    )
                }

                if (state.showsPaymentMethod) {
                    Box(
                        Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(Hairline)
                    )

                    PaymentMethods.forEach { (value, label) ->
                        Chip(
                            text = label,
                            active = state.paymentMethod == value,
                            onClick = { model.onPaymentMethod(value) }
                        )
                    }
                }
            }
        }

        if (state.cards.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (labelled) {
                    GroupLabel("No cartão")
                }

                state.cards.forEach { card ->
                    Chip(
                        text = card.name,
                        active = state.target == Target.Card && state.cardId == card.id,
                        onClick = {
                            model.onTarget(Target.Card)
                            model.onCard(card.id)
                        },
                        leading = { ColorDot(card.color) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextFaint
    )
}

@Composable
private fun CategoryPickers(state: ComposerState, model: ComposerViewModel) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.visibleCategories.forEach { category ->
            Chip(
                text = category.name,
                active = state.categoryId == category.id,
                onClick = { model.onCategory(if (state.categoryId == category.id) null else category.id) },
                leading = { ColorDot(category.color) }
            )
        }
    }
}

@Composable
private fun TransferPickers(state: ComposerState, model: ComposerViewModel) {
    Column {
        Eyebrow("De")
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.accounts.forEach { account ->
                Chip(
                    text = account.name,
                    active = state.accountId == account.id,
                    onClick = { model.onAccount(account.id) },
                    leading = { ColorDot(account.color) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Eyebrow("Para")
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
}

@Composable
private fun DatePickers(state: ComposerState, model: ComposerViewModel) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Chip(text = "Hoje", active = state.date == today, onClick = { model.onDate(today) })
        Chip(text = "Ontem", active = state.date == yesterday, onClick = { model.onDate(yesterday) })

        if (state.date != today && state.date != yesterday) {
            Chip(text = state.date.format(DayFormat), active = true, onClick = { model.onDate(today) })
        }

        Chip(text = "‹", active = false, onClick = { model.onDate(state.date.minusDays(1)) })
        Chip(text = "›", active = false, onClick = { model.onDate(state.date.plusDays(1)) })
    }
}

@Composable
private fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = TextFaint
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextStrong),
            cursorBrush = SolidColor(Primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun roleHint(role: String?): String = when (role) {
    "lent" -> "Vai entrar como dívida dessa pessoa com você."
    "borrowed" -> "Vai entrar como dívida sua com essa pessoa."
    else -> "Só registra quem participou, sem mexer em saldo."
}

private fun descriptionHint(kind: String): String = when (kind) {
    "income" -> "De onde veio?"
    "transfer" -> "Por quê?"
    else -> "No que foi?"
}

private fun editingLabel(state: ComposerState): String = when {
    state.isInstallment -> "Parcela ${state.installmentNumber} de ${state.installmentsTotal ?: "?"}"
    state.isOccurrence -> "Lançamento de uma recorrência"
    else -> "Editando lançamento"
}

@Composable
private fun DeleteDialog(
    state: ComposerState,
    onDismiss: () -> Unit,
    onPick: (DeleteScope) -> Unit
) {
    val choices = when {
        state.isInstallment -> listOf(
            ConfirmChoice(
                label = "Excluir só esta parcela",
                hint = "As outras parcelas continuam",
                onPick = { onPick(DeleteScope.Entry) }
            ),
            ConfirmChoice(
                label = "Excluir as ${state.installmentsTotal ?: 0} parcelas",
                hint = "Apaga o parcelamento inteiro, inclusive as já pagas",
                onPick = { onPick(DeleteScope.Plan) }
            )
        )
        state.isOccurrence -> listOf(
            ConfirmChoice(
                label = "Excluir só este lançamento",
                hint = "A recorrência continua gerando os próximos",
                onPick = { onPick(DeleteScope.Entry) }
            ),
            ConfirmChoice(
                label = "Excluir a recorrência inteira",
                hint = "Apaga a regra e todos os lançamentos que ela gerou",
                onPick = { onPick(DeleteScope.Recurrence) }
            )
        )
        else -> listOf(
            ConfirmChoice(
                label = "Excluir",
                onPick = { onPick(DeleteScope.Entry) }
            )
        )
    }

    ConfirmDialog(
        title = when {
            state.isInstallment -> "Excluir a parcela ${state.installmentNumber}?"
            else -> "Excluir lançamento?"
        },
        message = state.description.trim().takeIf { it.isNotEmpty() },
        choices = choices,
        onDismiss = onDismiss
    )
}

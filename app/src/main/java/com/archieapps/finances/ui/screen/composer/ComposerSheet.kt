package com.archieapps.finances.ui.screen.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.archieapps.finances.ui.components.DateChips
import com.archieapps.finances.ui.components.IconAction
import com.archieapps.finances.ui.components.IconBadge
import com.archieapps.finances.ui.components.SegmentedControl
import com.archieapps.finances.ui.components.SheetHandle
import com.archieapps.finances.ui.components.SheetShape
import com.archieapps.finances.ui.components.SheetTitle
import com.archieapps.finances.ui.theme.Accent
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Ink
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.SurfaceLift
import com.archieapps.finances.ui.theme.Warning
import com.archieapps.finances.ui.theme.soft
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
    "transfer" to "Transferência",
    "yield" to "Rendimento"
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
        shape = SheetShape,
        containerColor = Surface,
        contentColor = TextStrong,
        scrimColor = Ink.copy(alpha = 0.7f),
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
            SheetHandle(Modifier.align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(12.dp))

            SheetTitle(
                title = if (state.editingId != null) editingLabel(state) else "Novo lançamento",
                onClose = onDismiss,
                action = if (state.editingId != null) {
                    {
                        IconAction(
                            icon = AppIcons.Delete,
                            onClick = { confirmingDelete = true },
                            contentDescription = "Excluir",
                            tint = Negative,
                            container = Negative.soft(0.14f),
                            size = 36
                        )
                    }
                } else null
            )

            Spacer(Modifier.height(14.dp))

            if (state.expanded) {
                CompactSummary(state)
                Spacer(Modifier.height(12.dp))
            } else {
                SegmentedControl(
                    options = KindOptions.map { (value, label) -> value to (if (value == "transfer") "Transf." else label) },
                    selected = state.kind,
                    onSelect = model::onKind,
                    colorFor = ::kindColor
                )

                Spacer(Modifier.height(18.dp))

                TypedAmount(state)

                Spacer(Modifier.height(16.dp))
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
                    placeholder = descriptionHint(state.kind),
                    icon = AppIcons.Edit
                )

                Spacer(Modifier.height(14.dp))

                if (state.isTransfer) {
                    TransferPickers(state, model)
                } else {
                    SourcePickers(state, model)
                    Spacer(Modifier.height(8.dp))
                    CategoryPickers(state, model)
                }

                Spacer(Modifier.height(12.dp))
                DateChips(date = state.date, onChange = model::onDate)
            }

            Spacer(Modifier.height(12.dp))


            if (state.expanded) {

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        text = "Já pago",
                        active = state.paid,
                        icon = AppIcons.Check,
                        activeColor = Positive,
                        onClick = { if (!state.paid) model.onPaid(true) }
                    )
                    Chip(
                        text = "Só previsto",
                        active = !state.paid,
                        icon = AppIcons.Schedule,
                        activeColor = Warning,
                        onClick = { if (state.paid) model.onPaid(false) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Eyebrow("Tags")
                Spacer(Modifier.height(6.dp))
                InlineField(
                    value = state.tags,
                    onValueChange = model::onTags,
                    placeholder = "viagem, presente",
                    icon = AppIcons.Tag
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
                    placeholder = "Detalhe que você vai querer lembrar",
                    icon = AppIcons.Notes
                )
            }

            }

            ErrorBanner(state.error, Modifier.padding(top = 12.dp))

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    text = if (state.expanded) "Voltar ao valor" else "Mais detalhes",
                    active = state.expanded,
                    icon = if (state.expanded) AppIcons.ExpandMore else AppIcons.ExpandLess,
                    onClick = model::toggleExpanded
                )

                val editing = state.editingId

                if (editing != null) {
                    Chip(
                        text = "Itens",
                        active = false,
                        icon = AppIcons.Cart,
                        onClick = { onOpenItems(editing) }
                    )
                }

                if (state.isInstallment) {
                    Chip(
                        text = "Todas as parcelas",
                        active = state.applyToPlan,
                        icon = AppIcons.Layers,
                        onClick = model::toggleApplyToPlan
                    )
                }

                if (!state.paid) {
                    Chip(
                        text = "Só previsto",
                        active = true,
                        icon = AppIcons.Schedule,
                        activeColor = Warning,
                        onClick = { model.onPaid(true) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.expanded) {
                PrimaryAction(
                    icon = AppIcons.Check,
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
    val color = kindColor(state.kind)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLift)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = kindIcon(state.kind), color = color, size = 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.description.trim().ifEmpty { kind },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = TextStrong,
                maxLines = 1
            )
            Text(text = kind, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Text(
            text = "R$ " + Money.full(state.amountCents),
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 20.sp,
                fontWeight = FontWeight.W800
            ),
            color = color
        )
    }
}

@Composable
private fun TypedAmount(state: ComposerState) {
    val integer = Money.groupedDigits(state.integerDigits.ifEmpty { "0" })
    val cents = state.centsDigits.padEnd(2, '0')
    val idle = !state.hasAmount
    val color = kindColor(state.kind)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            text = "R$",
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600
            ),
            color = if (idle) TextFaint else color.copy(alpha = 0.7f),
            modifier = Modifier.alignByBaseline()
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = integer,
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 48.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = (-1).sp
            ),
            color = if (idle) TextFaint else color,
            modifier = Modifier.alignByBaseline()
        )
        Text(
            text = ",$cents",
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = 24.sp,
                fontWeight = FontWeight.W700
            ),
            color = when {
                state.typingCents -> Primary
                idle -> TextFaint
                else -> color.copy(alpha = 0.75f)
            },
            modifier = Modifier
                .alignByBaseline()
                .then(
                    if (state.typingCents) Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.soft(0.16f))
                        .padding(horizontal = 4.dp)
                    else Modifier
                )
        )
    }
}

private fun kindColor(kind: String?): Color = when (kind) {
    "income" -> Positive
    "transfer" -> Primary
    "yield" -> Accent
    else -> Negative
}

private fun kindIcon(kind: String?): ImageVector = when (kind) {
    "income" -> AppIcons.ArrowDown
    "transfer" -> AppIcons.Swap
    "yield" -> AppIcons.TrendingUp
    else -> AppIcons.ArrowUp
}

@Composable
private fun SourcePickers(state: ComposerState, model: ComposerViewModel) {
    val showsCards = state.cards.isNotEmpty() && !state.isYield
    val labelled = state.accounts.isNotEmpty() && showsCards

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.accounts.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (labelled || state.isYield) {
                    GroupLabel(if (state.kind == "expense") "Saiu da conta" else "Entrou na conta")
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
                            .height(24.dp)
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

        if (showsCards) {
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
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted
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
private fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLift)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Box(Modifier.weight(1f)) {
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
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
    "yield" -> "Rendimento (opcional)"
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

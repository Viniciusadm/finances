package com.archieapps.finances.ui.screen.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.archieapps.finances.ui.components.ChoiceChips
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

import androidx.activity.compose.BackHandler
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.paymentAccounts

private val KindOptions = listOf(
    "expense" to "Gasto",
    "income" to "Ganho",
    "transfer" to "Transferência",
    "yield" to "Rendimento"
)

private val KindTabItems = listOf(
    Triple("expense", "Gasto", AppIcons.ArrowUp),
    Triple("income", "Ganho", AppIcons.ArrowDown),
    Triple("transfer", "Transferir", AppIcons.Swap),
    Triple("yield", "Rendimento", AppIcons.TrendingUp)
)

/** Tipo do lançamento: quatro blocos do mesmo tamanho, com ícone e nome curto. */
@Composable
private fun KindTabs(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KindTabItems.forEach { (kind, label, icon) ->
            val active = kind == selected
            val color = kindColor(kind)

            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) color.copy(alpha = 0.18f) else SurfaceLift)
                    .border(1.5.dp, if (active) color else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { onSelect(kind) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) color else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (active) FontWeight.W700 else FontWeight.W600),
                    color = if (active) color else TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private val PayWith = listOf("account" to "Conta corrente", "card" to "Cartão")

/** Lançamento manual: uma tela rolável, na ordem natural, com o botão Salvar fixo no fim. */
@Composable
fun EntryFormScreen(
    editingId: Int? = null,
    copyFromId: Int? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenItems: (Int) -> Unit = {},
    onOpenAi: () -> Unit = {}
) {
    val model = appViewModel(key = "composer-${editingId ?: "new"}-copy${copyFromId ?: 0}") { container ->
        ComposerViewModel(container.repository, container.sessionStore, container.reference, editingId, copyFromId)
    }
    val state by model.state.collectAsStateWithLifecycle()

    var confirmingDelete by remember { mutableStateOf(false) }
    var confirmingDiscard by remember { mutableStateOf(false) }

    val dirty = editingId == null && copyFromId == null && (state.hasAmount || state.description.isNotBlank() || state.notes.isNotBlank())
    val leave: () -> Unit = { if (dirty) confirmingDiscard = true else onBack() }

    BackHandler(onBack = leave)

    LaunchedEffect(state.done) {
        if (!state.done) return@LaunchedEffect

        val itemsFor = if (state.addItemsAfter) state.createdId else null

        model.acknowledgeSaved()

        if (itemsFor != null) onOpenItems(itemsFor) else onSaved()
    }

    if (confirmingDelete) {
        DeleteDialog(state = state, onDismiss = { confirmingDelete = false }, onPick = model::delete)
    }

    if (confirmingDiscard) {
        ConfirmDialog(
            title = "Descartar este lançamento?",
            message = "O que você preencheu será perdido.",
            choices = listOf(ConfirmChoice(label = "Descartar") { confirmingDiscard = false; onBack() }),
            cancelLabel = "Continuar editando",
            onDismiss = { confirmingDiscard = false }
        )
    }

    if (state.preparing) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = if (editingId == null) (if (copyFromId != null) "Duplicar lançamento" else "Novo lançamento") else "Editar lançamento",
                onBack = onBack
            )
            com.archieapps.finances.ui.components.LoadingRow()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (editingId == null) (if (copyFromId != null) "Duplicar lançamento" else "Novo lançamento") else editingLabel(state),
            onBack = leave,
            action = {
                if (editingId != null) {
                    IconAction(AppIcons.Delete, onClick = { confirmingDelete = true }, contentDescription = "Excluir", tint = Negative)
                } else {
                    Chip(text = "Por texto", active = false, icon = AppIcons.Sparkle, onClick = onOpenAi)
                }
            }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            KindTabs(selected = state.kind, onSelect = model::onKind)

            MoneyInput(
                label = "Valor",
                reais = state.integerDigits,
                centavos = state.centsDigits,
                onReaisChange = model::onReais,
                onCentavosChange = model::onCentavos
            )

            Column {
                Eyebrow("O que foi")
                Spacer(Modifier.height(8.dp))
                InlineField(
                    value = state.description,
                    onValueChange = model::onDescription,
                    placeholder = descriptionHint(state.kind),
                    icon = AppIcons.Edit
                )
            }

            if (!state.isTransfer && !state.isYield) {
                Column { PlaceFields(state, model) }

                if (state.visibleCategories.isNotEmpty()) {
                    Column {
                        Eyebrow("Categoria")
                        Spacer(Modifier.height(8.dp))
                        CategoryPickers(state, model)
                    }
                }

                Column {
                    Eyebrow(if (state.kind == "income") "Entrou em" else "Pagou com")
                    Spacer(Modifier.height(8.dp))
                    PaymentPickers(state, model)
                }
            } else if (state.isTransfer) {
                TransferPickers(state, model)
            } else {
                Column {
                    Eyebrow("Conta")
                    Spacer(Modifier.height(8.dp))
                    YieldPickers(state, model)
                }
            }

            Column {
                Eyebrow("Data")
                Spacer(Modifier.height(8.dp))
                DateChips(date = state.date, onChange = model::onDate)

                if (state.target == Target.Card && !state.isTransfer && !state.isYield) {
                    Spacer(Modifier.height(12.dp))
                    StatementShiftPicker(state, model)
                }
            }

            if (!state.isYield) {
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
            }

            if (state.isInstallment) {
                Chip(
                    text = "Valer para todas as parcelas",
                    active = state.applyToPlan,
                    icon = AppIcons.Layers,
                    onClick = model::toggleApplyToPlan
                )
            }

            Chip(
                text = if (state.expanded) "Menos detalhes" else "Mais detalhes",
                active = state.expanded,
                icon = if (state.expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                onClick = model::toggleExpanded
            )

            if (state.expanded) {
                Column {
                    Eyebrow("Tags")
                    Spacer(Modifier.height(8.dp))
                    InlineField(
                        value = state.tags,
                        onValueChange = model::onTags,
                        placeholder = "viagem, presente",
                        icon = AppIcons.Tag
                    )
                }

                if (state.people.isNotEmpty()) {
                    Column {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(text = "Só marcação", active = state.personRole == null, onClick = { model.onPersonRole(state.personRole) })
                                Chip(text = "Emprestei", active = state.personRole == "lent", onClick = { model.onPersonRole("lent") })
                                Chip(text = "Peguei", active = state.personRole == "borrowed", onClick = { model.onPersonRole("borrowed") })
                            }
                        }
                    }
                }

                Column {
                    Eyebrow("Observação")
                    Spacer(Modifier.height(8.dp))
                    InlineField(
                        value = state.notes,
                        onValueChange = model::onNotes,
                        placeholder = "Contexto que você vai querer lembrar",
                        icon = AppIcons.Notes
                    )
                }

                if (!state.isTransfer && !state.isYield) {
                    val editing = state.editingId

                    Chip(
                        text = "Itens",
                        active = editing == null && state.addItemsAfter,
                        icon = AppIcons.Cart,
                        onClick = { if (editing != null) onOpenItems(editing) else model.toggleItemsAfter() }
                    )
                }
            }

            ErrorBanner(state.error)
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryAction(
                label = "Salvar",
                onClick = model::save,
                enabled = state.canSave,
                loading = state.saving
            )
        }
    }
}

/** Gasto e ganho: conta corrente ou cartão, e nada mais. */
@Composable
private fun PaymentPickers(state: ComposerState, model: ComposerViewModel) {
    val accounts = paymentAccounts(state.accounts)

    SegmentedControl(
        options = PayWith,
        selected = if (state.target == Target.Card) "card" else "account",
        onSelect = { value -> model.onTarget(if (value == "card") Target.Card else Target.Account) }
    )

    Spacer(Modifier.height(10.dp))

    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.target == Target.Card) {
            state.cards.forEach { card ->
                Chip(
                    text = card.name,
                    active = state.cardId == card.id,
                    onClick = { model.onCard(card.id) },
                    leading = { ColorDot(card.color) }
                )
            }
        } else {
            accounts.forEach { account ->
                Chip(
                    text = account.name,
                    active = state.accountId == account.id,
                    onClick = { model.onAccount(account.id) },
                    leading = { ColorDot(account.color) }
                )
            }
        }
    }

    if (state.target == Target.Account && accounts.isEmpty()) {
        Text(
            text = "Nenhuma conta corrente cadastrada.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

/** Rendimento: contas de investimento e poupança. */
@Composable
private fun YieldPickers(state: ComposerState, model: ComposerViewModel) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.accounts.filter { it.kind == "investment" || it.kind == "savings" }.forEach { account ->
            Chip(
                text = account.name,
                active = state.accountId == account.id,
                onClick = { model.onAccount(account.id) },
                leading = { ColorDot(account.color) }
            )
        }
    }
}

private fun kindColor(kind: String?): Color = when (kind) {
    "income" -> Positive
    "transfer" -> Accent
    "yield" -> Warning
    else -> Primary
}

private fun descriptionHint(kind: String): String = when (kind) {
    "income" -> "De onde veio?"
    "transfer" -> "Por quê?"
    "yield" -> "Rendimento (opcional)"
    else -> "O que foi? (curto)"
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

@Composable
private fun StatementShiftPicker(state: ComposerState, model: ComposerViewModel) {
    val card = state.cards.firstOrNull { it.id == state.cardId } ?: return
    val options = buildList {
        add("0" to "Fatura normal")
        add("1" to "Próxima fatura")
        if (state.statementShift < 0) add("-1" to "Fatura anterior")
    }

    ChoiceChips(
        label = "Fatura",
        options = options,
        selected = state.statementShift.toString(),
        onSelect = { value -> model.onStatementShift(value.toInt()) }
    )

    Spacer(Modifier.height(6.dp))
    Text(
        text = statementFor(card.closingDay, card.dueDay, state.date, state.statementShift).label(),
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted
    )
}

/** Local do lançamento: estabelecimento e, se quiser, a unidade, com sugestões do que já existe. */
@Composable
private fun PlaceFields(state: ComposerState, model: ComposerViewModel) {
    val query = state.placeName.trim()
    val matches = if (query.isBlank()) {
        emptyList()
    } else {
        state.establishments
            .filter { it.name.contains(query, ignoreCase = true) && !it.name.equals(query, ignoreCase = true) }
            .take(6)
    }
    val chosen = state.establishments.firstOrNull { it.name.equals(query, ignoreCase = true) }
    val units = chosen?.places?.mapNotNull { it.name }.orEmpty()
        .filter { !it.equals(state.placeUnit.trim(), ignoreCase = true) }

    Eyebrow("Local")
    Spacer(Modifier.height(6.dp))
    InlineField(
        value = state.placeName,
        onValueChange = model::onPlaceName,
        placeholder = "Burger King",
        icon = AppIcons.Place
    )

    if (matches.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            matches.forEach { establishment ->
                Chip(text = establishment.name, active = false, onClick = { model.onPlaceName(establishment.name) })
            }
        }
    }

    if (query.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        InlineField(
            value = state.placeUnit,
            onValueChange = model::onPlaceUnit,
            placeholder = "Unidade (opcional)",
            icon = AppIcons.Place
        )

        if (units.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                units.forEach { unit ->
                    Chip(text = unit, active = false, onClick = { model.onPlaceUnit(unit) })
                }
            }
        }
    }
}

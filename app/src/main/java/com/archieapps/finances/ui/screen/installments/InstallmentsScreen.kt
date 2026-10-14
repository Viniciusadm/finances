package com.archieapps.finances.ui.screen.installments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.InstallmentPlanDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.ConfirmChoice
import com.archieapps.finances.ui.components.ConfirmDialog
import com.archieapps.finances.ui.components.ColorDot
import com.archieapps.finances.ui.components.CountField
import com.archieapps.finances.ui.components.DateStepper
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneyInput
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortDate = DateTimeFormatter.ofPattern("dd/MM/yy", PtBr)

@Composable
fun InstallmentsScreen(onBack: () -> Unit, onOpenEntry: (Int) -> Unit = {}) {
    val model = appViewModel { container -> InstallmentsViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Parcelamentos",
            subtitle = "As parcelas viram lançamentos",
            onBack = onBack,
            action = { TextAction("Novo", model::startCreate) }
        )

        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Chip(
                text = if (state.onlyOpen) "Só em aberto" else "Todos",
                active = state.onlyOpen,
                onClick = model::toggleOpen
            )
        }

        Spacer(Modifier.height(8.dp))
        ErrorBanner(state.error)

        if (state.loading && state.plans.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.plans.isEmpty()) {
            EmptyState(
                headline = if (state.onlyOpen) "Nada parcelado em aberto" else "Nenhum parcelamento",
                hint = "Cadastre uma compra parcelada e o app gera cada parcela como lançamento, na fatura certa.",
                action = { QuietAction("Criar parcelamento", model::startCreate) }
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.plans, key = { it.id }) { plan ->
                PlanLine(plan = plan, onClick = { model.openDetail(plan) })
                HairlineDivider(inset = 20)
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    val form = state.form

    if (form != null) {
        EditorSheet(title = "Novo parcelamento", onDismiss = model::dismissForm) {
            LabeledField(
                label = "O que foi",
                value = form.description,
                onValueChange = { value -> model.editForm { it.copy(description = value) } },
                placeholder = "Geladeira, notebook…"
            )

            MoneyInput(
                label = if (form.amountIsInstallment) "Valor de cada parcela" else "Valor total",
                reais = form.reais,
                centavos = form.centavos,
                onReaisChange = { value -> model.editForm { it.copy(reais = value) } },
                onCentavosChange = { value -> model.editForm { it.copy(centavos = value) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    text = "Por parcela",
                    active = form.amountIsInstallment,
                    onClick = { model.editForm { it.copy(amountIsInstallment = true) } }
                )
                Chip(
                    text = "Valor total",
                    active = !form.amountIsInstallment,
                    onClick = { model.editForm { it.copy(amountIsInstallment = false) } }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountField(
                    label = "Parcelas",
                    value = form.installments,
                    onValueChange = { value -> model.editForm { it.copy(installments = value) } },
                    placeholder = "12",
                    modifier = Modifier.weight(1f)
                )
                CountField(
                    label = "Já estou na",
                    value = form.firstNumber,
                    onValueChange = { value -> model.editForm { it.copy(firstNumber = value) } },
                    placeholder = "1",
                    modifier = Modifier.weight(1f)
                )
            }

            if (form.installmentCount != null && form.amountCents > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = summary(form),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextStrong
                    )
                }
            }

            if ((form.firstNumberValue ?: 1) > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        text = "Gerar as anteriores como pagas",
                        active = form.backfill,
                        onClick = { model.editForm { it.copy(backfill = !it.backfill) } }
                    )
                }
            }

            DateStepper(
                label = if ((form.firstNumberValue ?: 1) > 1) "Data da parcela ${form.firstNumber}" else "Primeira parcela",
                value = form.firstDueDate,
                onChange = { value -> model.editForm { it.copy(firstDueDate = value) } }
            )

            Column {
                Eyebrow("Onde cai")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.cards.forEach { card ->
                        Chip(
                            text = card.name,
                            active = form.useCard && form.cardId == card.id,
                            onClick = { model.editForm { it.copy(useCard = true, cardId = card.id) } },
                            leading = { ColorDot(card.color) }
                        )
                    }
                    state.accounts.forEach { account ->
                        Chip(
                            text = account.name,
                            active = !form.useCard && form.accountId == account.id,
                            onClick = { model.editForm { it.copy(useCard = false, accountId = account.id) } },
                            leading = { ColorDot(account.color) }
                        )
                    }
                }
            }

            Column {
                Eyebrow("Categoria")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.categories.filter { it.kind != "income" }.forEach { category ->
                        Chip(
                            text = category.name,
                            active = form.categoryId == category.id,
                            onClick = {
                                model.editForm {
                                    it.copy(categoryId = if (it.categoryId == category.id) null else category.id)
                                }
                            },
                            leading = { ColorDot(category.color) }
                        )
                    }
                }
            }

            ErrorBanner(state.error)

            PrimaryAction(
                label = "Criar parcelamento",
                onClick = model::save,
                enabled = form.valid,
                loading = state.saving
            )
        }
    }

    val detail = state.detail
    var confirmingDelete by remember { mutableStateOf(false) }

    if (detail != null) {
        EditorSheet(title = detail.description, onDismiss = model::dismissDetail) {
            Text(
                text = detailSummary(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            HairlineDivider()

            if (detail.generatedCount < detail.installmentsTotal) {
                Text(
                    text = "${detail.generatedCount} de ${detail.installmentsTotal} parcelas — as outras foram excluídas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Warning
                )
            }

            detail.entries.forEach { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            model.dismissDetail()
                            onOpenEntry(entry.id)
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${entry.installmentNumber ?: "?"}/${detail.installmentsTotal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextFaint
                    )
                    Text(
                        text = shortDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (entry.paid) Positive else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    MoneyText(
                        cents = entry.amountCents,
                        size = MoneySize.Inline,
                        showSymbol = false,
                        color = if (entry.paid) TextMuted else TextStrong
                    )
                }
            }

            HairlineDivider()

            QuietAction(
                label = "Excluir parcelamento e parcelas",
                onClick = { confirmingDelete = true },
                destructive = true
            )
        }

        if (confirmingDelete) {
            ConfirmDialog(
                title = "Excluir \"${detail.description}\"?",
                message = "Some o parcelamento e as ${detail.generatedCount} parcelas geradas, inclusive as já pagas. Não dá para desfazer.",
                choices = listOf(
                    ConfirmChoice(
                        label = "Excluir tudo",
                        hint = "R$ ${Money.full(detail.totalAmountCents)} em ${detail.installmentsTotal} parcelas",
                        onPick = { model.delete(detail) }
                    )
                ),
                onDismiss = { confirmingDelete = false }
            )
        }
    }
}

@Composable
private fun PlanLine(plan: InstallmentPlanDto, onClick: () -> Unit) {
    LedgerRow(
        title = plan.description,
        subtitle = planSubtitle(plan),
        showMarker = false,
        navigates = true,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = plan.remainingCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = if (plan.remainingCents > 0) TextStrong else Positive
            )
        }
    )
}

private fun planSubtitle(plan: InstallmentPlanDto): String {
    val where = plan.cardName ?: plan.accountName
    val progress = "${plan.paidCount}/${plan.installmentsTotal} pagas"
    val next = plan.nextDate?.let { "próxima ${shortDate(it)}" }

    return listOfNotNull(progress, next, where).joinToString(" · ")
}

private fun detailSummary(plan: InstallmentPlanDto): String {
    val total = "Total R$ ${Money.full(plan.totalAmountCents)}"
    val paid = "pago R$ ${Money.full(plan.paidCents)}"
    val remaining = "falta R$ ${Money.full(plan.remainingCents)}"

    return "$total · $paid · $remaining"
}

private fun summary(form: PlanForm): String {
    val count = form.installmentCount ?: return ""
    val first = form.firstNumberValue ?: 1
    val generated = count - first + 1
    val each = if (form.amountIsInstallment) form.amountCents else form.amountCents / count

    val head = "${count}x de R$ ${Money.full(each)} · total R$ ${Money.full(form.projectedTotal)}"

    return if (first > 1 && !form.backfill) {
        "$head\nVou gerar da parcela $first à $count ($generated lançamentos)."
    } else {
        head
    }
}

private fun shortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(ShortDate) }.getOrDefault(date)

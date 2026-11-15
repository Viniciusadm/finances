package com.archieapps.finances.ui.screen.holdings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.ui.components.AccountChips
import com.archieapps.finances.ui.components.ChoiceChips
import com.archieapps.finances.ui.components.DateChips
import com.archieapps.finances.ui.components.EditorSheet
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.screen.accounts.AddHoldingForm
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

private val Modes = listOf(
    "owned" to "Já tenho",
    "buy" to "Vou comprar agora"
)

/** Passo a passo para cadastrar um ativo, com a conta em frases simples. */
@Composable
fun AddHoldingSheet(
    form: AddHoldingForm,
    typeLabel: String,
    accounts: List<AccountDto>,
    working: Boolean,
    error: String?,
    onEdit: ((AddHoldingForm) -> AddHoldingForm) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val example = if (typeLabel.equals("Cripto", ignoreCase = true)) "BTC, ETH" else "PETR4, VALE3"

    EditorSheet(title = "Adicionar ativo", onDismiss = onDismiss) {
        LabeledField(
            label = "1. Qual é o ativo?",
            value = form.symbol,
            onValueChange = { value -> onEdit { it.copy(symbol = value.uppercase()) } },
            placeholder = "Código, ex.: $example"
        )

        ChoiceChips(
            label = "2. Você já tem ou vai comprar agora?",
            options = Modes,
            selected = if (form.owned) "owned" else "buy",
            onSelect = { value -> onEdit { it.copy(owned = value == "owned") } }
        )

        Text(
            text = if (form.owned) {
                "Use quando você já tinha esse ativo. Nada sai de nenhuma conta: o valor entra no saldo desta conta."
            } else {
                "Use quando acabou de comprar. O dinheiro sai da conta que você escolher, em uma transferência."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        LabeledField(
            label = "3. Quantas unidades?",
            value = form.quantity,
            onValueChange = { value -> onEdit { it.copy(quantity = value) } },
            placeholder = "Ex.: 10 ou 0,05",
            keyboardType = KeyboardType.Decimal
        )

        LabeledField(
            label = if (form.owned) "4. Preço de 1 unidade hoje (R$)" else "4. Preço pago por 1 unidade (R$)",
            value = form.price,
            onValueChange = { value -> onEdit { it.copy(price = value) } },
            placeholder = "Ex.: 35,20",
            keyboardType = KeyboardType.Decimal
        )

        if (form.owned) {
            LabeledField(
                label = "5. Quanto você pagou por unidade, em média? (opcional)",
                value = form.avgPrice,
                onValueChange = { value -> onEdit { it.copy(avgPrice = value) } },
                placeholder = "Vazio = igual ao preço de hoje",
                keyboardType = KeyboardType.Decimal,
                supporting = "Serve para mostrar seu lucro ou prejuízo."
            )
        } else {
            AccountChips(
                label = "5. De qual conta sai o dinheiro?",
                accounts = accounts,
                selectedId = form.fromAccountId,
                onSelect = { id -> onEdit { it.copy(fromAccountId = id) } }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(if (form.owned) "6. Preço de que dia?" else "6. Quando você comprou?")
            DateChips(date = form.date, onChange = { value -> onEdit { it.copy(date = value) } })
        }

        val total = valueCents(form.quantity, form.price)

        if (total != null && isPositive(form.quantity) && isPositive(form.price)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("O que vai acontecer")
                Text(
                    text = "${formatQuantity(form.quantity)} × R$ ${formatPrice(form.price)} = R$ ${Money.full(total)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextStrong
                )
                Text(
                    text = if (form.owned) {
                        "Esse valor passa a fazer parte do saldo desta conta."
                    } else {
                        "Esse valor sai da conta escolhida e entra nesta conta, como compra."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        ErrorBanner(error)

        PrimaryAction(
            label = "Adicionar ativo",
            onClick = onSubmit,
            enabled = form.valid,
            loading = working
        )

        QuietAction(label = "Cancelar", onClick = onDismiss)
    }
}

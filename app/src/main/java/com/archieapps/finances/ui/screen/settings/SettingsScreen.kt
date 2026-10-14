package com.archieapps.finances.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.LocalContainer
import com.archieapps.finances.data.net.Session
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.SectionHeader
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var forgotten by remember { mutableStateOf(false) }
    val session by container.sessionStore.session.collectAsStateWithLifecycle(
        initialValue = Session("", "")
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Ajustes")

        SectionHeader("Cadastros")

        LedgerRow(
            title = "Contas",
            subtitle = "Onde o dinheiro fica",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("accounts") }
        )
        HairlineDivider(inset = 20)

        LedgerRow(
            title = "Categorias",
            subtitle = "Como o gasto é classificado",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("categories") }
        )
        HairlineDivider(inset = 20)

        LedgerRow(
            title = "Tags",
            subtitle = "Marcações livres, além da categoria",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("tags") }
        )
        HairlineDivider(inset = 20)

        LedgerRow(
            title = "Produtos",
            subtitle = "Último preço pago e custo por dose",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("products") }
        )
        HairlineDivider(inset = 20)

        LedgerRow(
            title = "Pessoas",
            subtitle = "Quem te deve e a quem você deve",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("people") }
        )
        HairlineDivider(inset = 20)

        SectionHeader("Compromissos")

        LedgerRow(
            title = "Parcelamentos",
            subtitle = "Compras divididas em parcelas",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("installments") }
        )
        HairlineDivider(inset = 20)

        LedgerRow(
            title = "Recorrências",
            subtitle = "O que repete todo mês",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("recurrences") }
        )
        HairlineDivider(inset = 20)

        SectionHeader("Análise")

        LedgerRow(
            title = "Relatórios",
            subtitle = "Por categoria, tag e mês",
            showMarker = false,
            navigates = true,
            onClick = { onOpen("reports") }
        )
        HairlineDivider(inset = 20)

        SectionHeader("Acesso")

        LedgerRow(
            title = "Esquecer o código guardado",
            subtitle = "Pede o código de novo na próxima abertura",
            showMarker = false,
            onClick = {
                scope.launch {
                    container.codeGate.forget()
                    forgotten = true
                }
            }
        )
        HairlineDivider(inset = 20)

        if (forgotten) {
            Text(
                text = "Código removido do aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = Positive,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        SectionHeader("Conexão")

        Text(
            text = session.baseUrl,
            style = MaterialTheme.typography.bodyMedium,
            color = TextFaint,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(20.dp))

        QuietAction(
            label = "Sair desta conta",
            onClick = {
                scope.launch {
                    container.codeGate.forget()
                    container.sessionHolder.token = ""
                    container.sessionStore.clearToken()
                }
            },
            destructive = true,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(40.dp))
    }
}

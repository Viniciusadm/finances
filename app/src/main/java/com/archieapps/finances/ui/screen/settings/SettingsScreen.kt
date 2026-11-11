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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.archieapps.finances.ui.components.IconBadge
import com.archieapps.finances.ui.components.NoticeBanner
import com.archieapps.finances.ui.components.grouped
import com.archieapps.finances.ui.theme.Accent
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import com.archieapps.finances.ui.theme.Warning
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
        ScreenHeader("Mais")

        MenuGroup(
            title = "Cadastros",
            items = listOf(
                MenuItem("Contas", "Bancos, caixinhas, aplicações e dinheiro", AppIcons.Bank, Primary) { onOpen("accounts") },
                MenuItem("Categorias", "Como o gasto é classificado", AppIcons.Category, Accent) { onOpen("categories") },
                MenuItem("Tags", "Marcações livres, além da categoria", AppIcons.Tag, Warning) { onOpen("tags") },
                MenuItem("Produtos", "Último preço pago e custo por dose", AppIcons.Cart, Positive) { onOpen("products") },
                MenuItem("Pessoas", "Quem te deve e a quem você deve", AppIcons.Group, Negative) { onOpen("people") }
            )
        )

        MenuGroup(
            title = "Compromissos",
            items = listOf(
                MenuItem("Parcelamentos", "Compras divididas em parcelas", AppIcons.Layers, Primary) { onOpen("installments") },
                MenuItem("Recorrências", "O que repete todo mês", AppIcons.Repeat, Accent) { onOpen("recurrences") }
            )
        )

        MenuGroup(
            title = "Análise",
            items = listOf(
                MenuItem("Relatórios", "Por categoria, tag, conta e mês", AppIcons.PieChart, Positive) { onOpen("reports") }
            )
        )

        SectionHeader("Acesso")

        Column(Modifier.grouped(0, 2)) {
            LedgerRow(
                title = "Esquecer o código guardado",
                subtitle = "Pede o código de novo na próxima abertura",
                icon = AppIcons.Key,
                markerHex = "#FBBF24",
                onClick = {
                    scope.launch {
                        container.codeGate.forget()
                        forgotten = true
                    }
                }
            )
        }
        Column(Modifier.grouped(1, 2)) {
            LedgerRow(
                title = "Servidor",
                subtitle = session.baseUrl,
                icon = AppIcons.Cloud,
                markerHex = "#9AA4B5"
            )
        }

        NoticeBanner(if (forgotten) "Código removido do aparelho." else null, Modifier.padding(top = 8.dp))

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
            icon = AppIcons.Logout,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(40.dp))
    }
}

private class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun MenuGroup(title: String, items: List<MenuItem>) {
    SectionHeader(title)

    items.forEachIndexed { index, item ->
        Column(Modifier.grouped(index, items.size)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = item.icon, color = item.color)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = TextStrong)
                    Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextFaint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

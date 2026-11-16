package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Ink
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 40.dp, height = 5.dp)
            .background(Hairline, RoundedCornerShape(3.dp))
    )
}

@Composable
fun SheetTitle(title: String, onClose: () -> Unit, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextStrong,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
        IconAction(
            icon = AppIcons.Close,
            onClick = onClose,
            contentDescription = "Fechar",
            tint = TextMuted,
            size = 36,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Página de edição em tela cheia. Não existe área de fora para tocar nem gesto de arrastar:
 * só a seta do cabeçalho ou o botão de voltar do sistema fecham, para não perder o que foi digitado.
 */
@Composable
fun EditorPage(
    title: String,
    onBack: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding()
        ) {
            ScreenHeader(title = title, onBack = onBack)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                content()
            }

            if (footer != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    footer()
                }
            }
        }
    }
}

/** Mantido com o nome antigo: agora abre uma página em tela cheia, e não uma janela que fecha ao tocar fora. */
@Composable
fun EditorSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    EditorPage(title = title, onBack = onDismiss, content = content)
}


/** Tela inteira de formulário, usada pelas rotas: cabeçalho com voltar, conteúdo rolável e rodapé fixo opcional. */
@Composable
fun PageScaffold(
    title: String,
    onBack: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(title = title, onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            content()
        }

        if (footer != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { footer() }
        }
    }
}

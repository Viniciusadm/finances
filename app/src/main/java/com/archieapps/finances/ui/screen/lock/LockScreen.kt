package com.archieapps.finances.ui.screen.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.core.session.CodePad
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.SurfaceLift
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val model = appViewModel { container -> LockViewModel(container.codeGate) }
    val state by model.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity

    LaunchedEffect(Unit) { model.start(activity) }

    LaunchedEffect(state.unlocked) {
        if (state.unlocked) onUnlocked()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp)
    ) {
        Eyebrow("Finanças")
        Spacer(Modifier.height(10.dp))

        Text(
            text = if (state.askingCode) "Código de hoje" else "Confirme para abrir",
            style = MaterialTheme.typography.displayLarge,
            color = TextStrong
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = hint(state),
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )

        Spacer(Modifier.height(24.dp))

        ErrorBanner(state.error, Modifier.padding(horizontal = 0.dp))

        if (state.checking) {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
            }
        }

        if (state.askingCode) {
            Spacer(Modifier.height(8.dp))
            Dots(typed = state.typed)
            Spacer(Modifier.height(20.dp))

            state.keypad.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { option ->
                        PadKey(
                            label = CodePad.label(option),
                            enabled = !state.hardStopped && !state.sending,
                            modifier = Modifier.weight(1f),
                            onClick = { model.pick(option) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuietAction(label = "Limpar", onClick = model::clear)

                if (!state.biometricsMissing) {
                    QuietAction(label = "Usar digital", onClick = { model.retryBiometrics(activity) })
                }
            }

            Spacer(Modifier.height(14.dp))

            PrimaryAction(
                label = "Liberar",
                onClick = { model.submit(activity) },
                enabled = state.complete && !state.hardStopped,
                loading = state.sending
            )

            if (state.remaining != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Restam ${state.remaining} tentativas antes do bloqueio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextFaint
                )
            }
        } else if (!state.checking) {
            PrimaryAction(
                label = "Tentar de novo",
                onClick = { model.retryBiometrics(activity) }
            )
        }
    }
}

@Composable
private fun Dots(typed: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(CodePad.SLOTS) { index ->
            Box(
                Modifier
                    .size(14.dp)
                    .background(
                        if (index < typed) Primary else Hairline,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun PadKey(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(58.dp)
            .background(SurfaceLift, RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Figures,
                fontSize = 19.sp,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center
            ),
            color = if (enabled) TextStrong else TextFaint
        )
    }
}

private fun hint(state: LockState): String = when {
    state.hardStopped -> "Muitas tentativas. O acesso volta amanhã."
    state.askingCode && state.biometricsMissing ->
        "Sem digital no aparelho, o código fica só nesta sessão."
    state.askingCode ->
        "Toque na tecla que contém o dígito de hoje, quatro vezes. Depois guardo com a sua digital."
    else -> "Todas as telas precisam do código do dia. A digital libera o código guardado."
}

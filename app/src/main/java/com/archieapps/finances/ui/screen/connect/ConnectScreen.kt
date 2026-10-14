package com.archieapps.finances.ui.screen.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.LabeledField
import com.archieapps.finances.ui.components.PrimaryAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun ConnectScreen() {
    val model = appViewModel { container -> ConnectViewModel(container.repository, container.sessionStore, container.sessionHolder) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Eyebrow("Finanças")
        Text(
            text = "Entre para começar a lançar.",
            style = MaterialTheme.typography.displayLarge,
            color = TextStrong
        )
        Text(
            text = "O app conversa com o seu servidor. Confirme o endereço antes de entrar.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )

        Spacer(Modifier.height(4.dp))

        LabeledField(
            label = "Servidor",
            value = state.baseUrl,
            onValueChange = model::onBaseUrl,
            placeholder = "http://127.0.0.1:8001",
            keyboardType = KeyboardType.Uri,
            supporting = "Endereço onde o seu servidor responde."
        )

        LabeledField(
            label = "Email",
            value = state.email,
            onValueChange = model::onEmail,
            placeholder = "voce@exemplo.com",
            keyboardType = KeyboardType.Email
        )

        LabeledField(
            label = "Senha",
            value = state.password,
            onValueChange = model::onPassword,
            password = true,
            imeAction = ImeAction.Done
        )

        ErrorBanner(state.error, Modifier.padding(horizontal = 0.dp))

        PrimaryAction(
            label = "Entrar",
            onClick = model::submit,
            enabled = state.canSubmit,
            loading = state.loading
        )
    }
}

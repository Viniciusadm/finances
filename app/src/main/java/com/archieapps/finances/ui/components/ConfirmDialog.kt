package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

data class ConfirmChoice(
    val label: String,
    val hint: String? = null,
    val destructive: Boolean = true,
    val onPick: () -> Unit
)

@Composable
fun ConfirmDialog(
    title: String,
    message: String? = null,
    choices: List<ConfirmChoice>,
    cancelLabel: String = "Cancelar",
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(20.dp))
                .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextStrong
            )

            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            choices.forEach { choice ->
                ChoiceRow(choice, onDismiss)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cancelLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(choice: ConfirmChoice, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable {
                onDismiss()
                choice.onPick()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = choice.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (choice.destructive) Negative else TextStrong
        )

        if (choice.hint != null) {
            Text(
                text = choice.hint,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

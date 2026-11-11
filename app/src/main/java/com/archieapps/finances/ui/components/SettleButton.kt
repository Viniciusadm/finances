package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.finances.ui.theme.OnPrimary
import com.archieapps.finances.ui.theme.Primary

@Composable
fun SettleButton(receiving: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = if (receiving) "Recebi" else "Paguei",
        style = MaterialTheme.typography.labelLarge,
        color = OnPrimary,
        modifier = modifier
            .background(Primary, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

package com.archieapps.finances.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val LongDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PtBr)

@Composable
fun DateStepper(
    label: String,
    value: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        Spacer(Modifier.height(8.dp))

        Text(
            text = value.format(LongDate),
            style = MaterialTheme.typography.titleMedium,
            color = TextStrong
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(text = "− dia", active = false, onClick = { onChange(value.minusDays(1)) })
            Chip(text = "+ dia", active = false, onClick = { onChange(value.plusDays(1)) })
            Chip(text = "− mês", active = false, onClick = { onChange(value.minusMonths(1)) })
            Chip(text = "+ mês", active = false, onClick = { onChange(value.plusMonths(1)) })
            Chip(text = "hoje", active = false, onClick = { onChange(LocalDate.now()) })
        }
    }
}

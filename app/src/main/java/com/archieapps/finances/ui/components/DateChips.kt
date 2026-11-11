package com.archieapps.finances.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TextMuted
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ChipDay = DateTimeFormatter.ofPattern("d 'de' MMM", Locale.forLanguageTag("pt-BR"))

@Composable
fun DateChips(date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    var picking by remember { mutableStateOf(false) }
    val custom = date != today && date != yesterday

    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip(text = "Hoje", active = date == today, onClick = { onChange(today) })
        Chip(text = "Ontem", active = date == yesterday, onClick = { onChange(yesterday) })
        Chip(
            text = if (custom) date.format(ChipDay) else "Outra data",
            active = custom,
            icon = AppIcons.Calendar,
            onClick = { picking = true }
        )
        IconAction(icon = AppIcons.ChevronLeft, onClick = { onChange(date.minusDays(1)) }, size = 36, tint = TextMuted, contentDescription = "Dia anterior")
        IconAction(icon = AppIcons.ChevronRight, onClick = { onChange(date.plusDays(1)) }, size = 36, tint = TextMuted, contentDescription = "Próximo dia")
    }

    if (picking) {
        DatePickerPopup(initial = date, onDismiss = { picking = false }, onPick = {
            picking = false
            onChange(it)
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerPopup(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    )
    val colors = DatePickerDefaults.colors(
        containerColor = Surface,
        selectedDayContainerColor = Primary,
        todayDateBorderColor = Primary,
        todayContentColor = Primary
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextAction("Escolher", onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) else onDismiss()
            })
        },
        dismissButton = { TextAction("Cancelar", onClick = onDismiss, color = TextMuted) }
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}

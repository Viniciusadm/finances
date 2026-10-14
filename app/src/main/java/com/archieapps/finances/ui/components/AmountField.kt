package com.archieapps.finances.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supporting: String? = null
) {
    LabeledField(
        label = label,
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter(Char::isDigit).take(3)) },
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        supporting = supporting
    )
}

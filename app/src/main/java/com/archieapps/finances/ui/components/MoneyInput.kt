package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.finances.core.Money
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.Hairline
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.TabularFigures
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun MoneyInput(
    label: String,
    reais: String,
    centavos: String,
    onReaisChange: (String) -> Unit,
    onCentavosChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "R$",
                style = figureStyle(15.sp),
                color = TextFaint
            )

            Spacer(Modifier.width(8.dp))

            BasicTextField(
                value = reais,
                onValueChange = { raw -> onReaisChange(raw.filter(Char::isDigit).take(9)) },
                textStyle = figureStyle(26.sp).copy(color = TextStrong, textAlign = TextAlign.End),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (reais.isEmpty()) {
                            Text(text = "0", style = figureStyle(26.sp), color = TextFaint)
                        }
                        field()
                    }
                }
            )

            Text(text = ",", style = figureStyle(20.sp), color = TextStrong)

            BasicTextField(
                value = centavos,
                onValueChange = { raw -> onCentavosChange(raw.filter(Char::isDigit).take(2)) },
                textStyle = figureStyle(18.sp).copy(color = TextStrong, textAlign = TextAlign.Start),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                modifier = Modifier.width(54.dp),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (centavos.isEmpty()) {
                            Text(text = "00", style = figureStyle(18.sp), color = TextFaint)
                        }
                        field()
                    }
                }
            )
        }
    }
}

private fun figureStyle(size: androidx.compose.ui.unit.TextUnit) = TextStyle(
    fontFamily = Figures,
    fontFeatureSettings = TabularFigures,
    fontSize = size,
    fontWeight = FontWeight.W600
)

fun moneyCents(reais: String, centavos: String): Long = Money.fromTyped(reais, centavos)

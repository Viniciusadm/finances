package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
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
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.SurfaceLift
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
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val main = if (compact) 18.sp else 26.sp
    val comma = if (compact) 16.sp else 20.sp
    val cents = if (compact) 16.sp else 18.sp
    val padding = if (compact) 15.dp else 12.dp

    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(SurfaceLift, RoundedCornerShape(14.dp))
                .padding(horizontal = if (compact) 16.dp else 14.dp, vertical = padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "R$",
                style = figureStyle(if (compact) 14.sp else 15.sp),
                color = TextFaint
            )

            Spacer(Modifier.width(8.dp))

            BasicTextField(
                value = reais,
                onValueChange = { raw -> onReaisChange(raw.filter(Char::isDigit).take(9)) },
                textStyle = figureStyle(main).copy(color = TextStrong, textAlign = TextAlign.End),
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
                            Text(text = "0", style = figureStyle(main), color = TextFaint)
                        }
                        field()
                    }
                }
            )

            Text(text = ",", style = figureStyle(comma), color = TextStrong)

            BasicTextField(
                value = centavos,
                onValueChange = { raw -> onCentavosChange(raw.filter(Char::isDigit).take(2)) },
                textStyle = figureStyle(cents).copy(color = TextStrong, textAlign = TextAlign.Start),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                modifier = Modifier.width(if (compact) 40.dp else 54.dp),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (centavos.isEmpty()) {
                            Text(text = "00", style = figureStyle(cents), color = TextFaint)
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

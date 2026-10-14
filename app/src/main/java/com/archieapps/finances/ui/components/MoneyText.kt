package com.archieapps.finances.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.finances.core.Money
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.TabularFigures
import com.archieapps.finances.ui.theme.TextFaint
import androidx.compose.ui.text.TextStyle

enum class MoneySize(val integer: TextUnit, val cents: TextUnit, val symbol: TextUnit) {
    Hero(46.sp, 22.sp, 15.sp),
    Section(28.sp, 15.sp, 11.sp),
    Row(17.sp, 11.sp, 9.sp),
    Inline(14.sp, 10.sp, 8.sp)
}

@Composable
fun MoneyText(
    cents: Long,
    size: MoneySize = MoneySize.Row,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    symbol: String = "R$",
    showSymbol: Boolean = true,
    weight: FontWeight = FontWeight.W600
) {
    val integer = Money.integerPart(cents)
    val decimals = Money.centsPart(cents)
    val sign = Money.sign(cents)

    Row(modifier = modifier) {
        if (showSymbol) {
            Text(
                text = symbol,
                style = TextStyle(
                    fontFamily = Figures,
                    fontFeatureSettings = TabularFigures,
                    fontSize = size.symbol,
                    fontWeight = FontWeight.W500
                ),
                color = TextFaint,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(5.dp))
        }

        Text(
            text = sign + integer,
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = size.integer,
                fontWeight = weight,
                letterSpacing = (-0.5).sp
            ),
            color = color,
            modifier = Modifier.alignByBaseline()
        )

        Text(
            text = ",$decimals",
            style = TextStyle(
                fontFamily = Figures,
                fontFeatureSettings = TabularFigures,
                fontSize = size.cents,
                fontWeight = weight
            ),
            color = color.copy(alpha = 0.72f),
            modifier = Modifier.alignByBaseline()
        )
    }
}

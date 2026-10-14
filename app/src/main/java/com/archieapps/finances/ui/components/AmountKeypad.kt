package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.finances.ui.theme.Figures
import com.archieapps.finances.ui.theme.MonoLabel
import com.archieapps.finances.ui.theme.OnPrimary
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.SurfaceLift
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong

@Composable
fun AmountKeypad(
    onDigit: (Char) -> Unit,
    onCents: () -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Salvar",
    confirmEnabled: Boolean = true,
    confirming: Boolean = false,
    centsActive: Boolean = false,
    keyHeight: Int = 50
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    DigitKey(digit, keyHeight, Modifier.weight(1f)) { onDigit(digit.first()) }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommaKey(keyHeight, Modifier.weight(1f), centsActive, onCents)
            DigitKey("0", keyHeight, Modifier.weight(1f)) { onDigit('0') }
            GlyphKey("⌫", keyHeight, Modifier.weight(1f), onBackspace)
        }

        ConfirmBar(
            label = confirmLabel,
            enabled = confirmEnabled,
            loading = confirming,
            onClick = onConfirm
        )
    }
}

@Composable
private fun DigitKey(digit: String, height: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(height.dp)
            .background(SurfaceLift, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = TextStyle(fontFamily = Figures, fontSize = 23.sp, fontWeight = FontWeight.W600),
            color = TextStrong
        )
    }
}

@Composable
private fun CommaKey(height: Int, modifier: Modifier, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .height(height.dp)
            .background(
                if (active) Primary.copy(alpha = 0.18f) else SurfaceLift,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "centavos",
            style = TextStyle(fontFamily = MonoLabel, fontSize = 11.sp, fontWeight = FontWeight.W500, letterSpacing = 1.sp),
            color = if (active) Primary else TextMuted
        )
    }
}

@Composable
private fun GlyphKey(glyph: String, height: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(height.dp)
            .background(Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = TextStyle(fontFamily = MonoLabel, fontSize = 20.sp, fontWeight = FontWeight.W500),
            color = TextFaint
        )
    }
}

@Composable
private fun ConfirmBar(label: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (enabled) Primary else SurfaceLift, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = OnPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.W700),
                color = if (enabled) OnPrimary else TextFaint
            )
        }
    }
}

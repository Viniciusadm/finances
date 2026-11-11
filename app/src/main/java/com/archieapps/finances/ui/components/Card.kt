package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.SurfaceLift
import com.archieapps.finances.ui.theme.TextMuted

private const val CardRadius = 22

val CardShape = RoundedCornerShape(CardRadius.dp)

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) CardRadius.dp else 6.dp
    val bottom = if (index == count - 1) CardRadius.dp else 6.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

fun Modifier.grouped(index: Int, count: Int, color: Color = Surface): Modifier = this
    .padding(horizontal = 16.dp)
    .padding(bottom = if (index == count - 1) 0.dp else 2.dp)
    .fillMaxWidth()
    .clip(groupShape(index, count))
    .background(color)

fun Modifier.cardPadding(): Modifier = padding(horizontal = 16.dp)

@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Primary,
    track: Color = SurfaceLift,
    height: Int = 6
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp))
                .background(color)
        )
    }
}

@Composable
fun StatTile(
    label: String,
    cents: Long,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    SurfaceCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        IconBadge(icon = icon, color = color, size = 32)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Spacer(Modifier.height(2.dp))
        MoneyText(cents = cents, size = MoneySize.Row, color = color, showSymbol = false)
    }
}

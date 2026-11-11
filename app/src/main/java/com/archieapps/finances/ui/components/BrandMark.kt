package com.archieapps.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.HeroBrush

@Composable
fun BrandMark(modifier: Modifier = Modifier, icon: ImageVector = AppIcons.Wallet) {
    Box(
        modifier
            .size(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HeroBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

package com.archieapps.finances.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.archieapps.finances.ui.theme.AppIcons

fun accountIcon(kind: String?, investmentType: String? = null): ImageVector = when (kind) {
    "cash" -> AppIcons.Wallet
    "investment" -> when (investmentType) {
        "stock" -> AppIcons.BarChart
        "crypto" -> AppIcons.Layers
        else -> AppIcons.TrendingUp
    }
    "savings" -> AppIcons.Archive
    else -> AppIcons.Bank
}

fun entryIcon(kind: String?): ImageVector? = when (kind) {
    "income" -> AppIcons.ArrowDown
    "transfer" -> AppIcons.Swap
    "yield" -> AppIcons.TrendingUp
    else -> null
}

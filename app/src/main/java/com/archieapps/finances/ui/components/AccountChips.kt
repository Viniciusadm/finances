package com.archieapps.finances.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.finances.data.dto.AccountDto

fun accountLabel(account: AccountDto): String {
    val institution = account.institution?.takeIf { it.isNotBlank() } ?: return account.name

    return if (account.name.contains(institution, ignoreCase = true)) account.name else "${account.name} · $institution"
}

@Composable
fun AccountChips(
    label: String,
    accounts: List<AccountDto>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Eyebrow(label)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                Chip(
                    text = accountLabel(account),
                    active = selectedId == account.id,
                    onClick = { onSelect(account.id) }
                )
            }
        }
    }
}

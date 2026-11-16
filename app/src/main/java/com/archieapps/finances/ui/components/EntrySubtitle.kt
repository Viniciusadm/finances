package com.archieapps.finances.ui.components

import com.archieapps.finances.data.dto.EntryDto

/**
 * Subtítulo de um lançamento nas listas: o local primeiro, depois o que ajuda a localizar o
 * lançamento. A categoria some quando repete o título.
 */
fun entrySubtitle(
    entry: EntryDto,
    leading: List<String> = emptyList(),
    withSource: Boolean = true
): String {
    val where = when {
        !withSource -> null
        entry.kind == "transfer" -> listOfNotNull(entry.accountName, entry.counterAccountName).joinToString(" → ").ifBlank { null }
        else -> entry.cardName ?: entry.accountName
    }

    val items = entry.itemsCount?.takeIf { it > 0 }?.let { if (it == 1) "1 item" else "$it itens" }

    val dividend = if (entry.isDividend) "Dividendo${entry.holdingSymbol?.let { " $it" } ?: ""}" else null

    return (
        listOfNotNull(dividend) + leading +
            listOfNotNull(
                entry.placeLabel,
                entry.categoryName?.takeIf { it != entry.description },
                items,
                where,
                entry.personName,
                if (entry.paid) null else "previsto"
            )
        ).joinToString(" · ")
}

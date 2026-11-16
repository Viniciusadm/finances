package com.archieapps.finances.ui

import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.ui.components.entrySubtitle
import org.junit.Assert.assertEquals
import org.junit.Test

class EntrySubtitleTest {

    private fun entry(
        description: String = "Combo",
        place: String? = null,
        category: String? = null,
        card: String? = null,
        items: Int? = null,
        paid: Boolean = true
    ) = EntryDto(
        id = 1,
        kind = "expense",
        kindLabel = "Gasto",
        description = description,
        amountCents = 3390,
        signedAmountCents = -3390,
        date = "2026-10-30",
        paid = paid,
        placeLabel = place,
        categoryName = category,
        cardName = card,
        itemsCount = items
    )

    @Test
    fun placeComesFirstThenCategoryAndSource() {
        val subtitle = entrySubtitle(entry(place = "Burger King · Águas Claras Shopping", category = "Restaurante", card = "Inter Virtual"))

        assertEquals("Burger King · Águas Claras Shopping · Restaurante · Inter Virtual", subtitle)
    }

    @Test
    fun categoryIsSkippedWhenItRepeatsTheTitle() {
        assertEquals("Inter Virtual", entrySubtitle(entry(description = "Restaurante", category = "Restaurante", card = "Inter Virtual")))
    }

    @Test
    fun itemsCountAndPendingAreShown() {
        assertEquals("1 item · previsto", entrySubtitle(entry(items = 1, paid = false)))
        assertEquals("3 itens", entrySubtitle(entry(items = 3)))
    }

    @Test
    fun leadingPartsAndNoSource() {
        val subtitle = entrySubtitle(entry(place = "Hostinger", card = "Inter Virtual"), leading = listOf("10/11"), withSource = false)

        assertEquals("10/11 · Hostinger", subtitle)
    }
}

package com.archieapps.finances.ui

import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.EstablishmentDto
import com.archieapps.finances.data.repo.ReferenceSnapshot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceSnapshotTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun snapshotSurvivesTheRoundTripToDisk() {
        val snapshot = ReferenceSnapshot(
            accounts = listOf(AccountDto(id = 1, name = "Nubank", kind = "checking", kindLabel = "Conta corrente", initialBalanceCents = 100)),
            establishments = listOf(EstablishmentDto(id = 2, name = "Express Grill")),
            loadedAt = 123
        )

        val decoded = json.decodeFromString<ReferenceSnapshot>(json.encodeToString(ReferenceSnapshot.serializer(), snapshot))

        assertEquals(snapshot, decoded)
    }

    @Test
    fun oldOrPartialCacheStillLoads() {
        val decoded = json.decodeFromString<ReferenceSnapshot>("""{"loadedAt": 5, "campoNovo": 1}""")

        assertEquals(5L, decoded.loadedAt)
        assertEquals(emptyList<AccountDto>(), decoded.accounts)
    }
}

package com.archieapps.finances.data.repo

import android.util.Log
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.EstablishmentDto
import com.archieapps.finances.data.dto.PersonDto
import com.archieapps.finances.data.dto.ProductCategoryDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Dados de apoio que várias telas usam (contas, cartões, categorias…). */
@Serializable
data class ReferenceSnapshot(
    val accounts: List<AccountDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val people: List<PersonDto> = emptyList(),
    val establishments: List<EstablishmentDto> = emptyList(),
    val productCategories: List<ProductCategoryDto> = emptyList(),
    val loadedAt: Long = 0
)

/**
 * Guarda no app os dados que o servidor devolve para montar as telas de lançamento e produtos.
 * Eles vêm do disco na hora (sem esperar a rede), e a rede só atualiza por baixo, sem a tela
 * ficar sem dados nem mudar de tamanho depois de aberta.
 */
class ReferenceData(
    private val repository: FinanceRepository,
    private val file: File
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val mutex = Mutex()

    private val _snapshot = MutableStateFlow(readDisk())
    val snapshot: StateFlow<ReferenceSnapshot> = _snapshot.asStateFlow()

    /** Já há dados (do disco ou da rede) para abrir as telas. */
    private val _ready = MutableStateFlow(_snapshot.value.loadedAt > 0)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _failure = MutableStateFlow<String?>(null)
    val failure: StateFlow<String?> = _failure.asStateFlow()

    suspend fun refresh() = mutex.withLock {
        val results = coroutineScope {
            val accounts = async { repository.accounts() }
            val cards = async { repository.cards() }
            val categories = async { repository.categories() }
            val people = async { repository.people() }
            val establishments = async { repository.establishments() }
            val productCategories = async { repository.productCategories() }

            listOf(accounts, cards, categories, people, establishments, productCategories).map { it.await() }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> pick(index: Int, current: List<T>): List<T> =
            (results[index] as? ApiResult.Ok<List<T>>)?.value ?: current

        val failed = results.filterIsInstance<ApiResult.Failure>().firstOrNull()
        val anyOk = results.any { it is ApiResult.Ok<*> }

        if (!anyOk) {
            _failure.value = failed?.message ?: "Sem conexão com o servidor."
            return@withLock
        }

        _failure.value = null

        val current = _snapshot.value
        val next = ReferenceSnapshot(
            accounts = pick(0, current.accounts),
            cards = pick(1, current.cards),
            categories = pick(2, current.categories),
            people = pick(3, current.people),
            establishments = pick(4, current.establishments),
            productCategories = pick(5, current.productCategories),
            loadedAt = System.currentTimeMillis()
        )

        if (next != current) {
            _snapshot.update { next }
            writeDisk(next)
        }

        _ready.value = true
    }

    private fun readDisk(): ReferenceSnapshot = runCatching {
        if (file.exists()) json.decodeFromString<ReferenceSnapshot>(file.readText()) else ReferenceSnapshot()
    }.getOrElse {
        Log.w("ReferenceData", "cache ilegível, ignorando: ${it.message}")
        ReferenceSnapshot()
    }

    private fun writeDisk(snapshot: ReferenceSnapshot) {
        runCatching { file.writeText(json.encodeToString(ReferenceSnapshot.serializer(), snapshot)) }
            .onFailure { Log.w("ReferenceData", "não consegui gravar o cache: ${it.message}") }
    }
}

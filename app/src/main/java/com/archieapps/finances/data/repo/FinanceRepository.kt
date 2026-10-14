package com.archieapps.finances.data.repo

import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.EntryDraftDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.Envelope
import com.archieapps.finances.data.dto.OverviewDto
import com.archieapps.finances.data.dto.PageInfo
import com.archieapps.finances.data.dto.PagedEnvelope
import com.archieapps.finances.data.dto.PersonBalanceDto
import com.archieapps.finances.data.dto.PersonDto
import com.archieapps.finances.data.dto.InstallmentPlanDto
import com.archieapps.finances.data.dto.RecurrenceDto
import com.archieapps.finances.data.dto.CategoryReportRowDto
import com.archieapps.finances.data.dto.MethodReportRowDto
import com.archieapps.finances.data.dto.MiscEnvelope
import com.archieapps.finances.data.dto.MonthReportMiscDto
import com.archieapps.finances.data.dto.MonthReportRowDto
import com.archieapps.finances.data.dto.PriceHistoryDto
import com.archieapps.finances.data.dto.ReportWindowDto
import com.archieapps.finances.data.dto.TagReportRowDto
import com.archieapps.finances.data.dto.ProductCategoryDto
import com.archieapps.finances.data.dto.ProductDto
import com.archieapps.finances.data.dto.StatementDto
import com.archieapps.finances.data.dto.UpcomingOccurrenceDto
import com.archieapps.finances.data.dto.TagDto
import com.archieapps.finances.data.net.FinancesApi
import com.archieapps.finances.data.net.NetworkModule
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Response
import kotlinx.serialization.Serializable
import java.io.IOException

class FinanceRepository(private val api: FinancesApi) {

    suspend fun login(email: String, password: String): ApiResult<String> = call {
        api.login(buildJsonObject {
            put("email", email)
            put("password", password)
        })
    }.let { result ->
        when (result) {
            is ApiResult.Ok -> {
                val token = result.value.token
                if (token.isNullOrBlank()) {
                    ApiResult.Failure("A resposta do servidor não trouxe um token.")
                } else {
                    ApiResult.Ok(token)
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun validateCode(code: String): CodeCheck = try {
        val response = api.testCode(buildJsonObject { put("code", code) })
        val envelope = response.body()

        when {
            response.code() == 401 -> CodeCheck.Failed("Sessão expirada. Entre novamente.")
            response.isSuccessful && envelope?.success == true -> CodeCheck.Accepted
            else -> classify(failureMessage(response))
        }
    } catch (e: IOException) {
        CodeCheck.Failed("Sem conexão com o servidor. Confira o endereço em Ajustes.")
    } catch (e: Exception) {
        CodeCheck.Failed(e.message ?: "Erro inesperado.")
    }

    private fun classify(message: String): CodeCheck = when {
        message.contains("bloquead", ignoreCase = true) -> CodeCheck.Blocked
        message.contains("inválid", ignoreCase = true) ||
            message.contains("invalid", ignoreCase = true) ->
            CodeCheck.Rejected(message, attemptsLeft(message))
        else -> CodeCheck.Failed(message)
    }

    private fun attemptsLeft(message: String): Int? =
        Regex("mais (\\d+) tentativa").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()

    suspend fun overview(month: String? = null): ApiResult<OverviewDto> = call { api.overview(month) }

    suspend fun entries(filter: EntryFilter, page: Int = 1): ApiResult<EntryPage> = try {
        val response = api.entries(
            from = filter.from,
            to = filter.to,
            kind = filter.kind,
            categoryId = filter.categoryId,
            cardId = filter.cardId,
            accountId = filter.accountId,
            personId = filter.personId,
            tag = filter.tag,
            paid = filter.paid?.let { if (it) 1 else 0 },
            search = filter.search?.takeIf { it.isNotBlank() },
            page = page,
            perPage = filter.perPage
        )

        val envelope = response.body()

        when {
            response.code() == 401 -> ApiResult.Failure("Sessão expirada. Entre novamente.", unauthorized = true)
            response.isSuccessful && envelope?.success == true ->
                ApiResult.Ok(EntryPage(envelope.body, envelope.pagination))
            else -> ApiResult.Failure(pagedFailure(response))
        }
    } catch (e: IOException) {
        ApiResult.Failure("Sem conexão com o servidor. Confira o endereço em Ajustes.")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Erro inesperado.")
    }

    suspend fun entry(id: Int): ApiResult<EntryDto> = call { api.entry(id) }

    suspend fun createEntry(body: JsonObject): ApiResult<EntryDto> = call { api.createEntry(body) }

    suspend fun quickEntry(body: JsonObject): ApiResult<EntryDto> = call { api.quickEntry(body) }

    suspend fun updateEntry(id: Int, body: JsonObject): ApiResult<EntryDto> = call { api.updateEntry(id, body) }

    suspend fun payEntry(id: Int): ApiResult<EntryDto> = call { api.payEntry(id, buildJsonObject { }) }

    suspend fun unpayEntry(id: Int): ApiResult<EntryDto> = call { api.unpayEntry(id, buildJsonObject { }) }

    suspend fun deleteEntry(id: Int): ApiResult<JsonObject> = call { api.deleteEntry(id) }

    suspend fun createTransfer(body: JsonObject): ApiResult<EntryDto> = call { api.createTransfer(body) }

    suspend fun parseEntryDraft(text: String): ApiResult<EntryDraftDto> =
        call { api.parseEntryDraft(jsonBody { put("text", text) }) }

    suspend fun commitEntryDraft(body: JsonObject): ApiResult<JsonObject> = call { api.commitEntryDraft(body) }

    suspend fun personBalances(includeSettled: Boolean = false): ApiResult<List<PersonBalanceDto>> =
        call { api.personBalances(if (includeSettled) 1 else null) }

    suspend fun people(): ApiResult<List<PersonDto>> = call { api.people() }

    suspend fun personEntries(id: Int): ApiResult<List<EntryDto>> = call { api.personEntries(id) }

    suspend fun settlePerson(id: Int, body: JsonObject): ApiResult<EntryDto> = call { api.settlePerson(id, body) }

    suspend fun accounts(includeArchived: Boolean = false): ApiResult<List<AccountDto>> =
        call { api.accounts(if (includeArchived) 1 else null) }

    suspend fun createAccount(body: JsonObject): ApiResult<AccountDto> = call { api.createAccount(body) }

    suspend fun updateAccount(id: Int, body: JsonObject): ApiResult<AccountDto> = call { api.updateAccount(id, body) }

    suspend fun archiveAccount(id: Int, archived: Boolean): ApiResult<AccountDto> =
        call { api.archiveAccount(id, buildJsonObject { put("archived", archived) }) }

    suspend fun reorderAccounts(ids: List<Int>): ApiResult<JsonObject> =
        call { api.reorderAccounts(orderedIds(ids)) }

    suspend fun deleteAccount(id: Int): ApiResult<JsonObject> = call { api.deleteAccount(id) }

    suspend fun cards(includeArchived: Boolean = false): ApiResult<List<CardDto>> =
        call { api.cards(if (includeArchived) 1 else null) }

    suspend fun createCard(body: JsonObject): ApiResult<CardDto> = call { api.createCard(body) }

    suspend fun updateCard(id: Int, body: JsonObject): ApiResult<CardDto> = call { api.updateCard(id, body) }

    suspend fun archiveCard(id: Int, archived: Boolean): ApiResult<CardDto> =
        call { api.archiveCard(id, buildJsonObject { put("archived", archived) }) }

    suspend fun deleteCard(id: Int): ApiResult<JsonObject> = call { api.deleteCard(id) }

    suspend fun reportByCategory(from: String, to: String, kind: String, source: String): ApiResult<Pair<List<CategoryReportRowDto>, ReportWindowDto?>> =
        withMisc { api.reportByCategory(from, to, kind, source) }

    suspend fun reportByMonth(months: Int, source: String): ApiResult<Pair<List<MonthReportRowDto>, MonthReportMiscDto?>> =
        withMisc { api.reportByMonth(months, source) }

    suspend fun reportByTag(from: String, to: String, kind: String, source: String): ApiResult<Pair<List<TagReportRowDto>, ReportWindowDto?>> =
        withMisc { api.reportByTag(from, to, kind, source) }

    suspend fun reportByMethod(from: String, to: String, kind: String): ApiResult<Pair<List<MethodReportRowDto>, ReportWindowDto?>> =
        withMisc { api.reportByMethod(from, to, kind) }

    private suspend fun <B, M> withMisc(
        block: suspend () -> Response<MiscEnvelope<List<B>, M>>
    ): ApiResult<Pair<List<B>, M?>> = try {
        val response = block()
        val envelope = response.body()

        when {
            response.code() == 401 -> ApiResult.Failure("Sessão expirada. Entre novamente.", unauthorized = true)
            response.isSuccessful && envelope?.success == true ->
                ApiResult.Ok((envelope.body ?: emptyList()) to envelope.misc)
            else -> ApiResult.Failure(
                listOfNotNull(envelope?.message, envelope?.error).joinToString(" ").trim()
                    .ifBlank { "Não foi possível carregar o relatório." }
            )
        }
    } catch (e: IOException) {
        ApiResult.Failure("Sem conexão com o servidor. Confira o endereço em Ajustes.")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Erro inesperado.")
    }

    suspend fun products(search: String? = null, categoryId: Int? = null, page: Int = 1): ApiResult<ProductPage> = try {
        val response = api.products(
            search = search?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            page = page
        )
        val envelope = response.body()

        when {
            response.code() == 401 -> ApiResult.Failure("Sessão expirada. Entre novamente.", unauthorized = true)
            response.isSuccessful && envelope?.success == true ->
                ApiResult.Ok(ProductPage(envelope.body, envelope.pagination))
            else -> ApiResult.Failure(
                listOfNotNull(envelope?.message, envelope?.error).joinToString(" ").trim()
                    .ifBlank { "Não foi possível carregar os produtos." }
            )
        }
    } catch (e: IOException) {
        ApiResult.Failure("Sem conexão com o servidor. Confira o endereço em Ajustes.")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Erro inesperado.")
    }

    suspend fun searchProducts(search: String, limit: Int = 12): ApiResult<List<ProductDto>> =
        call { api.searchProducts(search.takeIf { it.isNotBlank() }, limit) }

    suspend fun product(id: Int): ApiResult<ProductDto> = call { api.product(id) }

    suspend fun priceHistory(id: Int): ApiResult<List<PriceHistoryDto>> = call { api.priceHistory(id) }

    suspend fun createProduct(body: JsonObject): ApiResult<ProductDto> = call { api.createProduct(body) }

    suspend fun updateProduct(id: Int, body: JsonObject): ApiResult<ProductDto> =
        call { api.updateProduct(id, body) }

    suspend fun archiveProduct(id: Int, archived: Boolean): ApiResult<ProductDto> =
        call { api.archiveProduct(id, buildJsonObject { put("archived", archived) }) }

    suspend fun deleteProduct(id: Int): ApiResult<JsonObject> = call { api.deleteProduct(id) }

    suspend fun productCategories(): ApiResult<List<ProductCategoryDto>> = call { api.productCategories() }

    suspend fun createProductCategory(body: JsonObject): ApiResult<ProductCategoryDto> =
        call { api.createProductCategory(body) }

    suspend fun deleteProductCategory(id: Int): ApiResult<JsonObject> = call { api.deleteProductCategory(id) }

    suspend fun replaceEntryItems(id: Int, body: JsonObject): ApiResult<EntryDto> =
        call { api.replaceEntryItems(id, body) }

    suspend fun installmentPlans(onlyOpen: Boolean = true): ApiResult<List<InstallmentPlanDto>> =
        call { api.installmentPlans(if (onlyOpen) 1 else 0) }

    suspend fun installmentPlan(id: Int): ApiResult<InstallmentPlanDto> = call { api.installmentPlan(id) }

    suspend fun createInstallmentPlan(body: JsonObject): ApiResult<InstallmentPlanDto> =
        call { api.createInstallmentPlan(body) }

    suspend fun updateInstallmentPlan(id: Int, body: JsonObject): ApiResult<InstallmentPlanDto> =
        call { api.updateInstallmentPlan(id, body) }

    suspend fun deleteInstallmentPlan(id: Int): ApiResult<JsonObject> = call { api.deleteInstallmentPlan(id) }

    suspend fun recurrences(onlyActive: Boolean = true): ApiResult<List<RecurrenceDto>> =
        call { api.recurrences(if (onlyActive) 1 else 0) }

    suspend fun upcomingOccurrences(days: Int = 60): ApiResult<List<UpcomingOccurrenceDto>> =
        call { api.upcomingOccurrences(days) }

    suspend fun createRecurrence(body: JsonObject): ApiResult<RecurrenceDto> = call { api.createRecurrence(body) }

    suspend fun updateRecurrence(id: Int, body: JsonObject): ApiResult<RecurrenceDto> =
        call { api.updateRecurrence(id, body) }

    suspend fun pauseRecurrence(id: Int, active: Boolean): ApiResult<RecurrenceDto> =
        call { api.pauseRecurrence(id, buildJsonObject { put("active", active) }) }

    suspend fun materializeRecurrence(id: Int): ApiResult<JsonObject> =
        call { api.materializeRecurrence(id, buildJsonObject { }) }

    suspend fun deleteRecurrence(id: Int, keepEntries: Boolean): ApiResult<JsonObject> =
        call { api.deleteRecurrence(id, buildJsonObject { put("keepEntries", keepEntries) }) }

    suspend fun statements(cardId: Int, months: Int = 6): ApiResult<List<StatementDto>> =
        call { api.statements(cardId, months) }

    suspend fun currentStatement(cardId: Int): ApiResult<StatementDto> = call { api.currentStatement(cardId) }

    suspend fun statement(cardId: Int, reference: String): ApiResult<StatementDto> =
        call { api.statement(cardId, reference) }

    suspend fun payStatement(id: Int, body: JsonObject): ApiResult<StatementDto> =
        call { api.payStatement(id, body) }

    suspend fun unpayStatement(id: Int): ApiResult<StatementDto> =
        call { api.unpayStatement(id, buildJsonObject { }) }

    suspend fun categories(includeArchived: Boolean = false, kind: String? = null): ApiResult<List<CategoryDto>> =
        call { api.categories(if (includeArchived) 0 else null, kind) }

    suspend fun createCategory(body: JsonObject): ApiResult<CategoryDto> = call { api.createCategory(body) }

    suspend fun updateCategory(id: Int, body: JsonObject): ApiResult<CategoryDto> =
        call { api.updateCategory(id, body) }

    suspend fun archiveCategory(id: Int, active: Boolean): ApiResult<CategoryDto> =
        call { api.archiveCategory(id, buildJsonObject { put("active", active) }) }

    suspend fun reorderCategories(ids: List<Int>): ApiResult<JsonObject> =
        call { api.reorderCategories(orderedIds(ids)) }

    suspend fun deleteCategory(id: Int, reassignTo: Int?): ApiResult<JsonObject> =
        call {
            api.deleteCategory(id, buildJsonObject {
                if (reassignTo != null) put("reassignTo", reassignTo)
            })
        }

    suspend fun tags(search: String? = null): ApiResult<List<TagDto>> =
        call { api.tags(search?.takeIf { it.isNotBlank() }, 1) }

    suspend fun createTag(body: JsonObject): ApiResult<TagDto> = call { api.createTag(body) }

    suspend fun updateTag(id: Int, body: JsonObject): ApiResult<TagDto> = call { api.updateTag(id, body) }

    suspend fun mergeTag(id: Int, intoId: Int): ApiResult<JsonObject> =
        call { api.mergeTag(id, buildJsonObject { put("intoId", intoId) }) }

    suspend fun deleteTag(id: Int): ApiResult<JsonObject> = call { api.deleteTag(id) }

    private fun orderedIds(ids: List<Int>): JsonObject = buildJsonObject {
        put("orderedIds", kotlinx.serialization.json.JsonArray(ids.map {
            kotlinx.serialization.json.JsonPrimitive(it)
        }))
    }

    private suspend fun <T> call(block: suspend () -> Response<Envelope<T>>): ApiResult<T> = try {
        val response = block()

        if (response.code() == 401) {
            ApiResult.Failure("Sessão expirada. Entre novamente.", unauthorized = true)
        } else {
            val payload = response.body()?.takeIf { it.success }?.body

            if (payload != null) {
                ApiResult.Ok(payload)
            } else {
                ApiResult.Failure(failureMessage(response))
            }
        }
    } catch (e: IOException) {
        ApiResult.Failure("Sem conexão com o servidor. Confira o endereço em Ajustes.")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Erro inesperado.")
    }

    private fun pagedFailure(response: Response<PagedEnvelope<EntryDto>>): String {
        val envelope = response.body()
        val stated = listOfNotNull(envelope?.message, envelope?.error).joinToString(" ").trim()

        if (stated.isNotBlank()) {
            return stated
        }

        return failureMessage(response)
    }

    private fun failureMessage(response: Response<*>): String {
        val stated = (response.body() as? Envelope<*>)?.let { envelope ->
            listOfNotNull(envelope.message, envelope.error).joinToString(" ")
        }

        if (!stated.isNullOrBlank()) {
            return stated
        }

        val raw = response.errorBody()?.string().orEmpty()

        val decoded = runCatching {
            NetworkModule.json.decodeFromString(ErrorEnvelope.serializer(), raw)
        }.getOrNull()

        val message = listOfNotNull(decoded?.message, decoded?.error).joinToString(" ").trim()

        return message.ifBlank { "O servidor respondeu ${response.code()}." }
    }

}

data class EntryFilter(
    val from: String? = null,
    val to: String? = null,
    val kind: String? = null,
    val categoryId: Int? = null,
    val cardId: Int? = null,
    val accountId: Int? = null,
    val personId: Int? = null,
    val tag: String? = null,
    val paid: Boolean? = null,
    val search: String? = null,
    val perPage: Int = 40
)

data class EntryPage(val entries: List<EntryDto>, val pagination: PageInfo?)

data class ProductPage(val products: List<ProductDto>, val pagination: PageInfo?)

sealed interface CodeCheck {
    data object Accepted : CodeCheck
    data class Rejected(val message: String, val attemptsLeft: Int?) : CodeCheck
    data object Blocked : CodeCheck
    data class Failed(val message: String) : CodeCheck
}

@Serializable
private data class ErrorEnvelope(val message: String? = null, val error: String? = null)

fun jsonBody(build: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject(build)

package com.archieapps.finances.data.net

import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.Envelope
import com.archieapps.finances.data.dto.EntryDraftDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.LoginDto
import com.archieapps.finances.data.dto.OverviewDto
import com.archieapps.finances.data.dto.PagedEnvelope
import com.archieapps.finances.data.dto.PersonBalanceDto
import com.archieapps.finances.data.dto.PersonDto
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
import com.archieapps.finances.data.dto.InstallmentPlanDto
import com.archieapps.finances.data.dto.RecurrenceDto
import com.archieapps.finances.data.dto.StatementDto
import com.archieapps.finances.data.dto.UpcomingOccurrenceDto
import com.archieapps.finances.data.dto.TagDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FinancesApi {
    @POST("api/user/test")
    suspend fun testCode(@Body body: JsonObject): Response<Envelope<JsonObject>>

    @POST("api/auth/login")
    suspend fun login(@Body body: JsonObject): Response<Envelope<LoginDto>>

    @GET("api/finance/overview")
    suspend fun overview(@Query("month") month: String? = null): Response<Envelope<OverviewDto>>

    @GET("api/finance/entries")
    suspend fun entries(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("kind") kind: String? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("cardId") cardId: Int? = null,
        @Query("accountId") accountId: Int? = null,
        @Query("personId") personId: Int? = null,
        @Query("tag") tag: String? = null,
        @Query("paid") paid: Int? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 40
    ): Response<PagedEnvelope<EntryDto>>

    @GET("api/finance/entries/{id}")
    suspend fun entry(@Path("id") id: Int): Response<Envelope<EntryDto>>

    @POST("api/finance/entries")
    suspend fun createEntry(@Body body: JsonObject): Response<Envelope<EntryDto>>

    @POST("api/finance/entries/quick")
    suspend fun quickEntry(@Body body: JsonObject): Response<Envelope<EntryDto>>

    @PUT("api/finance/entries/{id}")
    suspend fun updateEntry(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<EntryDto>>

    @PATCH("api/finance/entries/{id}/pay")
    suspend fun payEntry(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<EntryDto>>

    @PATCH("api/finance/entries/{id}/unpay")
    suspend fun unpayEntry(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<EntryDto>>

    @DELETE("api/finance/entries/{id}")
    suspend fun deleteEntry(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @POST("api/finance/transfers")
    suspend fun createTransfer(@Body body: JsonObject): Response<Envelope<EntryDto>>

    @POST("api/finance/ai/draft")
    suspend fun parseEntryDraft(@Body body: JsonObject): Response<Envelope<EntryDraftDto>>

    @POST("api/finance/ai/commit")
    suspend fun commitEntryDraft(@Body body: JsonObject): Response<Envelope<JsonObject>>

    @GET("api/finance/people/balances")
    suspend fun personBalances(@Query("settled") settled: Int? = null): Response<Envelope<List<PersonBalanceDto>>>

    @GET("api/people/all")
    suspend fun people(): Response<Envelope<List<PersonDto>>>

    @GET("api/finance/people/{id}/entries")
    suspend fun personEntries(@Path("id") id: Int): Response<Envelope<List<EntryDto>>>

    @POST("api/finance/people/{id}/settle")
    suspend fun settlePerson(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<EntryDto>>

    @GET("api/finance/accounts")
    suspend fun accounts(@Query("archived") archived: Int? = null): Response<Envelope<List<AccountDto>>>

    @POST("api/finance/accounts")
    suspend fun createAccount(@Body body: JsonObject): Response<Envelope<AccountDto>>

    @PUT("api/finance/accounts/{id}")
    suspend fun updateAccount(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<AccountDto>>

    @PATCH("api/finance/accounts/{id}/archive")
    suspend fun archiveAccount(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<AccountDto>>

    @POST("api/finance/accounts/reorder")
    suspend fun reorderAccounts(@Body body: JsonObject): Response<Envelope<JsonObject>>

    @DELETE("api/finance/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @GET("api/finance/cards")
    suspend fun cards(@Query("archived") archived: Int? = null): Response<Envelope<List<CardDto>>>

    @POST("api/finance/cards")
    suspend fun createCard(@Body body: JsonObject): Response<Envelope<CardDto>>

    @PUT("api/finance/cards/{id}")
    suspend fun updateCard(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<CardDto>>

    @PATCH("api/finance/cards/{id}/archive")
    suspend fun archiveCard(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<CardDto>>

    @DELETE("api/finance/cards/{id}")
    suspend fun deleteCard(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @GET("api/finance/reports/by-category")
    suspend fun reportByCategory(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("kind") kind: String? = null,
        @Query("source") source: String? = null
    ): Response<MiscEnvelope<List<CategoryReportRowDto>, ReportWindowDto>>

    @GET("api/finance/reports/by-month")
    suspend fun reportByMonth(
        @Query("months") months: Int? = null,
        @Query("source") source: String? = null
    ): Response<MiscEnvelope<List<MonthReportRowDto>, MonthReportMiscDto>>

    @GET("api/finance/reports/by-tag")
    suspend fun reportByTag(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("kind") kind: String? = null,
        @Query("source") source: String? = null
    ): Response<MiscEnvelope<List<TagReportRowDto>, ReportWindowDto>>

    @GET("api/finance/reports/by-method")
    suspend fun reportByMethod(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("kind") kind: String? = null
    ): Response<MiscEnvelope<List<MethodReportRowDto>, ReportWindowDto>>

    @GET("api/products")
    suspend fun products(
        @Query("search") search: String? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("tag") tag: String? = null,
        @Query("archived") archived: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 40
    ): Response<PagedEnvelope<ProductDto>>

    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<Envelope<List<ProductDto>>>

    @GET("api/products/{id}")
    suspend fun product(@Path("id") id: Int): Response<Envelope<ProductDto>>

    @GET("api/products/{id}/price-history")
    suspend fun priceHistory(@Path("id") id: Int): Response<Envelope<List<PriceHistoryDto>>>

    @POST("api/products")
    suspend fun createProduct(@Body body: JsonObject): Response<Envelope<ProductDto>>

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<ProductDto>>

    @PATCH("api/products/{id}/archive")
    suspend fun archiveProduct(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<ProductDto>>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @GET("api/product-categories")
    suspend fun productCategories(
        @Query("withCounts") withCounts: Int? = 1
    ): Response<Envelope<List<ProductCategoryDto>>>

    @POST("api/product-categories")
    suspend fun createProductCategory(@Body body: JsonObject): Response<Envelope<ProductCategoryDto>>

    @PUT("api/product-categories/{id}")
    suspend fun updateProductCategory(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<ProductCategoryDto>>

    @DELETE("api/product-categories/{id}")
    suspend fun deleteProductCategory(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @PUT("api/finance/entries/{id}/items")
    suspend fun replaceEntryItems(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<EntryDto>>

    @GET("api/finance/installment-plans")
    suspend fun installmentPlans(@Query("open") open: Int? = null): Response<Envelope<List<InstallmentPlanDto>>>

    @GET("api/finance/installment-plans/{id}")
    suspend fun installmentPlan(@Path("id") id: Int): Response<Envelope<InstallmentPlanDto>>

    @POST("api/finance/installment-plans")
    suspend fun createInstallmentPlan(@Body body: JsonObject): Response<Envelope<InstallmentPlanDto>>

    @PUT("api/finance/installment-plans/{id}")
    suspend fun updateInstallmentPlan(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<InstallmentPlanDto>>

    @DELETE("api/finance/installment-plans/{id}")
    suspend fun deleteInstallmentPlan(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @GET("api/finance/recurrences")
    suspend fun recurrences(@Query("active") active: Int? = null): Response<Envelope<List<RecurrenceDto>>>

    @GET("api/finance/recurrences/upcoming")
    suspend fun upcomingOccurrences(
        @Query("days") days: Int? = null,
        @Query("from") from: String? = null
    ): Response<Envelope<List<UpcomingOccurrenceDto>>>

    @POST("api/finance/recurrences")
    suspend fun createRecurrence(@Body body: JsonObject): Response<Envelope<RecurrenceDto>>

    @PUT("api/finance/recurrences/{id}")
    suspend fun updateRecurrence(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<RecurrenceDto>>

    @PATCH("api/finance/recurrences/{id}/pause")
    suspend fun pauseRecurrence(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<RecurrenceDto>>

    @POST("api/finance/recurrences/{id}/materialize")
    suspend fun materializeRecurrence(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<JsonObject>>

    @HTTP(method = "DELETE", path = "api/finance/recurrences/{id}", hasBody = true)
    suspend fun deleteRecurrence(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<JsonObject>>

    @GET("api/finance/cards/{id}/statements")
    suspend fun statements(
        @Path("id") cardId: Int,
        @Query("months") months: Int? = null
    ): Response<Envelope<List<StatementDto>>>

    @GET("api/finance/cards/{id}/statements/current")
    suspend fun currentStatement(@Path("id") cardId: Int): Response<Envelope<StatementDto>>

    @GET("api/finance/cards/{id}/statements/{reference}")
    suspend fun statement(
        @Path("id") cardId: Int,
        @Path("reference") reference: String
    ): Response<Envelope<StatementDto>>

    @POST("api/finance/statements/{id}/pay")
    suspend fun payStatement(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<StatementDto>>

    @POST("api/finance/statements/{id}/unpay")
    suspend fun unpayStatement(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<StatementDto>>

    @GET("api/finance/categories")
    suspend fun categories(
        @Query("active") active: Int? = null,
        @Query("kind") kind: String? = null
    ): Response<Envelope<List<CategoryDto>>>

    @POST("api/finance/categories")
    suspend fun createCategory(@Body body: JsonObject): Response<Envelope<CategoryDto>>

    @PUT("api/finance/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<CategoryDto>>

    @PATCH("api/finance/categories/{id}/archive")
    suspend fun archiveCategory(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<CategoryDto>>

    @POST("api/finance/categories/reorder")
    suspend fun reorderCategories(@Body body: JsonObject): Response<Envelope<JsonObject>>

    @HTTP(method = "DELETE", path = "api/finance/categories/{id}", hasBody = true)
    suspend fun deleteCategory(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<JsonObject>>

    @GET("api/finance/tags")
    suspend fun tags(
        @Query("search") search: String? = null,
        @Query("withCounts") withCounts: Int? = 1
    ): Response<Envelope<List<TagDto>>>

    @POST("api/finance/tags")
    suspend fun createTag(@Body body: JsonObject): Response<Envelope<TagDto>>

    @PUT("api/finance/tags/{id}")
    suspend fun updateTag(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<TagDto>>

    @POST("api/finance/tags/{id}/merge")
    suspend fun mergeTag(@Path("id") id: Int, @Body body: JsonObject): Response<Envelope<JsonObject>>

    @DELETE("api/finance/tags/{id}")
    suspend fun deleteTag(@Path("id") id: Int): Response<Envelope<JsonObject>>
}

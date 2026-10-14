package com.archieapps.finances.data.net

import com.archieapps.finances.BuildConfig

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("finances_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val lastKindKey = stringPreferencesKey("last_kind")
    private val lastCategoryKey = intPreferencesKey("last_category")
    private val lastAccountKey = intPreferencesKey("last_account")
    private val lastCardKey = intPreferencesKey("last_card")
    private val lastPaymentMethodKey = stringPreferencesKey("last_payment_method")

    val recent: Flow<RecentChoices> = context.dataStore.data.map { prefs ->
        RecentChoices(
            kind = prefs[lastKindKey] ?: "expense",
            categoryId = prefs[lastCategoryKey],
            accountId = prefs[lastAccountKey],
            cardId = prefs[lastCardKey],
            paymentMethod = prefs[lastPaymentMethodKey]
        )
    }

    suspend fun rememberChoices(choices: RecentChoices) {
        context.dataStore.edit { prefs ->
            prefs[lastKindKey] = choices.kind
            choices.categoryId?.let { prefs[lastCategoryKey] = it }
            choices.accountId?.let { prefs[lastAccountKey] = it }
            choices.cardId?.let { prefs[lastCardKey] = it }
            choices.paymentMethod?.let { prefs[lastPaymentMethodKey] = it }
        }
    }

    val session: Flow<Session> = context.dataStore.data.map { prefs ->
        Session(
            token = prefs[tokenKey].orEmpty(),
            baseUrl = prefs[baseUrlKey] ?: DEFAULT_BASE_URL
        )
    }

    suspend fun save(token: String, baseUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[baseUrlKey] = baseUrl
        }
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        context.dataStore.edit { prefs -> prefs[baseUrlKey] = baseUrl }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs -> prefs[tokenKey] = "" }
    }

    companion object {
        val DEFAULT_BASE_URL: String = BuildConfig.DEFAULT_BASE_URL
    }
}

data class Session(val token: String, val baseUrl: String) {
    val authenticated: Boolean get() = token.isNotBlank()
}

data class RecentChoices(
    val kind: String = "expense",
    val categoryId: Int? = null,
    val accountId: Int? = null,
    val cardId: Int? = null,
    val paymentMethod: String? = null
)

package com.archieapps.finances.ui.screen.accounts

import com.archieapps.finances.data.dto.AccountDto

internal fun AccountDto.currentCents(): Long = balanceCents ?: initialBalanceCents

internal data class AccountGroup(val institution: String?, val accounts: List<AccountDto>) {
    val totalCents: Long get() = accounts.filterNot { it.archived }.sumOf { it.currentCents() }
}

/** Ações e cripto oscilam e ficam fora do patrimônio. */
internal fun AccountDto.isVariable(): Boolean = !separated && (investmentType == "stock" || investmentType == "crypto")

/** Contas e renda fixa do usuário: as únicas que contam no patrimônio. */
internal fun ownAccounts(accounts: List<AccountDto>): List<AccountDto> =
    accounts.filterNot { it.separated || it.isVariable() }

internal fun variableAccounts(accounts: List<AccountDto>): List<AccountDto> = accounts.filter { it.isVariable() }

internal fun variableCents(accounts: List<AccountDto>): Long =
    variableAccounts(accounts).filterNot { it.archived }.sumOf { it.currentCents() }

/** Dinheiro que está nas contas mas não é do usuário. */
internal fun separatedAccounts(accounts: List<AccountDto>): List<AccountDto> = accounts.filter { it.separated }

internal fun patrimonyCents(accounts: List<AccountDto>): Long =
    ownAccounts(accounts).filterNot { it.archived }.sumOf { it.currentCents() }

internal fun separatedCents(accounts: List<AccountDto>): Long =
    separatedAccounts(accounts).filterNot { it.archived }.sumOf { it.currentCents() }

internal fun groupByInstitution(accounts: List<AccountDto>): List<AccountGroup> {
    val groups = accounts.groupBy { it.institution?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }
    val named = groups.filterKeys { it != null }
        .map { (_, rows) -> AccountGroup(rows.first().institution, rows) }
        .sortedByDescending { it.totalCents }
    val loose = groups[null]?.let { AccountGroup(null, it) }

    return named + listOfNotNull(loose)
}

package com.archieapps.finances.ui.screen.wealth

import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.OverviewDto

/** Como o patrimônio e o dinheiro livre da home são formados, para explicar na tela. */
data class WealthBreakdown(
    val totalCents: Long,
    val accounts: List<AccountDto>,
    val fixedIncome: List<AccountDto>,
    val committedCents: Long,
    val lockedCents: Long,
    val freeCents: Long,
    val separatedCents: Long,
    val hasVariable: Boolean
) {
    val accountsCents: Long get() = accounts.sumOf { it.cents() }

    val fixedIncomeCents: Long get() = fixedIncome.sumOf { it.cents() }

    /** O que a lista mostra somado: deve bater com o patrimônio que o servidor devolveu. */
    val listedCents: Long get() = accountsCents + fixedIncomeCents

    val lockedAccounts: List<AccountDto> get() = fixedIncome.filter { it.locked }

    companion object {
        private fun AccountDto.cents(): Long = balanceCents ?: initialBalanceCents

        fun from(data: OverviewDto): WealthBreakdown {
            val own = data.accounts.filter { !it.archived && !it.separated && it.investmentType != "stock" && it.investmentType != "crypto" }

            return WealthBreakdown(
                totalCents = data.totalCents,
                accounts = own.filter { it.kind != "investment" },
                fixedIncome = own.filter { it.kind == "investment" },
                committedCents = data.committedCents,
                lockedCents = data.notFree.lockedCents,
                freeCents = data.freeCents,
                separatedCents = data.separated.totalCents,
                hasVariable = data.variable.accountIds.isNotEmpty()
            )
        }
    }
}

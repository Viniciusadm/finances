package com.archieapps.finances

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.archieapps.finances.data.net.NetworkModule
import com.archieapps.finances.data.net.SessionHolder
import com.archieapps.finances.data.net.SessionStore
import com.archieapps.finances.core.session.BiometricGate
import com.archieapps.finances.core.session.CodeGate
import com.archieapps.finances.core.session.CodeVault
import com.archieapps.finances.data.repo.FinanceRepository

class Container(application: Application) {
    val sessionStore = SessionStore(application)
    val sessionHolder = SessionHolder()
    val repository = FinanceRepository(NetworkModule.api(sessionHolder))
    val codeVault = CodeVault(application)
    val codeGate = CodeGate(codeVault, repository, BiometricGate(application))

    init {
        sessionHolder.code = { codeVault.currentCode() }
    }
}

class FinancesApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
    }
}

val LocalContainer = staticCompositionLocalOf<Container> {
    error("Container não fornecido")
}

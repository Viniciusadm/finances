package com.archieapps.finances

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.archieapps.finances.ui.nav.FinancesRoot
import com.archieapps.finances.ui.theme.FinancesTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as FinancesApp).container

        setContent {
            CompositionLocalProvider(LocalContainer provides container) {
                FinancesTheme {
                    FinancesRoot()
                }
            }
        }
    }
}

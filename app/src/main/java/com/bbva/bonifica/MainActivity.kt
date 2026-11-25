package com.bbva.bonifica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbva.bonifica.di.rememberAppContainer
import com.bbva.bonifica.ui.components.LocalRole
import com.bbva.bonifica.ui.navigation.AppNavHost
import com.bbva.bonifica.ui.theme.BonificaTheme
import com.bbva.bonifica.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = rememberAppContainer()

            // ThemeViewModel: wired via AppContainer, persisted in DataStore.
            val themeViewModel = remember(container) {
                ThemeViewModel(container.themePreferences)
            }
            val isDark by themeViewModel.isDark.collectAsState()

            BonificaTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                // §11.2: el rol sale solo del claim `cognito:groups`. Se relee en
                // cada cambio de pila porque entrar o cerrar sesión siempre navega
                // a otro destino; así `LocalRole` refleja la sesión activa y
                // `RoleGuard` queda como doble protección junto al 403 de la API.
                val backStackEntry by navController.currentBackStackEntryAsState()
                val role = remember(backStackEntry) {
                    container.authRepository.currentSession()?.role
                }

                CompositionLocalProvider(LocalRole provides role) {
                    AppNavHost(
                        navController = navController,
                        isDark = isDark,
                        onToggleTheme = themeViewModel::toggle
                    )
                }
            }
        }
    }
}

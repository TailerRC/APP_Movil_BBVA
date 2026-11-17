package com.bbva.bonifica.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bbva.bonifica.BonificaApp

/**
 * Factory manual sin Hilt (§4.5): cada ViewModel pide sus repositorios a
 * [AppContainer] con una `ViewModelProvider.Factory`.
 *
 * Uso: `val vm: LoginViewModel = viewModel(factory = rememberAppFactory { LoginViewModel(it.authRepository) })`
 */
@Composable
fun <VM : ViewModel> rememberAppFactory(create: (AppContainer) -> VM): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context, create) {
        val container = (context.applicationContext as BonificaApp).container
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
        }
    }
}

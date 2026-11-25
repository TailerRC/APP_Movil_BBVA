package com.bbva.bonifica.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que expone y gestiona la preferencia de modo oscuro.
 * Se instancia en MainActivity vía AppContainer.
 */
class ThemeViewModel(private val prefs: ThemePreferences) : ViewModel() {

    val isDark: StateFlow<Boolean> = prefs.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun toggle() {
        viewModelScope.launch {
            prefs.setDarkMode(!isDark.value)
        }
    }
}

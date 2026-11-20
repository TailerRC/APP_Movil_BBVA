package com.bbva.bonifica.ui.promociones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.PromotionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel de Promociones (Módulo 4, rutas 6 y 7).
 *
 * Carga políticas y campañas juntas; las pestañas y los filtros son estado local
 * de pantalla. Cambiar el estado de campaña recarga solo esa lista y, si el usuario
 * ya eligió otro estado, la respuesta tardía se descarta.
 */
class PromocionesViewModel(
    private val promotionRepository: PromotionRepository,
    /** Fecha de "hoy" para ocultar las vencidas (§12); se inyecta para tests. */
    private val today: LocalDate = LocalDate.now()
) : ViewModel() {

    private val _state = MutableStateFlow<PromocionesUiState>(UiState.Loading)
    val state: StateFlow<PromocionesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val policies = when (val result = promotionRepository.getActivePolicies()) {
                is AppResult.Failure -> {
                    _state.value = UiState.Error(result.error)
                    return@launch
                }
                is AppResult.Success -> result.data
            }
            val campaigns = when (val result = promotionRepository.getCampaigns(DEFAULT_CAMPAIGN_STATUS)) {
                is AppResult.Failure -> {
                    _state.value = UiState.Error(result.error)
                    return@launch
                }
                is AppResult.Success -> result.data
            }
            val data = PromocionesData(
                today = today,
                policies = policies,
                campaigns = campaigns
            )
            _state.value = if (data.policies.isEmpty()) {
                UiState.Empty
            } else {
                UiState.Content(data)
            }
        }
    }

    fun selectTab(tab: PromocionesTab) = updateContent { it.copy(tab = tab) }

    fun selectCategory(category: String?) = updateContent { it.copy(categoryFilter = category) }

    fun selectCurrency(currency: String?) = updateContent { it.copy(currencyFilter = currency) }

    fun selectChannel(channel: String?) = updateContent { it.copy(channelFilter = channel) }

    fun selectCampaignStatus(status: String) {
        updateContent {
            it.copy(campaignStatus = status, campaignsLoading = true, campaignsError = null)
        }
        viewModelScope.launch {
            when (val result = promotionRepository.getCampaigns(status)) {
                is AppResult.Success -> updateContent { data ->
                    // Ignora la respuesta si el usuario ya cambió a otro estado.
                    if (data.campaignStatus == status) {
                        data.copy(campaigns = result.data, campaignsLoading = false)
                    } else data
                }
                is AppResult.Failure -> updateContent { data ->
                    if (data.campaignStatus == status) {
                        data.copy(campaignsLoading = false, campaignsError = result.error)
                    } else data
                }
            }
        }
    }

    fun retryCampaigns() {
        val current = (_state.value as? UiState.Content)?.data ?: return
        selectCampaignStatus(current.campaignStatus)
    }

    private fun updateContent(transform: (PromocionesData) -> PromocionesData) {
        _state.update { state ->
            if (state is UiState.Content) UiState.Content(transform(state.data)) else state
        }
    }

    companion object {
        const val DEFAULT_CAMPAIGN_STATUS = "ACTIVE"
    }
}

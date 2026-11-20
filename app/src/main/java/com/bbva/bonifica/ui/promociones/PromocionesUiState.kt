package com.bbva.bonifica.ui.promociones

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.Campaign
import com.bbva.bonifica.domain.model.Policy
import java.time.LocalDate

/** Pestañas de Promociones (Módulo 4, §12). */
enum class PromocionesTab { POLITICAS, CAMPANAS }

/**
 * Estado de la pantalla de Promociones (Módulo 4, rutas 6 y 7).
 *
 * `visiblePolicies` aplica las reglas de la pestaña Políticas de §12: se ocultan
 * las vencidas (`validTo` < hoy) y se respetan los filtros de categoría, moneda
 * y canal. `today` viaja en el estado para que la regla sea testeable.
 */
data class PromocionesData(
    val today: LocalDate,
    val policies: List<Policy>,
    val campaigns: List<Campaign>,
    val tab: PromocionesTab = PromocionesTab.POLITICAS,
    /** Estado de campaña elegido; `ACTIVE` por defecto (§12). */
    val campaignStatus: String = "ACTIVE",
    val categoryFilter: String? = null,
    val currencyFilter: String? = null,
    val channelFilter: String? = null,
    /** Recarga de campañas al cambiar de estado: no bloquea la pestaña completa. */
    val campaignsLoading: Boolean = false,
    val campaignsError: AppError? = null
) {

    /** Políticas visibles: vigentes y según los filtros de categoría, moneda y canal. */
    val visiblePolicies: List<Policy>
        get() = policies.filter { policy ->
            isNotExpired(policy, today) &&
                (categoryFilter == null || appliesToCategory(policy, categoryFilter)) &&
                (currencyFilter == null || policy.currency == currencyFilter) &&
                (channelFilter == null || policy.channel == "ALL" || policy.channel == channelFilter)
        }
}

/** Se ocultan las vencidas: `validTo` anterior a hoy (§12). */
private fun isNotExpired(policy: Policy, today: LocalDate): Boolean = runCatching {
    !today.isAfter(LocalDate.parse(policy.validTo))
}.getOrDefault(false)

/**
 * Filtro de categoría: la política aplica si apunta a la categoría elegida o si su
 * alcance es `ALL` (aplica a todas). Las de alcance `PRODUCT` solo se ven en "Todas".
 */
private fun appliesToCategory(policy: Policy, category: String): Boolean =
    policy.scopeReference == category || policy.scope == "ALL"

typealias PromocionesUiState = UiState<PromocionesData>

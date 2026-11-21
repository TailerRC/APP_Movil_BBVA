package com.bbva.bonifica.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.NewPolicy
import com.bbva.bonifica.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Formulario de `Policy/Register` (Módulo 7, ruta 5).
 *
 * Validación local antes de llamar a la API (§12): formato `POL-###`, valor > 0
 * (y ≤ 100 si es porcentaje), referencia obligatoria salvo alcance `ALL`,
 * montos y fechas parseables, y vigencia coherente. Un 409 se muestra como
 * aviso general con `AppError.toMessage()` ("La política ya existe").
 */
class RegistrarPoliticaViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrarPoliticaUiState())
    val state: StateFlow<RegistrarPoliticaUiState> = _state.asStateFlow()

    /** Cambia un campo de texto y limpia su error (§12). */
    fun onChange(campo: CampoPolitica, value: String) {
        _state.update { current ->
            val updated = when (campo) {
                CampoPolitica.POLICY_ID -> current.copy(policyId = value)
                CampoPolitica.NOMBRE -> current.copy(name = value)
                CampoPolitica.VALOR -> current.copy(discountValue = value)
                CampoPolitica.REFERENCIA -> current.copy(scopeReference = value)
                CampoPolitica.MINIMO -> current.copy(minimumAmount = value)
                CampoPolitica.DESDE -> current.copy(validFrom = value)
                CampoPolitica.HASTA -> current.copy(validTo = value)
            }
            updated.copy(errors = updated.errors - campo, generalError = null)
        }
    }

    fun selectTipo(value: String) {
        // El límite de 100% solo aplica a PERCENTAGE: al cambiar el tipo se
        // revalida el valor ya escrito (el error se limpia en submit()).
        _state.update { it.copy(discountType = value, generalError = null) }
    }

    fun selectAlcance(value: String) {
        _state.update {
            it.copy(
                scope = value,
                // Con alcance ALL la referencia no aplica: su error deja de valer.
                errors = if (value == "ALL") it.errors - CampoPolitica.REFERENCIA else it.errors,
                generalError = null
            )
        }
    }

    fun selectCanal(value: String) {
        _state.update { it.copy(channel = value, generalError = null) }
    }

    fun selectMoneda(value: String) {
        _state.update { it.copy(currency = value, generalError = null) }
    }

    /** Valida en local y, si todo es válido, llama a la ruta 5. */
    fun submit() {
        val current = _state.value
        if (current.submitting || current.registered) return

        val errors = validar(current)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, generalError = null) }
            return
        }

        _state.update { it.copy(errors = emptyMap(), generalError = null, submitting = true) }
        viewModelScope.launch {
            val policy = NewPolicy(
                policyId = current.policyId.trim(),
                name = current.name.trim(),
                discountType = current.discountType,
                discountValue = current.discountValue.trim().toDouble(),
                scope = current.scope,
                scopeReference = if (current.scope == "ALL") "*" else current.scopeReference.trim(),
                minimumAmount = current.minimumAmount.trim().toDoubleOrNull() ?: 0.0,
                currency = current.currency,
                channel = current.channel,
                validFrom = current.validFrom.trim(),
                validTo = current.validTo.trim()
            )
            when (val result = adminRepository.registerPolicy(policy)) {
                is AppResult.Success -> _state.update {
                    it.copy(submitting = false, registered = true, generalError = null)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(submitting = false, generalError = result.error)
                }
            }
        }
    }

    /** Reglas visibles del formulario (§12); una sola fuente de verdad. */
    private fun validar(s: RegistrarPoliticaUiState): Map<CampoPolitica, ErrorCampo> {
        val errors = mutableMapOf<CampoPolitica, ErrorCampo>()

        if (s.policyId.isBlank()) {
            errors[CampoPolitica.POLICY_ID] = ErrorCampo.REQUIRED
        } else if (!POLICY_ID_REGEX.matches(s.policyId.trim())) {
            errors[CampoPolitica.POLICY_ID] = ErrorCampo.FORMAT
        }

        if (s.name.isBlank()) errors[CampoPolitica.NOMBRE] = ErrorCampo.REQUIRED

        val valor = s.discountValue.trim().toDoubleOrNull()
        when {
            s.discountValue.isBlank() -> errors[CampoPolitica.VALOR] = ErrorCampo.REQUIRED
            valor == null -> errors[CampoPolitica.VALOR] = ErrorCampo.FORMAT
            valor <= 0.0 -> errors[CampoPolitica.VALOR] = ErrorCampo.RANGE
            s.discountType == "PERCENTAGE" && valor > 100.0 -> errors[CampoPolitica.VALOR] = ErrorCampo.RANGE
        }

        if (s.scope != "ALL" && s.scopeReference.isBlank()) {
            errors[CampoPolitica.REFERENCIA] = ErrorCampo.REQUIRED
        }

        if (s.minimumAmount.isNotBlank()) {
            val minimo = s.minimumAmount.trim().toDoubleOrNull()
            when {
                minimo == null -> errors[CampoPolitica.MINIMO] = ErrorCampo.FORMAT
                minimo < 0.0 -> errors[CampoPolitica.MINIMO] = ErrorCampo.RANGE
            }
        }

        val desde = fechaValida(s.validFrom)
        val hasta = fechaValida(s.validTo)
        when {
            s.validFrom.isBlank() -> errors[CampoPolitica.DESDE] = ErrorCampo.REQUIRED
            desde == null -> errors[CampoPolitica.DESDE] = ErrorCampo.FORMAT
        }
        when {
            s.validTo.isBlank() -> errors[CampoPolitica.HASTA] = ErrorCampo.REQUIRED
            hasta == null -> errors[CampoPolitica.HASTA] = ErrorCampo.FORMAT
            desde != null && hasta.isBefore(desde) -> errors[CampoPolitica.HASTA] = ErrorCampo.ORDER
        }

        return errors
    }

    /** `AAAA-MM-DD`; null si está vacío o no se puede leer. */
    private fun fechaValida(texto: String): LocalDate? =
        if (texto.isBlank()) null else runCatching { LocalDate.parse(texto.trim()) }.getOrNull()

    private companion object {
        val POLICY_ID_REGEX = Regex("POL-\\d{3}")
    }
}

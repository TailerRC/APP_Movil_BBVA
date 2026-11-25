package com.bbva.bonifica.core

/**
 * Reglas de contraseña de Cognito (§5): mín. 8, mayúscula, minúscula y número.
 * Las comprueba la pantalla de nueva contraseña (a tiempo real) y también el
 * repositorio, como lo haría la API real.
 */
object PasswordRules {
    const val MIN_LENGTH = 8

    fun hasMinLength(password: String): Boolean = password.length >= MIN_LENGTH
    fun hasUpperCase(password: String): Boolean = password.any { it.isUpperCase() }
    fun hasLowerCase(password: String): Boolean = password.any { it.isLowerCase() }
    fun hasDigit(password: String): Boolean = password.any { it.isDigit() }

    fun isValid(password: String): Boolean =
        hasMinLength(password) && hasUpperCase(password) && hasLowerCase(password) && hasDigit(password)
}

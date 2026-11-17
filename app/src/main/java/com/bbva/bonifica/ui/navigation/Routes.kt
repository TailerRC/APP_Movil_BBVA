package com.bbva.bonifica.ui.navigation

/** Rutas de navegación (diseño §11.1). */
object Routes {
    const val SPLASH = "splash"
    // Menú temporal de desarrollo (§14): se entra con pulsación larga en el logo
    // del splash y se retira cuando todas las pantallas tengan su entrada.
    const val DEV_MENU = "dev_menu"
    const val LOGIN = "login"
    // Nota: `cognitoSession` es un texto largo; viaja en un estado compartido
    // (AccesoViewModel, con scope de Activity) y no dentro de la URL de navegación.
    const val NEW_PASSWORD = "new_password"
    const val HOME_CLIENTE = "home_cliente"
    const val CATALOGO = "catalogo"
    const val CATALOGO_CATEGORIA = "catalogo/{category}"
    const val PRODUCTO = "producto/{ref}"
    const val OPERACIONES = "operaciones"
    const val OPERACION = "operacion/{ref}"
    const val CALCULAR = "calcular?pos={pos}&prod={prod}"
    const val RESULTADO = "resultado/{dpId}"
    const val PAGOS = "pagos/{posRef}"
    const val COMPROBANTE = "comprobante/{payId}"
    const val HISTORIAL = "historial/{dpId}"
    const val PROMOCIONES = "promociones"
    const val NEGOCIO_CONSULTA = "negocio_consulta"
    const val NEGOCIO_CLIENTE = "negocio_cliente/{ref}"
    const val NEGOCIO_POLITICA = "negocio_politica"

    fun catalogoCategoria(category: String) = "catalogo/$category"
    fun producto(ref: String) = "producto/$ref"
    fun operacion(ref: String) = "operacion/$ref"
    fun calcular(pos: String? = null, prod: String? = null): String {
        val params = listOfNotNull(pos?.let { "pos=$it" }, prod?.let { "prod=$it" })
        return if (params.isEmpty()) "calcular" else "calcular?" + params.joinToString("&")
    }
    fun resultado(dpId: String) = "resultado/$dpId"
    fun pagos(posRef: String) = "pagos/$posRef"
    fun comprobante(payId: String) = "comprobante/$payId"
    fun historial(dpId: String) = "historial/$dpId"
    fun negocioCliente(ref: String) = "negocio_cliente/$ref"
}

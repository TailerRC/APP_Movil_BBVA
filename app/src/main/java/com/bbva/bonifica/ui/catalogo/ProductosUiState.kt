package com.bbva.bonifica.ui.catalogo

import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.Product

/**
 * Datos para la pantalla de productos de una categoría (§12).
 */
data class ProductosUiData(
    val category: String,
    val allProducts: List<Product>,
    val searchQuery: String = "",
    val filteredProducts: List<Product> = allProducts
)

/**
 * Estado de la pantalla de lista de productos por categoría.
 */
typealias ProductosUiState = UiState<ProductosUiData>

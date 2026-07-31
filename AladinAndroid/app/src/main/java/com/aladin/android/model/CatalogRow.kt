package com.aladin.android.model

/** Una fuente de catálogo (VizieR/Simbad) dibujada como overlay. */
data class CatalogRow(
    val id: String,
    val position: SkyPosition,
    val raDeg: Double,
    val decDeg: Double,
    val mag: Double? = null,
    val columns: Map<String, String> = emptyMap(),
)

/** Descriptor de un catálogo/encuesta para el selector. */
data class CatalogDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val rowCount: Int = 0,
)
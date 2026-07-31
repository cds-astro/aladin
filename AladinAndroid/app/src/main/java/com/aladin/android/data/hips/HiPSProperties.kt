package com.aladin.android.data.hips

/**
 * Cabecera de propiedades de un survey HiPS (fichero `properties`).
 * Solo se conservan los campos que usa el render. Ver cds/allsky/Constante.java.
 */
data class HiPSProperties(
    val baseUrl: String,
    val title: String = "",
    val order: Int = 0,
    val orderMin: Int = 3,
    val tileWidth: Int = 512,
    val tileFormat: String = "jpg", // jpg | png | fits
    val frame: String = "equatorial",
    val initialRa: Double = 0.0,
    val initialDec: Double = 0.0,
    val initialFov: Double = 60.0,
) {
    val ext: String get() = when (tileFormat.lowercase()) {
        "png" -> "png"; "fits" -> "fits"; else -> "jpg"
    }
}

object HiPSPropertiesParser {
    /** Parsea un texto `key = value` (uno por línea) del fichero properties. */
    fun parse(baseUrl: String, text: String): HiPSProperties {
        val map = HashMap<String, String>()
        text.lineSequence().forEach { line ->
            val i = line.indexOf('=')
            if (i > 0) {
                val k = line.substring(0, i).trim()
                val v = line.substring(i + 1).trim()
                if (k.isNotEmpty()) map[k] = v
            }
        }
        val fmt = (map["hips_tile_format"] ?: map["format"] ?: "jpg")
            .split(',', ';').first().trim().lowercase()
        return HiPSProperties(
            baseUrl = baseUrl,
            title = map["obs_title"] ?: map["title"] ?: "",
            order = (map["hips_order"] ?: map["maxOrder"] ?: "0").toIntOrNull() ?: 0,
            orderMin = (map["hips_order_min"] ?: map["minOrder"] ?: "3").toIntOrNull() ?: 3,
            tileWidth = (map["hips_tile_width"] ?: "512").toIntOrNull() ?: 512,
            tileFormat = fmt,
            frame = (map["hips_frame"] ?: map["coordsys"] ?: "equatorial"),
            initialRa = (map["hips_initial_ra"] ?: "0").toDoubleOrNull() ?: 0.0,
            initialDec = (map["hips_initial_dec"] ?: "0").toDoubleOrNull() ?: 0.0,
            initialFov = (map["hips_initial_fov"] ?: "60").toDoubleOrNull() ?: 60.0,
        )
    }
}
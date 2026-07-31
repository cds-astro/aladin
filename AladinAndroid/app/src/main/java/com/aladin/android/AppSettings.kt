package com.aladin.android

import android.content.Context
import android.content.SharedPreferences

/**
 * Nivel de calidad / densidad de tiles. Controla el umbral de subdivisión
 * (`TARGET_SCALE` en SkyRenderer): a menor calidad, menos tiles por pantalla
 * -> carga más rápida (menos fetch a 3-4s/tile) a costa de algo de nitidez.
 *
 * - FAST:      scalePx <= 1.0  (tile ~ resolución nativa; mínimos tiles)
 * - BALANCED:  scalePx <= 1.5  (ligero oversample; equilibrio)
 * - HIGH:      scalePx <= 2.5  (tiles más finos; más nitidez, más carga)
 *
 * Es el mismo knob que el `maxOrder(v)` de Aladin desktop (elegir el orden cuyo
 * pixel scale se aproxima al de pantalla), pero expuesto al usuario.
 */
enum class TileQuality(val targetScale: Float, val label: String) {
    FAST(1.0f, "Fast"),
    BALANCED(1.5f, "Balanced"),
    HIGH(2.5f, "High"),
}

/**
 * Ajustes persistentes de la app (SharedPreferences). Regulables desde el panel
 * de Settings: tamaño de caché en memoria, calidad de tile, concurrencia de
 * descarga y modos de vista (rejilla de coordenadas, brillo/contraste/inversión
 * de la imagen). El tamaño de caché de disco es fijo (no cambia en caliente).
 */
data class AppSettings(
    val quality: TileQuality = TileQuality.BALANCED,
    val memCacheMb: Int = 48,
    val concurrency: Int = 12,
    // Modos de vista
    val showGrid: Boolean = false,
    val invert: Boolean = false,
    val brightness: Float = 0f,   // -0.5 .. 0.5
    val contrast: Float = 1f,     // 0.5 .. 2.0
) {
    companion object {
        private const val PREFS = "aladin_settings"
        private const val K_QUALITY = "quality"
        private const val K_MEM = "mem_cache_mb"
        private const val K_CONC = "concurrency"
        private const val K_GRID = "show_grid"
        private const val K_INV = "invert"
        private const val K_BRIGHT = "brightness"
        private const val K_CONT = "contrast"

        fun load(ctx: Context): AppSettings {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val q = runCatching { TileQuality.valueOf(p.getString(K_QUALITY, "BALANCED")!!) }
                .getOrDefault(TileQuality.BALANCED)
            return AppSettings(
                quality = q,
                memCacheMb = p.getInt(K_MEM, 48).coerceAtLeast(8),
                concurrency = p.getInt(K_CONC, 12).coerceIn(2, 24),
                showGrid = p.getBoolean(K_GRID, false),
                invert = p.getBoolean(K_INV, false),
                brightness = p.getFloat(K_BRIGHT, 0f).coerceIn(-0.5f, 0.5f),
                contrast = p.getFloat(K_CONT, 1f).coerceIn(0.5f, 2f),
            )
        }

        fun save(ctx: Context, s: AppSettings) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(K_QUALITY, s.quality.name)
                .putInt(K_MEM, s.memCacheMb)
                .putInt(K_CONC, s.concurrency)
                .putBoolean(K_GRID, s.showGrid)
                .putBoolean(K_INV, s.invert)
                .putFloat(K_BRIGHT, s.brightness)
                .putFloat(K_CONT, s.contrast)
                .apply()
        }
    }
}
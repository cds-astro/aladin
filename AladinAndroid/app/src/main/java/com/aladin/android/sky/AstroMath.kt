@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.sky

import kotlin.math.abs
import kotlin.math.floor

/** Utilidades astronómicas: formato sexagesimal y conversiones. */
object AstroMath {
    fun formatRaHms(raDeg: Double): String {
        val hours = ((raDeg % 360.0 + 360.0) % 360.0) / 15.0
        val h = floor(hours).toInt()
        val m = floor((hours - h) * 60.0).toInt()
        val s = (hours - h - m / 60.0) * 3600.0
        return "%02dh%02dm%05.2fs".format(h, m, s)
    }

    fun formatDecDms(decDeg: Double): String {
        val sign = if (decDeg < 0) "-" else "+"
        val a = abs(decDeg)
        val d = floor(a).toInt()
        val m = floor((a - d) * 60.0).toInt()
        val s = (a - d - m / 60.0) * 3600.0
        return "%s%02d°%02d'%04.1f\"".format(sign, d, m, s)
    }

    /** Formato compacto RA/Dec para mostrar en la barra de estado. */
    fun formatRa(raDeg: Double): String {
        val hours = ((raDeg % 360.0 + 360.0) % 360.0) / 15.0
        val h = floor(hours).toInt()
        val m = floor((hours - h) * 60.0).toInt()
        return "%02dh%02dm".format(h, m)
    }

    fun formatDec(decDeg: Double): String {
        val sign = if (decDeg < 0) "-" else "+"
        val a = abs(decDeg)
        val d = floor(a).toInt()
        val m = floor((a - d) * 60.0).toInt()
        return "%s%02d°%02d'".format(sign, d, m)
    }

    /** Distancia legible: arcsec, arcmin o grados. */
    fun formatSeparation(deg: Double): String {
        val a = abs(deg)
        return when {
            a < 1.0 / 3600.0 -> "%.2f\"".format(a * 3600.0)
            a < 1.0 / 60.0 -> "%.2f'".format(a * 60.0)
            a < 1.0 -> "%.3f°".format(a)
            else -> "%.2f°".format(a)
        }
    }
}

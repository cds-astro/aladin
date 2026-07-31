package com.aladin.android.model

import com.aladin.android.sky.AstroMath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Posición en el cielo en grados (ICRS). RA en [0,360), Dec en [-90,90]. */
data class SkyPosition(
    val raDeg: Double,
    val decDeg: Double,
) {
    val raRadians get() = Math.toRadians(raDeg)
    val decRadians get() = Math.toRadians(decDeg)

    /** Vector unitario cartesiano (x,y,z) en sistema eclítico? No: ecuatorial. */
    fun toVector(): DoubleArray {
        val cd = cos(decRadians)
        return doubleArrayOf(cd * cos(raRadians), cd * sin(raRadians), sin(decRadians))
    }

    fun formatRa(): String = AstroMath.formatRaHms(raDeg)
    fun formatDec(): String = AstroMath.formatDecDms(decDeg)

    companion object {
        /** Distancia angular (grados) entre dos posiciones (gran círculo). */
        fun separation(a: SkyPosition, b: SkyPosition): Double {
            val dRa = Math.toRadians(b.raDeg - a.raDeg)
            val dDec = Math.toRadians(b.decDeg - a.decDeg)
            val s = sin(dDec / 2).let { it * it } +
                cos(Math.toRadians(a.decDeg)) * cos(Math.toRadians(b.decDeg)) *
                sin(dRa / 2).let { it * it }
            return Math.toDegrees(2 * asinSafe(sqrt(s.coerceAtMost(1.0))))
        }

        private fun asinSafe(x: Double): Double {
            val c = x.coerceIn(-1.0, 1.0)
            return kotlin.math.asin(c)
        }
    }
}
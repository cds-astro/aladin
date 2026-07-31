@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.sky

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Zenithal Equal Area projection (ZEA / Lambert azimuthal), the same one
 * Aladin desktop uses for most of the zoom range (see
 * `Projection.getDefaultType` in the desktop code: TAN only below
 * ~4° FoV, ZEA up to ~120°, AIT beyond). Near the center (small angles)
 * ZEA and TAN match almost exactly, so using ZEA across the whole range
 * changes nothing at high zoom and avoids the TAN problem at low zoom:
 * the gnomonic diverges near its horizon (90° from center) and gives the
 * feeling of being "inside a ball" that you have to rotate; ZEA remains
 * well-behaved (bounded radius) almost to the antipode, so zooming out
 * reveals progressively more real sky instead of an endlessly stretching horizon.
 *
 * The projection center is (ra0, dec0) in degrees, with a rotation angle
 * (position) to orient the sky (default north up, rot = 0).
 * `radPerPixel` = projection units per pixel (defined by vertical FoV and height).
 */
data class Projection(
    val ra0: Double,
    val dec0: Double,
    val rotationDeg: Double = 0.0,
    val radPerPixel: Double,
    val widthPx: Float,
    val heightPx: Float,
) {
    val cxPx get() = widthPx / 2f
    val cyPx get() = heightPx / 2f
    private val rot = Math.toRadians(rotationDeg)
    private val cosRot = cos(rot)
    private val sinRot = sin(rot)
    private val ra0Rad = Math.toRadians(ra0)
    private val dec0Rad = Math.toRadians(dec0)
    private val sinDec0 = sin(dec0Rad)
    private val cosDec0 = cos(dec0Rad)

    /** (raDeg, decDeg) -> (screenX, screenY) or null if at the antipode (undefined). */
    fun forward(raDeg: Double, decDeg: Double): FloatArray? = forwardInternal(raDeg, decDeg, clamp = false)

    /**
     * Same as [forward] but never returns null for points beyond the [forward] cutoff:
     * instead it clamps z to the cutoff, projecting them onto the boundary circle.
     *
     * Used only for the tile mesh grid (see SkyRenderer.placementMesh). A HEALPix tile
     * near the edge of the visible hemisphere straddles the z cutoff, so some of its mesh
     * grid points fall outside it; with plain [forward] those points are null and the
     * whole mesh cell touching them gets dropped, leaving a HEALPix-rhombus-shaped black
     * gap right at the edge of the view. Clamping instead gives every grid point a
     * (possibly distorted, but on-screen) position, so the tile's texture still covers
     * the gap instead of leaving a hole. The tile is already being culled/leveled based
     * on [forward]'s stricter cutoff, so this only affects a thin sliver of pixels right
     * at the visible boundary — never the choice of which tiles to draw.
     */
    fun forwardClamped(raDeg: Double, decDeg: Double): FloatArray = forwardInternal(raDeg, decDeg, clamp = true)!!

    private fun forwardInternal(raDeg: Double, decDeg: Double, clamp: Boolean): FloatArray? {
        val dec = Math.toRadians(decDeg)
        var dra = Math.toRadians(raDeg) - ra0Rad
        if (dra > PI) dra -= 2 * PI
        else if (dra < -PI) dra += 2 * PI
        val cosDec = cos(dec)
        var x = cosDec * sin(dra)
        var y = sin(dec) * cosDec0 - cosDec * sinDec0 * cos(dra)
        var z = sin(dec) * sinDec0 + cosDec * cosDec0 * cos(dra)

        // ZEA can mathematically project the entire sphere (except the exact antipode),
        // but rendering flat parallelograms across the back hemisphere causes massive
        // distortion and overlapping polygons over the front view.
        // We restrict rendering to the front hemisphere + a margin for the screen corners.
        // z < -0.15 corresponds to an angle > ~98.6 degrees from the center.
        if (z < -0.15) {
            if (!clamp) return null
            // Project onto the boundary circle along the radial vector in the xy-plane
            // instead of modifying z alone (which mapped antipode points to the screen center).
            val rCut = sqrt(1.0 - 0.15 * 0.15)
            val r = sqrt(x * x + y * y)
            if (r > 1e-9) {
                val scale = rCut / r
                x *= scale
                y *= scale
            } else {
                x = rCut
                y = 0.0
            }
            z = -0.15
        }

        // ZEA (Zenithal Equal Area): w = sqrt((1+z)/2)
        val w = sqrt((1.0 + z) / 2.0)
        if (w < 1e-9) return null // exact antipode: undefined
        val xi = x / w
        val eta = y / w
        val xr = xi * cosRot - eta * sinRot
        val yr = xi * sinRot + eta * cosRot
        val sx = cxPx + (xr / radPerPixel).toFloat()
        val sy = cyPx - (yr / radPerPixel).toFloat()
        return floatArrayOf(sx, sy)
    }

    /** (screenX, screenY) -> (raDeg, decDeg). */
    fun inverse(sx: Float, sy: Float): DoubleArray {
        val xr = (sx - cxPx) * radPerPixel.toFloat()
        val yr = -(sy - cyPx) * radPerPixel.toFloat()
        val xi = xr * cosRot + yr * sinRot
        val eta = -xr * sinRot + yr * cosRot
        val rho = sqrt(xi * xi + eta * eta)
        // ZEA: rho = 2*sin(c/2) -> c = 2*asin(rho/2). rho can reach up to 2
        // (antipode); coerceIn avoids NaN due to rounding when rho touches that limit.
        val c = 2.0 * kotlin.math.asin((rho / 2.0).coerceIn(-1.0, 1.0))
        val sinC = sin(c)
        val cosC = cos(c)
        val dec: Double
        val dra: Double
        if (rho < 1e-12) {
            dec = Math.toDegrees(dec0Rad)
            dra = 0.0
        } else {
            dec = Math.toDegrees(
                kotlin.math.asin((cosC * sinDec0 + eta * sinC * cosDec0 / rho).coerceIn(-1.0, 1.0))
            )
            dra = Math.toDegrees(atan2(xi * sinC, rho * cosDec0 * cosC - eta * sinDec0 * sinC))
        }
        var ra = (Math.toDegrees(ra0Rad) + dra + 360.0) % 360.0
        if (ra < 0) ra += 360.0
        return doubleArrayOf(ra, dec)
    }

    companion object {
        /**
         * Projection units (rho) per pixel for a given vertical FoV.
         * In ZEA, rho = 2*sin(theta/2) where theta is the angular distance to
         * the center (see `inverse`: c = 2*asin(rho/2) is exactly that distance).
         * At the vertical screen edge theta = fovDeg/2, so the projection radius
         * there is rho_edge = 2*sin(fovDeg/4). (We previously used
         * tan(fovRad/2), the TAN/gnomonic formula; with ZEA the edge never goes
         * to infinity, which is exactly what allows zooming out without artifacts.)
         */
        fun radPerPixelFromFov(fovDeg: Double, heightPx: Float): Double {
            val fovRad = Math.toRadians(fovDeg.coerceIn(0.0001, 179.99))
            val half = 2.0 * sin(fovRad / 4.0)
            return half / (heightPx / 2.0)
        }
    }
}

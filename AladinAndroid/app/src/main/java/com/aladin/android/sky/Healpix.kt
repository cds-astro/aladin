package com.aladin.android.sky

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HEALPix anidado (NESTED), dirección forward únicamente.
 *
 * Portado fielmente del algoritmo canónico (HEALPix / KStars): descomposición
 * nest2xyf + toVec3 con jrll/jpll. En orden NESTED los 4 hijos de un píxel de
 * orden N son 4*ipix + {0,1,2,3} en orden N+1, así que el render por recursión
 * nunca necesita la dirección inversa (ang2pix).
 *
 * Devuelve grados (ra, dec) ICRS.
 */
object Healpix {
    private val JRLL = intArrayOf(2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4)
    private val JPLL = intArrayOf(1, 3, 5, 7, 0, 2, 4, 6, 1, 3, 5, 7)

    fun nside(order: Int): Int = 1 shl order
    fun npix(order: Int): Long = 12L * nside(order).toLong() * nside(order)

    /** Coordenadas (ix, iy) dentro de la cara, a partir del índice local ipf. */
    private fun decompose(ipf: Int): Pair<Int, Int> {
        val ix = compressEven(ipf.toLong())
        val iy = compressOdd(ipf.toLong())
        return ix to iy
    }

    /** Comprime los bits pares (0,2,4,…) de x en posiciones consecutivas. */
    private fun compressEven(xIn: Long): Int {
        var v = 0
        var i = 0
        var b = xIn
        while (b != 0L) {
            if (b and 1L != 0L) v = v or (1 shl i)
            b = b ushr 2
            i++
        }
        return v
    }

    private fun compressOdd(x: Long): Int = compressEven(x ushr 1)

    /**
     * Vector unitario (x,y,z) para coordenadas continuas (fx,fy) en [0,1]
     * dentro de la cara `face` (0..11). Igual que HEALPix::toVec3.
     */
    private fun toVec3(fx: Double, fy: Double, face: Int): DoubleArray {
        val jr = JRLL[face] - fx - fy
        val nr: Double
        val z: Double
        var haveSth = false
        var sth = 0.0
        if (jr < 1.0) {
            nr = jr
            val tmp = nr * nr / 3.0
            z = 1.0 - tmp
            if (z > 0.99) { sth = sqrt(tmp * (2.0 - tmp)); haveSth = true }
        } else if (jr > 3.0) {
            nr = 4.0 - jr
            val tmp = nr * nr / 3.0
            z = tmp - 1.0
            if (z < -0.99) { sth = sqrt(tmp * (2.0 - tmp)); haveSth = true }
        } else {
            nr = 1.0
            z = (2.0 - jr) * 2.0 / 3.0
        }
        var t = JPLL[face] * nr + fx - fy
        if (t < 0) t += 8
        if (t >= 8) t -= 8
        val phi = if (nr < 1e-15) 0.0 else (PI / 4.0 * t) / nr
        val st = if (haveSth) sth else sqrt((1.0 - z) * (1.0 + z))
        return doubleArrayOf(st * cos(phi), st * sin(phi), z)
    }

    private fun vecToRadec(v: DoubleArray): DoubleArray {
        val ra = ((Math.toDegrees(atan2(v[1], v[0])) % 360.0) + 360.0) % 360.0
        val dec = Math.toDegrees(kotlin.math.asin(v[2].coerceIn(-1.0, 1.0)))
        return doubleArrayOf(ra, dec)
    }

    /** Centro (raDeg, decDeg) del píxel anidado (order, ipix). */
    fun center(order: Int, ipix: Long): DoubleArray {
        val nside = nside(order)
        val npface = nside.toLong() * nside
        val face = (ipix / npface).toInt()
        val ipf = (ipix % npface).toInt()
        val (ix, iy) = decompose(ipf)
        val fx = (ix + 0.5) / nside
        val fy = (iy + 0.5) / nside
        return vecToRadec(toVec3(fx, fy, face))
    }

    /**
     * Punto (raDeg, decDeg) en la posición fraccionaria (fx,fy) dentro del píxel
     * (fx,fy en [0,1]: 0,0 = esquina "inferior izquierda", 1,1 = "superior
     * derecha" según la parametrización de la cara). Se usa para subdividir un
     * tile en una malla N×N y colocar cada celda con su propio afín: al proyectar
     * puntos celestes reales, los bordes compartidos entre tiles vecinos coinciden
     * exactamente (sin huecos por la distorsión perspectivica de la gnomónica).
     */
    fun radecAt(order: Int, ipix: Long, fx: Double, fy: Double): DoubleArray {
        val nside = nside(order)
        val npface = nside.toLong() * nside
        val face = (ipix / npface).toInt()
        val ipf = (ipix % npface).toInt()
        val (ix, iy) = decompose(ipf)
        val fxFace = (ix + fx) / nside
        val fyFace = (iy + fy) / nside
        return vecToRadec(toVec3(fxFace, fyFace, face))
    }

    /**
     * 4 esquinas (raDeg, decDeg) del píxel, en sentido horario según la
     * parametrización (fx,fy): (x0,y0),(x1,y0),(x1,y1),(x0,y1).
     */
    fun corners(order: Int, ipix: Long): Array<DoubleArray> {
        val nside = nside(order)
        val npface = nside.toLong() * nside
        val face = (ipix / npface).toInt()
        val ipf = (ipix % npface).toInt()
        val (ix, iy) = decompose(ipf)
        val x0 = ix.toDouble() / nside
        val x1 = (ix + 1).toDouble() / nside
        val y0 = iy.toDouble() / nside
        val y1 = (iy + 1).toDouble() / nside
        return arrayOf(
            vecToRadec(toVec3(x0, y0, face)),
            vecToRadec(toVec3(x1, y0, face)),
            vecToRadec(toVec3(x1, y1, face)),
            vecToRadec(toVec3(x0, y1, face)),
        )
    }
}
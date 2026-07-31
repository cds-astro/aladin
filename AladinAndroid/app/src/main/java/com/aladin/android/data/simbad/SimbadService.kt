@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.data.simbad

import com.aladin.android.data.VotableParser
import com.aladin.android.data.net.HttpClient
import com.aladin.android.model.CatalogRow
import com.aladin.android.model.SkyPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/** Resultado de una búsqueda en Simbad. */
data class SimbadObject(
    val mainId: String,
    val otype: String,
    val position: SkyPosition,
    val magV: Double? = null,
    val spectralType: String = "",
    val raw: Map<String, String> = emptyMap(),
)

/**
 * Consulta Simbad por nombre de objeto o por coordenadas (query = "ra dec" en grados).
 * Endpoint: simbad/sim-all?Ident=...&Radius=...&Radius.unit=arcmin&Votable=...
 * (Véase cds/aladin/ServerSimbad.java y el diccionario GLU SimbadXML1.)
 */
class SimbadService {
    private val client = HttpClient.client

    private val columns = "main_id,otype,ra,dec,flux(V),sp"

    suspend fun search(query: String): List<SimbadObject> =
        withContext(Dispatchers.IO) {
            val ident = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=$ident&Votable=$columns"
            val text = fetchText(url) ?: return@withContext emptyList()
            VotableParser.parse(text).mapNotNull { row -> row.toSimbad() }
        }

    suspend fun searchRegion(raDeg: Double, decDeg: Double, radiusArcmin: Double = 10.0, maxRows: Int = 500): List<SimbadObject> =
        withContext(Dispatchers.IO) {
            val tapUrl = "https://simbad.u-strasbg.fr/simbad/sim-tap/sync"
            val radiusDegrees = radiusArcmin / 60.0
            val rStr = String.format(java.util.Locale.US, "%.6f", raDeg)
            val dStr = String.format(java.util.Locale.US, "%.6f", decDeg)
            val radStr = String.format(java.util.Locale.US, "%.6f", radiusDegrees)
            val topClause = if (maxRows > 0) "TOP $maxRows " else ""
            val adql = "SELECT $topClause b.main_id, b.otype, b.ra, b.dec, f.flux as flux_V, b.sp_type FROM basic AS b LEFT JOIN flux AS f ON b.oid = f.oidref AND f.filter = 'V' WHERE CONTAINS(POINT('ICRS', b.ra, b.dec), CIRCLE('ICRS', $rStr, $dStr, $radStr))=1 ORDER BY flux_V ASC"
            val url = "$tapUrl?REQUEST=doQuery&LANG=ADQL&FORMAT=votable/td&QUERY=${URLEncoder.encode(adql, "UTF-8")}"
            val text = fetchText(url) ?: return@withContext emptyList()
            VotableParser.parse(text).mapNotNull { row -> row.toSimbad() }
        }

    /** Búsqueda de cono por coordenadas para identificar un objeto al tocar. */
    suspend fun coneSearch(raDeg: Double, decDeg: Double, radiusArcmin: Double = 1.0): SimbadObject? {
        val list = searchRegion(raDeg, decDeg, radiusArcmin)
        return list.minByOrNull {
            SkyPosition.separation(it.position, SkyPosition(raDeg, decDeg))
        }
    }

    fun toCatalogRow(obj: SimbadObject): CatalogRow = CatalogRow(
        id = obj.mainId,
        position = obj.position,
        raDeg = obj.position.raDeg,
        decDeg = obj.position.decDeg,
        mag = obj.magV,
        columns = obj.raw,
    )

    fun Map<String, String>.toSimbad(): SimbadObject? {
        fun firstValue(keys: List<String>): String? = keys.firstOrNull { this.containsKey(it) }?.let { this[it] }

        val mainId = firstValue(listOf("main_id", "MAIN_ID")) ?: return null
        val otype = firstValue(listOf("otype", "OTYPE")) ?: "Unknown"
        val raStr = firstValue(listOf("ra", "RA"))?.toDoubleOrNull() ?: return null
        val decStr = firstValue(listOf("dec", "DEC"))?.toDoubleOrNull() ?: return null
        val fluxV = firstValue(listOf("flux_V", "FLUX_V", "flux(V)"))?.toDoubleOrNull()
        val sp = firstValue(listOf("sp_type", "SP_TYPE", "sp")) ?: ""
        
        return SimbadObject(
            mainId = mainId,
            otype = otype,
            position = SkyPosition(raStr, decStr),
            magV = fluxV,
            spectralType = sp,
            raw = this,
        )
    }

    private fun fetchText(url: String): String? = try {
        client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { r ->
            if (r.isSuccessful) {
                val body = r.body?.string()
                body
            } else {
                android.util.Log.e("Simbad", "HTTP Error: ${r.code}")
                null
            }
        }
    } catch (e: Exception) { 
        android.util.Log.e("Simbad", "Network Error", e)
        null 
    }
}

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

    suspend fun search(query: String, radiusArcmin: Double = 10.0): List<SimbadObject> =
        withContext(Dispatchers.IO) {
            val ident = URLEncoder.encode(query.trim(), "UTF-8")
            val url =
                "https://simbad.u-strasbg.fr/simbad/sim-all?Ident=$ident" +
                    "&Radius=$radiusArcmin&Radius.unit=arcmin&Votable=$columns"
            val text = fetchText(url) ?: return@withContext emptyList()
            VotableParser.parse(text).mapNotNull { row -> row.toSimbad() }
        }

    /** Búsqueda de cono por coordenadas para identificar un objeto al tocar. */
    suspend fun coneSearch(raDeg: Double, decDeg: Double, radiusArcmin: Double = 1.0): SimbadObject? {
        val q = "%.6f %.6f".format(raDeg, decDeg)
        val list = search(q, radiusArcmin)
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

    private fun Map<String, String>.toSimbad(): SimbadObject? {
        val ra = this["ra(d)"]?.toDoubleOrNull() ?: this["ra"]?.toDoubleOrNull() ?: return null
        val dec = this["dec(d)"]?.toDoubleOrNull() ?: this["dec"]?.toDoubleOrNull() ?: return null
        return SimbadObject(
            mainId = this["main_id"] ?: "?",
            otype = this["otype"] ?: "",
            position = SkyPosition(ra, dec),
            magV = this["flux(V)"]?.toDoubleOrNull(),
            spectralType = this["sp"] ?: "",
            raw = this,
        )
    }

    private fun fetchText(url: String): String? = try {
        client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    } catch (e: Exception) { null }
}
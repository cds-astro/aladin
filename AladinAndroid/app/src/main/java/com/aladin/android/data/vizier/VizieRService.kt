package com.aladin.android.data.vizier

import com.aladin.android.data.VotableParser
import com.aladin.android.data.net.HttpClient
import com.aladin.android.model.CatalogDescriptor
import com.aladin.android.model.CatalogRow
import com.aladin.android.model.SkyPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Consulta VizieR (cone search) y devuelve fuentes como overlays.
 * Endpoint: viz-bin/votable?-source=...&-c=ra dec&-c.rm=arcmin&-out.add=_RAJ,_DEJ&-out.max=...
 * (Véase cds/aladin/ServerVizieR.java y cds/vizier/VizieRQuery.java.)
 */
class VizieRService {
    private val client = HttpClient.client

    /** Popular catalogs for the picker. */
    val catalogs: List<CatalogDescriptor> = listOf(
        CatalogDescriptor("I/355/gaiadr3", "Gaia DR3", "Gaia astrometric catalog (DR3)"),
        CatalogDescriptor("II/246/out", "2MASS", "2MASS all-sky infrared survey"),
        CatalogDescriptor("I/259/tyc2", "Tycho-2", "Tycho-2 astrometric catalog"),
        CatalogDescriptor("V/147/sdss16", "SDSS16", "Sloan Digital Sky Survey (DR16)"),
        CatalogDescriptor("I/322a", "Gaia DR1", "Gaia catalog (DR1)"),
        CatalogDescriptor("SIMBAD", "Simbad", "Simbad Astronomical Database"),
    )

    suspend fun coneSearch(
        catalogId: String,
        raDeg: Double,
        decDeg: Double,
        radiusArcmin: Double,
        maxRows: Int = 500,
    ): List<CatalogRow> = withContext(Dispatchers.IO) {
        val c = URLEncoder.encode(String.format(java.util.Locale.US, "%.6f %+.6f", raDeg, decDeg), "UTF-8")
        val maxParam = if (maxRows <= 0) "unlimited" else maxRows.toString()
        val url =
            "https://vizier.cds.unistra.fr/viz-bin/votable" +
                "?-source=$catalogId" +
                "&-c=$c" +
                "&-c.rm=$radiusArcmin" +
                "&-out.add=_RAJ,_DEJ" +
                "&-out.max=$maxParam" +
                "&-out.meta=DhuL" +
                "&-oc.form=dm"
        android.util.Log.d("VizieR", "Fetching: $url")
        val text = fetchText(url)
        if (text == null) {
            android.util.Log.e("VizieR", "Fetch failed (null text)")
            return@withContext emptyList()
        }
        val rows = VotableParser.parse(text).mapNotNull { row -> row.toCatalogRow() }
        android.util.Log.d("VizieR", "Parsed ${rows.size} rows")
        rows
    }

    private val raKeys = listOf("_RAJ", "RAJ2000", "RA_ICRS", "RA(ICRS)", "RA", "RAJ2000")
    private val decKeys = listOf("_DEJ", "DEJ2000", "DE_ICRS", "DE(ICRS)", "DE", "DEJ2000")

    private fun Map<String, String>.toCatalogRow(): CatalogRow? {
        val ra = firstValue(raKeys)?.toDoubleOrNull() ?: return null
        val dec = firstValue(decKeys)?.toDoubleOrNull() ?: return null
        val mag = firstValue(listOf("Gmag", "Kmag", "Jmag", "Hmag", "VTmag", "BTmag", "rmag", "g", "Vmag"))
            ?.toDoubleOrNull()
        val id = firstValue(listOf("Source", "2MASS", "TYC", "objID", "Source_id")) ?: "${ra}_${dec}"
        return CatalogRow(
            id = id,
            position = SkyPosition(ra, dec),
            raDeg = ra,
            decDeg = dec,
            mag = mag,
            columns = this,
        )
    }

    private fun Map<String, String>.firstValue(keys: List<String>): String? =
        keys.firstOrNull { containsKey(it) }?.let { get(it) }

    private fun fetchText(url: String): String? = try {
        client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    } catch (e: Exception) { null }
}
package com.aladin.android.data.hips

/**
 * Un survey HiPS seleccionable. `baseUrl` apunta a la raíz del árbol de tiles.
 * Los tiles se sirven en: {baseUrl}/Norder{order}/Dir{dir}/Npix{npix}.{ext}
 * donde dir = (npix / 10000) * 10000  (DIRSIZE = 10000, ver cds/tools/pixtools/Util.java).
 */
data class HiPSSurvey(
    val id: String,
    val title: String,
    val baseUrl: String,
    val isColor: Boolean = true,
    val description: String = "",
) {
    fun tileUrl(order: Int, npix: Long, ext: String): String {
        val dir = (npix / 10000L) * 10000L
        return "$baseUrl/Norder$order/Dir$dir/Npix$npix.$ext"
    }

    fun propertiesUrl(): String = "$baseUrl/properties"
}

/** Registro de encuestas HiPS conocidas (URLs reales del CDS). */
object SurveyRegistry {
    val builtIn: List<HiPSSurvey> = listOf(
        HiPSSurvey("CDS/P/DSS2/color", "DSS2 color", "https://alasky.cds.unistra.fr/DSS/DSSColor", true, "Optical survey (color)"),
        HiPSSurvey("CDS/P/DSS2/red", "DSS2 red (DSS1)", "https://alasky.cds.unistra.fr/DSS/DSS", false, "Optical survey (red)"),
        HiPSSurvey("CDS/P/DSS2/NIR", "DSS2 NIR", "https://alasky.cds.unistra.fr/DSS/DSS2-NIR", false, "Near-IR"),
        HiPSSurvey("CDS/P/DSS2/blue", "DSS2 blue", "https://alasky.cds.unistra.fr/DSS/DSS2-blue-XJ-S", false, "Optical (blue)"),
        HiPSSurvey("P/2MASS/color", "2MASS color", "https://alasky.cds.unistra.fr/2MASS/Color", true, "Near-IR all-sky"),
        HiPSSurvey("P/AllWISE/color", "ALLWISE color", "https://alasky.cds.unistra.fr/AllWISE/AllWISE2MASS-RGB", true, "Mid-IR"),
        HiPSSurvey("P/SDSS9/color", "SDSS9 color", "https://alasky.cds.unistra.fr/Pan-STARRS/DR1/color", true, "Optical, deeper"),
        HiPSSurvey("P/GALEXGR6/AIS/color", "GALEX color", "https://alasky.cds.unistra.fr/GALEX/GR6-ACS-Color", true, "UV"),
        HiPSSurvey("P/Fermi/3", "Fermi 3", "https://alasky.cds.unistra.fr/Fermi/3", false, "Gamma-ray"),
        HiPSSurvey("P/HST/ACS", "HST ACS", "https://alasky.cds.unistra.fr/HST/ACS", false, "Hubble ACS"),
    )

    fun byId(id: String): HiPSSurvey? = builtIn.firstOrNull { it.id == id }
}
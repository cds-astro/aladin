package com.aladin.android.data.simbad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SimbadServiceTest {
    @Test
    fun testToSimbad() {
        val service = SimbadService()
        
        val map = mapOf(
            "main_id" to "M 31",
            "otype" to "Galaxy",
            "ra" to "10.6847",
            "dec" to "41.2687",
            "flux_V" to "3.44",
            "sp_type" to "G0"
        )
        
        with(service) {
            val simbad = map.toSimbad()
            assertNotNull(simbad)
            assertEquals("M 31", simbad?.mainId)
            assertEquals("Galaxy", simbad?.otype)
            assertEquals(10.6847, simbad?.position?.raDeg)
            assertEquals(41.2687, simbad?.position?.decDeg)
            assertEquals(3.44, simbad?.magV)
            assertEquals("G0", simbad?.spectralType)
        }
    }

    @Test
    fun testToSimbadMissingKeys() {
        val service = SimbadService()
        val map = mapOf(
            "otype" to "Galaxy"
        )
        
        with(service) {
            val simbad = map.toSimbad()
            assertNull(simbad)
        }
    }
}

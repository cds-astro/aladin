package com.aladin.android.sky

import org.junit.Assert.assertEquals
import org.junit.Test

class AstroMathTest {

    @Test
    fun testFormatRaHms() {
        assertEquals("00h00m00.00s", AstroMath.formatRaHms(0.0))
        assertEquals("12h00m00.00s", AstroMath.formatRaHms(180.0))
        assertEquals("01h00m00.00s", AstroMath.formatRaHms(15.0))
    }

    @Test
    fun testFormatDecDms() {
        assertEquals("+00°00'00.0\"", AstroMath.formatDecDms(0.0))
        assertEquals("+90°00'00.0\"", AstroMath.formatDecDms(90.0))
        assertEquals("-90°00'00.0\"", AstroMath.formatDecDms(-90.0))
        assertEquals("+15°30'00.0\"", AstroMath.formatDecDms(15.5))
    }

    @Test
    fun testFormatRa() {
        assertEquals("00h00m", AstroMath.formatRa(0.0))
        assertEquals("12h00m", AstroMath.formatRa(180.0))
    }

    @Test
    fun testFormatDec() {
        assertEquals("+00°00'", AstroMath.formatDec(0.0))
        assertEquals("-15°30'", AstroMath.formatDec(-15.5))
    }

    @Test
    fun testFormatSeparation() {
        assertEquals("0.50\"", AstroMath.formatSeparation(0.5 / 3600.0))
        assertEquals("0.50'", AstroMath.formatSeparation(0.5 / 60.0))
        assertEquals("0.500°", AstroMath.formatSeparation(0.5))
        assertEquals("1.50°", AstroMath.formatSeparation(1.5))
    }
}

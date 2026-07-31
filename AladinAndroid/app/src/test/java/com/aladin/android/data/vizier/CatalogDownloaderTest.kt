package com.aladin.android.data.vizier

import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogDownloaderTest {

    @Test
    fun detectVotableErrorReportsVizierErrorMessage() {
        val file = File.createTempFile("votable_error", ".xml")
        file.deleteOnExit()
        file.writeText(
            """
            <VOTABLE>
            <INFO ID="Error" name="Error" value="Table 'messier' does not exist in catalog: VII/253"/>
            <RESOURCE></RESOURCE>
            </VOTABLE>
            """.trimIndent()
        )

        val error = CatalogDownloader.detectVotableError(file)

        assertNotNull("An embedded VOTable error should be detected", error)
        assertTrue(error!!.contains("does not exist in catalog"))
    }

    @Test
    fun detectVotableErrorReturnsNullForValidVotable() {
        val file = File.createTempFile("votable_ok", ".xml")
        file.deleteOnExit()
        file.writeText(
            """
            <VOTABLE>
            <INFO name="service_protocol" value="ASU"/>
            <RESOURCE><TABLE><DATA><TABLEDATA>
            <TR><TD>M 31</TD></TR>
            </TABLEDATA></DATA></TABLE></RESOURCE>
            </VOTABLE>
            """.trimIndent()
        )

        assertNull("A valid VOTable must not be flagged as an error", CatalogDownloader.detectVotableError(file))
    }
}

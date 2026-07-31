package cds.aladin;

import org.junit.Test;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class CatalogDownloaderTest {

    @Test
    public void testDetectVotableErrorReportsVizierErrorMessage() throws Exception {
        File tempFile = File.createTempFile("votable_error", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter w = new FileWriter(tempFile)) {
            w.write("<VOTABLE>\n"
                    + "<INFO ID=\"Error\" name=\"Error\" value=\"Table 'messier' does not exist in catalog: VII/253\"/>\n"
                    + "<RESOURCE></RESOURCE>\n"
                    + "</VOTABLE>\n");
        }

        String error = CatalogDownloader.detectVotableError(tempFile);

        assertNotNull("An embedded VOTable error should be detected", error);
        assertTrue(error.contains("does not exist in catalog"));
    }

    @Test
    public void testDetectVotableErrorReturnsNullForValidVotable() throws Exception {
        File tempFile = File.createTempFile("votable_ok", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter w = new FileWriter(tempFile)) {
            w.write("<VOTABLE>\n"
                    + "<INFO name=\"service_protocol\" value=\"ASU\"/>\n"
                    + "<RESOURCE><TABLE><DATA><TABLEDATA>\n"
                    + "<TR><TD>M 31</TD></TR>\n"
                    + "</TABLEDATA></DATA></TABLE></RESOURCE>\n"
                    + "</VOTABLE>\n");
        }

        assertNull("A valid VOTable must not be flagged as an error", CatalogDownloader.detectVotableError(tempFile));
    }

    @Test
    public void testDownloadVizierEdgeCase() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean error = new AtomicBoolean(false);
        
        File tempFile = File.createTempFile("test_cat", ".xml");
        tempFile.deleteOnExit();

        CatalogDownloader.download("Vizier", "I/239/hip_main", 0, 0, 0, tempFile, new CatalogDownloader.DownloaderListener() {
            @Override
            public void onProgress(long totalBytesDownloaded) {
            }

            @Override
            public void onComplete() {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                error.set(true);
                latch.countDown();
            }
        });

        // Wait up to 30 seconds for download to complete
        assertTrue("Timeout waiting for download", latch.await(30, TimeUnit.SECONDS));
        assertTrue("Download should be successful", success.get());
        assertFalse("Download should not have errors", error.get());
        assertTrue("File should have content", tempFile.length() > 0);
    }
}

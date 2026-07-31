package cds.aladin;

import org.junit.Test;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class CatalogDownloaderTest {

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

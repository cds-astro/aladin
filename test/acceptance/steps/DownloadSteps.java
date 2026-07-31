package steps;

import cds.aladin.CatalogDownloader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import static org.junit.Assert.*;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DownloadSteps {

    private File targetFile;
    private boolean downloadComplete = false;
    private boolean downloadError = false;

    @Given("the CatalogDownloader is ready")
    public void the_catalog_downloader_is_ready() throws Exception {
        targetFile = File.createTempFile("cucumber_cat", ".xml");
        targetFile.deleteOnExit();
    }

    @When("I request to download {string} catalog {string} at RA {double}, DEC {double}, radius {double}")
    public void i_request_to_download_catalog_at_ra_dec_radius(String source, String catalog, Double ra, Double dec, Double radius) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CatalogDownloader.download(source, catalog, ra, dec, radius, targetFile, new CatalogDownloader.DownloaderListener() {
            @Override
            public void onProgress(long totalBytesDownloaded) {}

            @Override
            public void onComplete() {
                downloadComplete = true;
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                downloadError = true;
                latch.countDown();
            }
        });
        latch.await(30, TimeUnit.SECONDS);
    }

    @Then("the file should be successfully downloaded and not empty")
    public void the_file_should_be_successfully_downloaded_and_not_empty() {
        assertTrue("Download must be complete", downloadComplete);
        assertFalse("Download must not error", downloadError);
        assertTrue("File must be non-empty", targetFile.length() > 0);
    }
}

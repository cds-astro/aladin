package cds.aladin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;

public class HiPSDownloader {

    private String baseUrl;
    private File targetDir;
    private int maxOrder;
    private String ext = ".jpg";
    private ThreadPoolExecutor executor;
    private AtomicInteger tilesDownloaded = new AtomicInteger(0);
    private AtomicLong totalBytesDownloaded = new AtomicLong(0);
    private AtomicInteger activeTasks = new AtomicInteger(0);
    
    private volatile boolean isCancelled = false;
    private HiPSDownloaderListener listener;

    public interface HiPSDownloaderListener {
        void onProgress(int tilesDownloaded, long totalBytesDownloaded, int activeTasks);
        void onComplete();
        void onError(String message);
    }

    public HiPSDownloader(String baseUrl, File targetDir, int maxOrder, HiPSDownloaderListener listener) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        this.baseUrl = baseUrl;
        this.targetDir = targetDir;
        this.maxOrder = maxOrder;
        this.listener = listener;
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    }

    public void start() {
        new Thread(() -> {
            try {
                // 1. Download properties file
                downloadProperties();
                
                // 2. Start order 0
                for (int i = 0; i < 12; i++) {
                    queueTile(0, i);
                }
                
                // Wait for completion
                while (!isCancelled && activeTasks.get() > 0) {
                    Thread.sleep(500);
                    updateProgress();
                }
                
                if (!isCancelled) {
                    SwingUtilities.invokeLater(() -> listener.onComplete());
                }
                executor.shutdownNow();
                
            } catch (Exception e) {
                if (!isCancelled) {
                    SwingUtilities.invokeLater(() -> listener.onError(e.getMessage()));
                }
                executor.shutdownNow();
            }
        }).start();
    }

    public void cancel() {
        isCancelled = true;
        executor.shutdownNow();
    }

    private void updateProgress() {
        SwingUtilities.invokeLater(() -> {
            if (listener != null) {
                listener.onProgress(tilesDownloaded.get(), totalBytesDownloaded.get(), activeTasks.get());
            }
        });
    }

    private void downloadProperties() throws Exception {
        URL url = new URL(baseUrl + "properties");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == 200) {
            File propFile = new File(targetDir, "properties");
            targetDir.mkdirs();
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(propFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propFile)))) {
                
                byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, n);
                }
            }
            
            // Read properties to find extension
            try (BufferedReader reader = new BufferedReader(new FileReader(propFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("hips_tile_format=")) {
                        String format = line.substring("hips_tile_format=".length()).trim().toLowerCase();
                        String[] formats = format.split(" ");
                        if (formats.length > 0) {
                            String f = formats[0];
                            if (f.equals("jpeg")) ext = ".jpg";
                            else if (f.equals("png")) ext = ".png";
                            else if (f.equals("fits")) ext = ".fits";
                            else ext = "." + f;
                        }
                        break;
                    }
                }
            }
        } else {
            throw new Exception("Could not download properties file (HTTP " + conn.getResponseCode() + ")");
        }
    }

    private void queueTile(int order, int npix) {
        if (isCancelled) return;
        activeTasks.incrementAndGet();
        executor.submit(() -> {
            try {
                if (isCancelled) return;
                processTile(order, npix);
            } catch (Exception e) {
                // Ignore single tile errors
            } finally {
                activeTasks.decrementAndGet();
                updateProgress();
            }
        });
    }

    private void processTile(int order, int npix) throws Exception {
        int dirY = (npix / 10000) * 10000;
        String relPath = "Norder" + order + "/Dir" + dirY + "/Npix" + npix + ext;
        File tileFile = new File(targetDir, relPath);
        
        boolean success = false;
        
        if (tileFile.exists() && tileFile.length() > 0) {
            // Already exists, skip download
            success = true;
        } else {
            // Download
            URL url = new URL(baseUrl + relPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int code = conn.getResponseCode();
            if (code == 200) {
                tileFile.getParentFile().mkdirs();
                long downloaded = 0;
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(tileFile)) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = is.read(buffer)) != -1) {
                        if (isCancelled) break;
                        fos.write(buffer, 0, n);
                        downloaded += n;
                    }
                }
                if (!isCancelled) {
                    totalBytesDownloaded.addAndGet(downloaded);
                    tilesDownloaded.incrementAndGet();
                    success = true;
                }
            } else if (code == 404) {
                // Empty sky
                success = false;
            }
        }
        
        if (success && order < maxOrder && !isCancelled) {
            // Queue children
            for (int i = 0; i < 4; i++) {
                queueTile(order + 1, 4 * npix + i);
            }
        }
    }
}

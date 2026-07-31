package cds.aladin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatalogDownloader {

    // VizieR's ASU service reports invalid catalog names (and similar errors) as an
    // HTTP 200 response whose VOTable body carries an <INFO name="Error" .../> tag,
    // instead of a non-2xx status. Detect that so we don't report success with a
    // garbage file. Errors always appear near the top of the document, so peeking
    // at the first few KB is enough even for multi-megabyte catalog downloads.
    private static final Pattern VOTABLE_ERROR = Pattern.compile("name=\"Error\"\\s+value=\"([^\"]*)\"");

    static String detectVotableError(File file) {
        byte[] buf = new byte[8192];
        int len;
        try (FileInputStream fis = new FileInputStream(file)) {
            len = fis.read(buf);
        } catch (IOException e) {
            return null;
        }
        if (len <= 0) return null;
        String head = new String(buf, 0, len, StandardCharsets.UTF_8);
        Matcher m = VOTABLE_ERROR.matcher(head);
        if (m.find()) {
            String msg = m.group(1).trim();
            return msg.isEmpty() ? "Unknown VizieR/Simbad error" : msg;
        }
        return null;
    }

    public interface DownloaderListener {
        void onProgress(long totalBytesDownloaded);
        void onComplete();
        void onError(String message);
    }
    
    public static void download(String source, String catalog, double ra, double dec, double radius, File targetFile, DownloaderListener listener) {
        new Thread(() -> {
            try {
                String queryUrl = "";
                if ("Simbad".equalsIgnoreCase(source)) {
                    String tapUrl = "https://simbad.u-strasbg.fr/simbad/sim-tap/sync";
                    String query;
                    if (radius > 0) {
                        String rStr = String.format(java.util.Locale.US, "%.6f", ra);
                        String dStr = String.format(java.util.Locale.US, "%.6f", dec);
                        String radStr = String.format(java.util.Locale.US, "%.6f", radius);
                        query = "SELECT b.*, f.flux as flux_V FROM basic AS b LEFT JOIN flux AS f ON b.oid = f.oidref AND f.filter = 'V' WHERE CONTAINS(POINT('ICRS', b.ra, b.dec), CIRCLE('ICRS', " + rStr + ", " + dStr + ", " + radStr + "))=1 ORDER BY flux_V ASC";
                    } else {
                        query = "SELECT b.*, f.flux as flux_V FROM basic AS b LEFT JOIN flux AS f ON b.oid = f.oidref AND f.filter = 'V' ORDER BY flux_V ASC";
                    }
                    queryUrl = tapUrl + "?REQUEST=doQuery&LANG=ADQL&FORMAT=votable/td&QUERY=" + URLEncoder.encode(query, "UTF-8");
                } else {
                    if (radius > 0) {
                        queryUrl = "http://vizier.u-strasbg.fr/viz-bin/votable?-source=" + URLEncoder.encode(catalog, "UTF-8") 
                                 + "&-c=" + ra + "%20" + dec + "&-c.r=" + radius + "&-out.max=unlimited";
                    } else {
                        queryUrl = "http://vizier.u-strasbg.fr/viz-bin/votable?-source=" + URLEncoder.encode(catalog, "UTF-8") + "&-out.max=unlimited";
                    }
                }
                
                URL url = new URL(queryUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(0); // No read timeout for large downloads
                
                if (conn.getResponseCode() == 200) {
                    targetFile.getParentFile().mkdirs();
                    try (InputStream is = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[8192];
                        int n;
                        long downloaded = 0;
                        while ((n = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, n);
                            downloaded += n;
                            long finalDownloaded = downloaded;
                            javax.swing.SwingUtilities.invokeLater(() -> listener.onProgress(finalDownloaded));
                        }
                    }
                    String votableError = detectVotableError(targetFile);
                    if (votableError != null) {
                        targetFile.delete();
                        javax.swing.SwingUtilities.invokeLater(() -> listener.onError(votableError));
                    } else {
                        javax.swing.SwingUtilities.invokeLater(() -> listener.onComplete());
                    }
                } else {
                    String error = "HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage();
                    javax.swing.SwingUtilities.invokeLater(() -> listener.onError(error));
                }
            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> listener.onError(e.getMessage()));
            }
        }).start();
    }
}

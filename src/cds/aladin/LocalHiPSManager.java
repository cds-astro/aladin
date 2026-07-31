package cds.aladin;

import java.io.*;
import java.util.*;

public class LocalHiPSManager {
    
    // original URL -> local path
    private static Map<String, String> localHiPSMap = new HashMap<>();
    static { load(); }
    
    public static void load() {
        localHiPSMap.clear();
        File f = new File(System.getProperty("user.home") + File.separator + Aladin.CACHE + File.separator + "local_hips.txt");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf("==>");
                    if (idx > 0) {
                        String url = line.substring(0, idx).trim();
                        String path = line.substring(idx + 3).trim();
                        localHiPSMap.put(url.replaceAll("/+$", ""), path);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void save() {
        File dir = new File(System.getProperty("user.home") + File.separator + Aladin.CACHE);
        dir.mkdirs();
        File f = new File(dir, "local_hips.txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (Map.Entry<String, String> entry : localHiPSMap.entrySet()) {
                pw.println(entry.getKey() + "==>" + entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void addLocalHiPS(String originalUrl, String path) {
        localHiPSMap.put(originalUrl.replaceAll("/+$", ""), path);
        save();
    }
    
    public static String getLocalEquivalent(String originalUrl) {
        if (originalUrl == null) return null;
        String normalizedUrl = originalUrl.replaceAll("/+$", "");
        String path = localHiPSMap.get(normalizedUrl);
        if (path != null) {
            File f = new File(path);
            if (f.exists() && f.isDirectory()) {
                return "file://" + f.getAbsolutePath();
            }
        }
        return null;
    }
}

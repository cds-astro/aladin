// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.aladin;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Quelques fonctions utilitaires utilisés par SAMPManager
 * @author boch
 *
 */
public class SAMPUtil {

    // forbids instantiation
    private SAMPUtil() {}

    /** la méthode File.toURL produit des résultats incorrects du type file:file-path
     * au lieu de file://absolute-file-path
     *
     * Pour se simplifier la vie, on va générer qqch de la forme file://localhost/path
     *
     * @param file le fichier pour lequel on veut obtenir un objet l'URL
     * @return l'objet URL correspondant
     */
    static protected URL getURLForFile(File file) {
        try {
            String parentPath = file.getParentFile().getAbsolutePath();
            String filename = file.getName();

            if( parentPath.charAt(0) != '/' ) {
                parentPath = "/" + parentPath;
            }

            URL u;

            try {
                u = new URL("file://localhost" +
                            parentPath.replaceAll(" ", "%20") +
                            "/" + URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20"));
            }
            // should never happen
            catch(UnsupportedEncodingException uee) {
                uee.printStackTrace();
                return null;
            }

            return u;
        }
        catch(MalformedURLException e) {
            return null;
        }
    }

    /**
     * Suppression des caractères pouvant être gênants dans l'URL transmise par SAMP
     * @param fileName
     * @return
     */
    static protected String sanitizeFilename(String fileName) {
        if (fileName == null ) {
            return null;
        }
        return fileName.replaceAll("/", ".").replaceAll("[\\[|\\]]", " ").replaceAll(" ", "").replaceAll("\\+", "");
    }

}

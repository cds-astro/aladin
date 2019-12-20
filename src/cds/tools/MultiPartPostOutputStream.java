// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.tools;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPOutputStream;

/**
 * Code pour gérer l'upload de fichier en POST (encoding type: multipart/form-data)
 * comme le ferait un browser dans un navigateur
 *
 * @author T. Boch [CDS]
 *
 */
public class MultiPartPostOutputStream {
    private static final String CRLF = "\r\n";

    private static final String PREFIX = "--";

    static private String tmpDir = null;

    // output stream to write to
    private DataOutputStream out = null;

    // boundayr string between form parts
    public String boundary = null;

    /**
     * Create a new MultiPartPostOutputStream object
     *
     *
     * @param  os        the output stream
     * @param  boundary  the boundary. If null, a boundary will be created for you
     */
    public MultiPartPostOutputStream(OutputStream os, String boundary) {
        if(os == null) {
            throw new IllegalArgumentException("Output stream is required.");
        }
        if(boundary == null || boundary.length() == 0) {
            boundary = createBoundary();
        }
        this.out = new DataOutputStream(os);
        this.boundary = boundary;
    }



    /**
     * Writes an string field value.  If the value is null, an empty string
     * is sent ("").
     *
     * @param  name   the field name (required)
     * @param  value  the field value
     * @throws  java.io.IOException  on input/output errors
     */
    public void writeField(String name, String value)
            throws java.io.IOException {
        if(name == null) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if(value == null) {
            value = "";
        }

        // write boundary
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(CRLF);
        // write content header
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
        out.writeBytes(CRLF);
        out.writeBytes(CRLF);
        // write content
        out.writeBytes(value);
        out.writeBytes(CRLF);
        out.flush();
    }

    /**
     * Writes a file's contents.  If the file is null, does not exists, or
     * is a directory, a <code>java.lang.IllegalArgumentException</code>
     * will be thrown.
     *
     * @param  name      the field name
     * @param  mimeType  the file content type (optional, recommended)
     * @param  file      the file (the file must exist)
     * @param  gzip      do we compress the file ?
     * @throws  java.io.IOException  on input/output errors
     */
    public void writeFile(String name, String mimeType, File file, boolean gzip)
            throws java.io.IOException {
        if(file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }
        if(!file.exists()) {
            throw new IllegalArgumentException("File does not exist.");
        }
        if(file.isDirectory()) {
            throw new IllegalArgumentException("File cannot be a directory.");
        }
        writeFile(name, mimeType, file.getCanonicalPath(), new FileInputStream(file), gzip);
    }

    /**
     * Writes a input stream's contents.  If the input stream is null, a
     * <code>java.lang.IllegalArgumentException</code> will be thrown.
     *
     * @param  paramName the field name
     * @param  mimeType  the file content type (optional, recommended)
     * @param  fileName  the file name (required)
     * @param  is        the input stream
     * @throws  java.io.IOException  on input/output errors
     */
    public void writeFile(String paramName, String mimeType,
            String fileName, InputStream is, boolean gzip)
            throws java.io.IOException {
        if(is == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }
        if(fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }

        // write boundary
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(CRLF);
        // write content header
        out.writeBytes("Content-Disposition: form-data; name=\"" + paramName +
            "\"; filename=\"" + fileName + "\"");
        out.writeBytes(CRLF);
        if(mimeType != null) {
            out.writeBytes("Content-Type: " + (gzip?"application/x-gzip":mimeType));
            out.writeBytes(CRLF);
        }
        out.writeBytes(CRLF);

        File gzipFile = null;
        if( gzip ) {
            if( tmpDir!=null )  gzipFile = File.createTempFile("gzip", "multipart", new File(tmpDir));
            else gzipFile = File.createTempFile("gzip", "multipart");

            GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(gzipFile));

            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                gzipStream.write(buf, 0, len);
            }
            is.close();
            gzipStream.flush();
            gzipStream.close();

            is = new FileInputStream(gzipFile);
        }
        // write content
        byte[] buf = new byte[1024];
        int r = 0;
        while((r = is.read(buf, 0, buf.length)) != -1) {
            out.write(buf, 0, r);
        }
        // close input stream, but ignore any possible exception for it
        try {
            is.close();
        } catch(Exception e) {}
        out.writeBytes(CRLF);
        out.flush();

        if( gzip ) {
            gzipFile.delete();
        }
    }

    /**
     * Writes the given bytes.  The bytes are assumed to be the contents
     * of a file, and will be sent as such.  If the data is null, a
     * <code>java.lang.IllegalArgumentException</code> will be thrown.
     *
     * @param  name      the field name
     * @param  mimeType  the file content type (optional, recommended)
     * @param  fileName  the file name (required)
     * @param  data      the file data
     * @throws  java.io.IOException  on input/output errors
     */
    public void writeFile(String name, String mimeType,
            String fileName, byte[] data)
            throws java.io.IOException {
        if(data == null) {
            throw new IllegalArgumentException("Data cannot be null.");
        }
        if(fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }

        // write boundary
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(CRLF);
        // write content header
        out.writeBytes("Content-Disposition: form-data; name=\"" + name +
            "\"; filename=\"" + fileName + "\"");
        out.writeBytes(CRLF);
        if(mimeType != null) {
            out.writeBytes("Content-Type: " + mimeType);
            out.writeBytes(CRLF);
        }
        out.writeBytes(CRLF);
        // write content
        out.write(data, 0, data.length);
        out.writeBytes(CRLF);
        out.flush();
    }

    /**
     * Closes the stream.  <br />
     * <br />
     * <b>NOTE:</b> This method <b>MUST</b> be called to finalize the
     * multipart stream.
     *
     * @throws  java.io.IOException  on input/output errors
     */
    public void close() throws java.io.IOException {
        // write final boundary
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(PREFIX);
        out.writeBytes(CRLF);
        out.flush();
        out.close();
    }

    /**
     * Gets the multipart boundary string being used by this stream.
     *
     * @return  the boundary
     */
    public String getBoundary() {
        return this.boundary;
    }

    /**
     * Creates a new <code>java.net.URLConnection</code> object from the
     * specified <code>java.net.URL</code>.  This is a convenience method
     * which will set the <code>doInput</code>, <code>doOutput</code>,
     * <code>useCaches</code> and <code>defaultUseCaches</code> fields to
     * the appropriate settings in the correct order.
     *
     * @return  a <code>java.net.URLConnection</code> object for the URL
     * @throws  java.io.IOException  on input/output errors
     */
    public static URLConnection createConnection(URL url)
            throws java.io.IOException {
        URLConnection urlConn = url.openConnection();
        if(urlConn instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection)urlConn;
            httpConn.setRequestMethod("POST");
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setDefaultUseCaches(false);
        return urlConn;
    }

    /**
     * Creates a multipart boundary string by concatenating 20 hyphens (-)
     * and the hexadecimal (base-16) representation of the current time in
     * milliseconds.
     *
     * @return  a multipart boundary string
     * @see  #getContentType(String)
     */
    public static String createBoundary() {
        return "--------------------" +
            Long.toString(System.currentTimeMillis(), 16);
    }

    /**
     * Gets the content type string suitable for the
     * <code>java.net.URLConnection</code> which includes the multipart
     * boundary string.  <br />
     * <br />
     * This method is static because, due to the nature of the
     * <code>java.net.URLConnection</code> class, once the output stream
     * for the connection is acquired, it's too late to set the content
     * type (or any other request parameter).  So one has to create a
     * multipart boundary string first before using this class, such as
     * with the <code>createBoundary()</code> method.
     *
     * @param  boundary  the boundary string
     * @return  the content type string
     * @see  #createBoundary()
     */
    public static String getContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }

    public static void setTmpDir(String dir) {
        tmpDir = dir;
    }
}

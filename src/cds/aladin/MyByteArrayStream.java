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

package cds.aladin;

/**
 * <p>Title: MyByteArrayStream</p>
 * <p>Description: un OutputStream qui fournit l'InputStream correspondant a la demande </p>
 * Usage : on cree l'objet, on y ecrit. Quand on veut lire les donnees, on recupere 
 * un objet InputStream. On peut par la suite continuer a ecrire sans que l'InputStream
 * prealablement recupere n'en soit affecte.
 * 
 * Cette classe s'inspire de ByteArrayOutputStream et ByteArrayInputStream
 * @author Thomas Boch [CDS]
 * @version 0.5 (kickoff 16 June 2004)
 *
 */

import java.io.*;
  
public class MyByteArrayStream extends ByteArrayOutputStream {

    public MyByteArrayStream() {
        super();
    }

    public MyByteArrayStream(int size) {
        super(size);
    }

    public void write(String s) {
    	try {
    		write(s.getBytes());
    	}
    	catch(IOException ioe) {
    		ioe.printStackTrace();
    	}
    }
    
    /**
     * la methode "reset" n'est pas supporte
     * @throws RuntimeException cette exception est SYSTEMATIQUEMENT envoyee
     */
    public synchronized void reset() {
        throw new RuntimeException("The reset method is not supported");
    }
      
    /**
     * retourne un InputStream qui lit le contenu du tableau courant
     * La fin du stream est fixee a la taille courante du tableau
     */
    public synchronized InputStream getInputStream() {
        return new ArrayInputStream();
    }

    //-----------------------------------------------------------------------------------
    // InputStream
    //-----------------------------------------------------------------------------------

    /**
     * Cette classe est une copie de java.io.ByteArrayInputStream
     * Seule la synchronisation a ete modifiee (liee a l'objet englobant)
     */
    private class ArrayInputStream extends InputStream {

        protected int pos;
      
        protected int mark = 0;
        protected int count;
      
        public ArrayInputStream() {
            synchronized (MyByteArrayStream.this) {
                this.pos = 0;
                this.count = MyByteArrayStream.this.count;
            }
        }

        public int read() {
            synchronized (MyByteArrayStream.this) {
                return (pos < count) ? (buf[pos++] & 0xff) : -1;
            }
        }

        public int read(byte b[], int off, int len) {
            synchronized (MyByteArrayStream.this) {
                if (b == null) {
                    throw new NullPointerException();
                } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                }
                if (pos >= count) {
                    return -1;
                }
                if (pos + len > count) {
                    len = count - pos;
                }
                if (len <= 0) {
                    return 0;
                }
                System.arraycopy(buf, pos, b, off, len);
                pos += len;
                return len;
            }
        }

        public long skip(long n) {
            synchronized (MyByteArrayStream.this) {
                if (pos + n > count) {
                    n = count - pos;
                }
                if (n < 0) {
                    return 0;
                }
                pos += n;
                return n;
            }
        }

        public int available() {
            synchronized (MyByteArrayStream.this) {
                return count - pos;
            }
        }

        public boolean markSupported() {
            return true;
        }

        public void mark(int readAheadLimit) {
            synchronized (MyByteArrayStream.this) {
                mark = pos;
            }
        }

        public void reset() {
            synchronized (MyByteArrayStream.this) {
                pos = mark;
            }
        }

        public void close() throws IOException {}
        
    } // fin classe interne ArrayInputStream
} 

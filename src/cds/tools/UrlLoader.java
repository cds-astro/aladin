// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//



package cds.tools;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Permet la lecture du contenu d'une URL avec un timeout (qui marche)
 * @author Pierre Fernique [CDS] - décembre 2010
 */
public class UrlLoader extends Thread {
   
   private URL url;                                      // URL a interroger
   private StringBuffer res=new StringBuffer();          // va contenir le résultat
   private long timeout;                                 // timeout en ms
   private String error=null;                            // erreur éventuelle, null si aucune
   private HttpURLConnection conn=null;               
   private InputStream is=null;
   private boolean isWaiting=true;                       // false si l'URL a été totalement appelé et récupéré
   
   /** Création d'un URL loader - ne peut être utilisée qu'une fois
    * @param url Url a lire
    * @param timeout temps maximum pour la lecture (en ms)
    */
   public UrlLoader(URL url,int timeout) {
      this.timeout = timeout;
      this.url = url;
   }
   
   /** Demande la lecture effective de l'URL */
   public String getData() throws Exception {
      long t1 = System.currentTimeMillis();
//      System.out.println("Pere en attente de résultat...");
      start();
      while( isWaiting && System.currentTimeMillis()-t1 < timeout ) {
//         System.out.println("Pere waiting..."+(System.currentTimeMillis()-t1));
         try { Thread.currentThread().sleep(500); } catch( Exception e) {}
      }
      if( isWaiting ) {
//         System.out.println("Fils trop long, le pere disconnecte le fils !");
         try {
            if( conn!=null ) conn.disconnect();
            if( is!=null ) is.close();
         } catch( Exception e ) { }
         error = "Timeout";
      }
      if( error!=null ) {
//         System.out.println("Pere throws this error : "+error);
         throw new Exception(error);
      }
//      System.out.println("Pere ok ["+res+"]");
      return res.toString();
   }
   
   
   public void run() {
      try {
//         System.out.println("Fils créé...");
         conn = (HttpURLConnection) url.openConnection();
         is = conn.getInputStream();
         byte [] buf = new byte[512];
         int n;
         while( (n=is.read(buf))!=-1 ) {
//            System.out.println("Fils en lecture...");
            res.append( new String(buf,0,n) );
         }
         is.close();
      } catch( Exception e ) {
         error = e.getMessage();
      }
      isWaiting=false;
//      System.out.println("Fils meurt...");
   }
}

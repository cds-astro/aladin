// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import cds.tools.MultiPartPostOutputStream;

/** Spécialisation de la classe GluServer pour le cas particulier de Sextractor
 * @version 1.0 - 1 fév 2022 - création
 */
public class ServerSextractor extends ServerGlu {
   
   static private String IMG = "img";
   
   private Plan planImage=null;  // Le plan image concerné   

   protected ServerSextractor(Aladin aladin, String A, String D, String MV,
         String MP, String ML, String LP, String PP, String FU,
         String [] PD, String [] PK, String [] PV, String R, String MI,
		 String [] AF, String AL,StringBuffer record) {

      super(aladin,A,D,MV,MP,ML,LP,PP,FU,PD,PK,PV,null, R, -1 ,MI,AF,AL,null,null,record,null,null, null, null, false);
   }

   // Pas utilisable dans le bouton "ALL VO"
   protected boolean isDiscovery() { return false; }
   
   /** Mémorise le plan concerné par le flux POST multipart (1 seule image) */
   protected String getUrl4LocalData(Plan p) {
      planImage = p;
      return IMG;
   }
   
   /** Appel effectif pour la création du plan.
    * voir super.callCreatePlan. Cette méthode est une surcharge pour transformer
    * une requête en POST alors qu'elle était prévue en GET 
    */
   protected Plan callCreatePlan(String u,String label, String orig, Server server) {
      waitCursor();

      Plan res=null;

      // On transforme l'Url GET en flux POST
      int qMark = u.indexOf('?');
      String baseUrl = u.substring(0,qMark);
      String params = u.substring(qMark+1);

      // DEBUG
//      baseUrl = "http://alasky.cds.unistra.fr/cgi/sextractor-new.pl";

      try {
         MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
         String boundary = MultiPartPostOutputStream.createBoundary();
         URLConnection urlConn = MultiPartPostOutputStream.createConnection( new URL(baseUrl ));
         urlConn.setRequestProperty("Accept", "*/*");
         urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
         urlConn.setRequestProperty("Connection", "Keep-Alive");
         urlConn.setRequestProperty("Cache-Control", "no-cache");
         MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

         // Liste des paramètres
         StringTokenizer st = new StringTokenizer(params, "&");
         while( st.hasMoreTokens() ) {
            String p = st.nextToken();
            int egal = p.indexOf('=');
            String name = p.substring(0,egal);
            String val  = URLDecoder.decode( p.substring(egal+1) );
            out.writeField(name, val);
         }

         // Post de l'image
         if( aladin.save==null ) aladin.save = new Save(aladin);
         out.writeFile(IMG, "image/fits", IMG,
               aladin.save.saveImageFITS((OutputStream)null, (PlanImage)planImage), true);

         out.close();

         res=aladin.calque.createPlanCatalog( new MyInputStream( urlConn.getInputStream()), label );
         
// DEBUG
//         InputStream in = urlConn.getInputStream();
//         byte [] buf = new byte[512];
//         int n;
//         while( (n=in.read(buf))>0 ) {
//            System.out.print( new String(buf,0,n) );
//         }
//         in.close();
//         throw new Exception("debug");
         
      } catch(Exception ioe) {
         if( aladin.levelTrace>=3 ) ioe.printStackTrace();
      }
      defaultCursor();
      
      // traitement d'une erreur éventuelle
      if( res==null ) {
         Aladin.error("An error occured while contacting the Sextractor service");
         return null;
      }

      return res;
   }

      
}

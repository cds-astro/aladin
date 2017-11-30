// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.allsky;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.Util;

/** Recopie d'un HiPS distant via HTTP
 * @author Pierre Fernique
 * @version 1.0 juin 2015 création
 */
public class BuilderMirror extends BuilderTiles {

   private Fits bidon;                  // Ne sert qu'à renvoyer quelque chose pour faire plaisir à BuilderTiles
   private MyProperties prop;           // Correspond aux propriétés distantes
   private boolean isPartial=false;     // indique que la copie sera partielle (spatialement, order ou format des tuiles)
   private boolean isSmaller=false;     // indique que la copie concerne une zone spatial plus petite que l'original
   private boolean isUpdate=false;      // true s'il s'agit d'une maj d'un HiPS déjà copié
   private boolean flagIsUpToDate=false;      // true s'il s'agit d'une maj d'un HiPS déjà copié et déjà à jour
   private String dateRelease="";       // Date of last release date of the local copy
   private boolean isLocal=false;       // true s'il s'agit d'une copie locale

   public BuilderMirror(Context context) {
      super(context);
   }

   public Action getAction() { return Action.MIRROR; }

   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      validateOutput();
      
      // Détermination d'une éventuelle copie locale
      String dir = context.getInputPath();
      isLocal = !dir.startsWith("http://") && !dir.startsWith("https://") 
            && !dir.startsWith("ftp://");
      if( isLocal ) context.info("Local mirror copy");

      // Chargement des propriétés distantes
      prop = new MyProperties();
      MyInputStream in = null;
      
      InputStreamReader in1=null;
      try {
         in1 = new InputStreamReader( Util.openAnyStream( context.getInputPath()+"/properties"), "UTF-8" );
         prop.load(in1);
      } finally{  if( in1!=null ) in1.close(); }

      // Détermination du statut
      String s = prop.getProperty(Constante.KEY_HIPS_STATUS);
      if( s!=null && context.testClonable && s.indexOf("unclonable")>=0 ) {
         throw new Exception("This HiPS is unclonable => status: "+s);
      }

      // Détermination de l'ordre max: si non précisé, récupéré depuis
      // les propriétés distantes
      s = prop.getProperty(Constante.KEY_HIPS_ORDER);
      if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_ORDER);
      if( s==null ) context.warning("No order specified in the remote HiPS properties file !");
      int o = s==null ? -1 : Integer.parseInt(s) ;
      int paramO = context.getOrder();
      if( paramO ==-1 ) {
         if( o==-1 ) throw new Exception("Order unknown !");
         context.setOrder(o);
      } else {
         if( o!=-1 ) {
            if( paramO>o ) throw new Exception("Order greater than the original");
            else if( o!=paramO ) isPartial=true;
         }
      }

      // Détermination des types de tuiles: si non précisé, récupéré
      // depuis les propriétés distantes
      s = prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
      if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_TILE_FORMAT);
      if( context.tileTypes==null ) {
         if( s==null ) throw new Exception("tile format unknown");
         Tok tok = new Tok(s);
         while( tok.hasMoreTokens() ) context.addTileType(tok.nextToken());
      } else {
         if( s!=null && !context.getTileTypes().equals(s) ) isPartial=true;
      }
      context.info("Mirroring tiles: "+context.getTileTypes()+"...");

      // Détermination du Moc
      HealpixMoc area = new HealpixMoc();
      in = null;
      try {
         in = Util.openAnyStream( context.getInputPath()+"/Moc.fits");
         area.read(in);
         if( context.getArea()==null ) {
            context.setMocArea( area );
         } else {
            if( !context.getArea().equals(area)) {
               isSmaller=isPartial=true;
               context.setMocArea( area.intersection( context.getArea()) );
               context.info("Partial spacial mirror");
            }
         }
      } finally{  if( in!=null ) in.close(); }

      // Mode couleur ou non
      s = prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE);
      if( s!=null ) { if( s.equals("color")) context.setBitpixOrig(0); }
      else {
         s = prop.getProperty(Constante.OLD_ISCOLOR);
         if( s==null ) s = prop.getProperty("isColor");
         if( s!=null && s.equals("true")) context.setBitpixOrig(0);
      }
      if( context.isColor() ) context.info("Mirroring colored HiPS");

      // référence spatiale
      s = prop.getProperty(Constante.KEY_HIPS_FRAME);
      if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_FRAME);
      if( s!=null ) context.setFrameName(s);

      // Cube ?
      s = prop.getProperty(Constante.KEY_CUBE_DEPTH);
      if( s==null ) s = prop.getProperty(Constante.OLD_CUBE_DEPTH);
      if( s!=null ) {
         int depth = Integer.parseInt(s);
         context.setDepth(depth);
      }
      if( context.isCube() ) context.info("Mirroring cube HiPS (depth="+context.depth+")");

      // Détermination de la zone à copier
      context.moc = context.mocArea;
      context.setValidateRegion(true);

      // Peut être existe-t-il déjà une copie locale à jour ?
      if( (new File(context.getOutputPath()+"/properties")).exists() ) {
         MyProperties localProp = new MyProperties();
         in1 = null;
         try {
            in1 = new InputStreamReader( Util.openAnyStream( context.getOutputPath()+"/properties") , "UTF-8");
            localProp.load(in1);

            String dLocal = localProp.getProperty(Constante.KEY_HIPS_RELEASE_DATE);
            String dRemote = prop.getProperty(Constante.KEY_HIPS_RELEASE_DATE);
            if( dLocal!=null && dRemote!=null && dLocal.equals(dRemote) ) {
               dateRelease=dLocal;
               flagIsUpToDate=true && !isSmaller && !isPartial;
//               throw new Exception("Local copy already up-to-date ("+dLocal+") => "+context.getOutputPath());
            }

            // IL FAUDRAIT VERIFIER ICI QUE
            //   1) SI LE MOC LOCAL COUVRE UNE ZONE EN DEHORS DU MODE DISTANT
            //   2) SI L'ORDER LOCAL EST PLUS GRAND QUE L'ORDER DISTANT
            //   3) SI LES TYPES DE TUILES EN LOCAL NE SONT PLUS DISTRIBUES PAR LE SITE DISTANT
            // => ALORS IL FAUDRAIT FAIRE LE MENAGE EN LOCAL, OU FORCER UN CLEAN LOCAL AVANT

            isUpdate=true;
            context.info("Updating a previous HiPS copy ["+context.getOutputPath()+"]...");

         } finally{ if( in1!=null ) in1.close(); }
      }
   }

   public void run() throws Exception {
      if( flagIsUpToDate ) {
         context.info("Local HiPS copy seems to be already up-to-date (same hips_release_date="+dateRelease+")");
         context.info("Only the properties file will be updated");
      } else {
         build();

         if( !context.isTaskAborting() ) {

            copyX(context.getInputPath()+"/index.html",context.getOutputPath()+"/index.html");
            copyX(context.getInputPath()+"/preview.jpg",context.getOutputPath()+"/preview.jpg");
            
            // On recopie simplement le MOC, sauf si copie partielle, ou erreur
            // et dans ce cas, on le recalcule.
            try {
               if( isSmaller ) throw new Exception();
               copy(context.getInputPath()+"/Moc.fits",context.getOutputPath()+"/Moc.fits");
            } catch( Exception e ) {
               (b=new BuilderMoc(context)).run(); b=null;
            }
            copyAllsky();

            //  regeneration de la hierarchie si nécessaire
            if( isSmaller ) {
               b = new BuilderTree(context);
               b.run();
               b=null;
            }
         }

         // Nettoyage des vieilles tuiles (non remise à jour)
         // CA NE VA PAS MARCHER CAR CERTAINES TUILES PEUVENT NE PAS AVOIR ETE MIS A JOUR SUR LE SERVEUR DISTANT
         // BIEN QUE LA RELEASE DATE AIT EVOLUEE. ELLES SERONT ALORS SUPPRIMEES PAR ERREUR

         //      if( isUpdate && !context.isTaskAborting()) {
         //         b=new BuilderCleanDate(context);
         //         ((BuilderCleanDate)b).setDate(lastReleaseDate);
         //         b.run();
         //         b=null;
         //      }
      }

      // Maj des properties
      if( !context.isTaskAborting() ) {

         prop.remove(Constante.KEY_HIPS_SERVICE_URL);
         prop.remove(Constante.KEY_MOC_ACCESS_URL);
         prop.remove(Constante.KEY_HIPS_ESTSIZE);

         double skyFraction = context.moc.getCoverage();
         prop.replaceValue(Constante.KEY_MOC_SKY_FRACTION, Util.myRound( skyFraction ) );

         prop.replaceValue(Constante.KEY_HIPS_TILE_FORMAT, context.getTileTypes() );

         String status = prop.getProperty(Constante.KEY_HIPS_STATUS);
         StringBuilder status1;
         if( status==null ) status1 = new StringBuilder(Constante.PUBLIC+" "+Constante.MIRROR+" "+Constante.CLONABLEONCE);
         else {
            Tok tok = new Tok(status);
            status1 = new StringBuilder();
            while( tok.hasMoreTokens() ) {
               String s = tok.nextToken();
               if( s.equals(Constante.MASTER)) s= isPartial ? Constante.PARTIAL : Constante.MIRROR;
               if( s.equals(Constante.CLONABLEONCE) ) s=Constante.UNCLONABLE;
               if( status1.length()>0 ) status1.append(' ');
               status1.append(s);
            }
         }
         prop.replaceValue(Constante.KEY_HIPS_STATUS, status1.toString());

         OutputStreamWriter out = null;
         try {
            out = new OutputStreamWriter( new FileOutputStream( context.getOutputPath()+"/properties"), "UTF-8");
            prop.store( out, null);
         } finally {  if( out!=null ) out.close(); }
      }
      
      

   }

   int statNbFile=0;
   long statCumul=0L;
   long lastCumul=0L;
   long lastTime=0L;

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      if( flagIsUpToDate ) return;
      long t = System.currentTimeMillis();
      long delai = t-lastTime;
      long lastCumulPerSec = delai>1000L && lastTime>0 ? lastCumul/(delai/1000L) : 0L;
      lastTime=t;
      lastCumul=0;
      context.showMirrorStat(statNbFile, statCumul, lastCumulPerSec, totalTime,
            statNbThread,statNbThreadRunning);
   }

   public void build() throws Exception {
      bidon = new Fits();
      initStat();
      super.build();
   }

   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z) throws Exception {
      return createLeaveHpx(hpx,file,path,order,npix,z,true);
   }

   private Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z,boolean stat) throws Exception {
      String fileInX = context.getInputPath()+"/"+cds.tools.pixtools.Util.getFilePath(order,npix,z);

      try {
         long size=0L;
         for( String ext : context.tileTypes ) {
            String fileIn = fileInX+ext;
            String fileOut = file+ext;
            size+=copy(fileIn,fileOut);
         }
         if( stat ) updateStat(size);
      } catch( Exception e ) {
         e.printStackTrace();
         context.taskAbort();
      }
      return bidon;
   }

   // Copie des Allsky
   private void copyAllsky() throws Exception {
      for( int z=0; z<context.depth; z++) {
         for( String ext : context.tileTypes ) {
            String suf = z==0 ? "" : "_"+z;
            String fileIn = context.getInputPath()+"/Norder3/Allsky"+suf+ext;
            String fileOut = context.getOutputPath()+"/Norder3/Allsky"+suf+ext;
            copyX(fileIn,fileOut);
         }
      }
   }

   // Copie d'un fichier distant (url) vers un fichier local sans générer d'exception
   private int copyX(String fileIn, String fileOut) throws Exception {
      try { return copy(fileIn,fileOut); } catch( Exception e) {};
      return 0;
   }
   
   private int copy(String fileIn, String fileOut) throws Exception {
      try {
         if( isLocal ) return copyLocal(fileIn,fileOut);
         return copyRemote(fileIn,fileOut);
         
      } catch( FileNotFoundException e ) {
         context.warning("File not found ["+fileIn+"] => ignored (may be out of the MOC)");
      }
      return 0;
   }
   
//   static public void main(String [] s) {
//      try {
//         String fileIn="http://alasky.u-strasbg.fr/SDSS/DR9/color/Norder10/Dir20000/Npix28115.jpg";
////         String fileIn="http://alasky.u-strasbg.fr/SDSS/DR9/color/Norder10/Dir20000/Npix28124.jpg";
//         String fileOut="/Users/Pierre/Desktop/toto.jpg";
//         int size = copyRemote(fileIn,fileOut);
//         System.out.println("copy done => "+size);
//      } catch( Exception e ) {
//         
//         e.printStackTrace();
//      }
//      
//   }

   // Copie d'un fichier distant (url) vers un fichier local, uniquement si la copie locale évenutelle
   // et plus ancienne et/ou de taille différente à l'originale.
   // Vérifie si possible que la taille du fichier copié est correct.
   // Effectue 3 tentatives consécutives avant d'abandonner
   private int copyRemote(String fileIn, String fileOut) throws Exception {
      File fOut;
      long lastModified;
      long size=-1;
      byte [] buf=null;

      // Laisse-t-on souffler un peu le serveur HTTP ?
      try {
         if( context.mirrorDelay>0 ) Thread.currentThread().wait(context.mirrorDelay);
      } catch( Exception e ) { }

      for( int i=0; i<3; i++ ) {
         MyInputStream dis=null;
         try {
            URL u = new URL(fileIn);
            URLConnection conn = u.openConnection();
            HttpURLConnection httpc = (HttpURLConnection)conn;
            httpc.setRequestMethod("HEAD");
            lastModified = httpc.getLastModified();

            fOut = new File(fileOut);
            if( fOut.exists() && fOut.length()>0 ) {
               size = httpc.getContentLength();
               if( size==fOut.length() && lastModified<=fOut.lastModified() ) {
//                  httpc.disconnect();   => Coupe le keep-alive
                  
                  // On doit tout de même vider le buffer
                  InputStream es = httpc.getInputStream();
                  try {
                     byte [] buf1 = new byte[512];
                     while( es.read(buf1) > 0) { }
                     es.close();  
                     
                  // Et le buffer d'erreur éventuel
                  } catch( IOException e ) {
                     es = httpc.getErrorStream();
                     byte [] buf1 = new byte[512];
                     while( es.read(buf1) > 0) { }
                     es.close();  
                  }
                  return 0;  // déjà fait
               }
            }
            
            httpc = (HttpURLConnection)u.openConnection();
            httpc.setRequestMethod("GET");
            dis = new MyInputStream(httpc.getInputStream());
            try {
               buf = dis.readFully();
               dis.close();
               dis=null;
               
            // Vidange du flux d'erreur => nécessaire pour Keepalive
            } catch( IOException e ) {
               InputStream es = httpc.getErrorStream();
               byte [] buf1 = new byte[512];
               while( es.read(buf1) > 0) { }
               es.close();  
            }
         
         } finally { if( dis!=null ) try{ dis.close(); } catch( Exception e) {}  }

         RandomAccessFile f = null;
         try {
            Util.createPath(fileOut);
            f = new RandomAccessFile(fileOut, "rw");
            f.write(buf);
            f.close();
            f=null;
         } finally { if( f!=null ) try{ f.close(); } catch( Exception e) {} }
         fOut.setLastModified(lastModified);

         if( size>0 && (new File(fileOut)).length()<size) {
            if( i==2 ) throw new Exception("Truncated file copy");
            context.warning("Truncated file copy => try again ["+fileIn+"]");
         }
         else break;  // a priori c'est bon
      }

      return buf==null ? 0 : buf.length;

   }
   
   // Copie d'un fichier local (path) vers un fichier local, uniquement si la copie évenutelle
   // et plus ancienne et/ou de taille différente à l'originale.
   private int copyLocal(String fileIn, String fileOut) throws Exception {
      File fOut,fIn;
      long lastModified;
      long size=-1;
      int n;

      fIn = new File(fileIn);
      lastModified = fIn.lastModified();
      fOut = new File(fileOut);

      // Si même taille et date antérieure ou égale => déjà fait
      if( fOut.exists() && fOut.length()>0 ) {
         size = fIn.length();
         if( size==fOut.length() && lastModified<=fOut.lastModified() ) {
            return 0;  // déjà fait
         }
      }

      // Copie par bloc
      size=0L;
      RandomAccessFile f = null;
      RandomAccessFile g = null;
      byte [] buf=new byte[512];
      try {
         Util.createPath(fileOut);
         f = new RandomAccessFile(fileOut, "rw");
         g = new RandomAccessFile(fileIn, "r");
         while( (n=g.read(buf))>0 ) { f.write(buf,0,n); size+=n; }
         f.close(); f=null;
         g.close(); g=null;
      } finally { 
         if( f!=null ) try{ f.close(); } catch( Exception e) {} 
         if( g!=null ) try{ g.close(); } catch( Exception e) {} 
      }
      fOut.setLastModified(lastModified);

      return (int)size;

   }


   // Dans le cas d'un mirroir complet, on copie également les noeuds. En revanche pour un miroir partiel
   // on regénérera l'arborescence à la fin
   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[], int z) throws Exception {
      if( !isSmaller ) return createLeaveHpx(null,file,path,order,npix,z,false);
      return bidon;
   }

   /** Recherche et chargement d'un losange déjà calculé
    *  Retourne null si non trouvé
    * @param file Nom du fichier ( sans extension)
    */
   public Fits findLeaf(String file) throws Exception { return null; }

   private void initStat() {
      statNbFile=0;
      statCumul=0L;
      startTime = System.currentTimeMillis();
   }

   // Mise à jour des stats
   private void updateStat(long size) {
      statNbFile++;
      lastCumul+=size;
      statCumul+=size;
      totalTime = System.currentTimeMillis()-startTime;
   }
}

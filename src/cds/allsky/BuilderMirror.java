// Copyright 2012 - UDS/CNRS
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
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
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

   public BuilderMirror(Context context) {
      super(context);
   }

   public Action getAction() { return Action.MIRROR; }

   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      validateOutput();

      // Chargement des propriétés distantes
      prop = new MyProperties();
      MyInputStream in = null;
      try {
         in = Util.openAnyStream( context.getInputPath()+"/properties");
         prop.load(in);
      } finally{  if( in!=null ) in.close(); }

      // Détermination de l'ordre max: si non précisé, récupéré depuis
      // les propriétés distantes
      String s = prop.getProperty(Constante.KEY_HIPS_ORDER);
      if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_ORDER);
      if( s==null ) throw new Exception("Order max unknown");
      int o = Integer.parseInt(s) ;
      int paramO = context.getOrder();
      if( paramO ==-1 ) {
         context.setOrder(o);
      } else {
         if( paramO>o ) throw new Exception("Order greater than the original");
         else if( o!=paramO ) isPartial=true;
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
         if( !context.getTileTypes().equals(s) ) isPartial=true;
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
               context.info("Partial spacial mirror => tile hierarchy will be locally recomputed");
            }
         }
      } finally{  if( in!=null ) in.close(); }

      // Mode couleur ou non
      s = prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE);
      if( s!=null ) { if( s.equals("color")) context.setBitpixOrig(0); }
      else {
         s = prop.getProperty(Constante.OLD_ISCOLOR);
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
         in = null;
         try {
            in = Util.openAnyStream( context.getOutputPath()+"/properties");
            localProp.load(in);

            String dLocal = localProp.getProperty(Constante.KEY_HIPS_RELEASE_DATE);
            String dRemote = prop.getProperty(Constante.KEY_HIPS_RELEASE_DATE);
            if( dLocal!=null && dRemote!=null && dLocal.equals(dRemote) ) {
               throw new Exception("Local copy already up-to-date ("+dLocal+") => "+context.getOutputPath());
            }
         } finally{  if( in!=null ) in.close(); }
      }
   }

   public void run() throws Exception {
      build();

      if( !context.isTaskAborting() ) { (b=new BuilderMoc(context)).run(); b=null; }
      if( !context.isTaskAborting() ) {

         copyX(context.getInputPath()+"/index.html",context.getOutputPath()+"/index.html");
         copyAllsky();

         //  regeneration de la hierarchie si nécessaire
         if( isSmaller ) {
            b = new BuilderTree(context);
            b.run();
            b=null;
         }
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
         Tok tok = new Tok(status);
         StringBuilder status1 = new StringBuilder();
         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equals("master")) s= isPartial ? "partial" : "mirror";
            if( status1.length()>0 ) status1.append(' ');
            status1.append(s);
         }
         prop.replaceValue(Constante.KEY_HIPS_STATUS, status1.toString());

         FileOutputStream out = null;
         try {
            out = new FileOutputStream( context.getOutputPath()+"/properties");
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
      long t = System.currentTimeMillis();
      long delai = t-lastTime;
      long lastCumulPerSec = delai>1 && lastTime>0 ? lastCumul/(delai/1000L) : 0L;
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

   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix, int z) throws Exception {
      return createLeaveHpx(hpx,file,order,npix,z,true);
   }

   private Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix, int z,boolean stat) throws Exception {
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

   // Copie d'un fichier distant (url) vers un fichier local, uniquement si la copie locale évenutelle
   // et plus ancienne et/ou de taille différente à l'originale.
   // Vérifie si possible que la taille du fichier copié est correct.
   // Effectue 3 tentatives consécutives avant d'abandonner
   private int copy(String fileIn, String fileOut) throws Exception {
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
            lastModified = conn.getLastModified();

            fOut = new File(fileOut);
            if( fOut.exists() && fOut.length()>0 ) {
               size = conn.getContentLength();
               if( size==fOut.length() && lastModified<=fOut.lastModified() ) return 0;  // déjà fait
            }

            dis = new MyInputStream(conn.getInputStream());
            buf = dis.readFully();
         } finally { if( dis!=null ) dis.close(); }

         RandomAccessFile f = null;
         try {
            Util.createPath(fileOut);
            f = new RandomAccessFile(fileOut, "rw");
            f.write(buf);
         } finally { if( f!=null ) f.close(); }
         fOut.setLastModified(lastModified);

         if( size>0 && (new File(fileOut)).length()<size) {
            if( i==2 ) throw new Exception("Truncated file copy");
            context.warning("Truncated file copy => try again ["+fileIn+"]");
         }
         else break;  // a priori c'est bon
      }

      return buf==null ? 0 : buf.length;

   }

   // Dans le cas d'un mirroir complet, on copie également les noeuds. En revanche pour un miroir partiel
   // on regénérera l'arborescence à la fin
   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[], int z) throws Exception {
      if( !isSmaller ) return createLeaveHpx(null,file,order,npix,z,false);
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
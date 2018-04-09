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

package cds.allsky;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import cds.aladin.Aladin;
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
   
   static private final int TIMEOUT = 15000;    // 15 sec sans nouvelle on réinitialise la connection HTTP
   static private final int MAXRETRY = 10;      // Nombre max de réinitialisations possibles avant panique

   private Fits bidon;                  // Ne sert qu'à renvoyer quelque chose pour faire plaisir à BuilderTiles
   private MyProperties prop;           // Correspond aux propriétés distantes
   private boolean isPartial=false;     // indique que la copie sera partielle (spatialement, order ou format des tuiles)
   private boolean isSmaller=false;     // indique que la copie concerne une zone spatial plus petite que l'original
   private boolean isUpdate=false;      // true s'il s'agit d'une maj d'un HiPS déjà copié
   private boolean flagIsUpToDate=false;      // true s'il s'agit d'une maj d'un HiPS déjà copié et déjà à jour
   private String dateRelease="";       // Date of last release date of the local copy
   private boolean isLocal=false;       // true s'il s'agit d'une copie locale
   private long timeIP;
   private boolean check=false;       // true si on redémarre une session => pas de test de taille sur les tuiles déjà arrivées

   public BuilderMirror(Context context) {
      super(context);
   }

   public Action getAction() { return Action.MIRROR; }
   
   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      
      // Détermination d'une éventuelle copie locale
      String dir = context.getInputPath();
      check = context.getMirrorCheck();
      isLocal = !dir.startsWith("http://") && !dir.startsWith("https://") && !dir.startsWith("ftp://");
      if( isLocal ) context.info("Local mirror copy");
      if( !isLocal && check ) context.info("Will check all date and size for already loaded tiles");

      // Chargement des propriétés distantes
      prop = new MyProperties();
      MyInputStream in = null;
      
      InputStreamReader in1=null;
      try {
         in1 = new InputStreamReader( Util.openAnyStream( context.getInputPath()+"/properties"), "UTF-8" );
         prop.load(in1);
      } finally{  if( in1!=null ) in1.close(); }
      
      // On valide le répertoire de destination
      validateOutput();
      
      // Détermination du statut
      String s = prop.getProperty(Constante.KEY_HIPS_STATUS);
      if( s!=null && context.testClonable && s.indexOf("unclonable")>=0 ) {
         throw new Exception("This HiPS is unclonable => status: "+s);
      }

      // Détermination du type de HiPS
      s = prop.getProperty("dataproduct_type");
      if( s!=null && s.indexOf("catalog")>=0 ) {
         throw new Exception("Hipsgen mirror not usable for catalog HiPS");
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

   private int statNbFile=0;
   private long statCumul=0L;
   private long lastCumul=0L;
   private long lastTime=0L;
   private long timeIPArray[];
   private int timeIPindex=0;
   private final int MAXTIMEIP=50;
   
   private final int MAXMESURE=3;
   private int nbMesure=0;
   private long mesure[] = new long[ MAXMESURE ];
   boolean acceleration = true;    // true= accélération, false = déccélération
   private long lastMesure=0L;

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      if( flagIsUpToDate ) return;
      long t = System.currentTimeMillis();
      long delai = t-lastTime;
      long lastCumulPerSec = delai>1000L && lastTime>0 ? lastCumul/(delai/1000L) : 0L;
      lastTime=t;
      lastCumul=0;
      long lastTimeIP=0L;
      if( statNbFile>=MAXTIMEIP ) {
         t=0L;
         for( long a : timeIPArray ) t+=a;
         lastTimeIP = t/MAXTIMEIP;
      }
      
      int nbThreads = getNbThreads();
      int statNbThreadRunning = getNbThreadRunning();
      
      int maxThreads = (lastTimeIP==0?20:64);
      int max = context.getMaxNbThread();
      if( max!=-1 && maxThreads>max ) maxThreads=max;
      int minThreads=16;
      
      // Ajustement du nombre de threads pour optimiser le débit
      if( !isLocal && statNbFile>1 && nbThreads>=0 && maxThreads>minThreads ) {
         
         // PLUS ou MOINS de threads ?
         // 1) on mérorise le débit instantanné
         // 2) on change le nombre de threads en fonction du mode courant
         // 3) on mesure le nouveau débit instantanné sur une période de temps suffisante longue
         // 4) si moins bien qu'avant on inverse le mode
         // 5) en bout de cours (max ou min) on inverse le mode
         
         if( nbMesure<MAXMESURE ) mesure[ nbMesure++ ] = lastCumulPerSec;
         else {
            long moyenne=0L;
            for( long m : mesure ) moyenne += m/nbMesure;
            if( lastMesure!=0 && moyenne<lastMesure ) acceleration = !acceleration;   // Changement de sens
            try {
               if( context.getVerbose()>=3 ) context.info("MODE "+(acceleration?"acceleration":"deceleration")
                     +" lastMeasure="+Util.getUnitDisk(lastMesure)+" newMeasure="+Util.getUnitDisk(moyenne));
               if( acceleration ) {
                  if( nbThreads<maxThreads ) addThreadBuilderHpx( nbThreads+4<=maxThreads? 4 : maxThreads-nbThreads);
               } else {
                  if( nbThreads>minThreads ) removeThreadBuilderHpx(2);
               }
            } catch( Exception e) { e.printStackTrace(); }
            
            // Si on est au max (resp. au min) on change de sens
            if( nbThreads<=minThreads ) acceleration=true;
            if( nbThreads>=maxThreads ) acceleration=false;
            
            lastMesure=moyenne;
            nbMesure=0;
         }

      }
      
      context.showMirrorStat(statNbFile, statCumul, lastCumulPerSec, totalTime, nbThreads,statNbThreadRunning, lastTimeIP);
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
            size+=copy(hpx,order,fileIn,fileOut);
         }
         if( stat ) updateStat(size,timeIP);
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
   
   private int copy(String fileIn, String fileOut) throws Exception { return copy(null,-1,fileIn,fileOut); }
   private int copy(ThreadBuilderTile hpx, int order,String fileIn, String fileOut) throws Exception {
      try {
         if( isLocal ) return copyLocal(fileIn,fileOut);
         return copyRemote(hpx,fileIn,fileOut);
         
      } catch( FileNotFoundException e ) {
         if( order>=3 ) context.warning("File not found ["+fileIn+"] => ignored (may be out of the MOC)");
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

   
   class TimeOut extends Thread {
      HttpURLConnection con;
      long lastSize=0;
      long size=0;
      boolean encore=true;
      int timeout;
      String file;

      TimeOut(HttpURLConnection con,String file, int timeout) { this.timeout=timeout; this.file=file; this.con = con; }

      void end() { encore=false; this.interrupt(); }

      private long getBytes() { return size; }
      void setSize(long size) { this.size=size; }

      public void run() {
         while( encore ) {
            try { Thread.sleep(timeout); } catch (InterruptedException e) { }
            if( encore ) {
               long size = getBytes();
               
               // Rien lu depuis la dernière fois ?
               if( size==lastSize ) {
                  con.disconnect();
                  encore=false;
               }
               lastSize=size;
            }
         }
      }
   }
   
   // Copie d'un fichier distant (url) vers un fichier local, uniquement si la copie locale évenutelle
   // et plus ancienne et/ou de taille différente à l'originale.
   // Vérifie si possible que la taille du fichier copié est correct.
   // Effectue 3 tentatives consécutives avant d'abandonner
   private int copyRemote(ThreadBuilderTile hpx, String fileIn, String fileOut) throws Exception {
      File fOut=null;
      long lastModified=0L;
      int size=0;
      int sizeRead=0;
      int n;
      long len;
      byte [] buf = new byte[512];

      // Laisse-t-on souffler un peu le serveur HTTP ?
      try {
         if( context.mirrorDelay>0 ) Thread.currentThread().wait(context.mirrorDelay);
      } catch( Exception e ) { }

      for( int i=0; i<MAXRETRY; i++ ) {
         InputStream dis=null;
         RandomAccessFile f = null;
         TimeOut timeout = null;
         HttpURLConnection httpc=null;
         
         try {
            lastModified=-1;

            URL u = new URL(fileIn);
            fOut = new File(fileOut);
            
//            if( i>0 ) context.warning("Reopen connection for "+fileIn+" ...");
   
            // Si on a déjà la tuile, on vérifie qu'elle est à jour (date et taille)
            if( fOut.exists() && (len=fOut.length())>0 ) {
               
               // reprise => pas de vérif date&size des tuiles déjà arrivées
               // On garde un vieux doute sur les fichiers vraiments petits
               // ON POURRAIT VERIFIER QUE LE FICHIER N'EST PAS TRONQUE EN CHARGEANT LA TUILE SANS ERREUR MAIS CA VA PRENDRE DES PLOMBES...
               if( !check && len>1024L)  return 0;
               
               httpc = (HttpURLConnection)u.openConnection();
               timeout = new TimeOut(httpc,fileIn,TIMEOUT);
               timeout.start();
               httpc.setReadTimeout(TIMEOUT-500);
               httpc.setConnectTimeout(TIMEOUT-500);
               httpc.setRequestProperty("User-Agent", "Aladin/Hipsgen/"+Aladin.VERSION);
               httpc.setRequestMethod("HEAD");
               lastModified = httpc.getLastModified();
               size = httpc.getContentLength();
               if( size==fOut.length() && lastModified<=fOut.lastModified() ) {

                  // On doit tout de même vider le buffer
                  dis = httpc.getInputStream();
                  while( (n=dis.read(buf)) > 0) { sizeRead+=n; }
                  dis.close();  
                  dis=null;
                  return 256; // sizeRead;  // déjà fait  (à la louche la taille du HEAD http)
               }
            }

            long t0 = System.currentTimeMillis();
            httpc = (HttpURLConnection)u.openConnection();
            timeout = new TimeOut(httpc,fileIn,TIMEOUT);
            timeout.start();
            httpc.setReadTimeout(TIMEOUT-500);
            httpc.setConnectTimeout(TIMEOUT-500);
            httpc.setRequestProperty("User-Agent", "Aladin/Hipsgen/"+Aladin.VERSION);
            httpc.setRequestMethod("GET");
            if( lastModified==-1 ) lastModified = httpc.getLastModified();
//            if( hpx!=null ) hpx.threadBuilder.setInfo("copyRemote opening inputstream from  "+fileIn+"...");
            dis = httpc.getInputStream();
            long t1 = System.currentTimeMillis();
            timeIP=t1-t0;
            
            Util.createPath(fileOut);
            f = new RandomAccessFile(fileOut, "rw");
            while( (n=dis.read(buf))>0 ) {
               sizeRead+=n;
               timeout.setSize(sizeRead);
//               if( hpx!=null ) hpx.threadBuilder.setInfo("loading try:"+i+" "+n+" bytes from tile "+fileIn+"...");
               f.write(buf,0,n);
            }
            timeout.end(); timeout=null;
            dis.close(); dis=null;
            f.close(); f=null;

         } catch( Exception e ) {
            if( e instanceof FileNotFoundException ) throw e;

//            e.printStackTrace();
            fOut.delete();
            if( i<MAXRETRY-1 ) context.warning("File copy error  => try again ("+(i+1)+"x) ["+fileIn+"]");
            else throw new Exception("File copy error ["+fileIn+"]");
            continue;
            
         } finally {
            if( timeout!=null ) timeout.end();
            if( dis!=null ) try{ dis.close(); } catch( Exception e) {} 
            if( f!=null ) try{ f.close(); } catch( Exception e) {}
         }

         if( lastModified!=0L ) fOut.setLastModified(lastModified);

         if( sizeRead>0 && (new File(fileOut)).length()<size) {
            if( i==MAXRETRY-1 ) throw new Exception("Truncated file copy ["+fileIn+"]");
            context.warning("Truncated file copy => try again ["+fileIn+"]");
         }
         else break;  // a priori c'est bon
      }

      return sizeRead;

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
   
   boolean oneWaiting() {
      try {
         Iterator<ThreadBuilder> it = threadList.iterator();
         while( it.hasNext() ) if( it.next().isWaitingAndUsable(false) ) return true;
      } catch( Exception e ) { }
      return false;
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
      timeIPArray = new long[MAXTIMEIP ];
      timeIPindex=0;
   }

   // Mise à jour des stats
   private void updateStat(long size,long timeIP) {
      statNbFile++;
      lastCumul+=size;
      statCumul+=size;
      totalTime = System.currentTimeMillis()-startTime;
      try {
         timeIPArray[timeIPindex++]=timeIP;
      } catch( Exception e ) { }
      if( timeIPindex>=MAXTIMEIP ) timeIPindex=0;
      
   }
}

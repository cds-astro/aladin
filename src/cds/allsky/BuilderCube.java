package cds.allsky;

import healpix.newcore.HealpixProc;

import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

import cds.aladin.Aladin;
import cds.aladin.ColorMap;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.aladin.MyProperties;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.PlanHealpix;
import cds.aladin.PlanImage;
import cds.aladin.PlanImageMosaic;
import cds.aladin.PlanImageRGB;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

public class BuilderCube extends Builder {

   static final int COPY = 0;
   static final int LINK = 1;
   
   private String inputPath [];
   private int mode = COPY;

   private int statNbFile;
   private long statSize;
   private long startTime,totalTime;

   public BuilderCube(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CUBE; }

   public void run() throws Exception {
      build();       
      
      if( !context.isTaskAborting() ) {
         BuilderMoc bm = new BuilderMoc(context);
         bm.createMoc( context.getOutputPath() );
         context.moc=bm.moc;
      }
      if( !context.isTaskAborting() ) context.writePropertiesFile();
   }

   // Demande d'affichage des stats (dans le TabRgb)
   public void showStatistics() {
      context.showRgbStat(statNbFile, statSize, totalTime);
   }

   public void validateContext() throws Exception { 
      
      boolean propFound=false;
      
      // découpage et vérification de la liste des HiPS sources
      String s = context.getInputPath();
      StringTokenizer st = new StringTokenizer(s," ");
      inputPath = new String [st.countTokens()];
      for( int i=0; i<inputPath.length; i++ ) {
         String path = inputPath[i]=st.nextToken();
         if( !(new File(path)).isDirectory() ) throw new Exception("Input HiPS error ["+path+"]");
         
         // Mémorisation ou check du order
         int order = Util.getMaxOrderByPath( path );
         if( i==0 ) context.order = order;
         else if( order!=context.order ) throw new Exception("Error: No homogeneous input HiPS ["+path+" => order="+order+"]");
         
         // Mémorisation des propriétés à partir du premier HiPS
         if( !propFound ) {
            String propFile = path+Util.FS+PlanHealpix.PROPERTIES;
            context.prop = new MyProperties();
            File f = new File( propFile );
            if( f.exists() ) {
               if( !f.canRead() || !f.canWrite() ) throw new Exception("Propertie file not available ! ["+propFile+"]");
               FileInputStream in = new FileInputStream(propFile); 
               context.prop.load(in);
               in.close();
               propFound=true;
            }
         }
      }
      
      // Check du répertoire de destination
      validateOutput();
      
      // Ajout des paramètres propres aux cubes
      context.depth=inputPath.length;
      if( !propFound ) context.frame=Localisation.GAL;  // Si rien n'est mentionné, c'est du GAL
   }

   private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   private void updateStat(File f) {
      statNbFile++;
      statSize += f.length();
      totalTime = System.currentTimeMillis()-startTime;
   }

   public void build() throws Exception  {
      initStat();
      String output = context.getOutputPath();
      
      for( int z=0; z<context.depth; z++ ) {
         String input = inputPath[z];   
         
         for( int order = 3; order<=context.getOrder(); order++ ) {
            String path ="Norder"+order;
            treeCopy(input, output, path,z);
         }
         
      }
   }
   
   // Parcours récursive du cube source input à copier dans output avec comme profondeur z
   private void treeCopy(String input, String output, String path,int z) throws Exception {
      File src = new File( input+Util.FS+path );    
      if( !src.exists() ) return;
      
      // procédure récursive par répertoire
      if( src.isDirectory() ) {
         
         File [] list = src.listFiles();
         for( int i=0; i<list.length; i++ ) {
            String s1 = list[i].getName();
            treeCopy(input,output,path+Util.FS+s1,z);
         }
         return;
      }
      
      // Copie simple
      String s = src.getName();
      int pos = s.lastIndexOf('.');
      String ext  = pos<0 ? "" : s.substring(pos);
      String name = pos<0 ? s  : s.substring(0,pos);
      String suffixe = z==0 ? "" : "_"+z;
      
      String subPath = (new File(path)).getParent();
      
      (new File(output+Util.FS+subPath)).mkdirs();
      File trg = new File( output+Util.FS+subPath+Util.FS+name+suffixe+ext );
      copy(src,trg);
   }

   // Copie du fichier
   private void copy(File src, File trg) throws Exception {
      RandomAccessFile r = null;
      byte buf [] =null;
      try { 
         r = new RandomAccessFile(src, "r");
         buf = new byte[ (int)src.length() ];
         r.read( buf );
      } finally { if( r!=null ) r.close(); }

      r=null;
      try {
         r = new RandomAccessFile(trg, "rw");
         r.write(buf);
      } 
      catch( Exception e ) { e.printStackTrace(); }
      finally { if( r!=null ) r.close(); }
      
      updateStat(trg);
   }
}

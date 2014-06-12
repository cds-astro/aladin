package cds.allsky;


import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

import cds.aladin.Localisation;
import cds.aladin.MyProperties;
import cds.aladin.PlanHealpix;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

public class BuilderCube extends Builder {

   private String inputPath [];
   private Mode mode = Mode.COPY;

   private int nbFmt=1;         // Nombre de formats de tuiles concernés
   private boolean hasFITS=false,hasPNG=false,hasJPEG=false;    // Formats déjà rencontrés
   private int statNbFile;
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
   
   // Pour les stats, retourne le nombre de formats de tuiles rencontrés et ou estimés
   private int getNbFmt() {
      int n = 0;
      if( hasFITS ) n++;
      if( hasJPEG ) n++;
      if( hasPNG ) n++;
      return Math.max(n,nbFmt);
   }

   // Demande d'affichage des stats (dans le TabRgb)
   public void showStatistics() {
      context.showJpgStat( statNbFile/getNbFmt(), totalTime,0,0);
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
               
               // Pour la stat d'avancement => une idée du nombre de format de tuiles
               try {
                  String fmt = (String)context.prop.get(PlanHealpix.KEY_FORMAT);
                  int n = (new StringTokenizer(fmt," ")).countTokens();
                  if( n>1 ) nbFmt=n;
               } catch( Exception e ) { }
            }
         } 
         
         // Récupération des noms des bandes
         String lab= getALabel(path);
         context.setPropriete(PlanHealpix.KEY_LABEL+"_"+i, lab);
         
         // Estimation du MOC final (union)
         try {
            HealpixMoc m = new HealpixMoc();
            m.read( path+Util.FS+BuilderMoc.MOCNAME);
            if( context.moc==null ) context.moc=m;
            else context.moc = context.moc.union(m);
         } catch( Exception e ) {
           context.warning("Missing original MOC in "+path+" => running time estimation will be wrong");
         }
      }
      
      // Check du répertoire de destination
      validateOutput();
//      validateFrame();
      validateLabel();
      
      // Ajout des paramètres propres aux cubes
      context.depth=inputPath.length;
      
      // Mode de travail (link ou copy)
      if( context.getMode().equals(Mode.LINK) ) mode = context.getMode();
      context.info(mode.getExplanation(mode));
   }

   private void initStat() { statNbFile=0;  startTime = System.currentTimeMillis(); }

   
   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }

   public void build() throws Exception  {
      initStat();
      String output = context.getOutputPath();
      String outputHpxFinder = output+Util.FS+Constante.HPX_FINDER;
      
      for( int z=0; z<context.depth; z++ ) {
         String input = inputPath[z];   
         
         for( int order = 3; order<=context.getOrder(); order++ ) {
            treeCopy(input, output, "Norder"+order,z);
         }
         
         String inputHpxFinder = input+Util.FS+Constante.HPX_FINDER;
         if( (new File(inputHpxFinder).isDirectory()) ) {
            for( int order = 3; order<=context.getOrder(); order++ ) {
               treeCopy(inputHpxFinder, outputHpxFinder, "Norder"+order,z);
            }
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
      
      if( !hasPNG && ext.equals(".png") ) hasPNG=true;
      else if( !hasFITS && ext.equals(".fits") ) hasFITS=true;
      else if( !hasJPEG && ext.equals(".jpg") ) hasJPEG=true;
      
      String subPath = (new File(path)).getParent();
      
      (new File(output+Util.FS+subPath)).mkdirs();
      File trg = new File( output+Util.FS+subPath+Util.FS+name+suffixe+ext );
      
      if( mode==Mode.LINK ) link(src,trg);
      else copy(src,trg);
      
      updateStat();
   }
   
   static private void link(File src, File trg) throws Exception {
      Path pSrc = Paths.get(src.toURI() );
      Path pTrg = Paths.get(trg.toURI() );
      Files.createSymbolicLink(pTrg, pSrc);
   }
   
//   static public void main(String [] args) {
//      try {
//         File src = new File("C:\\Users\\Pierre\\Desktop\\GALEXHiPS");
//         File trg = new File("C:\\Users\\Pierre\\Desktop\\Link");
//         link(src,trg);
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }

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
   }
}

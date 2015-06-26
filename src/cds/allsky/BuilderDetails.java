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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import cds.aladin.HealpixProgen;
import cds.tools.pixtools.Util;

/** Construction de la hiérarchie des tuiles d'index à partir des tuiles de plus bas
 * niveau. Le but est de donner accès aux progéniteurs
 *
 * Methode:
 * 1) on détermine a priori le niveau le plus adapté pour mémoriser les JSON cumulatifs
 *    => on peut estimer que c'est dépendant de la taille classique d'une image (récupérer depuis l'image étalon, sinon
 *       depuis le HpxFinder [x,y, => 8000, sinon supposé à 1024)
 *    => On part du order du HiPS -ImgWidth/Context.SIZE -1   (si < 3 garder 3)
 *    => ex: HiPS F555W = 14, imgWidth=5000 => 14 - 5000/512 - 1 = 6
 *
 * @author Pierre Fernique
 */
public class BuilderDetails extends Builder {

   static public final int MINORDER = 3;   // niveau minimal pris en compte

   private int detailOrder;
   private int maxOrder;
   private long nbItemPerOrder[];

   private int statNbFile;
   private long startTime,totalTime;

   /**
    * Création du générateur de l'arbre des index.
    * @param context
    */
   public BuilderDetails(Context context) {
      super(context);
   }

   public Action getAction() { return Action.DETAILS; }

   public void run() throws Exception {
      build();
   }

   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      validateIndex();

      // détermination de l'ordre minimum pour les tuiles concaténées
      // soit indiqué via le parametre "order", soit déterminé à partir du order
      // de l'index et de la taille typique d'une image.
      maxOrder = Util.getMaxOrderByPath( context.getHpxFinderPath() );
      if( maxOrder==-1 ) throw new Exception("HpxFinder seems to be not yet ready ! (order=-1)");
      context.info("Order retrieved from HpxFinder => "+maxOrder);

      // Pour compatibilité - on passait "order=xx" pour indiquer le minOrder avant a version 8.118
      if( context.getOrder()<maxOrder )  context.setMinOrder(context.getOrder());

      context.setOrder(maxOrder); // juste pour que les statistiques de progression s'affichent correctement

      detailOrder = context.getMinOrder();
      if( detailOrder!=-1 ) {
         context.info("Detail table min order determined by \"minOrder\" parameter => "+detailOrder);
      } else {

         validateImgWidth();
         if( context.typicalImgWidth==-1 ) {
            detailOrder = maxOrder - 5;
            context.warning("Detail table min order determined in function of max order => "+detailOrder);
         } else {
            detailOrder = maxOrder - context.typicalImgWidth/context.getTileSide() -2; //-1;
            context.info("Detail table min order determined by original image resolution => "+detailOrder);
         }
      }
      if( detailOrder<MINORDER ) detailOrder=MINORDER;
      if( detailOrder>maxOrder ) {
         context.warning("The target Order ("+detailOrder+") is greater than the index order ("+maxOrder+") => assuming "+maxOrder+"...");
         detailOrder=maxOrder;
      }

      context.mocIndex=null;
      context.initRegion();
   }

   // Vérifie que le répertoire HpxIndex existe et peut être utilisé
   private void validateIndex() throws Exception {
      String path = context.getHpxFinderPath();
      if( path==null ) throw new Exception("HEALPix index directory [HpxFinder] not defined => specify the output (or input) directory");
      File f = new File(path);
      if( !f.exists() || !f.isDirectory() || !f.canWrite() || !f.canRead() ) throw new Exception("HEALPix index directory not available ["+path+"]");
   }

   private void validateImgWidth() throws Exception {
      String img = context.getImgEtalon();
      if( img==null && context.getInputPath()!=null) {
         img = context.justFindImgEtalon( context.getInputPath() );
         if( img!=null ) context.info("Use this reference image => "+img);
      }
      if( img!=null ) {
         try { context.setImgEtalon(img); }
         catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
      }
   }

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, totalTime,0,0);
   }

   public void build() throws Exception {
      initStat();
      context.setProgressMax(768);

      String output = context.getHpxFinderPath();
      HealpixProgen allsky=null;
      nbItemPerOrder = new long[maxOrder+1];

      for( int i=0; i<768; i++ ) {
         HealpixProgen hi = createTree(output,3,i);
         if( hi!=null ) {
            if( allsky==null ) allsky  = new HealpixProgen();
            if( !allsky.hasTooMany() ) {
               allsky.merge(hi);
               if( allsky.size()>HealpixProgen.TOOMANY ) allsky.setTooMany(true);
            }
         }
         context.setProgress(i);
      }


      // Suppression des niveaux ayant trop d'entrées.
      if( detailOrder>3 ) {
         long size=768;
         int o;
         for( o=3; o<detailOrder && nbItemPerOrder[o]> HealpixProgen.TOOMANY * size; o++, size *=4 ) {
            String file = cds.tools.Util.concatDir(output, "Norder"+o);
            context.info("Removing HpxFinder detail order "+o+" (too many entries ["+nbItemPerOrder[o]+"])...");
            cds.tools.Util.deleteDir( new File(file) );
            if( o==3 ) allsky=null;
         }
         detailOrder=o;
      }

      // Génération du Allsky si nécessaire
      if( allsky!=null && !allsky.hasTooMany() ) {
         String file = BuilderAllsky.getFileName(context.getHpxFinderPath(), 3,0);
         //         System.out.println("Create "+file);
         writeIndexFile(file, allsky);
      }

      // Generation du fichier metadata.xml
      generateMedataFile();
   }

   private void initStat() { statNbFile=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }

   /** Construction récursive de la hiérarchie des tuiles FITS à partir des tuiles FITS
    * de plus bas niveau. La méthode employée est la moyenne
    */
   private HealpixProgen createTree(String path,int order, long npix ) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");

      // Si son père n'est pas dans le MOC, on passe
      if( !context.isInMocTree(order-1,npix/4) ) return null;

      String file = Util.getFilePath(path,order,npix);

      HealpixProgen out = null;
      if( order==maxOrder ) out = createLeave(file);
      else {
         HealpixProgen fils[] = new HealpixProgen[4];
         boolean found = false;
         for( int i =0; i<4; i++ ) {
            fils[i] = createTree(path,order+1,npix*4+i);
            if (fils[i] != null && !found) found = true;
         }
         if( found ) out = createNode(fils);
      }

      // Si on a trop de Progen
      if( order<detailOrder && out!=null && out.size()>HealpixProgen.TOOMANY ) {
         out.setTooMany(true);
      }

      if( out!=null && context.isInMocTree(order,npix) /* && order>=detailOrder */ ) {

         // calcule du nombre d'entrées par niveau
         nbItemPerOrder[order] += out.size();

         if( !out.hasTooMany() ) {
            writeIndexFile(file,out);
            //            Aladin.trace(4, "Writing " + file);
         }
      }

      if( out!=null && out.hasTooMany() ) out=null;



      //      if( order<detailOrder ) return null;
      return out;
   }

   // Ecriture du fichier d'index HEALPix correspondant à la map passée en paramètre
   private void writeIndexFile(String file,HealpixProgen map) throws Exception {
      cds.tools.Util.createPath(file);
      map.writeStream(new FileOutputStream(file) );
   }

   /** Construction d'une tuile terminale. Lit le fichier est map les entrées de l'index
    * dans une TreeMap */
   private HealpixProgen createLeave(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
      //      System.out.println("   createLeave("+file+")");
      HealpixProgen out = new HealpixProgen();
      out.loadStream( new FileInputStream(f));
      updateStat();
      return out;
   }

   /** Construction d'une tuile intermédiaire à partir des 4 tuiles filles */
   private HealpixProgen createNode(HealpixProgen fils[]) throws Exception {
      //      System.out.println("   createNode()");

      HealpixProgen out = new HealpixProgen();
      for( int i=0; i<4; i++ ) {
         if( fils[i]==null ) continue;
         if( fils[i].hasTooMany() ) { out.setTooMany(true); break; }
         out.merge(fils[i]);
         fils[i]=null;
      }

      return out;
   }

   private static final String METADATA =
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "\n" +
               "<!-- VOTable HiPS hpxfinder mapping file.\n" +
               "     Use to map and build from a HpxFinder JSON tile a classical VOTable HiPS tile.\n" +
               "     Adapt it according to your own (see examples below in the comments)\n" +
               "-->\n" +
               "\n" +
               "<VOTABLE version=\"1.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "  xmlns=\"http://www.ivoa.net/xml/VOTable/v1.2\"\n" +
               "  xsi:schemaLocation=\"http://www.ivoa.net/xml/VOTable/v1.2 http://www.ivoa.net/xml/VOTable/v1.2\">\n" +
               " \n" +
               "<RESOURCE>\n" +
               "  <COOSYS ID=\"J2000\" system=\"eq_FK5\" equinox=\"J2000\"/>\n" +
               "  <TABLE name=\"YOUR_SURVEY_LABEL\">\n" +
               "    <FIELD name=\"RAJ2000\" ucd=\"pos.eq.ra\" ref=\"J2000\" datatype=\"double\" precision=\"5\" unit=\"deg\">\n" +
               "      <DESCRIPTION>Right ascension</DESCRIPTION>\n" +
               "    </FIELD>\n" +
               "    <FIELD name=\"DEJ2000\" ucd=\"pos.eq.dec\" ref=\"J2000\" datatype=\"double\" precision=\"5\" unit=\"deg\">\n" +
               "      <DESCRIPTION>Declination</DESCRIPTION>\n" +
               "    </FIELD>\n" +
               "    <FIELD name=\"id\" ucd=\"meta.id;meta.dataset\" datatype=\"char\" arraysize=\"13*\">\n" +
               "      <DESCRIPTION>Dataset name, uniquely identifies the data for a given exposure.</DESCRIPTION>\n" +
               "       <!-- Simple HTTP link description (Aladin will open it in a Web navigator)\n" +
               "         <LINK href=\"http://your.server.edu/info?param=${id}&amp;otherparam=foo\"/>\n" +
               "       -->\n" +
               "     </FIELD>\n" +
               "    <FIELD name=\"access\" datatype=\"char\" arraysize=\"9*\">\n" +
               "      <DESCRIPTION>Display original image</DESCRIPTION>\n" +
               "       <LINK content-type=\"image/fits\" href=\"${access}\"/>\n" +
               "       <!--  Image HTTP link description (Aladin will load it)\n" +
               "          <LINK content-type=\"image/fits\" href=\"http://your.server.edu/getdata?param=${id}&amp;otherparam=foo\" title=\"remote img\"/>\n" +
               "        -->\n" +
               "    </FIELD>\n" +
               "    <FIELD name=\"FoV\" datatype=\"char\" utype=\"stc:ObservationLocation.AstroCoordArea.Region\" arraysize=\"12*\">\n" +
               "       <DESCRIPTION>Field of View (STC description)</DESCRIPTION>\n" +
               "    </FIELD>\n" +
               "    <!-- Additional Field for extracting Instrument name from original filepath\n" +
               "         see also associated TD example below\n" +
               "       <FIELD name=\"Instrument\" datatype=\"char\" arraysize=\"12*\">\n" +
               "          <DESCRIPTION>Instrument</DESCRIPTION>\n" +
               "       </FIELD \n" +
               "     -->\n" +
               "<DATA>\n" +
               "   <TABLEDATA> \n" +
               "      <TR>\n" +
               "      <TD>$[ra]</TD>\n" +
               "      <TD>$[dec]</TD>\n" +
               "      <TD>$[name]</TD>\n" +
               "      <TD>$[path:([^\\[]*).*]</TD>\n" +
               "      <TD>$[stc]</TD>\n" +
               "      <!-- Extended example via prefix and regular expression mapping\n" +
               "           (here, the instrument name is coded in the original path after \"data\" directory)\n" +
               "           <TD>Instrument: $[path:.*/data/(.+)/.*]</TD> \n" +
               "        -->\n" +
               "      </TR>\n" +
               "   </TABLEDATA>\n" +
               "</DATA>\n" +
               "</TABLE>\n" +
               "</RESOURCE>\n" +
               "</VOTABLE>\n" +
               "\n" +
               "";

   // Génération si nécessaire du fichier de MetaData afin d'exploiter l'index pour
   // un accès au progéniteur
   private void generateMedataFile() throws Exception {
      String metadata = cds.tools.Util.concatDir(context.getHpxFinderPath(),Constante.FILE_METADATAXML);
      if( (new File(metadata)).exists() ) {
         context.info("Pre-existing "+Constante.FILE_METADATAXML+" file => keep it");
      } else {
         RandomAccessFile f = new RandomAccessFile(metadata ,"rw");
         String s = METADATA.replace("YOUR_SURVEY_LABEL",context.getLabel()+" details");
         f.write(s.getBytes());
         f.close();
         context.info("Mapping hpxFinder/"+Constante.FILE_METADATAXML+" file has been generated");
      }

      //      writeProperties();
      context.writeHpxFinderProperties();
      context.writeIndexHtml();
   }

   //   // On écrit le fichier des propriétés
   //   private void writeProperties() throws Exception {
   //      int frame = context.getFrame();
   //
   //      MyProperties prop = new MyProperties();
   //      prop.setProperty(PlanHealpix.KEY_LABEL, context.getLabel()+"/"+Constante.HPX_FINDER);
   //      prop.setProperty(PlanHealpix.KEY_ISMETA, "true");
   //      prop.setProperty(PlanHealpix.KEY_COORDSYS, frame==Localisation.ICRS ? "C" : frame==Localisation.ECLIPTIC ? "E" : "G");
   //      prop.setProperty(PlanHealpix.KEY_MAXORDER, context.getOrder()+"");
   //      if( detailOrder>3 ) prop.setProperty(PlanHealpix.KEY_MINORDER, detailOrder+"");
   //      prop.setProperty(PlanHealpix.KEY_PROCESSING_DATE, context.getNow());
   //      prop.setProperty(PlanHealpix.KEY_HIPSBUILDER, "Aladin/HipsGen "+Aladin.VERSION);
   //
   //      String propFile = context.getHpxFinderPath()+Util.FS+PlanHealpix.PROPERTIES;
   //      File f = new File(propFile);
   //      if( f.exists() ) f.delete();
   //      FileOutputStream out = null;
   //      try {
   //         out = new FileOutputStream(f);
   //         prop.store( out, null);
   //      } finally {  if( out!=null ) out.close(); }
   //   }



}
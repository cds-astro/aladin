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

package cds.allsky;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

import cds.aladin.HealpixProgen;
import cds.aladin.HealpixProgenItem;
import cds.moc.SMoc;
import cds.moc.TMoc;
import cds.tools.pixtools.Util;


/**
 * Création d'un THiPS meta dans TimeFinder
 * @author Pierre Fernique [CDS]
 */
final public class BuilderTIndex  extends Builder {

   static public final int TIMEORDER = 14;   // Order max du TimeFinder (environ 17mn)

   private int hpxOrder;

   private int statNbFile;
   private long startTime,totalTime;

   public BuilderTIndex(Context context) { super(context); }

   public Action getAction() { return Action.TINDEX; }


   public void run() throws Exception {
      build();
   }

   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      validateIndex();

      hpxOrder = Util.getMaxOrderByPath( context.getHpxFinderPath() );
      if( hpxOrder==-1 ) throw new Exception("HpxFinder seems to be not yet ready ! (order=-1)");
      context.info("Order retrieved from HpxFinder => "+hpxOrder);

      context.setOrder(hpxOrder); // juste pour que les statistiques de progression s'affichent correctement

      context.mocIndex=null;
      context.initRegion();
   }

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, totalTime,0,0);
   }

   public void build() throws Exception {

      // Détermination des paths en entrée (HpxFinder) et en sortie (TimeFinder)
      String hpxPath = context.getHpxFinderPath();
      String timePath = context.getTimeFinderPath();
      
      // détermination des tuiles de HpxFinder à scanner
      SMoc mocRegion = context.getRegion();
      if( mocRegion.getMocOrder()!=hpxOrder ) {
         mocRegion = mocRegion.clone();
         mocRegion.setMocOrder( hpxOrder );
      }
      
      // On utilisera le nombre de tuiles en entrées comme indice de progression
      initStat( mocRegion.getNbValues() );
      
      // On va créer également le TMoc correspondant 
      TMoc tmoc = new TMoc( TIMEORDER );
      
      // Parcours de toutes les tuiles meta du HpxFinder
      int i=0;
      Iterator<Long> it = mocRegion.valIterator();
      while( it.hasNext() ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         long npix = it.next();
         String fileIn = Util.getFilePath(hpxPath,hpxOrder,npix);
         HealpixProgen tileIn = createLeave(fileIn);
         updateTimeFinder(timePath,tmoc,tileIn);
         context.setProgress(++i);
         updateStat();
      }
      
      // Génération du TMOC
//      tmoc.toMocSet();
      tmoc.write(timePath+Util.FS+"TMoc.fits");

      // Generation du fichier metadata.xml
//      generateMedataFile();
   }

   private void initStat( long size) {
      statNbFile=0;
      startTime = System.currentTimeMillis();
      context.setProgressMax( size );
   }
   
   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }
   
   // mise à jour de toutes les tuiles du TimeFinder pour chacune des entrées de la tuile meta
   private void updateTimeFinder(String timePath, TMoc tmoc, HealpixProgen tileIn) {

      for( String key : tileIn ) {
         HealpixProgenItem item = tileIn.get(key);
         String json = item.getJson();
         double tmin;
         double tmax;

         try {
            
            // Détermination de l'intervalle de temps
            String s = cds.tools.Util.extractJSON("T_MIN", json);
            if( s==null ) continue;
            tmin = Double.parseDouble( s );
            s= cds.tools.Util.extractJSON("T_MAX", json);
            if( s==null ) continue;
            tmax = Double.parseDouble(s  );
            double jdtmin = tmin+2400000.5;
            double jdtmax = tmax+2400000.5;

            // Ajout dans le TMOC global
            tmoc.add(jdtmin,jdtmax);
            
            // Ajout dans toutes les tuiles qu'il faut du TimeFinder
            TMoc a = new TMoc( TIMEORDER );
            a.add(jdtmin,jdtmax);
//            a.toMocSet();
            Iterator<Long> it = a.valIterator();
            while( it.hasNext() ) {
               long npix = it.next();
               String fileOut = Util.getFilePath(timePath,TIMEORDER,npix);
               HealpixProgen tileOut = createLeave(fileOut);
               if( tileOut!=null ) {
                  // si déjà présent, pas besoin d'aller plus loin
                  if( tileOut.containsKey( key ) ) continue;
               } 

               // Ajout de la ligne en fin de la tuile meta temporelle
               RandomAccessFile out = null;
               try {
                  out = openFile(fileOut);
                  out.seek( out.length() );
                  out.write( (json+"\n").getBytes() );
                  out.close();
                  out=null;
               } finally { if( out!=null ) out.close(); }
            }

         } catch( Exception e ) {
            context.warning("parsing error => "+json);
            continue;
         }
      }
   }

   // Création si nécessaire du fichier passé en paramètre et ouverture en écriture
   private RandomAccessFile openFile(String filename) throws Exception {
      File f = new File( filename );
      if( !f.exists() ) cds.tools.Util.createPath(filename);
      return new RandomAccessFile(f,"rw");
   }


   /** Construction d'une tuile terminale. Lit le fichier est map les entrées de l'index
    * dans une TreeMap */
   private HealpixProgen createLeave(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
      HealpixProgen hp = new HealpixProgen();
      hp.loadStream( new FileInputStream(f));
      return hp;
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

}

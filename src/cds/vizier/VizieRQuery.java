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


package cds.vizier;

import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.net.URL;

import cds.aladin.*;
import cds.tools.*;
import cds.xml.*;

/**
 * VizieR Query
 * <P>
 * This class has been created to separate graphic interface and query features
 * <P>
 *
 * @authors Andre Schaaff [CDS], Thomas Boch [CDS]
 *  21 June 2010 (AS) : fix bug concerning the description field of the catalog list (it follows a change of VOTable)
 *  20 march 2008 (TB) : fix bug which shifted density and wavelength info in the catalogs list
 *  2 march 2004 : startElement, INFO null attribute bug correction
 * @version 1.0 beta : (june 2002) creation
 */
 public class VizieRQuery implements XMLConsumer, CDSConstants {

   static final int KEYWORDS = 0;
   static final int SURVEYS  = 1;
   static final int ARCHIVES = 2;
   static final int CATALOGS = 3;
   static final int TD = 4;
   static final int SURVEYMAXCOL = 3;
   static final int ARCHIVEMAXCOL = 3;
   private int section;     // current section, -1 if unknown

   private String currentCat=null; //Id du catalogue courant pour le parsing des catalogues
   private String currentWaveLength=null; //Longueur d'onde du catalogue courant pour le parsing des catalogues
   private String currentDensity=null; //Densite du catalogue courant pour le parsing des catalogues
   private String currentDesc=null; // Description du catalogue
   private String metaError;    // XML parser meta information error
   private Vector vKey;         // Current keywords section value
   private boolean inCSV;       // true if in ths <CSV section ..
   private boolean inTD;        // true if in ths <TD> section ..
   private boolean inDesc;      // true if in a <DESCRIPTION section ..
   private String headlines;    // memo des infos de la section <CSV
   private String recsep;       // idem
   private String colsep;       // idem
   private int column = 0;      // TD column in a TR row

   //En static pour ne le faire qu'une fois (usage Aladin derrière Tomcat)
   static private Vector vSurveys = null;
   static private Vector vArchives = null;
   static private Hashtable hKey = null;
   static private Vector nameKey = null;

   private StringBuffer currentSurvey = new StringBuffer();
   private StringBuffer currentArchive = new StringBuffer();
   private MyInputStream vizierStream = null;
   private int currentFormat = ASTRORES;

   private Vector resultat = null;     // List filled with the query result

   // GLU
   protected Glu glu = null; // GLU interactions, must be replaced with JGlu as soon as possible


   /** Reset les listes de mots clés afin de pouvoir les recharger (Glu.reload()); */
   static public void resetKeywords() { vSurveys=vArchives=nameKey=null; hKey=null; }

  /** Lancement de la recuparation des informations d'interrogation
   * de VizieR
   * @return true if ok, false else
   */
   public boolean metaDataQuery() {

      if( vSurveys!=null ) return true;     // déjà fait

      MyInputStream dis;
      XMLParser xmlParser = new XMLParser(this);

      // Init
      metaError=null;
      section=-1;
      vSurveys = new Vector(30);
      vArchives = new Vector(30);
      hKey = new Hashtable();
      nameKey = new Vector();
      vKey=null;
      inCSV=false;
      URL url = null;

      // Parsing launch
      try {
        // if glu is not null use it else use cds vizier server
        if ( glu != null) {
          url = glu.getURL(VIZIERMETAGLU, "", false, false);
        } else url = new URL(VIZIERMETA);
//        InputStream instream = url.openStream();
        InputStream instream = glu.aladin.getCache().get(url);
        dis = new MyInputStream(instream);
        boolean res=xmlParser.parse(dis);

        return (res && metaError==null );
     }
     catch( Exception e) {
        metaError = "" + e;
        System.err.println("metaDataQuery : " + e);
        e.printStackTrace();
        return false;
      }
   }

    /**
    * Get data about catalogs
    * @param param
    * @param outputMode
    * @param resultat
    * @return true if ok, false else
    */
   public boolean catalogsDataQuery(String param, int outputMode, Vector resultat) {
      MyInputStream dis;
      XMLParser xmlParser = new XMLParser(this);

      // Init
      metaError=null;
      section=CATALOGS;
      inDesc=false;
      currentCat = null;
      currentWaveLength = null;
      currentDensity = null;
      currentDesc = null;
      this.resultat = resultat;
      URL url = null;

      // Parsing launch
      try {
        // if glu is not null use it else use cds vizier server
        if (glu != null) {
          url = glu.getURL(VIZIERMETACATGLU, param, true);
        }
        else
          url = new URL(VIZIERMETACAT + param);

// System.out.println("url " + url.toString());

        if (outputMode == FRAME) {
          this.resultat.clear();
          dis = new MyInputStream(url.openStream());
          boolean res=xmlParser.parse(dis);
          return res;
        }
        else {
          this.vizierStream = new MyInputStream(url.openStream());
          return(this.vizierStream != null);
        }
     }
     catch( Exception e) {
        metaError= "" + e;
        System.err.println("catalogsDataQuery : " + e);
        return false;
      }
   }

   /** Method for the XML parser implementation
    *
    * @param name
    * @param atts
    */
   public void startElement(String name, Hashtable atts) {
        String ID;

        // format detection, VOTABLE or ASTRORES
        if (name.equals("VOTABLE")) {
            currentFormat = VOTABLE;
        }
        if (name.equals("ASTRO")) {
            currentFormat = ASTRORES;
        }

        // Catalogs parsing case
        if (section == CATALOGS) {
            if (name.equals("DESCRIPTION"))  {
                inDesc = true;
            }
            else {
                if (name.equals("RESOURCE")) {
                    if (currentFormat == VOTABLE)
                        currentCat = (String) atts.get("name");
                    else if (currentFormat == ASTRORES)
                        currentCat = (String) atts.get("ID");
                } else if (name.equals("INFO")) {
                    String attribute = (String) atts.get("name");
                    if (attribute != null) {
                        if (attribute.equals("-kw.Wavelength")) {
                            currentWaveLength = (String) atts.get("value");
                        }
                        if (attribute.equals("-density")) {
                            currentDensity = (String) atts.get("value");
                        }
                    }
                }
            }
            return;
        }

      // <RESOURCE ID=VizieR section
      if( name.equals("RESOURCE") ) {
         ID = (String)atts.get("ID");
         if( ID!=null && ID.equals("VizieR") )
          section=KEYWORDS;
      }

      // <TABLE ID=AladinSurveys and ID=AladinArchives sections
      else
      if ( name.equals("TABLE") ) {
         ID = (String)atts.get("ID");
         if( ID!=null && ID.equals("AladinSurveys") )
           section=SURVEYS;
         else
         if ( ID!=null && ID.equals("AladinArchives") )
          section=ARCHIVES;
      }

      // Reperage des sections d'interrogation par mots-cles
      // On cree un vector vKey qui memorisera les mots-cles de la section
      // courante, d'autre part, on memorise le nom de la section dans
      // nameKey et enfin on associe le vecteur des mots-cles et le
      // nom de la section dans la Hashtable hKey
      //
      // Le fait que vKey soit different de null permet de savoir
      // que le parsing XML se trouve dans une section de mots-cles
      else
      if ( section==KEYWORDS && name.equals("PARAM")  && currentFormat == VOTABLE) {
         String section = (String)atts.get("name");
         if ( section.startsWith("-kw." ) ) {
            vKey = new Vector(30);
            nameKey.addElement(section); // pour connaitre l'ordre des sections
            hKey.put(section,vKey);      // association nom de section/liste des valeurs
         }
      }
      else
      if ( section==KEYWORDS && name.equals("FIELD") && currentFormat == ASTRORES) {
         String section = (String)atts.get("name");
         if ( section.startsWith("-kw." ) ) {
            vKey = new Vector(30);
            nameKey.addElement(section); // pour connaitre l'ordre des sections
            hKey.put(section,vKey);      // association nom de section/liste des valeurs
         }
      }
      else
      if ( vKey!=null && name.equals("OPTION") ) {
         String s = (String)atts.get("value");
         if( s!=null )
           vKey.addElement(s);
      }
      // Reperage de la section des SURVEYS ou des ARCHIVES de logs
      else
      if ( (section==SURVEYS || section==ARCHIVES) && name.equals("CSV") ) {
         inCSV=true;
         headlines=(String)atts.get("headlines");
         recsep=(String)atts.get("recsep");
         colsep=(String)atts.get("colsep");
      }
      else
      if ( (section==SURVEYS || section==ARCHIVES) && name.equals("TD") ) {
         inTD=true;
         column++;
      }
      else
      if (name.equals("TR") ) {
        currentSurvey = new StringBuffer();
        currentArchive = new StringBuffer();
        column = 0;
      }

   }

   /** Method for the XML parser implementation
    *
    * @param name
    */
   public void endElement(String name) {
      // Fin de la section courante de mots-cles
      if( vKey!=null && name.equals("FIELD") ) {
         vKey=null;
      }
      // fin de la section DESCRIPTION
      else if( section == CATALOGS && name.equals("DESCRIPTION") ) {
            inDesc = false;
      }
      // Fin de la section <CSV.. courante
      else if ( name.equals("CSV") ) {
        inCSV=false;
      }
      // fin de RESOURCE ?
      else if( name.equals("RESOURCE") ) {
          if ( currentCat != null ) {
              addItem(currentCat+"\t"+currentWaveLength+"\t"+currentDensity+"\t"+currentDesc);
              currentCat = null;
              currentWaveLength = currentDensity = currentDesc = "";
            }
      }
   }

  /** In one CSV line (described by ch[], cur and end, recsep and colsep)
    * memorizes the current value in the StringBuffer rec
    *
    * @param rec the value will be added on it
    * @param ch ,cur,recsep,colsep informations to retrieve the CSV line
    * @param cur
    * @param end
    * @param recsep
    * @param colsep
    * @return the new value of cur variable. Points to the next character in ch[]
    */
   private int getField(StringBuffer rec,char [] ch, int cur, int end, char recsep, char colsep) {
      int start=cur;
      while ( cur<end && ch[cur]!=colsep && ch[cur]!=recsep )
        cur++;
      String s = new String(ch,start,cur-start).trim();

      if( s.length()!=0 ) {
         if( rec.length()!=0 )
          rec.append("\t");
         rec.append(s);
      }

      return ch[cur]==colsep?cur+1:cur;
   }

   /** In one CSV line (described by ch[], cur and end, recsep and colsep)
    * cuts each individual value and memorizes them in the StringBuffer rec
    *
    * @param rec has to be created before. The result of the analyse
    * @param ch ,cur,sep,recsep,colsep informations to retrieve the CSV line
    * @param cur
    * @param end
    * @param recsep
    * @param colsep
    * @return the new value of cur variable. Points to the next character in ch[]
    */
   private int getRec(StringBuffer rec, char [] ch, int cur, int end, char recsep,char colsep) {
      while ( cur<end && ch[cur]!=recsep ) {
         cur=getField(rec,ch,cur,end,recsep,colsep);
      }

      // On ajoute l'unite a la fin de chaque ligne

      return cur;
   }

   /** Characters
    *
    * @param ch
    * @param start
    * @param length
    */
   public void characters(char ch[], int start, int length) {
      char rs = '\n';       // Default record separator
      char cs = '\t';       // Default field separator
      int h;            // Number of head lines
      int cur = start;      // Current character
      int end = start+length;   // Last character
      StringBuffer rec;     // To build the record
      int n=0;          // Counter of fields

      // Cas du parsing des catalogues
        if (section == CATALOGS) {
            if (inDesc) {
                currentDesc = new String(ch, start, length);
            }
            return;
        }

      // Traitement de la section data dans le cas des surveys
      // ou des archives
      if( inCSV ) {

//System.out.println("ch=["+new String(ch,start,length)+"]");
         // Separators and headlines ?
         if( recsep!=null )
          rs=recsep.charAt(0);
         if( colsep!=null )
          cs=colsep.charAt(0);
         h = (headlines==null)?0:Integer.parseInt(headlines);

         // Heading treatement
         n=0;               // current line number
         for ( n=0; cur<end && n<h; n++ ) {
            rec = new StringBuffer();
            cur = getRec(rec,ch,cur,end,rs,cs);
            cur++;
         }

         // Data treatement
         while ( cur<end ) {
            rec = new StringBuffer();
            cur = getRec(rec,ch,cur,end,rs,cs);
            switch(section) {
              case SURVEYS: vSurveys.addElement(rec.toString());
              break;

              case ARCHIVES: vArchives.addElement(rec.toString());
              break;
            }
            cur++;
         }
         return;
      }

      // Traitement de la section data dans le cas des surveys
      // ou des archives
      if( inTD ) {
        String data = new String(ch,start,length);
        switch(section) {
          case SURVEYS:
            if( column < SURVEYMAXCOL ) {
              if (column != 1)
              currentSurvey.append("\t");
              currentSurvey.append(data);
            }
            else {
              currentSurvey.append("\t");
              currentSurvey.append(data);
              vSurveys.addElement(currentSurvey.toString());
            }
            inTD = false;
            break;

          case ARCHIVES:
            if( column < ARCHIVEMAXCOL) {
              if (column != 1)
              currentArchive.append("\t");
              currentArchive.append(data);
            }
            else {
              currentArchive.append("\t");
              currentArchive.append(data);
              vArchives.addElement(currentArchive.toString());
            }
            inTD = false;
            break;
          }
        return;
      }
  }

  public String getMetaError() {
    return metaError;
  }

  public Vector getvArchives() {
    return vArchives;
  }

  public Vector getvSurveys() {
    return vSurveys;
  }

  public Hashtable gethKey() {
    return hKey;
  }

  public Vector getNameKey() {
    return nameKey;
  }

  public void setGLU(Glu glu) {
    this.glu = glu;
  }

  /** Add an item to the result list (VizieRServer)
   * @param s      L'item a ajouter
   */
   protected void addItem(String s) {
      if( resultat!=null ) {
        resultat.addElement(s);
      }
   }

   /**
    * Return the Catalogs data input stream
    *
    * @return xml stream
    */
   public MyInputStream getResultStream() {
      return vizierStream;
   }

   /** Query VizieR for the catalogs filtering
    *
    * @param target
    * @param radius
    * @param unit
    * @param coordinate
    * @param tauthor
    * @param extra
    * @param mode
    * @param resultat
    * @return boolean
    */
   public boolean submit(String target, String radius, String unit, String coordinate, String author, String extra, int mode, Vector resultat) {
      StringBuffer param = new StringBuffer();

      if( target==null )
        param.append("-pos");
      else {
        CDSMethods.append(param, "-c", target);
           if( radius!=null ) {
             StringTokenizer radiusToken = new StringTokenizer(radius, " ");
             if ( radiusToken.hasMoreTokens() ) {
                 CDSMethods.append(param,"-c.r", radiusToken.nextToken());
             }
             if (radiusToken.hasMoreTokens() ) {
              String token = radiusToken.nextToken();
              if (token.compareTo("deg") == 0 || token.compareTo(DEGREE) == 0)
                CDSMethods.append(param,"-c.u", DEGREE);
              else
                CDSMethods.append(param,"-c.u", token);
             }
             else //default unit
              CDSMethods.append(param,"-c.u", ARCMIN);
         }
      }

      // coordinate
      if( coordinate != null )
        if (coordinate.compareTo("") != 0)
          CDSMethods.append(param,"-c.eq",coordinate);

      // free keywords
      if( author != null )
        if (author.compareTo("") != 0)
          CDSMethods.append(param,"-words",author);

      // add selected keywords
      if (extra != null)
        param.append('&' + extra);

      // URL call
      return callVizieR( param.toString(), mode, resultat);
   }

  /** Query VizieR for the catalogs filtering
    *
    * @param target
    * @param radius
    * @param unit
    * @param tauthor
    * @param extra
    * @param mode
    * @param resultat
    * @return boolean
    */
   public boolean submit(String target, String radius, String unit, String tauthor, String extra, int mode, Vector resultat) {

    return submit(target, radius, unit, null, tauthor, extra, mode, resultat);
   }

  /** VizieR call via GLU VizGlu.
    * Parameters are HTTP formated
    *
    * @param param
    * @param mode
    * @param resultat
    * @return boolean
    */
   protected boolean callVizieR(String param, int mode, Vector resultat) {
    // Vizier meta call
    if (param == null) System.out.println("param null");

    if (resultat == null) System.out.println("resultat null");
    boolean res = catalogsDataQuery(param, mode, resultat);

    // default cursor setting and return code
    if (mode == FRAME) {
      return( res && resultat != null && resultat.size() > 0);
    }
    else {
      return res;
    }
   }

   //
   public void query() {
   }
}

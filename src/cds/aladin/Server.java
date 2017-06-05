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

package cds.aladin;

import static cds.aladin.Constants.DATALINK_FORM;
import static cds.aladin.Constants.REGEX_BAND_RANGEINPUT;
import static cds.aladin.Constants.REGEX_TIME_RANGEINPUT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ADQLParser;
import adql.parser.TokenMgrError;
import adql.query.ADQLQuery;
import cds.aladin.Constants.TapServerMode;
import cds.moc.HealpixMoc;
import cds.tools.Astrodate;
import cds.tools.ScientificUnitsUtil;
import cds.tools.Util;

/**
 * Interface pour les formulaires d'acces aux bases de donnees (images ou data).
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : 19 sept 2005 - gestion homogène du radius
 * @version 1.1 : 18 aout 2005 - Maximisation d'un component pour le layout
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Server extends JPanel
       implements ActionListener,KeyListener,Comparator {

   // Les types de serveurs
   static final int IMAGE     = 1;
   static final int CATALOG   = 2;
   static final int SPECTRUM  = 4;
   static final int APPLI     = 8;
   static final int APPLIIMG  =16;
   static final int MOC       =32;

   // Les différents status de la dernière interrogation ALLVO
   static final int STATUS_OK       = 0;
   static final int STATUS_NORESULT = 1;
   static final int STATUS_ERROR    = 2;
   static final int STATUS_QUERYING = 3;
   static final int STATUS_ABORT    = 4;

   // Les variables statiques
   static int WIDTH = Aladin.OUTREACH ? 430 : 500; 	// Largeur du Panel
   static int HEIGHT = Aladin.OUTREACH ? 300 : 400;	// Hauteur du Panel
   static final int YOUTREACH = 60; // Ordonnée du premier label en mode OUTREACH
   static final int MAXSELECTEDPLANE = 10; // Nombre max d'images à charger avant affichage un warning

	protected String TARGET, RAD, GRABIT = "", DEFAULT_METHODE, TARGET_EX, RADIUS_EX, WNEEDOBJ, WNEEDRAD, WNEEDDATE,
			WNEEDCAT, WERROR, WTOOLARGE, WERRORDATE, WDEJA, HASFILTER1, HASFILTER2, NOINPUTITEM, WNEEDCHECK, UNKNOWNOBJ,
			NOTTOOMANY, DATEFORMATINCORRECT, BANDFORMATINCORRECT, TARGETOUTOFBOUNDSMESSAGE, CHECKQUERY_SUCCESS,
			CHECKQUERY_ISBLANK;

   // Pour le positionnement des widgets en absolu
   static final int XTAB1=10;		// Abscisse des labels des champs à saisir
   static final int XTAB2=160;		// Abscisse des champs de saisie
   static final int XWIDTH=WIDTH-XTAB1;		// Largeur max


   // Les elements communs aux formulaires
   String ordre = "X";             // numéro d'ordre du formulaire
   MetaDataTree tree=null;	       // Le session tree si necessaire pour le formulaire
   JTextArea tap=null;             // Chaine de la requete ADQL pour un serveur TAP
   static boolean message=true;    // Indique qu'il faut afficher une fenetre pour OK
   JTextField target=null;         // Le target (methode unifiee)
   JTextField radius;              // la taille (si necessaire)

   protected boolean DISCOVERY = false;  // true si le serveur doit être pris en compte en mode discovery
   protected boolean TESTSERVER = false; // true s'il y a l'alternative d'un serveur test
   protected boolean HIDDEN = false;  // true si le serveur n'a pas son propre formulaire
                                      // uniquement accessible via IVOA bouton

   static final protected int MAXINPUT = 10;	// Nombre max de champs input dans un formulaire

   // Reperage des TextFields indiquants le centre,la taille la date du champ et les éventuels
   // champs input (utilisés pour passer l'url associé à un plan déjà chargé)
   protected JTextField coo[];	 // Pointe les champs du target
   protected JTextField rad[];	 // Pointe les champs du field (radius...)
   protected JTextField date;	 // Pointe sur le champ de la date (s'il y a lieu)
   protected JTextField band;
   protected JComponent input[];	 // Pointe sur les champs des inputs (TextField pour IMGs et CATs, sinon Choice)
   protected int nbInput=0;      // nombre de champs input
   protected JLabel targetLabel=null; // Le label du target
   protected Vector<JTextField> adqlOpInputs;

   protected int modeDate=0;
   protected int modeRad=0	;
   protected int modeCoo=0;
   protected int modeInput[]; // Mémorise le modeInput de chaque champ input
   protected int modeBand=0;
   protected int modePos = 0; 

   static final int NOMODE = 0;  // Aucun mode défini (pour target ou radius)
   static final int COO    = 1;   // Coordonnees en J2000 sexa un champ
   static final int COOb   = 2;   // Coordonnees en J2000 sexa un champ (separateur ' ')
   static final int SIMBAD = 4;   // Id. Simbad (un champ)
   static final int NED    = 8;   // Id. NED (un champ)
   static final int RADEC  = 16;   // Coor en J2000 sexa sur deux champs
   static final int RADEd  = 32;  // Coor en J2000 deci sur deux champs
   static final int RADEb  = 64;  // Coor en J2000 deci sur deux champs, separateur ' '
   static final int RADE6  =128;  // Coor en J2000 sexa sur six champs
   static final int COOd   =256 ; // Coor en J2000 deci sur un champ

   static final int RADIUS = 1; // Field en radius (arcmin)
   static final int RADSQR = 2; // Field en carre (arcmin), 1 champ
   static final int RADBOX = 4; // Field en box (arcmin), 2 champs
   static final int RADIUSd= 8; // Field en radius (degres)
   static final int RADIUSs=16; // Field en radius (secondes)
   static final int RADSQRd=32; // Field en carre (degrés), 1 champ
   static final int RADBOXd=64; // Field en box (degrés), 2 champs
   static final int RADSQRs=128; // Field en carre (arcsec), 1 champ
   static final int RADBOXs=256; // Field en box (arcsec), 2 champs
   static final int STRINGd = 512; //user inputs- box or circle will be processed in degrees 1 field

   static final int JD    = 1;		// Date en Modified Julian Day
   static final int MJD   = 2;		// Date en Julian Day
   static final int YEARd = 4;		// Date en années décimale
   static final int ParseToMJD = 8; //Parse any date to MJD
   
   static final int BANDINMETERS = 1; //Parse to meters

   static final int IMG    = 1;		// Input : une URL d'image (FITS)
   static final int IMGs   = 2;		// Input : URLs d'images (FITS)
   static final int CAT    = 4;		// Input : une URL de catalogue (VOTABLE)
   static final int CATs   = 8;		// Input : URLs de catalogues (VOTABLE)
   static final int ALLIMG = 16;     // Input : une URL d'image qq soit son type (FITS)

   static int ORDRE=0;

   // Les parametres propres
   int type;                      // Le type de serveur (IMAGE, CATALOG, SPECTRUM)
   protected double aladinMenuNumber=0;      // Numéro d'ordre du serveur (ordre d'apparition dans le serveur selector)
   protected String title;		  // Le titre de la fenêtre
   protected String aladinLabel="";       // le nom du serveur
   protected String description;         // les infos (une ligne) decrivant le serveur
   protected String verboseDescr;         // Description détaillée du serveur
   protected String institute;         // L'origine
   protected String aladinLogo;         // Le logo attaché au serveur (null si aucun)
   protected String docUser=null; // Tag GLU ou URL de la doc associee au serveur
   JToggleButton grab=null;		      // Bouton grab s'il y en a un dans l'interface
   String aladinMenu=null;		      // Label du popup ou null sinon
   boolean flagVerif=true;        // false si on ne verifie pas les redondances des plans
   boolean flagToFront=true;      // false si on ne passe pas la fenetre Aladin devant
   protected String filters[];    // Liste des filtres prédéfinis selon la syntaxe suivate:
                                  //    # Description une ligne\nfilter Nom.Filter { contrainte { action }.... }
   JComboBox filtersChoice=null;     // La boite de choix du filtre prédéfini
   private Component maxComp=null;// Le component du Server qui doit être maximisé

   protected JCheckBox cbAllVO=null;   // Le checkbox de selection pour le Discovery mode
   protected boolean filterAllVO=true; // false si le serveur n'est pas pris en compte dans la liste AllVO (non retenu dans le filtre)
   protected JLabel statusAllVO;       // Le status pour le mode ALLVO
   protected JButton statusReport=null;// Le bouton qui permet d'afficher le detail du dernier status
   protected String statusUrl=null;    // Dernière URL d'appel pour le mode ALLVO
   protected String statusError=null;  // Dernière erreur
   protected Ball ball;                // Voyant d'état

   // Les references aux objets
   public Aladin aladin;

   //for tap client
   ADQLParser adqlParser;
   public TapClient tapClient;
   protected TapServerMode mode;
   JComboBox modeChoice = null;
   
   protected String getTitle() { return aladinLabel; }
   protected String getOrigin() { return institute; }
   protected String getType() { return type==IMAGE?"Image":type==CATALOG?"Catalog":type==SPECTRUM?"Spectra":type==MOC?"Coverage (MOC)":""; }
   protected MyInputStream getMetaData(String target,String radius,StringBuffer infoUrl) throws Exception { return null; }
   protected boolean isHidden() { return HIDDEN; }
   protected boolean isDiscovery() { return DISCOVERY; }
   protected boolean isAllVOChecked() {
      if( !filterAllVO ) return false;
      if( cbAllVO==null  ) return true;
      else return cbAllVO.isSelected();
   }

   @Override
   public Dimension getMinimumSize() { return getSize(); }
   @Override
   public Dimension getPreferredSize() { return getSize(); }
   @Override
   public Dimension getSize() { return new Dimension(WIDTH,HEIGHT); }

   // Gestion d'un verrou pour la synchronisation script.
//   private boolean sync=true;
//   synchronized protected boolean isSync() { return sync && (tree==null || tree.isSync()); }
//   synchronized protected void setSync(boolean sync) { this.sync=sync; }

   protected void createChaine() {
//      aladinMenuNumber = ORDRE++;      // On se sert du createChaine pour initialiser le numéro d'ordre d'apparition par défaut

      TARGET         =aladin.chaine.getString("TARGET");
      TARGET_EX      =aladin.chaine.getString("TARGET_EX");
      RAD            =aladin.chaine.getString("RADIUS");
      RADIUS_EX      =aladin.chaine.getString("RADIUS_EX");
      GRABIT         =aladin.chaine.getString("GRABIT");
      WNEEDCHECK     =aladin.chaine.getString("WNEEDCHECK");
      WNEEDOBJ       =aladin.chaine.getString("WNEEDOBJ");
      WNEEDDATE      =aladin.chaine.getString("WNEEDDATE");
      WNEEDRAD       =aladin.chaine.getString("WNEEDRAD");
      WNEEDCAT       =aladin.chaine.getString("WNEEDCAT");
      WERROR         =aladin.chaine.getString("WERROR");
      WTOOLARGE      =aladin.chaine.getString("WTOOLARGE");
      WERRORDATE     =aladin.chaine.getString("WERRORDATE");
      WDEJA          =aladin.chaine.getString("WDEJA");
      HASFILTER1     =aladin.chaine.getString("HASFILTER1");
      HASFILTER2     =aladin.chaine.getString("HASFILTER2");
      NOINPUTITEM    =aladin.chaine.getString("NOINPUTITEM");
      UNKNOWNOBJ     =aladin.chaine.getString("UNKNOWNOBJ");
      NOTTOOMANY       =aladin.chaine.getString("NOTTOOMANY");
      DATEFORMATINCORRECT = aladin.chaine.getString("DATEFORMATINCORRECT");
      BANDFORMATINCORRECT = aladin.chaine.getString("BANDFORMATINCORRECT");
      TARGETOUTOFBOUNDSMESSAGE = aladin.chaine.getString("TARGETOUTOFBOUNDSMESSAGE");
      CHECKQUERY_ISBLANK = Aladin.chaine.getString("CHECKQUERY_ISBLANK");
      CHECKQUERY_SUCCESS = Aladin.chaine.getString("CHECKQUERY_SUCCESS");

      statusAllVO=new JLabel(" "); // Le status pour le mode ALLVO

   }
   
   protected void resumeTargetLabel() {
      if( targetLabel==null ) return;
      String t = TARGET;
      try {
         String frame = aladin.localisation.getFrameName();
         if( frame.length()>0 ) t = t+" ("+frame+", name)";
      } catch( Exception e) {}
      targetLabel.setText(addDot(t));
   }

   /** Maj des arbres MetaData (celui propre au serveur ainsi que celui de l'historique */
   protected boolean updateMetaData(MyInputStream is, Server server,String target,String requestedPos,boolean append) {
      boolean rep;
      if( append ) rep=aladin.treeView.appendToTree(is,tree,server,target,requestedPos);
      else rep=aladin.treeView.updateTree(is,tree,server,target,requestedPos);
      aladin.setButtonMode();
      if( !aladin.dialog.isVisible() ) aladin.dialog.show();
      return rep;
   }

   protected boolean updateMetaData(MyInputStream is, Server server,String target,String requestedPos) {
      return updateMetaData(is, server, target, requestedPos, false);
   }

   /** Positionnement du status du serveur de la dernière requête AllVO */
   protected void setStatusAllVO(int status,String url) {
      if( statusAllVO==null ) return;

      statusAllVO.setText( status==STATUS_OK ? "Ok    "
            : status==STATUS_NORESULT ? "No result    "
            : status==STATUS_QUERYING ? "Querying...    "
            : status==STATUS_ERROR ?    "Error    "
            : status==STATUS_ABORT ?    "Abort    "
                  : "-");
      statusAllVO.setForeground( status==STATUS_OK ? Aladin.COLOR_GREEN
            : status==STATUS_ERROR ? Color.red
            : status==STATUS_NORESULT ? Color.blue: Color.black);

      statusAllVO.setFont(Aladin.BOLD);

      statusUrl = url;
   }

   /** Methode TEMPORAIRE pour créer un SIA pour liste de catalogues qui en l'occurence
    * ne comportera qu'une entrée décrivant le nombre d'objets retournés par l'URL */
   protected MyInputStream getMetaDataForCat(URL u) {
      try {
         Plan plan = new Plan();
         Pcat pcat = new Pcat(plan,null,aladin.calque,null,aladin);
         int count = pcat.setPlanCat(plan,u,false);
         if( count<=0 ) return null;

         MyByteArrayStream bas = null;
         OutputStream out;
         out = bas = new MyByteArrayStream(10000);

         Aladin.writeBytes(out,
               "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
               "<!DOCTYPE VOTABLE SYSTEM \"http://us-vo.org/xml/VOTable.dtd\">"+
               "<VOTABLE version=\"v1.0\">"+
               "<RESOURCE type=\"results\">"+
               "   <INFO name=\"QUERY_STATUS\" value=\"OK\"/>"+
               "   <TABLE>"+
               "<FIELD name=\"Resource\" datatype=\"char\" ucd=\"VOX:Image_Title\" arraysize=\"*\"/>"+
               "<FIELD name=\"Origin\" datatype=\"char\"/>"+
               "<FIELD name=\"url\" datatype=\"char\" ucd=\"VOX:Image_AccessReference\" arraysize=\"*\"/>"+
               "<FIELD ID=\"FORMAT\" datatype=\"char\" arraysize=\"*\"/>"+
               "<DATA>"+
               "<TABLEDATA>"+
               "   <TR>"+
               "      <TD>"+aladinLabel+": "+count+" object"+(count>1?"s":"")+"</TD>" +
               "      <TD>"+institute+"</TD>"+
               "      <TD><![CDATA["+u+"]]></TD>"+
               "      <TD>CATALOG</TD>"+
               "   </TR>"+
               "</TABLEDATA>"+
               "</DATA>"+
               "</TABLE>"+
               "</RESOURCE>"+
               "</VOTABLE>"      );

         return new MyInputStream(bas.getInputStream());
      } catch( Exception e) { }
      return null;
   }

   
   /** Interprête la coordonnées et exécute Sésame si nécessaire */
   protected String sesameIfRequired(String s,String sep) throws Exception {
       Coord c;
       if( !View.notCoord(s) ) c = new Coord(s);
       else c=aladin.view.sesame(s);
       return c==null?null:c.getSexa(sep);
   }

  /** Transformation du target en champs propres au serveur
   * avec eventuellement resolution Simbad si necessaire
   * @param le target passé en paramètre
   * @return le target (resolu ou non) sinon null
   */
   protected String resolveTarget(String t) throws Exception {
//      t=aladin.localisation.getICRSCoord(t);
      if( coo!=null ) {
         if( (modeCoo & SIMBAD)!=0 ) {
            coo[0].setText(t);
            return t;
         }

         Coord c=null;
         if( !View.notCoord(t) ) c = new Coord(t);
         else c=aladin.view.sesame(t);
         if( c==null ) return null;

         if( (modeCoo & COO)!=0 ) coo[0].setText(c.getSexa(":"));
         else if( (modeCoo & COOb)!=0 ) coo[0].setText(c.getSexa(" "));
         else if( (modeCoo & COOd)!=0 ) coo[0].setText(c.al+" "+(c.del>=0?"+":"")+c.del);
         else if( (modeCoo & RADEC)!=0 ) {
            coo[0].setText(c.getRA());
            coo[1].setText(c.getDE());
         } else if( (modeCoo & RADE6)!=0 ) {
            StringTokenizer tok = new StringTokenizer(c.getRA(),":");
            coo[0].setText(tok.nextToken());
            coo[1].setText(tok.nextToken());
            coo[2].setText(tok.nextToken());
            tok = new StringTokenizer(c.getDE(),":");
            coo[3].setText(tok.nextToken());
            coo[4].setText(tok.nextToken());
            coo[5].setText(tok.nextToken());
         } else if( (modeCoo & RADEb)!=0 ) {
            coo[0].setText(c.getRA(' '));
            coo[1].setText(c.getDE(' '));
            System.out.println("c.getSexa="+c.getSexa()+" coo[0]= "+c.getRA(' ')+" coo[1]="+c.getDE(' '));
         } else if( (modeCoo & RADEd)!=0 ) {
            coo[0].setText(c.al+"");
            //            coo[1].setText((c.del>=0?"+":"")+c.del+"");
            coo[1].setText(c.del+"");
         }
         return c.getSexa(":");
      }
      return t;
   }

   /** Transformation du radius en champs propres au serveur
    * @param le radius (ex 14'  ou  13" x 15")
    * @param updateRadius true s'il faut aussi mettre à jour le champ radius
    */
   protected void resolveRadius(String s,boolean updateRadius) {
      double rm=getRM(s);
      double wm=getWM(s);
      double hm=getHM(s);
      double bm=Math.max(wm,hm);

      // Affectation des champs GLU si besoin est
      if( rad!=null ) {
              if( (modeRad&RADBOX)!=0 )  { rad[0].setText(wm+"");rad[1].setText(hm+""); }
         else if( (modeRad&RADBOXd)!=0 ) { rad[0].setText((wm/60.)+"");rad[1].setText((hm/60.)+""); }
         else if( (modeRad&RADBOXs)!=0 ) { rad[0].setText((wm*60.)+"");rad[1].setText((hm*60.)+""); }
         else if( (modeRad&RADSQR)!=0 )  rad[0].setText(bm+"");
         else if( (modeRad&RADSQRd)!=0 ) rad[0].setText((bm/60.)+"");
         else if( (modeRad&RADSQRs)!=0 ) rad[0].setText((bm*60.)+"");
         else if( (modeRad&RADIUS)!=0 )  rad[0].setText(rm+"");
         else if( (modeRad&RADIUSd)!=0 ) rad[0].setText((rm/60.)+"");
         else if( (modeRad&RADIUSs)!=0 ) rad[0].setText((rm*60.)+"");
         else if( (modeRad&STRINGd)!=0 ) {rad[0].setText((rm/60.)+"");
        	 /*int i = getDelimiterIndex(s); if (i<0) { //circle
        		 rad[0].setText((rm/60.)+""); } else {//rectangle
				rad[0].setText((wm/60.)+" "); if (rad[1]==null) { rad[1] = new JTextField(); } rad[1].setText((hm/60.)+"");
			}*/
        	 
         }     
      }

      // Mise à jour du champ radius générique
//      if( (modeRad&(RADSQR|RADSQRd|RADSQRs))!=0 ) radius.setText( Coord.getUnit(bm/60.)+" x "+Coord.getUnit(bm/60.) );
//      else if( (modeRad&(RADBOX|RADBOXd|RADBOXs))!=0 ) radius.setText( Coord.getUnit(wm/60.)+" x "+Coord.getUnit(hm/60.) );
//    else if( (modeRad&(RADIUS|RADIUSd|RADIUSs))!=0 ) radius.setText( Coord.getUnit(rm/60.) );
      if( updateRadius && modeRad!=NOMODE ) {
      	try {
      		String newRadius = Coord.getUnit(rm/60.);
      		radius.setText( newRadius );

      		// résolution du bug #213 (cf. http://pclx5:8080/support/issue213 )
      		radius.setCaretPosition(Math.min(radius.getCaretPosition(), newRadius.length()));
      	}
      	// résolution de la demande #222 (cf. http://pclx5:8080/support/issue222 )
      	catch(Exception e) {}

      }
   }

   /** Transformation de la date dans la syntaxe propre au serveur
    * ATTENTION: POUR LE MOMENT JE LAISSE TEL QUEL, IL FAUDRA PRENDRE EN COMPTE JD, MJD...
    * (sert pour initialiser la valeur par défaut pour les commandes scripts)
    * @param s la date (ex: 1999/05/05T10:32)
    */
   protected void resolveDate(String s) {
      setDate(s==null ? "" : s);
   }


  /** Mise en place des differents de curseurs */
   protected void waitCursor() { ball.setMode(Ball.WAIT); makeCursor(Aladin.WAITCURSOR); }
   protected void defaultCursor() { ball.setMode(Ball.OK); makeCursor(Aladin.DEFAULTCURSOR); }
   protected void makeCursor(int c) {
      Aladin.makeCursor(aladin.dialog,c);
      Aladin.makeCursor(aladin,c);
      if( coo!=null ) {
         Aladin.makeCursor(coo[0],c);
         if( coo.length>1 ) Aladin.makeCursor(coo[1],c);
      }
      if( rad!=null ) Aladin.makeCursor(rad[0],c);
      if( tree!=null ) Aladin.makeCursor(tree,c);
   }

   // Retourne le nombre de pixel du label
   protected int stringSize(JLabel l) {
      FontMetrics m = l.getFontMetrics(l.getFont());
      int i=m.stringWidth(l.getText());
      return i+60;
   }

   static protected int HAUT=25;    // Hauteur des components classiques
   static protected int MARGE=1;      // Marge entre les components
   static protected int LIST_HAUT=80; 
   
   protected JCheckBox testServer=null;

   /**
    * Construction du panel du titre du serveur (titre + bouton info)
    * @param title le titre
    * @return le panel
    */
   protected Dimension makeTitle(JPanel p, String title) {
       p.setLayout(new FlowLayout(FlowLayout.CENTER));

       p.add(ball = new Ball());

       JLabel t = new JLabel(title.replace('\n',' '));
       t.setFont( Aladin.LBOLD );
       p.add(t);
       
//       JButton b = new JButton(FrameServer.INFO);
//       JButton b = Util.getHelpButton(this,MESSAGE)
             
       JButton b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Help.png")));
       b.setMargin(new Insets(0,0,0,0));
       b.setBorderPainted(false);
       b.setContentAreaFilled(false);
       b.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) { showStatusReport(); }
       });

       Insets m = b.getMargin();
       b.setMargin(new Insets(m.top,3,m.bottom,3));
       b.setOpaque(false);
       b.addActionListener(this);
       p.add(b);

       int width = stringSize(t)+20;
       if( TESTSERVER ) {
          testServer=new JCheckBox("test",true);
          testServer.setMargin(new Insets(m.top,10,m.bottom,3));
          testServer.setOpaque(false);
          testServer.setSelected(false);
          p.add(testServer);
          width+=120;
       }

       return new Dimension(width,HAUT+5);
   }

//   private int testMac=-1;

   protected String addDot(String s) {
      return s;
      // Sous certains Mac, les Dots se superposent stupidement au TextField qui suit
      // je ne les génère donc pas.
//      if( testMac==-1 ) {
//         String syst = System.getProperty("os.name");
//         testMac =  syst != null && syst.startsWith("Mac") ? 1 : 0;
//      }
//      if( testMac==1 ) return s;

//      return s+".......................................";
   }


   final protected int NOPICK   = 1;
   final protected int NORADIUS  = 2;
   final protected int NOTARGET  = 4;
   final protected int FORVIZIER = 8;
   final protected int FORALADIN = 16;

   /** Construction du panel générique pour le Target/radius
    * Retourne la hauteur du panel
    * @param p le Panel à créer (de fait mis à jour)
    * @return La hauteur du Panel
    */
   protected int makeTargetPanel(JPanel p,int mode) {
      int x=0,y=0,l;
      int pickL=75;
      boolean pickView  = (mode & NOPICK)   ==0;
      boolean forVizieR = (mode & FORVIZIER)!=0;
      boolean noRadius  = (mode & NORADIUS) !=0;
      boolean noTarget  = (mode & NOTARGET) !=0;

      JLabel label;

      p.setLayout(null);

      //  Construction des éléments pour le Target
      if( !noTarget ) {
         l=XTAB2-10;
         if( forVizieR ) l=100;
         targetLabel = label= new JLabel("");
         resumeTargetLabel();
         label.setFont(Aladin.BOLD);
         label.setBounds(XTAB1,y,l,HAUT);
         p.add(label);

         target = new JTextField(40);
         target.addKeyListener(this);
         target.addActionListener(this);
         x=XTAB2;
         l = XWIDTH-XTAB2/*-20*/;
         if( forVizieR ) { l=XWIDTH-180-30; x=70+30; }
         if( pickView ) l-=pickL;
         target.setBounds(x,y,l,HAUT);
         p.add(target);
         Util.toolTip(label, TARGET_EX);
         Util.toolTip(target, TARGET_EX);

         if( pickView )  {
            grab = new JToggleButton(GRABIT);
            Insets m = grab.getMargin();
            grab.setMargin(new Insets(m.top,2,m.bottom,2));
            grab.setOpaque(false);
            grab.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					aladin.dialog.startGrabIt();
					if (aladin.additionalServiceDialog != null) {
						aladin.additionalServiceDialog.startGrabIt();
					}
					// ABOVE NOT REALLY NEEDED, BEACUSE OF THE BELOW.
					aladin.f.toFront();
					JPanel server = Server.this;
					aladin.grabUtilInstance.grabFrame = (GrabItFrame) SwingUtilities.getRoot(server);

				}
            });
            grab.setFont(Aladin.SBOLD);
            grab.setEnabled(false);
            if (this.aladinLabel.equalsIgnoreCase(Constants.DATALINK_CUTOUT_FORMLABEL)) {//TODO:change this logic?
            	grab.setEnabled(true);//deefault true for datalink forms
			}
            grab.setBounds(x+l+3,y+2,pickL-2,HAUT-4);
            p.add(grab);
         }

         y+=HAUT+MARGE;
      }

      if( noRadius || forVizieR ) return y;

      // Construction des éléments pour le Radius
      x=0;
      l = 55;
      String rad=RAD;
      if( (mode&FORALADIN) != 0 ) rad="Search cone";
      label= new JLabel(addDot(rad));
      label.setFont(Aladin.BOLD);
      label.setBounds(XTAB1,y,XTAB2-10,HAUT); x+=l+5;
      p.add(label);

      radius = new JTextField(50);
      radius.addKeyListener(this);
      radius.addActionListener(this);
      l=XWIDTH-XTAB2/*-15*/;
      if( pickView ) l-=pickL;
      radius.setBounds(XTAB2,y,l,HAUT);
      p.add(radius);
      Util.toolTip(label, RADIUS_EX);
      Util.toolTip(radius, RADIUS_EX);

      y+=HAUT+MARGE;

      return y+3;
   }

   /**
    * Fixe le focus sur l'un des éléments du formulaire
    * (on économise ainsi un click de souris)
    */
   protected void setInitialFocus() {
      if( target!=null && isVisible() ) {
         target.requestFocusInWindow();
         target.setCaretPosition(target.getText().length());
      }
   }

   // TODO : temporaire
   // sera supprimé après refonte MetadataTree/BasicTree
   boolean initDone;
   protected void initServer() {
       if( initDone ) return;

       if( tree!=null ) {
           tree.traverseTree();
//           tree.init();
           repaint();
       }
       initDone = true;
   }

   /**
    * Mémorise le component qui sera toujours maximisé
    * @param c Component à maximiser dans le panel
    */
   protected void setMaxComp(Component c) { maxComp=c; }

   protected void setMaxComp(Component c, boolean onlyInY) {
   	    maximizeInYOnly = onlyInY;
   	    setMaxComp(c);
   }

   // true si on ne souhaite maximiser qu'en Y, sans toucher à la largeur
   private boolean maximizeInYOnly = false;


   /**
    * Maximise la taille du component maxComp mémorisé par setMaxComp en fonction
    * de la taille courante du Panel. Décale les components situés en dessous
    */
   synchronized private void maximizeComp() {
      if( maxComp==null || getBounds().height<100 ) return;

      Component c[] = getComponents();
      int maxY=0;
      for( int i=0; i<c.length; i++ ) {
         int y = c[i].getBounds().y+c[i].getBounds().height;
         if( y>maxY ) maxY=y;
      }
      int gaph = getBounds().height-maxY;
      int yComp = maxComp.getBounds().y;
      for( int i=0; i<c.length; i++ ) {
         Rectangle rc = c[i].getBounds();
         if( c[i]==maxComp ) {
             c[i].setBounds(rc.x,rc.y,maximizeInYOnly?c[i].getBounds().width:getBounds().width-rc.x,rc.height+gaph);
         }
         else if( rc.y>yComp ) c[i].setBounds(rc.x,rc.y+gaph,rc.width,rc.height);
      }
   }

   @Override
public void layout() {
      maximizeComp();
      super.layout();
   }


   /** Transformation des infos target/radius exprimées dans les champs génériques
    * en valeurs correspondantes dans rad[] et coo[]
    * avec eventuellement resolution Simbad si necessaire
    * @return le target (resolu ou non) sinon null
    */
    protected String resolveQueryField() throws Exception {
       if( radius!=null ) resolveRadius( radius.getText().trim(), false );
       if( target==null ) return null;
       String t=aladin.localisation.getICRSCoord(target.getText().trim() );
       return resolveTarget( t );
    }
    
    /**
     * Method to test if the target specified is within the target limits.
     * @param posBounds
     * @param rectVertices
     * @return
     * @throws NumberFormatException
     * @throws Exception
     */
    protected String isWithinBounds(HealpixMoc posBounds, List<Coord> rectVertices) throws NumberFormatException, Exception {
    	String result = null;
    	HealpixMoc userSpecified = null;
    	
		if (posBounds!=null) {
    		String radiusInput  = radius.getText().trim();
    		int i = getDelimiterIndex(radiusInput);
    		if (i<0) {
				//circle
    			userSpecified = aladin.createMocRegionCircle(Double.parseDouble(coo[0].getText()), Double.parseDouble(coo[1].getText()), Double.parseDouble(rad[0].getText()), -1);
			} else {
				//rectangly
				double width = getWM(radiusInput)/60.;
				double height = getHM(radiusInput)/60.;
				userSpecified = aladin.createMocRegionRectangle(rectVertices, Double.parseDouble(coo[0].getText()), Double.parseDouble(coo[1].getText()), width, height);
			}
    		
    		if (userSpecified!=null) {
    			userSpecified.toRangeSet();
            	if (!posBounds.rangeSet.containsAll(userSpecified.rangeSet)) {
    				result = TARGETOUTOFBOUNDSMESSAGE;
    			}
			} else {
				result = TARGETOUTOFBOUNDSMESSAGE;
			}
        	
		}
    	return result;
	}
    
    public List<Coord> getRectVertices() {
    	String radiusInput  = radius.getText().trim();
    	double width = getWM(radiusInput)/60.;
		double height = getHM(radiusInput)/60.;
		return Util.getRectangleVertices(Double.parseDouble(coo[0].getText()), Double.parseDouble(coo[1].getText()), width, height);
	}

 /** Retourne le target courant en ICRS
  * @param confirm true - teste que le champ est bien renseigné
  */
   protected String getTarget() { return getTarget(true); }
   protected String getTarget(boolean confirm) {
      if( target==null ) return null;
      String s = target.getText().trim();
      if( confirm && s.length()==0 ) {
         if( ball!=null ) ball.setMode(Ball.NOK);
         Aladin.warning(this,WNEEDOBJ);
         return null;
      }
      return aladin.localisation.getICRSCoord(s);
   }

   /** Retourne le radius courant
    * @param confirm true - teste que le champ est bien renseigné et non <0
    */
     protected String getRadius() { return getRadius(true); }
     protected String getRadius(boolean confirm) {
        if( radius==null ) return null;
        String s = radius.getText().trim();
        if( !confirm ) return s;
        if( confirm && (s.length()==0 || getRM(s)<=0) ) {
           ball.setMode(Ball.NOK);
           Aladin.warning(this,WNEEDRAD);
           return null;
        }
        return s;
     }

     /** Retourne la date courante du formulaire
      * @param confirm true - teste que le champ est bien renseigné
      */
      protected String getDate(boolean confirm) {
         if( date==null ) return null;
         String s = date.getText().trim();

         if( s.length()==0 ) {
            ball.setMode(Ball.NOK);
            if( confirm ) Aladin.warning(this,WNEEDDATE);
            return null;
         }
//        try {
//            double Yd = Double.parseDouble(s);
//            s=Astrodate.YdToJD(Yd)+"";
//         } catch( Exception e ) {
//            if( confirm ) Aladin.warning(this,WERRORDATE);
//            return null;
//         }

          return s;
      }



   /**
    * Retourne le rayon exprimé par r en arcmin. r peut être suivi
    * d'une unité. S'il n'y en a pas, l'unité considéré sera celle
    * passé en paramètre (ou celle du formulaire si non indiqué)
    * @param r la chaine exprimant le rayon
    * @param modeRad l'unité par défaut
    * @return le rayon en arcmin
    */
   static protected double getAngleInArcmin(String r,int modeRad) {
      double fct=1.0;		// Fct multiplicatif en fct de l'unite

// System.out.println("getAngle["+r+"]");
// Determination de l'unite
      int offsetD1 = r.indexOf('°');
      int offsetD  = r.indexOf('d');
      int offsetS  = r.indexOf('s');
      int offsetQ  = r.indexOf('\"');
      int offsetM  = r.indexOf('\'');
      
      // si l'utilisateur a passé deux quotes simples, plutôt qu'une double
      if( offsetM<r.length()-1 && r.charAt(offsetM+1)=='\'' ) { offsetQ=offsetM; offsetM=-1; }
      
      int offsetM1 = r.indexOf('m');
      
      if( offsetD1>0 || offsetD>0 && (offsetS==-1 || offsetD>offsetS) ) fct=60.0; // Degres
      else if( offsetS>0 || offsetQ>0) fct=1/60.0;	// Secondes
      else if( offsetM>0 || offsetM1>0 ) fct=1;     // Minutes
      
      // Si absence d'unité, il s'agit de l'unité par défaut (modeRad)
      else {
         if( modeRad==RADIUSd ) fct=60.;
         else if( modeRad==RADIUSs ) fct=1/60.;
      }
      
      r = (new StringTokenizer(r)).nextToken();		// Recup du premier mot
      char [] a = r.toCharArray();
      int i;
      for( i=0; i<a.length && ((a[i]>='0' && a[i]<='9') || a[i]=='.' || a[i]=='-'); i++);
      r = new String(a,0,i);
      return Double.valueOf(r).doubleValue()*fct;
   }
   
//   public static void main(String [] argv) {
//      String s = "14'";
//      double a = getAngle(s,RADIUSs)/60;
//      System.out.println("==>"+s+" ==> "+(a*60)+" ==> "+Coord.getUnit(a));
//   }

   /** Retourne la rayon (en arcmin) associé à un champ décrit par la chaine s */
   static protected double getRM(String s) { return getRWM(s,0); }

   /** Retourne la largeur (en arcmin) associé à un champ décrit par la chaine s */
   static protected double getWM(String s) { return getRWM(s,1); }

   /** Retourne la hauteur (en arcmin) associé à un champ décrit par la chaine s */
   static protected double getHM(String s) { return getRWM(s,2); }

   /** Retourne la taille (en arcmin) associé à un champ décrit par une chaine
    * Si le champ est donné sous forme d'un rayon on retournera le cas échéant le carré ENGLOBE,
    * et si le champ est donné sous forme d'un rectangle on retournera le cas échéant le cercle ENGLOBANT.
    * @param s le champ (ex: 12"  ou 13' x 14'  ou 1.5 deg)
    * @param mode 0 - le rayon, 1 - la largeur, 2 - la hauteur
    */
   static private double getRWM(String s,int mode) {
      double rm,wm,hm;

      if( s.length()==0 ) return 0;

      int i = getDelimiterIndex(s);

      // Le Champ est exprimé en Rayon (un seul paramètre)
      if( i<0 ) {
         rm = getAngleInArcmin(s,RADIUS); // Ca colle direct pour rm
         hm=wm = 2*rm;            // On prend la boite à l'extérieur du cercle pour hm,wm

      // Le Champ est exprimé en Rectangle (2 paramètres)
      } else {
         wm = getAngleInArcmin(s.substring(0,i),RADIUS);  // On récupère la largeur
         hm = getAngleInArcmin(s.substring(i+1),RADIUS);  // On récupère la hauteur
         rm = Math.sqrt(wm*wm/4+hm*hm/4);         // On prend le cercle englobant la boite
      }

      switch(mode) {
         case 0: return rm;
         case 1: return wm;
         case 2: return hm;
      }
      return 0;
   }
   
   public static int getDelimiterIndex(String s) {
	   int i = s.indexOf(',');
	   if( i<0 ) i = s.trim().indexOf('x');
	   return i;
   }

  /** Clear du formulaire (et reaffichage) */
   protected void clear() {
      ball.setMode(Ball.UNKNOWN);
      resolveRadius(ServerDialog.DEFAULTTAILLE,true);
      if( target!=null ) target.setText("");
      aladin.dialog.setDefaultTarget("");
   }

   // Dans le cas de formulaire en deux étapes, met en exergue
   // l'indication de l'étape courante en fonction de l'arbre
   // des métadonnées
   static private String EXERGUE = ">>> ";
   protected void setStepColor(JLabel step1, JLabel step2) {
      if( tree==null ) return;
      JLabel step,oldStep;
      if( tree.isEmpty() ) { step=step1; oldStep=step2; }
      else { oldStep=step1; step=step2; }
      step.setForeground(Color.blue);
      oldStep.setForeground(Color.black);
      String a = oldStep.getText();
      if( a.startsWith(EXERGUE) ) oldStep.setText(a.substring(EXERGUE.length()));
      a = step.getText();
      if( !a.startsWith(EXERGUE) ) step.setText(EXERGUE+a);

   }

  /** Reset du formulaire (et reaffichage) */
   protected void reset() {
      ball.setMode(Ball.UNKNOWN);
      aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),5);
   }

  /** Pre-remplissage du(des) champ target + activation eventuelle
   * du bouton GrabIt
   * @param s La chaine a mettre dans le champ target
   */
   protected void setTarget(String s) {
      if( target==null || s.equals(aladin.GETOBJ) ) return;
      target.setText(s);

      // Activation ou non du bouton GrabIt
//      if( aladin.dialog!=null && !aladin.dialog.isGrabIt() && grab!=null ) {
//         Plan pref = aladin.calque.getPlanRef();
//         boolean grabEnable = pref!=null && Projection.isOk(pref.projd);
//         grab.setEnabled(grabEnable);
//      }
   }

   protected boolean updateWidgets() {
      int widgetsUpdateCounter = 0;
      if( aladin.dialog!=null ) {
         this.updateWidgets(aladin.dialog);
         widgetsUpdateCounter++;
      }

      if( aladin.additionalServiceDialog!=null ) {
         this.updateWidgets(aladin.additionalServiceDialog);
         widgetsUpdateCounter++;
      }

      // Activation ou non du bouton GrabIt
      return widgetsUpdateCounter==2;
   }

   protected void updateWidgets(ServerDialog dialog) {
	   if( !dialog.isGrabIt() && grab!=null ) {
	         Plan pref = aladin.calque.getPlanRef();
	         boolean grabEnable = pref!=null && Projection.isOk(pref.projd);
	         grab.setEnabled(grabEnable);
	      }
   }
   
   protected void updateWidgets(GrabItFrame frame) {
	   if( !frame.isGrabIt() && grab != null ) {
	         Plan pref = aladin.calque.getPlanRef();
	         boolean grabEnable = pref != null && Projection.isOk(pref.projd);
	         grab.setEnabled(grabEnable);
	      }
   }
   
   /**
    * Quickfix for a (one of datalink) SODA form to not have the default date like Skybot
    * It would not be a travesty not to have this quickfix
    * It will only fill the SODA client form with an epoch-in ISO which the client has to change to proper format
    * So we will not fill epoch in the soda form
    * @return
    */
	public boolean setDateForServerGluIsDateLinkForms() {
		boolean result = false;
		if (this instanceof ServerGlu) {
			if (((ServerGlu) this).actionName.equals(DATALINK_FORM)
					&& (date.getText() != null || !date.getText().isEmpty())) {
				result = true;
			}
		}
		return result;
	}

   /** Pre-remplissage du champ Date. Si c'est une valeur double, on considère
    * que c'est une année décimale et on la convertit en JD, sinon on laisse
    * tel que.
    * @param s La chaine a mettre dans le champ Date
    */
    protected void setDate(String s) {
       if( date==null ) return;
       if( setDateForServerGluIsDateLinkForms()) return;
       // On suppose que s est en année décimale (via getEpoch() )
       try {
// Methode Fox
//          Astrotime a = new Astrotime();
//          a.set(s);
//          date.setText( a.getJD()+"" );

          double Yd = Double.valueOf( s ).doubleValue();
//          date.setText( Astrodate.YdToJD(Yd)+"" );
          s=Astrodate.JDToDate(Astrodate.YdToJD(Yd));
          date.setText(s);

       } catch( Exception e ) {
             date.setText(s);
       }

       // résolution du bug #241 (cf. http://pclx5:8080/support/issue241 )
       try {
       	date.setCaretPosition(Math.min(date.getCaretPosition(), s.length()));
       }
       catch(Exception e) {}

    }
    
    /**
	 * Method to process date to MJD
     * @param replaceUserField -to replace the processed value in the user field or not
     * @param input
     * @param strings 
     * @return dateinMJD
     * @throws Exception 
	 */
    public StringBuffer setDateInMJDFormat(boolean replaceUserField, String input, String[] range) throws Exception {
		StringBuffer error = null;
		StringBuffer processedText = null;
		if( date!=null && modeDate == ParseToMJD && input!=null && !input.isEmpty()) {
			processedText = new StringBuffer();
			Pattern p = Pattern.compile(Constants.REGEX_NUMBERNOEXP);
			String delimiterRegex = REGEX_TIME_RANGEINPUT;
			Pattern regex = Pattern.compile(delimiterRegex);
			Matcher matcher = regex.matcher(input);
			String delimiter = getDelimiter(matcher);
			String[] time = input.split(delimiterRegex);
			for (int i = 0; i < time.length; i++) {
				time[i] = time[i].trim();
				Matcher m = p.matcher(time[i]);
				if (m.find()) {
					double timeInput = Double.parseDouble(time[i]);
					error = isValueWithinLimits(timeInput, range, Constants.TIME);
					if (error != null) {
						throw new Exception(error.toString());
					}
					processedText.append(timeInput);
					if (i + 1 < time.length) {
						processedText.append(delimiter);
					}
				} else {
					try {	
						Date date = Util.parseDate(time[i]);
						if (date==null) {
							throw new Exception(DATEFORMATINCORRECT);
						} else {
							double timeInput = Util.ISOToMJD(date);
							processedText.append(timeInput);
							if (i + 1 < time.length) {
								processedText.append(delimiter);
							}
							error = isValueWithinLimits(timeInput, range, Constants.TIME);
							if (error != null) {
								throw new Exception(error.toString());
							}
						}
						
					} catch (ParseException pe) {
						// TODO Auto-generated catch block
						pe.printStackTrace();
						throw pe;
					}
					
				}
			}
			if (processedText!=null && processedText.length()!=0) {
				if (replaceUserField) {
					date.setText(processedText.toString());
				}
				processedText = new StringBuffer(processedText.toString().replaceAll(delimiter, " "));
			}
			
			System.out.println(date.getText());
		}
		return processedText;
	}
   
	/**
	 * Method to process spectral band inputs
	 * @param replaceUserField -to replace the processed value in the user field or not
	 * @param input
	 * @throws Exception 
	 */
	public StringBuffer processSpectralBand(boolean replaceUserField, String input, String[] range) throws Exception {
		StringBuffer error = null;
		StringBuffer result = null;
		if (band!=null && (modeBand == BANDINMETERS) && input!=null && !input.isEmpty()) {
			result = new StringBuffer();
			String delimiterRegex = REGEX_BAND_RANGEINPUT;
			Pattern regex = Pattern.compile(delimiterRegex);
			Matcher matcher = regex.matcher(input);
			String delimiter = getDelimiter(matcher);
			String[] spectralBand = input.split(delimiter);
			for (int i = 0; i < spectralBand.length; i++) {
				double bandInputInMeters = ScientificUnitsUtil.getUnitInMeters(spectralBand[i].trim());
				error = isValueWithinLimits(bandInputInMeters, range, Constants.BAND);
				if (error!=null) {
					throw new Exception(error.toString());
				}
				result.append(bandInputInMeters);
				if ((i + 1) < spectralBand.length) {
					result.append(delimiter);
				}
			}
			if (result!=null && result.length()!=0) {
				if (replaceUserField) {
					band.setText(result.toString().trim());
				}
				result = new StringBuffer(result.toString().trim().replaceAll(delimiter, " "));
			}
			
		}
		return result;
	}
	
	/**
	 * Extract delimiter or return default -1;
	 * @param matcher
	 * @return
	 */
	public static String getDelimiter(Matcher matcher) {
		String delimiter = null;
		if (matcher.find()) {
			delimiter = matcher.group("delimiter");
		}
		if (delimiter == null) {
			delimiter = ",";
		}
		return delimiter;
	}
	
	/**
	 * Method to check if field value is within the limits
	 * @param input
	 * @param range
	 * @param paramName
	 * @return message to display incase of limit violation
	 */
	public StringBuffer isValueWithinLimits(String input, String[] range, String paramName) {
		StringBuffer output = null;
		if (range!=null &&  input!=null && !input.isEmpty() && range[0]!=null && range[1]!=null) {
			String [] inputs = input.split("\\s");
			for (int i = 0; i < inputs.length; i++) {
				Double inputNumber = Double.parseDouble(inputs[i]);
				output = isValueWithinLimits(inputNumber, range, paramName);
			}
		}
		return output;
	}
	
	/**
	 * Method to check if field value is within the limits
	 * @param input
	 * @param range
	 * @param paramName
	 * @return message to display incase of limit violation
	 */
	public StringBuffer isValueWithinLimits(double input, String[] range, String paramName) {
		StringBuffer output = null;
		if (range!=null && ((isValidNumberRange(range[0]) && input<Double.parseDouble(range[0])) || (isValidNumberRange(range[1]) && input>Double.parseDouble(range[1])))) {
			output = new StringBuffer("Please specify ");
			if (paramName==null) {
				output.append(" value");
			}else {
				output.append(paramName);
			}
			output.append(" between ").append(range[0]).append(" and ").append(range[1]);
		}
		return output;
	}
	
	public static boolean isValidNumberRange(String range) {
		return range!=null && !range.trim().isEmpty(); 
	}
	
	
	/**
	 * Method to check if the field value is within the allowed values
	 * @param input
	 * @param allowedValues
	 * @param paramName
	 * @return message to display incase of violation
	 */
	public StringBuffer isValueWithinGivenOptions(String inputs, String[] allowedValues, String paramName) {
		boolean valueFound = false;
		StringBuffer output = null;
		String[] input= null;
		if (allowedValues!=null && inputs!=null && !inputs.isEmpty()) {
			input=inputs.split(" ");
			for (int i = 0; i < input.length; i++) {
				for (int j = 0; j < allowedValues.length; j++) {
					if (allowedValues[j]!=null && input[i].equalsIgnoreCase(allowedValues[j])) {
						valueFound = true;
						break;
					}
				}
				if (!valueFound) {
					break;
				}
			}
			if (!valueFound) {
				output = new StringBuffer("Please specify ");
				if (paramName==null) {
					output.append(" value ");
				}else {
					output.append(paramName);
				}
				output.append("within: ").append(Arrays.toString(allowedValues).replaceAll("[\\[\\]null(,$)]", ""));
			}
			
		}
		return output;
	}

    /** Positionnement d'une valeur particulière (s) sur un component
     * de type INPUT. Nécessaire pour l'utilisation via Aladin script
     */
    protected void setSelectedItem(Component c,String s) {
       if( c instanceof TextField ) { ((TextField)c).setText(s); return; }

       // Il s'agit donc d'un choice
       resumeInputChoice();
       ((JComboBox)c).setSelectedItem(s);
    }

    /** Retourne la liste des plans concerné par un sélecteur de type INPUT */
    protected Plan [] getInputPlane(JComponent c) {
       String masq;

       int n = getFieldInput(c);
       if( n==-1 ) return null;

       // Un seul plan attendu => component est un choice
       if( (modeInput[n] & (ALLIMG|IMG|CAT))!=0 ) {
          masq = (String)((JComboBox)c).getSelectedItem();
          if( masq.equals(NOINPUTITEM) ) return null;

       // multiplan possible => le component est un textField
       } else {
          masq = ((JTextField)c).getText().trim();
          if( masq.length()==0 ) return null;
       }

       // Recherche des plans correspondants
       Vector v = aladin.calque.getPlans(masq);
       if( v==null ) return null;

       // Construction du vecteur résultat en fonction du type de input (IMG|IMGs|CAT|CATs)
       Vector v1 = new Vector();
       Enumeration e = v.elements();
       while( e.hasMoreElements() ) {
          Plan p = (Plan)e.nextElement();
          if( !p.flagOk ) continue;
          if( (modeInput[n] & (CAT|CATs))!=0 && !p.isSimpleCatalog() ) continue;
          if( (modeInput[n] & (ALLIMG|IMG|IMGs))!=0 && !p.isImage() /*!p.hasAvailablePixels()*/ ) continue;
          v1.addElement(p);
          if( (modeInput[n] & (ALLIMG|CAT|IMG))!=0 ) break;	// Un seul
       }

       if( v1.size()==0 ) return null;
       Plan [] plan = new Plan[v1.size()];
       v1.copyInto(plan);
       return plan;

    }

    /** Retourne une chaine constituée de la concaténation de tous les noms de plans
     * correspondant au "masque de nom de plan" indiqué par le Component passé en paramètre
     * Le séparateur est l blanc
     */
    protected String getInputPlaneName(JComponent c) {
       Plan plan [] = getInputPlane(c);
       if( plan==null ) return "";

       StringBuffer res=null;
       for( int i=0; i<plan.length; i++ ) {
          String name = plan[i].getLabel();
          if( res==null ) res = new StringBuffer(name);
          else res.append(" "+name);
       }
       return res==null ? null : res.toString();
    }

    /** Retourne une chaine constituée de la concaténation de toutes les URL de plans
     * correspondant au "masque de nom de plan" indiqué par le Component passé en paramètre
     * Le séparateur est le | (pipe). Les éventuels | se trouvant dans les urls seront
     * backslashés
     * */
    protected String getInputUrl(JComponent c) {
       Plan plan [] = getInputPlane(c);
       if( plan==null ) return "";

       StringBuffer res=null;
       for( int i=0; i<plan.length; i++ ) {
          String url = plan[i].getUrl();
          if( url==null || url.startsWith("file:") ) url = Export.export(plan[i]);  // Serveur local
          if( url==null ) continue;

          url=backSlashPipe(url);
          if( res==null ) res = new StringBuffer(url);
          else res.append("|"+url);
       }
       return res==null ? null : res.toString();
    }

     /** Backslash les éventuels '|' */
     static protected String backSlashPipe(String s) {
        char a [] = s.toCharArray();
        StringBuffer res = new StringBuffer(100);
        for( int i=0; i<a.length; i++ ) {
           if( a[i]=='|' ) res.append("\\|");
           else res.append(a[i]);
        }
        return res.toString();
     }

     /** Retourne true si le Component passé en paramètre concerne un des champs
      * du target ou du radius */
     protected boolean isFieldTargetOrRadius(JComponent c) {
        if( coo!=null ) {
           if( coo[0]==c) return true;
           if( coo.length>1 && coo[1]==c ) return true;
        }
        if( rad!=null ) {
           if( rad[0]==c ) return true;
           if( rad.length>1 && rad[1]==c ) return true;
        }
        return false;
     }

     /** Retourne true si le Component passé en paramètre concerne un
      * dhamp de date  */
     protected boolean isFieldDate(JComponent c) {
    	 if( date!=null ) {
             if( date==c) return true;
          }
    	 return false;
     }
     
     protected boolean isFieldBand(JComponent c) {
    	 if( band!=null ) {
             if( band==c) return true;
          }
    	 return false;
     }

     /*protected boolean isFieldDate(JComponent c) {
        return date!=null;
     }*/

     /** Retourne true si le Component passé en paramètre est input */
     protected boolean isFieldInput(JComponent c) {
        return getFieldInput(c)>=0;
     }

     /** Retourne l'indice dans input[] du component passé en paramètre */
     protected int getFieldInput(JComponent c) {
        if( input==null ) return -1;
        for( int i=0; i<nbInput; i++ ) if( input[i]==c ) return i;
        return -1;
     }

     /** Mise à jour dynamique des choices associés aux champs input en fonction
      * des plans disponibles
      */
     protected void resumeInputChoice() {
        if( input==null ) return;
        Vector<Plan> vCat = aladin.calque.getPlanCat();
        Vector<Plan> vImg = aladin.calque.getPlanImg();
        Vector<Plan> vAllImg = aladin.calque.getPlanAllImg();
        for( int i=0; i<nbInput; i++ ) {
           if( (modeInput[i] & (CAT|IMG|ALLIMG))==0 ) continue;
           adjustInputChoice((JComboBox)input[i],
                 (modeInput[i]& IMG)!=0  ? vImg :
                    (modeInput[i]& ALLIMG)!=0  ? vAllImg : vCat, 0);
        }
     }

     /** Surcharge de show() pour remettre à jour les Choice Input */
     public void setVisible(boolean flag) {
        if( flag) {
           resumeTargetLabel();
           resumeInputChoice();
        }
        super.setVisible(flag);
     }

     /**
      * Mise a jour d'un menu deroulant contenant les labels des plans des champs input IMG et CAT
      * @param Choice le Choice à mettre à jour
      * @param v le vector contenant les plans dont les labels vont être utilisés.
      * @param default L'item du menu par defaut
      */
     protected void adjustInputChoice(JComboBox c, Vector v,int defaut) {
        int i=c.getSelectedIndex();
        String s=(i>=0) ? (String)c.getItemAt(i) : null;
        c.removeAllItems();

        c.addItem(NOINPUTITEM);
        if( v!=null ) {
           Enumeration e = v.elements();
           while( e.hasMoreElements() ) c.addItem( ((Plan)e.nextElement()).label );
        }

        // Sélection de l'item désigné
        if (defaut>0) c.setSelectedIndex(defaut);

        // Premier item, ou deuxième item
        else if (s==null || s.equals(NOINPUTITEM)) c.setSelectedIndex( c.getItemCount()>1?1:0);

        // Précédent item sélectionné
        else c.setSelectedItem(s);
     }

   /** Retourne true si le serveur correspond a la chaine
    * passee en parametre (presence de l'identificateur dans nom sans
    * tenir compte des majuscules ni des pluriels en "s")
    * @param s : identificateur du serveur recherche
    * @return true: Ok c'est ce serveur.
    */
    protected boolean is(String s) {
       if( s.endsWith("s") ) s=s.substring(0,s.length()-1);
       StringTokenizer st = new StringTokenizer(aladinLabel," .");
       while( st.hasMoreTokens() ) {
          String m = st.nextToken(" ()");
          if( m.endsWith("s") ) m=m.substring(0,m.length()-1);
          if( s.equalsIgnoreCase(m) ) return true;
       }
       return false;
    }

   /** Creation d'un plan de maniere generique
    * @param target l'objet ou la coordonnees
    * @param radius le rayon de l'interrogation
    * @param criteria les criteres d'interrogation (syntaxe a definir)
    * @param label Le label du plan qui va etre cree
    * @param origin La mention de l'origine de l'image ou des donnees
    * @return le numero du plan dans la pile, -1 si erreur
    */
   protected int createPlane(String target,String radius,String criteria,
   				     String label, String origin) {
      return -1;
   }

  /** Verifications et messages divers associes a la creation d'un nouveau
   * plan
   */
   protected boolean verif(int type,String obj,String qual) {
      return verif(type,obj,qual,null);
   }
   protected boolean verif(int type,String obj,String qual,String other) {
      return true;  // ON NE VERIFIE PLUS MAINTENANT

//      flagVerif=flagVerif && !MyButton.shiftDown();
//      if( flagVerif && aladin.calque.dejaCharge(type,obj,qual,other) ) {
//         ball.setMode(Ball.PARTIAL);
//         Aladin.warning(this,WDEJA,1);
//         return false;
//      }
//
//      flagVerif=true;
//      flagToFront=true;
//      return true;
   }

   /** Pour les interrogations par script, pour les vieux Servers tels que Aladin, VizieR,
    * je déquote les paramètres le cas échéant. De toutes façons, ils ne supportent
    * que des critères sans espaces
    * @param s la liste des critères (ex: ["DSS1",Fits])
    * @return la liste des critères déquotés (ex: [DSS1 Fits])
    */
   static String specialUnQuoteCriteria(String s) {
      if( s==null || s.length()<2 ) return s;
      StringBuffer rep=null;
      Tok st = new Tok(s," ,");
      while( st.hasMoreTokens() ) {
         if( rep==null ) rep=new StringBuffer(st.nextToken());
         else rep.append(","+st.nextToken());
      }
      return rep.toString();
   }

   /** Retourne true si l'utilisateur a indiqué qu'il y avait trop d'images sélectionnées
    * dans l'arbre */
   protected boolean tooManyChecked() {
      int n = tree.nbSelected();
      if( n>0 && n<MAXSELECTEDPLANE ) return false;
      return !aladin.confirmation(this,NOTTOOMANY+" ("+n+")");
   }

   /** Voir classes derivees */
   public void submit() {}

    /** Retourne le target par défaut */
    protected String getDefaultTarget(){ return aladin.dialog.getDefaultTarget(); }

    /** Retourne la taille par défaut */
    protected String getDefaultTaille(){ return aladin.dialog.getDefaultTaille(); }

    /** Retourne la date par défaut */
    protected String getDefaultDate() { return aladin.dialog.getDefaultDate(); }


   /** Memorisation du dernier target/radius saisie  */
   protected void memTarget() {
      if( target!=null && modeCoo!=NOMODE ) aladin.dialog.setDefaultTarget(target.getText().trim());
      if( radius!=null && modeRad!=NOMODE ) aladin.dialog.setDefaultTaille(radius.getText().trim());
   }

   /** Affichage du status report pour le serveur */
   protected void showStatusReport() {
      if( aladin.frameInfoServer==null )  aladin.frameInfoServer = new FrameInfoServer(aladin);
      else if (!(this instanceof ServerTap) && aladin.frameInfoServer.isOfTapServerType()){
    	  aladin.frameInfoServer.dispose();
    	  aladin.frameInfoServer = new FrameInfoServer(aladin);
	}
      
      aladin.frameInfoServer.show(this);
   }

   public void actionPerformed(ActionEvent arg0) {
      Object s = arg0.getSource();
      if( s instanceof JComboBox && tree!=null && !tree.isEmpty() ) tree.clear();
//      if( s instanceof JButton
//            && ((JButton)s).getActionCommand().equals(FrameServer.INFO)) showStatusReport();
      updateWidgets();
   }

//   // Je mange l'évènement pour pas qu'il se propage
//   public boolean action(Event evt, Object what) {
//      if( evt.target instanceof Choice && tree!=null && !tree.isEmpty() ) tree.clear();
//   	  if( what.equals(FrameServer.INFO)) showStatusReport();
//      return true;
//   }

  /** Gestion du ENTER.
   * Pour pouvoir gerer le ENTER comme si on appuyait sur le bouton SUBMIT
   *
   * @see aladin.Server#loadHips()
   */
   public void keyPressed(KeyEvent e) {
      if( e.getSource() instanceof JTextField ) {
         // On modifie quelque chose => on supprime l'arbre
         if( tree!=null && !tree.isEmpty()
                 && !e.isActionKey() && (e.getModifiers()==0 || (e.isShiftDown() && e.getKeyCode()!=KeyEvent.VK_SHIFT)) ) {
             tree.clear();
         }
         if( ball!=null ) ball.setMode(Ball.UNKNOWN);

         // Validation par ENTER
         if( e.getKeyCode()==KeyEvent.VK_ENTER ) {
            flagVerif=!e.isShiftDown();   // Pour ne pas verifier les redondances
            submit();
         }
      }
   }
   
   public void keyReleased(KeyEvent e) { updateWidgets(); }
   public void keyTyped(KeyEvent e) { }

  /** Retourne le Nom du server éventuellement précédé par son Popup.
    * les / seront préfixés par \, les \n seront remplacés par un blanc,
    * les ... seront supprimés en fin de popup.
    * ex: Others/The STScI Server (DSS1\/DSS2)
    */
   protected String [] getNomPaths() {
      StringBuffer res = new StringBuffer();
      int i;

      // On met le Popup en préfixe s'il y a lieu
      if( aladinMenu!=null ) {
         for( i=0 ;i<aladinMenu.length(); i++ ) {
            char c = aladinMenu.charAt(i);
            if( c=='/' ) res.append('\\');
            res.append(c);
         }
         while( res.charAt((i=res.length()-1))=='.' ) res.deleteCharAt(i);
         res.append('/');
      }

      // On met le nom
      for( i=0; i<aladinLabel.length(); i++ ) {
         char c = aladinLabel.charAt(i);
         if( c=='/' ) res.append('\\');
         if( c=='\n') c=' ';
         res.append(c);
      }

      return new String[]{res.toString()};
   }

   /** Test l'égalité du nom du serveur en prenant en compte les \n qui auraient
    * pu être remplacé par ' ' */
   protected boolean sameNom(String s){ return sameNom(aladinLabel,s); }

   static protected boolean sameNom(String nom,String s) {
      int n=nom.length();
      if( s.length()!=n ) return false;
      for( int i=0; i<n; i++ ) {
         char c = nom.charAt(i);
         char d = s.charAt(i);
         if( c=='\n' ) c=' ';
         if( d=='\n' ) d=' ';
         if( c!=d ) return false;
      }
      return true;
   }

   /** Positionne si possible le paramètre principal dans le formulaire
    * (voir ArchiveServer) */
   protected boolean setParam(String param) { return false; }

// /** Retourne le premier mot du nom */
//   protected String getNom() {
//      StringTokenizer st = new StringTokenizer(aladinLabel);
//      return st.nextToken();
//   }
   
   /** Retourne un label associé au plan généré par ce serveur.
    * Si s est vide ou null, ou ne contient qu'un identificateur technique "as id" retourne 
    * un label par défaut. Dans le dernier cas le label par défaut sera inséré en préfixe.
    * Dans tous les autres cas retourne le label proposé. */
   protected String getDefaultLabelIfRequired(String s) { return getDefaultLabelIfRequired(s,aladinLabel); }
   protected String getDefaultLabelIfRequired(String s,String defaut) {
      if( s==null || s.length()==0 ) return defaut;
      return s;
   }

 /** Retrouve le frame.
   * @param c le composante pour lequel on veut retrouver le Frame
   */
   protected Frame getFrame(Component c) {
      while( c!=null && !(c instanceof Frame) ) c=c.getParent();
      return (Frame) c;
   }

   /**
    * Retourne la ligne de description d'un filtre prédéfini
    * @param filter le filtre selon la syntaxe décrite dans filters[]
    * @return la description du filtre, ou null si aucune
    */
   static protected String getFilterDescription(String filter) {
      if( filter.charAt(0)!='#') return null;
      int i=filter.indexOf('\n');
      if( i<0 ) return null;
      return filter.substring(1,i).trim();
   }

   /**
    * Retourne le nom d'un filtre prédéfini
    * @param filter le filtre selon la syntaxe décrite dans filters[]
    * @return le nom du filtre, ou null si probleme
    */
   static protected String getFilterName(String filter) {
      int i=filter.indexOf((filter.charAt(0)=='#' ? "\n":"")+"filter ");
      if( i<0 ) return null;
      int j=filter.indexOf('{',i);
      String s=filter.substring(i+7,j).trim();
//      System.out.println("filter = ["+filter+"]");
//      System.out.println("==> ["+s+"]");
      return s;
   }

   /**
    * Retourne le texte d'un filtre prédéfini sans les commentaires
    * @param filter le filtre selon la syntaxe décrite dans filters[]
    * @return le text du filtre
    */
   static protected String getFilter(String filter) {
      int i=filter.indexOf((filter.charAt(0)=='#' ? "\n":"")+"filter ");
      if( i<0 ) return null;
      return filter.substring(i).trim();
   }

   /**
    * Retourne l'indice du filtre prédéfini
    * @param filters le tableau des filtres prédéfinis
    * @param name le nom ou la description du filtre dont on cherche l'indice
    * @return l'indice du filtre, ou -1 si non trouvé
    */
   static protected int getFilterIndex(String filters[],String name) {
      // on cherche sur les noms des filtres
      for( int i=0; i<filters.length; i++ ) {
         String n = getFilterName(filters[i]);
         if( n!=null && n.equals(name) ) return i;
      }

      // on cherche sur les descriptions des filtres
      for( int i=0; i<filters.length; i++ ) {
         String n = getFilterDescription(filters[i]);
         if( n!=null && n.equals(name) ) return i;
      }
      return -1;
   }

   /**
    * Retourne le script d'un filter prédéfini
    * @param filter le filtre selon la syntaxe décrite dans filters[]
    * @return le script du filtre ou null si problème
    */
   static protected String getFilterScript(String filter) {
      int i=filter.indexOf('{');
      int j=filter.lastIndexOf('}');
      if( i<0 || j<0 ) return null;
      return filter.substring(i+1,j).trim();
   }

   /**
    * Creation d'un choice en fonction des filtres prédéfinis du serveur
    * (cf filters[])
    */
   protected JComboBox createFilterChoice() {
      filtersChoice = new JComboBox();
      populateFilterChoice(filtersChoice);
      return filtersChoice;
   }

   protected void modifyFilterChoice(String AF[]) {
      filters=AF;
      if( filtersChoice==null ) filtersChoice=createFilterChoice();
      else {
         filtersChoice.removeAll();
         populateFilterChoice(filtersChoice);
      }
   }

   /** Peuple la Combobox des filtres dédiés */
   private void populateFilterChoice(JComboBox c) {
      c.addItem(" - no filter -");
      for( int i=0; i<filters.length; i++ ) {
         String name = getFilterDescription(filters[i]);
         if( name==null ) name = getFilterName(filters[i]);
         if( name!=null ) c.addItem(name);
      }
      c.setSelectedIndex(aladin.configuration.getFilter()+1);
   }

   /** Surcharge éventuelle des filtres prédéfinis - par exemple pour Simbad ou NED.
    * Ceci ne concerne pas les serveurs automatiquement créés par le GLU, car
    * ils ont déjà leur définition de filtres prédéfinis */
   protected void getGluFilters(String tagGlu) {

      if( aladin.glu.aladinDicFilters!=null ) {
         Object f = aladin.glu.aladinDicFilters.get(tagGlu);
         if( f!=null ) filters=(String[])f;
      }
   }

   /**
    * Retourne le numéro du filtre sélectionné dans filtersChoice, -1 si aucun ou problème
    */
   protected int getFilterChoiceIndex() {
      int nFilter=-1;
//      int nFilter=aladin.configuration.getFilter();
//      if( nFilter==-1 ) return nFilter;
      if( filtersChoice!=null ) nFilter=getFilterIndex(filters,(String)filtersChoice.getSelectedItem());
      return nFilter;
   }

   /** Positionnement du mode du voyant d'état en fonction du dernier plan
    * de la pile issu de ce serveur */
   protected void setStatus() {
      Plan [] allPlan = aladin.calque.getPlans();
      for( int i=0; i<allPlan.length; i++ ) {
         Plan p = allPlan[i];
         if( p.server!=this || p.type==Plan.NO ) continue;

         if( !p.flagOk && p.error==null ) ball.setMode(Ball.WAIT);
         else if( p.error!=null && !p.hasNoReduction() ) ball.setMode(Ball.PARTIAL);
         else if( p.error!=null ) ball.setMode(Ball.NOK);
         else ball.setMode(Ball.OK);
         return;
      }
      if( ball.isBlinking() ) ball.setMode(Ball.UNKNOWN);
   }

   /**
    * Ajoute au Panel du server les commentaires indiquant que le serveur fournit
    * des filters
    * @param x positionnement en abscisse
    * @param y positionnement en ordonnée
    * @return nouvelle ordonnée
    */
   protected int addFilterComment(int x, int y) {
      JLabel label = new JLabel(HASFILTER1);
      label.setFont(Aladin.ITALIC);
      label.setBounds(x,y,455-x,20); y+=18;
      label.setForeground(Color.blue);
      add(label);
      label = new JLabel(HASFILTER2);
      label.setFont(Aladin.ITALIC);
      label.setBounds(x,y,455-x,20); y+=18;
      label.setForeground(Color.blue);
      add(label);
      return y;
   }


//   public int compareTo(Object o) {
//      Server s = (Server) o;
//      return s.aladinMenuNumber==aladinMenuNumber ? 0 : s.aladinMenuNumber<aladinMenuNumber ? -1 : 1;
//   }
   
   public Server() {}
   
   /** Fournit un Comparator de mouvement pour les tris */
   static protected Comparator getComparator() { return new Server(); }

   public int compare(Object o1, Object o2) {
      Server a1 = (Server)o1;
      Server a2 = (Server)o2;
      if( a1.ordre==a2.ordre ) return 0;
      if( a1.ordre==null ) return -1;
      if( a2.ordre==null ) return 1;
      return a1.ordre.compareTo(a2.ordre);
   }
   
   /**
    * Essentially calls checkQuery and then shows valid message on screen
    * @param arrayList 
    */
	public void checkQueryFlagMessage() {
		try {
			if (this.checkQuery() != null) {
				Aladin.info(this, CHECKQUERY_SUCCESS);
			}
		} catch (UnresolvedIdentifiersException uie) {
			// TODO Auto-generated catch block
			Iterator<adql.parser.ParseException> it = uie.getErrors();
			adql.parser.ParseException ex = null;
			while(it.hasNext()){
				ex = it.next();
				highlightQueryError(tap.getHighlighter(), ex);
			}
			Aladin.warning(this, "Not sure about the highlighted words : " + ex.getMessage());
		}
	}
   /**
	 * Method parses adql query from user using Grégory
	 * Mantelet's (ARI/ZAH) adql parser lib
	 * @return the adql query
	 * @throws UnresolvedIdentifiersException 
	 */
	public ADQLQuery checkQuery() throws UnresolvedIdentifiersException {
		if (tap.getText().isEmpty()) {
			Aladin.warning(this, CHECKQUERY_ISBLANK);
			return null;
		}
		ADQLQuery query = null;
		Highlighter highlighter = tap.getHighlighter();
		try {
			highlighter.removeAllHighlights();
			query = this.adqlParser.parseQuery(tap.getText());//parser already set when/if table changed
		} catch (UnresolvedIdentifiersException ie) {	
			Aladin.trace(3, "Number of errors in the query:"+ie.getNbErrors());
			throw ie;
		} catch (adql.parser.ParseException pe) {
			highlightQueryError(highlighter, pe);
			Aladin.warning(this, "Check the syntax around the highlighted words : " + pe.getMessage());
		} catch (TokenMgrError e) {
			// TODO: handle exception
			Aladin.warning(this, "Incorrect query: " + e.getMessage());
		}
		return query;
	}
	
	/**
	 * Conveinience method. Submits a tap server(glu or ServerTap type) of sync or async requests
	 * @param sync
	 * @param requestParams
	 * @param name
	 * @param url
	 */
	public void submitTapServerRequest(boolean sync, Map<String, Object> requestParams, String name, String url) {
		ADQLQuery query = null;
		try {
			query = checkQuery();
		} catch (UnresolvedIdentifiersException e) {
			//error is handled in the respective checkQuery() methods
			ADQLParser syntaxParser = new ADQLParser();
			try {
				query = syntaxParser.parseQuery(tap.getText());
			} catch (adql.parser.ParseException e1) {
				// TODO Auto-generated catch block
				Aladin.trace(3, "Parse exception with query..");
			}
			Aladin.trace(3, "I do not understand some identifiers..but may be user knows better..");
		}
		if (query != null) {
			try {
				TapManager tapManager = TapManager.getInstance(aladin);
				Aladin.trace(3, "Firing " + sync + " for " + name + " url: " + url + "\n query: " + tap.getText()
						/*+ "\n ADQLQuery: " + query.toADQL()*/ + "\n requestParams: " + requestParams);
				if (sync) {
					//Spec: Synchronous requests may issue a redirect to the result using HTTP code 303: See Other.
					tapManager.fireSync(name, url, tap.getText(), query, requestParams);
				} else {
					tapManager.fireASync(name, url, tap.getText(), query, requestParams);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if( Aladin.levelTrace >= 3 ) e.printStackTrace();
				Aladin.warning(aladin.dialog, "Server error!");
			}
		}
	}
	
	public void highlightQueryError(Highlighter highlighter, adql.parser.ParseException pe) {
		int errorStart = pe.getPosition().beginColumn-1;
		int errorEnd = pe.getPosition().endColumn-1;
		highlightQueryError(highlighter, errorStart, errorEnd);
	}
	
	/**
	 * Method to highlight error written in the tap text field
	 * @param highlighter
	 * @param pe
	 * @param unrecognisedParams -not used currently
	 */
	public void highlightQueryError(Highlighter highlighter, int errorStart, int errorEnd) {
		HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Aladin.LIGHTORANGE);
		try {
//			String tableName = tap.getText().substring(errorStart, errorEnd);
//			unrecognisedParams.add(tableName);
			highlighter.addHighlight(errorStart, errorEnd, painter);
		} catch (BadLocationException e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			//Don't do anything if this feature fails
		} catch (IndexOutOfBoundsException e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			//Don't do anything if this fails
		}
	}


}

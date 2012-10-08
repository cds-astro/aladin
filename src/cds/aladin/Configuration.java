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


package cds.aladin;

import cds.aladin.prop.PropPanel;
import cds.tools.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Gestion du fichier de configuration Aladin. Il va être enregistré dans
 * ".aladin/Aladin.conf" La syntaxe est la suivante: # Commentaires éventuels
 * clé1 valeur 1 clé2 valeur 2 Cette classe utilise un Vector de
 * ConfigurationItem (voir fin de la classe) (key,value). La clé ne peut
 * contenir de blanc -automatiquement remplacé par des soulignés. Pour une ligne
 * de commentaire, la clé "#" est utilisée, pour une ligne vide, la clé " ",
 * mais sans valeur associée Le fichier n'est regénéré que si un élément a été
 * modifié, ajouté ou supprimé pendant la session.
 * 
 * REMARQUE : la gestion des accès concurrent au fichier en cas de multisession
 * par le même utilisateur n'est pas géré actuellement (le dernier est le
 * gagnant).
 * 
 * AIDE : pour ajouter une nouvelle propriété : 1) créer une nouvelle variable
 * pour la clé (ex: BROWSER = "UnixBrowser") 2) Dans le cas où cette propriété
 * peut être modifiée par les préférences de l'utilisateur a) mettre à jour le
 * formulaire: createPanel() b) mettre à jour l'action associée: apply()
 * 
 * @author Pierre Fernique [CDS]
 * @version fév 2007 - Ajout de Simbad pointer
 * @version nov 2005 - création
 */
public final class Configuration extends JFrame 
       implements Runnable, ActionListener, ItemListener, ChangeListener  {
   
   static final String ASTRONOMER    = "astronomer";
   static final String UNDERGRADUATE = "undergraduate";
   static final String PREVIEW       = "preview";
   
   // Nom du fichier de configuration
   private static String   CONFIGNAME = "Aladin.conf";
   
   // NOm du fichier des fonctions locales associées aux bookmarks
   private static String   CONFIGBKM = "Bookmarks.ajs";

   // Les mots clés possibles
   protected static String BROWSER    = "UnixBrowser";
   protected static String GLU        = "DefaultGluSite";
   protected static String DIR        = "DefaultDir";
//   protected static String PIXEL      = "PixelMode";
   protected static String CM         = "DefaultCM";
   protected static String BKG        = "BackgroundCM";
   protected static String POSITION   = "PositionMode";
   protected static String LANG       = "Language";
   protected static String CSV        = "CSVchar";
   protected static String SMB        = "SimbadPointer";
   protected static String FILTER     = "DedicatedFilter";
   protected static String TRANSOLD   = "FootprintTransparency";
   protected static String TRANS      = "Transparency";
   protected static String TRANSLEVEL = "TransparencyLevel";
   protected static String SURVEY     = "Survey";
   protected static String SERVER     = "Server";
   protected static String WINLOC     = "WindowLocation";
   protected static String RETICLE    = "Reticle";
   protected static String TOOLTIP    = "Tooltip";
   protected static String SCROLL     = "AutoScroll";
   protected static String MOD        = "Profile";
   protected static String HPXGRID    = "HealpixGrid";
   protected static String MESURE     = "HideMeasurements";
   protected static String MHEIGHT    = "MeasurementHeight";
   protected static String BOOKMARKS  = "Bookmarks";
   protected static String FRAME      = "Frame";
   protected static String FRAMEALLSKY= "FrameAllsky";
   protected static String VERSION    = "OfficialVersion";
   protected static String CACHE      = "HpxCacheSize";
   protected static String MAXCACHE   = "HpxMaxCacheSize";
   protected static String LOG        = "Log";
   protected static String HELP       = "Wizard";
//   protected static String TAG        = "CenteredTag";
//   protected static String WENSIZE    = "WenSize";
   
   static String NOTACTIVATED = "Not activated";
   static String ACTIVATED = "Activated";
   
   // Les labels des boutons
   static String TITLE,DEFDIR,DEFDIRH,LANGUE,LANGUEH,LANGCONTRIB,CSVCHAR,CSVCHARH,PIXB,PIXH,PIX8,PIXF,
                 CMB,CMH,CMV,CMM,CMC,CMF,BKGB,BKGH,WEBB,WEBH,RELOAD,
                 REGB,REGH,/*REGCL,REGMAN,*/APPLY,CLOSE,/*GLUTEST,GLUSTOP,*/BROWSE,FRAMEB,FRAMEALLSKYB,FRAMEH,OPALEVEL,
                 FILTERB,FILTERH,FILTERN,FILTERY,SMBB,SMBH,TRANSB,TRANSH,
                 IMGB,IMGH,IMGS,IMGC,MODE,MODEH,CACHES,CACHEH,CLEARCACHE,LOGS,LOGH,HELPS,HELPH/*,TAGCENTER,TAGCENTERH*/;
   
   static private String CSVITEM[] = { "tab","|",";",",","tab |","tab | ;" };
   static private String CSVITEMLONG[];

   // référence externe
   private Aladin          aladin;

   // Contient les propriétés (ConfigurationItem)
   private Vector          prop;

   // Les flags d'états
   private boolean         flagModif;            // true si la config est modifiée pendant la session
   private boolean         first      = true;    // Pour savoir si le JPanel à déjà été créé
   private boolean         flagModifLang=true;   // true si on vient de modifier la langue (ou au démarrage)
   private String          currentLang="En";     // Le suffixe de la langue courante

   // Les variables pour la gestion des champs de préférences
   private JTextField       browser;              // Pour la saisie du browser de l'utilisateur
   private JTextField       dir;                  // Pour la saisie du répertoire par défaut
   private JTextField       maxCache;             // Pour la saisie de la taille du cache
   private JLabel           cache;                // Pour indiquer la valeur du cache
//   private JComboBox        pixelChoice;          // Pour la sélection du mode Pixel par défaut
   private JComboBox        frameChoice;          // Pour la sélection du frame par défaut
   private JComboBox        frameAllskyChoice;    // Pour la sélection du frame par défaut dans le cas des Allsky
   private JComboBox        videoChoice;          // Choix du mode vidéo
   private JComboBox        mapChoice;            // Choix de la color map
   private JComboBox        cutChoice;            // Choix de l'autocut
   private JComboBox        fctChoice;            // Choix de la fonction de transfert
   private JComboBox        gluChoice;            // Pour la sélection du site GLU
   private JComboBox        langChoice;           // Pour la sélection de la langue
   private JComboBox        modeChoice;           // Pour la sélection du mode (astronomers | undergraduate)
//   private JComboBox        smbChoice;            // Pour la sélection du mode Simbad pointer
   private JComboBox        filterChoice;         // Pour l'activation du filtre par défaut
   private JComboBox        transparencyChoice;   // Pour l'activation de la transparence des footprints
   private JComboBox        logChoice;            // Pour l'activation des logs
   private JComboBox        helpChoice;           // Pour l'activation de l'aide des débutants
//   private JComboBox        tagChoice;           // Pour l'activation du centrage des tags
   private JSlider          transparencyLevel;    // niveau de transparence pour footprints
   private JComboBox        csvChoice;            // Pour la sélection du caractère CSV
   private int              langItem;             // Pour savoir si langChoice a été modifié
   private int              modeItem;             // Pour savoir si modeChoice a été modifié
   private int 	            csvItem;              // Pour savoir si csvChoice a été modifié
   private Vector           gluUrl;               // Les Urls correspondantes
   private JTextField       serverTxt;            // Le serveur d'images par défaut
   private JTextField       surveyTxt;            // La couleur/survey par défaut
   private int              lastGluChoice=-1;     // Dernier choix glu validé
   private JButton          reload;               // Le bouton de reload du glu

   
   static private Langue lang[];                  // La liste des langues installées
   private Vector remoteLang = null;              // Lal iste des langues connues mais non installées
   

   protected void createChaine() {
      TITLE = aladin.chaine.getString("UPTITLE");
      DEFDIR = aladin.chaine.getString("UPDEFDIR");
      DEFDIRH = aladin.chaine.getString("UPDEFDIRH");
      LANGUE = aladin.chaine.getString("UPLANGUE");
      LANGUEH = aladin.chaine.getString("UPLANGUEH");
      LANGCONTRIB = aladin.chaine.getString("UPLANGCONTRIB");
      MODE = aladin.chaine.getString("UPMODE");
      MODEH = aladin.chaine.getString("UPMODEH");
      CSVCHAR = aladin.chaine.getString("UPCSVCHAR");
      CSVCHARH = aladin.chaine.getString("UPCSVCHARH");
      PIXB = aladin.chaine.getString("UPPIXB");
      PIXH = aladin.chaine.getString("UPPIXH");
      PIX8 = "8 bit grey level";
      PIXF = "Full pixel";
      CMB = aladin.chaine.getString("UPCMB");
      CMH = aladin.chaine.getString("UPCMH");
      CMV = aladin.chaine.getString("UPCMV");
      CMM = aladin.chaine.getString("UPCMM");
      CMC = aladin.chaine.getString("UPCMC");
      CMF = aladin.chaine.getString("UPCMF");
      BKGB = aladin.chaine.getString("UPBKGB");
      BKGH = aladin.chaine.getString("UPBKGH");
      WEBB = aladin.chaine.getString("UPWEBB");
      WEBH = aladin.chaine.getString("UPWEBH");
      REGB = aladin.chaine.getString("UPREGB");
      REGH = aladin.chaine.getString("UPREGH");
      RELOAD = aladin.chaine.getString("PROPRELOAD");
//      REGCL = aladin.chaine.getString("UPREGCL");
//      REGMAN = aladin.chaine.getString("UPREGMAN");
      APPLY = aladin.chaine.getString("UPAPPLY");
      CLOSE = aladin.chaine.getString("UPCLOSE");     
//      GLUTEST = aladin.chaine.getString("UPTEST");     
//      GLUSTOP = aladin.chaine.getString("UPSTOP");   
      BROWSE = aladin.chaine.getString("FILEBROWSE");
      FRAMEB = aladin.chaine.getString("UPFRAMEB");
      CACHES = aladin.chaine.getString("UPCACHE");
      CLEARCACHE = aladin.chaine.getString("NPRESET");
      CACHEH = aladin.chaine.getString("UPCACHEH");
      FRAMEALLSKYB = aladin.chaine.getString("UPFRAMEALLSKYB");
      FRAMEH = aladin.chaine.getString("UPFRAMEH");
      OPALEVEL = aladin.chaine.getString("PROPOPACITYLEVEL");
      SMBB = aladin.chaine.getString("UPSMBB");
      SMBH = aladin.chaine.getString("UPSMBH");
      FILTERB = aladin.chaine.getString("UPFILTERB");
      FILTERH = aladin.chaine.getString("UPFILTERH");
      TRANSB = aladin.chaine.getString("UPTRANSB");
      TRANSH = aladin.chaine.getString("UPTRANSH");
      IMGB = aladin.chaine.getString("UPIMGB");
      IMGH = aladin.chaine.getString("UPIMGH");
      IMGS = aladin.chaine.getString("UPIMGS");
      IMGC = aladin.chaine.getString("UPIMGC");
      LOGS = aladin.chaine.getString("UPLOG");
      LOGH = aladin.chaine.getString("UPLOGH");
      HELPS = aladin.chaine.getString("UPHELP");
      HELPH = aladin.chaine.getString("UPHELPH");
//      TAGCENTER = aladin.chaine.getString("UPTAGCENTER");
//      TAGCENTERH = aladin.chaine.getString("UPTAGCENTERH");
      
      CSVITEMLONG = new String[] { "Tab","Pipe (|)","Semicolon (;)",
                                   "Comma (,)","Tab or Pipe (|)","Tab, Pipe (|) or Semicolon (;)" };
   }
   
   /** Création de la configuration */
   public Configuration(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false,aladin);
      prop = new Vector(10);
      flagModif = false;
   }

   /** Construction du panel des boutons de validation
    * @return Le panel contenant les boutons Apply/Close
    */
    protected JPanel getValidPanel() {
       JPanel p = new JPanel();
       p.setLayout( new FlowLayout(FlowLayout.CENTER));
       JButton b;
       p.add( b=new JButton(APPLY)); b.addActionListener(this);
       b.setFont(b.getFont().deriveFont(Font.BOLD));
       p.add( b=new JButton(CLOSE)); b.addActionListener(this);
       return p;
    }
   /**
    * Affichage du panel pour permettre à l'utilisateur de modifier sa
    * configuration
    */
   public void show() {
      if( first ) {
         createChaine();
         setTitle(TITLE);
         ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
         Aladin.makeAdd(getContentPane(), createPanel(), "Center");
         Aladin.makeAdd(getContentPane(), getValidPanel(), "South");
         pack();
         first = false;
         setLocation(Aladin.computeLocation(this));
      }
      updateWidgets();
      super.show();
   }

   /**
    * Génére le JComboBox des sites GLU possibles et positionne celui qui est
    * choisi par défaut
    */
   private void createGluChoice() {
      gluChoice = new JComboBox();
      gluChoice.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            if( reload!=null ) {
               int index = gluChoice.getSelectedIndex();
               reload.setEnabled( index==lastGluChoice );
            }
         }
      });
      gluUrl = new Vector();
      Enumeration e = aladin.glu.aladinDic.keys();
      int i = 0;
      while( e.hasMoreElements() ) {
         String key = (String) e.nextElement();
         if( !key.startsWith("AlaU.") ) continue;
         String v = (String) aladin.glu.aladinDic.get(key);
         int k = v.indexOf("//");
         if( i < 0 ) continue;
         int j = v.indexOf("/", k + 2);
         if( j < 0 ) continue;
         gluUrl.addElement(v);
         v = v.substring(k + 2, j);
         gluChoice.addItem(v);
         i++;
      }
   }
   
   
   /** Spécifie le mode d'affichage des pixels */
//   private void setPixelMode(String mode) throws Exception {
//      if( !aladin.pixel.setPixelMode(mode) ) {
//         throw new Exception("Not available pixel mode ! ["+mode+"]");
//      }
//   }
   
   /** Spécifie le mode de synchronisation des commandes script */
   private void setSyncMode(String mode) throws Exception {
      mode = Util.toUpper(mode);
      aladin.command.setSyncMode( mode==null || mode.indexOf("OFF")>=0 || mode.indexOf("AUTO")>=0 ? Command.SYNCOFF : Command.SYNCOFF);
   }
   
   /** Spécifie le mode du simbad pointer. Si la présence de la sous-chaine NO ou OFF
    * est détecté on désactive, sinon on active */
   private void setSimbadMode(String mode) throws Exception {
      mode = Util.toUpper(mode);
      boolean flag = mode!=null && mode.indexOf("NO")<0 && mode.indexOf("OFF")<0;
      aladin.calque.flagSimbad=flag;
      aladin.setButtonMode();
   }
   
   /** Spécifie le mode du filtre dédié. Si la présence de la sous-chaine NO ou OFF
    * est détecté on désactive, sinon on active */
   private void setFilterMode(String mode) throws Exception {
      aladin.FILTERDEFAULT=Util.toUpper(mode);
   }
   
   // Liste des valeurs possibles pour la commandes "setconf cm="
   static protected String CMPARAM[] = null;
   
   /** Spécifie le mode de mapping de pixel par défaut (via command setconf)
    * La sauvegarde dans le fichier de conf ne sera pas faite pour autant */
   private void setCMMode(String mode) throws Exception { setMode1(mode,false); }
   
   /** Spécifie le mode de mapping du background par défaut (via command setconf)
    * La sauvegarde dans le fichier de conf ne sera pas faite pour autant */
   private void setBkgMode(String mode) throws Exception { setMode1(mode,true); }
   
   // Voir setCMMod() et setBkgMode()
   private void setMode1(String mode,boolean flagBackground) throws Exception {
      if( CMPARAM==null ) {
         CMPARAM = new String[4+PlanImage.TRANSFERTFCT.length+FrameCM.CMA.length];
         CMPARAM[0] = "reverse"; CMPARAM[1]="noreverse";
         CMPARAM[2] = "autocut"; CMPARAM[3]="noautocut";
         System.arraycopy(PlanImage.TRANSFERTFCT,0,CMPARAM,4,PlanImage.TRANSFERTFCT.length);
         System.arraycopy(FrameCM.CMA,0,CMPARAM,4+PlanImage.TRANSFERTFCT.length,FrameCM.CMA.length);
      }
      Tok tok = new Tok(mode);
      StringBuffer cm = null;
      while( tok.hasMoreTokens() ) {
         String s = tok.nextToken();
         int i;
         if( cm==null ) cm = new StringBuffer();
         else cm.append(' ');
         if( (i=Util.indexInArrayOf(s,CMPARAM, true))<0 ) throw new Exception("Not available cm mode ! ["+s+"]");
         else cm.append(CMPARAM[i]);
      }
      if( flagBackground ) aladin.BKGDEFAULT=cm.toString();
      else aladin.CMDEFAULT=cm.toString();
      updateWidgets();
   }
   
   /** Positionnement du CSV */
   private void setCSV(String cs) {
      if( cs==null ) return;
      
      StringBuffer colsep = new StringBuffer();      
      StringTokenizer st = new StringTokenizer(cs," ");
      while( st.hasMoreTokens() ) {
         String s = st.nextToken();
         if( s.equals("tab") ) colsep.append('\t');
         else colsep.append(s.charAt(0));
      }
      aladin.CSVCHAR=colsep.toString();
   }
   
//   /** Positionnement du CENTEREDTAG - outil tag autocentré */
//   private void setCENTEREDTAG(String s) {
//      if( s==null ) return;
//      aladin.CENTEREDTAG = s.equals(ACTIVATED);
//   }
   
   /** Positionnement de la taille du cache Healpix en KO */
   private void setMaxCache(String s) {
      if( s==null ) return;
      try {
         long maxCache = Long.parseLong(s);
         PlanBG.setMaxCacheSize(maxCache);
      } catch( Exception e ) {}
   }
   
   /** Transparence des footprints */
   private void setTransparency(String s, float level) {
       transparencyLevel.setToolTipText(OPALEVEL+" : "+(int)(level*100));
	   boolean oTrans = Aladin.ENABLE_FOOTPRINT_OPACITY;
	   boolean trans;
	   if( s.equals(NOTACTIVATED) ) trans = false;
	   else trans = true;
	   
	   if( trans!=oTrans ) {
		   Aladin.ENABLE_FOOTPRINT_OPACITY = trans;
		   aladin.calque.repaintAll();
	   }
	   
	   float oLevel = Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL;
	   if( level!=oLevel ) {
		   Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL = level;
		   aladin.calque.updateFootprintOpacity(oLevel, level);
		   aladin.calque.repaintAll();
	   }
   }

   /** Selection de l'item du JComboBox du Glu correspondant à s */
   protected void setSelectGluChoice(String s) {
      Enumeration e = gluUrl.elements();
      for( int i = 0; e.hasMoreElements(); i++ ) {
         if( s != null && s.startsWith((String) e.nextElement()) ) {
            gluChoice.setSelectedIndex(i);
            lastGluChoice=i;
            return;
         }
      }
   }

   private Thread gluTestThread = null;

//   /** Lancement du thread de test des GLU */
//   synchronized private void startGluTest() {
//      if( gluTestThread != null ) return;
//      gluTestThread = new Thread(this,"AladinGluTest");
////      gluTest.setText(GLUSTOP);
//      gluTestThread.start();
//   }

//   /** Arrêt du thread du test des GLU */
//   synchronized private void stopGluTest() {
//      if( gluTestThread == null ) return;
//      gluTestThread.interrupt();
////System.out.println("Je tente d'interrompre le thread "+gluTestThread);
//      gluTestThread=null;
////      gluTest.setText(GLUTEST);
//   }

   /** Démarrage dans un Thread séparé du test sur le site GLU le plus proche */
   public void run() { launchGluTest(); }

   /** Lance le test des sites GLU et mémorise le plus rapide */
   private void launchGluTest() {
      Aladin.makeCursor(this, Aladin.WAITCURSOR);
      aladin.glu.testAlaSites(false, true);
      Aladin.makeCursor(this, Aladin.DEFAULT);

      setSelectGluChoice(aladin.glu.NPHGLUALADIN);
   }

   /** Retourne vrai s'il s'agit d'une session Aladin non-applet Unix */
   private boolean isUnixStandalone() {
      if( Aladin.isApplet() ) return false;
      String syst = System.getProperty("os.name");
      if( syst == null || syst.startsWith("Windows") || syst.startsWith("Mac") ) return false;
      return true;
   }
   
   /** Retourne le mode video par défaut pour le background */
   protected int getBkgVideo() {
      String s = aladin.BKGDEFAULT==null ? get(BKG) : aladin.BKGDEFAULT;
      if( s!=null && s.indexOf("noreverse")>=0 ) return PlanImage.VIDEO_NORMAL;
      return Aladin.OUTREACH ? PlanImage.VIDEO_NORMAL : PlanImage.VIDEO_INVERSE;
   }
   
   /** Retourne la fonction de transfert par défaut pour le background */
   protected int getBkgFct() {
      String s = aladin.BKGDEFAULT==null ? get(BKG) : aladin.BKGDEFAULT;
      return getFct1(s);
   }
   
   // Voir getCMFct() et getBkgFct()
   private int getFct1(String s) {
      if( s!=null ) {
         for( int i=0; i<PlanImage.TRANSFERTFCT.length; i++ ) {
            if( s.indexOf(PlanImage.TRANSFERTFCT[i])>=0 ) return i;
         }
      }
      return PlanImage.LINEAR;
   }
   
   /** Retourne la color map par défaut pour le background */
   protected int getBkgMap() {
      String s = aladin.BKGDEFAULT==null ? get(BKG) : aladin.BKGDEFAULT;
      return getMap1(s);
   }
   
   // Voir getCMMap() et getBkgMap()
   private int getMap1(String s) {
      int i;      
      if( s!=null ) {
         for( i=0; i<FrameCM.CMA.length; i++ ) {
            if( s.indexOf(FrameCM.CMA[i])>=0 ) return i;
         }
         if( ColorMap.customCMName!=null ) {
            Enumeration e = ColorMap.customCMName.elements();
            for( ;e.hasMoreElements(); i++ ) {
               String t = (String)e.nextElement();
               if( s.indexOf(t)>=0 ) return i;
            }
         }
      }
      return Aladin.OUTREACH ? 1 : 0;
   }
   
   /** Retourne le mode video par défaut */
   protected int getCMVideo() {
      String s = aladin.CMDEFAULT==null ? get(CM) : aladin.CMDEFAULT;
      if( s!=null && s.indexOf("noreverse")>=0 ) return PlanImage.VIDEO_NORMAL;
      return Aladin.OUTREACH ? PlanImage.VIDEO_NORMAL : PlanImage.VIDEO_INVERSE;
   }
   
   /** Retourne true si par défault l'autocut est activé */
   protected boolean getCMCut() {
      String s = aladin.CMDEFAULT==null ? get(CM) : aladin.CMDEFAULT;
      if( s!=null && s.indexOf("noautocut")>=0 ) return false;
      return true;
   }
   
   /** Retourne la fonction de transfert par défaut */
   protected int getCMFct() {
      String s = aladin.CMDEFAULT==null ? get(CM) : aladin.CMDEFAULT;
      return getFct1(s);
   }
   
   /** Retourne la color map par défaut */
   protected int getCMMap() {
      String s = aladin.CMDEFAULT==null ? get(CM) : aladin.CMDEFAULT;
      return getMap1(s);
   }
   
   /** Retourne le serveur d'images par défaut */
   protected String getServer() {
      String s = get(SERVER);
      if( s==null ) return Aladin.OUTREACH ? "ESO" : "allsky";
      return s;
   }
   
   /** Retourne les bookmarks particuliers, null si aucun */
   public String getBookmarks() {
      if( Aladin.OUTREACH ) return null;
      return get(BOOKMARKS);
   }
   
   /** Supprime les bookmarks particuliers */
   public void resetBookmarks() { remove(BOOKMARKS); }
   
   /** Retourne le survey d'images par défaut */
   protected String getSurvey() {
      return get(SURVEY);
   }
   
   /** Retourne la chaine décrivant la dernière version officielle */
   protected String getOfficialVersion() {
      return get(VERSION);
   }
   
   /** Génère la commande script de chargement d'une image sur le server d'image
    * par défaut définit dans la configuration de l'utilisateur
    * exemple : "get ESO(DSS1)"
    */
   protected String getLoadImgCmd() {
      String survey = getSurvey();
      return getServer()+( survey==null ? "" : "("+survey+")");
   }
   
   /** Retourne 0 si le filtre dédié doit être activé par défaut, sinon -1 */
   protected int getFilter() {
     String s = aladin.FILTERDEFAULT==null ? get(FILTER) : aladin.FILTERDEFAULT;
     if( s!=null && (Util.indexOfIgnoreCase(s,"NO")>=0 
                      || Util.indexOfIgnoreCase(s,"OFF")>=0) ) return -1;
     return 0;
   }
   
   /** retourne le suffixe de la langue courante. En première approximation
    * on utilise simplement les deux premières lettres précédées d'un point,
    * et rien pour l'anglais
    * @return suffixe de la langue (ex: .fr);
    */
   protected String getLang() { 
      if( flagModifLang ) {
         String modeLang = "User";
         flagModifLang=false;
         String s = get(LANG);
         if( s==null ) {
            try {
               modeLang = "Default";
               s = System.getProperty("user.language");
               s = getLanguage(s);               
            }catch( Exception e ) { s=null; }
         }
         currentLang = getLangSuffix(s);
Aladin.trace(2,modeLang+" language ["+s+"] => assume ["+currentLang+"]");
      }
      return currentLang;
   }
   
   /** Retourne l'indice de la frame mémorisée par l'utilisateur, ICRS par défaut */
   protected int getFrame() {
      if( Aladin.OUTREACH ) return Localisation.ICRS;
      try {
         String frame = get(FRAME);
         int i = Util.indexInArrayOf(frame, Localisation.REPERE);
         if( i>=0 ) return i;
      } catch( Exception e ) { }
      return Localisation.ICRS;
   }
   
   
   // EN ATTENDANT
//   protected int getFrameAllsky() { return getFrame(); }
   
   private boolean setConfFrame=false; // true si l'utilisateur a modifié par script 
   
   /** Spécifie le mode d'affichage des positions (J2000, B1950...) */
   private void setPositionMode(String mode) throws Exception {
      if( !aladin.localisation.setPositionMode(mode) ) {
         throw new Exception("Not available position mode ! ["+mode+"]");
      }
      setConfFrame=true;
   }
   
   /** Retourne l'indice de la frame qui sera utilisé par défaut pour le tracé des Allsky */
   protected int getFrameDrawing() {
      if( Aladin.OUTREACH ) return 3;   // GAL
      if( !Aladin.PROTO ) return 0;   // Pour le moment le frame par défaut pour les allsky n'est supporté qu'en mode PROTO   
      
      if( setConfFrame ) return getFrame();   // L'utilisateur a modifié le cas par défaut via une commande setconf frame=
      String frame = get(FRAMEALLSKY);
      try {
         int i = Util.indexInArrayOf(frame, Localisation.FRAME);
         if( i>=0 ) return i;
      } catch( Exception e ) { }
      return 0;   // Default => celui du repère céleste
   }
   
   /** Retourne le code 2 lettres de la langue courante, même pour l'anglais */
   protected String getLanguage() {
      String s = getLang();
      if( s.length()==0 ) return "en";
      return s.substring(1);
   }
   
   /** Retourne true si le mode log est activé */
   protected boolean isLog() {
      String s = get(LOG);
      return s==null || s.equals(ACTIVATED);
   }
   
   /** Retourne true si le mode HELP pour les débutants est activé */
   protected boolean isHelp() {
      String s = get(HELP);
      return s==null || s.equals(ACTIVATED);
   }

   /** Retourne le mode repéré dans le fichier de config */
   protected boolean isOutReach() {
      String s = get(MOD);
      if( s!=null && s.charAt(0)=='u' ) return true;
      return false;
   }
   
   /** Retourne le mode repéré dans le fichier de config */
   protected boolean isBeginner() {
      String s = get(MOD);
      if( s!=null && s.charAt(0)=='p' ) return true;
      return false;
   }
   
   public boolean isTransparent() {
	   String s = get(TRANS);
	   if( s!=null && s.charAt(0)=='N' ) return false;
	   return true;
   }
   
   public float getTransparencyLevel() {
	   String s = get(TRANSLEVEL);
	   if( s==null ) return 0.15f+0.000111f; // valeur par défaut
	   
	   try {
		   float f = Float.parseFloat(s);
		   return f;
	   }
	   catch(NumberFormatException nfe) {return 0.15f+0.000111f;}
   }
   
   /** Ajoute au sélecteur de langue la liste des langues distances en évitant les
    * doublons
    */
   private void addRemoteLanguage() {
      Enumeration e = remoteLang.elements();
      while( e.hasMoreElements() ) {
         Langue lg = (Langue)e.nextElement();
         
         int i,n = langChoice.getItemCount();
         for( i=0; i<n && !((String)langChoice.getItemAt(i)).equals(lg.langue); i++); 
         if( i==n ) langChoice.addItem(lg.langue);
      }
   }
   
   /**
    * Ajoute à remoteLang la langue décrite par le nmo de fichier passé en paramètre
    * qui doit suivre la syntaxe de l'exemple ci-dessous
    * ex : Aladin-SimplifiedChinese-4.024.string.utf
    */
   private void setRemoteLanguage(String t) {
      try {
         int t1 = t.indexOf('-');
         int t2 = t.indexOf('-',t1+1);
         int dot = t.indexOf(".string");
         
         StringBuffer name = new StringBuffer();
         for( int i=t1+1; i<t2; i++ ) {
            char ch = t.charAt(i);
            if( i>t1+1 && Character.isUpperCase(ch) ) name.append(' ');
            name.append(ch);
         }
         Langue lg = new Langue(name.toString());
         
         try { lg.version = Double.parseDouble(t.substring(t2+1,dot) ); }
         catch( Exception e) {}

         lg.file=t;

         remoteLang.addElement(lg);
      } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
   }

   /**
    * Chargement de la description des langues distantes et mise à jour
    * de leur liste dans remoteLang
    */
   protected void loadRemoteLang() {
      remoteLang = new Vector();
      if( !Aladin.STANDALONE ) return;
      
      (new Thread("loadLang") {
         public void run() {
            try {
               Util.pause(1000);
               String s;
               Aladin.trace(3,"Checking language support...");
               InputStream in = aladin.cache.get(Aladin.LANGURL);
               BufferedReader dis = new BufferedReader(new InputStreamReader(in));
               while( (s=dis.readLine())!=null ) {
//System.out.println("Lang="+s);
                  setRemoteLanguage(s);
               }
               in.close();
               
               // Mise à jour éventuelle
               s = get(LANG);
               Langue lg = getBestLangVersion(s);
               if( lg!=null ) {
                  Langue clg = findLangue(s);
                  if( clg!=null && clg.version!=lg.version ) {
                     Util.pause(10000);
                     installRemoteLanguage(s);
                  }
               }
            } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
         }
      }).start();
   }
   
   /** Retourne l'objet Langue correspondant à la chaine */
   private Langue findLangue(String s) {
      for( int i=0; i<lang.length; i++ ) {
         if( lang[i].isLangue(s) ) return lang[i];
      }
      return null;
   }
   
   /** Retourne le code de la langue précédé d'un "." ou "" si inconnu d'Aladin */
   private String getLangSuffix(String s) {
      
      if( lang==null ) return "";
      if( s==null ) return "";
      for( int i=0; i<lang.length; i++ ) {
         if( s.equals(lang[i].langue) ) return ( lang[i].code.length()>0 ? ".":"") + lang[i].code;
      }
      return "";
   }
   
   /** Retourne la langue correspondant au code du pays ou null si inconnu d'Aladin */
   protected String getLanguage(String s) {
      for( int i=0; i<lang.length; i++ ) {
         if( lang[i].isLangue(s) ) return lang[i].langue;
      }
      return null;
   }
   
   /** Retourne l'auteur de la traduction correspondant au code du pays
    * ou null si inconnu d'Aladin */
   protected String getLanguageAuthor(String s) {
      for( int i=0; i<lang.length; i++ ) {
         if( lang[i].isLangue(s) ) return lang[i].auteur;
      }
      return null;
   }
   
   /** Retourne la taille du cache Healpix si on la connait, sinon -1 */
   protected long getHpxCacheSize() {
      String s = get(CACHE);
      if( s==null ) return -1;
      long size;
      try {  size = Long.parseLong(s); }
      catch( Exception e) { size=-1; }
      return size;
   }
   
   private Point initWinLocXY=new Point();      // Position initiale de la fenêtre
   private Dimension initWinLocWH=new Dimension();  // Dimenison initiale de la fenêtre
   private int initMesureHeight=0;               // Taille de la fenêtre des mesures
   
   /** Retourne true si la fenêtre d'Aladin n'a ni bougé, ni été redimensionnée */
   private boolean sameWinParam() {
      if( aladin.isApplet() ) return true;  // pas de gestion de positionnement en mode applet
      Dimension d = aladin.f.getSize();
      Point p = aladin.f.getLocation();
      int mesureHeight = aladin.splitH.getMesureHeight();
      return initWinLocXY.equals(p) && initWinLocWH.equals(d) && initMesureHeight==mesureHeight;
   }
   
   /** Retourne la position et la taille de la fenêtre Aladin. Mémorise
    * cette position pour vérifier qu'à la fin de la session on a bougé ou non */
   protected Rectangle getWinLocation() {
      try {
         String s = get(WINLOC);
         StringTokenizer st = new StringTokenizer(s);
         Rectangle r = new Rectangle(
               Integer.parseInt(st.nextToken()),
               Integer.parseInt(st.nextToken()),
               Integer.parseInt(st.nextToken()),
               Integer.parseInt(st.nextToken())
               );
         setInitWinLoc(r.x,r.y,r.width,r.height);
         return r;
      } catch( Exception e ) { }
      return null;
   }
   
   /** Retourne la proportion de la fenêtre des mesures d'Aladin. Mémorise
    * cette position pour vérifier qu'à la fin de la session on a bougé ou non */
   protected int getWinDivider() {
      String s;
      int mesureHeight=150;
      try { s = get(MHEIGHT);
            mesureHeight=Integer.parseInt(s);
      } catch( Exception e ) {}
      setInitMesureHeight(mesureHeight);
      return mesureHeight;
   }
   
   /** Mémorisation de la position et de la taille de la fenêtre initiale d'Aladin en vue
    * de comparaison (voir save())*/
   protected void setInitWinLoc(int x,int y,int width, int height) {
      initWinLocXY.x=x;
      initWinLocXY.y=y;
      initWinLocWH.width=width;
      initWinLocWH.height=height;
   }
   
   /** Mémorisation de limite de séparation de la fenêtre initiale des mesures d'Aladin en vue
    * de comparaison (voir save())*/
   protected void setInitMesureHeight(int mesureHeight) {
      this.initMesureHeight=mesureHeight;
   }
   
   /** Etend le tableau des langues si nécessaire de n cases et
    * retourne l'indice de la première case inutilisée
    */
   private int extendLang(int n) {
      int i;
      if( lang!=null ) {
         Langue a[] = lang;
         lang = new Langue[a.length+n];
         System.arraycopy(a,0,lang,0,a.length);
         i=a.length;
      } else {
         lang = new Langue[n];
         i=0;
      }
      return i;
   }
      
   /** Construit ou met à jour les tableaux de chaines contenant les langues
    * et les codes associés connues par Aladin (voir lang[] et langCode[]
    * Utilise le mot clé LANGUAGE dans Aladin0.string
    * ex: LANGUAGE  English, Français (fr), Spanish (sp) 5.023 P.Gonzales
    */
   protected void setLanguage(String t) {
      StringTokenizer st = new StringTokenizer(t,",");
      int i,j,k,l;
      
      i=extendLang(st.countTokens());
      
      for( ; i<lang.length; i++ ) {
         String s = st.nextToken();
         int n=s.length();
         lang[i] = new Langue(s);
         
         // Recherche du code de la langue
         j = s.indexOf('(');
         k=-1;
         if( j>0 ) {
            k=s.indexOf(')',j);
            if( k>0 ) {
               lang[i].langue = s.substring(0,j).trim();
               lang[i].code = s.substring(j+1,k);
            }            
         }
         
         // Recherche d'un numéro de version d'Aladin associé
         // (premier nombre après la parenthèse qui contient le code de la langue)
         try {
            if( k>0 ) {
               j=k;
               for( ; k<n && !Character.isDigit(s.charAt(k)); k++ );
               if( k<n ) {
                  l = s.indexOf(' ',k);
                  if( l==-1 ) l=n;
                  try { lang[i].version = Double.parseDouble(s.substring(k,l)); }
                  catch( Exception e) {}
               }

               // Recherche de l'auteur de la traduction
               // (Première chaine qui n'est pas un nombre qui suit la parenthèse du code
               // de la langue)
               for( l=j+1; l<n && !Character.isLetter(s.charAt(l)); l++ );
               if( l<n ) {
                  if( k>l ) lang[i].auteur=s.substring(l,k).trim();
                  else lang[i].auteur=s.substring(l).trim();
               }
            }
         }catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
         
         Aladin.trace(2,"Supported language ["+lang[i].langue+"] ("+lang[i].code+") "
               +(lang[i].version>0?lang[i].version+"":"")+" "+lang[i].auteur);
      }
   }
   
   protected JPanel createPanel() {
      if( Aladin.OUTREACH ) return createPanel1();
      JPanel p = new JPanel( new BorderLayout());
      JScrollPane sc = new JScrollPane(createPanel1());
      p.add(sc,BorderLayout.CENTER);
//      p.setPreferredSize(new Dimension(580,600));
      return p;
   }

   /** Construction du panel de la configuration utilisateur */
   private JPanel createPanel1() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,2,2,2);
      JLabel l;
      JButton b;
      JComboBox x;
      JPanel panel;

      JPanel p = new JPanel();
      p.setLayout(g);

      // Le langage de l'interface
      panel = new JPanel(new BorderLayout(5,5));
      langChoice = new JComboBox();
      langChoice.addItem("-- default --");
      for( int i=0; i<lang.length; i++ ) langChoice.addItem(lang[i].langue);
      (l = new JLabel(LANGUE)).setFont(l.getFont().deriveFont(Font.BOLD));
      panel.add(langChoice,BorderLayout.CENTER);
      if( !Aladin.OUTREACH ) {
         b=new JButton(LANGCONTRIB); b.addActionListener(this);
         b.setMargin( new Insets(2,4,2,4));
         panel.add(b,BorderLayout.EAST);
      }
      PropPanel.addCouple(this, p, l, LANGUEH, panel, g, c, GridBagConstraints.EAST);           
      
      addRemoteLanguage();
      
      // Le mode de l'interface (uniquement si non modifié par paramètre
      // sur la ligne de commande
      modeChoice = new JComboBox();
      modeChoice.addItem(ASTRONOMER);
      modeChoice.addItem(UNDERGRADUATE);
      modeChoice.addItem(PREVIEW);
      (l = new JLabel(MODE)).setFont(l.getFont().deriveFont(Font.BOLD));
      if( !aladin.setOUTREACH ) {
         PropPanel.addCouple(this, p, l, MODEH, modeChoice, g, c, GridBagConstraints.EAST);           
      }
      
      if( !Aladin.OUTREACH ) {
         (l = new JLabel(HELPS)).setFont(l.getFont().deriveFont(Font.BOLD));
         helpChoice = new JComboBox();
         helpChoice.addItem(ACTIVATED);
         helpChoice.addItem(NOTACTIVATED);
         PropPanel.addCouple(this, p, l, HELPH, helpChoice, g, c, GridBagConstraints.EAST);
      }
      
      // Le Répertoire par défaut
      dir = new JTextField(35);
      b=new JButton(BROWSE); b.addActionListener(this);
      b.setMargin( new Insets(2,4,2,4));
      (l = new JLabel(DEFDIR)).setFont(l.getFont().deriveFont(Font.BOLD));
      panel = new JPanel(new BorderLayout(5,5));
      panel.add(dir,BorderLayout.CENTER);
      panel.add(b,BorderLayout.EAST);
      PropPanel.addCouple(this, p, l,DEFDIRH, panel, g, c, GridBagConstraints.EAST);
      
      // Le frame
      frameChoice = aladin.localisation.createChoice();
      frameAllskyChoice = aladin.localisation.createFrameCombo();
      (l = new JLabel(FRAMEB)).setFont(l.getFont().deriveFont(Font.BOLD));
      panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(frameChoice);
      if( aladin.PROTO ) {
         panel.add(new JLabel(" - "+FRAMEALLSKYB));
         panel.add(frameAllskyChoice);
      }
      if( !aladin.OUTREACH ) {
         PropPanel.addCouple(this, p, l, FRAMEH, panel, g, c, GridBagConstraints.EAST);
      }
      
      // Le mode pixel
//      pixelChoice = new JComboBox();
//      pixelChoice.addItem(PIXF);      
//      pixelChoice.addItem(PIX8);
      
      // Le pixel mapping
      videoChoice = x=new JComboBox(); x.addItem("reverse"); x.addItem("noreverse");
      mapChoice = x = FrameCM.createChoiceCM();
      cutChoice = x=new JComboBox();   x.addItem("autocut"); x.addItem("noautocut");
      fctChoice = x=new JComboBox();   for( int i=0; i<PlanImage.TRANSFERTFCT.length; i++ ) x.addItem(PlanImage.TRANSFERTFCT[i]);
      if( !aladin.OUTREACH ) {
         panel = new JPanel(new GridLayout(2,2,4,4));
         panel.add(new JLabel("- "+CMV,JLabel.LEFT)); panel.add(videoChoice);
         panel.add(new JLabel("  - "+CMM,JLabel.LEFT)); panel.add(mapChoice);
         panel.add(new JLabel("- "+CMC,JLabel.LEFT)); panel.add(cutChoice);
         panel.add(new JLabel("  - "+CMF,JLabel.LEFT)); panel.add(fctChoice);
         (l = new JLabel(CMB)).setFont(l.getFont().deriveFont(Font.BOLD));
         PropPanel.addCouple(this, p, l, CMH, panel, g, c, GridBagConstraints.EAST);
      }

//      csvChoice = new JComboBox();
//      for( int i=0; i<CSVITEM.length; i++ ) csvChoice.addItem(CSVITEMLONG[i]);
//      (l = new JLabel(CSVCHAR)).setFont(l.getFont().deriveFont(Font.BOLD));
//      if( !aladin.OUTREACH ) {
//         Properties.addCouple(this, p, l, CSVCHARH, csvChoice, g, c, GridBagConstraints.EAST);
//      }

      // Le filtre par défaut
      filterChoice = new JComboBox();
      filterChoice.addItem(NOTACTIVATED);      
      filterChoice.addItem(ACTIVATED);
      if( !aladin.OUTREACH ) {
         (l = new JLabel(FILTERB)).setFont(l.getFont().deriveFont(Font.BOLD));
         PropPanel.addCouple(this, p, l, FILTERH, filterChoice, g, c, GridBagConstraints.EAST);
      }

      // Transparence des footprints
      transparencyChoice = new JComboBox();
      transparencyChoice.addItem(NOTACTIVATED);
      transparencyChoice.addItem(ACTIVATED);
      JPanel transparencyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      // TODO : créer une classe TransparencySlider (utilisé à plusieurs endroits)
      transparencyPanel.add(transparencyChoice);
      transparencyLevel = new JSlider(0, 100);
      transparencyLevel.setValue((int)(100*Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL));
      transparencyLevel.setMajorTickSpacing(20);
      transparencyLevel.setPaintLabels(true);
      transparencyLevel.setPaintTicks(true);
      transparencyLevel.setPaintTrack(true);
      transparencyLevel.setPreferredSize(new Dimension(230,transparencyLevel.getPreferredSize().height));
      transparencyLevel.setToolTipText(OPALEVEL+" : "+transparencyLevel.getValue());
      transparencyLevel.addChangeListener(this);
      transparencyPanel.add(transparencyLevel);

      if( !aladin.OUTREACH ) {
         (l = new JLabel(TRANSB)).setFont(l.getFont().deriveFont(Font.BOLD));
         PropPanel.addCouple(this, p, l, TRANSH, transparencyPanel, g, c, GridBagConstraints.EAST);

//         // Les centrage des tags
//         (l = new JLabel(TAGCENTER)).setFont(l.getFont().deriveFont(Font.BOLD));
//         tagChoice = new JComboBox();
//         tagChoice.addItem(ACTIVATED);
//         tagChoice.addItem(NOTACTIVATED);
//         Properties.addCouple(this, p, l, TAGCENTERH, tagChoice, g, c, GridBagConstraints.EAST);
      }
      
      // Le Web Browser
      if( isUnixStandalone() && !aladin.OUTREACH ) {
         PropPanel.addFilet(p, g, c);
         browser = new JTextField(30);
         (l = new JLabel(WEBB)).setFont(l.getFont().deriveFont(Font.BOLD));
         PropPanel.addCouple(this, p, l, WEBH, browser, g, c, GridBagConstraints.EAST);
      }
      
      // Le survey par défaut
      serverTxt = new JTextField(10);
      surveyTxt = new JTextField(10);
      if( !aladin.OUTREACH ) {
         JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
         p1.add(new JLabel(IMGS,JLabel.LEFT)); p1.add(serverTxt);
         p1.add(new JLabel(IMGC,JLabel.LEFT)); p1.add(surveyTxt);
         (l = new JLabel(IMGB)).setFont(l.getFont().deriveFont(Font.BOLD));
         PropPanel.addCouple(this, p, l, IMGH, p1, g, c,GridBagConstraints.EAST);
      }

      // Le GLU
      (l = new JLabel(REGB)).setFont(l.getFont().deriveFont(Font.BOLD));
      reload = b = new JButton(RELOAD);
      createGluChoice();
      
      if( !aladin.OUTREACH ) {

         // Le glu
         panel = new JPanel(new BorderLayout(5,5));
         panel.add(gluChoice,BorderLayout.WEST);
         b.setMargin( new Insets(2,4,2,4));
         b.addActionListener(this);
         panel.add(b,BorderLayout.EAST);
         PropPanel.addCouple(this, p, l, REGH, panel, g, c, GridBagConstraints.EAST);

         // Les logs
         if( Aladin.LOG ) {
            (l = new JLabel(LOGS)).setFont(l.getFont().deriveFont(Font.BOLD));
            logChoice = new JComboBox();
            logChoice.addItem(ACTIVATED);
            logChoice.addItem(NOTACTIVATED);
            PropPanel.addCouple(this, p, l, LOGH, logChoice, g, c, GridBagConstraints.EAST);
         }
         
         // Le cache
         (l = new JLabel(CACHES)).setFont(l.getFont().deriveFont(Font.BOLD));
         panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
         panel.add( cache=new JLabel("???? / "));
         panel.add( maxCache = new JTextField(6));
         panel.add( new JLabel("MB"));
         b=new JButton(CLEARCACHE); b.addActionListener(this);
         b.setMargin( new Insets(2,4,2,4));
         panel.add( b );
         PropPanel.addCouple(this, p, l, CACHEH, panel, g, c, GridBagConstraints.EAST);
      }

      return p;
   }

   // Nettoyage du cache HPX et du cache GLU
   private void clearCache() {
      aladin.makeCursor(this,Aladin.WAITCURSOR);
      aladin.cache.clear();
      PlanBG.clearCache();
      set(CACHE,"0");
      cache.setText("0 / ");
      aladin.makeCursor(this,Aladin.DEFAULT);
   }
   
   /** Positionnement des valeurs courantes */
   private void updateWidgets() {
      String s;
      
      // Peut être n'ai-je pas encore créé le panneau de la config. ?
      if( dir==null ) return;
      
      s = get(DIR); 
      if( s == null ) s = "";
      dir.setText(s);
      
      s = get(LANG);
      if( s == null ) langChoice.setSelectedIndex(0);
      else langChoice.setSelectedItem(s);      
      langItem = langChoice.getSelectedIndex();
      
//      if( tagChoice!=null ) {
//         s = get(TAG);
//         if( s==null ) tagChoice.setSelectedIndex(0);
//         else tagChoice.setSelectedItem(s);
//      }

      s = get(MOD);
      if( s == null ) modeChoice.setSelectedIndex(0);
      else modeChoice.setSelectedItem(s);      
      modeItem = modeChoice.getSelectedIndex();

//      s = get(PIXEL);
//      if( s == null || s.charAt(0)!='8' ) pixelChoice.setSelectedIndex(0);
//      else pixelChoice.setSelectedIndex(1);   
      
      s = get(FRAME);
      if( s == null ) frameChoice.setSelectedItem("ICRS");
      else frameChoice.setSelectedItem(s);   
      
      s = get(FRAMEALLSKY);
      if( s == null ) frameAllskyChoice.setSelectedItem("GAL");
      else frameAllskyChoice.setSelectedItem(s);   
      
      fctChoice.setSelectedIndex(PlanImage.LINEAR);
      s = aladin.CMDEFAULT!=null ? aladin.CMDEFAULT : get(CM);
      JComboBox c;
      if( s!=null ) {
         Tok tok = new Tok(s);
         while( tok.hasMoreTokens() ) {
            s = tok.nextToken();
            suite: for( int i=0; i<4; i++) {
               c = i==0 ? videoChoice : i==1 ? mapChoice : i==2 ? cutChoice : fctChoice;
               for( int j=0; j<c.getItemCount(); j++ ) {
                  if( ((String)c.getItemAt(j)).equalsIgnoreCase(s) ) { c.setSelectedIndex(j); break suite; }
               }
            }
         }
      }

//      fctBkgChoice.setSelectedIndex(2);  // LINEAR par défaut
//      s = aladin.BKGDEFAULT!=null ? aladin.BKGDEFAULT : get(BKG);
//      if( s!=null ) {
//         Tok tok = new Tok(s);
//         while( tok.hasMoreTokens() ) {
//            s = tok.nextToken();
//            suite: for( int i=0; i<3; i++) {
//               c = i==0 ? videoBkgChoice : i==1 ? mapBkgChoice : fctBkgChoice;
//               for( int j=0; j<c.getItemCount(); j++ ) {
//                  if( ((String)c.getItemAt(j)).equalsIgnoreCase(s) ) { c.setSelectedIndex(j); break suite; }
//               }
//            }
//         }
//      }

//      s = get(CSV);
//      csvItem=0;
//      if( s == null ) csvChoice.setSelectedIndex(0);
//      else {
//         for( int i=0; i<CSVITEM.length; i++ ) {
//            if( CSVITEM[i].equals(s) ) csvChoice.setSelectedIndex(csvItem=i);      
//         }
//      }
      
      s = get(FILTER);
      if( s != null && s.charAt(0)=='N' ) filterChoice.setSelectedIndex(0);
      else filterChoice.setSelectedIndex(1);     

      s = get(TRANS);
      if( s != null && s.charAt(0)=='N' ) {
          transparencyChoice.setSelectedIndex(0);
          transparencyLevel.setEnabled(false);
      }
      else {
          transparencyChoice.setSelectedIndex(1);
          transparencyLevel.setEnabled(true);
      }
      transparencyChoice.addItemListener(this);
      
      s = get(TRANSLEVEL);
      if( s==null ) transparencyLevel.setValue(15);
      
      if( isUnixStandalone() && !Aladin.OUTREACH ) {
         s = get(BROWSER);
         if( s == null ) s = "";
         browser.setText(s);
      }
      
      s = get(SERVER);
      serverTxt.setText( s==null ? "allsky" : s );
      s = get(SURVEY);
      surveyTxt.setText( s==null ? "" : s);
      
      String defaultGlu = get(GLU);
      if( defaultGlu == null ) defaultGlu = Glu.NPHGLUALADIN;
      setSelectGluChoice(defaultGlu);
      
      reload.setEnabled( true );
      
      if( logChoice!=null) logChoice.setSelectedIndex(isLog()?0:1);
      
      if( helpChoice!=null) helpChoice.setSelectedIndex(isHelp()?0:1);

      if( cache!=null ) {
         long cacheSize = PlanBG.cacheSize;
         try {
            if( cacheSize==-1 ) cacheSize = Long.parseLong(get(CACHE));
         } catch( NumberFormatException e ) { }
         if( cacheSize==-1 ) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  long cacheSize = PlanBG.getCacheSize(new File(PlanBG.getCacheDirPath()), null);
                  PlanBG.setCacheSize(cacheSize);
                  cache.setText((cacheSize/1024)+" / ");
               }
            });
         } else cache.setText((cacheSize/1024)+" / ");
         int mCache = (int)(PlanBG.MAXCACHE/1024);
         maxCache.setText(mCache+"");
      }
   }

   /**
    * Récupération de la valeur associée à une clé
    * @param key la clé de la propriété
    * @return la valeur associée à la clé ou null si clé inconnue
    */
   protected String get(String key) {
      ConfigurationItem item = getItem(key);
      if( item == null ) return null;
      return item.value;
   }

   /**
    * Suppression d'une propriété
    * @param key la clé de la propriété à supprimer
    */
   protected void remove(String key) {
      ConfigurationItem item = getItem(key);
      if( item != null ) {
         prop.removeElement(item);
         flagModif = true;
      }
   }

   /**
    * Ajout d'une propriété, ou modification d'une propriété pré-existante
    * @param key la clé associée à la propriété (ne peut contenir de blancs)
    * @param value la (nouvelle) valeur associée à la clé
    */
   protected void set(String key, String value) {
      key.replace(' ', '_');
      flagModif = true;
      ConfigurationItem item = getItem(key);
      if( item == null ) {
         item = new ConfigurationItem(key, value);
         prop.addElement(item);
      } else item.value = value;
   }

   /**
    * Retourne la propriété associée à une clé
    * @param key la clé de la propriété recherchée
    * @return la propriété associée ou null si inconnue
    */
   private ConfigurationItem getItem(String key) {
      Enumeration e = prop.elements();
      while( e.hasMoreElements() ) {
         ConfigurationItem item = (ConfigurationItem) e.nextElement();
         if( item.key.equals(key) ) return item;
      }
      return null;
   }
   
   private int oFrame=Localisation.ICRS;

   /**
    * Sauvegarde des propriétés dans un fichier de Configuration (si nécessaire)
    */
   protected void save() throws Exception {
      if( Aladin.NOGUI ) return;
      
      // On conserve l'état du réticule (large ou normal)
      if( aladin.calque.reticleMode==2 && get(RETICLE)==null ) set(RETICLE,"Large");
      if( aladin.calque.reticleMode!=2 && get(RETICLE)!=null ) remove(RETICLE);
      
      // On conserve l'état du tooltip
      if( aladin.calque.flagTip && get(TOOLTIP)==null ) set(TOOLTIP,"On");
      if( !aladin.calque.flagTip && get(TOOLTIP)!=null ) remove(TOOLTIP);
      
      // On conserve l'état du frame
//      int frame=aladin.localisation.getFrame();
//      if( getFrame()!=frame ) set(FRAME,Localisation.REPERE[frame]);
      
      // On conserve l'état de l'autoscroll
      if( aladin.AUTOSCROLL && get(SCROLL)==null ) set(SCROLL,"On");
      if( !aladin.AUTOSCROLL && get(SCROLL)!=null ) remove(SCROLL);
      
      // On conserve l'état de la fenêtre des mesures
      if( aladin.mesure.isReduced() && get(MESURE)==null ) remove(MESURE);
      if( !aladin.mesure.isReduced() && get(MESURE)!=null ) set(MESURE,"on");
      
      // On conserve la taille de la fenêtre des mesures
      int hd = aladin.splitH.getMesureHeight();
      set(MHEIGHT,""+hd);
      
      // On mémorise les bookmarks si nécessaire
      if( !Aladin.OUTREACH && aladin.bookmarks.canBeSaved() ) {
         String list = aladin.bookmarks.getBookmarkList();
         if( aladin.bookmarks.isDefaultList() ) remove(BOOKMARKS);
         else if( get(BOOKMARKS)==null || !get(BOOKMARKS).equals(list) ) set(BOOKMARKS,list);
      }
      
      String ocache = get(CACHE);
      if( PlanBG.cacheSize!=-1L ) {
         String s=PlanBG.cacheSize+"";
         if( ocache==null || !s.equals(ocache) ) set(CACHE,s);
      }
      
      ocache = get(MAXCACHE);
      String s1 = PlanBG.MAXCACHE+"";
      if( ocache==null || !s1.equals(ocache) ) set(MAXCACHE,s1);
      
      String s = get(LOG);
      if( s!=null && s.equals(ACTIVATED) ) remove(LOG);
      
      s = get(HELP);
      if( s!=null && s.equals(ACTIVATED) ) remove(HELP);
      
      // On conserve l'état du pointeur Simbad
      if( !Aladin.OUTREACH ) {
         if( aladin.calque.flagSimbad
               && (get(SMB)==null || !get(SMB).equals("On")) ) set(SMB,"On");
         if( !aladin.calque.flagSimbad && get(SMB)!=null ) remove(SMB);
      }
      
      // On conserve la position de la fenêtre
      if( !flagModif && sameWinParam() ) return;
      
      // Existe-il déjà un répertoire générique .aladin sinon je le crée ?
      String configDir = System.getProperty("user.home") + Util.FS + aladin.CACHE;
      File f = new File(configDir);
      if( !f.isDirectory() ) if( !f.mkdir() ) throw new Exception(
            "Cannot create " + aladin.CACHE + " directory");

      // Je vais (re)créer le fichier de configuration
      String configName = configDir + Util.FS + CONFIGNAME;
      f = new File(configName);
      f.delete();
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));

      // Détermination de la dernière position/taille de la fenêtre Aladin
      // Si la fenêtre est maximisée, la largeur et la hauteur seront négatives et on réduira
      // tout d'abord la fenêtre pour connaître sa taille réduite
      if( !aladin.isApplet() ) {
         boolean max = (aladin.f.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0 ;
         aladin.f.setExtendedState(Frame.NORMAL);
         Point p = aladin.f.getLocation();
         Dimension d = aladin.f.getSize();
         
         // Obligatoire pour les Macs
         if( p.x<0 ) p.x=0;
         if( p.y<0 ) p.y=0;
         
         // Test pour éviter les valeurs incongrues
         if( Math.abs(p.x)>aladin.SCREENSIZE.width 
               || Math.abs(p.y)>aladin.SCREENSIZE.height
               || d.width<100 || d.width>aladin.SCREENSIZE.width*1.5
               || d.height<100 || d.height>aladin.SCREENSIZE.height*1.5
               ) remove(WINLOC);
         else set(WINLOC,p.x+" "+p.y+" "+(max?-d.width:d.width)+" "+(max?-d.height:d.height));
      }

      // Je sauvegarde les propriétés de la configuration
      boolean first = true;
      Enumeration e = prop.elements();
      while( e.hasMoreElements() ) {
         ConfigurationItem item = (ConfigurationItem) e.nextElement();

         // Entête si nécessaire
         if( first && !(item.key.equals("#") && item.value.startsWith("#Aladin")) ) {
            bw.write("#Aladin user configuration file");
            bw.newLine();
            bw.newLine();
         }
         first = false;

         if( item.key.equals("#") ) bw.write(item.value); // Commentaires
         else if( item.key.trim().length() > 0 ) bw.write(Util.align(item.key, 20) + item.value); // Propriétés
         bw.newLine();
      }
      bw.close();
      flagModif = false;
      
      Aladin.trace(3, "Aladin user configuration file saved");
   }
   
   protected void saveLocalFunction() throws Exception {
      if( !aladin.command.functionModif() ) return;
      String configDir = System.getProperty("user.home") + Util.FS + aladin.CACHE;
      File f  = new File(configDir + Util.FS + CONFIGBKM);
      f.delete();
      StringBuffer s = new StringBuffer();
      BufferedWriter bw=null;
      Enumeration e = aladin.command.getLocalFunctions().elements();
      while( e.hasMoreElements() ) {
         Function f1 = (Function)e.nextElement();
         if( !f1.isLocalDefinition() ) continue;
         if( s.length()>0 ) s.append(',');
         s.append(f1.getName());
         if( bw==null ) bw = new BufferedWriter(new FileWriter(f));
         bw.write(f1.toString(Util.CR)+Util.CR);
      }
      if( bw!=null ) bw.close();
      Aladin.trace(3, "Aladin user local functions saved: "+s);
   }
   
   public String getLocalBookmarksFileName() {
      return System.getProperty("user.home") + Util.FS + aladin.CACHE + Util.FS + CONFIGBKM;
   }

   /** Chargement des propriétés depuis un fichier de configuration */
   protected void load() throws Exception {

      // Existe-il déjà un répertoire générique .aladin
      String configDir = System.getProperty("user.home") + Util.FS + aladin.CACHE;
      File f = new File(configDir);
      if( !f.isDirectory() ) return;

      // Je vais tenter de lire le fichier de configuration
      String configName = configDir + Util.FS + CONFIGNAME;
      f = new File(configName);
      if( !f.exists() ) return;
      BufferedReader br = new BufferedReader(new FileReader(f));

      // Je remet à zéro les éventuelles propriétés déjà chargées
      prop = new Vector();

      // Je lis les propriétés de la configuration
      String s;
      int line = 0;
      Aladin.trace(2, "Loading Aladin user configuration file...");
      while( (s = br.readLine()) != null ) {
         line++;
         if( s.trim().length() == 0 ) {
            prop.addElement(new ConfigurationItem(" ", null));
            continue;
         }
         if( s.charAt(0) == '#' ) {
            prop.addElement(new ConfigurationItem("#", s));
            continue;
         }
         char a[] = s.toCharArray();
         int i, j;
         for( i = 0; i < a.length && !Character.isSpace(a[i]); i++ );
         String key = new String(a, 0, i);
         for( j = i; j < a.length && Character.isSpace(a[j]); j++ );
         String value = new String(a, j, a.length - j);
         if( key.equals(TRANSOLD) ) key=TRANS;      // Pour compatiblité
         aladin.trace(4, "Configuration.load() [" + key + "] = [" + value + "]");
         set(key, value);
      }
      br.close();
      
      // Positionnement du CSVCHAR s'il y a lieu
      setCSV(get(CSV));
      
//      // Positionne du CENTEREDTAG s'il y a lieu
//      setCENTEREDTAG(get(TAG));
      
      // Positionnement du MAXCACHE s'il y a lieu
      setMaxCache(get(MAXCACHE));
      
      flagModif = false;
   }
   
   /** On recharge toutes les définitions du glu */
   private void reloadGlu() {
      try { aladin.glu.reload(); } catch(Exception e) { e.printStackTrace(); }
   }
   
   /** Ouverture de la fenêtre de contribution à une nouvelle traduction */
   private void langContrib() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      
      TextField lang = new TextField(15);
      TextField code = new TextField(3);
      Panel panel = new Panel();
      panel.setLayout(new GridLayout(2,2));
      panel.add(new Label("Language name:"));
      panel.add(lang);
      panel.add(new Label("2 letter code:"));
      panel.add(code);
      g.setConstraints(panel,c);
      
      if( !aladin.question(this,"Specify the new language name (in english)\n" +
            "and the corresponding 2 letter abbreviation.\nOr just press \"Ok\" " +
            "for checking/completing the current language translation.",panel) ) return;
      String s = Util.toLower(code.getText())+" "+Util.toUpLower(lang.getText());
      aladin.chaine.testLanguage(s);
      dispose();
   }

   /**
    * Activation des choix de l'utilisateur
    * 
    * @return true si tout est bon, sinon false
    */
   public boolean apply() throws Exception {
      boolean rep = true;
      String s;
      
      // Pour le centrage des tags
//      if( tagChoice!=null ) {
//         s = (String)tagChoice.getSelectedItem();
//         set(TAG,s);
//         setCENTEREDTAG(s);
//      }
      
      // Pourle log
      if( logChoice!=null ) {
         if( logChoice.getSelectedIndex()==1 ) {
            if( get(LOG)==null ) aladin.glu.log("Log", "off");
            set(LOG,(String)logChoice.getSelectedItem());
         }
         else {
            boolean pub = get(LOG)!=null && get(LOG).equals(NOTACTIVATED);
            remove(LOG);
            if( pub ) aladin.glu.log("Log", "on");
         }
      }

      // Pour l'assistant débutant
      if( helpChoice!=null ) {
         if( helpChoice.getSelectedIndex()==1 ) set(HELP,(String)helpChoice.getSelectedItem());
         else remove(HELP);
      }

      // Pour le site Glu
      int index = gluChoice.getSelectedIndex();
      if( index!=-1 && index!=lastGluChoice ) {
         s = (String) gluUrl.elementAt(index);
         String sNew = aladin.glu.setDefaultGluSite(s);
         lastGluChoice=index;
         if( sNew != null ) {
            setSelectGluChoice(sNew);
            rep = false;
         }
         reloadGlu();
      }
      
      // Pour le mode Pixel
//      set(PIXEL,(String)pixelChoice.getSelectedItem());
      
      // Pour les frames par défaut
      set(FRAME,(String)frameChoice.getSelectedItem());
      set(FRAMEALLSKY,(String)frameAllskyChoice.getSelectedItem());
      
      // Pour le choix du mapping pixel
      s = videoChoice.getSelectedItem()+" "
         +mapChoice.getSelectedItem()+" "
         +cutChoice.getSelectedItem()+" "
         +fctChoice.getSelectedItem();
      set(CM,s);
      aladin.CMDEFAULT=null;    // plus de surcharge éventuel via setconf
      
//      // Pour le choix du mapping du background
//      s = videoBkgChoice.getSelectedItem()+" "
//          +mapBkgChoice.getSelectedItem()+" "
//          +fctBkgChoice.getSelectedItem();
//      set(BKG,s);
//      aladin.calque.planBG.setCM();
//      aladin.BKGDEFAULT=null;    // plus de surcharge éventuel via setconf
    
      // Pour le choix du serveur d'images par défaut
      set(SERVER,serverTxt.getText().trim());
      set(SURVEY,surveyTxt.getText().trim());
      
      // Pour le mode Simbad pointer
//      set(SMB,(String)smbChoice.getSelectedItem());

      // Pour le filtre
      set(FILTER,(String)filterChoice.getSelectedItem());

      // Pour la transparence des footprints
      float level = (float)(transparencyLevel.getValue()/100.0);
      if( level>=1 ) level -= 0.000111f;
      else level += 0.000111f; // le '+/-0.000111f' me permet de distinguer les plans footprints ayant encore la valeur par défaut
      
      set(TRANS,(String)transparencyChoice.getSelectedItem());
      set(TRANSLEVEL,level+"");
      setTransparency((String)transparencyChoice.getSelectedItem(), level);
      
      // Pour le langage
      index = langChoice.getSelectedIndex();   
      if( index!=langItem ) {         
         langItem=index;
         if( index>lang.length ) {
            installRemoteLanguage((String)langChoice.getSelectedItem());
         } else {
            setLang(index==0 ? null : (String)langChoice.getSelectedItem());
         }
         Aladin.info(this,aladin.chaine.getString("RESTART"));      
      }
      
      // Pour le mode
      index = modeChoice.getSelectedIndex();   
      if( index!=modeItem ) {
         modeItem=index;
         setMode(index==0 ? null : (String)modeChoice.getSelectedItem());
         Aladin.info(this,aladin.chaine.getString("RESTART"));      
      }
     
      
      // Pour le caractère de CVS
      if( csvChoice!=null ) {
         index = csvChoice.getSelectedIndex();
         if( index!=csvItem ) {
            set(CSV,CSVITEM[index]);
            setCSV(CSVITEM[index]);
         }   
      }

       // Pour le browser
      if( browser != null ) {
         s = browser.getText().trim();
         if( s.length() != 0 ) set(BROWSER, s);
         else remove(BROWSER);
      }

      // Pour le répertoire par défaut
      if( dir != null ) {
         s = dir.getText().trim();
//         setDir( s.length()==0 ? null : s );
         if( s.length()==0 ) remove(DIR);
         else { setDir(s); set(DIR,s); }
      }
      
      // Pour la taille du cache 
      if( maxCache!=null ) {
         try {
            s=  maxCache.getText();
            long maxCacheSize = Long.parseLong(s);
            PlanBG.setMaxCacheSize(maxCacheSize*1024);
         } catch( Exception e ) { }
      }
      
      // Sauvegarde immédiate
      save();
      updateWidgets();

      return rep;
   }
   
   /** Retourne la meilleure version de la langue passée en paramètre
    * Il s'agit du numéro de version le plus proche de celui d'Aladin
    */
   private Langue getBestLangVersion(String s) {
      Langue bestLang=null;
      try {
         double minDiff = Double.MAX_VALUE;
         double v =  Double.parseDouble( Aladin.VERSION.substring(1) );
         
         Enumeration e = remoteLang.elements();
         while( e.hasMoreElements() ) {
            Langue lg = (Langue)e.nextElement();
            if( !lg.isLangue(s) ) continue;
            double diff = Math.abs(v-lg.version);
            if( minDiff>diff ) { minDiff=diff; bestLang=lg; }
//System.out.println((minDiff==diff?"* ":"  ")+lg);            
         }
      } catch( Exception e ) { }
      return bestLang;
   }
   
   /** Supprime les fichiers de langues anciens qui traine dans le répertoire
    * cache en évitant de supprimer les fichiers locaux de langues
    * (Aladin-xxxx-nnn-loc.string[.utf])*/
   private void clearPreviousRemoteLang() {
      try {
         String dir = System.getProperty("user.home")+Util.FS+Aladin.CACHE;
         FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
               return Util.matchMaskIgnoreCase("Aladin*.string*", name);
            }
         };
         File fdir = new File(dir);
         File [] list = fdir.listFiles(filter);
         for( int i=0; i<list.length; i++ ) {
            String name = list[i].getName();
            if( !name.endsWith(".utf") && !name.endsWith(".string") ) continue;
            if( name.endsWith("-perso.string") || name.endsWith("-perso.string.utf") ) continue;
//            System.out.println("Je devrais supprimer "+list[i]);
            list[i].delete();
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }

   
   /**
    * Déchargement d'un fichier de traduction de la langue "s" le plus proche du numéro
    * de version d'Aladin et mémorisation de celui-ci dans le
    * fichier .aladin/Lang/Aladin-xx.string[.utf]
    * @param s langue à récupérer sur le site via le tag glu "Aladin.lang"
    */
   private void installRemoteLanguage(String s)  throws Exception {
      Langue lg = getBestLangVersion(s);
         
      // Lecture complète du flux
      URL u = new URL(Aladin.LANGURL+"&id="+lg.file);
      MyInputStream in = Util.openStream(u);
//      MyInputStream in = new MyInputStream( u.openStream() );
//      in = in.startRead();
      byte buf[] = in.readFully();
      in.close();
      if( buf.length<10 ) throw new Exception("Cannot download translation file ["+u+"]");

      // Nettoyage
      clearPreviousRemoteLang();
      
      // Création/installation du fichier de la nouvelle langue
      installLanguage(s,lg.file,buf);
      
      return;
   }
   
   /** Installation d'une nouvelle langue dans le répertoire cache.
    * @param langue Le nom de la langue en anglais
    * @param filename Le nom du fichier (ex: Aladin-Spanish-5.026b.string)
    * @param buf Le contenu du fichier de traduction (déjà UTF si nécessaire)
    */
   protected void installLanguage(String langue, String filename, byte buf[]) throws Exception {
      String dir = System.getProperty("user.home")+Util.FS+Aladin.CACHE;
      String fullName = dir+Util.FS+filename;
      
      // Ecriture dans un fichier temporaire
      File f = new File(fullName+".tmp");
      RandomAccessFile rf = new RandomAccessFile(f,"rw");
      rf.write(buf);
      rf.close();
      
      // Suppression d'une éventuelle version UTF ou ISO du même fichier
      if( fullName.endsWith(".string") ) {
         File f2 = new File(fullName+".utf");
         f2.delete();
      } else if( fullName.endsWith(".utf") ) {
         File f2 = new File(fullName.substring(0,fullName.length()-4));
         f2.delete();
      }
            
      // Remplacement du fichier temporaire par son nom définitif
      File f1 = new File(fullName);
      f1.delete();
      f.renameTo(f1);
      Aladin.trace(1, "Translation file installed ["+fullName+"]");
      set(LANG,langue);
   }
   
   /** Positionnement du langage, avec vérification d'existence */
   private void setLang(String s) throws Exception {
      if( s==null ) { remove(LANG); return; }
      
      String s1;
      if( (s1=getLanguage(s))==null ) throw new Exception("Language not supported ! ["+s+"]");
      set(LANG,s1);
   }
   
   /** Positionnement du mode */
   private void setMode(String s) throws Exception {
      if( s==null ) { remove(MOD); return; }
      set(MOD,s);
   }
   
   /** Positionnement du répertoire par défaut, avec vérification d'existence */
   private void setDir(String d) throws Exception {
//      if( d==null ) { remove(DIR); return; }
      File f = new File(d);
      if( !f.isDirectory() ) throw new Exception("Not a directory ! ["+d+"]");
//      set(DIR,d);
   }
   
   /** Positionne la chaine décrivrant la dernière version officielle */
   protected void setOfficialVersion(String s) {
      set(VERSION,s);
   }

   /** Propriétés modifiables par la commande script "setconf xxx=valeur"
     * uniquement pour la session en cours
     */
   protected void setconf(String prop,String value) throws Exception  {
      if( prop.equalsIgnoreCase(DIR)
            || prop.equalsIgnoreCase("dir") )     setDir(value);
      else if( prop.equalsIgnoreCase(CSV) )       setCSV(value);
      else if( prop.equalsIgnoreCase("sync") )    setSyncMode(value);
      else if( prop.equalsIgnoreCase(POSITION)
            || prop.equalsIgnoreCase("position")
            || prop.equalsIgnoreCase("frame"))    setPositionMode(value);
//      else if( prop.equalsIgnoreCase(PIXEL)
//            || prop.equalsIgnoreCase("pixel"))    setPixelMode(value);
      else if( prop.equalsIgnoreCase(SMB)
            || prop.equalsIgnoreCase("simbad"))   setSimbadMode(value);
      else if( prop.equalsIgnoreCase(FILTER)
            || prop.equalsIgnoreCase("filter"))   setFilterMode(value);
      else if( prop.equalsIgnoreCase(CM)
            || prop.equalsIgnoreCase("Colormap") 
            || prop.equalsIgnoreCase("cm"))       setCMMode(value);
      else if( prop.equalsIgnoreCase(BKG)
            || prop.equalsIgnoreCase("Background"))setBkgMode(value);
    else throw new Exception("Unknown conf. propertie ["+prop+"]");
   }
   private static final String DEFAULT_FILENAME = "-";
   
   // Gestion des evenements
   public void actionPerformed(ActionEvent evt) {
      Object src = evt.getSource();
      
      String what = src instanceof JButton ? ((JButton)src).getActionCommand() : "";

      if( CLOSE.equals(what) ) dispose();
      else if( LANGCONTRIB.equals(what)) langContrib();
      else if( RELOAD.equals(what) ) reloadGlu();
      else if( APPLY.equals(what) ) {
         try { if( apply() ) { aladin.calque.repaintAll(); /* dispose(); */ } }	   
         catch( Exception e ) { Aladin.warning(this," "+e.getMessage(),1); }
      }
//      else if( GLUTEST.equals(what) ) startGluTest();
//      else if( GLUSTOP.equals(what) ) stopGluTest();
      
      // Affichage du selecteur de répertoires
      else if( CLEARCACHE.equals(what) ) {
         clearCache();
      }
      // Affichage du selecteur de répertoires
      else if( BROWSE.equals(what) ) {

         FileDialog fd = new FileDialog(aladin.dialog);
         aladin.setDefaultDirectory(fd);

         // (thomas) astuce pour permettre la selection d'un repertoire
         // (c'est pas l'ideal, mais je n'ai pas trouve de moyen plus propre en AWT)
         fd.setFile(DEFAULT_FILENAME);

         fd.show();
         aladin.memoDefaultDirectory(fd);
         String directory = fd.getDirectory();
         String name =  fd.getFile();
         // si on n'a pas changé le nom, on a selectionne un repertoire
         boolean isDir = false;
         if( name!=null && name.equals(DEFAULT_FILENAME) ) {
            name = "";
            isDir = true;
         }
         if( (name!=null && name.length()>0) || isDir ) dir.setText(directory);
      }
	}
   
   // implementation d'EventListener (déplacement du niveau de transparence)
   public void stateChanged(ChangeEvent e) {
       float level = (float)(transparencyLevel.getValue()/100.0);
       if( level>=1 ) level -= 0.000111f;
       else level += 0.000111f;
       
       if( Math.abs(level-Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL)>0.02 ) {
           setTransparency((String)transparencyChoice.getSelectedItem(), level);
           set(TRANSLEVEL, level+"");
       }
   }
   
   // implementaion de ChangeListener
   public void itemStateChanged(ItemEvent e) {
       // on ne s'intéresse qu'à la sélection
       if( e.getStateChange()!=ItemEvent.SELECTED ) return;
       transparencyLevel.setEnabled(e.getItem().equals(FILTERY));
   }
   
   
   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) dispose();
      super.processWindowEvent(e);
   }
   
   
   /**
    * Classe permettant la mémorisation d'un propriété, c'est-à-dire un couple
    * (clé,valeur)
    */
   private class ConfigurationItem {
      protected String key;  // Clé associée à la propriété

      protected String value; // valeur associée à la propriété

      private ConfigurationItem(String key, String value) {
         this.key = key;
         this.value = value;
      }
   }
   
   /**
    * Structure pour mémoriser le descriptifs des langues
    */
   class Langue {
      String code;      // Code deux lettres de la langue
      String langue;    // Langue en toute lettre (en anglais)
      double version;   // Numéro de version d'Aladin prévue pour cette langue
      String auteur;    // Auteur de la traduction
      String file;      // Nom du fichier distant lorsqu'il s'agit de la description d'une
                        // langue distance
      
      Langue(String s) {
         langue=s;
         code=auteur="";
         version=0;
         file=null;
      }
      
      /** Retourne true si s correspond au code ou à la langue
       * sans différencier les majuscules et les minuscules
       */
      boolean isLangue(String s) {
         try {
            return code!=null   && s.equalsIgnoreCase(code)
             || langue!=null && s.equalsIgnoreCase(langue);
         } catch( Exception e ) { return false; }
      }
      
      public String toString() {
         return langue+" ("+code+") "+version+" "+auteur+(file!=null?" "+file:"");
      }
   }

}

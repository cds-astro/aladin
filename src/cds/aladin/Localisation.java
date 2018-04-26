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

package cds.aladin;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Coo;
import cds.astro.Ecliptic;
import cds.astro.FK4;
import cds.astro.FK5;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.astro.Supergal;
import cds.tools.Util;

/**
 * Classe gerant l'indication de position de la souris dans la vue
 * Elle permet de choisir le repere. Etroitement associee a la classe
 * LCoord
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (12 dec 00) Gestion du champ de saisie rapide
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 * @see aladin.LCoord()
 */
public class Localisation extends MyBox  {
   // les constantes associees a chaque repere
   static final public int ICRS   = 0;
   static final public int ICRSD  = 1;
   static final public int ECLIPTIC = 2;
   static final public int GAL    = 3;
   static final public int SGAL   = 4;
   static final public int J2000  = 5;
   static final public int J2000D = 6;
   static final public int B1950  = 7;
   static final public int B1950D = 8;
   static final public int B1900  = 9;
   static final public int B1875  = 10;
   static final public int XY     = 11;
   static final public int XYNAT  = 12;
   static final public int XYLINEAR  = 13;
   static final public int PLANET = 14;
   static final public int PLANETD = 15;

   // Le label pour chaque repere (dans l'ordre des constantes ci-dessus)
   static final String [] REPERE = {
      "ICRS","ICRSd","Ecliptic","Gal","SGal",
      "J2000","J2000d","B1950","B1950d","B1900","B1875",
      "XY Fits","XY image","XY linear","Planet","Planet deg"
   };

  // Le label pour chaque frame dans le vocabulaire FoX
   static final String [] FRAMEFOX = {
         "ICRS","ICRS","ECL","GAL","SGAL",
         "J2000","J2000","B1950","B1950","B1900","B1875",
         "","","","",""
      };

   // Le mot clé RADECSYS Fits correspondant au système de coordonnée
   static final String [] RADECSYS = {
      "ICRS","ICRS",null,null,null,
      "FK5","FK5","FK4","FK4","FK4","FK4",
      null,null,null,null,null,
   };

   // Le préfixe du mot clé CTYPE1 Fits correspondant au système de coordonnée
   static final String [] CTYPE1 = {
      "RA---","RA---","ELON-","GLON-","SLON-",
      "RA---","RA---","RA---","RA---","RA---","RA---",
      null,null,"SOLAR",null,null,
   };

   // Le préfixe du mot clé CTYPE2 Fits correspondant au système de coordonnée
   static final String [] CTYPE2 = {
      "DEC--","DEC--","ELAT-","GLAT-","SLAT-",
      "DEC--","DEC--","DEC--","DEC--","DEC--","DEC--",
      null,null,"SOLAR",null,null,
   };

   // Les différents Frames possibles (mode AllSky)
   static final String [] FRAME = { "Default", REPERE[ICRS], REPERE[ECLIPTIC], REPERE[GAL], REPERE[SGAL] };
   static JComboBox createFrameCombo() {return new JComboBox(FRAME); }

   // Les différents Frames possibles (pour la recalibration)
   static final String [] FRAMEBIS = { "Equatorial", "Galactic", "Ecliptic", "SuperGal" };
   static final int [] FRAMEBISVAL = { Calib.FK5, Calib.GALACTIC, Calib.ECLIPTIC, Calib.SUPERGALACTIC };
   static final int [] FRAMEVAL = { ICRS, GAL, ECLIPTIC, SGAL };
   static JComboBox createFrameComboBis() {return new JComboBox(FRAMEBIS); }
   static int getFrameComboBisValue(String s) {
      int i = Util.indexInArrayOf(s, FRAMEBIS, true);
      if( i<0 ) return 0;
      return FRAMEBISVAL[i];
   }
   static int getFrameComboValue(String s) {
      int i = Util.indexInArrayOf(s, FRAMEBIS, true);
      if( i<0 ) return 0;
      return FRAMEVAL[i];
   }

   // Retourne true s'il s'agit du même système de référence (en ignorant la différence degrés et sexa)
   static final boolean isSameFrame(int frame1,int frame2) {
      if( frame1==ICRSD || frame1==J2000D || frame1==B1950D ) frame1--;
      if( frame2==ICRSD || frame2==J2000D || frame2==B1950D ) frame2--;
      return frame1==frame2;
   }

   static final String NOREDUCTION = "No astrometrical reduction";
   static final String NOHPX = "No HEALPix map";
   static final String NOPROJECTION = "No proj => select "+REPERE[XYLINEAR];
   static final String NOXYLINEAR = "No XY linear trans.";
   static protected String POSITION,YOUROBJ;

   private int previousFrame=-1;   // Frame précédent;
   private JPopupMenu popup=null;  // Le popup de l'historique des commandes


   /* Pour gerer les changements de frame */
   Astrocoo afs = new Astrocoo(AF_ICRS);	// Frame ICRS (la reference de base)

   protected Localisation() { super(); }
   
   /** Creation de l'objet de localisation. */
   protected Localisation(Aladin aladin) {
      super(aladin,aladin.chaine.getString("POSITION"));
      String tip = aladin.chaine.getString("TIPCMD"); // "TIPPOS");
      Util.toolTip(pos, tip);
      Util.toolTip(label,tip);
      Util.toolTip(text, aladin.chaine.getString("TIPCMD"));
      Util.toolTip(c,aladin.chaine.getString("TIPPOSCHOICE"));
      //      c.setEnabled(false);

      POSITION = aladin.chaine.getString("POSITION");
      YOUROBJ = aladin.chaine.getString("YOUROBJ");


      text.addKeyListener(new KeyAdapter() {
         public void keyPressed(KeyEvent e) {
            clearIfRequired();
         }
         public void keyReleased(KeyEvent e) {
            clearIfRequired();
            if( e.getKeyCode()==KeyEvent.VK_ENTER ) submit();
         }
      });

      text.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            flagReadyToClear=false;
            setMode(SAISIE);
         }
      });

      pos.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            setMode(SAISIE);
         }
      });

      text.requestFocusInWindow();
   }
   
   boolean first=true;
   int posHist=-1;

   /** Positionnement du texte qui sera affiché en mode de saisie */
   protected void setTextSaisie(String s) {
      first=true;
      super.setTextSaisie(s);
      text.select(0, text.getText().length());
   }

   /** La commande en cours reçoit un nouveau caractère */
   protected void sendKey(KeyEvent e) {
      int key = e.getKeyCode();
      char k = e.getKeyChar();

      if( e.isControlDown() || e.isAltDown() ) return;

      clearIfRequired();

      StringBuffer cmd = new StringBuffer(text.getText());
      if( key==KeyEvent.VK_ENTER ) {
         String s=shortCutLoad(cmd.toString());
         aladin.execAsyncCommand(s);
         first=true;
      } else if( key==KeyEvent.VK_BACK_SPACE || key==KeyEvent.VK_DELETE ) {
         first =false;
         if( cmd.length()>0 ) cmd.deleteCharAt(cmd.length()-1);

      // On remonte/descend dans l'historique des commandes précédentes
      } else if( key==KeyEvent.VK_UP || key==KeyEvent.VK_DOWN || key==KeyEvent.VK_PAGE_DOWN) {
         String s1 = browseHistory( key==KeyEvent.VK_PAGE_DOWN ? 2 : key==KeyEvent.VK_UP ? -1 : 1 );
         if( s1!=null ) cmd = new StringBuffer( s1 );

         // On insere un nouveau caractere
      } else {
         posHist = -1;
         if( first ) { cmd.delete(0, cmd.length()); first=false; }
         if( k>=31 && k<=255 || k=='@') cmd.append(k);
      }
      setMode(SAISIE);
      String s = cmd.toString();
      if( s.startsWith(aladin.GETOBJ) ) s = s.substring(aladin.GETOBJ.length());
      super.setTextSaisie(s);
   }
   
   // Chaine en cours d'édition
   private String lastEditingCmd=null;
   
   /** Permet d'afficher l'historique récent des commandes */
   protected String browseHistory(int sens) {
      boolean flagHead=false;
      if( aladin.console.getIndexArrowHistory()==-1 ) {
         lastEditingCmd = text.getText();
         flagHead=true;
      };
            
      String s = aladin.console.getNextArrowHistory( sens );
      
      // on est au début de la pile, et la commande est encore affichée => on la saute 
      if( s!=null && flagHead && lastEditingCmd.equals(s) ) s=aladin.console.getNextArrowHistory( sens );
      
      if( s==null ) { s=lastEditingCmd; lastEditingCmd=null; }
      if( s==null ) return null;
//      System.out.println("sens="+sens+" flagHead="+flagHead+" ==> "+s);
      first=false;
      return s;
   }

   private boolean flagReadyToClear=false;     // Indique que le champ de saisie est prêt à être effacé (voir testClear())
   private boolean flagStopInfo=false;      // Indique que l'info de démarrage doit s'arrêter immédiatement

   /** Effacement du champ de saisie si on a pas cliqué dans le champ auparavant */
   protected void clearIfRequired() {
      flagStopInfo=true;
      if( !flagReadyToClear ) return;
      flagReadyToClear=false;
      text.setText("");
   }

   /** Spécifie que le champ de saisie s'effacera à la prochaine frappe de clavier,
    * sauf si on a cliqué dans le champ */
   protected void readyToClear() {
      flagReadyToClear=true;
   }

   protected void setMode(int mode ) {
      super.setMode(mode);
      if( mode==SAISIE ) /* text.requestFocusInWindow() */;
      else {
         ViewSimple v=aladin.view.getCurrentView();
         if( v!=null && !v.hasFocus() ) v.requestFocusInWindow();
      }
   }

   /** Fait clignoter le champ pour attirer l'attention
    * de l'utilisateur et demande le focus sur le champ de saisie */
   protected void focus(String s) { focus(s,null); }
   protected void focus(String s,final String initial) {
      setMode(SAISIE);
      text.setText(s);

      (new Thread() {
         Color def = text.getBackground();
         Color deff = text.getForeground();
         public void run() {
            for( int i=0; i<2; i++ ) {
               text.setBackground(Color.green);
               text.setForeground(Color.black);
               Util.pause(1000);
               text.setBackground(def);
               text.setForeground(deff);
               Util.pause(100);
            }
            if( initial==null ) {
               text.setText("");
               text.requestFocusInWindow();
            } else {
               text.setText(initial);
               setInitialFocus();
            }
         }
      }).start();
   }

   protected void setInitialFocus() {
      setMode(SAISIE);
      text.requestFocusInWindow();
      text.setCaretPosition(text.getText().length());
   }
   
   // Pause pouvant être interrompue prématurément
   private void myPause(int delai) {
      long start=System.currentTimeMillis();
      while( !flagStopInfo && System.currentTimeMillis()-start < delai ) {
         Util.pause(20);
      }
   }

   protected void infoStart() {
      if( !aladin.calque.isFree() || text.getText().length()>0  || aladin.dialog==null || aladin.dialog.isVisible() ) return;
      setMode(SAISIE);
      final String s = aladin.GETOBJ;
      text.setText(s);
      text.setFont(text.getFont().deriveFont(Font.ITALIC));
      (new Thread() {
         Color def = text.getBackground();
         Color deff = text.getForeground();
         public void run() {
            flagReadyToClear=true;
            text.setBackground( Aladin.COLOR_TEXT_BACKGROUND );
            for( int i=0; i<3 && aladin.calque.isFree() && !flagStopInfo ; i++ ) {
               if( !flagStopInfo ) {
                  text.setText("");
                  text.setForeground( Color.black );
                  myPause(100);
               }
               if( !flagStopInfo ) {
                  text.setText(s);
                  myPause(1500);
               }
            }
            if( flagStopInfo ) {
               text.setCaretPosition(text.getText().length());
               flagReadyToClear=flagStopInfo=false;
            }
            text.setText("");
            text.setForeground(deff);
            text.setBackground(def);
            text.setFont(text.getFont().deriveFont(Font.BOLD));
            text.requestFocusInWindow();
         }
      }).start();
   }

   protected JComboBox createSimpleChoice() {
//      return new JComboBox(REPERE);
      JComboBox c = new JComboBox();
      int n = Aladin.BETA ? REPERE.length : REPERE.length-2;
      for( int i=0; i<n; i++ ) c.addItem(REPERE[i]);
      return c;
      
   }
   protected JComboBox createChoice() {
      final JComboBox c = super.createChoice();
      c.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            setInternalFrame(c.getSelectedIndex());
         }
      });
      c.setPrototypeDisplayValue(new Integer(100000));
      c.setMaximumRowCount(REPERE.length);
      c.setFont(F);
      int n = Aladin.BETA ? REPERE.length : REPERE.length-2;
      for( int i=0; i<n; i++ ) c.addItem(REPERE[i]);
      //      else for( int i=0; i<REPERE.length-1; i++ ) c.addItem(REPERE[i]);
      c.setSelectedIndex(ICRS);
      previousFrame=ICRS;
      return c;
   }

   /**
    * Positionnement du système de coordonnées
    * @param s une valeur possible dans le menu déroulant des coord. (REPERE[])
    * @return true si ok, false sinon
    */
   protected boolean setPositionMode(String s) {
      if( s.equalsIgnoreCase("xy") ) s = REPERE[XY];  // juste pour tolérer xy et XY
      for( int i=0; i<REPERE.length; i++ ) {
         if( !REPERE[i].equalsIgnoreCase(s) ) continue;
         setFrame(i);
         actionChoice();
         return true;
      }
      return false;
   }

   private int frame = 0;

   private void setInternalFrame(int frame) { this.frame=frame; }

   /** Positionne le frame */
   protected void setFrame(int frame) {
      this.frame=frame;
      setChoiceIndex(frame);
   }
   
   /** Retourne le nom du frame courant sous la forme d'un code compatible avec la librairie Fox */
   protected String getFrameFox() { return FRAMEFOX[frame]; }
   
   /** Retourne true si le mot est un frame à la FOX */
   static protected boolean isFrameFox(String s) {
      return s!=null && s.length()>0 && Util.indexInArrayOf(s, FRAMEFOX, true)>=0;
   }
   
   /** Retourne le nom du frame passé en paramètre */
   protected String getFrameName() { return getFrameName(frame); }
   static public String getFrameName(int frame) { return frame<0 ? "" : REPERE[frame]; }

   /** Retourne la position du menu deroulant */
   protected int getFrame() { return frame; }
   
   /** Retourne true si la coordonnée est suffixée par le frame à la Fox (ex: 134 +89 ICRS) */
   static protected boolean hasFoxSuffix(String s) {
      int i = s.lastIndexOf(' ');
      if( i>0 ) {
         String w = s.substring(i+1);
         if( Localisation.isFrameFox(w) ) return true;
      }
      return false;
   }
   
   /** Retourne vrai si la chaine n'est pas une coordonnées */
   static protected boolean notCoord(String s) {
      
      // La coordonnée peut être suffixé par un nom de frame (ICRS, GAL ...)
      boolean flagFox=false;
      int i = s.lastIndexOf(' ');
      if( i>0 ) {
         String w = s.substring(i+1);
         if( Localisation.isFrameFox(w) ) {
            s = s.substring(0,i).trim();
            flagFox=true;
         }
      }
      
      // La coordonnée peut être suffixé par une lettre (N,S,E ou W) indiquant la direction
      char a[] = s.toCharArray();
      int n = a.length;
      if( !flagFox && n>1 ) {
         char c = a[ n-1 ];
         if( c=='N' || c=='S' || c=='E' || c=='W' ) n--;
      }
      
      for( i=0; i<n; i++ ) {
         if( a[i]>='a' && a[i]<='z' ||  a[i]>='A' && a[i]<='Z' ) return true;
      }
      return false;
   }
   
   
//   static final public int ICRS   = 0;
//   static final public int ICRSD  = 1;
//   static final public int ECLIPTIC = 2;
//   static final public int GAL    = 3;
//   static final public int SGAL   = 4;
//   static final public int J2000  = 5;
//   static final public int J2000D = 6;
//   static final public int B1950  = 7;
//   static final public int B1950D = 8;
//   static final public int B1900  = 9;
//   static final public int B1875  = 10;
//   static final public int XY     = 11;
//   static final public int XYNAT  = 12;
//   static final public int XYLINEAR  = 13;

   
   /** Retourne le frame "générique" courant (ICRS | GAL | SGAL | ECLIPTIC | -1) */
   protected int getFrameGeneric() {
      return frame==GAL ? GAL :
             frame==ECLIPTIC ? ECLIPTIC :
             frame==SGAL ? SGAL :
             (frame!=XY || frame!=XYNAT || frame!=XYLINEAR || frame!=PLANET) ? ICRS : -1;
   }
   
   
   /** Insère le résultat d'une résolution Sésame dans le champ de commande avec le label
    * POSITION histoire que cela se comprenne */
   protected void setSesameResult(String s) {
      setTextSaisie(s);
      readyToClear();
   }
   
   /** Affiche la position, en fonction du frame defini
    * dans le menu deroulant
    * @param x,y Les coordonnees de la souris dans la View
    */
   private Coord coo = new Coord();
   protected void setPos(ViewSimple v,double x,double y) { setPos(v,x,y,0); }
   protected void setPos(ViewSimple v,double x,double y,int methode) { setPos(v,x,y,methode,false); }
   protected void setPos(ViewSimple v,double x,double y,int methode,boolean sendPlasticMsg) {
      int frame     = getFrame();

      // Forcage pour les nuage de point
      ViewSimple view = aladin.view.getMouseView();
      if( view!=null && view.isPlot() ) frame=XYLINEAR;

      Plan plan = v.pref;
      if( plan==null ) return;
      Projection proj = v.getProj();

      PointD p   = v.getPosition(x,y);
      String s=null;

      // Position (X,Y) simplement (mode FITS)
      if( frame==XY || proj!=null && proj.modeCalib==Projection.NO ) {
         if( plan.isImage() )  s=Util.myRound(""+(p.x+0.5),4)
               +"  "+Util.myRound(""+(((PlanImage)plan).naxis2-p.y+0.5),4);
         else s="";

         // Position (X,Y) simplement (mode Natif)
      } else if( frame==XYNAT || proj!=null && proj.modeCalib==Projection.NO ) {
         if( plan.isImage() )  s=Util.myRound(""+p.x,0)
               +"  "+Util.myRound(""+p.y,0);
         else s="";

         // Calcul de la projection
      } else {
         if( !Projection.isOk(proj) ) s=NOREDUCTION;
         else {
            coo.x = p.x;
            coo.y = p.y;
            proj.getCoord(coo);

            if( Double.isNaN(coo.al) ) s="";
            else if( frame==PLANET ) {
               s = coo.getSexaPlanet();
            } else if( frame==PLANETD ) {
               s = coo.getDegPlanet();
            } else if( frame==XYLINEAR ) {
               if( !proj.isXYLinear() ) s=NOXYLINEAR;
               else s=Util.myRound(coo.al+"",4)+" : "+Util.myRound(coo.del+"",4);
            } else {
               if( proj.isXYLinear() ) s=NOPROJECTION;
               else {

                  // Gestion de la précision en fonction du champ
                  double r = v.getTailleRA();
                  int precision = r==0.0 ? Astrocoo.ARCMIN :
                     r> 0.001 ? Astrocoo.ARCSEC+1 :
                        r > 0.00001 ?Astrocoo.MAS-1 :
                           Astrocoo.MAS+1;

                        s=J2000ToString(coo.al,coo.del,precision,false);
                        if( Aladin.PLASTIC_SUPPORT && sendPlasticMsg ) {
                           aladin.getMessagingMgr().pointAtCoords(coo.al, coo.del);
                        }
               }
            }
         }
      }

      lastPosition = s==NOREDUCTION ? "" : s;

      //Affichage du resultat
      if( methode==1 ) {
         setTextSaisie(s);
         setMode(SAISIE);
      } else {
         setTextAffichage(s);
         setMode(AFFICHAGE);
      }
   }

   private String lastPosition="";
   protected String getLastPosition() { return lastPosition; }
   protected Coord getLastCoord() { return coo; }
   protected Coord getLastCoordInCurrentFrame() { return ICRSToFrame(coo); }
   protected void setLastCoord(double ra,double dec) { coo=new Coord(ra,dec); }

   static final Astroframe AF_FK4 = new FK4();
   static final Astroframe AF_FK5 = new FK5();
   static final Astroframe AF_GAL = new Galactic();
   static final Astroframe AF_SGAL = new Supergal();
   static final Astroframe AF_ICRS = new ICRS();
   static final Astroframe AF_ECLI = new Ecliptic();
   static final Astroframe AF_FK4_1900 = new FK4(1900);
   static final Astroframe AF_FK4_1875 = new FK4(1875);

   // Retourne la valeur du frame prevue dans Astroframe
   // en fonction de la valeur courante du menu deroulant
   static protected Astroframe getAstroframe(int i) {
      return (i==ICRS  || i==ICRSD )?AF_ICRS:
         (i==GAL)?AF_GAL:
            (i==J2000 || i==J2000D)?AF_FK5:
               (i==B1950 || i==B1950D)?AF_FK4:
                  (i==B1900)?AF_FK4_1900:
                     (i==B1875)?AF_FK4_1875:
                        (i==ECLIPTIC)?AF_ECLI:
                           (i==SGAL)?AF_SGAL:AF_ICRS;
   }


//   public static Coord frameToFrame(Coord c, int frameSrc, int frameDst) {
//      if( frameSrc==frameDst )  return c;
//      Astrocoo coo = new Astrocoo( getAstroframe(frameSrc), c.al, c.del );
//      coo.setPrecision(Astrocoo.MAS+3);
//
//      coo.convertTo( getAstroframe(frameDst) );
//
//      c.al = coo.getLon();
//      c.del= coo.getLat();
//      return c;
//   }


   public static Coord frameToFrame(Coord c, int frameSrc,int frameDst) {
      if( frameSrc==frameDst ) return c;
      Coo cTmp = new Coo(c.al,c.del);
      if( frameSrc!=ICRS && frameSrc!=ICRSD ) getAstroframe(frameSrc).toICRS(cTmp);
      if( frameDst!=ICRS && frameDst!=ICRSD ) getAstroframe(frameDst).fromICRS(cTmp);
      c.al = cTmp.getLon();
      c.del= cTmp.getLat();
      return c;
   }
   
//   public static void main( String []s ) {
//      Coord c = new Coord();
//      c.al  = 189.9976249999999;
//      c.del = -11.62305555555556;
//      System.out.println("ICRS => "+c);
//      c = frameToFrame(c,Localisation.ICRS, Localisation.GAL);
//      System.out.println("GAL => "+c);
//      c = frameToFrame(c,Localisation.GAL, Localisation.ICRS);
//      System.out.println("ICRS => "+c);
//   }

   protected Coord ICRSToFrame(Coord c) {
      if( frame==ICRS || frame==ICRSD ) return c;
      return frameToFrame(c,ICRS,frame);
   }

   protected Coord frameToICRS(Coord c) {
      if( frame==ICRS || frame==ICRSD ) return c;
      return frameToFrame(c,frame,ICRS);
   }

   /** Mise en forme des coordonnees en ICRS sexa */
   protected String getICRSCoord(String coo) {
      if( coo.length()==0 ) return coo;
      return convert(coo, frame, ICRS );
   }

   /** Mise en forme des coordonnees dans le frame courant */
   protected String getFrameCoord(String coo) {
      return convert(coo, ICRS, frame);
   }
   
   /** Conversion et/ou mise en forme de coordonnées
    * @param coo coordonnées ou identificateur
    * @param frameSource numéro du système de référence source : ICRS, ICRSd...
    * @param frameTarget numéro du système de référence cible
    * @return les coordonnées éditées dans le système cible, ou l'identificateur inchangé
    * Rq: si coordonnées planéto pas de modif.
    */
   static protected String convert(String coo,int frameSource,int frameTarget) {

      String coo1=coo;
      boolean flagFox = false;
      boolean flagPlaneto = false;
      
      // Champ vide => Rien à faire
      if( coo==null || coo.length()==0 || coo.indexOf("--")>=0 ) return "";
      
      // Y a-t-il un frame spécifique indiqué en suffixe => override le frame courant
      int i = coo.lastIndexOf(' ');
      if( i>=0 ) {
         String s = coo.substring(i+1);
         int f = Util.indexInArrayOf(s, FRAMEFOX, true);
         if( f>=0 ) {
            frameSource=f;
            coo = coo.substring(0,i).trim();
            flagFox=true;
//            System.out.println("Bingo: "+s+" => "+coo);
         }
      }
      
      // Y a-t-il une indication de directino NSEW en suffixe ?
      char a [] = coo.toCharArray();
      int n = a.length;
      if( !flagFox && n>1 ) {
         char c = a[ n-1 ];
         if( c=='N' || c=='S' || c=='E' || c=='W' ) {
            n--;
            flagPlaneto=true;
         }
      }
      
      // Identificateur à la place d'une coordonnée => Rien à faire
      for( i=0; i<n; i++) {
         char c = a[i];
         if( (c>='A' && c<='Z') || (c>='a' && c<='z') ) return coo1;
         if( c==',' ) flagPlaneto=true;
      }
      
      // Coordonnées planéto ? => rien à faire
      if( flagPlaneto ) return coo1;

      // Edition et conversion si nécessaire
      try {
         
         Astrocoo aft = new Astrocoo( getAstroframe(frameSource) );
         aft.set(coo);
         aft.setPrecision(Astrocoo.MAS+3);
         if( frameSource!=frameTarget ) aft.convertTo( getAstroframe(frameTarget) );

         String s = (frameTarget==J2000D || frameTarget==B1950D || frameTarget==ICRSD
               || frameTarget==ECLIPTIC || frameTarget==GAL || frameTarget==SGAL )?
                     aft.toString("2d"):aft.toString("2s");

//        if( frameSource!=frameTarget ) {
//           System.out.println("convert ["+coo+"]/"+Localisation.REPERE[frameSource]+"  => ["+s+"]/"+Localisation.REPERE[frameTarget]);
//        }

        if( s.indexOf("--")>=0 ) return "";
        return s;
        
      } catch( Exception e ) { return coo; }
   }

   /** Retourne la position d'un objet en fonction du frame
    * courant
    * @param al,del : coordonnees (ICRS)
    * @return La chaine decrivant la position
    */
   protected String J2000ToString(double al,double del) { return J2000ToString(al,del,Astrocoo.ARCSEC+1,false); }
   protected String J2000ToString(double al,double del,int precision,boolean withFox) {
      Coord cTmp = new Coord(al,del);
      cTmp = ICRSToFrame(cTmp);
      afs.setPrecision(precision);
      String rep = frameToString(cTmp.al,cTmp.del,precision);
      if( withFox ) rep = rep+" "+getFrameFox();
      return rep;
   }
   
   /** Retourne la position d'un objet en fonction du frame
    * courant et ajoute ce frame en suffixe
    * @param al,del : coordonnees (ICRS)
    * @return La chaine decrivant la position
    */
   protected String foxString(double al, double del) { return J2000ToString(al,del,Astrocoo.ARCSEC+1,true); }

   protected String frameToString(double al,double del) { return frameToString(al,del,Astrocoo.ARCSEC+1); }
   protected String frameToString(double al,double del,int precision) {
      int i = getFrame();
      
      afs.set(al,del);
      afs.setPrecision(precision);
      try {
         return (i==J2000D || i==B1950D || i==ICRSD
               || i==ECLIPTIC || i==GAL || i==SGAL )?
                     afs.toString("2d"):afs.toString("2:");
      } catch( Exception e) { System.err.println(e); }
      return "";
   }
   
   /** Retourne le label pour la grille de coordonnées en fonction du frame courant,
    * @param al
    * @param del
    * @param indice 0-premier élément de la coord, 1-deuxième élément
    * @return
    */
   protected String getGridLabel(double al, double del, int indice) {
      int i=getFrame();
      String s;
      int offset;
      if( i==PLANET || i==PLANETD) {
         if( i==PLANETD ) s = (new Coord(al,del)).getDegPlanet();
         else s = (new Coord(al,del)).getSexaPlanet();
         if( s.length()==0 ) return "";
         offset = s.indexOf(',');
         return zeroSec( indice==1 ? s.substring(0,offset) : s.substring(offset+2) );
         
      } else {
         s = frameToString(al,del);
         if( s.length()==0 ) return "";
         offset = s.indexOf(' ');
         return zeroSec( indice==0 ? s.substring(0,offset) : s.substring(offset+1) );
      }
   }
   
   /** Gère le cas d'un suffixe N,S,E,W éventuel à la fin de la coordonnée */
   private String zeroSec(String s) {
      int n=s.length();
      if( n==0 ) return s;
      char c = s.charAt(n-1);
      if( c!='N' && c!='S' && c!='E' && c!='W' ) return zeroSec1(s);
      return zeroSec1( s.substring(0,n-2) ) + s.substring(n-2);
   }
   
   /** Tronque les centièmes de secondes, voire les secondes afin que les labels
    * de la grille ne soient pas trop longs */
   private String zeroSec1(String s) {
      char a[]=s.toCharArray();
      int flagDecimal=0;
      int flagZero=0;

      for( int i=a.length-1; i>0 && (a[i]<'1' || a[i]>'9'); i--) {
         if( a[i]=='.') flagDecimal=i;
         else if( a[i]=='0' ) flagZero=i;
         else if( a[i]==':') {
            s = s.substring(0,i);
            return s;
         }
         else if( a[i]=='\'') {
            s = s.substring(0,i+1);
            if( s.endsWith("°0'") ) s=s.substring(0,s.length()-2);    // On enlève aussi les minutes nulles
            return s;
         }
            }
      if( flagDecimal>0 ) return s.substring(0,flagDecimal);
      if( flagZero>0 ) return s.substring(0,flagZero+1);
      return s;
   }


   /** Indication de la position d'une source.
    * (en fonction du repere courant)
    * @param o La source
    * @param methode 0 dans pos, 1 dans text (memorisation+clipboard)
    */
   protected void seeCoord(Position o) { seeCoord(o,0); }
   protected void seeCoord(Position o,int methode) {
      String s=getLocalisation(o);
      if( s==null ) return;

      if( methode==0 ) { setTextAffichage(s); setMode(AFFICHAGE); }
      else {
         //         aladin.copyToClipBoard(s); //POSE TROP DE PROBLEME
         setTextSaisie(s);
         setMode(SAISIE);
         aladin.console.printInPad(s+"\n");
      }
   }
   
   /** Localisation de la source en fonction du frame courant */
   protected String getLocalisation(Obj o) {
      String s="";
      int frame = getFrame();
      switch( frame ) {
         case XY:
            ViewSimple v = aladin.view.getCurrentView();
            Projection proj = v.getProj();
            Coord c = new Coord(o.getRa(),o.getDec());
            proj.getXY(c);
            double x = c.x;
            double y = c.y;
            Plan plan = v.pref;
            if( plan.isImage() ) s=Util.myRound(""+(x+0.5),2)
                  +" "+Util.myRound(""+(((PlanImage)plan).naxis2-(y-0.5)),2);
            else s=null;
            break;
         default : s = s+ J2000ToString(o.getRa(),o.getDec());
      }
      return s;
   }

   // retourne true s'il s'agit d'un nom de fichier local
   private boolean isFile(String s) {
      File f = new File(aladin.getFullFileName(s));
      return f.canRead();
   }

   // Petit raccourci pour insérer "load " devant une url ou un nom de fichier
   private String shortCutLoad(String s) {
      if( s.startsWith("http://") || s.startsWith("https://")
            || s.startsWith("ftp://") || s.startsWith("file://")
            || isFile(s) ) {
         s = "load "+s;
         setTextSaisie(s);
      }
      return s;
   }

   /** Gere la validation du champ de saisie rapide. */
   private void submit() {
      String s = getTextSaisie();
      if( s.length()>0 ) {
         s=shortCutLoad(s);
         aladin.console.addLot(s);
//         aladin.console.addCmd(s);
      }
      readyToClear();
   }

   protected void actionChoice() {
      if( text==null ) return;
      try {
         aladin.calque.resumeFrame();
         previousFrame=getFrame();

         // Change la dernière coordonnée mémorisée
         setTextSaisie( convert(getTextSaisie(),previousFrame,getFrame()));
      } catch( Exception e ) {  if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
   }
   public void keyTyped(KeyEvent e) { }
   public void keyReleased(KeyEvent e) { }
   public void keyPressed(KeyEvent e) { }
      
   /** Action à opérer lorsque l'on clique sur le triangle au bout du champ de saisie */
   protected void triangleAction(int x) { triangleAction(x,-1); }
   protected void triangleAction( final int x, int initIndex ) {
      int max=20;
      ArrayList<String> v = aladin.console.getRecentHistory( initIndex, max );
      if( v.size()==0 ) return;
      
      // On crée un JPopupmenu contenant les 20 dernières commandes, et s'il y en a encore,
      // ajoute à la fin de la liste une entrée "..." qui permet d'avoir les 20 suivantes
      popup = new JPopupMenu();
      for( String s: v ) {
         JMenuItem mi = null;
         if( s.equals("...") ) {
            mi = new JMenuItem(s);
            mi.setActionCommand(""+(initIndex+max));
            mi.addActionListener( new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  try {
                     int index = Integer.parseInt( ((JMenuItem)e.getSource()).getActionCommand() );
                     triangleAction(x,index);
                  }catch( Exception e1) {}
               }
            });

         } else {
            mi = new JMenuItemExt( s.length()>80 ? s.substring(0,78)+" ..." : s);
            mi.setActionCommand(s);
            mi.addActionListener( new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  String s = ((JMenuItem)e.getSource()).getActionCommand();
                  aladin.console.addLot(s);
               }
            });
         }
         popup.add(mi);
      }
      setComponentPopupMenu(popup);
      popup.show(this, x-50, getHeight());
   }
   
   protected boolean isPopupVisible() { return c.isPopupVisible() || isPopupShown(); }
   
   /** retourne true si le menu de l'historique des commandes est actuellement ouvert (visible) */
   protected boolean isPopupShown() {
      if( popup==null ) return false;
      return popup.isVisible();
   }
   
   class JMenuItemExt extends JMenuItem {
      JMenuItemExt(String s) {
         super(s);
         addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
               String s = ((JMenuItem)e.getSource()).getActionCommand(); 
               setMode(SAISIE);
               setTextSaisie(s);
            }
         });
      }
   }
   
   
   private boolean flagCheckHistory = false;
   
   /** Retourne true s'il faut afficher un petit triangle au bout du champ de saisie */
   protected boolean hasTriangle() {
      // Le non affichage du triangle n'a lieu qu'au premier lancement d'Aladin. Une fois
      // qu'une commande a été passée, il n'y plus de raison de tester le contenu de l'historique
      if( !flagCheckHistory ) {
         if( aladin.console.getRecentHistory(1).size()>0 ) flagCheckHistory=true;
         return false;
      }
      return true;
   }
}

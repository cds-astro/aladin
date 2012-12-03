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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

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
public final class Localisation extends MyBox {
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
  
   // Le label pour chaque repere (dans l'ordre des constantes ci-dessus)
   static final String [] REPERE = {
      "ICRS","ICRSd","Ecliptic","Gal","SGal",
      "J2000","J2000d","B1950","B1950d","B1900","B1875",
      "XY Fits","XY image","XY linear"
   };
   
   // Le mot clé RADECSYS Fits correspondant au système de coordonnée
   static final String [] RADECSYS = {
      "ICRS","ICRS",null,null,null,
      "FK5","FK5","FK4","FK4","FK4","FK4",
      null,null,null,
   };
   
   // Le préfixe du mot clé CTYPE1 Fits correspondant au système de coordonnée
   static final String [] CTYPE1 = {
      "RA---","RA---","ELON-","GLON-","SLON-",
      "RA---","RA---","RA---","RA---","RA---","RA---",
      null,null,"SOLAR",
   };

   // Le préfixe du mot clé CTYPE2 Fits correspondant au système de coordonnée
   static final String [] CTYPE2 = {
      "DEC--","DEC--","ELAT-","GLAT-","SLAT-",
      "DEC--","DEC--","DEC--","DEC--","DEC--","DEC--",
      null,null,"SOLAR",
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

   private int previousFrame=-1; // Frame précédent;
   
   /* Pour gerer les changements de frame */
   Astrocoo afs = new Astrocoo(AF_ICRS);	// Frame ICRS (la reference de base)

  /** Creation de l'objet de localisation. */
   protected Localisation(Aladin aladin) {
      super(aladin,aladin.chaine.getString("POSITION"));
      String tip = aladin.chaine.getString("TIPPOS");
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

//      } else if( key==KeyEvent.VK_DOWN || key==KeyEvent.VK_UP) {
//         first=true;
//         String s = aladin.pad.getHistCommand(key);
//         if( s!=null ) cmd = new StringBuffer(s);
         
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
   
//   public Dimension getPreferredSize() {
//      return  new Dimension(350,24);
//   }
   
   /** Fait clignoter le champ pour attirer l'attention
    * de l'utilisateur et demande le focus sur le champ de saisie */
   protected void focus(String s) {
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
            text.setText("");
            text.requestFocusInWindow();
         }
      }).start();
   }
   
   protected void setInitialFocus() {
      setMode(SAISIE);
      text.requestFocusInWindow();
      text.setCaretPosition(text.getText().length());
   }
   
   protected void infoStart() {
      if( !aladin.calque.isFree() || text.getText().length()>0  || aladin.dialog==null || aladin.dialog.isVisible() ) return;
      setMode(SAISIE);
      final String s = aladin.GETOBJ;
      text.setText(s);
     (new Thread() {
        Color def = text.getBackground();
        Color deff = text.getForeground();
        public void run() {
           flagReadyToClear=true;
           text.setBackground(Color.white);
           for( int i=0; i<3 && aladin.calque.isFree() && !flagStopInfo ; i++ ) {
              if( !flagStopInfo ) {
                 text.setText("");
                 text.setForeground(Color.gray);
                 Util.pause(200);
              }
              if( !flagStopInfo ) {
                 text.setText(s);
                 Util.pause(1000);
              }
           }
           if( flagStopInfo ) {
              text.setCaretPosition(text.getText().length());
              flagReadyToClear=flagStopInfo=false;
           }
           text.setForeground(deff);
           text.requestFocusInWindow();
        }
     }).start();
  }
   
   protected JComboBox createChoice() {
      final JComboBox c = super.createChoice();
      c.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            setInternalFrame(c.getSelectedIndex());
         }
      });
      c.setPrototypeDisplayValue(new Integer(100000));
      c.setFont(F);
      for( int i=0; i<REPERE.length; i++ ) c.addItem(REPERE[i]);
//      else for( int i=0; i<REPERE.length-1; i++ ) c.addItem(REPERE[i]);
      c.setSelectedIndex(ICRS);
      previousFrame=ICRS;
      c.setMaximumRowCount(REPERE.length);
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
   
   /** Retourne le nom du frame passé en paramètre */
   protected String getFrameName() { return getFrameName(frame); }
   static public String getFrameName(int frame) { return frame<0 ? "" : REPERE[frame]; }

   /** Retourne la position du menu deroulant */
   protected int getFrame() { return frame; }
   
   /** Insère le résultat d'une résolution Sésame dans le champ de commande avec le label
    * POSITION histoire que cela se comprenne */
   protected void setSesameResult(String s) {
      aladin.localisation.setTextSaisie(s);
//      aladin.localisation.label.setText(POSITION);
      aladin.localisation.readyToClear();
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
      if( view!=null && view.isPlotView() ) frame=XYLINEAR;

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
            else if( frame==XYLINEAR ) {
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
                   
                   s=J2000ToString(coo.al,coo.del,precision);
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
//         Aladin.copyToClipBoard(s);   C'EST VRAIMENT TROP GONFLANT
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
   
   
//   static protected Coord frameToFrame1(Coord c, int frameSrc,int frameDst) {
//      if( frameSrc==frameDst ) return c;
//      Astrocoo aft = new Astrocoo(Localisation.getAstroframe(frameSrc),c.al,c.del);
//      aft.convertTo(Localisation.getAstroframe(frameDst));
//      c.al=aft.getLon();
//      c.del=aft.getLat();
//      return c;
//   }
   
   
   public static Coord frameToFrame(Coord c, int frameSrc,int frameDst) {
      if( frameSrc==frameDst ) return c;
      Coo cTmp = new Coo(c.al,c.del);
      if( frameSrc!=ICRS && frameSrc!=ICRSD ) Localisation.getAstroframe(frameSrc).toICRS(cTmp);
      if( frameDst!=ICRS && frameDst!=ICRSD ) Localisation.getAstroframe(frameDst).fromICRS(cTmp);
      c.al = cTmp.getLon();
      c.del= cTmp.getLat();
      return c;
   }
   
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
    */
   protected String convert(String coo,int frameSource,int frameTarget) {

      // Champ vide => Rien à faire
      if( coo==null || coo.length()==0 || coo.indexOf("--")>=0 ) return "";
      
      // Identificateur à la place d'une coordonnée => Rien à faire
      for( int i=0; i<coo.length(); i++) {
         char ch = coo.charAt(i);
         if( (ch>='A' && ch<='Z') || (ch>='a' && ch<='z') ) return coo;
      }
      
      // Edition et conversion si nécessaire
      try {
         Astrocoo aft = new Astrocoo( getAstroframe(frameSource) );
         aft.set(coo);
         if( frameSource!=frameTarget ) aft.convertTo( getAstroframe(frameTarget) );
         aft.setPrecision(Astrocoo.ARCSEC+1);
         String s = (frameTarget==J2000D || frameTarget==B1950D || frameTarget==ICRSD
                  || frameTarget==ECLIPTIC || frameTarget==GAL || frameTarget==SGAL )?
                aft.toString("2d"):aft.toString("2s");

//if( frameSource!=frameTarget ) {
//   System.out.println("convert ["+coo+"]/"+Localisation.REPERE[frameSource]+"  => ["+s+"]/"+Localisation.REPERE[frameTarget]);         
////try { throw new Exception("convert"); } catch(Exception e) { e.printStackTrace(); }
//}
         
         if( s.indexOf("--")>=0 ) return "";
         return s;
      } catch( Exception e ) { e.printStackTrace(); return coo; }
   }

  /** Retourne la position d'un objet en fonction du frame
   * courant
   * @param al,del : coordonnees (ICRS)
   * @return La chaine decrivant la position
   */
   protected String J2000ToString(double al,double del) { return J2000ToString(al,del,Astrocoo.ARCSEC+1); }
   protected String J2000ToString(double al,double del,int precision) {    
      Coord cTmp = new Coord(al,del);
      cTmp = ICRSToFrame(cTmp);
      afs.setPrecision(precision);
      return frameToString(cTmp.al,cTmp.del);
   }
   
   protected String frameToString(double al,double del) { return frameToString(al,del,Astrocoo.ARCSEC+1); }
   protected String frameToString(double al,double del,int precision) {
      int i = getFrame();
      afs.setPrecision(precision);
      afs.set(al,del);
      try {
         return (i==J2000D || i==B1950D || i==ICRSD
              || i==ECLIPTIC || i==GAL || i==SGAL )?
               afs.toString("2d"):afs.toString("2:");
      } catch( Exception e) { System.err.println(e); }
      return "";
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
      else { Aladin.copyToClipBoard(s); setTextSaisie(s); setMode(SAISIE); aladin.console.setInPad(s+"\n"); }
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
         aladin.console.pushCmd(s);
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
   
}

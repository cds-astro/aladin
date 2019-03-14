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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.astro.Astrocoo;
import cds.astro.Astropos;
import cds.astro.Astrotime;
import cds.astro.Coo;
import cds.astro.Unit;
import cds.tools.Computer;
import cds.tools.Util;
import cds.xml.TableParser;

/**
 * Gestion d'une fenêtre d'outils basiques sur les coordonnées
 * - Affichage dans chaque système de coordonnées (ICRS,Gal,SGAL,ECL,J2000,B1950...)
 * - Prise en compte d'une époque d'observation et d'une époque d'affichage
 * - Prise en compte d'un mouvement propre
 * La position initiale peut être donnée soit :
 * - en cliquant dans l'image (position du réticule)
 * - en cliquant dans la table des sources (source courante)
 * - en entrant directement des valeurs dans le formulaire
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (janvier 2014) creation
 */
public class FrameCooToolbox extends JFrame {

   protected Aladin aladin;
   
   private boolean init=false;       // True si une coord. a été fournie
   private double ra,dec;            // Coordonnées de la position d'origine ICRS
   private double originEpoch=2000;  // Epoque de la position d'origine
   private double pmra=0,pmdec=0;    // Mouvement propre (mas/yr) associée à la position d'origine
   private double rv=0;              // Vélocité radiale (km/s)
   private double plx=0;             // Parallax (mas)
   private double targetEpoch=2000;  // Epoque pour laquelle on veut calculer la position
   private int precision=-1;         // Précision, -1 si non précisée

   private JTextField cooField[];   // Les champs des coordonnées dans les différents systèmes
   private JTextField pmraField,pmdeField,originEpochField,targetEpochField;
   private JTextField distField,plxField,rvField,distALField,distParsecField;
   private SliderEpochTool sliderEpoch;
   private JComboBox<String> precisionField;
   
   static private String PREC_S  [] = { "unknown", "1degree", "0.1degree", "1arcmin", "0.1arcmin", "1arcsec",
   "0.1arcsec", "10mas", "1mas", "0.1mas", "10\u00b5as",
   "1\u00b5as", "0.1\u00b5as", "0.01\u00b5as"  };

   static private int    PREC_VAL[] = { -1,   1,2,3,4,5,6,7,8,9,10,11,12 };
   
   protected FrameCooToolbox(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      setTitle(aladin.chaine.getString("COOTOOL"));
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, true, aladin);
      setLocation( Aladin.computeLocation(this) );
      getContentPane().add( createPanelLeft(), BorderLayout.WEST);
      getContentPane().add( createPanelRight(), BorderLayout.EAST);
      getContentPane().add( getPanelBottom(), BorderLayout.SOUTH);
      pack();
      setVisible(true);
   }
   
   public void processWindowEvent(WindowEvent e) {
      if( e.getID() == WindowEvent.WINDOW_CLOSING ) {
         aladin.frameCooTool=null;
      }
      super.processWindowEvent(e);
   }
   
   // Création du panel des coordonnées dans les différents systèmes
   private JPanel createPanelRight() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);

      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      p.setLayout(g);
      
      int n;
      
      // On ne prend pas en compte les systèmes cartésiens
      for( n=0; n<Localisation.REPERE.length && !Localisation.REPERE[n].startsWith("XY"); n++ );
      
      JLabel [] label = new JLabel[n];
      cooField = new JTextField[n];
      for( int i=0; i<n; i++ ) {
         label[i] = new JLabel( Localisation.REPERE[i] );
         cooField[i] = new JTextField(22);
         cooField[i].setActionCommand(i+"");
         cooField[i].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               setCoordFrom(e.getActionCommand(), ((JTextField)e.getSource()).getText());
            }
         });
         if( i==Localisation.J2000 ) c.insets.top=10;
         else c.insets.top=0;
         PropPanel.addCouple(p,label[i], cooField[i], g,c);
      }
      
      return p;
   }
   
   /** Construction du panel des boutons de validation
    * @return Le panel contenant les boutons Apply/Close
    */
   protected JPanel getPanelBottom() {
      JPanel p = new JPanel();
      p.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton b;
//      p.add( b=new JButton(aladin.chaine.getString("UPAPPLY"))); 
//      b.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { modify(); }} );
//      b.setFont(b.getFont().deriveFont(Font.BOLD));
      p.add( b=new JButton(aladin.chaine.getString("TAGIT"))); 
      b.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { tagit(); }} );
      p.add( b=new JButton(aladin.chaine.getString("CLEAR"))); 
      b.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { reset(); }} );
      p.add( b=new JButton(aladin.chaine.getString("UPCLOSE"))); 
      b.addActionListener( new ActionListener() { 
         public void actionPerformed(ActionEvent e) { 
            aladin.frameCooTool=null;
            dispose(); }
      });
      return p;
   }

   
   // Création du panel contenant les époques et les composantes du PM
   private JPanel createPanelLeft() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);

      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      p.setLayout(g);
      
      c.insets = new Insets(0,0,0,0);
      JLabel pmraLabel = new JLabel("PMRA (mas/yr)");
      pmraField = new JTextField(10);
      pmraField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });
      JLabel pmdeLabel = new JLabel("PMDEC (mas/yr)");
      pmdeField = new JTextField(10);
      pmdeField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });
      JLabel plxLabel = new JLabel("Parallax (mas)");
      plxField = new JTextField(10);
      plxField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });
      JLabel distParsecLabel = new JLabel("Dist (parsec)");
      distParsecField = new JTextField(10);
      distParsecField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify(2);
         }
      });

      JLabel distALLabel = new JLabel("Dist (light-year)");
      distALField = new JTextField(10);
      distALField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify(1);
         }
      });

      JLabel rvLabel = new JLabel("Radial velocity (km/s)");
      rvField = new JTextField(10);
      rvField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });
     
      
      JLabel originEpochLabel = new JLabel("Origin epoch (Jy)");
      originEpochField = new JTextField(10);
      originEpochField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });

      JLabel currentEpochLabel = new JLabel("Target epoch (Jy)");
      targetEpochField = new JTextField(10);
      targetEpochField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });
      
      JLabel precisionLabel = new JLabel("Precision");
      precisionField = new JComboBox<>( PREC_S );
      precisionField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            modify();
         }
      });
      
      sliderEpoch = new SliderEpochTool(this);
      
      PropPanel.addCouple(p,pmraLabel, pmraField, g,c);
      PropPanel.addCouple(p,pmdeLabel, pmdeField, g,c);
      PropPanel.addCouple(p,plxLabel,  plxField, g,c);
      PropPanel.addCouple(p,distParsecLabel, distParsecField, g,c);
      PropPanel.addCouple(p,distALLabel, distALField, g,c);
      PropPanel.addCouple(p,rvLabel,   rvField, g,c);
      PropPanel.addCouple(p,originEpochLabel, originEpochField, g,c);
      PropPanel.addCouple(p,currentEpochLabel, targetEpochField, g,c);
      
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      c.weightx = 1.0;
      g.setConstraints(sliderEpoch,c);
      p.add(sliderEpoch);
      
      JLabel distLabel = new JLabel("Ang.dist. from orig.loc");
      distField = new JTextField(10);
      distField.setEditable(false);
      PropPanel.addCouple(p,distLabel, distField, g,c);
      PropPanel.addCouple(p,precisionLabel, precisionField, g,c);

      return p;
   }
      
   /** Positionne les informations de positions associées à une source
    * Repère les colonnes RA,DEC,PMRA,PMDEC, effectue les conversions requises
    * et affiche les coordonnées en conséquence.
    * @TODO L'époque d'origine devrait encore être récupéré du COOSYS ou équivalent
    *       plutôt que uniquement le défaut 2000
    * @param s La source dont on extrait la position
    */
   protected void setSource(Source s)  {
      Legende leg = s.getLeg();
      
      // Récupération des coordonnées d'origines
      int nra   = leg.getRa();
      int ndec  = leg.getDe();
      if( nra==-1 && ndec==-1 ) return;
      String sra = s.getValue(nra);
      String sdec= s.getValue(ndec);
      
      // On suppose que si RA est en RADIANS, DE aussi
      int unit = TableParser.getUnit(leg.getUnit(nra));
     
      try {
         Astrocoo c = new Astrocoo();
         TableParser.getRaDec(c, sra, sdec, TableParser.FMT_UNKNOWN, unit);
         ra = c.getLon();
         dec = c.getLat();
         
         pmra=pmdec=0;
         
         // Récupération des mouvements propres
         int npmra = leg.getPmRa();
         int npmde = leg.getPmDe();
         if( npmra>0 && npmde>0 ) {
            Unit mu1 = new Unit();
            try {
               mu1.setUnit(s.getUnit(npmra));
               mu1.setValue(s.getValue(npmra));
            } catch( Exception e1 ) { }
            Unit mu2 = new Unit();
            try {
               mu2.setUnit(s.getUnit(npmde));
               mu2.setValue(s.getValue(npmde));
            } catch( Exception e1 ) { }
            if( (mu1.getValue()!=0 || mu2.getValue()!=0) 
                  && !Double.isNaN(mu1.getValue()) && !Double.isNaN(mu2.getValue()) ) {
               try {
                  mu1.convertTo(new Unit("mas/yr"));
               } catch( Exception e) { 
                  // Il faut reinitialiser parce que mu1 a changé d'unité malgré l'échec !
                  mu1.setUnit(s.getUnit(npmra));
                  mu1.setValue(s.getValue(npmra));
                  mu1.convertTo(new Unit("ms/yr"));
                  double v = 15*mu1.getValue()*Math.cos(c.getLat()*Math.PI/180);
                  mu1 = new Unit(v+"mas/yr");
               };

               pmra =mu1.getValue();
               mu2.convertTo(new Unit("mas/yr"));
               pmdec = mu2.getValue();
            }
         }
         
      } catch( Exception e ) {
         e.printStackTrace();
      }
      
      // Récupération de l'époque
      originEpoch = s.plan.getOriginalEpoch().getJyr();
      targetEpoch = s.plan.getEpoch().getJyr();
      
      init=true;
      resume();

   }
   
   /** Extrait les coordonnées à partir de la position courante
    * du réticule
    * @param ra  Ascension droite ICRS ep2000 du réticule
    * @param dec  Déclinaison ICRS ep2000 du réticule
    */
   protected void setReticle(double ra,double dec) {
      this.ra=ra;
      this.dec=dec;
      targetEpoch=2000;
      
      originEpoch=2000;
      try {
         Plan pi = aladin.calque.getPlanBase();
         if( pi instanceof PlanImage) {
            String ep=((PlanImage)pi).getDateObs();
            if( ep!=null ) {
               Astrotime t = new Astrotime();
               t.set( ep );
               originEpoch = t.getJyr();
            }
         }
      } catch( Exception e ) { }

      pmra=pmdec=0;
      init=true;
      resume(false);
   }
   
   /** Extrait les coordonnées à partir d'une saisie directe dans la liste
    * des coordonnées affichées.
    * @param sFrameSource le numéro (String !) correspondant au frame d'origine
    * @param s La coordonnée à extraire.
    */
   protected void setCoordFrom(String sFrameSource, String s) {
      try {
         int frameSource = Integer.parseInt( sFrameSource);
         Astrocoo aft = new Astrocoo( Localisation.getAstroframe(frameSource) );
//         aft.setPrecision(Astrocoo.MAS+3);
         
         // On ajoute l'époque si elle n'est pas mentionnée
         if( s.indexOf('(')<0 &&  originEpochField.getText().trim().length()>0 ) {
            s = s+" (J"+ originEpochField.getText().trim() + ")";
         }
         aft.set(s);
         aft.convertTo( Localisation.getAstroframe(Localisation.ICRS) );
         originEpochField.setText(aft.epoch+"");
         targetEpochField.setText(aft.epoch+"");
         precision=-1;
         ra=aft.getLon();
         dec=aft.getLat();
         init=true;
         modify();
      } catch( Exception e ) { }
   }
   
   /** Calcule la position en fonction de l'époque source, de l'époque cible
    * et du mouvement propre
    */
   protected void modify() { modify(0); }
   protected void modify(int from) {
      String s;
      try {
         s = originEpochField.getText();
         if( Character.isDigit( s.charAt(0)) ) s = "J"+s;
         originEpoch = (new Astrotime(s)).getJyr();
      } catch( Exception e ) { }

      try {
         s = targetEpochField.getText();
         if( Character.isDigit( s.charAt(0)) ) s = "J"+s;
         targetEpoch = (new Astrotime(s)).getJyr();
      } catch( Exception e ) { }

      try {
         precision = PREC_VAL[ precisionField.getSelectedIndex() ];
      } catch( Exception e ) { precision=-1; }

      try {
         pmra = getField(pmraField);
      } catch( Exception e ) { }

      try {
         pmdec = getField(pmdeField);
      } catch( Exception e ) { }

      try {
         if( from==2 ) {
            double d = getField(distParsecField);
            plx = arsec2plx(d);
         } else if( from==1 ) {
            double d = getField(distALField);
            plx = al2plx(d);
        } else {
           plx = getField(plxField);
         }
      } catch( Exception e ) { }

      try {
         rv = getField(rvField);
      } catch( Exception e ) { }
      resume();
      moveRepere();      
   }
   
   // Création d'un tag à la position indiquée, et déplacement des vues à l'emplacement correspondant
   private void tagit() {
      try {
         Plan plan = aladin.calque.selectPlanTool();
         Coord coo = getCoordInICRS();
         Tag tag = new Tag(plan,coo,null);
         aladin.calque.updateToolCatTag(tag,aladin.view.getCurrentView());
//         aladin.view.setRepere(coo);
         aladin.view.gotoThere( coo );
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   // Retourne la valeur numérique du champ qui peut être une expression algébrique
   private double getField(JTextField field) throws Exception {
      String s = field.getText();
      return Computer.compute(s);
   }
   
   /** Change l'époque cible et relance le calcul en conséquence */
   protected void setEpoch( String s) {
      try {
         if( Character.isDigit( s.charAt(0)) ) s = "J"+s;
         targetEpoch = (new Astrotime(s)).getJyr();
      } catch(Exception e ) {
      }
      resume(false);
   }

   // Remet à jour tout le tableau en fonction des valeurs courantes */
   private void resume() { resume(true); }
   private void resume(boolean flagLog) {
      if( cooField==null ) return;
      for( int i=0;i<cooField.length; i++ ) cooField[i].setText( getCoordIn(i) );
      
      originEpochField.setText( !init ? "" : originEpoch+"" );
      targetEpochField.setText( !init ? "" : targetEpoch+"" );
//      int i = Util.indexInArrayOf(precision+"", PREC_S);
//      precisionField.setSelectedIndex( !init ||i==-1 ? 0 : i );
      pmraField.setText(  !init ? "" : (pmdec==0 && pmra==0) ? "" : Util.myRound(pmra+"",3) );
      pmdeField.setText(  !init ? "" : (pmdec==0 && pmra==0) ? "" :Util.myRound( pmdec+"",3) );
      rvField.setText(    !init ? "" : rv==0 ? "" : Util.myRound(rv+"",3) );
      plxField.setText(   !init ? "" : getParallaxe() );
      
      distField.setText( getDistance() );
      distALField.setText( getDistAL() );
      distParsecField.setText( getDistParsec() );
      
      sliderEpoch.setValue((int)targetEpoch);
      
      if( flagLog ) aladin.glu.log("CoordToolbox","");
   }
   
   // Reset complet du formulaire
   private void reset() {
      init=false;
      resume(false);
      precision=-1;
      precisionField.setSelectedIndex(0);
   }
   
   
//   /** Déplace le réticule à la position courante */
   private void moveRepere() {
      try {
         Coord coo = getCoordInICRS();
         aladin.view.gotoThere( coo );
      } catch( Exception e ) { }
   }
   
   static private double PARSEC2AL = 3.2614945566008;
  
   
   // Calcule la distance en AL à partir de la parallaxe (mas)
   private String getDistAL() {
      if( plx==0 ) return "";
      return Util.myRound( 1000.*PARSEC2AL/plx );
   }
   
   // Calcule la distance en parsec à partir de la parallaxe (mas)
   private String getDistParsec() {
      if( plx==0 ) return "";
      return Util.myRound( 1000./plx );
   }
   
   // Donne la parallaxe en mas à partir de la distance en AL
   private double al2plx(double al) {
      return 1000.*PARSEC2AL/al;
   }
   
// Donne la parallaxe en mas à partir de la distance en parsec
   private double arsec2plx(double p) {
      return  1000./p;
   }
   
   // Calcul la parallaxe entre la position à l'époque d'origine et à l'époque cible
   private String getParallaxe() {
      if( plx==0 ) return "";
      if( targetEpoch==originEpoch || rv==0 ) return plx+"";
      Astropos targetCoo = new Astropos(Localisation.AF_ICRS);
      targetCoo.set(ra,dec,originEpoch,null,pmra,pmdec,originEpoch,null,
            new double[] { plx,0}, new double []{ rv,0});
      if( precision>-1 ) targetCoo.setPrecision(precision);
      targetCoo.toEpoch(targetEpoch);
     
//      System.out.println("==> "+targetCoo);
      return Util.myRound( ""+targetCoo.plx,6 );
   }
   
   // Calcul la distance entre la position à l'époque d'origine et à l'époque cible
   private String getDistance() {
      if( !init || targetEpoch==originEpoch || pmra==0 && pmdec==0 ) return "";
      Astropos targetCoo = new Astropos(Localisation.AF_ICRS);
      targetCoo.set(ra,dec,originEpoch,null,pmra,pmdec,originEpoch,null,
            new double[] { plx,0}, new double []{ rv,0});
      if( precision>-1 ) targetCoo.setPrecision(precision);
      targetCoo.toEpoch(targetEpoch);
     
//      System.out.println("==> "+targetCoo);
      double d = Coo.distance(ra, dec, targetCoo.getLon(), targetCoo.getLat() );
      return Coord.getUnit( d );
   }
   
   // Calcule les coordonnées en ICRS à l'époque indiquée
   private Coord getCoordInICRS() {
      Astropos targetCoo = new Astropos(Localisation.AF_ICRS);
      targetCoo.set(ra,dec,originEpoch,null,pmra,pmdec,originEpoch,null,
            new double[] { plx,0}, new double []{ rv,0});
      if( precision>-1 ) targetCoo.setPrecision(precision);
      targetCoo.convertTo( Localisation.getAstroframe(Localisation.ICRS) );
      targetCoo.toEpoch(targetEpoch);
      
      Coord coo = new Coord( targetCoo.getLon(), targetCoo.getLat() );
      return coo;
   }
   
   // Calcule et édite les coordonnées dans un système donnée (frameTarget) en prenant
   // en compte l'époque d'origine, l'époque cible et le mouvement propre
   private String getCoordIn(int frameTarget) {
      if( !init ) return "";
      Astropos targetCoo = new Astropos(Localisation.AF_ICRS);
      targetCoo.set(ra,dec,originEpoch,null,pmra,pmdec,originEpoch,null,
            new double[] { plx,0}, new double []{ rv,0});
      if( precision>-1 ) targetCoo.setPrecision(precision);
      targetCoo.convertTo( Localisation.getAstroframe(frameTarget) );
      targetCoo.toEpoch(targetEpoch);
      
      String s = (frameTarget==Localisation.J2000D || frameTarget==Localisation.B1950D 
               || frameTarget==Localisation.ICRSD  || frameTarget==Localisation.ECLIPTIC
               || frameTarget==Localisation.GAL    || frameTarget==Localisation.SGAL )?
                  targetCoo.toString("2d"):targetCoo.toString("2s");
                  
      if( s.indexOf("--")>=0 ) return "";
      return s;
   }
      
}

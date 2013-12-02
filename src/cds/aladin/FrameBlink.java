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

import java.awt.*;
import java.util.*;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Gestion de la fenetre associee a la creation d'un plan Blink
 * @author Pierre Fernique [CDS]
 * @version 1.0 : nov 2004 - Creation
 */
public final class FrameBlink extends FrameRGBBlink {
   // Les délais possibles entre deux frames
   static final protected int DELAY[]={0, 25, 50, 100, 200,400,800,1600,3200};
   String TITLE,INFO,HELP1,RSAMPREF,BLKDELAY,MOSAIC,BLINK,ERR1,ERR2;
   static final String STOPPED = "-- stopped --";
   
   // l'indice du délai par défaut dans DELAY[]
   static final int DEFAULT_DELAY=5;

   private ButtonGroup cmb;	    // Pour determiner si c'est du mosaic ou du blink
   private JRadioButton bcb,mcb;            // Le checkbox du blink et du mosaic
   private JComboBox cDelay;			// Le Choice des délais possible
   private JTextField textFieldRef;  // Pour indiquer l'image de référence pour le resampling
   
   protected void createChaine() {
      super.createChaine();
      TITLE     = a.chaine.getString("BLKTITLE");
      INFO      = a.chaine.getString("BLKINFO");
      HELP1     = a.chaine.getString("IMGHELP1");
      RSAMPREF  = a.chaine.getString("RGBRSAMPREF");
      BLKDELAY  = a.chaine.getString("BLKDELAY");
      BLINK     = a.chaine.getString("BLKBLK");
      MOSAIC    = a.chaine.getString("BLKMOSAIC");
      ERR1      = a.chaine.getString("BLKERR1");
      ERR2      = a.chaine.getString("BLKERR2");
   }

   /** Creation du Frame gerant la creation d'un plan Blink. */
   protected FrameBlink(Aladin aladin) {
      super(aladin);
      Aladin.setIcon(this);
   }

   protected String getTitre() {
      return TITLE;
   }

   protected String getInformation() {
      return INFO;
   }

   protected String getHelp() {
      return HELP1;
   }

   protected int getToolNumber() { return ToolBox.BLINK; }
   protected int getNb() { return 50; }
   protected String getLabelSelector(int i) { return (i + 1) + ")";}
   protected Color getColorLabel(int i) { return Color.black; }
   
   /** Création d'un Choice indiquant tous les délais possible
    * @param defaultDelay la valeur en ms par défaut (doit correspondre
    *                     à une valeur de DELAY[]
    * @return Le Choice des délais 
    */
   static protected JComboBox createChoiceDelay() {
      return createChoiceDelay(getDefaultDelay());
   }

   static protected JComboBox createChoiceDelay(int defaultDelay) {
      JComboBox c=new JComboBox();
      c.setFont(Aladin.BOLD);
      int n=0;
      for( int i=0; i < DELAY.length; i++ ) {
         if( DELAY[i]==0 ) c.addItem(STOPPED);
         else c.addItem(DELAY[i] + " ms");
         if( defaultDelay == DELAY[i] ) n=i;
      }
      c.setSelectedIndex(n);
      return c;
   }

   /** Retourne le délais sélectionné par l'utilisateur */
   protected int getDelay() {
      String s=(String)cDelay.getSelectedItem();
      if( s.equals(STOPPED) ) return 0;
      s=s.substring(0,s.indexOf(' '));
      return Integer.valueOf(s).intValue();
   }
   
   /** Retourne le délai minimal autorisé */
   static protected int getMinDelay() { return DELAY[1]; }
   
   /** retourne le délai maximal autorisé */
   static protected int getMaxDelay() { return DELAY[DELAY.length-1]; }

   /** retourne le délai par défaut */
   static protected int getDefaultDelay() { return DELAY[DEFAULT_DELAY]; }

   /** Retourne le JPanel additionnel de la Frame. Il contient le sélecteur
    * du délais */
   protected JPanel getAddPanel() {
      JPanel p=new JPanel();
      p.setLayout( new GridLayout(0,1));
      
      cmb = new ButtonGroup();
      
      JPanel p1 = new JPanel();
      JRadioButton b=mcb=new JRadioButton(MOSAIC); b.setActionCommand(MOSAIC);
      cmb.add(b);
      p1.add(b);
      bcb = b=new JRadioButton(BLINK);      b.setActionCommand(BLINK);
      cmb.add(b);
      b.setSelected(true);
      p1.add(b);
      p1.add(new Label(BLKDELAY));
      p1.add(cDelay=createChoiceDelay());

      p.add(p1);
      
                 
      p1 = new JPanel();
      p1.add(new Label(RSAMPREF));
      p1.add(textFieldRef=new JTextField("1"));
      p.add(p1);
      
      return p;
   }

   protected void adjustWidgets() {
      cDelay.setEnabled( cmb.getSelection().getActionCommand().equals(BLINK) );
   }
   
   protected void reset() {
      super.reset();
      textFieldRef.setText("1");
      cDelay.setSelectedIndex(DEFAULT_DELAY);
//      bcb.setSelected(true);
   }
   
   /** Positionne soit le mode BLINK (mode==0), soit le mode MOSAIC (mode=1) */
   protected void setMode(int mode) {
      if( mode==0 ) bcb.setSelected(true);
      else mcb.setSelected(true);
   }

   protected void submit() {
      // Détermination du plan de référence (ce sera le premier)
      int first=-1;
      try { first = Integer.parseInt(textFieldRef.getText())-1; }
      catch( Exception e1 ) {  }
      
      // Vérification que cela correspond à quelque chose
      if( first<0 || first>=ch.length  ) {
         Aladin.warning(this,ERR1+" [1.."+(ch.length-1)+"] !");
         return;
      }
      
      // Détermination des plans concernés en commençant par first
      int n=getNb();
      Vector v=new Vector(10);
      for( int i=0; i < n; i++, first++ ) {
         if( first>=ch.length ) first=0;
         PlanImage pi=(PlanImage)getPlan(ch[first]);
         if( pi != null ) v.addElement(pi);
      }
      
      if( v.size()==1 ) {
         Aladin.warning(this,ERR2);
         return;
      }
      
      // Récupération de la commande (blink ou mosaic)
      boolean blink = cmb.getSelection().getActionCommand().equals(BLINK);
      
      // recopie dans un tableau et génération de la commande pour le pad
      StringBuffer cmd = new StringBuffer(blink ? "blink" : "mosaic");
      PlanImage p[]=new PlanImage[v.size()];
      Enumeration e=v.elements();
      for( int i=0; e.hasMoreElements(); i++ ) {
         p[i]=(PlanImage) e.nextElement();
         cmd.append(" "+Tok.quote(p[i].label));
      }

      a.console.printCommand(cmd.toString());
      if( blink ) a.calque.newPlanImageBlink(p,null,getDelay());
      else a.calque.newPlanImageMosaic(p,null,null);
      hide();
   }
}

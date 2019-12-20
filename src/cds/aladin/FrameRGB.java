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

package cds.aladin;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.tools.Util;

/**
 * Gestion de la fenetre associee a la creation d'un plan RGB
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : novembre 2004 découpage en FrameRGBBlink dérivée en FrameRGB 
 *                              et FrameBlink
 * @version 1.0 : (1 fevrier 2002) Creation
 */
public final class FrameRGB extends FrameRGBBlink {

   String R,G,B;
   String X,TITLE,INFO,HELP1,RED,GREEN,BLUE,LUM,RSAMPREF,SUBSTR;

   // Les composantes de l'objet
   private ButtonGroup cbg;	 // Les checkBox pour determiner l'image de base
   private JRadioButton cbDiff;		 // Le checkbox pour indiquer une difference entre 2 plans
   private JRadioButton cbX;		 // Checkbox pour le plan defaut du reechantillonage
   protected JComboBox cR, cG, cB,cL; // must be class members, so that they can be found in robot mode
   
   protected void createChaine() {
      super.createChaine();
      X     = a.chaine.getString("IMGBEST");
      TITLE = a.chaine.getString("RGBTITLE");
      INFO  = a.chaine.getString("RGBINFO");
      HELP1  = a.chaine.getString("IMGHELP1");
      RED   = a.chaine.getString("RGBRED");
      GREEN = a.chaine.getString("RGBGREEN");
      BLUE  = a.chaine.getString("RGBBLUE");
      LUM   = "Luminosity";
      RSAMPREF  = a.chaine.getString("RGBRSAMPREF");
      SUBSTR= a.chaine.getString("RGBSUBSTR");
      R = "R";
      G = "G";
      B = "B";
   }

  /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameRGB(Aladin aladin) {
      super(aladin);
      Aladin.setIcon(this);
   }
 
   protected String getTitre() { return TITLE; }

   protected String getInformation() { return INFO; }

   protected String getHelp() { return HELP1; }

   protected int getToolNumber() { return ToolBox.RGB; }
   protected int getNb() { return 3; }

   protected String getLabelSelector(int i) {
      return i == 0?RED:i == 1?GREEN:i==2?BLUE:LUM;
   }

   protected Color getColorLabel(int i) {
      return i == 0?Color.red:i == 1?Color.green:i==2?Color.blue:Color.black;
   }

   protected JPanel getAddPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH;

      JPanel p=new JPanel();
      p.setLayout(g);

      JLabel lx=new JLabel(RSAMPREF);
      cbg=new ButtonGroup();

      JRadioButton cbR=new JRadioButton(R); cbR.setActionCommand(R);
      cbg.add(cbR);
      JRadioButton cbG=new JRadioButton(G); cbG.setActionCommand(G);
      cbg.add(cbG);
      JRadioButton cbB=new JRadioButton(B); cbB.setActionCommand(B);
      cbg.add(cbB);
      cbX=new JRadioButton(X);              cbX.setActionCommand(X);
      cbg.add(cbX);
      cbX.setSelected(true);

      JPanel pp=new JPanel();
      pp.add(lx);
      pp.add(cbR);
      pp.add(cbG);
      pp.add(cbB);
      pp.add(cbX);
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);

      // Pour la difference entre 2 plans
      cbDiff=new JRadioButton(SUBSTR);
      cbDiff.setEnabled(false);
      c.fill=GridBagConstraints.NONE;
      g.setConstraints(cbDiff,c);
      p.add(cbDiff);
      c.fill=GridBagConstraints.BOTH;

      return p;
   }

   // Reset de la fenetre
   protected void reset() {
      cbX.setSelected(true);
      super.reset();
   }

   protected void adjustWidgets() { adjustCbDiff(); }

   // Activation ou desactivation du checkbox de la difference
   // Active uniquement si 2 plans et uniquement 2 plans sont selectionnes
   private void adjustCbDiff() {
      int i=(ch[0].getSelectedIndex() > 0?1:0)
            + (ch[1].getSelectedIndex() > 0?1:0)
            + (ch[2].getSelectedIndex() > 0?1:0);
      boolean enabled=cbDiff.isEnabled();
      boolean newEnabled=(i == 2);
      if( enabled == newEnabled ) return;
      cbDiff.setEnabled(newEnabled);
   }
   
   // Tableaux des correspondances couleurs => longueur d'ondes */
   private static String WAVEBAND[] = {
      "92CM",
      "100MU","60MU","25MU","12MU",
      "z","2MASS J","IRSA  J","H","K",
      "IR","i","N",
      "red","ER","SR","E","F",
      "green","V","g",
      "blue","O","J",
      "u",
    };
   private static double WAVELEN[] = {
      920000,
      100,60,25,12,
      1.0,1.2,1.6,2.2,
      0.8,0.8,0.8,0.8,
      0.6,0.6,0.6,0.6,0.6,
      0.5,0.5,0.5,
      0.4,0.4,0.4,
      0.3,
   };
   
   /** Retourne la longueur d'onde en fonction d'une chaine décrivant un plan.
    * Il s'agit d'essayer de trouver un code couleur (ex DSS2 ER SERC => ER)
    * et d'en déduire sa longueur d'onde. Utilise les deux tableaux WAVEBAND
    * et WAVELEN ci-dessus */
   private double getWaveLen(String info) {
      if( info==null ) return Double.MAX_VALUE;
      for(int i=0; i<WAVEBAND.length; i++ ) {
         int offset=-1;
         do {
            if( (offset=Util.indexOfIgnoreCase(info,WAVEBAND[i],offset+1))>=0 
                  && (offset==0
                        || !Character.isLetter(info.charAt(offset-1)))
                        && (offset+WAVEBAND[i].length()==info.length()
                              || !Character.isLetter(info.charAt(offset+WAVEBAND[i].length())))
            ) {
//System.out.println(info+" ["+WAVEBAND[i]+"] => "+WAVELEN[i]);
               return WAVELEN[i];
            }
         } while( offset!=-1 );
      }
//System.out.println(info+" => rien trouvé");
      return Double.MAX_VALUE;
   }
   
   // Mémorise la liste des plans images par longueur d'onde décroissante */
   private Object choicePlanSort[]=null;
   
   /** Crée le tableau choicePlanSort et le tri par longueur d'onde décroissante */
   private void sortPlan() {
      Vector v = new Vector(choicePlan.length);
      
      // S'il n'y a aucun ou 1 plan image sélectionné, on prend les 3 premiers
      // dans la pile
      int n=0;
      for( int i=0; i<choicePlan.length; i++ ) if( choicePlan[i].selected ) n++;
      boolean flagAll = n<=1;
      
      for (int i=0; i<choicePlan.length && v.size()<=3; i++) {
         if( flagAll || choicePlan[i].selected ) v.addElement(choicePlan[i]);
      }
      choicePlanSort = v.toArray();

      Arrays.sort(choicePlanSort, new Comparator() {
         public int compare(Object o1, Object o2) {
            PlanImage p1 = (PlanImage)o1;
            PlanImage p2 = (PlanImage)o2;
            double wb1 = getWaveLen(p1.survey());
            double wb2 = getWaveLen(p2.survey());
            return wb1==wb2 ? 0 : wb1>wb2 ? -1 : 1;
         }
      });
   }
   
   /** Retourne l'ordre d'apparition des noms de plans pour chaque CHOICE
    *  En fonction de la longueur d'onde si elle est connue
    */
   synchronized protected int [] getChoiceOrder() {
      int n=getNb();
      sortPlan();
      int def[]=new int[choicePlanSort.length];
      
      
//for( int i=0; i<choicePlanSort.length; i++ ) System.out.println("["+i+"] => "+choicePlanSort[i]);
      for( int i=0; i<choicePlanSort.length; i++ ) {
         def[i]=Util.indexInArrayOf(choicePlanSort[i], choicePlan)+1;
      }
//for( int i=0; i<def.length; i++ ) System.out.println("choix "+(i+1)+" => "+def[i]);
      
      choicePlanSort=null;
      return def;
   }

   public void show() {
	   cR = ch[0];
	   cG = ch[1];
	   cB = ch[2];
//	   cL = ch[3];
	   super.show();
   }
   
   protected void submit() {
      // Determination de chaque plan et du plan de reference
      PlanImage r=(PlanImage)getPlan(ch[0]), g=(PlanImage)getPlan(ch[1]), b=(PlanImage)getPlan(ch[2]), ref/*=getPlan(ch[3])*/;
      String s=cbg.getSelection().getActionCommand();
      /* if( ref==null ) */ ref=s.equals(R)?r:s.equals(B)?b:s.equals(G)?g:null;

      // Doit-on appliquer une difference entre 2 plans
      boolean diff=cbDiff.isEnabled() && cbDiff.isSelected();

      if( r!=null && g!=null && b!=null && ref==null ) {
         a.console.printCommand("RGB "+Tok.quote(r.label)+" "+Tok.quote(g.label)+" "+Tok.quote(b.label));
      }
      a.calque.newPlanImageRGB(r,g,b,ref,null,diff);
      hide();
    }
}

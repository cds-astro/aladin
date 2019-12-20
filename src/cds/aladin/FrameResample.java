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
import java.util.*;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
* Gestion de la fenetre associee au resampling d'un plan
* @author Pierre Fernique [CDS]
* @version 1.0 : dec 2004 - Creation
*/
public final class FrameResample extends FrameRGBBlink {
   
   
   private PlanImage pref;
   private ButtonGroup cbPix;     // Pour repérer le mode pixel
   private ButtonGroup cbMethod;  // Pour repérer le mode du resampling
   private JRadioButton cbPPV,cbBil,cb8,cbF;
   private JCheckBox cbKeep;
   
   String TITLE,INFO,HELP1,PIXF,PIX8,METHOD_PPV,METHOD_BIL,KEEP_IMGS,KEEP_IMG,METHOD;
   
   protected void createChaine() {
      super.createChaine();
      TITLE     = a.chaine.getString("RSPTITLE");
      INFO      = a.chaine.getString("RSPINFO");
      HELP1     = a.chaine.getString("RSPHELP1");
      PIXF      = a.chaine.getString("RSPPIXF");
      PIX8      = a.chaine.getString("RSPPIX8");
      METHOD_PPV= a.chaine.getString("RSPPPV");
      METHOD_BIL= a.chaine.getString("RSPBIL");
      KEEP_IMGS = a.chaine.getString("RSPKEEPIMGS");
      KEEP_IMG  = a.chaine.getString("RSPKEEPIMG");
      METHOD    = a.chaine.getString("RSPMETHOD");
   }

   /** Creation du Frame gerant la creation d'un plan Resampl. */
   protected FrameResample(Aladin aladin) {      
      super(aladin);      
      Aladin.setIcon(this);

      if( aladin.calque.getNbSelectedPlans()>1 ) pref=null;
      else pref = (PlanImage)aladin.view.getCurrentView().pref;
      
      show();
      reset();
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

   protected int getToolNumber() {
//      return ToolBox.RESAMP;
      return -1;
   }

   protected int getNb() {
      return 1;
   }

   protected String getLabelSelector(int i) {
      return "";
   }

   protected Color getColorLabel(int i) {
      return Color.black;
   }
   
   /** Retourne le JPanel additionnel de la Frame. Il contient le sélecteur
    * du délais */
   protected JPanel getAddPanel() {
      
      // Choix des pixels
      JPanel p1=new JPanel();
      p1.add(new JLabel("Pixels:"));
      cbPix=new ButtonGroup();
      cb8=new JRadioButton(PIX8); cbPix.add(cb8); cb8.setSelected(true);
      cbF=new JRadioButton(PIXF); cbPix.add(cbF);
      p1.add(cbF);
      p1.add(cb8);
      
      // Choix de la méthode
      JPanel p2=new JPanel();
      p2.add(new JLabel(METHOD));
      cbMethod=new ButtonGroup();
      cbPPV=new JRadioButton(METHOD_PPV); cbMethod.add(cbPPV); cbPPV.setSelected(true);
      cbBil=new JRadioButton(METHOD_BIL); cbMethod.add(cbBil);
      p2.add(cbPPV);
      p2.add(cbBil);
      
      // Choix pour la conservation de l'image d'origine
      JPanel p3=new JPanel();
      cbKeep=new JCheckBox(pref==null?KEEP_IMGS:KEEP_IMG);
      cbKeep.setSelected(true);
      p3.add(cbKeep);

      JPanel p = new JPanel(new GridLayout(0,1));
      p.add(p1);
      p.add(p2);
      p.add(p3);
      return p;
   }
   
   protected void reset() {
      super.reset();
      
      // Si c'est déjà un plan Resamplé, je vais remettre les
      // choix que l'utilisateur avait fait précédemment
      if( pref!=null && pref.type==Plan.IMAGERSP ) {
//         cbPix.setCurrent(pref!=null && ((PlanImageResamp)pref).fullPixel?cbF:cb8);
         if( pref!=null && ((PlanImageResamp)pref).fullPixel ) cbF.setSelected(true);
         else cb8.setSelected(true);
         
//         cbMethod.setCurrent(pref!=null && ((PlanImageResamp)pref).methode
//               ==PlanImageResamp.PPV?cbPPV:cbBil);
         if( pref!=null && ((PlanImageResamp)pref).methode==PlanImageResamp.PPV ) cbPPV.setSelected(true);
         else cbBil.setSelected(true);
     } else {
          
         // Les défauts
//         cbPix.setCurrent(cb8);
//         cbMethod.setCurrent(cbBil);
        cb8.setSelected(true);
        cbBil.setSelected(true);
      }
      
      adjustWidgets();
   }

   protected void adjustWidgets() {
      // Invalidation du full pixel si on ne les a pas
      cbF.setEnabled(!( pref!=null && !pref.hasOriginalPixels() ));
      cbKeep.setText(pref==null?KEEP_IMGS:KEEP_IMG);
      cbKeep.setEnabled(!( pref!=null && pref.type==Plan.IMAGERSP )); 

   }

   protected void submit() {
      // Récupération du mode pixel demandé
//      boolean fullPixel=cbPix.getCurrent().getLabel().equals(PIXF);
      boolean fullPixel=cbF.isSelected();
      String sFull = fullPixel ? " Full":"";
      
      // Récupération de la méthode du resampling
//      int methode = cbMethod.getCurrent().getLabel().equals(METHOD_PPV)?
//            			PlanImageResamp.PPV:PlanImageResamp.BILINEAIRE;
      int methode = cbPPV.isSelected() ? PlanImageResamp.PPV : PlanImageResamp.BILINEAIRE;
      String sMethode = methode==PlanImageResamp.PPV ? " Closest":"";
      
      PlanImage p = (PlanImage)getPlan(ch[0]);
      
      int n=0;
      for( int i=0; i<a.calque.plan.length; i++ ) {
         if( !a.calque.plan[i].isSimpleImage() ) continue;
         PlanImage pref = (PlanImage)a.calque.plan[i];
         if( !pref.flagOk || !pref.selected ) continue;
         if( p!=null ) a.console.printCommand("resamp "+Tok.quote(pref.label)+" "
            +Tok.quote(p.label)+sFull+sMethode);
         a.calque.newPlanImageResamp(pref,p,null,methode,fullPixel,cbKeep.isSelected());
         n++;
      }
      if( sMethode.length()==0 ) sMethode = "Bilinear";
      if( sFull.length()==0 ) sMethode = "8bits";
      
      hide();
   }
}

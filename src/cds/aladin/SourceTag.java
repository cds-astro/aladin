// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.tools.Util;
import cds.xml.Field;

/**
 * Objet graphique correspondant a un tag sous forme de Source modifiable (avec des mesures associ�es)
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : juin 2016 - Mise en place de la hi�rarchie Source -> SourceTag -> SourcePhot
 * @version 1.0 : avril 2016 - cr�ation (d�placement d'un code qui se trouvait dans View)
 */
public class SourceTag extends Source  {
   
   static private int INDICE=0;                         // prochain indice pour les labels
   static protected Legende legende=createLegende();    // L�gende du SourceTag
   
   /** Cr�ation ou maj d'une l�gende associ�e � un SourceTag */
   static protected Legende createLegende() {
      if( legende!=null ) return legende;
      legende = Legende.adjustDefaultLegende(legende,Legende.NAME,     new String[]{  "_RAJ2000","_DEJ2000","Label",  "Plan", "RA (ICRS)","DE (ICRS)", "X",      "Y" });
      legende = Legende.adjustDefaultLegende(legende,Legende.DATATYPE, new String[]{  "double",  "double",  "char",   "char", "char",     "char",      "double", "double"});
      legende = Legende.adjustDefaultLegende(legende,Legende.UNIT,     new String[]{  "deg",     "deg",     "char",   "",     "\"h:m:s\"","\"h:m:s\"", "",       ""});
      legende = Legende.adjustDefaultLegende(legende,Legende.WIDTH,    new String[]{  "10",      "10",      "15",  "10",      "13",      "13",        "8",      "8"});
      legende = Legende.adjustDefaultLegende(legende,Legende.PRECISION,new String[]{  "6",       "6",       "",       "",     "2",        "3",        "2",      "2"});
      legende = Legende.adjustDefaultLegende(legende,Legende.DESCRIPTION,
            new String[]{  "RA","DEC", "Identifier",  "Reference plane", "Right ascension",  "Declination", "Current image X axis (FITS convention)", "Current image Y axis (Fits Convention)" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UCD,
            new String[]{  "pos.eq.ra;meta.main","pos.eq.dec;meta.main","meta.id;meta.main","","pos.eq.ra","pos.eq.dec","","" });
      legende.name="Positional tags";
      hideRADECLegende(legende);
      return legende;
   }
   
   // Cache les champs portants les coordonn�es
   static protected void hideRADECLegende(Legende legende) {
      int nra = legende.find("_RAJ2000");
      int nde = legende.find("_DEJ2000");
      if( nra<0 || nde<0 ) return;
      Field fra = legende.field[ nra ];
      Field fde = legende.field[ nde ];
      fra.type=fde.type="hidden";
      fra.visible=fde.visible=false;
   }
   
   /** Retourne le prochain indice pour les labels */
   static protected int nextIndice() { return ++INDICE; }
   
   /** Le plan de base dont sont issues les mesures */
   protected Plan planBase;
   
   /** Creation pour les backups */
   protected SourceTag(Plan plan) { super(plan); }

   /** Creation � partir d'une position x,y dans l'image
    * @param plan plan d'appartenance
    * @param v vue de r�f�rence qui d�terminera le PlanBase
    * @param x,y  position
    * @param id identificateur sp�cifique, ou null pour attribution automatique
    */
   protected SourceTag(Plan plan, ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,0,0,XY|RADE_COMPUTE,id);
      leg=legende;
      this.planBase = v.pref;
      plan.setSourceRemovable(true);
      suite();
   }

   /** Creation � partir d'une position c�leste
    * @param plan plan d'appartenance
    * @param v vue de r�f�rence qui d�terminera le PlanBase
    * @param c coordonn�es
    * @param id identificateur sp�cifique, ou null pour attribution automatique
    */
   protected SourceTag(Plan plan,ViewSimple v, Coord c,String id) {
      super(plan,c.al,c.del,id,null);
      this.planBase = v.pref;
      plan.setSourceRemovable(true);
      suite();
   }

   /** Post-traitement lors de la cr�ation */
   protected void suite() {
      leg=legende;
      setShape(Obj.PLUS);
      setTag(true);
      setWithLabel(true);
      resumeMesures();
   }

   /** (Re)gen�ration des mesures et r�affichage */
   protected void resume() {
      resumeMesures();
      plan.aladin.view.newView(1);
      plan.aladin.mesure.redisplay();
   }
   
   /** (Re)�n�ration de la ligne des infos (d�termine les mesures associ�es) */
   protected void resumeMesures() {
      Coord c = new Coord(raj,dej);
      if( id==null ) id = "Tag "+plan.pcat.getNextID();
      
      String x = " ";
      String y = " ";
      
      if( planBase instanceof PlanImage ) {
         planBase.projd.getXY(c);
         x = Util.myRound(""+(c.x+0.5),4);
         y = Util.myRound(""+( ((PlanImage)planBase).naxis2-c.y+0.5));
      }
      
      info = "<&_A Tags>\t"+raj+"\t"+dej+"\t"+id+"\t"+planBase.label+"\t"+c.getRA()+"\t"+c.getDE()+"\t"+x+"\t"+y;
   }
   
   /** Cet objet a des propri�t�s sp�cifiques */
   public boolean hasProp() { return true; }
   
   /** Retourne la liste des Propri�t�s �ditables */
   public Vector<Prop> getProp() {
      Vector<Prop> propList = new Vector<Prop>();
      JLabel l = new JLabel("\""+getObjType()+"\" in plane: \""+plan.getLabel()+"\"");
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      l.setFont(l.getFont().deriveFont(14f));
      propList.add( Prop.propFactory("object","",null,l,null,null) );
     
      // Edition du label
      final JTextField idL = new JTextField(id,20);
      PropAction updateId = new PropAction() {
         public int action() { 
            id = idL.getText();
            resume();
            return PropAction.SUCCESS;
         }
      };
      propList.add( Prop.propFactory("label","Label","Identifier",idL,null,updateId) );
      
      // Plan de r�f�rence
      l = new JLabel("\""+getObjType()+"\" object in plane: \""+plan.getLabel()+"\"");
      propList.add( Prop.propFactory("Plan","Plan","Reference plane",l,null,null) );
      
      // Edition de la position c�leste
      final Obj myself = this;
      final JTextField pos = new JTextField(20);
      PropAction updatePos = new PropAction() {
         public int action() {
            pos.setText( plan.aladin.localisation.getLocalisation(myself) );
            resume();
            return PropAction.SUCCESS;}
      };
      PropAction changePos = new PropAction() {
         public int action() { 
            pos.setForeground(Color.black);
            String opos = plan.aladin.localisation.getLocalisation(myself);
            try {
               String npos = pos.getText();
               if( npos.equals(opos) ) return PropAction.NOTHING;
               Coord c1 = new Coord(pos.getText());
               if( (""+c1).indexOf("--")>=0 ) throw new Exception();
               c1 = plan.aladin.localisation.frameToICRS(c1);
               setRaDec(c1.al, c1.del);
               resume();
               return PropAction.SUCCESS;
           } catch( Exception e1 ) { 
               pos.setForeground(Color.red);
               pos.setText(opos); 
           }
           return PropAction.FAILED;
         }
      };
      propList.add( Prop.propFactory("coord","Coord","Localisation",pos,updatePos,changePos) );
      
      // Visualisation du label dans la vue
      final JCheckBox labelCheck =  new JCheckBox("label display");
      labelCheck.setSelected(isWithLabel());
      final PropAction changeLabel = new PropAction() {
         public int action() {
            if( labelCheck.isSelected()==isWithLabel() ) return PropAction.NOTHING;
            setWithLabel( labelCheck.isSelected() );
            return PropAction.SUCCESS;
         }
      };
      labelCheck.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeLabel.action(); plan.aladin.view.repaintAll(); }
      });
      propList.add( Prop.propFactory("display","Display","Display the label in the view",labelCheck,null,changeLabel) );
      
      // Edition du tagging
      final JCheckBox tagCheck =  new JCheckBox("tagged (permanently selected)");
      tagCheck.setSelected(isTagged());
      final PropAction changeTagged = new PropAction() {
         public int action() {
            if( tagCheck.isSelected()==isTagged() ) return PropAction.NOTHING;
            setTag( tagCheck.isSelected() );
            return PropAction.SUCCESS;
         }
      };
      tagCheck.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeTagged.action(); plan.aladin.view.repaintAll(); }
      });
      propList.add( Prop.propFactory("tagged","Tagged","Check it to tag this object in the measurement panel",tagCheck,null,changeTagged) );

      return propList;
   }
   
//   /** Trac� effectif dans la vue indiqu�e */
//   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
//      if( !isVisible() ) return false;
//      int L = getL();
//      Point p = getViewCoord(v,L,L);
//      g.setColor( getColor() );
//      super.drawPlus(g, p);
//      if( isSelected() ) {
//         g.setColor( isTagged() ? Color.magenta : Color.green);
//         g.drawRect(p.x-L-1,p.y-L-1, L*2+2, L*2+2);
//      }
//      return true;
//   }
}


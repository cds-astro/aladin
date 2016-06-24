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

/**
 * Objet graphique correspondant a un tag sous forme de Source modifiable (avec des mesures associées)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : avril 2016 - création (déplacement d'un code qui se trouvait dans View)
 */
public class SourceTag extends Source  {
   
   static protected Legende legende=null;
   
   /** Création ou maj d'une légende associée à un SourceTag */
   static protected Legende createLegende() {
      if( legende!=null ) return legende;
      legende = Legende.adjustDefaultLegende(legende,Legende.NAME,     new String[]{  "Label",  "RA (ICRS)","DE (ICRS)", "X",      "Y" });
      legende = Legende.adjustDefaultLegende(legende,Legende.DATATYPE, new String[]{  "char","char",     "char",      "double", "double"});
      legende = Legende.adjustDefaultLegende(legende,Legende.UNIT,     new String[]{  "char","\"h:m:s\"","\"h:m:s\"", "",       ""});
      legende = Legende.adjustDefaultLegende(legende,Legende.WIDTH,    new String[]{  "15",   "13",      "13",        "8",      "8"});
      legende = Legende.adjustDefaultLegende(legende,Legende.PRECISION,new String[]{  "",     "2",        "3",        "2",      "2"});
      legende = Legende.adjustDefaultLegende(legende,Legende.DESCRIPTION,
            new String[]{  "Identifier",  "Right ascension",  "Declination", "Current image X axis (FITS convention)", "Current image Y axis (Fits Convention)" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UCD,
            new String[]{  "meta.id;meta.main","pos.eq.ra;meta.main","pos.eq.dec;meta.main","","" });
      return legende;
   }
   
   protected PlanImage planBase;
   
   public SourceTag(Plan plan, PlanImage planBase, double raj, double dej,String id) {
      super(plan,raj,dej,null,null);
      this.planBase = planBase;
      setInfo(raj,dej,id);
      suite();
   }
   
   protected void suite() {
      setShape(Obj.PLUS);
      setTag(true);
      setWithLabel(true);
      leg=createLegende();
   }
   
   protected void setInfo(double raj, double dej,String id) {
      Coord c = new Coord(raj,dej);
      if( id==null ) id = "Tag "+plan.pcat.getNextID();
      
      String x = " ";
      String y = " ";
      
      if( !(planBase instanceof PlanBG) ) {
         planBase.projd.getXY(c);
         x = Util.myRound(""+(c.x+0.5),4);
         y = Util.myRound(""+(planBase.naxis2-c.y+0.5));
      }
      
      this.info = "<&_A>\t"+id+"\t"+c.getRA()+"\t"+c.getDE() +"\t"+x+"\t"+y;
      this.id=id;
   }
   
   public boolean hasProp() { return false ; }
   
   public Vector<Prop> getProp() {
      Vector<Prop> propList = new Vector<Prop>();
      JLabel l = new JLabel("\""+getObjType()+"\" tag in plane: \""+plan.getLabel()+"\"");
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      l.setFont(l.getFont().deriveFont(14f));
      propList.add( Prop.propFactory("object","",null,l,null,null) );
     
      // Edition du label
      final JTextField idL = new JTextField(id,20);
      PropAction updateId = new PropAction() {
         public int action() { 
            id = idL.getText();
            reinitInfo();
            return PropAction.SUCCESS;
         }
      };
      propList.add( Prop.propFactory("label","Label","Tag identifier",idL,null,updateId) );
      
      // Edition de la position céleste
      final Obj myself = this;
      final JTextField pos = new JTextField(20);
      PropAction updatePos = new PropAction() {
         public int action() {
            pos.setText( plan.aladin.localisation.getLocalisation(myself) );
            reinitInfo();
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
               reinitInfo();
               return PropAction.SUCCESS;
           } catch( Exception e1 ) { 
               pos.setForeground(Color.red);
               pos.setText(opos); 
           }
           return PropAction.FAILED;
         }
      };
      propList.add( Prop.propFactory("coord","Coord","Tag localisation",pos,updatePos,changePos) );
      
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

   private void reinitInfo() {
      setInfo(raj,dej,id);
      plan.aladin.view.newView(1);
      plan.aladin.mesure.redisplay();
   }
}


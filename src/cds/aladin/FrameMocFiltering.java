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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;

/**
 * Gestion de la fenetre associeeau filtrage de sources par un MOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (oct 2012) Creation
 */
public final class FrameMocFiltering extends FrameRGBBlink {

   String TITLE,INFO,HELP1,CATPLANE,MOCPLANE,IN,OUT;

   // Les composantes de l'objet
   private ButtonGroup cbg;	         // Les checkBox des opérations possibles

   @Override
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCFILTERINGTITLE");
      INFO  = a.chaine.getString("MOCFILTERINGINFO");
      HELP1  = a.chaine.getString("MOCHELP");
      CATPLANE    = a.chaine.getString("MOCFILTERINGCAT");
      MOCPLANE    = a.chaine.getString("MOCFILTERINGMOC");
      IN  = a.chaine.getString("MOCFILTERINGIN");
      OUT= a.chaine.getString("MOCFILTERINGOUT");
   }

   /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameMocFiltering(Aladin aladin) {
      super(aladin);
      Aladin.setIcon(this);
   }

   @Override
   protected String getTitre() { return TITLE; }

   @Override
   protected String getInformation() { return INFO; }

   @Override
   protected String getHelp() { return HELP1; }

   @Override
   protected int getToolNumber() { return -2; }
   @Override
   protected int getNb() { return 8; }

   @Override
   protected String getLabelSelector(int i) {
      return i==0 ? MOCPLANE : CATPLANE;
   }

   /** Recupere la liste des plans images et catalogues valides */
   @Override
   protected Plan[] getPlan() {
      Vector<Plan> v  = a.calque.getPlans(Plan.ALLSKYMOC);
      Vector<Plan> v2  =a.calque.getPlans(Plan.CATALOG);
      if( v==null ) v=v2;
      else if( v2!=null ) v.addAll(v2);
      if( v==null ) return new Plan[0];
      Plan pi [] = new Plan[v.size()];
      v.copyInto(pi);
      return pi;
   }

   @Override
   protected Color getColorLabel(int i) {
      return Color.black;
   }
   
   @Override
   protected JPanel getAddPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH;

      JPanel p=new JPanel();
      p.setLayout(g);

      cbg=new ButtonGroup();

      JPanel pp=new JPanel();
      JRadioButton cb;
      cb=new JRadioButton(IN); cb.setActionCommand(IN);
      cbg.add(cb); pp.add(cb);  cb.setSelected(true);
      cb=new JRadioButton(OUT); cb.setActionCommand(OUT);
      cbg.add(cb); pp.add(cb);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);

      return p;
   }
   
   /** Création d'un plan catalogue contenant les sources d'une liste de plans catalogs, filtrées par un Moc */
   protected PlanCatalog createPlane(String label,PlanMoc pMoc, Plan [] p, boolean lookIn) throws Exception {
      Coord c = new Coord();
      Healpix hpx = new Healpix();
      HealpixMoc moc = pMoc.getMoc();
      Vector<Obj> v = new Vector<Obj>();
      
      for( int i=0; i<p.length; i++ ) {
         Plan pCat = p[i];
         if( pCat==null ) continue;
         Iterator<Obj> it = pCat.iterator();
         while( it.hasNext() ) {
            Obj o = it.next();
            if( !(o instanceof Source) ) continue;
            c.al=o.getRa();
            c.del=o.getDec();
            if( Double.isNaN(c.al) ||  Double.isNaN(c.del) ) continue;
            c=Localisation.frameToFrame(c,Localisation.ICRS, pMoc.frameOrigin);
            boolean in = moc.contains(hpx, c.al, c.del);

            if( lookIn==in ) v.add(o);
         }
      }

      return a.calque.newPlanCatalogBySources(v,label,false);
   }
   
   @Override
   protected void submit() {
      try {
         StringBuilder list=null;
         String s=cbg.getSelection().getActionCommand();
         boolean lookIn = s.equals(IN);

         Plan pMoc = getPlan(ch[0]);
         if( pMoc.type!=Plan.ALLSKYMOC ) throw new Exception("Not a MOC");
         
         // Détermination des plans concernés
         String label="";
         ArrayList<Plan> v = new ArrayList<Plan>();
         for( int i=1; i<ch.length; i++ ) {
            Plan pCat = getPlan(ch[i]);
            if( pCat==null ) continue;
            if( label.length()==0 ) label=pCat.label;
            else label=label+", "+pCat.label;
            v.add(pCat);
            
            // Liste des plans concernés
            if( list==null ) list = new StringBuilder( Tok.quote(pCat.label) );
            else list.append(" "+Tok.quote(pCat.label) );
         }
         Plan [] plans = new Plan[ v.size() ];
         v.toArray(plans);
         
         createPlane(label,(PlanMoc)pMoc,plans,lookIn);
         a.calque.repaintAll();
         hide();
         
         a.console.printCommand("ccat -uniq "+(!lookIn?"-out ":" ")+Tok.quote(pMoc.label)+(list.length()>0?" "+list.toString():""));

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning("Catalog filtering by MOC failed !");
      }
   }

   @Override
   protected void adjustWidgets() { };
}

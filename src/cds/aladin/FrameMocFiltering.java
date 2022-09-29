// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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
import cds.moc.Moc;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;

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
      Vector<Plan> v  = a.calque.getPlansMoc();
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
      long t0 = System.currentTimeMillis();
      Coord c = new Coord();
      Healpix hpx = new Healpix();
      Vector<Obj> v = new Vector<>(10000);
      
      Moc moc = pMoc.moc;
      int mode = moc instanceof STMoc ? 2 : moc instanceof TMoc ? 1 :0;
      
      SMoc spaceMoc = mode==0 ? (SMoc)moc : mode==2 ? ((STMoc)moc).getSpaceMoc() : null;
      TMoc timeMoc =   mode==1 ? (TMoc)moc  : mode==2 ? ((STMoc)moc).getTimeMoc()  : null;
      
      for( int i=0; i<p.length; i++ ) {
         Plan pCat = p[i];
         if( pCat==null ) continue;
         Iterator<Obj> it = pCat.iterator();
         while( it.hasNext() ) {
            Obj o = it.next();
//            if( !(o instanceof Source) ) continue;
            if( !o.asSource() ) continue;
            
            long npixSpace=-1;
            boolean in=false;
            
            // Test Space
            if( mode==0 || mode==2 ) {
               c.al=o.getRa();
               c.del=o.getDec();
               if( Double.isNaN(c.al) ||  Double.isNaN(c.del) ) continue;
               c=Localisation.frameToFrame(c,Localisation.ICRS, pMoc.frameOrigin);
               npixSpace = hpx.ang2pix(SMoc.MAXORD_S, c.al, c.del);

               in = spaceMoc.contains( npixSpace);
               if( in!=lookIn ) continue;
            }
            
            // Test time
            if( mode==1 || mode==2 ) {
               in = timeMoc.contains( ((Source)o).jdtime );
               if( in!=lookIn ) continue;
            }
            
            // Test Space & Time
            if( mode==2 ) {
               in = ((STMoc)moc).contains(npixSpace,((Source)o).jdtime);
            }
            
            if( lookIn==in ) v.add(o);
         }
            
      }
      
      long t1 = System.currentTimeMillis();
      System.out.println("Catalog filtered by MOC in "+(t1-t0)+"ms");

      return a.calque.newPlanCatalogBySources(v,label,false);
   }
   
   @Override
   protected void submit() {
      try {
         StringBuilder list=null;
         String s=cbg.getSelection().getActionCommand();
         boolean lookIn = s.equals(IN);

         Plan pMoc = getPlan(ch[0]);
         if( !pMoc.isMoc() ) throw new Exception("Not a MOC");
         
         // Détermination des plans concernés
         String label="";
         ArrayList<Plan> v = new ArrayList<>();
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
         Aladin.error("Catalog filtering by MOC failed !");
      }
   }

   @Override
   protected void adjustWidgets() { };
}

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

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Le formulaire d'interrogation de l'arbre des Allskys
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : juillet 2010
 */
public class ServerAllsky extends ServerTree  {
   private boolean populated=false;
   private JRadioButton fitsRadio;

   /** Initialisation des variables propres */
   @Override
   protected void init() {
      type        = APPLI;
      aladinLabel = "Allsky";
      aladinLogo  = "Allsky.gif";
   }
   
   // boutons radio pour choix JPEG/FITS
   protected int addTailPanel(int y) {
      int h=25;
      if( Aladin.OUTREACH ) return y;
      JPanel formatPanel = new JPanel();
      JRadioButton b;
      ButtonGroup group = new ButtonGroup();
      JLabel l = new JLabel(aladin.chaine.getString("ALADINDEFFMT"));
      formatPanel.add(l);
      b = new JRadioButton(aladin.chaine.getString("ALLSKYJPEG"));
      b.setBackground(Aladin.BLUE);
      b.setSelected(true);
      group.add(b);
      formatPanel.add(b);
      fitsRadio = b = new JRadioButton(aladin.chaine.getString("ALLSKYFITS"));
      b.setBackground(Aladin.BLUE);
      group.add(b);
      formatPanel.add(b);
      
      formatPanel.setBackground(Aladin.BLUE);
      formatPanel.setBounds(0,y,XWIDTH,h); y+=h;

      add(formatPanel);
      return y;
   }
   
   protected int makeTarget(int y) {
      JPanel tPanel = new JPanel();
      int h = makeTargetPanel(tPanel,0);
      tPanel.setBackground(Aladin.BLUE);
      tPanel.setBounds(0,y,XWIDTH,h); y+=h;
      add(tPanel);
      return y;
   }

   @Override
   protected void createChaine() {
      super.createChaine();
      title = aladin.chaine.getString("ALLSKYTITLE");
      info = aladin.chaine.getString("ALLSKYINFO");
      info1 = null;
      description = aladin.chaine.getString("ALLSKYDESC");
   }

   /** Creation du formulaire d'interrogation par arbre. */
   protected ServerAllsky(Aladin aladin) { super(aladin); }

   @Override
   protected int createPlane(String target,String radius,String criteria, 
         String label, String origin) {
      String survey;
      int defaultMode=PlanBG.UNKNOWN;
      
      if( criteria==null || criteria.trim().length()==0 ) survey="DSS colored";
      else {
         Tok tok = new Tok(criteria,", ");
         survey = tok.nextToken();
         
         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equalsIgnoreCase("Fits") ) defaultMode=PlanBG.FITS;
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") ) defaultMode=PlanBG.JPEG;
         }
      }
      
      int j = aladin.glu.findGluSky(survey,2);
      if( j<0 ) {
         Aladin.warning(this,"Healpix allsky unknown ["+survey+"]",1);
         return -1;
      }

      TreeNodeAllsky gSky = aladin.glu.getGluSky(j);
      try { gSky.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.allsky(gSky,label,target,radius);
      
//      return j;
      
   }

   @Override
   protected boolean is(String s) { return s.equalsIgnoreCase(aladinLabel); }

   @Override
   protected void initTree() { 
      if( populated ) return;
      populated=true;
      tree.freeTree();
      tree.populateTree( aladin.glu.vGluSky.elements() );
   }
   
   public void submit() {
      String mode = fitsRadio!=null && fitsRadio.isSelected() ? ",fits":"";
      for( TreeNode n : tree ) {
         if( !(n instanceof TreeNodeAllsky) ) continue;
         if( !n.isCheckBoxSelected() ) continue;
         TreeNodeAllsky ta = (TreeNodeAllsky) n;
         String target = getTarget(false);
         String radius = getRadius(false);
         String cible = target==null || target.trim().length()==0 ? "" : (" "+target+( radius==null ? "" : " "+radius));
         String criteria = ta.id+mode;
         String code = "get allsky("+Tok.quote(ta.id)+mode+")";
         aladin.console.setCommand(code+cible);
         int m=createPlane(target,radius,criteria,ta.aladinLabel,ta.copyright);
         if( m!=-1 ) aladin.calque.getPlan(m).setBookmarkCode(code+" $TARGET $RADIUS");
      }
      reset();

      
//      int mode = fitsRadio!=null && fitsRadio.isSelected() ? PlanBG.FITS : PlanBG.JPEG;
//      for( TreeNode n : tree ) {
//         if( !(n instanceof TreeNodeAllsky) ) continue;
//         ((TreeNodeAllsky)n).setDefaultMode( mode );
//      }
//      super.submit();
   }
   
//   public void submit(TreeNode n) {
//      TreeNodeAllsky gsky = (TreeNodeAllsky)n;
//      gsky.setDefaultMode( fitsRadio.isSelected() ? PlanBG.FITS : PlanBG.JPEG);
//      
//      aladin.calque.newPlanBG(gsky, null, getTarget(false), getRadius(false) );
//   }


}

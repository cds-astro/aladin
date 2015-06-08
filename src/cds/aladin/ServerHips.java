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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.tree.DefaultMutableTreeNode;

import cds.tools.Util;

/**
 * Le formulaire d'interrogation de l'arbre des Allskys
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : juillet 2010
 */
public class ServerHips extends ServerTree  {
   private boolean populated=false;
   private JRadioButton fitsRadio;

   /** Initialisation des variables propres */
   @Override
   protected void init() {
      type        = APPLI;
      aladinLabel = "hips";
      aladinLogo  = "Hips.png";
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
      //      info = aladin.chaine.getString("ALLSKYINFO");
      info=null;
      info1 = null;
      description = aladin.chaine.getString("ALLSKYDESC");
   }

   /** Creation du formulaire d'interrogation par arbre. */
   protected ServerHips(Aladin aladin) { super(aladin); }

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
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") || s.equalsIgnoreCase("png") ) defaultMode=PlanBG.JPEG;
         }
      }

      int j = aladin.glu.findGluSky(survey,2);
      if( j<0 ) {
         Aladin.warning(this,"Progressive survey (HiPS) unknown ["+survey+"]",1);
         return -1;
      }

      TreeNodeAllsky gSky = aladin.glu.getGluSky(j);

      try {
         if( defaultMode!=PlanBG.UNKNOWN ) gSky.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.hips(gSky,label,target,radius);

      //      return j;

   }


   // Dernier champs interrogé sur le MocServer
   private Coord oc=null;
   private double osize=-1;

   /** Interroge le MocServer pour connaître les HiPS disponibles dans le champ.
    * Met à jour l'arbre en conséquence
    */
   protected void hipsUpdate() {
      try {
         BufferedReader in=null;
         try {

            ViewSimple v = aladin.view.getCurrentView();
            if( v.isFree() ) return;
            Coord c = v.getCooCentre();
            double size = v.getTaille();
            if( c.equals(oc) && size==osize ) return;
            oc=c;
            osize=size;

            String params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&RA="+c.al+"&DEC="+c.del+"&SR="+size*Math.sqrt(2);
            URL u = aladin.glu.getURL("MocServer", params, true);
            Aladin.trace(4,"ServerHips.hipsUpdate: Contacting MocServer : "+u);
            in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
            String s;

            // récupération de chaque IVORN concernée (1 par ligne)
            HashSet<String> set = new HashSet<String>();
            while( (s=in.readLine())!=null ) set.add( getId(s) );

            // Nettoyage préalable de l'arbre
            for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) gSky.ok=false;

            // Positionnement des datasets dans le champ
            for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) {
               gSky.ok = set.contains(gSky.internalId);
               //               if( !gSky.ok ) System.out.println(gSky.internalId+" is out");
            }

            DefaultMutableTreeNode root = tree.getRoot();
            tree.setOkTree(root);
            validate();
            repaint();

         } finally{ if( in!=null ) in.close(); }
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

   }

   // Extraction du suffixe de l'IVORN pour rester compatible avec la nomenclature interne de l'arbre (TreeNodeAllsky.internalId)
   private String getId(String ivorn) {
      if( ivorn.length()<6 ) return "";
      int offset = ivorn.indexOf("/",6);
      String id = ivorn.substring(offset+1);
      return id;
   }

   @Override
   protected boolean is(String s) { return s.equalsIgnoreCase(aladinLabel); }

   public void submit() {
      String mode = fitsRadio!=null && fitsRadio.isSelected() ? ",fits":"";
      for( TreeNode n : tree ) {
         if( !(n instanceof TreeNodeAllsky) ) continue;
         if( !n.isCheckBoxSelected() ) continue;
         TreeNodeAllsky ta = (TreeNodeAllsky) n;
         String target = getTarget(false);
         String radius = getRadius(false);
         String cible = target==null || target.trim().length()==0 ? "" : (" "+target+( radius==null ? "" : " "+radius));
         String id = ta.internalId!=null ? ta.internalId : ta.id;
         String criteria = id+mode;
         String code = "get hips("+Tok.quote(id)+mode+")";
         aladin.console.printCommand(code+cible);
         int m=createPlane(target,radius,criteria,ta.aladinLabel,ta.copyright);
         if( m!=-1 ) aladin.calque.getPlan(m).setBookmarkCode(code+" $TARGET $RADIUS");
      }
      reset();


   }

   //   @Override
   //   protected void initTree() {
   //      if( populated ) return;
   //      populated=true;
   //      tree.freeTree();
   //      tree.populateTree( aladin.glu.vGluSky.elements() );
   //   }


   private boolean dynTree=false;
   protected void initTree() {
      if( dynTree ) return;
      (new Thread("initTree") {
         public void run() {
            loadRemoteTree();
            tree.populateTree(aladin.glu.vGluSky.elements());
         }
      }).start();
   }

   /** Chargement des descriptions de l'arbre */
   protected void loadRemoteTree() {
      if( dynTree ) return;
      DataInputStream dis=null;
      try {
         dynTree=true;
         Aladin.trace(3,"Loading Tree definitions...");
         String params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&fmt=glu&get=record";
         String u = aladin.glu.getURL("MocServer", params, true).toString();
         dis = new DataInputStream(aladin.cache.getWithBackup(u));
         aladin.glu.loadGluDic(dis,0,false,true,false,false);

      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }
      finally { if( dis!=null ) { try { dis.close(); } catch( Exception e) {} }
      }
   }
}

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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cds.aladin.MyTree.NoeudEditor;
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

      int j = aladin.glu.findHips(survey,2);
      if( j<0 ) {
         Aladin.warning(this,"Progressive survey (HiPS) unknown ["+survey+"]",1);
         return -1;
      }

      TreeObjHips gSky = aladin.glu.getHips(j);

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
//            if( v.isFree() ) return;

            if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
               for( TreeObjHips gSky : aladin.glu.vHips ) gSky.isIn=0;

            } else {
               String params;

               // Pour éviter de faire 2x la même chose
               Coord c = v.getCooCentre();
               double size = v.getTaille();
               if( c.equals(oc) && size==osize ) return;
               oc=c;
               osize=size;

               // Interrogation par cercle
               if( v.getTaille()>45 ) {
                  params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&RA="+c.al+"&DEC="+c.del+"&SR="+size*Math.sqrt(2);

                  // Interrogation par rectangle
               } else {
                  StringBuilder s1 = new StringBuilder("Polygon");
                  for( Coord c1: v.getCooCorners())  s1.append(" "+c1.al+" "+c1.del);
                  params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&stc="+URLEncoder.encode(s1.toString());
               }

               URL u = aladin.glu.getURL("MocServer", params, true);
               
               Aladin.trace(4,"ServerHips.hipsUpdate: Contacting MocServer : "+u);
               in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
               String s;

               // récupération de chaque IVORN concernée (1 par ligne)
               HashSet<String> set = new HashSet<String>();
               while( (s=in.readLine())!=null ) set.add( getId(s) );

               // Nettoyage préalable de l'arbre
               for( TreeObjHips gSky : aladin.glu.vHips ) gSky.isIn=0;

               // Positionnement des datasets dans le champ
               for( TreeObjHips gSky : aladin.glu.vHips ) {
                  gSky.isIn = set.contains(gSky.internalId) ? 1 : 0;
//                  if( !gSky.ok ) System.out.println(gSky.internalId+" is out");
               }
            }

            // Mise à jour de la cellule de l'arbre en cours d'édition
            try {
               NoeudEditor c = (NoeudEditor)tree.getCellEditor();
               if( c!=null ) {
                  TreeObj n = (TreeObj)c.getCellEditorValue();
                  if( n!=null &&  n.hasCheckBox() ) {
                     if( n.getIsIn()==1 ) n.checkbox.setForeground(Color.black);
                     else n.checkbox.setForeground(Color.lightGray);
                  }
               }
            } catch( Exception e ) {
               if( Aladin.levelTrace>=3 ) e.printStackTrace();
            }

            // Mise à jour des branches de l'arbre
            tree.populateFlagIn();
            validate();
            repaint();

         } finally{ if( in!=null ) in.close(); }
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

   }

   // Extraction de l'obs_id de l'IVOID pour rester compatible avec la nomenclature interne de l'arbre (TreeNodeAllsky.internalId)
   private String getId(String ivoid) {
      int start = ivoid.startsWith("ivo://") ? 6 : 0;
      int offset = ivoid.indexOf("/",start);
      int offset1 = ivoid.indexOf("?",start);
      if( offset1>0 ) offset = Math.min(offset,offset1);
      String id = ivoid.substring(offset+1);
      return id;
   }

   @Override
   protected boolean is(String s) { return s.equalsIgnoreCase(aladinLabel); }

   public void submit() {
      String mode = fitsRadio!=null && fitsRadio.isSelected() ? ",fits":"";
      for( TreeObj n : tree ) {
         if( !(n instanceof TreeObjHips) ) continue;
         if( !n.isCheckBoxSelected() ) continue;
         TreeObjHips ta = (TreeObjHips) n;
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


   private boolean dynTree=false;
   protected void initTree() {
      if( dynTree ) return;
      (new Thread("initTree") {
         public void run() {
            loadRemoteTree();
            tree.populateTree(aladin.glu.vHips.elements());
            aladin.hipsReload();
         }
      }).start();
   }

   /** Chargement des descriptions de l'arbre */
   protected void loadRemoteTree() {
      if( dynTree ) return;
      DataInputStream dis=null;

      // Recherche sur le site principal, et sinon dans le cache local
      try {
         dynTree=true;
         Aladin.trace(3,"Loading HiPS Tree definitions...");
         String params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&fmt=glu&get=record";
         
         String u = aladin.glu.getURL("MocServer", params, true).toString();
         InputStream in;
         try {
            in = aladin.cache.getWithBackup(u);

            // Peut être un site esclave actif ?
         } catch( Exception e) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            in = Util.openStream(u);
         }
         dis = new DataInputStream(in);
         aladin.glu.loadGluDic(dis,0,false,true,false,false);
         aladin.glu.tri();

      }
      catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }
      finally { if( dis!=null ) { try { dis.close(); } catch( Exception e) {} }
      }
   }
}

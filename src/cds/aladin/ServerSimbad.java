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

import java.awt.Dimension;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.aladin.prop.Filet;

/**
 * Le formulaire d'interrogation de Simbad
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class ServerSimbad extends Server  {
   	
   String info1,/* info2,*/filter;
   protected double maxRadius=60; // Rayon maximal autorisé par défaut
  
   // retourne le tagGLU à utiliser en fonction du choix du serveur test ou non
   private String getTagGlu(boolean testServer) {
      if( TESTSERVER && !testServer ) return gluTag.substring(0,gluTag.length()-1);
      return gluTag;
   }
   
  /** Initialisation des variables propres a Simbad */
   protected void init() {
      type    = CATALOG;
      aladinLabel     = "Simbad database";
      gluTag  = "SimbadXML1";
      aladinLogo    = "SimbadLogo.gif";
      docUser = "http://simbad.u-strasbg.fr/guide/ch15.htx";
//      maxRadius=30*60;
      maxRadius=-1;

      // Juste pour revenir au serveur Simbad normal si on n'a pas 
      // la surcharge GLU pour le nouveau serveur
      if( !aladin.CDS || aladin.glu.getURL(gluTag,"",false,false)==null ) {
         gluTag = gluTag.substring(0,gluTag.length()-1);
      } else TESTSERVER=true;
      
      filters = 
//         !Aladin.OUTREACH ? new String[] {
//            "#Simbad filter\nfilter Smb.filter {\n" +
//            "(${OTYPE}=\"Star\" || ${OTYPE}=\"*\\**\") && (${B}!=0 || ${V}!=0 || ${R}!=0 || ${J}!=0 || ${K}!=0) {\n" +
//            "   draw pm(${PMRA},${PMDEC})\n" +
//            "   draw circle(-$[phot.mag*],3,15)\n" +
//            "}\n" +
//            "(${OTYPE}=\"Star\" ||${OTYPE}=\"*\\**\") && ${B}=\"\" && ${V}=\"\" && ${R}=\"\" && ${J}=\"\" && ${K}=\"\" {\n" +
//            "   draw pm(${PMRA},${PMDEC})\n" +
//            "   draw circle(3)\n" +
//            "}\n" +
//            "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\") && ${GALDIM_MAJAXIS}!=0 {\n" +
//            "   draw ellipse(0.5*${GALDIM_MAJAXIS},0.5*${GALDIM_MINAXIS},${GALDIM_ANGLE})\n" +
//            "}\n" +
//            "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\")) && ${GALDIM_MAJAXIS}==\"\" {\n" +
//            "   draw oval\n" +
//            "}\n" +
//            "${OTYPE}=\"Radio*\" || ${OTYPE}=\"Maser\" || ${OTYPE}=\"HI\" { draw triangle }\n" +
//            "${OTYPE}=\"UV\" {draw cross;draw plus}\n" +
//            "${OTYPE}=\"IR\" || ${OTYPE}=\"Red*\" {draw rhomb}\n" +
//            "${OTYPE}=\"Neb\"  || ${OTYPE}=\"PN*\" || ${OTYPE}=\"SNR*\" {draw square}\n" +
//            "${OTYPE}=\"HII\" { draw dot }" +
//            "${OTYPE}=\"X\" { draw cross }\n" +
//            "${OTYPE}!=\"Unknown\" { draw ${OTYPE} }\n" +
//            "{ draw dot }\n" +
//            "}",
//            "#Simbad filter (colorized)\nfilter Smb.filterC {" +
//            "(${OTYPE}=\"Star\" || ${OTYPE}=\"*\\**\") && (${B}!=0 || ${V}!=0 || ${R}!=0 || ${J}!=0 || ${K}!=0) {\n" +
//            "   draw red pm(${PMRA},${PMDEC})\n" +
//            "   draw red circle(-$[phot.mag*],3,15)\n" +
//            "}\n" +
//            "(${OTYPE}=\"Star\" ||${OTYPE}=\"*\\**\") && ${B}=\"\" && ${V}=\"\" && ${R}=\"\" && ${J}=\"\" && ${K}=\"\" {\n" +
//            "   draw red pm(${PMRA},${PMDEC})\n" +
//            "   draw red circle(3)\n" +
//            "}\n" +
//            "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\") && ${GALDIM_MAJAXIS}!=0 {\n" +
//            "   draw blue ellipse(0.5*${GALDIM_MAJAXIS},0.5*${GALDIM_MINAXIS},${GALDIM_ANGLE})\n" +
//            "}\n" +
//            "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\")) && ${GALDIM_MAJAXIS}==\"\" {\n" +
//            "   draw blue oval\n" +
//            "}\n" +
//            "${OTYPE}=\"Radio*\" || ${OTYPE}=\"HI\" || ${OTYPE}=\"Maser\" { draw green triangle }\n" +
//            "${OTYPE}=\"UV\" {draw magenta cross;draw magenta plus}\n" +
//            "${OTYPE}=\"IR\" || ${OTYPE}=\"Red*\" {draw red rhomb}\n" +
//            "${OTYPE}=\"Neb\"  || ${OTYPE}=\"PN*\" || ${OTYPE}=\"SNR*\" {draw red square}\n" +
//            "${OTYPE}=\"HII\" { draw red dot }" +
//            "${OTYPE}=\"X\" { draw black cross }\n" +
//            "${OTYPE}!=\"Unknown\" { draw #9900CC ${OTYPE} }\n" +
//            "{ draw #9900CC dot }\n" +
//            "}",
//            "#object class\nfilter Class { draw ${OTYPE} }",
//            "#position uncertainty\nfilter Uncertainty {" +
//            "draw;" +
//            "draw ellipse(0.5*${COO_ERR_MAJA},0.5*${COO_ERR_MINA},${COO_ERR_ANGLE})" +
//            "}",
//            "#object size\nfilter Size {" +
//            "draw;" +
//            "draw magenta ellipse(0.5*${GALDIM_MAJAXIS},0.5*${GALDIM_MINAXIS},${GALDIM_ANGLE})" +
//            "}",
//            "#proper motion\nfilter PM { draw;draw magenta pm(${PMRA},${PMDEC}) }",
////            "#B-V rainbow effect\nfilter BVrainbow { ${Bmag}!=\"\" && ${Vmag}!=\"\" { draw square rainbow(${Bmag}-${Vmag},-0.3,1) }}",
//      } :
         new String[] {
                  "#All objects\nfilter All {\n" +
                  "(${OTYPE}=\"Star\" || ${OTYPE}=\"*\\**\") && (${B}!=0 || ${V}!=0 || ${R}!=0 || ${J}!=0 || ${K}!=0) {\n" +
                  "   draw pm(${PMRA},${PMDEC})\n" +
                  "   draw circle(-$[phot.mag*],3,15)\n" +
                  "}\n" +
                  "(${OTYPE}=\"Star\" ||${OTYPE}=\"*\\**\") && ${B}=\"\" && ${V}=\"\" && ${R}=\"\" && ${J}=\"\" && ${K}=\"\" {\n" +
                  "   draw pm(${PMRA},${PMDEC})\n" +
                  "   draw circle(3)\n" +
                  "}\n" +
                  "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\") && ${GALDIM_MAJAXIS}!=0 {\n" +
                  "   draw ellipse(0.5*${GALDIM_MAJAXIS},0.5*${GALDIM_MINAXIS},${GALDIM_ANGLE})\n" +
                  "}\n" +
                  "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\")) && ${GALDIM_MAJAXIS}==\"\" {\n" +
                  "   draw oval\n" +
                  "}\n" +
                  "${OTYPE}=\"Radio*\" || ${OTYPE}=\"Maser\" || ${OTYPE}=\"HI\" { draw triangle }\n" +
                  "${OTYPE}=\"UV\" {draw cross;draw plus}\n" +
                  "${OTYPE}=\"IR\" || ${OTYPE}=\"Red*\" {draw rhomb}\n" +
                  "${OTYPE}=\"Neb\"  || ${OTYPE}=\"PN*\" || ${OTYPE}=\"SNR*\" {draw square}\n" +
                  "${OTYPE}=\"HII\" { draw dot }" +
                  "${OTYPE}=\"X\" { draw cross }\n" +
                  "${OTYPE}!=\"Unknown\" { draw ${OTYPE} }\n" +
                  "{ draw dot }\n" +
                  "}",
                  "#Star\nfilter Star {\n" +
                  "(${OTYPE}=\"Star\" || ${OTYPE}=\"*\\**\") && (${B}!=0 || ${V}!=0 || ${R}!=0 || ${J}!=0 || ${K}!=0) {\n" +
                  "   draw pm(${PMRA},${PMDEC})\n" +
                  "   draw circle(-$[phot.mag*],3,15)\n" +
                  "}\n" +
                  "(${OTYPE}=\"Star\" ||${OTYPE}=\"*\\**\") && ${B}=\"\" && ${V}=\"\" && ${R}=\"\" && ${J}=\"\" && ${K}=\"\" {\n" +
                  "   draw pm(${PMRA},${PMDEC})\n" +
                  "   draw circle(3)\n" +
                  "}\n" +
                  "}",
                  "#Galaxy\nfilter Galaxy {\n" +
                  "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\") && ${GALDIM_MAJAXIS}!=0 {\n" +
                  "   draw ellipse(0.5*${GALDIM_MAJAXIS},0.5*${GALDIM_MINAXIS},${GALDIM_ANGLE})\n" +
                  "}\n" +
                  "(${OTYPE}=\"G*\" || ${OTYPE}=\"*G\")) && ${GALDIM_MAJAXIS}==\"\" {\n" +
                  "   draw oval\n" +
                  "}\n" +
                  "}",
                  "#Radio\nfilter Radio {\n" +
                  "${OTYPE}=\"Radio*\" || ${OTYPE}=\"Maser\" || ${OTYPE}=\"HI\" { draw triangle }\n" +
                  "}",
                  "#UV\nfilter UV {\n" +
                  "${OTYPE}=\"UV\" {draw cross;draw plus}\n" +
                  "}",
                  "#IR\nfilter IR {\n" +
                  "${OTYPE}=\"IR\" || ${OTYPE}=\"Red*\" {draw rhomb}\n" +
                  "}",
                  "#Neb\nfilter Neb {\n" +
                  "${OTYPE}=\"Neb\"  || ${OTYPE}=\"PN*\" || ${OTYPE}=\"SNR*\" {draw square}\n" +
                  "}",
                  "#HII\nfilter HII {\n" +
                  "${OTYPE}=\"HII\" { draw dot }" +
                  "}",
                  "#X\nfilter X {\n" +
                  "${OTYPE}=\"X\" { draw cross }\n" +
                  "}",
                
      };
   }
   
   protected void createChaine() {
      super.createChaine();
      description  = aladin.chaine.getString("SMBINFO");
      institute  = aladin.chaine.getString("SMBFROM");
      title = aladin.chaine.getString("SMBTITLE");
      verboseDescr  = aladin.chaine.getString("SMBDESC");
      info1 = aladin.chaine.getString("SMBINFO1");
//      info2 = aladin.chaine.getString("SMBINFO2");
      filter= aladin.chaine.getString("SMBFILTER");
   }

 /** Creation du formulaire d'interrogation de Simbad.
   * @param aladin reference
   * @see aladin.ServerSimbad#createGluSky()
   */
   protected ServerSimbad(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
      init();

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=/* Aladin.OUTREACH ? YOUTREACH : */ 50;
      int X=150;

      // Le titre
      JPanel tp = new JPanel();
      Dimension d = makeTitle(tp,title);
//      if( TESTSERVER ) testServer.setText("(direct access)");
      if( TESTSERVER ) {
         testServer.setText("(live Simbad)");
         d.width+=80;
         testServer.setSelected(false);
      }
      tp.setBackground(Aladin.BLUE);
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);
      
      // Premiere indication
      JLabel l = new JLabel(info1);
      l.setBounds(90,y,400, 20); y+=20;
      add(l);
//      l = new JLabel(info2);
//      l.setBounds(138,y,300, 20); y+=20;
//      add(l);

      // JPanel pour la memorisation du target (+bouton DRAG)
      JPanel tPanel = new JPanel();
      tPanel.setBackground(Aladin.BLUE);
      int h = makeTargetPanel(tPanel,0);
      tPanel.setBounds(0,y,XWIDTH,h); y+=h;
      add(tPanel);

      modeCoo = COO|SIMBAD;
      modeRad = RADIUS;
      
      getGluFilters(gluTag);
      
      // Pas de filtre dédiée en mode Outreach      
//      if( Aladin.OUTREACH ) filters=null;
      
      if( filters!=null ) {
         
         y+=20;
         Filet f = new Filet();
         f.setBounds(40,y,XWIDTH-60,5); y+=20;
         add(f);
         
         filtersChoice = createFilterChoice();
         filtersChoice.setOpaque(false);
         y+=10;
         JLabel pFilter = new JLabel(addDot(filter));
         pFilter.setFont(Aladin.BOLD);
         pFilter.setBounds(XTAB1,y,XTAB2-10,HAUT);
         add(pFilter);
         filtersChoice.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
         add(filtersChoice);
      }
   }
   
   protected boolean isDiscovery() { return true; }
      
   protected MyInputStream getMetaData(String target, String radius,StringBuffer infoUrl) {
      try {
         URL u;
         double rm = getRM(radius);
         target = resolveTarget(target);
         String s = Glu.quote(target)+" "+Glu.quote(rm+"");
         if( (u=aladin.glu.getURL(getTagGlu(false),s))==null ) return null;
         infoUrl.append(u+"");
         return getMetaDataForCat(u);
      } catch( Exception e ) {
         e.printStackTrace();
      }
      return null;
   }

   
   protected int createPlane(String target,String radius,String criteria,
         String label, String origin) {
      URL u;
      defaultCursor();

      // On enlève l'éventuel unité
      double r=getRM(radius);
      if( maxRadius>0 && r>maxRadius ) {
         ball.setMode(Ball.HS);
         Aladin.error(this,WTOOLARGE+" (>"+Coord.getUnit(maxRadius/60.)+")",1);
         return -1;
      }
      radius = r+"";
      
      String s = Glu.quote(target)+" "+Glu.quote(radius);

      label = getDefaultLabelIfRequired(label,"CDS/Simbad");

      // S'agit-il d'un accès au Simbad Live ?
      boolean cdstest=false;
      if( criteria!=null && criteria.indexOf("live")>=0 || testServer!=null && testServer.isSelected() ) cdstest=true;
      
      if( (u=aladin.glu.getURL(getTagGlu(cdstest),s))==null ) {
         ball.setMode(Ball.HS);
         Aladin.error(this,WERROR,1);
         return -1;
      }
      if( !verif(Plan.CATALOG,target,label+" "+radius) ) return -1;
      aladin.targetHistory.add(target);
      return aladin.calque.newPlanCatalog(u,label,target,label+" "+radius,origin,this);
   }

  /** Interrogation de Simbad */
   public void submit() {
      String objet = getTarget(flagVerif);
      if( objet==null ) return;
      String r = getRadius();
      if( r==null ) return;
      double rm = getRM(r);

      
//      // Recuperation du filtre par defaut s'il y a lieu
//      int filterIndex = getFilterChoiceIndex();

      // Creation du plan
      waitCursor();
      String script = "get "+aladinLabel+" "+objet+" "+Coord.getUnit(rm/60.);
      aladin.console.printCommand(script);
      int n=createPlane(objet,rm+"",null,null,institute);
      if( n!=-1 ) aladin.calque.getPlan(n).setBookmarkCode("get "+aladinLabel+" $TARGET $RADIUS");
   }
}

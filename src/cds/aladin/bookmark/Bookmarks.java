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


package cds.aladin.bookmark;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;

import cds.aladin.Aladin;
import cds.aladin.Cache;
import cds.aladin.Command;
import cds.aladin.Function;
import cds.aladin.Glu;
import cds.aladin.MyInputStream;
import cds.aladin.Tok;

/**
 * Liste des bookmarks
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Mars 2011 - mise en place
 */
public class Bookmarks {
   private Aladin aladin;
   private FrameBookmarks frameBookmarks;     // Gère la fenêtre de consultation/édition des favoris
   private JToolBar toolBar;            // JToolbar des bookmarks sélectionnés

   private String memoDefaultList="";
   
   public Bookmarks(Aladin aladin) {
       this.aladin = aladin;
       frameBookmarks = null;
       toolBar = null;
   }
   
   /** Initialisation des bookmarks */
   public void init(boolean noCache) {
      createBookmarks(noCache);
      aladin.getCommand().setFunctionModif(false);
      if( aladin.hasGUI() ) resumeToolBar();
   }
   
   public FrameBookmarks getFrameBookmarks() {
      if( frameBookmarks==null ) frameBookmarks = new FrameBookmarks(aladin,this);
      return frameBookmarks;
   }
   
   /** Fournit la toolbar des signets (éventuellement la crée) */
   public JToolBar getToolBar() {
      if( toolBar==null ) {
         toolBar = new JToolBar();
         toolBar.setRollover(true);
         toolBar.setFloatable(false);
         toolBar.setBorder(BorderFactory.createEmptyBorder());
         populateToolBar(toolBar);
      }
      return toolBar;
   }
   
   /** Retourne un nom unique pour un nouveau signet */
   public String getUniqueName(String name) {
      int j = name.indexOf('(');
      if( j>0 ) name = name.substring(0,j);
      
      Vector<Function> v = aladin.getCommand().getBookmarkFunctions();
      for( int i=0; true; i++ ) {
         boolean trouve=false;
         String sn = name+ (i==0 ? "": "~"+i);
         Enumeration<Function> e = v.elements();
         while( e.hasMoreElements() ) {
            Function f = e.nextElement();
            if( sn.equals(f.getName()) ) { trouve=true; break; }
         }
         if( !trouve ) return sn;
      }
   }
   
   // Ajoute les boutons qu'il faut dans la toolbar
   private void populateToolBar(JToolBar toolBar) {
      Enumeration<Function> e = aladin.getCommand().getBookmarkFunctions().elements();
      while( e.hasMoreElements() ) {
         Function f = e.nextElement();
         ButtonBookmark bkm = new ButtonBookmark(aladin,f);
         toolBar.add(bkm);
      }
      
      if( !Aladin.OUTREACH ) {
         JButton plus = new JButton("  +  ");
         plus.setToolTipText(aladin.getChaine().getString("BKMEDITOR"));
         plus.setFont( plus.getFont().deriveFont(Font.BOLD));
         plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { editFrame(); }
         });
         toolBar.add(plus);
      }
   }
   
   /** Remet à jour la toolbar des signets suite à des modifs internes */
   public void resumeToolBar() {
      toolBar.removeAll();
      populateToolBar(toolBar);
      toolBar.validate();
      aladin.validate();
      aladin.repaint();
   }
   
   /** Affichage de la frame de consultation des signets */
   public void showFrame() { getFrameBookmarks().setVisible(true); }
   
   /** Affichage de la frame d'édition et de consultation des signets */
   public void editFrame() { getFrameBookmarks().setVisibleEdit(); }
   
   /** Retourne true si la liste courante des bookmarks correspond à la liste par défaut
    * définie par l'enregistrement GLU (voir glu.createBookmarks()) */
   public boolean isDefaultList() {
      return memoDefaultList.equals(getBookmarkList());
   }
   
   /** Mémorise la liste courante des bookmarks */
   public void memoDefaultList(String s) { memoDefaultList = s; }
   
   /** Retourne la liste des bookmarks */
   public String getBookmarkList() { return getBookmarkList(false); }
   public String getBookmarkList(boolean onlyLocal) {
      StringBuffer bkm = new StringBuffer();
      Enumeration<Function> e = aladin.getCommand().getBookmarkFunctions().elements();
      while( e.hasMoreElements() ) {
         Function f = e.nextElement();
         if( onlyLocal && !f.isLocalDefinition() ) continue;
         if( bkm.length()>0 ) bkm.append(',');
         bkm.append(f.getName());
      }
      return bkm.toString();
   }   
   
   private boolean remoteBookmarksLoaded=false; // true si les bookmarks distants ont été correctement chargés
   
   /** Retourne true si la liste des bookmarks peut être sauvegardée dans le fichier de configuration */
   public boolean canBeSaved() {
      return remoteBookmarksLoaded;
   }
   
   /** Mise à jour de la liste des bookmarks sélectionnés (les noms séparés par une simple virgule).
    * Si la liste commence par '+' ou '-', il s'agit d'une mise à jour */
   public void setBookmarkList(String names) {
      int mode = 0;
      if( names.length()>1 ) {
         if( names.charAt(0)=='+' ) { mode=1; names=names.substring(1); }
         else if( names.charAt(0)=='-' ) { mode=-1; names=names.substring(1); }
      }
      if( mode==0 ) { aladin.getCommand().resetBookmarks(); mode=1; }
      
      Tok tok = new Tok(names,",");
      while( tok.hasMoreTokens() ) {
         String name = tok.nextToken().trim();
         Function f = aladin.getCommand().getFunction(name);
         if( f==null ) continue;
         f.setBookmark(mode==1);
      }
      
      resumeToolBar();
   }

   
   private String defaultBookmarkListByGlu=null;     // Liste des bookmarks trouvés via l'enregistrement GLU dedié aux BookMarks (%Aladin.Bookmarks)
   private String gluTag=null;                       // Identificateur de l'enregistrement de bookmarks
   
   // Mémorise les infos pour générer les bookmarks (voir createBookmarks() */
   public void memoGluBookmarks(String actionName, String aladinBookmarks) {
      Aladin.trace(4,"Bookmarks.memoBookmarks() %A="+actionName+" %Aladin.Bookmarks="+aladinBookmarks); 
      gluTag=actionName;
      defaultBookmarkListByGlu=aladinBookmarks;
   }
   
   /** Création des bookmarks par défaut :
    * 1) Chargement à distance des définitions de fonctions scripts
    * 2) Assignation de certaines d'entre-elles en tant que bookmarks.
    */
   synchronized public void createBookmarks(boolean noCache) {
      Glu glu = aladin.getGlu();
      Cache cache = aladin.getCache();
      Command command = aladin.getCommand();
      
      if( gluTag!=null ) {
         try {
            aladin.trace(3,"Remote bookmarks loaded...");
            command.setFunctionLocalDefinition(false);  // Toutes ces fonctions sont considérées comme distantes
            if( noCache )  {
               String u = glu.getURL(gluTag,"",false,false)+"";
               cache.putInCache(u);
            }
            MyInputStream in =  new MyInputStream(cache.get(glu.getURL(gluTag,"",false,false)));
            aladin.getCommand().execScript(new String(in.readFully()),false,true);
            in.close();
            remoteBookmarksLoaded=true;
         } catch( Exception e ) {
            e.printStackTrace();
            System.err.println("Remote bookmarks error: "+e.getMessage());
         }
      }
      
      File f = new File(aladin.getConfiguration().getLocalBookmarksFileName());
      if( !aladin.isOutreach() && f.canRead() ) {
         try {
            aladin.trace(3,"Local bookmarks loaded ["+f.getCanonicalPath()+"]...");
            command.setFunctionLocalDefinition(true);  // Toutes ces fonctions sont considérées comme locales
            MyInputStream in = new MyInputStream(new FileInputStream(f));
            command.execScript(new String(in.readFully()),false,true);
            in.close();
         } catch( Exception e ) {
            e.printStackTrace();
            System.err.println("Local bookmarks error: "+e.getMessage());
         }
     }
      
      if( defaultBookmarkListByGlu!=null ) memoDefaultList(defaultBookmarkListByGlu);
      
      String t="Default ";
      String s = aladin.configuration.getBookmarks();
      if( s==null || s.trim().length()==0 ) s=defaultBookmarkListByGlu;
      else t="Local ";
      if( s!=null ) {
         aladin.trace(2,t+"bookmarks: "+s);
         setBookmarkList(s);
      }
      
      command.setFunctionLocalDefinition(true);  // Toutes les autres fonctions seront considérées comme locales
   }
   

}

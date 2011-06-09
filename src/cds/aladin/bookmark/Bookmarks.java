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

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;

import javax.swing.BorderFactory;
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
   private FrameBookmarks frameBookmarks;     // G�re la fen�tre de consultation/�dition des favoris
   private JToolBar toolBar;            // JToolbar des bookmarks s�lectionn�s

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
   
   /** Fournit la toolbar des signets (�ventuellement la cr�e) */
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
   
   // Ajoute les boutons qu'il faut dans la toolbar
   private void populateToolBar(JToolBar toolBar) {
      Enumeration<Function> e = aladin.getCommand().getBookmarkFunctions().elements();
      while( e.hasMoreElements() ) {
         Function f = e.nextElement();
         ButtonBookmark bkm = new ButtonBookmark(aladin,f);
         toolBar.add(bkm);
      }
   }
   
   /** Remet � jour la toolbar des signets suite � des modifs internes */
   public void resumeToolBar() {
      toolBar.removeAll();
      populateToolBar(toolBar);
      toolBar.validate();
      aladin.validate();
      aladin.repaint();
   }
   
   /** Affichage de la frame d'�dition et de consultation des signets */
   public void showFrame() { getFrameBookmarks().setVisible(true); }
   
   /** Retourne true si la liste courante des bookmarks correspond � la liste par d�faut
    * d�finie par l'enregistrement GLU (voir glu.createBookmarks()) */
   public boolean isDefaultList() {
      return memoDefaultList.equals(getBookmarkList());
   }
   
   /** M�morise la liste courante des bookmarks */
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
   
   /** Retourne true si la liste des bookmarks peut �tre sauvegard�e dans le fichier de configuration */
   public boolean canBeSaved() {
      return aladin.hasNetwork();
   }
   
   /** Mise � jour de la liste des bookmarks s�lectionn�s (les noms s�par�s par une simple virgule).
    * Si la liste commence par '+' ou '-', il s'agit d'une mise � jour */
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

   
   private String defaultBookmarkListByGlu=null;     // Liste des bookmarks trouv�s via l'enregistrement GLU dedi� aux BookMarks (%Aladin.Bookmarks)
   private String gluTag=null;                       // Identificateur de l'enregistrement de bookmarks
   
   // M�morise les infos pour g�n�rer les bookmarks (voir createBookmarks() */
   public void memoGluBookmarks(String actionName, String aladinBookmarks) {
      Aladin.trace(4,"Bookmarks.memoBookmarks() %A="+actionName+" %Aladin.Bookmarks="+aladinBookmarks); 
      gluTag=actionName;
      defaultBookmarkListByGlu=aladinBookmarks;
   }
   
   /** Cr�ation des bookmarks par d�faut :
    * 1) Chargement � distance des d�finitions de fonctions scripts
    * 2) Assignation de certaines d'entre-elles en tant que bookmarks.
    */
   public void createBookmarks(boolean noCache) {
      Glu glu = aladin.getGlu();
      Cache cache = aladin.getCache();
      Command command = aladin.getCommand();
      
      if( gluTag!=null ) {
         try {
            aladin.trace(3,"Remote bookmarks loaded...");
            command.setFunctionLocalDefinition(false);  // Toutes ces fonctions sont consid�r�es comme distantes
            if( noCache )  {
               String u = glu.getURL(gluTag,"",false,false)+"";
               cache.putInCache(u);
            }
            MyInputStream in =  new MyInputStream(cache.get(glu.getURL(gluTag,"",false,false)));
            aladin.getCommand().execScript(new String(in.readFully()),false,true);
         } catch( Exception e ) {
            e.printStackTrace();
            System.err.println("Remote bookmarks error: "+e.getMessage());
         }
      }
      
      File f = new File(aladin.getConfiguration().getLocalBookmarksFileName());
      if( !aladin.isOutreach() && f.canRead() ) {
         try {
            aladin.trace(3,"Local bookmarks loaded ["+f.getCanonicalPath()+"]...");
            command.setFunctionLocalDefinition(true);  // Toutes ces fonctions sont consid�r�es comme locales
            MyInputStream in = new MyInputStream(new FileInputStream(f));
            command.execScript(new String(in.readFully()),false,true);
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
      
      command.setFunctionLocalDefinition(true);  // Toutes les autres fonctions seront consid�r�es comme locales
   }
   

}

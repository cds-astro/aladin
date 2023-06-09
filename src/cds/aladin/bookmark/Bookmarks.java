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

package cds.aladin.bookmark;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import cds.aladin.Aladin;
import cds.aladin.Cache;
import cds.aladin.Command;
import cds.aladin.Function;
import cds.aladin.Glu;
import cds.aladin.MyInputStream;
import cds.aladin.Tok;
import cds.aladin.Widget;
import cds.aladin.WidgetControl;

/**
 * Liste des bookmarks
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Mars 2011 - mise en place
 */
public class Bookmarks extends JToolBar implements Widget {
   private Aladin aladin;
   private FrameBookmarks frameBookmarks;     // G�re la fen�tre de consultation/�dition des favoris
   private Color cbg;   // Couleur du fond (pour que �a marche sous Ubuntu)
   private String memoDefaultList="";

   private String defaultBookmarkListByGlu=null;     // Liste des bookmarks trouv�s via l'enregistrement GLU dedi� aux BookMarks (%Aladin.Bookmarks)
   private String gluTag=null;                       // Identificateur de l'enregistrement de bookmarks

   public Bookmarks(Aladin aladin) {
      this.aladin = aladin;
      frameBookmarks = null;
      
      setRollover(true);
      setFloatable(false);
      setBorderPainted(false);
      setBorder(BorderFactory.createEmptyBorder());
      setBackground( aladin.getBackground() );
   }
   
   protected void paintComponent(Graphics g) {
	   super.paintComponent(g);
       g.setColor( aladin.getBackground() );
       g.fillRect(0, 0, getWidth(), getHeight());
   }
   
   /** Initialisation des bookmarks */
   public void init(boolean noCache) {
      createBookmarks(noCache);
      aladin.getCommand().setFunctionModif(false);
   }

   public FrameBookmarks getFrameBookmarks() {
      if( frameBookmarks==null ) frameBookmarks = new FrameBookmarks(aladin,this);
      return frameBookmarks;
   }

   /** R�initialisation (rechargement) des bookmarks "officielles" */
   public void reload() {
      String list = aladin.configuration.getBookmarks();
      aladin.configuration.resetBookmarks();
      init(true);

      // On r�active les bookmarks locaux
      StringTokenizer tok = new StringTokenizer(list==null?"":list,",");
      while( tok.hasMoreTokens() ) {
         String name = tok.nextToken();
         Function f = aladin.getCommand().getFunction(name);
         if( f.isLocalDefinition() ) f.setBookmark(true);
      }
      if( aladin.hasGUI() ) resumeToolBar();
   }

   public JToolBar getToolBar() {
      return this;
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
      Enumeration<Function> e;
      try {
         e = aladin.getCommand().getBookmarkFunctions().elements();
      } catch( Exception e1 ) {
         e1.printStackTrace();
         return;
      }
      while( e.hasMoreElements() ) {
         Function f = e.nextElement();
         ButtonBookmark bkm = new ButtonBookmark(aladin,f);
         toolBar.add(bkm);
      }

      JButton plus = new JButton("+");
      plus.setBackground( aladin.getBackground());
      plus.setForeground( Aladin.COLOR_LABEL );
      plus.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
      plus.setToolTipText(aladin.getChaine().getString("BKMEDITOR"));
      plus.setFont( plus.getFont().deriveFont(Font.BOLD));
      plus.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { editFrame(); }
      });
      toolBar.add(plus);
   }


   public void resumeToolBar() {
      if( SwingUtilities.isEventDispatchThread() ) {
         resumeToolBar1();
      } else {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { resumeToolBar1(); }
         });
      }
   }

   private void resumeToolBar1() {
      removeAll();
      populateToolBar(this);
      
      revalidate();
      aladin.repaint();
      if( aladin.f!=null ) aladin.f.repaint();
   }


   /** Affichage de la frame de consultation des signets */
   public void showFrame() { getFrameBookmarks().setVisible(true); }

   /** Affichage de la frame d'�dition et de consultation des signets */
   public void editFrame() { getFrameBookmarks().setVisibleEdit(); }

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

   private boolean remoteBookmarksLoaded=false; // true si les bookmarks distants ont �t� correctement charg�s

   /** Retourne true si la liste des bookmarks peut �tre sauvegard�e dans le fichier de configuration */
   public boolean canBeSaved() {
      return remoteBookmarksLoaded;
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


   // M�morise les infos pour g�n�rer les bookmarks (voir createBookmarks() */
   public void memoGluBookmarks(String actionName, String aladinBookmarks) {
      Aladin.trace(3,"Bookmarks.memoBookmarks() %A="+actionName+" %Aladin.Bookmarks="+aladinBookmarks);
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
            aladin.getCommand().execScript(new String(in.readFully()),false,true,false);
            in.close();
            remoteBookmarksLoaded=true;
         } catch( Exception e ) {
            e.printStackTrace();
            System.err.println("Remote bookmarks error: "+e.getMessage());
         }
      }

      File f = new File(aladin.getConfiguration().getLocalBookmarksFileName());
      if( /* !aladin.isOutreach() && */ f.canRead() ) {
         try {
            aladin.trace(3,"Local bookmarks loaded ["+f.getCanonicalPath()+"]...");
            command.setFunctionLocalDefinition(true);  // Toutes ces fonctions sont consid�r�es comme locales
            MyInputStream in = new MyInputStream(new FileInputStream(f));
            command.execScript(new String(in.readFully()),false,true,false);
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

      command.setFunctionLocalDefinition(true);  // Toutes les autres fonctions seront consid�r�es comme locales
   }

   private WidgetControl voc=null;

   @Override
   public WidgetControl getWidgetControl() { return voc; }

   @Override
   public void createWidgetControl(int x, int y, int width, int height, float opacity,JComponent parent) {
      voc = new WidgetControl(this,x,y,width,height,opacity,parent);
      voc.setResizable(true);
      voc.setCollapsed(false);
   }

   @Override
   public void paintCollapsed(Graphics g) { }



}

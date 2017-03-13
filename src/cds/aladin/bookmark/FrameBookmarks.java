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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.aladin.Function;
import cds.tools.Util;

/**
 * Gestion de la fenêtre des bookmarks - permet la consultation, la sélection et l'édition
 * Gère également de la JToolbar qui permet leur affichage sous la barre du menu
 * @author Pierre Fernique [CDS]
 * @version 1.1 - Mars 2011 - nettoyage/commentaire du code
 */
public class FrameBookmarks extends JFrame {
   private Aladin aladin;
   private Bookmarks bookmarks;
 
   /**
    * Crée la fenêtre de gestion des bookmarks
    */
   public FrameBookmarks(Aladin aladin,Bookmarks bookmarks) {
      super();
      this.aladin=aladin;
      this.bookmarks=bookmarks;
      Aladin.setIcon(this);
      setTitle(aladin.getChaine().getString("BKMTITLE"));
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false, aladin);
   }
   
   // Ajout d'un bookmark
   private Function addBookmark(String name,String param,String description,String code) {
      Function f = new Function(name,param,code,description);
      f.setBookmark(true);
      aladin.getCommand().addFunction(f);
      return f;
   }
   
   /********************************* Gère la Frame ***********************************************/
   
   private boolean expertMode=false;    // Mode courant de la frame (avec ou sans panel d'édition)
   private JPanel expertPanel=null;     // Panel de l'expert (avec panel d'édition)
   private JPanel amateurPanel=null;    // Panel de l'amateur (sans panel d'édition)
   private JTextArea edit;              // Panel d'édition du bookmark
   private Function fctEdit=null;       // Fonction script dans le cas d'une édition en cours
   private JButton apply,delete;        // Boutons de contrôles
   
   @SuppressWarnings("deprecation")
   public void show() {
      if( genPanel==null ) {
         createPanel();
         pack();
      }
      super.show();
   }
   
   private JPanel genPanel=null;
   
   // Génération des différents panels de l'interface graphique
   private void createPanel() {
      genPanel = (JPanel)getContentPane();
      genPanel.setLayout( new BorderLayout(5,5));
      genPanel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
      genPanel.add(getBookmarksPanel(),BorderLayout.NORTH);
      genPanel.add(getAmateurPanel(),BorderLayout.SOUTH);
      
      // Juste pour qu'il soit créé
      getExpertPanel();
   }
   
   // Retourne le Panel orientée Expert (avec le panel d'édition)
   private JPanel getExpertPanel() {
      if( expertPanel!=null ) return expertPanel;
      JPanel p = expertPanel = new JPanel(new BorderLayout(5,5));
      p.add(getEditPanel(),BorderLayout.CENTER);
      p.add(getControlPanel(),BorderLayout.SOUTH);
      return p;
   }
   
   // Retourne le Panel orientée Amateur (sans le panel d'édition)
   private JPanel getAmateurPanel() {
      if( amateurPanel!=null ) return amateurPanel;
      JPanel p = amateurPanel = new JPanel( );
      Chaine chaine = aladin.getChaine();
      JButton b;
      
      p.add( b=new JButton(chaine.getString("EXEC")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { execute(); }
      });
      p.add( b=new JButton(chaine.getString("BKMEDITOR")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            genPanel.remove(amateurPanel);
            genPanel.add(getExpertPanel(),BorderLayout.CENTER);
            expertMode=true;
            pack();
         }
      });
      p.add( b=new JButton(chaine.getString("PROPCLOSE")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { dispose(); }
      });
      p.add( Util.getHelpButton(this, chaine.getString("BKMHELP")));
      return p;
   }
   
   public void setVisibleEdit() {
      if( genPanel==null ) createPanel();
      genPanel.remove(amateurPanel);
      genPanel.add(getExpertPanel(),BorderLayout.CENTER);
      expertMode=true;
      pack();
      setVisible(true);
   }
   
   // Retourne le panel d'édition
   protected JPanel getEditPanel() {
      JPanel p = new JPanel( new BorderLayout());
      edit = new JTextArea(8,50);
      edit.setFont( Aladin.COURIER );
      edit.addKeyListener(new KeyAdapter(){
         public void keyReleased(KeyEvent e) {
            apply.setEnabled(isModif());
         }
      });
      JScrollPane sc = new JScrollPane(edit);
      p.add(sc,BorderLayout.CENTER);
      return p;
   }
   
   // résume la fenêtre d'édition suivant la fonction passé en paramètre.
   // si valid==true, demande au préalable la validation de la précédente fonction
   // en cours d'édition
   private void resumeEdit(Function f,boolean valid) {
      if( valid ) valide(true);
      fctEdit=f;      
      boolean local=f==null ? false : f.isLocalDefinition();
      edit.setText(f==null ? "" : f.toString());
      edit.setEditable(local);
      delete.setEnabled(local);
      apply.setEnabled(isModif());
   }
   
   // retourne true si la fonction en cours d'édition a été modifiée
   // depuis le début de son édition
   private boolean isModif() {
      if( fctEdit==null ) return false;
      if( !fctEdit.isLocalDefinition() ) return false;  // on n'a pas le droit de modifier une définition distante
      return !fctEdit.toString().trim().equals(edit.getText().trim());
   }
   
   // Met à jour les bookmarks avec la fonction en cours d'édition, si celle-ci a été
   // modifié. Si flagTest==false, la mise à jour est faite sans demande de confirmation
   private void valide(boolean flagTest) {
      if( !isModif() ) return;
      if( !flagTest || aladin.confirmation(this,aladin.getChaine().getString("BKMAPPLY")) ) {
         try {
            Function t = new Function(edit.getText());
            fctEdit.setDescription(t.getDescription());
            fctEdit.setName(t.getName());
            fctEdit.setParam(t.getParam());
            fctEdit.setCode(t.getCode());
            fctEdit.setLocalDefinition(true);
            edit.setText(fctEdit.toString());
            resumeTable();
            bookmarks.resumeToolBar();
            aladin.log("Bookmark","create");
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }
   }
   
   private void execute() {
      Function f;
      try { 
         if( !isModif() ) {
            int row = table.getSelectedRow();
            f = aladin.getCommand().getFunction(row); 
         } else {
            f = new Function(edit.getText());
         }
         f.exec(aladin,"",true); }
      catch( Exception e ) { e.printStackTrace(); }
   }
   
   // retourne le panel des boutons de controle du panel Expert
   private JPanel getControlPanel() {
      JPanel p = new JPanel( );
      Chaine chaine = aladin.getChaine();
      JButton b;
      
      p.add( b=new JButton(chaine.getString("PROPNEWCALIB")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { createNewBookmark(); }
      });
      p.add( b=delete=new JButton(chaine.getString("SLMDEL")));
      b.setEnabled(false);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { deleteBookmark(); }
      });
      p.add( b=new JButton(chaine.getString("BKMDEFAULT")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { resetBookmarks(true); }
      });
      p.add(new JLabel(" - "));
      p.add( b=new JButton(chaine.getString("EXEC")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { execute(); }
      });
      p.add( b=apply=new JButton(chaine.getString("PROPAPPLY")));
      b.setEnabled(false);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { valide(false); }
      });
      p.add( b=new JButton(chaine.getString("PROPCLOSE")));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            dispose();
//            genPanel.remove(expertPanel);
//            genPanel.add(getAmateurPanel(),BorderLayout.SOUTH);
//            expertMode=false;
//            pack();
         }
      });
      return p;
   }
   
   // Initialisation de la fenêtre d'édition avec un Bookmark "vide"
   private void createNewBookmark() { 
      createNewBookmark("YourName","$TARGET,$RADIUS","Your description",null); 
   }
   public void createNewBookmark(String name,String param,String description,String code) {
      Function f = addBookmark(name,param,description,code);
      aladin.getCommand().addFunction(f);
      resumeEdit(f,true);
      if( code!=null ) {
         fctEdit.setCode(null);
         valide(false);
      }
      resumeTable();
      int row = table.getRowCount()-1;
      table.scrollRectToVisible(table.getCellRect(row,NAME,true));
      table.setRowSelectionInterval(row,row);
   }
   
   // Suppression du bookmark sélectionné dans la JTable
   private void deleteBookmark() {
       int row = table.getSelectedRow();
       Function f = aladin.getCommand().getFunction(row); 
       aladin.getCommand().removeFunction(f);
       resumeTable();
       bookmarks.resumeToolBar();
       resumeEdit(null,false);
   }
   
   // Réinitialisation de la liste des bookmarks
   protected void resetBookmarks(boolean withConfirmation) {
      if( withConfirmation &&
            !aladin.confirmation(this,aladin.getChaine().getString("BKMCONFIRM")) ) return;
      bookmarks.reload();
      resumeTable();
      resumeEdit(null,false);
   }
   
   // Fermeture de la fenêtre
   public void dispose() {
      if( expertMode ) {
         genPanel.remove(expertPanel);
         genPanel.add(getAmateurPanel(),BorderLayout.SOUTH);
         pack();
         expertMode=false;
      }
      setVisible(false);
   }
   
   /********************************** Gère la table des bookmarks *********************************/
   
   static final int BKM    = 0;
   static final int LOC    = 1;
   static final int NAME   = 2;
   static final int DESC   = 3;
   
   private JTable table;
   private BookmarkTable tableModel;
   
   protected JPanel getBookmarksPanel() {
      JPanel p = new JPanel( new BorderLayout());
      JScrollPane sc = new JScrollPane(createBookmarksTable());
      p.add(sc,BorderLayout.CENTER);
      return p;
   }
   
   private void resumeTable() { tableModel.fireTableDataChanged(); }

   private JTable createBookmarksTable() {
      table=new JTable(tableModel=new BookmarkTable());
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.getColumnModel().getColumn(LOC).setMinWidth(20);
      table.getColumnModel().getColumn(LOC).setMaxWidth(20);
      table.getColumnModel().getColumn(BKM).setMinWidth(60);
      table.getColumnModel().getColumn(BKM).setMaxWidth(60);
      table.getColumnModel().getColumn(NAME).setMinWidth(70);
      table.getColumnModel().getColumn(NAME).setMaxWidth(70);
      table.setPreferredScrollableViewportSize(new Dimension(420,38*12));
      
      MyRenderer cr = new MyRenderer();
      table.getColumnModel().getColumn(LOC).setCellRenderer(cr);

      table.addMouseListener(new MouseAdapter() {
         public void mouseReleased(MouseEvent e) {
            int row = table.rowAtPoint(e.getPoint());
            Function f = aladin.getCommand().getFunction(row);
            if( delete!=null ) delete.setEnabled(true);
            resumeEdit(f,true);
         }
      });

      return table;
   }

   class BookmarkTable extends AbstractTableModel {
      
      public int getColumnCount() { return 4; }
      
      public int getRowCount() { return aladin.getCommand().getNbFunctions(); }
      
      public String getColumnName(int col) { 
         return col==LOC  ? "" :
                col==BKM  ? "Bookmark" :
                col==NAME ? "Name" : 
                            "Description" ;
      }
      
      @SuppressWarnings({ "unchecked", "rawtypes" })
      public Class getColumnClass(int col) {
         if( col==LOC ) return (new LabelIcon()).getClass();
         if( col==BKM ) return (new Boolean(true)).getClass();
         return super.getColumnClass(col);
      }

      public Object getValueAt(int row, int col) {
         Function f = aladin.getCommand().getFunction(row);
         switch( col ) {
            case LOC  : return getIcon(f);
            case BKM  : return new Boolean(f.isBookmark());
            case NAME : return f.getName(); 
            default : return f.getDescription();
         }
      }
      
      public boolean isCellEditable(int row, int col) {
         if( col==BKM ) return true;
         Function f = aladin.getCommand().getFunction(row);
         return f.isLocalDefinition();
      }
      
      public void setValueAt(Object value,int row, int col) {
         Function f = aladin.getCommand().getFunction(row);
         if( col==BKM ) { f.setBookmark(!f.isBookmark()); bookmarks.resumeToolBar(); }
         else if( col==NAME || col==DESC ) {
            if( col==NAME ) f.setName((String)value);
            else f.setDescription((String)value);
            f.setLocalDefinition(true);
            resumeEdit(f,false);
            bookmarks.resumeToolBar();
         }
      }
   }
   
   class MyRenderer extends DefaultTableCellRenderer {
       public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,  
                                                      boolean hasFocus, int row, int column) {  
          return (Component)value;
       }  
   }  
   
   private LabelIcon getIcon(Function f) {
      return new LabelIcon(f);
   }
   
   class LabelIcon extends JLabel {
      Function f;
      
      LabelIcon() { super(); }
      LabelIcon(Function f) { this.f=f; }
      
//      public Dimension getPreferedDimension() { return new Dimension(60,20); }
      
      /** Affichage du bouton "bookmark", avec une petite étoile en préfixe */
      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         Util.drawStar(g, 6,7, f.isLocalDefinition() ? Color.blue : ButtonBookmark.Orange);
      }
   }
   


}

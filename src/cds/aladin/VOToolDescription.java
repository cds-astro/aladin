// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.util.EventListener;

import javax.swing.*;
import javax.swing.border.Border;

import cds.tools.Util;

/**
 * Panel de description d'un VOtool
 * @date janvier 2008 - création
 * @author Pierre Fernique
 */
public class VOToolDescription extends JPanel {

   static private String INSTITUTE, VERSION, URL, STATE, COPYRIGHT, SYSTEM, DIR, INSTALL;
   static private String MODEINSTALL[];

   private Aladin aladin;
   private EventListener listener;
   protected GluApp vo; // Le VOtool à décrire
   private boolean editable;

   // Les différents champs et boutons
   private MyLabel name, institute, version, right, doc, install;
   private MyLabel system,dir;
   private JTextArea descr;
   private JCheckBox state;
   protected Timer timer;
   private JButton browse;

   protected VOToolDescription(Aladin a,EventListener listener) {
      aladin = a;
      this.listener=listener;
      INSTITUTE = "Origin";
      VERSION = "Version";
      STATE = "Available in the Aladin menu";
      URL = "Documentation";
      COPYRIGHT = "Copyright";
      SYSTEM = "Command line";
      DIR = "Running directory";
      INSTALL = "Install. method";
      MODEINSTALL = new String[]{ "Local","Java jar package","Web download page","Java Webstart (no installation)","Java Applet (no installation)" };
      createPanel();
      timer=new Timer(500,new ActionListener() {
         public void actionPerformed(ActionEvent e) { downloading(); }
      });
      timer.setRepeats(true);
      editable=false;
   }

   /** Retourne une chaine vide si le paramètre est null ou vide */
   private String getInfo(String s) {
      if( s == null || s.trim().length() == 0 ) return "";
      return s;
   }

   /** Création du panel. L'initialisation des valeurs se fera par setPlugin() */
   private void createPanel() {
      JPanel p = new JPanel();
      p.setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(1, 3, 1, 3);
      c.anchor = GridBagConstraints.WEST;

      JLabel l;
      MyLabel m;
      JTextArea t;
      JButton b;

      c.gridx = 1;
      c.gridy = 0;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.WEST;
      c.insets.top += 5;
      c.insets.bottom += 10;
      p.add(m = name = new MyLabel(listener), c);
      m.setFont(m.getFont().deriveFont(Font.BOLD,
            m.getFont().getSize2D() + 3));
      m.setForeground(Aladin.COLOR_GREEN);
      c.insets.top -= 5;
      c.insets.bottom -= 10;

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(INSTITUTE + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(institute = new MyLabel(listener), c);

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(COPYRIGHT + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(m = right = new MyLabel(listener), c);

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(URL + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(m = doc = new MyLabel(listener), c);
      m.addMouseListener(new MouseAdapter() {
         public void mouseReleased(MouseEvent e) {
            if( doc.getToolTipText() == null ) return;
            aladin.glu.showDocument("Http", ((JLabel) e.getSource())
                  .getToolTipText(), true);
         }

         public void mouseEntered(MouseEvent e) {
            if( doc.getToolTipText() == null ) return;
            doc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         }

         public void mouseExited(MouseEvent e) {
            if( doc.getToolTipText() == null ) return;
            doc.setCursor(Cursor.getDefaultCursor());
         }

         public void mousePressed(MouseEvent e) {
            if( doc.getToolTipText() == null ) return;
            doc.setForeground(new Color(128, 0, 128));
         }
      });

      c.gridx = 0;
      c.gridy++;
      c.fill = GridBagConstraints.BOTH;
      c.gridwidth = 2;
      c.insets.top += 5;
      c.weightx = c.weighty = 1;
      p.add(new JScrollPane(t = descr = new JTextArea()), c);
      t.setFont(Aladin.PLAIN);
      t.setWrapStyleWord(true);
      t.setLineWrap(true);
      t.setEditable(false);
      c.weightx = c.weighty = 0;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = 1;
      c.insets.top -= 5;

      c.gridx = 0;
      c.gridy++;
      c.insets.top +=10;
      p.add(l = new JLabel(VERSION + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(m = version = new MyLabel(listener), c);
      m.setFont(m.getFont().deriveFont(Font.BOLD));
      c.insets.top -=10;

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(INSTALL + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(m = install = new MyLabel(listener), c);
      
      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(DIR + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      JPanel p1 = new JPanel(new BorderLayout());
      p1.add(dir = new MyLabel(listener),"Center");
      p1.add(b=browse=new JButton("Browse"),"East");
      b.setEnabled(false);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { dirBrowser(); }
      });
      p.add(p1, c);
      
      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(SYSTEM + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(system = new MyLabel(50,listener), c);
      
      c.gridx = 0;
      c.gridy++;
      c.gridwidth=2;
      c.anchor=GridBagConstraints.CENTER;
      p.add(state = new JCheckBox(STATE), c);
      state.setFont(state.getFont().deriveFont(Font.BOLD));
      state.addActionListener((ActionListener)listener);

      setLayout(new BorderLayout(5, 5));
      add(p, BorderLayout.CENTER);

      setBorder(BorderFactory.createEtchedBorder());
   }

   /** Pour gérer la taille */
   static private final Dimension DIM = new Dimension(550, 350);

   public Dimension getPreferredSize() {
      return DIM;
   }
   
   /** Ouverture de la fenêtre de sélection d'un fichier */
   private void dirBrowser() {
      FileDialog fd = new FileDialog(aladin.dialog,"Running directory selection");
      if( this.dir!=null ) fd.setDirectory(this.dir.getText());
      
      // (thomas) astuce pour permettre la selection d'un repertoire
      // (c'est pas l'ideal, mais je n'ai pas trouve de moyen plus propre en AWT)
      fd.setFile("-");
      fd.setVisible(true);
      String dir = fd.getDirectory();
      String name =  fd.getFile();
      // si on n'a pas changé le nom, on a selectionne un repertoire
      if( name!=null && name.equals("-") ) name = "";
      String t = (dir==null?"":dir)+(name==null?"":name);
      this.dir.setText(t);
      aladin.frameVOTool.apply.setEnabled(hasBeenChanged());
   }


   
   /** Retourne true si le formulaire a changé par rapport à son contenu initial */
   protected boolean hasBeenChanged() {
      if( vo==null ) return false;
      if( editable && name.getText().trim().length()>0 ) return true;
      if( vo.isActivated()!=state.isSelected() ) return true;
      if( system.hasBeenEdited() ) return true;
      if( dir.hasBeenEdited() ) return true;
      return false;
   }

   /** Applique les modifs du formulaire dans le GluApp */
   protected GluApp apply() {
      vo.activated = state.isSelected() ? "Yes":"No";      
      String s = dir.getText().trim();
      if( s.length()>0 ) vo.dir=s;
      else vo.dir=null;
      s=system.getText().trim();
      String cmd = vo.getJavaCommand();
      if( cmd!=null && cmd.equals(s) ) s="";
      if( s.length()>0 ) vo.system=s;
      else vo.system=null;
      
      if( editable ) {
         s=name.getText().trim(); vo.aladinLabel=s.length()>0 ? s : null;
         s=institute.getText().trim(); vo.institute=s.length()>0 ? s : null;
         s=version.getText().trim(); vo.releaseNumber=s.length()>0 ? s : "0";
         s=right.getText().trim(); vo.copyright=s.length()>0 ? s : null;
         setEditable(false);
      }
      
      setVOtool(vo);
      return vo;
   }
   
   protected int modeB=0;
   
   /** Reaffichage de la progression du downloading */
   private void downloading() {
      String dots = modeB==3?"...":modeB==2?"..":modeB==1?".":"";
      modeB++; if( modeB>3 ) modeB=0;
      String s=(vo.nextNumber!=null ? vo.nextNumber : vo.releaseNumber)
                +" downloading ("+(vo.downloading/1024)+" KB)"+dots;
      version.setText(s);
   }
   
   protected void setEditable(boolean flag) {
      editable=flag;
      name.setEditable(flag); 
      institute.setEditable(flag);
      version.setEditable(flag);
      right.setEditable(flag); 
      descr.setEditable(flag); 
   }
   
   /** Positionne le plugin à décrire */
   protected void setVOtool(GluApp vo) {
      String s;
      this.vo = vo;
      int mode = vo==null ? 0 : vo.getInstallMode();
      name.setText(vo == null ? "" : getInfo(vo.aladinLabel));
      institute.setText(vo == null ? "" : getInfo(vo.institute));
      
      version.setForeground(Color.black);
      if( vo==null ) { s=null; setEditable(false); }
      else {
         if( editable ) s= getInfo(vo.releaseNumber);
         else if( vo.downloading!=-1 ) {
            s="";
            version.setForeground(Color.orange.darker());
            timer.start();
         } else  {
            if( timer.isRunning() ) timer.stop();
            if( vo.releaseNumber==null ) {
               s="";
               version.setForeground(Color.black);
            }
            else {
               if( vo.canBeRun() ) {
                  s= getInfo(vo.releaseNumber+(mode==GluApp.WEBSTART || mode==GluApp.APPLET ? "":" installed"));
                  version.setForeground(Aladin.COLOR_GREEN);
                  if( vo.nextNumber!=null ) {
                     s=s+" - new available version: "+vo.nextNumber;
                     version.setForeground(Color.orange.darker());
                  }
               } else {
                  if( vo.nextNumber!=null ) s= getInfo(vo.nextNumber+" not installed");
                  else s= getInfo(vo.releaseNumber+" not installed");
                  version.setForeground(Color.red);
               }
            }
         }
      }
      version.setText(s);
      
      install.setText(vo==null ? "" : MODEINSTALL[mode]);
      
      if( mode==GluApp.APPLET || mode==GluApp.WEBSTART ) {
         dir.setMemo(" --");
         dir.setEditable(false); 
         system.setMemo(" -- ");
         system.setEditable(false); 
         browse.setEnabled(false);
      } else {
         dir.setMemo(vo.jarUrl!=null ? aladin.getVOPath() : "",
               vo==null ? null : vo.dir);
         system.setMemo(vo.javaParam!=null ? vo.getJavaCommand() : "",
               vo == null ? "" : vo.getCommand());
         browse.setEnabled(true);
      }

      descr.setText(vo == null ? "" : getInfo(vo.verboseDescr!=null?vo.verboseDescr:vo.description));
      descr.setCaretPosition(0);
      right.setText(vo == null ? "" : getInfo(vo.copyright));
      if( vo == null || vo.docUrl==null ) doc.setText("");
      else if( vo.docUrl != null ) {
         doc.setForeground(Color.blue);
         Util.toolTip(doc,vo.docUrl);
         doc.setText(cut(vo.docUrl, 60));
      } else Util.toolTip(doc,null);
      state.setSelected(vo!=null && vo.isActivated());
      state.setEnabled(vo!=null && vo.canBeRun());
      
   }
   
   /** Coupe une chaine trop longue et ajoute des ... */
   static String cut(String s, int m) {
      int n = s.length();
      if( n <= m ) return s;
      return s.substring(0, m - 2) + "...";
   }
   
   static private Color BG = new JLabel().getBackground();
   
   /** JLabel éditable */
   class MyLabel extends JTextField implements KeyListener {
      private Border border;
      private Border emptyBorder=BorderFactory.createEmptyBorder(1,1,1,1);
      private String memo,init;
      private boolean listen=false;
      
      MyLabel(EventListener listener) {
         super();
         suite(listener);
      }
      
      MyLabel(int n,EventListener listener) {
         super(n);
         suite(listener);
      }
      
      /** Suite des constructeurs */
      private void suite(EventListener listener) {
         border=getBorder();
         setEditable(false);
         addKeyListener((KeyListener)listener);
      }
      
      /** Editable, affiche en gris si egale à mémo, sinon en noir */
      protected void setMemo(String memo) { setMemo(memo,null); }
      protected void setMemo(String memo,String init) {
         this.memo=memo;
         this.init=init==null ? memo : init;
         setText(this.init);
         setEditable(true);
         if( !listen ) { addKeyListener(this); listen=true; }
      }
      
      /** retourne true si setMemo(xxx)  a été au préalablement appelé
       * et que la chaine en cours de saisie est différente de xxx */
      public boolean hasBeenChanged() {
         if( !isEditable() || memo==null ) return false;
         return !getText().trim().equals(memo);
      }
      
      /** retourne true si setMemo(xxx,yyy)  a été au préalablement appelé
       * et que la chaine en cours de saisie est différente de yyy */
      public boolean hasBeenEdited() {
         if( !isEditable() || init==null ) return false;
         return !getText().trim().equals(init);
      }
      
      public void setEditable(boolean flag) {
         super.setEditable(flag);
         setBackground(flag ? Color.white : BG );
         setBorder(flag ? border : emptyBorder);
         setCaretPosition(0);
      }
      
      public void setText(String s) {
         super.setText(s);
         if( memo!=null || init!=null ) testChange();
      }
      
      /** Change la couleur si la chaine différe de memo */
      private void testChange() {
         Color c = hasBeenChanged() || hasBeenEdited() ? Color.black : Color.gray;
         if( getForeground()!=c ) setForeground(c);
      }

      public void keyPressed(KeyEvent e) { }
      public void keyReleased(KeyEvent e) { testChange(); }
      public void keyTyped(KeyEvent e) { }
   }
}

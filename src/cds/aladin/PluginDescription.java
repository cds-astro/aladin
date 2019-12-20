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
import javax.swing.*;
import cds.tools.Util;

/**
 * Panel de description d'un plugin
 * @date janvier 2007 - création
 * @author Pierre Fernique
 */
public class PluginDescription extends JPanel {

   static private String AUTHOR, VERSION, CAT, ORIGIN, STATE, SCRIPT, NOSUPPORT,
         SCRIPTHELP, THREAD, RUNNING, ACTIVE, SUSPENDED, IDLE, START, STOP,
         SUSPEND, RESUME;

   private Aladin aladin;

   private AladinPlugin ap; // Le plugin à décrire

   // Les différents champs et boutons
   private JLabel name, author, version, cat, state, script, thread, url;

   private JTextArea descr;

   private JButton start, stop, suspend, resume, help;

   protected PluginDescription(Aladin a) {
      aladin = a;
      AUTHOR = "Author";
      VERSION = "Version";
      CAT = "Category";
      STATE = "State";
      ORIGIN = "Origin";
      SCRIPT = "Scripting";
      SCRIPTHELP = "Script usage?";
      NOSUPPORT = "Not supported";
      THREAD = "Threaded";
      RUNNING = "running";
      ACTIVE = "active";
      IDLE = "idle";
      START = "Start";
      STOP = "Stop";
      SUSPEND = "Suspend";
      SUSPENDED = "Sleeping";
      RESUME = "Resume";

      createPanel();
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
      JButton b;
      JTextArea t;
      Insets in = new Insets(1, 2, 1, 2);

      c.gridx = c.gridy = 0;
      c.gridwidth = 2;
      c.gridx = 0;
      c.fill = GridBagConstraints.NONE;
      c.anchor = GridBagConstraints.CENTER;
      c.insets.bottom += 5;
      p.add(l = name = new JLabel(), c);
      l.setFont(l.getFont().deriveFont(Font.BOLD + Font.ITALIC,
            l.getFont().getSize2D() + 2));
      l.setForeground(Aladin.COLOR_GREEN);
      c.gridwidth = 1;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets.bottom -= 5;

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(AUTHOR + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(l = author = new JLabel(), c);

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(VERSION + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(l = version = new JLabel(), c);

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(CAT + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(l = cat = new JLabel(), c);

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(ORIGIN + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(l = url = new JLabel(), c);
      l.addMouseListener(new MouseAdapter() {
         public void mouseReleased(MouseEvent e) {
            if( ((JLabel) e.getSource()).getToolTipText() == null ) return;
            aladin.glu.showDocument("Http", ((JLabel) e.getSource())
                  .getToolTipText(), true);
         }

         public void mouseEntered(MouseEvent e) {
            if( ((JLabel) e.getSource()).getToolTipText() == null ) return;
            url.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         }

         public void mouseExited(MouseEvent e) {
            if( ((JLabel) e.getSource()).getToolTipText() == null ) return;
            url.setCursor(Cursor.getDefaultCursor());
         }

         public void mousePressed(MouseEvent e) {
            if( ((JLabel) e.getSource()).getToolTipText() == null ) return;
            url.setForeground(new Color(128, 0, 128));
         }
      });

      c.gridx = 0;
      c.gridy++;
      c.fill = GridBagConstraints.BOTH;
      c.gridwidth = 2;
      c.insets.top += 5;
      c.weightx = c.weighty = 1;
      p.add(new JScrollPane(t = descr = new JTextArea()), c);
      t.setWrapStyleWord(true);
      t.setLineWrap(true);
      t.setEditable(false);
      c.weightx = c.weighty = 0;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = 1;
      c.insets.top -= 5;

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(SCRIPT + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(l = script = new JLabel(), c);

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(THREAD + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(l = thread = new JLabel(), c);
      l.setFont(l.getFont().deriveFont(Font.PLAIN));

      c.gridx = 0;
      c.gridy++;
      p.add(l = new JLabel(STATE + " : "), c);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      c.gridx++;
      p.add(state = l = new JLabel(), c);
      // l.setFont(l.getFont().deriveFont(Font.PLAIN));

      setLayout(new BorderLayout(5, 5));
      add(p, BorderLayout.CENTER);

      p = new JPanel();
      p.add(b = help = new JButton(SCRIPTHELP));
//      b.setFont(b.getFont().deriveFont(Font.PLAIN));
      b.setMargin(in);
      b.setEnabled(false);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if( ap.scriptHelp() == null ) return;
            aladin.command.execHelpCmd(ap.scriptHelp());
            aladin.f.toFront();
         }
      });
      p.add(start = b = new JButton(START));
      b.setEnabled(false);
      b.setMargin(in);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            try {
               ap.start();
            } catch( AladinException e1 ) {
               e1.printStackTrace();
               aladin.error(aladin, aladin.chaine.getString("PLUGERROR")
                     + "\n\n" + e1.getMessage());
            }
            Util.pause(100);
            resume();
         }
      });
      p.add(stop = b = new JButton(STOP));
      b.setEnabled(false);
      b.setMargin(in);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ap.stop();
            Util.pause(100);
            resume();
         }
      });
      p.add(suspend = b = new JButton(SUSPEND));
      b.setEnabled(false);
      b.setMargin(in);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ap.suspend();
            Util.pause(100);
            resume();
         }
      });
      p.add(resume = b = new JButton(RESUME));
      b.setEnabled(false);
      b.setMargin(in);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ap.resume();
            Util.pause(100);
            resume();
         }
      });
      add(p, BorderLayout.SOUTH);

      setBorder(BorderFactory.createEtchedBorder());
   }

   /** Pour gérer la taille */
   static private final Dimension DIM = new Dimension(400, 400);

   public Dimension getPreferredSize() {
      return DIM;
   }

   /** Positionne le plugin à décrire */
   protected void setPlugin(AladinPlugin ap) {
      this.ap = ap;
      name.setText(ap == null ? "" : getInfo(ap.menu()));
      author.setText(ap == null ? "" : getInfo(ap.author()));
      version.setText(ap == null ? "" : getInfo(ap.version()));
      descr.setText(ap == null ? "" : getInfo(ap.description()));
      script.setText(ap == null ? "" : (ap.scriptCommand() == null ? NOSUPPORT
            : ap.scriptCommand())
            + "   ");
      if( ap != null ) script.setFont(script.getFont().deriveFont(
            ap.scriptCommand() == null ? Font.PLAIN : Font.BOLD));
      thread.setText(ap == null ? "" : ap.inSeparatedThread() ? "true"
            : "false");
      cat.setText(ap == null ? "" : getInfo(ap.category()));
      help.setEnabled(ap == null ? false : ap.scriptHelp() != null);
      url.setText(ap == null ? "" : getInfo(ap.url()));
      if( ap == null ) url.setText("");
      else if( ap.url() != null ) {
         url.setForeground(Color.blue);
         Util.toolTip(url,ap.url());
         url.setText(cut(ap.url(), 60));
      } else Util.toolTip(url,null);

      resume();
   }

   /** Met à jour les variables d'état (running,...) */
   protected void resume() {
      boolean running, suspended, threaded;

      if( ap == null ) {
         state.setText("");
         start.setEnabled(false);
         stop.setEnabled(false);
         resume.setEnabled(false);
         suspend.setEnabled(false);
         return;
      }
      threaded = ap.inSeparatedThread();
      state.setText((running = suspended = ap.isSuspended()) ? SUSPENDED
            : !(running = ap.isRunning()) ? IDLE : threaded ? RUNNING : ACTIVE);
      state.setForeground(suspended ? Color.orange : running ? Color.red
            : Aladin.COLOR_GREEN);
      start.setEnabled(!running);
      stop.setEnabled(running);
      resume.setEnabled(threaded && running && suspended);
      suspend.setEnabled(threaded && running && !suspended);

   }

   /** Coupe une chaine trop longue et ajoute des ... */
   static String cut(String s, int m) {
      int n = s.length();
      if( n <= m ) return s;
      return s.substring(0, m - 2) + "...";
   }
}

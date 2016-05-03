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

package cds.allsky;

// Copyright 2012 - UDS/CNRS
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.RandomAccessFile;

import javax.swing.*;

import cds.aladin.Aladin;
import cds.aladin.prop.PropPanel;
import cds.tools.Util;

/**
 * Gère la définition GLU d'un survey Allsky généré par l'utilisateur
 * @author Pierre Fernique [CDS]
 * @version 1.0 - janvier 2011
 */
public class FrameGlu extends JFrame implements KeyListener {

   // décrit les 4 champs de GLUPARAM[]
   static private final int REQUIRED=0, LABEL=1,  FIELD=2, KEY=3,VALUE=4, INFO=5;

   // Informations pour la construction du formulaire de renseignements
   // Champ 1 : * -> requis, "-" -> optional, "" -> non visible
   // Champ 2 : Label GLU du champ
   // Champ 3 : Label properties du champ
   // Champ 4 : Valeur par défaut
   // Champ 5 : Courte description, avec exemple éventuel
   static private String GLUPARAM[][] = {
      { "*", "Survey ID",      "Id",            "",                         "",           "One word survey identifier (ex: P/DSS/2)"  },
      { "*", "Name",           "Description",   "",                         "",           "Survey name (ex: DSS blue" },
      { "*", "Url access",     "Url",           "",                         "http://...", "Url for accessing the Healpix data (a Healpix FITS file map or a Healpix Aladin directory)" },
      { "-", "Category",       "Aladin.Tree",   Constante.KEY_CLIENT_CATEGORY,     "Test",       "Aladin tree menu category - use / as separator (ex: Image/Test)" },
      { "-", "Description",    "Description",   Constante.KEY_OBS_TITLE,   "",          "Short description" },
      { "-", "Full descript.", "VerboseDescr",  Constante.KEY_OBS_DESCRIPTION,"",     "Full data description (can be a long paragraph)" },
      { "-", "Web info",       "Doc.User",      "",                         "",           "Web page describing the data" },
      { "-", "Institute",      "Institute",     "",                         "",           "Institute/origin of the data" },
      { "-", "Copyright",      "Copyright",     Constante.KEY_OBS_COPYRIGHT,  "",             "Copyright mention (ex: (c) Institute of ....)" },
      { "-", "Web site",       "Copyright.url", Constante.KEY_OBS_COPYRIGHT_URL,"",           "Web link for copyright mention" },
   };


   private JTextField [] field;  // Les champs de saisie du formulaires
   private Aladin aladin;
   Context context;

   /** Crée et affiche la JFrame qui contient le formulaire */
   public FrameGlu(Aladin aladin, Context c) {
      this.aladin = aladin;
      context = c;
      JPanel panel = new JPanel( new BorderLayout(5,5) );
      panel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panel.add( getInfo(), BorderLayout.NORTH);
      panel.add( getForm(), BorderLayout.CENTER);
      panel.add( getButton(), BorderLayout.SOUTH);
      getContentPane().add(panel);
      pack();
      setLocation(200, 200);
      setVisible(true);
   }

   // Panel de la description de la manip
   private JPanel getInfo() {
      JPanel panel = new JPanel();
      JLabel message = new JLabel(
            "<html><center>Fill up these fields and test your survey description<br>" +
                  "in your Aladin session. Since is ok, send the generated parameter GLU file<br>" +
                  "to your collaborators, or even send it to the CDS team in order to offer<br>" +
            "a full access to your data.</center></html>");
      panel.add(message);
      return panel;
   }

   private JButton test,save;

   // Panel des boutons de commandes
   private JPanel getButton() {
      JPanel panel = new JPanel();
      JButton b;
      test = b = new JButton("Test it locally");
      b.setEnabled(false);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { test(); }
      });
      add(b);
      panel.add(b);
      save = b= new JButton("Save it (for distributing)");
      b.setEnabled(false);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { save(); }
      });
      add(b);
      panel.add(b);
      return panel;
   }

   // Retourne le Panel du formulaire
   private JPanel getForm() {
      GridBagLayout g = new GridBagLayout();
      JPanel panel = new JPanel( g );
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(0,0,0,0);

      field = new JTextField[GLUPARAM.length];
      JTextField f;
      for( int i=0; i<GLUPARAM.length; i++ ) {
         String [] glup = GLUPARAM[i];
         JLabel l = new JLabel( glup[LABEL] );
         if( glup[REQUIRED].length()==0 ) continue;
         if( glup[REQUIRED].charAt(0)=='*' ) l.setFont( l.getFont().deriveFont(Font.BOLD) );
         String value = glup[VALUE];
         if( glup[FIELD].equals("Description") ) value = context.getLabel();
         field[i] =f= new JTextField( value );
         f.addKeyListener(this);
         f.setMinimumSize(new Dimension(300, f.getMinimumSize().height));
         f.setPreferredSize(new Dimension(300, f.getPreferredSize().height));
         PropPanel.addCouple(this, panel, l, glup[INFO], f, g, c, GridBagConstraints.EAST);
      }

      return panel;
   }

   public void keyTyped(KeyEvent e) { }
   public void keyPressed(KeyEvent e) { }
   public void keyReleased(KeyEvent e) { activeButtonIfPossible(); }

   // Active les boutons si tous les champs requis ont été renseignés
   private void activeButtonIfPossible() {
      boolean ok = isReady();
      test.setEnabled(ok);
      save.setEnabled(ok);
   }

   // Retourne true ssi tous les champs requis ont été renseignés
   private boolean isReady() { return missingField()==-1; }

   // Retourne le numéro du premier champ non optionnel qui est encore vide,
   // sinon -1
   private int missingField() {
      for( int i=0; i<GLUPARAM.length; i++ ) {
         if( !GLUPARAM[i][REQUIRED].equals("*") ) continue;
         if( field[i].getText().trim().length()==0 ) return i;
      }
      return -1;
   }

   // Permet le test de l'enregistrement GLU en local uniquement, affiche immédiatement
   // le formulaire Allsky mis à jour
   private void test() {
      try {
         String glu = getGluRecord();
         File f = aladin.createTempFile("GluHealpix", ".dic");
         RandomAccessFile rf = new RandomAccessFile(f, "rw");
         rf.writeBytes(glu);
         rf.close();
         aladin.execCommand("load "+f.getAbsolutePath());
         f.deleteOnExit();
         aladin.dialog.show("Allsky");

      } catch( Exception e ) { e.printStackTrace();  }
      updateProperties();
   }

   // Demande la confirmation, puis sauvegarde l'enregistrement GLU correspondant au survey de l'utilisateur
   private void save() {
      String glu = getGluRecord();
      if( !aladin.confirmation(this, "Your data corresponds to this following registry description (GLU record) " +
            "and can be saved on your disk as small text file for distributing to your collaborators." +
            "Simply by loading this file in Aladin, your collaborators will immediately see your data and will be able to access them.\n\n" +
            "You can also send this file to the CDS team (cds-question@unistra.fr) in order to expose your data " +
            "to the whole astronomical community:\n \n"
            +glu+"\n \n" + "Generate this file ?") ) return;
      FileDialog fd = new FileDialog(aladin.dialog,"GLU Data record",FileDialog.SAVE);
      fd.setVisible(true);
      String dir = fd.getDirectory();
      String name =  fd.getFile();
      if( name==null ) return;
      try {
         if( name.trim().length()==0 ) return;
         File f = new File(dir,name);
         RandomAccessFile rf = new RandomAccessFile(f, "rw");
         rf.writeBytes(glu);
         rf.close();
         aladin.trace(3,"Glu record Allsky saved ["+f.getAbsolutePath()+"] !");
      } catch( Exception e ) { e.printStackTrace(); }
      updateProperties();
   }

   private void updateProperties() {
      try {
         context.writeMetaFile();
      } catch( Exception e ) { }
   }


   // Retourne l'enregistrement GLU en fonction des informations saisies
   // par l'utilisateur dans le formulaire, et des informations techniques
   // issus du survey
   // En profite pour mettre à jour les propriétés du context pour la mise à jour du fichier properties
   private String getGluRecord() {
      StringBuffer s = new StringBuffer();
      String id = field[0].getText().trim().replace('/','_');
      s.append( Util.align("%ActionName",15)+" "+id+".htx"+Util.CR);
      for( int i=0; i<GLUPARAM.length; i++ ) {
         String [] glup = GLUPARAM[i];
         String value = field[i].getText().trim();
         if( value.length()==0 ) continue;
         s.append( Util.align("%"+glup[FIELD],15)+" "+value+Util.CR);
         if( glup[KEY].length()>0 ) context.setPropriete(glup[KEY], value);
      }
      s.append( Util.align("%Aladin.XLabel",15) +" "+field[1].getText().trim()+Util.CR);
      s.append( Util.align("%Aladin.Profile",15) +" >6.1"+Util.CR);
      s.append( Util.align("%Aladin.HpxParam",15)+" "+getHpxParam()+Util.CR);

      context.setPropriete(Constante.KEY_OBS_COLLECTION,id);

      return s.toString();
   }

   // Retourne les paramètres HPX en fonction du survey
   private String getHpxParam() {
      //      return orderMax+" "+(isJpg?"jpeg":"")+" fits";
      return context.getOrder()+" "+context.getAvailableTileFormats();
   }

}

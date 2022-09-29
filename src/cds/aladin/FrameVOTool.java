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

package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import cds.tools.Util;

public class FrameVOTool extends JFrame implements ActionListener,KeyListener {

	// refrence à l'objet Aladin
	private Aladin aladin;
    private VOToolDescription voForm;
    private JList voList;
    private JButton install,run,remove;
    protected JButton apply;

    static void display(Aladin aladin) { display(aladin,null); }
    static void display(Aladin aladin,GluApp ap) {
       if( aladin.frameVOTool==null ) aladin.frameVOTool = new FrameVOTool(aladin);
       aladin.frameVOTool.setVisible(true);
       if( ap!=null ) aladin.frameVOTool.selectionne(ap);
    }

    static protected String INSTALL,NEWINSTALL,REINSTALL,REMOVE,INTERRUPT,NEW,__NEW__,
    WARNING,DELCONF,DOWNLOADMAN,DISCARD,NOYET,CANNOT,APPLY,RUN,CLOSE,JARMAN;

    // Appelé par Chaine directement (pas possible par le constructeur)
    protected void createChaine(Chaine chaine) {
       INSTALL="Install...";
       NEWINSTALL="Install new release...";
       REINSTALL="Re-install...";
       REMOVE="Delete...";
       INTERRUPT="Interrupt!";
       __NEW__="__New__";
       NEW = "New...";
       APPLY = "Apply";
       RUN = "Run...";
       CLOSE = "Close";
       WARNING ="This list shows a selection of VO tools compatible with Aladin. " +
            "These softwares are able to cooperate with Aladin by exchanging data, " +
            "cross selecting sources...\n" +
            "Select, install if required, or define yourself (Running directory + Command line) " +
            "the VO applications  directly runnable from the Aladin menu.\n" +
            "Contact directly the authors for any questions, bugs...";
       DELCONF = "You are going to delete local description of this VO tool.\n" +
            "Do you want to continue ?";
       DOWNLOADMAN = "This application requires a dedicated installation method.\n" +
            "Follow the author instructions and after you successfully installed\n" +
            "the application, specify manually the command line\n" +
            "and the running directory.";
       JARMAN = "Aladin is going to download this following jar file\n Do you want to continue ?\n \n";
       DISCARD = "Discard your modifications ?";
       NOYET = "This installation method is not supported\n" +
            "by your Aladin version";
       CANNOT = "Cannot install this application";
    }

	/** Constructeur */
	private FrameVOTool(Aladin aladin) {
	    super();
        Aladin.setIcon(this);
        createChaine(aladin.chaine);
	    setTitle("VOTool Application installer & controller");
	    this.aladin = aladin;

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Util.setCloseShortcut(this, false, aladin);

        addWindowListener(new WindowAdapter() {
           public void windowClosing(WindowEvent e) { close(); }
        });

	    getContentPane().setLayout(new BorderLayout(5,5));
        getContentPane().add(createPanel(), "Center");

        setLocation(50,100);
        pack();
	}

    private JPanel createPanel() {
       JTextArea t;
       JButton b;
       JPanel p = new JPanel(new BorderLayout(5,5));
       voList = new JList(new VOList());
       voList.setVisibleRowCount(10);
       voList.setFixedCellWidth(100);
       voList.addMouseListener(new MouseAdapter() {
          public void mouseReleased(MouseEvent e) { selectionne(); }
       });
       p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
       p.add(t=new JTextArea(WARNING),BorderLayout.NORTH);
       t.setWrapStyleWord(true);
       t.setLineWrap(true);
       t.setEditable(false);
       t.setFont(Aladin.ITALIC);
       t.setBackground(getContentPane().getBackground());

       p.add(new JScrollPane(voList),BorderLayout.WEST);
       p.add(voForm=new VOToolDescription(aladin,this),BorderLayout.CENTER);

       JPanel p1 = new JPanel();
       p1.add(install=b=new JButton(INSTALL));
       b.setFont(b.getFont().deriveFont(Font.BOLD));
       b.setEnabled(false);
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { install(); }
       });
       p1.add(apply=b=new JButton(APPLY));
       b.setEnabled(false);
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { apply(); }
       });
       p1.add(run=b=new JButton(RUN));
       b.setEnabled(false);
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { run(); }
       });
       p1.add(remove=b=new JButton(REMOVE));
       b.setEnabled(false);
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { remove(); }
       });
       p1.add(new JLabel("      "));
       p1.add(b=new JButton(NEW));
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { nouveau(); }
       });

       p1.add(b=new JButton(CLOSE));
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { close(); }
       });
       p.add(p1,BorderLayout.SOUTH);

       return p;
    }

    /** Positionne l'état des boutons */
    protected void setButtonMode(GluApp ap) {
       install.setEnabled(ap!=null && ap.canBeInstall());
       run.setEnabled(ap!=null && ap.canBeRun());
       if( ap!=null ) {
          if( ap.isDownloading() ) install.setText(INTERRUPT);
          else install.setText( ap.hasNewRelease() ? NEWINSTALL : ap.dir!=null ? REINSTALL : INSTALL );
       }
       apply.setEnabled(ap!=null && voForm.hasBeenChanged());
       remove.setEnabled(ap!=null && !ap.isDownloading() );
    }

    /** Prise en compte des modifications utilisateurs du formulaire */
    private void apply() {
       GluApp ap = voForm.apply();
       setButtonMode(ap);

       // Changement du nom temporaire dans la liste
       if( ap.tagGlu.equals(__NEW__ ) ) {
          int i = ap.aladinLabel.indexOf(' ');
          if( i==-1 ) i=ap.aladinLabel.indexOf(':');
          if( i==-1 ) i=ap.aladinLabel.indexOf('-');
          if( i==-1 ) i=ap.aladinLabel.length();
          ap.tagGlu=ap.aladinLabel.substring(0,i);
          listUpdate();
       }

       aladin.glu.writeGluAppDic();
       aladin.VOReload();
    }

    /** Fermeture de la fenêtre */
    private void close() {
       if( !discard() ) return;
       setVisible(false);
    }

    /** Fin d'un téléchargement */
    protected void downloadEnd() {
       aladin.glu.writeGluAppDic();
       aladin.VOReload();
       selectionne();
    }

    /** Retourne true si l'utilisateur accepte de perdre le smodifs qu'il vient de faire */
    private boolean discard() {
       if( voForm.hasBeenChanged() ) {
          return aladin.confirmation(this, DISCARD);
       }
       return true;
    }

    /** Montre la description du plugin correspondant à la sélection dans la liste */
    protected void selectionne() {
       String name = (String)voList.getSelectedValue();
       GluApp ap = aladin.glu.getGluApp( aladin.glu.findGluApp(name) );
       voForm.setEditable(false);
       if( voForm.vo!=ap && !discard() ) return;
       voForm.setVOtool(ap);
       if( ap.tagGlu.equals(__NEW__) ) voForm.setEditable(true);
       setButtonMode(ap);
    }

    /** Selectionne dans la liste une application particulière
     * et met à jour le formulaire */
    protected void selectionne(GluApp ap) {
       int i = aladin.glu.findGluApp(ap.tagGlu);
       if( i<0 ) return;
       voList.setSelectedIndex(i);
       voForm.setVOtool(ap);
       setButtonMode(ap);
    }

    /** Formulaire vide pour la définition d'une nouvelle application VO */
    private void nouveau() {
       if( !discard() ) return;
       GluApp ap = aladin.glu.addApplication(__NEW__);
       listUpdate();
       selectionne(ap);
       voForm.setEditable(true);
    }

    /** Montre la description du plugin correspondant à la sélection dans la liste */
    private void install() {
       GluApp ap = voForm.vo;
       if( ap.isDownloading() ) { ap.interrupt(); return; }
       if( ap.getInstallMode()==GluApp.JAR
             && !aladin.confirmation(this, JARMAN+
                   "\n - Url: "+ap.jarUrl+"\n - Target: "+aladin.getVOPath()+"\n") ) return;
       int rep = ap.install();
       switch( rep ) {
          case 0 : aladin.error(this,CANNOT); break;
          case -1: Util.pause(1000); aladin.info(this,DOWNLOADMAN); break;
          case -2: aladin.error(this,NOYET); break;
       }
       aladin.glu.writeGluAppDic();
       aladin.log("VOinstall", ap.tagGlu+" "+(rep==1?"Ok":"Error"));
       voForm.setVOtool(ap);
       setButtonMode(ap);
    }

    /** Run l'application */
    private void run() {
       GluApp ap = voForm.vo;
       ap.exec();
    }

    /** Mise à jour de la liste */
    private void listUpdate() {
       ((VOList)voList.getModel()).listListener.contentsChanged(
             new ListDataEvent(this,ListDataEvent.CONTENTS_CHANGED,0,
                                   aladin.glu.vGluApp.size()));
    }

    /** Suppression d'une application */
    private void remove() {
       String name = (String)voList.getSelectedValue();
       if( !aladin.confirmation(this, DELCONF) ) return;
       aladin.glu.removeGluApp(name);
       voList.setSelectedIndex(0);
       listUpdate();
       selectionne();
       aladin.glu.writeGluAppDic();
       aladin.VOReload();
    }

    /** Gère la liste des applications disponibles */
    class VOList implements ListModel {

       public Object getElementAt(int index) {
          return ((GluApp)aladin.glu.vGluApp.elementAt(index)).tagGlu;
       }

       public int getSize() { return aladin.glu.vGluApp.size(); }

       public void removeListDataListener(ListDataListener l) { }

       protected ListDataListener listListener;
       public void addListDataListener(ListDataListener l) {
          listListener=l;
       }
    }

    public void actionPerformed(ActionEvent e) { setButtonMode(voForm.vo); }
    public void keyPressed(KeyEvent e) {  }
    public void keyReleased(KeyEvent e) { setButtonMode(voForm.vo); }
    public void keyTyped(KeyEvent e) { }

}

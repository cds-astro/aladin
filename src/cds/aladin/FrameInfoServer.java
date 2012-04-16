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

import cds.tools.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Gestion de la fenêtre d'affichage des infos sur un serveur ainsi que le status
 * de la dernière requête
 * @date 29 nov 2005 - création
 * @author P. Fernique
 */
public class FrameInfoServer extends JFrame implements ActionListener {

   static String TITLE,CLOSE,INFO,/*TEST,*/TYPE,DESC,MORE,
                 ORIGIN,LASTQUERY,STATUS,ERRORMSG,IDENTIFIER;

	private JTextArea ta;
	private JLabel nom;
	private JButton btInfo/*,btTest*/;
	private Aladin aladin;
	private Server server;

	protected void createChaine() {
	   TITLE = aladin.chaine.getString("ISTITLE");
	   CLOSE = aladin.chaine.getString("CLOSE");
	   INFO = aladin.chaine.getString("ISINFO");
//	   TEST = aladin.chaine.getString("ISTEST");
	   TYPE = aladin.chaine.getString("ISTYPE");
	   DESC = aladin.chaine.getString("ISDESC");
	   MORE = aladin.chaine.getString("ISMORE");
	   IDENTIFIER = aladin.chaine.getString("ISIDENTIFIER");
	   ORIGIN = aladin.chaine.getString("ISORIGIN");
	   LASTQUERY = aladin.chaine.getString("ISLASTQUERY");
	   STATUS = aladin.chaine.getString("ISSTATUS");
	   ERRORMSG = aladin.chaine.getString("ERROR");
	}

	protected FrameInfoServer(Aladin aladin) {
	   super();
	   this.aladin = aladin;
       Aladin.setIcon(this);
       JButton b;

       enableEvents(AWTEvent.WINDOW_EVENT_MASK);
       Util.setCloseShortcut(this, false, aladin);

	   createChaine();
	   setTitle(TITLE);
	   ta = new JTextArea(20,85);
	   ta.setFont( Aladin.COURIER );
	   ta.setBackground(Color.white);
	   ta.setEditable(false);
	   JScrollPane js = new JScrollPane(ta);

	   JPanel tnom = new JPanel(new FlowLayout(FlowLayout.CENTER));
	   nom = new JLabel(TITLE);
	   nom.setFont(Aladin.LLITALIC);
	   nom.setForeground(Aladin.GREEN);
	   tnom.add(nom);

	   JPanel submit = new JPanel();
	   submit.add( btInfo=b= new JButton(INFO)); b.addActionListener(this);
//	   submit.add( btTest=b= new Jutton(TEST));  b.addActionListener(this);
	   submit.add( b=new JButton(CLOSE));        b.addActionListener(this);

	   getContentPane().add(tnom,"North");
       getContentPane().add(js,"Center");
       getContentPane().add(submit,"South");

       setLocation(aladin.computeLocation(this));
	}

	private String A(String s) { return Util.align(s,14)+": "; }

	/** Affichage des infos associées à un serveur */
	protected void show(Server s) {
	   server = s;
//	   btTest.setEnabled(server.statusUrl!=null);
	   btInfo.setEnabled(server.docUser!=null);

	   nom.setText(s.aladinLabel.replace('\n',' '));
	   
	   String gluRecord=null;
	   if( s instanceof ServerGlu ) {
	      gluRecord = "GLU record:\n"+((ServerGlu)s).record.toString();
	   }
	   
	   ta.setText(
                                    "\n"+
                                 (s.description!=s.aladinLabel ? A(DESC)+Util.fold(s.description,70)+"\n":"")+
(s.type!=Server.APPLI && s.type!=Server.APPLIIMG? A(TYPE)+s.getType()+"\n":"")+
           (s.docUser!=null && s.docUser!=s.aladinLabel ? A(MORE)+s.docUser+"\n":"")+
(s.institute!=null && s.institute!=s.aladinLabel && s.institute!=s.description? A(ORIGIN)+s.institute+"\n":"")+
     (s.type!=Server.APPLI && s.type!=Server.APPLIIMG && s.statusUrl!=null ? A(LASTQUERY)+s.statusUrl+"\n":"")+
(s.type==Server.APPLI || s.type==Server.APPLIIMG? "":A(STATUS)+(s.statusAllVO!=null ? s.statusAllVO.getText()+"\n":"Not yet tested\n"))+
                           (s.statusError!=null ? A(ERRORMSG)+s.statusError+"\n":"")+
                           (s instanceof ServerGlu ? A(IDENTIFIER)+((ServerGlu)s).actionName+"\n" : "")+
                           (s.verboseDescr!=null ? "\n"+Util.fold(s.verboseDescr)+"\n":"")+
                           (gluRecord!=null ? "\n\n"+gluRecord:"")
                    );
	   ta.setCaretPosition(0);
	   pack();
	   setVisible(true);
	}

	// Gestion des evenements
	public void actionPerformed(ActionEvent evt) {
       String what = evt.getActionCommand();
	   if( what.equals(CLOSE) ) setVisible(false);
//	   else if( what.equals(TEST) ) aladin.glu.showDocument("Http",server.statusUrl,true);
	   else if( what.equals(INFO) ) aladin.glu.showDocument("Http",server.docUser,true);
	}

}

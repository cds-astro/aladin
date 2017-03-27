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

import static cds.aladin.Constants.INFOGUI;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import cds.tools.Util;

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
	private Future<JPanel> additionalComponent;
	private JPanel centerPanel;
	private boolean flagUpdate;
	private int guiType; //0- simple, 1 for ServerTap with metadata table

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
	   this.guiType = 0;
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
	   nom.setForeground(Aladin.COLOR_GREEN);
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
	
	/**
	 * @wbp.parser.constructor
	 */
	protected FrameInfoServer(Aladin aladin, Future<JPanel> infoPanel) {
		super();
		this.aladin = aladin;
		this.guiType = 1;
		Aladin.setIcon(this);
		JButton b;

		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);

		createChaine();
		setTitle(TITLE);
		ta = new JTextArea(20, 85);
		ta.setFont(Aladin.COURIER);
		ta.setBackground(Color.white);
		ta.setEditable(false);
		JScrollPane js = new JScrollPane(ta);
//		js.setBounds(10, 10, 800, 200);
//		js.setSize(new Dimension(800, 200));

		this.centerPanel = new JPanel();
//		BoxLayout boxLayout = new BoxLayout(this.centerPanel, BoxLayout.Y_AXIS);
		GridBagLayout gridbag = new GridBagLayout();
		this.centerPanel.setLayout(gridbag);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		c.weighty = 0.20;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0); 
		
		this.centerPanel.add(js, c);
		
		this.additionalComponent = infoPanel;
	    //this.additionalComponent.setBounds(10, 50, 800, 600);
		JScrollPane mainScrollPane;
		try {
			mainScrollPane = new JScrollPane(this.additionalComponent.get());
			mainScrollPane.setName(INFOGUI);
//			mainScrollPane.setSize(new Dimension(800, 300));
//			mainScrollPane.setBounds(10, 60, 800, 300);
			mainScrollPane.getVerticalScrollBar().setUnitIncrement(4);
			
			c.gridy = 1;
			c.weighty = 0.80;
			c.gridheight = 2;
			this.centerPanel.add(mainScrollPane, c);
        } catch (InterruptedException e) {
           e.printStackTrace();
       } catch (ExecutionException e) {
           e.printStackTrace();
		}
		

		JPanel tnom = new JPanel(new FlowLayout(FlowLayout.CENTER));
		nom = new JLabel(TITLE);
		nom.setFont(Aladin.LLITALIC);
		nom.setForeground(Aladin.COLOR_GREEN);
		tnom.add(nom);
	    
		JPanel submit = new JPanel();
		submit.add(btInfo = b = new JButton(INFO));
		b.addActionListener(this);
		// submit.add( btTest=b= new Jutton(TEST)); b.addActionListener(this);
		submit.add(b = new JButton(CLOSE));
		b.addActionListener(this);

		getContentPane().add(tnom,"North");
	    getContentPane().add(this.centerPanel,"Center");
	    getContentPane().add(submit,"South");
	    
		setLocation(aladin.computeLocation(this));
	}
	
	
	/**
	 * Updates the info panel gui
	 * @throws Exception 
	 */
	public void updateInfoPanel() throws Exception {
		Component[] components = this.centerPanel.getComponents();
		for (Component component : components) {
			if (component.getName()!=null && component.getName().equals(INFOGUI)) {
				JScrollPane oldScrollPane = (JScrollPane) component;
				try {
					JScrollPane  mainScrollPane= new JScrollPane(this.additionalComponent.get());
					mainScrollPane.setName(INFOGUI);
					mainScrollPane.setBounds(10, 10, 800, 600);
					mainScrollPane.getVerticalScrollBar().setUnitIncrement(4);
					this.getContentPane().remove(this.centerPanel);
					this.centerPanel.remove(oldScrollPane);
					this.centerPanel.add(mainScrollPane);
					this.getContentPane().add(this.centerPanel,"Center");
					break;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw e;
				}
			}
		}
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

	public Future<JPanel> getAdditionalComponent() {
		return additionalComponent;
	}

	public void setAdditionalComponent(Future<JPanel> additionalComponent) {
		this.additionalComponent = additionalComponent;
	}

	public boolean isFlagUpdate() {
		return flagUpdate;
	}

	public void setFlagUpdate(boolean flagUpdate) {
		this.flagUpdate = flagUpdate;
	}

	public int getGuiType() {
		return guiType;
	}

	public void setGuiType(int guiType) {
		this.guiType = guiType;
	}
	
	public boolean isOfTapServerType() {
		boolean typeIsTap = false;
		if (this.guiType == 1) {
			typeIsTap = true;
		}
		return typeIsTap;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}


}

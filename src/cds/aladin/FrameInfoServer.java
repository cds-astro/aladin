// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import static cds.aladin.Constants.INFOGUI;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

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
	private MySplitPane centerPanel;
	private int flagUpdate;// if 1- then it needs update
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

		this.additionalComponent = infoPanel;
	    //this.additionalComponent.setBounds(10, 50, 800, 600);
		JScrollPane mainScrollPane;
		try {
			mainScrollPane = new JScrollPane(this.additionalComponent.get());
			mainScrollPane.setName(INFOGUI);
//			mainScrollPane.setSize(new Dimension(800, 300));
//			mainScrollPane.setBounds(10, 60, 800, 300);
			mainScrollPane.getVerticalScrollBar().setUnitIncrement(4);
			
			this.centerPanel = new MySplitPane(aladin, JSplitPane.VERTICAL_SPLIT, js, mainScrollPane, 1);
//			this.centerPanel.setDividerSize(3);
			this.centerPanel.setBackground(Aladin.BLUE);
			js.setMinimumSize(new Dimension(800, 200));
			js.setPreferredSize(new Dimension(800, 300));
        } catch (InterruptedException e) {
        	//TODO::
           e.printStackTrace();
       } catch (ExecutionException e) {
    	   //TODO::
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
	    
		setLocation(Aladin.computeLocation(this));
	}
	
	
	/**
	 * Updates the info panel gui
	 * @throws Exception 
	 */
	public void updateInfoPanel() throws Exception {
		Component[] components = this.centerPanel.getComponents();
		for (Component component : components) {
			if (component.getName() != null && component.getName().equals(INFOGUI)) {
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
                           (s instanceof ServerGlu ? A(IDENTIFIER)+((ServerGlu)s).gluTag+"\n" : "")+
                           (s.verboseDescr!=null ? "\n"+Util.fold(s.verboseDescr)+"\n":"")+
                           (gluRecord!=null ? "\n\n"+gluRecord:"")
                    );
	   ta.setCaretPosition(0);
//	   tapInfoText(true);
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

	public int isFlagUpdate() {
		return flagUpdate;
	}

	public void setFlagUpdate(int flagUpdate) {
		this.flagUpdate = flagUpdate;
	}

	public int getGuiType() {
		return guiType;
	}

	public void setGuiType(int guiType) {
		this.guiType = guiType;
	}
	
	public boolean isOfDynamicTapServerType() {
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

	public boolean isThisInfoPanel(TapClient tapClient) {
		// TODO Auto-generated method stub
		boolean result = false;
		if (this.isOfDynamicTapServerType() && this.server != null && this.server.tapClient != null) {
			if (this.additionalComponent != null && tapClient.infoPanel != null
					&& this.additionalComponent.equals(tapClient.infoPanel)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Method facilitates hiding of server form specific component: tap description 
	 * so the info panel can be repurposed to show just generic tap meta info
	 * @param show
	 */
	public void showHidetapInfoText(boolean show) {
		// TODO Auto-generated method stub
		if (!isVisible() || (show != this.centerPanel.getTopComponent().isVisible())) {
			this.centerPanel.getTopComponent().setVisible(show);
			if (!show) {
				int dividerSize = (Integer) UIManager.get("SplitPane.dividerSize");
				double proportionalLocation = this.centerPanel.getHeight() > 1500 ? 0.05 : 0.2; //Magic numbers based on info panel size
				this.centerPanel.setDividerSize(dividerSize);
				this.centerPanel.setDividerLocation(proportionalLocation);
			} 
			this.centerPanel.resetToPreferredSizes();
			this.centerPanel.revalidate();
			this.centerPanel.repaint();
		}
	}


}

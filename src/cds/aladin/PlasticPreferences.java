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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/** Une classe pour gérer les préférences liées à PLASTIC
 * 
 * @author Thomas Boch [CDS]
 * @version 0.1 kickoff August 2006
 * 
 */
public class PlasticPreferences extends JFrame implements ActionListener {

	// Les mots clés possibles
	protected static final String PREF_AUTOCONNECT  = "PlasticAutoconnect";
	protected static final String PREF_LAUNCHHUB    = "PlasticLaunchHub";
	protected static final String PREF_SENDPOS      = "PlasticSendPos";
	protected static final String PREF_SELECT       = "PlasticSelect";
	protected static final String PREF_HIGHLIGHT    = "PlasticHighlight";
	
	// les différentes chaines nécessaires
	static String TITLE, DEFAUTOCONNECT, AUTOCONNECT, DEFLAUNCHHUB, LAUNCHHUB, DEFSELECTOBJECTS, SELECTOBJECTS,
			DEFHIGHLIGHTOBJECT, HIGHLIGHTOBJECT, DEFSENDPOSITION, SENDPOSITION, APPLY, CLOSE;
	
	private JCheckBox autoconnectCb, launchhubCb, selectobjectsCb, highlightCb, sendpositionCb;
	
	// référence à aladin
	private Aladin a;
	
	public PlasticPreferences(Aladin a) {
		super();
		this.a = a;
		Aladin.setIcon(this);
//		setBackground(Aladin.BKGD);
        
		addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent evt) {
	            setVisible(false);
	        }
	    });
		
		((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5,7,5,7));
		
		buildFrame();
	}
	
	private void buildFrame() {
		createChaine();
		
		setTitle(TITLE);
        getContentPane().setLayout(new BorderLayout(5, 5));
        // le titre
        JLabel info = new JLabel(TITLE, JLabel.CENTER);
        info.setFont(Aladin.LITALIC);
        Aladin.makeAdd(getContentPane(), info, "North");
        
        Aladin.makeAdd(getContentPane(), createPanel(), "Center");
        Aladin.makeAdd(getContentPane(), getValidPanel(), "South");
        pack();
	}
	
	/** Construction du panel des prefs PLASTIC
	 * 
	 * @return
	 */
	private JPanel createPanel() {
	      GridBagConstraints c = new GridBagConstraints();
	      GridBagLayout g = new GridBagLayout();
	      c.fill = GridBagConstraints.BOTH;

	      JPanel p = new JPanel();
	      p.setLayout(g);

	      // propriété autoconnect
	      Properties.addFilet(p, g, c);
	      Properties.addSectionTitle(p, DEFAUTOCONNECT, g, c);
	      autoconnectCb = new JCheckBox(AUTOCONNECT);
	      Properties.addCouple(p, "", autoconnectCb, g, c);
	      
	      // propriété launchhub at startup
	      Properties.addFilet(p, g, c);
	      Properties.addSectionTitle(p, DEFLAUNCHHUB, g, c);
	      launchhubCb = new JCheckBox(LAUNCHHUB);
	      Properties.addCouple(p, "", launchhubCb, g, c);
	      
	      // propriété selectobjects
	      Properties.addFilet(p, g, c);
	      Properties.addSectionTitle(p, DEFSELECTOBJECTS, g, c);
	      selectobjectsCb = new JCheckBox(SELECTOBJECTS);
	      Properties.addCouple(p, "", selectobjectsCb, g, c);
	      
	      // propriété highlightobject
	      Properties.addFilet(p, g, c);
	      Properties.addSectionTitle(p, DEFHIGHLIGHTOBJECT, g, c);
	      highlightCb = new JCheckBox(HIGHLIGHTOBJECT);
	      Properties.addCouple(p, "", highlightCb, g, c);

	      // propriété sendposition
	      Properties.addFilet(p, g, c);
	      Properties.addSectionTitle(p, DEFSENDPOSITION, g, c);
	      sendpositionCb = new JCheckBox(SENDPOSITION);
	      Properties.addCouple(p, "", sendpositionCb, g, c);
	      
	      Properties.addFilet(p, g, c);
	      
	      return p;
	}
	
	private JPanel getValidPanel() {
	       JPanel p = new JPanel();
	       p.setLayout( new FlowLayout(FlowLayout.CENTER));
	       p.setFont( Aladin.LBOLD );
           JButton b;
	       p.add(b = new JButton(APPLY));
           b.addActionListener(this);
	       p.add(b = new JButton(CLOSE));
           b.addActionListener(this);
	       return p;
	    }
	
	// Gestion des evenements
	public void actionPerformed(ActionEvent ae) {
        String what = ae.getActionCommand();
		if( CLOSE.equals(what) ) setVisible(false);
		else if( APPLY.equals(what) ) {
		try { if( apply() ) setVisible(false); }	   
		   catch( Exception e ) { Aladin.warning(this," "+e.getMessage(),1); }
		}
	}
	
	/** validation des préférences choisies par l'utilisateur
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean apply() {
		// autoconnect
		a.configuration.set( PREF_AUTOCONNECT, String.valueOf(autoconnectCb.isSelected()) );
		
		// launch internal hub if needed
		a.configuration.set( PREF_LAUNCHHUB, String.valueOf(launchhubCb.isSelected()) );
		
		// select objects
		a.configuration.set( PREF_SELECT, String.valueOf(selectobjectsCb.isSelected()) );
		
		// highlight object
		a.configuration.set( PREF_HIGHLIGHT, String.valueOf(highlightCb.isSelected()) );
		
		// send position
		a.configuration.set( PREF_SENDPOS, String.valueOf(sendpositionCb.isSelected()) );
		
		a.saveConfig();
		
		return true;
	}
	
	protected void showPrefs() {
		updateCbState();
		
		this.setVisible(true);
		this.toFront();
	}
	
	protected boolean getBooleanValue(String propName) {
		String s = a.configuration.get(propName);
		return s==null?getDefaultValue(propName):new Boolean(s).booleanValue();
	}
	
	// TODO : à améliorer pour ne pas retourner true si la prop n'est pas une prop plastic
	private boolean getDefaultValue(String propName) {
		return true;
	}
	
	/**
	 * mise à jour de l'état des checkbox selon les préférences enregistrées
	 *
	 */
	private void updateCbState() {
		boolean state;
		String s;
		
		// autoconnect
		s = a.configuration.get(PREF_AUTOCONNECT);
		state = s==null?getDefaultValue(PREF_AUTOCONNECT):new Boolean(s).booleanValue();
		autoconnectCb.setSelected(state);
		
		// launch internal hub if needed
		s = a.configuration.get(PREF_LAUNCHHUB);
		state = s==null?getDefaultValue(PREF_LAUNCHHUB):new Boolean(s).booleanValue();
		launchhubCb.setSelected(state);
		
		// select objects
		s = a.configuration.get(PREF_SELECT);
		state = s==null?getDefaultValue(PREF_SELECT):new Boolean(s).booleanValue();
		selectobjectsCb.setSelected(state);
		
		// highlight object
		s = a.configuration.get(PREF_HIGHLIGHT);
		state = s==null?getDefaultValue(PREF_HIGHLIGHT):new Boolean(s).booleanValue();
		highlightCb.setSelected(state);
		
		// send position
		s = a.configuration.get(PREF_SENDPOS);
		state = s==null?getDefaultValue(PREF_SENDPOS):new Boolean(s).booleanValue();
		sendpositionCb.setSelected(state);
		
	}
	
	private void createChaine() {
		TITLE = a.chaine.getString("PPTITLE");
		DEFAUTOCONNECT = a.chaine.getString("PPDEFAUTOCONNECT");
		AUTOCONNECT = a.chaine.getString("PPAUTOCONNECT");
		DEFLAUNCHHUB = a.chaine.getString("PPDEFLAUNCHHUB");
		LAUNCHHUB = a.chaine.getString("PPLAUNCHHUB");
		DEFSELECTOBJECTS = a.chaine.getString("PPDEFSELECTOBJECTS");
		SELECTOBJECTS = a.chaine.getString("PPSELECTOBJECTS");
		DEFHIGHLIGHTOBJECT = a.chaine.getString("PPDEFHIGHLIGHTOBJECT");
		HIGHLIGHTOBJECT = a.chaine.getString("PPHIGHLIGHTOBJECT");
		DEFSENDPOSITION = a.chaine.getString("PPDEFSENDPOSITION");
		SENDPOSITION = a.chaine.getString("PPSENDPOSITION");
	    APPLY = a.chaine.getString("UPAPPLY");
	    CLOSE = a.chaine.getString("UPCLOSE"); 
	}
	
}

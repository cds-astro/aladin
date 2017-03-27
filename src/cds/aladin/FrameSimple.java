package cds.aladin;

import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;

import java.awt.AWTEvent;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cds.aladin.Constants.TapServerMode;
import cds.tools.Util;


/**
 * This class is to show the upload frame for Tap servers
 *
 */
public class FrameSimple extends JFrame implements ActionListener, GrabItFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3541428440636805284L;
	
	Aladin aladin;
	Server server;
	JPanel buttonsPanel;
	
	protected FrameSimple() {
		super();
	}
	
	/**
	 * All you have is only one tapserver frame. 
	 * So we will need the argument during frame construction
	 * @param url 
	 * @wbp.parser.constructor
	 */
	protected FrameSimple(Aladin aladin) {
		super();
		this.aladin = aladin;
		Aladin.setIcon(this);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		setLocation(Aladin.computeLocation(this));
		setFont(Aladin.PLAIN);
	}
	
	/** Affichage des infos associées à un serveur */
	protected void show(Server s, String title) {
		if (s != this.server) {
			setTitle(title);
			if (this.server == null) {
				aladin.grabUtilInstance.grabItServers.add(s);
			} else {
				aladin.grabUtilInstance.removeAndAdd(this.server, s);
			}
			this.server = s;
			createFrame();
			this.server.updateWidgets(this);// to make sure grab is instantiated right
			pack();
		}
		setVisible(true);
	}
	
	/**
	 * Creates the first form for loading upload data
	 * @param server2 
	 */
	public void createFrame() {
//		this.getContentPane().setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		this.getContentPane().removeAll();
		this.getContentPane().setBackground(Aladin.COLOR_MAINPANEL_BACKGROUND);
		this.getContentPane().add(this.server, "Center");
		this.getContentPane().revalidate();
		this.getContentPane().repaint();
		
		/*buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton submit = new JButton("Submit");
		submit.addActionListener(this);
		submit.setActionCommand("SUBMIT");
		buttonsPanel.add(submit);
		
		this.getContentPane().add(buttonsPanel, "South");*/
//		setSize(700, 500);
	}
	
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (command.equals("SUBMIT")) {
			server.submit();
		}
	}

	@Override
	public void setGrabItCoord(double x, double y) {
		GrabUtil.setGrabItCoord(aladin, server, x, y);
	}

	@Override
	public void stopGrabIt() {
	    GrabUtil.stopGrabIt(aladin, this, server);
	}

	/**
	    * Retourne true si le bouton grabit du formulaire existe et qu'il est
	    * enfoncé
	    */
	@Override
	public boolean isGrabIt() {
	      return (server.modeCoo != Server.NOMODE
	            && server.grab != null && server.grab.getModel().isSelected());
	   }

	@Override
	public void setGrabItRadius(double x1, double y1, double x2, double y2) {
		GrabUtil.setGrabItRadius(aladin, server, x1, y1, x2, y2);
	}


}

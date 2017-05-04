/**
 * 
 */
package cds.aladin;
import static cds.aladin.Constants.GENERIC;
import static cds.aladin.Constants.GLU;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTCREATEDGUI;

import java.awt.Color;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import cds.aladin.Constants.TapClientMode;

/**
 * @author chaitra
 *
 */
public class TapClient implements ActionListener{
	
	public static String[] modeIconImages = { "gluIconV2.png", "genericIconV2.png" };
	public static String[] modeIconToolTips = { "Click to load GLU mode", "Click to load a generic tap client" };
	public static String[] modeChoices = { GLU, GENERIC };
	public static String TAPGLUGENTOGGLEBUTTONTOOLTIP;
	
	public TapManager tapManager;
	public String tapLabel;
	public String tapBaseUrl;
	public ServerGlu serverGlu;
	public ServerTap serverTap;
	public boolean isGluSelected;
	public TapClientMode mode;
	
	
	static {
		TAPGLUGENTOGGLEBUTTONTOOLTIP = Aladin.chaine.getString("TAPGLUGENTOGGLEBUTTONTOOLTIP");
	}
	
	public TapClient(TapClientMode mode, TapManager tapManager, String tapLabel, String tapBaseUrl) {
		// TODO Auto-generated constructor stub
		this.mode = mode;
		this.tapManager = tapManager;
		this.tapLabel = tapLabel;
		this.tapBaseUrl = tapBaseUrl;
	}
	
	public TapClient(TapClientMode mode, TapManager tapManager, String tapLabel, String tapBaseUrl, ServerGlu serverGlu, ServerTap serverTap) {
		// TODO Auto-generated constructor stub
		this(mode, tapManager, tapLabel, tapBaseUrl);
		this.serverGlu = serverGlu;
		this.serverTap = serverTap;
	}

	public JToggleButton getGluModeButton() {
		JToggleButton changeModeButton = null;
		Image image = Aladin.aladin.getImagette("gluIconV5.png");
		if (image == null) {
			changeModeButton = new JToggleButton(GLU);
		} else {
			changeModeButton = new JToggleButton(new ImageIcon(image));
		}
		changeModeButton.setToolTipText(TAPGLUGENTOGGLEBUTTONTOOLTIP);
		changeModeButton.setMargin(new Insets(0, 0, 0, 0));
		if (this.serverGlu == null) {
			changeModeButton.setVisible(false);
		}
		changeModeButton.setBorderPainted(false);
		changeModeButton.setContentAreaFilled(true);
		changeModeButton.setSelected(isGluSelected);
		changeModeButton.addActionListener(this);
		return changeModeButton;
	}
	
	public void enableModes() {
		if (this.serverTap != null && this.serverTap.modeChoice != null) {
			this.serverTap.modeChoice.setVisible(true);
			this.serverTap.modeChoice.revalidate();
			this.serverTap.modeChoice.repaint();
		}
	}
	
	/**
	 * Precedence is for serverGlu. So by default serverGlu this displayed
	 * In case there is not ServerGlu configured for the tap client then 
	 * generic client is displayed
	 * @param serverType - trumps priority. if specified returns the server asked for
	 * @return
	 * @throws Exception 
	 */
	public Server getServerToDisplay(String serverType) throws Exception {
		Server resultServer = null;
		if ((serverType == null || serverType == GLU ) && this.serverGlu != null ) {
			resultServer = this.serverGlu;
			isGluSelected = true;
			resultServer.modeChoice.setSelected(true);
		} else if (serverType == null || serverType == GENERIC ) {
			isGluSelected = false;
			if (this.serverTap == null) {
				this.serverTap = tapManager.createAndLoadServerTap(this);
			} else if (this.serverTap.formLoadStatus == TAPFORM_STATUS_NOTCREATEDGUI) {
				this.serverTap.showloading();
				if (this.mode == TapClientMode.TREEPANEL) {
					this.serverTap.primaryColor = Aladin.COLOR_FOREGROUND;
					this.serverTap.secondColor = Color.white;
				}
				tapManager.createGenericTapFormFromMetaData(this.serverTap);
			} else if(this.serverTap.modeChoice != null){
				this.serverTap.modeChoice.setSelected(isGluSelected);
			}
			resultServer = this.serverTap;
		} else {
			Aladin.warning("Error! unable load glu tap client!");
		}
		return resultServer;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() instanceof JToggleButton) {
			try {
				JToggleButton changeMode = (JToggleButton) e.getSource();
				String command = GLU;
				if (!changeMode.isSelected()) {
					command = GENERIC;
				}
				Server serverToDisplay = getServerToDisplay(command);
				if (this.mode == TapClientMode.TREEPANEL) {
					tapManager.showTapPanelFromTree(tapLabel, serverToDisplay);
				} else{
					tapManager.showTapServerOnServerSelector(serverToDisplay);
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				Aladin.warning("Error! unable load tap server!"+e1.getMessage());
			}
			
		}
	}

	/**
	 * Reloads the generic tap client
	 * @throws Exception 
	 */
	public void reload() throws Exception {
		// TODO Auto-generated method stub
		this.serverTap = tapManager.createAndLoadServerTap(this);
		if (this.mode == TapClientMode.TREEPANEL) {
			tapManager.showTapPanelFromTree(tapLabel, this.serverTap);
		} else {
			tapManager.showTapServerOnServerSelector(this.serverTap);
		}
	}
	
}

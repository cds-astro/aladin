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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cds.tools.Util;



/** Widget permettant de visualiser rapidement l'état de connexion au hub PLASTIC
 *  En cliquant dessus, on peut également se connecter/déconnecter du hub,
 *  et avoir accès à divers items en rapport avec PLASTIC
 *
 *
 * @author Thomas Boch [CDS]
 *
 * @version 0.1  Creation, 26 May 2006
 *
 */
public final class PlasticWidget extends JComponent  implements
          MouseMotionListener, MouseListener,
          KeyListener, ActionListener
          {
	static String DISCONNECTED, CONNECTED_ALONE, CAN_TRANSMIT, TRANSMITTING;

    // différentes couleurs
    static final Color WAVE_OFF_COLOR = new Color(111,12,187);
    static final Color WAVE_ON_COLOR = /*new Color(242,19,197)*/Color.red;

	// lignes constituant la tour (codé sur 13 bits)
	static final int[] TOWER_LINES = {0x0040, 0x00E0, 0x0040, 0x00E0, 0x0040, 0x00E0, 0x00A0, 0x00E0, 0x01F0, 0x00A0, 0x01F0, 0x01F0, 0x0150, 0x03F8, 0x03F8, 0x02A8, 0x071C, 0x0514, 0x07FC, 0x071C, 0x0E0E, 0x0E0E, 0x1C07};

	// colonnes constituant les 'ondes' (codé sur 13 bits)
	static final int[] WAVE_LINES = {0x00A0, 0x0040, 0x0110, 0x00E0, 0x0208, 0x0110, 0x00E0, 0x0404, 0x0318, 0x00E0, 0x1803, 0x060C, 0x01F0};

	// widget dimensions
	static final int W = 23;
	static final int H = 23;

    // popup with different options
    JPopupMenu popup;
    // différents items du popup
    static String REGISTER, UNREGISTER, BROADCAST, PREFS, STARTINTERNALHUB, STOPINTERNALHUB;

	private boolean isConnected = false;
    private boolean isIn = false;

	private JMenuItem registerItem, unregisterItem, broadcastItem, startInternalHubItem, stopInternalHubItem;

	// TODO : à virer si on ne s'en sert pas
	private Aladin aladin;

	/** PlasticWidget constructor
	 * @param aladin reference
	 */
	protected PlasticWidget(Aladin aladin) {
		this.aladin=aladin;
		addMouseMotionListener(this);
		addMouseListener(this);
        addKeyListener(this);
		createChaine();
	}

    public Dimension getPreferredSize() { return new Dimension(W,H); }

	// création des chaines de caractères nécessaires
	private void createChaine() {
	    String name = aladin.getMessagingMgr().getProtocolName();

		DISCONNECTED = aladin.chaine.getString("PWDISCONNECTED").replaceAll("SAMP", name);
		CONNECTED_ALONE = aladin.chaine.getString("PWCONNECTED").replaceAll("SAMP", name);
		CAN_TRANSMIT = aladin.chaine.getString("PWCANTRANSMIT").replaceAll("SAMP", name);
		TRANSMITTING = aladin.chaine.getString("PWTRANSMITTING").replaceAll("SAMP", name);
	}

	// TODO : on mouseUp, show available PLASTIC-related items
	public void mouseReleased(MouseEvent e) {
		if( popup==null ) createPopup();
		updatePopupStatus();
        popup.show(this, e.getX(), e.getY());
	}

	// on mouseMove, redisplay to hide the highlighting, and set the default cursor
	public void mouseExited(MouseEvent e) {
		Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
//		bkgdColor = Aladin.BKGD;
        isIn=false;
		repaint();
	}

	// on mouseEnter, highlight and change the cursor
	public void mouseEntered(MouseEvent e) {
		Aladin.makeCursor(this,Aladin.HANDCURSOR);
		requestFocus();
//		aladin.status.setText(getDescription());
        Util.toolTip(this,getDescription());
//		bkgdColor = Aladin.BLUE;
		// on force la maj de l'état
		aladin.getMessagingMgr().updateState();
        isIn=true;
		repaint();
	}

	// retourne la description correspondant à l'état courant (connecté ou non, etc)
	private String getDescription() {
		switch(state) {
			case AppMessagingInterface.NO_PLASTIC : return DISCONNECTED;
			case AppMessagingInterface.PLASTIC_NOT_CONNECTED : return DISCONNECTED;
			case AppMessagingInterface.PLASTIC_CONNECTED_ALONE : return CONNECTED_ALONE;
			case AppMessagingInterface.PLASTIC_CAN_TRANSMIT : {
				if( animationThread!=null && animationThread.isAlive() ) return TRANSMITTING;
				else return CAN_TRANSMIT;
			}
			default : return "";
		}

	}

    private void createPopup() {
        popup = new JPopupMenu();

        String name = aladin.getMessagingMgr().getProtocolName();

        REGISTER = aladin.chaine.getString("PWREGISTER").replaceAll("SAMP", name);
        UNREGISTER = aladin.chaine.getString("PWUNREGISTER").replaceAll("SAMP", name);
        BROADCAST = aladin.chaine.getString("PWBROADCAST");
        PREFS = aladin.chaine.getString("PWPREFS").replaceAll("SAMP", name);
        if( Aladin.BETA ) {
        	STARTINTERNALHUB = aladin.chaine.getString("PWSTARTINTERNALHUB");
        	STOPINTERNALHUB = aladin.chaine.getString("PWSTOPINTERNALHUB");
        }

        popup.add(registerItem = new JMenuItem(REGISTER));
        registerItem.addActionListener(this);
        popup.add(unregisterItem = new JMenuItem(UNREGISTER));
        unregisterItem.addActionListener(this);
        popup.addSeparator();
        if( Aladin.BETA ) {
        	popup.add(startInternalHubItem = new JMenuItem(STARTINTERNALHUB));
            startInternalHubItem.addActionListener(this);
        	popup.add(stopInternalHubItem = new JMenuItem(STOPINTERNALHUB));
            stopInternalHubItem.addActionListener(this);
            popup.addSeparator();
        }
        popup.add(broadcastItem = new JMenuItem(BROADCAST));
        broadcastItem.addActionListener(this);
        popup.addSeparator();
        JMenuItem prefItem;
        popup.add(prefItem = new JMenuItem(PREFS));
        prefItem.addActionListener(this);

        updatePopupStatus();

        super.add(popup);
    }


    /**
     * Montre la fenetre des preferences PLASTIC
     *
     */
    protected void showPrefs() {
    	aladin.plasticPrefs.showPrefs();
    	aladin.plasticPrefs.toFront();
    }

    public void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        drawWidget(gr);
    }

    // nombre d'ondes à colorier
    private int nbWavesToShow = 0;

    static private int[] waveColBoundaries = {0, 2, 4, 7, 10, 13};
	private Color getWaveColor(int i) {
        return mustBeHighilighted(i)?WAVE_ON_COLOR:WAVE_OFF_COLOR;
	}

    private boolean mustBeHighilighted(int i) {
        return waveColBoundaries[nbWavesToShow]>i;
    }

//	static private Point flagStartPoint = new Point(8,1);
	static private Point waveStartPoint = new Point(10,3);


    // Lignes Horizontales du Radar (y,x1,x2)
    static private int [][] RH = { {0,3,4}, {3,12,12},{4,8,11},{5,9,11},{6,10,11},{12,13,15},
                                   {17,2,3},{17,8,12},{18,0,14},{19,0,14} };
    // Lignes Verticales du Radar (x,y1,y2)
    static private int [][] RV = { {3,1,4},{4,3,8},{4,12,17},{5,1,1},{5,4,17},{6,2,2},{6,5,17},
                                   {7,3,3},{7,6,11},{7,15,17},{8,7,11},{8,16,16},{9,8,11},
                                   {10,9,12},{11,10,12},{11,7,7},{12,8,8},{12,11,12},{13,9,9},
                                   {14,10,10},{15,11,11} };
    // Lignes Horizontales en blanc (y,x1,x2)
    static private int [][] RB =  {{1,4,4},{2,4,5},{3,5,6},{4,6,7},{5,7,8},{6,8,9},{7,9,10},
                                   {8,10,11},{9,11,12},{10,12,13},{11,13,14} };
	/**
	 * actually draws the widget
	 * @param gr
	 */
	private void drawWidget(Graphics gr) {
        gr.setColor(getBackground());
		gr.fillRect(0,0,W,H);

		// first, we draw the tower
		gr.setColor(isConnected || isIn || state==AppMessagingInterface.PLASTIC_NOT_CONNECTED ?Color.black:Color.gray);
		for( int i=0; i<RH.length; i++ ) gr.drawLine(RH[i][1]-2,RH[i][0]+2,RH[i][2]-2,RH[i][0]+2);
        for( int i=0; i<RV.length; i++ ) gr.drawLine(RV[i][0]-2,RV[i][1]+2,RV[i][0]-2,RV[i][2]+2);
        gr.setColor(isIn?Aladin.COLOR_LABEL:Color.white);
        for( int i=0; i<RB.length; i++ ) gr.drawLine(RB[i][1]-2,RB[i][0]+2,RB[i][2]-2,RB[i][0]+2);
//		for( int i=0; i<TOWER_LINES.length; i++ ) {
//			drawLine(i, TOWER_LINES[i], gr);
//		}

		// then we draw the flag
//		gr.setColor(Color.blue);
//		gr.fillRect(flagStartPoint.x, flagStartPoint.y, 2, 4);
//		gr.setColor(Color.white);
//		gr.fillRect(flagStartPoint.x+2, flagStartPoint.y, 2, 4);
//		gr.setColor(Color.red);
//		gr.fillRect(flagStartPoint.x+4, flagStartPoint.y, 2, 4);


		// then the red cross if PLASTIC is not connected !
		if( !isConnected ) {
			Color c = isIn?Color.red.darker():Color.red.darker().darker();
			gr.setColor(c);
			gr.drawLine(16,15,22,21);
			gr.drawLine(16,21,22,15);

			c = isIn?Color.red:Color.red.darker();;
			gr.setColor(c);
			gr.drawLine(14,15,20,21);
			gr.drawLine(15,15,21,21);
			gr.drawLine(14,21,20,15);
			gr.drawLine(15,21,21,15);
		} else if( state!=AppMessagingInterface.PLASTIC_CONNECTED_ALONE ) {
			// then we draw the waves
			for( int i=0; i<WAVE_LINES.length; i++ ) {
				gr.setColor(getWaveColor(i));
				drawWaveLine(i, WAVE_LINES[i], gr);
				// double the highlighted lines
				if( mustBeHighilighted(i) )
					drawWaveLine(i-1, WAVE_LINES[i], gr);
			}
		}
	}

	// dessine une ligne de la tour
//	private void drawLine(int row, int codedLine, Graphics gr) {
//		int start, end;
//		start = end = 0;
//		int k=0; // compteur d'itérations
//		for( int j=12; j>=0; j-- ) {
//			if( ((1 << j) & codedLine) != 0)
//				end++;
//			else {
//				if( start!=end ) {
//					gr.drawLine(start, row, end-1, row);
//					start = end = k+1;
//				}
//				else {
//					start++;
//					end++;
//				}
//			}
//			k++;
//		}
//
//		if( start!=end ) {
//			gr.drawLine(start, row, end-1, row);
//		}
//	}

	// dessine une ligne des ondes
	private void drawWaveLine(int col, int codedLine, Graphics gr) {
		int start, end;
		start = end = 0;
		int k=0; // compteur d'itérations
		for( int j=12; j>=0; j-- ) {
			if( ((1 << j) & codedLine) != 0)
				end++;
			else {
				if( start!=end ) {
					gr.drawLine(col+waveStartPoint.x, start+waveStartPoint.y, col+waveStartPoint.x, end-1+waveStartPoint.y);
					start = end = k+1;
				}
				else {
					start++;
					end++;
				}
			}
			k++;
		}

		if( start!=end ) {
			gr.drawLine(col+waveStartPoint.x, start+waveStartPoint.y, col+waveStartPoint.x, end-1+waveStartPoint.y);
		}
	}

//	static private void writeBinary(int k) {
//		for( int j=12; j>=0; j-- ) {
//			if( ((1 << j) & k) != 0)
//				System.out.print("1");
//			else
//				System.out.print("0");
//		}
//		System.out.println();
//	}

	protected void updateStatus(boolean b) {
		if( this.isConnected!=b ) {
			this.isConnected = b;

			updatePopupStatus();
			repaint();
		}
	}

	private int state;
	protected void updateStatus(int state) {
		if( this.state!=state ) {
			this.state = state;

			updatePopupStatus();
			repaint();
		}
	}

    /** màj du popup */
    private void updatePopupStatus() {
    	if( popup==null ) return;

    	AppMessagingInterface mgr = aladin.getMessagingMgr();

        int nbCatalog=0;
        int nbImg=0;
        Plan [] plan = aladin.calque.getPlans();
        for( int i=0; i<plan.length; i++ ) {
            Plan pc = plan[i];
            if( !pc.selected ) continue;
            if( pc.isSimpleCatalog() && pc.flagOk ) nbCatalog++;
            if( pc.type==Plan.IMAGE && pc.flagOk ) nbImg++;
            if( pc.type==Plan.IMAGEHUGE && pc.flagOk ) nbImg++;
        }

        ArrayList<String> imgApps = mgr.getAppsSupporting(AppMessagingInterface.ABSTRACT_MSG_LOAD_FITS);
        ArrayList<String> tabApps = mgr.getAppsSupporting(AppMessagingInterface.ABSTRACT_MSG_LOAD_VOT_FROM_URL);

    	registerItem.setEnabled( ! isConnected );
    	unregisterItem.setEnabled( isConnected );
    	broadcastItem.setEnabled( isConnected && (nbCatalog>0 || nbImg>0) && (imgApps.size()>0 || tabApps.size()>0));
    	if( Aladin.BETA ) {
    		boolean plaskitRunning = aladin.getMessagingMgr().internalHubRunning();
    		startInternalHubItem.setEnabled(!plaskitRunning);
    		stopInternalHubItem.setEnabled(plaskitRunning);
    	}
    }

    // thread d'animation du widget
    private Thread animationThread;
    static private long sleepTime = 500;
    /**
     * petite animation du widget lors de l'envoi d'un message
     */
    protected void animateWidgetSend() {
        animationThread = new Thread("AladinPlasticWidget") {

            public void run() {

                int nbWaves = waveColBoundaries.length;
                // 2 cycles
                for( int i=0; i<2; i++ ) {
                    for( int j=1; j<nbWaves; j++ ) {
                        if( this!=animationThread ) break;

                        nbWavesToShow = j;
                        repaint();
                        try {
                            Thread.currentThread().sleep(sleepTime);
                        }
                        catch(InterruptedException e) {}
                    }
                }
                nbWavesToShow = 0;
                repaint();
            }

        };
        animationThread.start();

    }

    /**
     * petite animation du widget lors de la réception d'un message
     */
    protected void animateWidgetReceive(final boolean updateStatus, final boolean newStatus) {
        animationThread = new Thread("AladinPlasticWidget") {

            public void run() {
                // 2 x (on allume tout, on éteint tout)
                for( int i=0; i<4; i++ ) {
                    if( this!=animationThread ) break;

                    nbWavesToShow = i%2==0?waveColBoundaries.length-1:0;
                    repaint();
                    try {
                        Thread.currentThread().sleep(sleepTime);
                    }
                    catch(InterruptedException e) {}
                }
                if( updateStatus ) {
                    updateStatus(newStatus);
                }
            }

        };
        animationThread.start();
    }

    protected void animateWidgetReceive() {
        animateWidgetReceive(false, true);
    }

	/** Activation/désactivation du debugging PLASTIC */
    public void keyPressed(KeyEvent e) {
	      if( e.getKeyChar()=='d' ) {
	      	boolean pTrace;
	      	aladin.appMessagingMgr.setPlasticTrace(pTrace=!aladin.appMessagingMgr.getPlasticTrace());
	      	String protocolName = aladin.appMessagingMgr.getProtocolName();
	      	aladin.command.println(pTrace?"Activating "+protocolName+" trace":
	      	                                  "Desactivating "+protocolName+" trace");
	      }
	}

   public void mouseDragged(MouseEvent e) { }
   public void mouseClicked(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }
   public void mouseMoved(MouseEvent e) { }
   public void keyReleased(KeyEvent e) { }
   public void keyTyped(KeyEvent e) { }


   // implementation de ActionListener
    public void actionPerformed(ActionEvent ae) {
        String o;

        if( ae.getSource() instanceof JMenuItem ) o = ((JMenuItem)ae.getSource()).getText();
        else return;

        if (REGISTER.equals(o)) {
            aladin.getMessagingMgr().register(false, true);
        } else if (UNREGISTER.equals(o)) {
            if (aladin.getMessagingMgr().unregister())
                aladin.dontReconnectAutomatically = true;
        } else if (BROADCAST.equals(o)) {
            aladin.broadcastSelectedPlanes(null);
        } else if (PREFS.equals(o)) {
            showPrefs();
        } else if (STARTINTERNALHUB.equals(o)) {
            aladin.getMessagingMgr().startInternalHub();
        } else if (STOPINTERNALHUB.equals(o)) {
            aladin.getMessagingMgr().stopInternalHub(false);
        }

    }

	/*
	public static void main(String[] args) {
		for( int i=0; i<WAVE_LINES.length; i++ ) {
			writeBinary(WAVE_LINES[i]);
		}
	}
	*/

}
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JToggleButton;

public class GrabUtil {

	public GrabItFrame grabFrame;
	private static GrabUtil instance = null;
	List<Server> grabItServers = new ArrayList<Server>();// JFrames with their own rest methods might not need to use this
	List<JToggleButton> grabs = new ArrayList<JToggleButton>(); //some grabs are not on servers
	
	public static synchronized GrabUtil getInstance() {
		if (instance == null) {
			instance = new GrabUtil();
		}
		return instance;
	}
	
	public GrabUtil() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Mise en place du target en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 * @return 
	 */
	protected static String getGrabItCoord(Aladin aladin, double x, double y) {
		ViewSimple v = aladin.view.getCurrentView();
		Plan pr = v.pref;
		if (pr == null) return null;
		Projection proj = pr.projd;
		if (proj == null) return null;
		PointD p = v.getPosition(x, y);
		Coord c = new Coord();
		c.x = p.x;
		c.y = p.y;
		proj.getCoord(c);
		if (Double.isNaN(c.al)) return null;
		return c.getSexa();
	}
	
	/**
	 * Mise en place du target en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 */
	public static void setGrabItCoord(Aladin aladin, Server server, double x, double y) {
		String sexaCoord = getGrabItCoord(aladin, x, y);
		if (sexaCoord!=null) {
			server.setTarget(aladin.localisation.getFrameCoord(sexaCoord));
		}
	}

	
	/**
	 * Arrete le GrabIt
	 */
	public static void stopGrabIt(Aladin aladin, JFrame serverDialog, Server server) {
		JToggleButton grab = server.grab;
		if (grab != null) {
			Plan pref = aladin.calque.getPlanRef();
			grab.getModel().setSelected(false);
			grab.setEnabled(pref != null && Projection.isOk(pref.projd));
			Server serverToEdit = server;
			if (serverToEdit.tree != null && !serverToEdit.tree.isEmpty())
				serverToEdit.tree.clear();
			if (server instanceof ServerTapExamples) {
				((ServerTapExamples)server).targetSettingsChangedAction();
			}
		}
		serverDialog.toFront();
	}
	
	/**
	 * Démarrage d'une séquence de GrabIT
	 *//*
	protected void startGrabIt(Server server) {
		if (server.grab == null || !server.grab.getModel().isSelected())
			return;
		aladin.f.toFront();
	}*/
	
	/**
	 * Retourne true si le bouton grabit du formulaire existe et qu'il est
	 * enfoncé
	 */
	protected boolean isGrabIt(Server server) {
		return (server.modeCoo != Server.NOMODE && server.grab != null
				&& server.grab.getModel().isSelected());
	}
	
	/**
	 * Mise en place du radius en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 */
	public static String setGrabItRadius(Aladin aladin, Server server, double x1, double y1, double x2, double y2) {
		if (server!=null && server.modeRad == Server.NOMODE) return null;
		if (Math.abs(x1 - x2) < 3 && Math.abs(y1 - y2) < 3) return null;
		
		ViewSimple v = aladin.view.getCurrentView();
		Plan pr = v.pref;
		if (pr == null) return null;
		Projection proj = pr.projd;
		if (proj == null) return null;
		PointD p1 = v.getPosition(x1, y1);
		PointD p2 = v.getPosition(x2, y2);
		Coord c1 = new Coord();
		c1.x = p1.x;
		c1.y = p1.y;
		proj.getCoord(c1);
		if (Double.isNaN(c1.al)) return null;
		Coord c2 = new Coord();
		c2.x = p2.x;
		c2.y = p2.y;
		proj.getCoord(c2);
		if (Double.isNaN(c2.al)) return null;
		String radius = Coord.getUnit( Coord.getDist(c1, c2) );
		if( server!=null ) server.resolveRadius( radius, true);
	    return radius;
	}
	
	//TODO:: tintin 
	/*public JToggleButton getGrabItButton(final Server server, String buttonName){
		JToggleButton grab = new JToggleButton("Grab");
		Insets m = grab.getMargin();
        grab.setMargin(new Insets(m.top,2,m.bottom,2));
        grab.setOpaque(false);
        grab.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
       			startGrabIt(server);
           }
        });
        grab.setFont(Aladin.SBOLD);
        server.updateWidgets(aladin.dialog);
        return grab;
	}*/

	/**
	 * All servers registered to the util will be reset
	 * @param aladin
	 */
	public void resetAllGrabIts(Aladin aladin) {
		 Plan pref = aladin.calque.getPlanRef();
	      boolean flag = (pref != null && pref.projd != null);
	      setAllGrabItsEnabled(flag);
	}
	
	/**
	 * All servers registered to the util will be reset
	 * @param aladin
	 */
	public void setAllGrabItsEnabled(boolean isEnabled) {
		for (Server server : grabItServers) {
			if (server.grab != null) {
				server.grab.setEnabled(isEnabled);
			}
		}
		for (JToggleButton grab : grabs) {
			if (grab != null) {
				grab.setEnabled(isEnabled);
			}
		}
	}

	public void removeAndAdd(Server oldServer, Server newServer) {
		// TODO Auto-generated method stub
		this.grabItServers.remove(oldServer);
		this.grabItServers.add(newServer);
	}
	
	public void removeAndAdd(JToggleButton oldGrab, JToggleButton newGrab) {
		this.grabs.remove(oldGrab);
		this.grabs.add(newGrab);
	}
	

}

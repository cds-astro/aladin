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

import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.LOADCLIENTTAPURL;
import static cds.aladin.Constants.REGISTRYPANEL;
import static cds.aladin.Constants.SUBMITPANEL;
import static cds.tools.CDSConstants.DEFAULT;
import static cds.tools.CDSConstants.WAIT;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import cds.mocmulti.MocItem2;
import cds.tools.TwoColorJTable;
import cds.tools.Util;

/**
 * Created based on {@link FrameServer}
 * 
 */
public final class TapFrameServer extends JFrame implements ActionListener,KeyListener {
   
	private static final long serialVersionUID = 1L;

	static String FSTAPCONTROLLER, INFO = " ? ", CLOSE, TIPSUBMIT, TIPCLOSE, FILTER, RESET, GO, LOAD, TIPLOAD,
			TIPRELOAD, TAPNOFILELOAD, TAPNOFILERELOAD, SELECTSERVERLABEL, SELECTSERVERTOOLTIP,
			NOTAPSERVERSCONFIGUREDMESSAGE, TAPURLCLIENTFIELDLABEL, SELECTDIRTAPSERVERSTOOLTIP,
			SELECTSPLTAPSERVERSLABEL, SELECTSPLTAPSERVERSTOOLTIP, SELECTDIRTAPSERVERSLABEL, CHOOSEFROMTAPSERVERSTEXT,
			WAITLOADINGTAPSERVERSLIST, INCORRECTTAPURLMESSAGE;

   Aladin aladin;
   TapManager tapManager;
   JTextField filter=null;
   private JPanel splListPanelScroll;
   private JPanel completeListPanelScroll;
   JTable preselectedServersTable;
   JTable allServersTable;
   MocDataLabelTable allServersDataLabelTable;
   SimpleDataLabelTable splServersDataLabelTable;
   
   TableModelListener tableListener;
   
   JPanel registryPanel;
   JPanel splRegistryPanel;
   JPanel treeRegistryPanel;
   public Vector<String> selectedServerLabel;
   JTabbedPane options;
   JLabel info = new JLabel();
   
   public static int labelId = 0;
   public static int urlId = 2;
   public static int descriptionId = 1;
   
   //inputs
   JTextField userProvidedTapUrl;



   protected void createChaine() {
      GO = Aladin.chaine.getString("FSGO");
      RESET = Aladin.chaine.getString("RESET");
      FILTER = Aladin.chaine.getString("FSFILTER");
      CLOSE = Aladin.chaine.getString("CLOSE");
      TIPSUBMIT = Aladin.chaine.getString("TIPSUBMIT");
      TIPCLOSE = Aladin.chaine.getString("TIPCLOSE");
      LOAD = Aladin.chaine.getString("FSLOAD");
      TIPLOAD = Aladin.chaine.getString("TIPLOAD");
      FSTAPCONTROLLER = Aladin.chaine.getString("FSTAPCONTROLLER");
      TIPRELOAD = Aladin.chaine.getString("TIPRELOAD");
      TAPNOFILELOAD = Aladin.chaine.getString("TAPNOFILELOAD");
      TAPNOFILERELOAD = Aladin.chaine.getString("TAPNOFILERELOAD");
      SELECTSERVERLABEL = Aladin.chaine.getString("SELECTSERVERLABEL");
      SELECTSERVERTOOLTIP = Aladin.chaine.getString("SELECTSERVERTOOLTIP");
      NOTAPSERVERSCONFIGUREDMESSAGE = Aladin.chaine.getString("NOTAPSERVERSCONFIGUREDMESSAGE");
      TAPURLCLIENTFIELDLABEL = Aladin.chaine.getString("TAPURLCLIENTFIELDLABEL");
      SELECTDIRTAPSERVERSTOOLTIP = Aladin.chaine.getString("SELECTDIRTAPSERVERSTOOLTIP");
      SELECTSPLTAPSERVERSLABEL = Aladin.chaine.getString("SELECTSPLTAPSERVERSLABEL");
      SELECTSPLTAPSERVERSTOOLTIP = Aladin.chaine.getString("SELECTSPLTAPSERVERSTOOLTIP");
      SELECTDIRTAPSERVERSLABEL = Aladin.chaine.getString("SELECTDIRTAPSERVERSLABEL");
      CHOOSEFROMTAPSERVERSTEXT = Aladin.chaine.getString("CHOOSEFROMTAPSERVERSTEXT");
      WAITLOADINGTAPSERVERSLIST = Aladin.chaine.getString("WAITLOADINGTAPSERVERSLIST");
      INCORRECTTAPURLMESSAGE = Aladin.chaine.getString("INCORRECTTAPURLMESSAGE");
   }

	protected TapFrameServer(Aladin aladin, TapManager tapManager){
		super();
		this.setName(REGISTRYPANEL);
		this.aladin = aladin;
		Aladin.setIcon(this);
		createChaine();
		setTitle(FSTAPCONTROLLER);
		this.tapManager = tapManager;
		this.aladin = aladin;
		setLocation(Aladin.computeLocation(this));
		
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
	}

	public Dimension getPreferredSize() {
		return new Dimension(650, 450);
	}

	public JPanel createCenterPane(){
		splRegistryPanel = new JPanel();
		treeRegistryPanel = new JPanel();
		
		registryPanel = new JPanel(new BorderLayout());
		setInit();
		
		options = new JTabbedPane();
		registryPanel.add(options, "Center");
		registryPanel.setFont(Aladin.PLAIN);
		
		options.addTab(SELECTSPLTAPSERVERSLABEL, null, splRegistryPanel, SELECTSPLTAPSERVERSTOOLTIP);
		options.addTab(SELECTDIRTAPSERVERSLABEL, null, treeRegistryPanel, SELECTDIRTAPSERVERSTOOLTIP);
		
//		tabbedTapThings.addTab(SELECTSERVERLABEL, null, allRegistryPanel, SELECTSERVERTOOLTIP);
		getContentPane().removeAll();
		getContentPane().add(registryPanel, "Center");
		pack();
		return registryPanel;
	}
	
	public void initPreselectedServers() {
		synchronized (splRegistryPanel) {
			Aladin.makeCursor(splRegistryPanel, Aladin.WAITCURSOR);
			splRegistryPanel.removeAll();
			splRegistryPanel.setLayout(new BorderLayout(0, 0));

			int scrollWidth = 400;
			int scrollHeight = 350;
			
			try {
				List<Vector<String>> datalabels = tapManager.getPreSelectedTapServers();
				if (datalabels != null && !datalabels.isEmpty()) {
					JButton b;
					JScrollPane scroll = new JScrollPane(splListPanelScroll);
					scroll.setSize(scrollWidth, scrollHeight);
					scroll.setBackground(Color.white);
					scroll.getVerticalScrollBar().setUnitIncrement(70);

					fillWithSplRegistryServers();

					Aladin.makeAdd(splRegistryPanel, scroll, "Center");

					JPanel submit = new JPanel();
					submit.setName(SUBMITPANEL);
					submit.add(b = new JButton(LOAD));
					b.addActionListener(this);
					b.setToolTipText(TIPLOAD);
					submit.add(b = new JButton(CLOSE));
					b.addActionListener(this);
					b.setToolTipText(TIPCLOSE);
					Aladin.makeAdd(registryPanel, submit, "South");
					info.setText(CHOOSEFROMTAPSERVERSTEXT);
				} else {
					showSplListNotLoaded();
				}
			} catch (Exception e) {
				// TODO: handle exception
				showSplListNotLoaded();
			}
			Aladin.makeCursor(splRegistryPanel, Aladin.DEFAULTCURSOR);
		}
		pack();
	}
	
	public void addUrlPanel() {
		JPanel urlClientPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
	    c.gridy = 0;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		c.insets = new Insets(10, 10, 1, 1);
		
		c.gridx = 0;
		c.insets = new Insets(10,1,1,1);
		c.weightx = 0.05;
		urlClientPanel.add(new JLabel(TAPURLCLIENTFIELDLABEL), c);
		
		c.gridx = 1;
		c.weightx = 0.90;
		userProvidedTapUrl = new JTextField();
		userProvidedTapUrl.setPreferredSize(new Dimension(240, Server.HAUT));
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		urlClientPanel.add(userProvidedTapUrl, c);

		JButton loadUserUrl = new JButton(LOAD);
		loadUserUrl.setActionCommand(LOADCLIENTTAPURL);
		loadUserUrl.addActionListener(this);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 1, 1, 35);
		c.weightx = 0.05;
		c.gridx = 2;
		urlClientPanel.add(loadUserUrl, c);
		
		JPanel header = new JPanel(new BorderLayout(0, 7));
		header.add(urlClientPanel, "North");
		info.setText(WAITLOADINGTAPSERVERSLIST);
		//CHOOSEFROMTAPSERVERSTEXT
		header.add(info, "West");
		registryPanel.add(header, "North");
		pack();
	}
	
	public void showListNotLoaded(JComponent component) {
		component.removeAll();
		component.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
	}
	
	public synchronized void showSplListNotLoaded() {
		showListNotLoaded(splRegistryPanel);
	}
	
	public void showCompleteListNotLoaded() {
		showListNotLoaded(treeRegistryPanel);
	}
	
	public void showRegistryNotLoaded() {
		info.setText(NOTAPSERVERSCONFIGUREDMESSAGE);
		showCompleteListNotLoaded();
		showSplListNotLoaded();
		pack();
	}
	public void initAllServers() {
		synchronized (treeRegistryPanel) {
			int scrollWidth = 400;
			int scrollHeight = 350;
			completeListPanelScroll = new JPanel();
			try {
				if (tapManager.isValidTapServerList()) {
					info.setText(CHOOSEFROMTAPSERVERSTEXT);
					
					JButton b;
					if (fillWithRegistryServers(null)) {
						JPanel check = new JPanel();
						check.add(new JLabel(FILTER + ": "));
						filter = new JTextField(15);
						check.add(filter);
						filter.addKeyListener(this);
						check.add(b = new JButton(GO));
						b.addActionListener(this);
						check.add(b = new JButton(RESET));
						b.addActionListener(this);
						
						JScrollPane scroll = new JScrollPane(completeListPanelScroll);
						scroll.setSize(scrollWidth, scrollHeight);
						scroll.setBackground(Color.white);
						scroll.getVerticalScrollBar().setUnitIncrement(70);
						Aladin.makeCursor(treeRegistryPanel, Aladin.WAITCURSOR);
						treeRegistryPanel.removeAll();
						treeRegistryPanel.add(check, "North");
						Aladin.makeAdd(treeRegistryPanel, scroll, "Center");
						
					}
				} else {
					showCompleteListNotLoaded();
				}
			} catch (Exception e) {
				// TODO: handle exception
				showCompleteListNotLoaded();
			}
			treeRegistryPanel.revalidate();
			treeRegistryPanel.repaint();
			Aladin.makeCursor(treeRegistryPanel, Aladin.DEFAULTCURSOR);
		}
		pack();
	}
	
	public void setInit() {
		splRegistryPanel.removeAll();
		splRegistryPanel.setLayout(new BorderLayout(0, 0));
		treeRegistryPanel.removeAll();
		treeRegistryPanel.setLayout(new BorderLayout(0, 0));

		addUrlPanel();
		completeListPanelScroll = new JPanel();
		completeListPanelScroll.add(new JLabel(WAITLOADINGTAPSERVERSLIST));
		Aladin.makeAdd(treeRegistryPanel, completeListPanelScroll, "Center");
		splListPanelScroll = new JPanel();
		splListPanelScroll.add(new JLabel(WAITLOADINGTAPSERVERSLIST));
		Aladin.makeAdd(splRegistryPanel, splListPanelScroll, "Center");
	}
  
	private boolean fillWithSplRegistryServers() {
		Vector<Vector<String>> datalabels = tapManager.getPreSelectedTapServers();
		return fillWithSplListRegistryServers(datalabels);
	}
	
	private boolean fillWithRegistryServers(String mask) {
		tapManager.populateTapServersFromTree(mask);
		return fillWithRegistryServers();
	}
	
	/**
	 * Method to create frame for all tap servers
	 * @return 
	 * 
	 * @throws Exception
	 */
	private boolean fillWithSplListRegistryServers(Vector<Vector<String>> datalabels) {
		boolean result = false;
		synchronized (splListPanelScroll) {
			splListPanelScroll.removeAll();
			splListPanelScroll.setLayout(new BorderLayout(1, 1));
			if (datalabels != null && !datalabels.isEmpty()) {
				splServersDataLabelTable = new SimpleDataLabelTable(aladin, datalabels);
				preselectedServersTable = new TwoColorJTable(splServersDataLabelTable);
				preselectedServersTable.setGridColor(Color.lightGray);
				preselectedServersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				preselectedServersTable.getColumnModel().getColumn(0).setPreferredWidth(150);
				preselectedServersTable.getColumnModel().getColumn(1).setPreferredWidth(320);
				preselectedServersTable.getColumnModel().getColumn(2).setPreferredWidth(700);
//				preselectedServersTable.setRowSelectionInterval(0, 0);
				preselectedServersTable.getTableHeader().setDefaultRenderer(new TableHeaderRenderer(
						preselectedServersTable.getTableHeader().getDefaultRenderer(), splServersDataLabelTable));
				preselectedServersTable.getTableHeader().addMouseListener(new TableHeaderListener());
				splListPanelScroll.add(preselectedServersTable, BorderLayout.CENTER);
				splListPanelScroll.add(preselectedServersTable.getTableHeader(), BorderLayout.NORTH);
//			    splListPanelScroll.add(preselectedServersTable);
				result = true;
			} else {
				splListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
			}
			splListPanelScroll.revalidate();
			splListPanelScroll.repaint();
		}
		return result;
	}
	
	/**
	 * Method to create frame for all tap servers
	 * @return 
	 * 
	 * @throws Exception
	 */
	private boolean fillWithRegistryServers() {
		completeListPanelScroll.removeAll();
		completeListPanelScroll.setLayout(new BorderLayout(1, 1));
		List<String> datalabels = TapManager.allTapServerLabels;
//		Vector<Vector<String>> allRows = new Vector<Vector<String>>();
		boolean result = false;
		if (datalabels != null && !datalabels.isEmpty()) {
//			TwoColorJTable table = new TwoColorJTable(allRows, DataLabel.getColumnLabels());
			allServersDataLabelTable = new MocDataLabelTable(aladin, datalabels);

			allServersTable = new TwoColorJTable(allServersDataLabelTable);
			allServersTable.setGridColor(Color.lightGray);
			allServersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			allServersTable.getColumnModel().getColumn(0).setPreferredWidth(150);
			allServersTable.getColumnModel().getColumn(1).setPreferredWidth(320);
			allServersTable.getColumnModel().getColumn(2).setPreferredWidth(700);
			allServersTable.setRowSelectionInterval(0, 0);
			allServersTable.getTableHeader().setDefaultRenderer(new TableHeaderRenderer(
					allServersTable.getTableHeader().getDefaultRenderer(), allServersDataLabelTable));
			allServersTable.getTableHeader().addMouseListener(new TableHeaderListener());
//		    completeListPanelScroll.add(allServersTable);
		    completeListPanelScroll.add(allServersTable, BorderLayout.CENTER);
		    completeListPanelScroll.add(allServersTable.getTableHeader(), BorderLayout.NORTH);
			result = true;
		} else {
			completeListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
		}
		completeListPanelScroll.revalidate();
		completeListPanelScroll.repaint();
		return result;
	}
	
	 /** Renderer pour le header de la JTable
	    *  Permet d'afficher les triangles de tri
	    */
	   class TableHeaderRenderer extends DefaultTableCellRenderer {

	       TableCellRenderer renderer;
	       DataLabelTable dataLabelTable;

	       public TableHeaderRenderer(TableCellRenderer defaultRenderer, DataLabelTable dataLabelTable) {
	                   this.renderer = defaultRenderer;
	                   this.dataLabelTable = dataLabelTable;
	       }

	       /**
	        * Overwrites DefaultTableCellRenderer.
	        */
	       public Component getTableCellRendererComponent(JTable table, Object
	               value, boolean isSelected,
	               boolean hasFocus, int row,
	               int column) {

	           Component comp = renderer.getTableCellRendererComponent(
	                   table, value, isSelected, hasFocus, row, column);
	           
	           int idxSortedColView = table.convertColumnIndexToView(dataLabelTable.idxSortedCol);
	           // if column col has been clicked, add a small arrow to the column header
	           if( comp instanceof JLabel ) {
	               if( column==idxSortedColView ) {
	                   ImageIcon icon = dataLabelTable.ascSort?Util.getAscSortIcon():Util.getDescSortIcon();
	                   ((JLabel)comp).setIcon(icon);
	                   ((JLabel)comp).setHorizontalTextPosition(SwingConstants.LEADING);
	               }
	               else ((JLabel)comp).setIcon(null);
	           }
	           return comp;
	       }
	   }
	   
	   /** Classe interne pour le header de la table
	    *  Permet le tri lorsqu'on clique sur un des bandeaux
	    */
	   class TableHeaderListener extends MouseAdapter {

	       public void mouseClicked(MouseEvent e) {
	    	   JTable table = null;; 
	    	   DataLabelTable model = null;
	    	   Vector data = null;
	       	if (preselectedServersTable.getTableHeader().equals(e.getSource())) {
	       		table = preselectedServersTable;
	       		data = TapManager.splTapServerLabels;
	       		model = splServersDataLabelTable;
			} else {
				table = allServersTable;
				data = (Vector) TapManager.allTapServerLabels;
				model = allServersDataLabelTable;
			}
	           TableColumnModel columnModel = table.getColumnModel();
	           int viewColumn = columnModel.getColumnIndexAtX(e.getX());
	           final int column = table.convertColumnIndexToModel(viewColumn);

	           model.idxSortedCol = column;

	           if( e.getClickCount() == 1 && column != -1 ) {
	               // colonne d'indice 1 : on applique le tri par défaut
	               if( column==1) {
	                   int idx = table.getSelectedRow();
	                   final Object selected = idx>=0?data.get(idx):null;
	                   model.defaultSortServers();
	                   model.notifyTableChanged();
	                   selectItem(selected, table, data);
	                   return;
	               }

	               Comparator comp = new Comparator() {
	                   public final int compare (Object a, Object b) {
	                	   Object val1 = null;
	                       Object val2 = null;
	                	   if (a instanceof String) {
	                		   val1 = ((String)a);
			                    val2 = ((String)b);
						} else if (a instanceof Vector) {
							val1 = ((Vector<String>)a).get(column);
		                    val2 = ((Vector<String>)b).get(column);
						}
	                       
	                       return ((Comparable)val1).compareTo(val2);
	                   }
	               };

	               int idx = table.getSelectedRow();
	               final Object selected = idx>=0?data.get(idx):null;

	               Collections.sort(data, comp);
	               if( ! model.ascSort ) Collections.reverse(data);
	               model.ascSort = !model.ascSort;
	               model.notifyTableChanged();

	               // update selection so to keep initial selection after sorting is done
	               selectItem(selected, table, data);
	           }
	       }

	       private void selectItem(final Object o, final JTable table, final Vector data) {
	           SwingUtilities.invokeLater(new Runnable() {
	               public void run() {
//	            	   Vector<Vector<String>> data = TapManager.splTapServerLabels;
	                   table.clearSelection();
	                   if( o ==null ) return;
	                   int idxSelected = data.indexOf(o);
	                   if( idxSelected>=0 ) table.addRowSelectionInterval(idxSelected, idxSelected);
	               }
	           });
	       }

	   } // end of inner class TableHeaderListener
	

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (!(o instanceof JButton))
			return;

		String s = ((JButton) o).getActionCommand();

		// Affichage du selecteur de fichiers
		if (s.equals(INFO)) {
			for (int i = 0; i < aladin.dialog.server.length; i++) {
				if (o != aladin.dialog.server[i].statusReport)
					continue;
				aladin.dialog.server[i].showStatusReport();
			}
		} else if (s.equals(CLOSE)) {
			setVisible(false);
		} else if (s.equals(GO)) {
			go();
		} else if (s.equals(RESET)) {
			Aladin.makeCursor(registryPanel, WAIT);
			reset();
			Aladin.makeCursor(registryPanel, DEFAULT);
		} else if (s.equals(LOAD)) {
			try {
				Aladin.makeCursor(this, WAIT);
				final long startTime = TapManager.getTimeToLog();
				if (Aladin.levelTrace >= 4) System.out.println("In tapframeserver starting to load: "+startTime);
				this.tapManager.setSelectedServerLabel();
				if (this.selectedServerLabel != null) {
					if (this.selectedServerLabel == null || this.selectedServerLabel.size() < 2 
							|| this.selectedServerLabel.get(labelId) == null
							|| this.selectedServerLabel.get(urlId) == null) {// not a necessary condition. But adding just in case.
						this.tapManager.showTapRegistryForm();
					} else {
						String nodeTable = null;
						String gluActionName = null;
						MocItem2 mocItem = aladin.directory.multiProp.getItem(this.selectedServerLabel.firstElement());
						if (mocItem != null) {
							MyProperties prop = aladin.directory.multiProp.getItem(this.selectedServerLabel.firstElement()).prop;
							nodeTable = prop.get("tap_tablename");
							gluActionName = prop.get("tap_glutag");
						}
						this.tapManager.loadTapServer(gluActionName, this.selectedServerLabel.get(labelId),
								this.selectedServerLabel.get(urlId), nodeTable);
						this.aladin.dialog.show(this.aladin.dialog.tapServer);
					}
				} else {
					Aladin.error(this, TAPNOFILELOAD);
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				Aladin.error(this, e1.getMessage());
			} finally {
				Aladin.makeCursor(this, DEFAULT);
			}
		} else if (s.equals(LOADCLIENTTAPURL)) {
			loadInputUrlServer();
		}
	}

	/**
	 * Loads tap client from the url provided by the user
	 */
	private void loadInputUrlServer() {
		try {
			Aladin.makeCursor(this, WAIT);
			if (userProvidedTapUrl.getText() != null && !userProvidedTapUrl.getText().isEmpty()) {
				URL tapUrl = new URL(userProvidedTapUrl.getText());
				Aladin.trace(3, "Will create tap client for: " + tapUrl);
				tapManager.loadTapServer(null, tapUrl.toString(), tapUrl.toString(), null);
				this.aladin.dialog.show(this.aladin.dialog.tapServer);
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3)
				e1.printStackTrace();
			Aladin.error(this, INCORRECTTAPURLMESSAGE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		} finally {
			Aladin.makeCursor(this, DEFAULT);
		}
	}

	private void go() {
		Aladin.makeCursor(this, WAIT);
		String mask = filter.getText().trim();
		if (mask.length() == 0) {
			mask = null;
		}
		fillWithRegistryServers(mask);
		pack();
		Aladin.makeCursor(this, DEFAULT);
	}

	public void reset() {
		fillWithRegistryServers(EMPTYSTRING);
		reloadRegistryPanel();
	}
	
	public void reloadRegistryPanel() {
		splRegistryPanel.revalidate();
		splRegistryPanel.repaint();
	}

   public void keyPressed(KeyEvent e) { }
   public void keyTyped(KeyEvent e) { }

   public void keyReleased(KeyEvent e) {
      if( e.getKeyCode()==KeyEvent.VK_ENTER ) go();
   }

}

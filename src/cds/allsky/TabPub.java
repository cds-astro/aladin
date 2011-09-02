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

package cds.allsky;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import cds.aladin.Aladin;
import cds.tools.Util;

public class TabPub extends JPanel implements ActionListener {

	private static String NEXT, PUBLISH, EXPORT, OPEN;
    private static String LOCAL_FULL;
 	
	private JTextField url = new JTextField(40);
	private JTextField pathLocal = new JTextField(40);
    private JButton bLocal = new JButton(); 
	private JButton bPublic = new JButton();
	private JButton bExport = new JButton();
	protected JButton bNext = new JButton();
	JProgressBar progressHpx = new JProgressBar(0,100);
	
	private Aladin aladin;
	MainPanel allsky;
	private String mapfile;
	
	public TabPub(Aladin a,MainPanel allskyPanel) {
		super(new BorderLayout());
		aladin = a;
		allsky = allskyPanel;
		createChaine();
		
		Border emptyBorder = BorderFactory.createEmptyBorder(20, 0, 0, 0);
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(1, 2, 1, 2);
		c.anchor = GridBagConstraints.NORTHWEST;
	    c.gridy = 0;
	    initBtn();
		
		JPanel pCenter = new JPanel();
		pCenter.setLayout(new GridBagLayout());
		pCenter.setBorder(BorderFactory.createEmptyBorder(5, 20, 0,20));
		
	    // local
        c.gridy++;
	    c.gridx = 0;
	    JLabel titleLocal = new JLabel(getString("PUBFORYOUALLSKY"));
	    titleLocal.setFont(titleLocal.getFont().deriveFont(Font.BOLD));
	    pCenter.add(titleLocal,c);
	    c.gridy++;
	    c.gridwidth =GridBagConstraints.REMAINDER;// remplit toute la ligne
	    pCenter.add(new JLabel(LOCAL_FULL),c);
	    c.gridy++;	    
	    pCenter.add(pathLocal,c);
	    c.gridwidth=1;
	    c.gridy++;
	    c.gridx=0;
	    
	    // Export HPX
	    JLabel titleHPX = new JLabel(Util.fold(getString("PUBMAPALLSKY"),80,true));
	    titleHPX.setFont(titleHPX.getFont().deriveFont(Font.BOLD));
	    titleHPX.setBorder(emptyBorder);
	    pCenter.add(titleHPX,c);
	    c.gridy++;
	    c.gridwidth =GridBagConstraints.REMAINDER;// remplit toute la ligne
	    pCenter.add(new JLabel(Util.fold(getString("PUBMAPINFOALLSKY"),80,true)),c);

		// barre de progression
		progressHpx.setStringPainted(true);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridy++;c.gridx=0;
		JPanel pProgress = new JPanel(new BorderLayout());
		pProgress.setBorder(new EmptyBorder(5, 15, 5, 15));
		pProgress.add(progressHpx);
		pCenter.add(progressHpx,c);
		c.fill = GridBagConstraints.NONE;
	    c.gridwidth=1;
	    c.gridx=0;c.gridy++;
	    pCenter.add(bExport,c);
	    
	    // restricted
	    c.gridy++;
	    c.gridx = 0;
	    JLabel titleRestricted = new JLabel(getString("PUBRESTALLSKY"));
	    titleRestricted.setFont(titleLocal.getFont());
	    titleRestricted.setBorder(emptyBorder);
	    pCenter.add(titleRestricted,c);
	    c.gridy++;
	    c.gridwidth =GridBagConstraints.REMAINDER;// remplit toute la ligne
	    pCenter.add(new JLabel(Util.fold(getString("PUBRESTINFOALLSKY"),80,true)),c);
	    c.gridy++;
	    c.fill = GridBagConstraints.NONE;
	    c.gridx=0;
	    c.gridwidth = 1;
	    pCenter.add(url,c);
	    c.gridx++;c.gridy++;
	    
	    // public
	    c.gridy++;
	    c.gridx = 0;
	    JLabel titlePublic = new JLabel(getString("PUBLICALLSKY"));
	    titlePublic.setFont(titleLocal.getFont());
	    titlePublic.setBorder(emptyBorder);
	    pCenter.add(titlePublic,c);
	    c.gridy++;
	    c.gridwidth =GridBagConstraints.REMAINDER;// remplit toute la ligne
	    pCenter.add(new JLabel(Util.fold(getString("PUBLICINFOALLSKY"),80,true)),c);
	    c.gridwidth=1;
	    c.gridy++;
	    c.fill = GridBagConstraints.NONE;
	    c.gridx=0;
	    pCenter.add(bPublic,c);

		// composition du panel principal
		add(pCenter, BorderLayout.CENTER);
        setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}

	/**
	 * 
	 */
	private void initBtn() {
		bNext = new JButton(NEXT);
		bNext.setEnabled(false);
		bNext.addActionListener(this);
		bExport.setText(EXPORT);
		bExport.addActionListener(this);
		bExport.setEnabled(false);
		bPublic.setText(PUBLISH);
		bPublic.addActionListener(this);
		bPublic.setEnabled(false);
	    bLocal.setText(OPEN);
	    bLocal.addActionListener(this);
		bLocal.setEnabled(false);
	}
	
	public void clearForms() {
		url.setText("");
	}
	
	private void createChaine() {
		LOCAL_FULL = getString("OPENALLSKY");
		OPEN = getString("MOPENLOAD");
		NEXT = getString("NEXT");
		PUBLISH = getString("PUBLISHALLSKY");
		EXPORT = getString("EXPORTALLSKY");
	}

	private String getString(String k) { return allsky.aladin.getChaine().getString(k); }

	public void newAllskyDir(String dir) {
	    url.setText("http://servername.org/"+dir);
	    url.repaint();
	    if (allsky == null)
	    	pathLocal.setText(dir);
	    else
	    	pathLocal.setText(allsky.getOutputPath());
	    	
	    url.repaint();
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand() == PUBLISH) {
		   new FrameGlu(aladin,allsky.getOrder(),allsky.hasJpg());
		}
		else if (ae.getSource() == bExport) {
			mapfile = dirBrowserHPX();
			if (mapfile == null) return;
			bExport.setSelected(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			mapfile=aladin.getFullFileName(mapfile);
			ExportThread hpxThread = new ExportThread(allsky, mapfile);
			hpxThread.start();
			(new ThreadProgressBar(hpxThread)).start();

		}
		else if (ae.getSource() == bNext) {
			allsky.showRgbTab();
		}
	}

	/** Ouverture de la fenêtre de sélection d'un fichier
	 * @return chemin avec le nom du fichier ou null si annulé 
	 * */
	private String dirBrowserHPX() {
		FileDialog fd = new FileDialog(aladin.frameAllsky,"Running directory selection",FileDialog.SAVE);
		fd.setDirectory(allsky.getOutputPath());
//		fd.setFile("Allsky.hpx");
		fd.setVisible(true);
		if( fd.getFile()==null ) return null;
		else return fd.getDirectory()+fd.getFile();
	}

	public void setStartEnabled(boolean enabled) {
		bExport.setEnabled(enabled);
		bLocal.setEnabled(enabled);
		bPublic.setEnabled(enabled);
		bNext.setEnabled(enabled);
	}
	

	public void setProgress(int value) {
		progressHpx.setValue(value);
		if (value==100)
			Aladin.info(this,"Your HEALPix map has been successfully created\n"+mapfile);
	}
	
	class ThreadProgressBar implements Runnable {
		Object thread;
		public ThreadProgressBar(Object source) {
			thread = source;
		}
				
		public synchronized void start(){
			// lance en arrière plan le travail
			(new Thread(this)).start();
		}
		public void run() {
			int value = 0;
			while(thread != null && value < 99) {
				value = (int)((ExportThread)thread).getProgress();
				setProgress(value);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			setProgress(100);
			setCursor(null);

			bExport.setSelected(false);
		}
	}
	
    class ExportThread implements Runnable {
	    String outfile;
	    MainPanel allsky;
	    int progress=0;
	    
	    public ExportThread(MainPanel allsky, String filename) {
	        this.allsky=allsky;
	        outfile = filename;
	    }

	    public int getProgress() {
	        File f = new File(outfile);
	        if (!f.exists())
	            return 0;
	        long size = f.length()/1024/1024;
	        // la taille d'un fichier avec nside=4096 et bitpix=-32 est 768M
	        long sizeFin = 4096*4096*12*(Math.abs(allsky.getBitpix()/8))/1024/1024;
	        return (int) (100*size/sizeFin);
	    }

	    public synchronized void start(){
	        (new Thread(this)).start();
	    }
	    
	    public void run() {
	        File f = new File(outfile);
	        f.delete();
	        allsky.export(outfile);
	    }
	}

}

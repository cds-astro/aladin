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
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import cds.tools.Util;

public class AllskyStepsPanel extends JPanel {
	JPanel p1Index = new JPanel();
	JPanel p2Tess = new JPanel();
//	JPanel p3Jpg = new JPanel();

	String string1 = "Healpix Indexation";
	String string2 = "Sky tesselation";
//	String string3 = "Preview Jpg";
	
	JLabel labelIndex = new JLabel(string1);
	JLabel labelTess = new JLabel(string2);
//	JLabel labelJpg = new JLabel(string3);
	
	JProgressBar progressIndex = new JProgressBar(0,100);
	JProgressBar progressTess = new JProgressBar(0,100);
//	JProgressBar progressJpg = new JProgressBar(0,100);
	
    Border border1 = BorderFactory.createLineBorder(Color.GRAY);
    Border border2 = BorderFactory.createLineBorder(Color.GRAY);
//	Border border3 = BorderFactory.createLineBorder(Color.GRAY);
	
	public static final int INDEX = AllskyConst.INDEX;
	public static final int TESS = AllskyConst.TESS;
	public static final int JPG = AllskyConst.JPG;

	
	public AllskyStepsPanel() {
		super(new GridLayout(1,5,0,10));

		p1Index.setLayout(new BorderLayout());
		p2Tess.setLayout(new BorderLayout());
//		p3Jpg.setLayout(new BorderLayout());
		
//		p1Index.setBorder(border1);
//		p2Tess.setBorder(border2);
//		p3Jpg.setBorder(border3);

		select(true,INDEX);
		select(true,TESS);
//		select(true,JPG);
		
		p1Index.add(labelIndex, BorderLayout.NORTH);
		p2Tess.add(labelTess, BorderLayout.NORTH);
//		p3Jpg.add(labelJpg, BorderLayout.NORTH);
		
		progressIndex.setStringPainted(true);
		progressTess.setStringPainted(true);

		p1Index.add(progressIndex, BorderLayout.SOUTH);
		p2Tess.add(progressTess, BorderLayout.SOUTH);
//		p3Jpg.add(progressJpg, BorderLayout.SOUTH);

		add(p1Index);
//		add(new JLabel(Util.getAscSortIcon()));
		add(p2Tess);
//		add(new JLabel(Util.getAscSortIcon()));
//		add(p3Jpg);
		
	}
	
	public void clearForms() {
		progressTess.setValue(0);
		progressIndex.setValue(0);
		labelIndex.setText(string1);
		select(true,INDEX);
		select(true,TESS);
	}
	
	public void setProgressTess(int value) {
		progressTess.setValue(value);
		progressIndex.repaint();
	}
	public void setProgressIndex(int value) {
		progressIndex.setValue(value);
		progressIndex.repaint();
	}


	public void select(boolean enabled, int index) {
		JPanel p = null;
		Border bord=null;
		JLabel label = null;
		switch (index) {
		   case INDEX : label = labelIndex; bord = border1; p = p1Index; break;
		   case TESS : label = labelTess; bord = border2;  p = p2Tess; break;
		   //		case JPG : label = labelJpg; bord = border2;  p = p3Jpg; break;
		}
		if (!enabled) {
			label.setFont(getFont().deriveFont(Font.ITALIC));
			label.setForeground(Color.LIGHT_GRAY);
			bord = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
		}
		else {
			label.setFont(getFont().deriveFont(Font.PLAIN));
			label.setForeground(Color.BLACK);
			bord = BorderFactory.createLineBorder(Color.GRAY);
		}
//		p.setBorder(bord);
		repaint();
	}


	public JProgressBar getProgressTess() {
		return progressTess;
	}


	public void setProgressIndexTxt(String txt) {
		labelIndex.setText(string1+" "+txt);
		labelIndex.setPreferredSize(labelIndex.getSize());
	}
	
	
}

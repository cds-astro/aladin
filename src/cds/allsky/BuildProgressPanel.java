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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import cds.aladin.Properties;
import cds.tools.Util;

public class BuildProgressPanel extends JPanel {
   private JPanel p1Index = new JPanel();
   private JPanel p2Tess = new JPanel();

   private String string1 = "Healpix Indexation";
   private String string2 = "Sky tesselation";

   private JLabel labelIndex = new JLabel(string1);
   private JLabel labelTess = new JLabel(string2);

   private JProgressBar progressIndex = new JProgressBar(0,100);
   private JProgressBar progressTess = new JProgressBar(0,100);

   private Border border1 = BorderFactory.createLineBorder(Color.GRAY);
   private Border border2 = BorderFactory.createLineBorder(Color.GRAY);


   public BuildProgressPanel() {
      setLayout(new BorderLayout());
      add( createProgressPanel(),BorderLayout.NORTH );
      add( createStatPanel(),BorderLayout.CENTER );
   }


   protected void setSrcStat(int nbFile,long totalSize,long maxSize,int maxWidth,int maxHeight,int maxNbyte) {
      String s;
      if( nbFile==-1 ) s = "--";
      else {
         s= nbFile+" file"+(nbFile>1?"s":"")
         + " using "+Util.getUnitDisk(totalSize)
         + (nbFile>1 && maxSize<0 ? "" : " (biggest file:"+Util.getUnitDisk(maxSize)+" => ["+maxWidth+"x"+maxHeight+"x"+maxNbyte+"])");
      }
      srcFileStat.setText(s);
   }

   protected void setMemStat(int nbRunningThread,int nbThread) {
      long maxMem = Runtime.getRuntime().maxMemory();
      long totalMem = Runtime.getRuntime().totalMemory();
      long freeMem = Runtime.getRuntime().freeMemory();
      long usedMem = totalMem-freeMem;

      String s= (nbRunningThread==-1?"":nbRunningThread+" / "+nbThread+" thread"+(nbRunningThread>1?"s":""))
      + " using "+Util.getUnitDisk(usedMem)+"/"+Util.getUnitDisk(maxMem);

      memStat.setText(s);
   }

   protected void setLowTileStat(int nbTile,long sizeTile,long minTime, long maxTime, long avgTime) {
      String s;
      if( nbTile==-1 ) s="";
      else 
       s= nbTile+" tile"+(nbTile>1?"s":"")
          + " using "+Util.getUnitDisk(sizeTile*nbTile)
          + " - processing time: min:"+Util.getTemps(minTime)+" / max:"+Util.getTemps(maxTime)+" / avg:"+Util.getTemps(avgTime);

      lowTileStat.setText(s);
   }
   
   protected void setNodeTileStat(int nbTile,long sizeTile, long avgTime) {
      String s;
      if( nbTile==-1 ) s="";
      else 
       s= nbTile+" tile"+(nbTile>1?"s":"")
          + " using "+Util.getUnitDisk(sizeTile*nbTile)
          + " - avg processing time: "+Util.getTemps(avgTime);

      nodeTileStat.setText(s);
   }
   
   private JLabel srcFileStat,memStat,lowTileStat,nodeTileStat;

   private JPanel createStatPanel() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,10,2,2);
      JPanel p = new JPanel(g);

      srcFileStat = new JLabel("--");
      Properties.addCouple(p, ".Original images: ", srcFileStat, g, c);           

      memStat = new JLabel("--");
      Properties.addCouple(p, ".Processing info: ", memStat, g, c);           

      lowTileStat = new JLabel("--");
      Properties.addCouple(p, ".Low level tiles: ", lowTileStat, g, c);           

      nodeTileStat = new JLabel("--");
      Properties.addCouple(p, ".Tree tiles: ", nodeTileStat, g, c);           

      return p;
   }


   private JPanel createProgressPanel() {
      JPanel p = new JPanel(new GridLayout(1,2,5,5));
      p1Index.setLayout(new BorderLayout());
      p2Tess.setLayout(new BorderLayout());

      select(true,Constante.INDEX);
      select(true,Constante.TESS);

      p1Index.add(labelIndex, BorderLayout.NORTH);
      p2Tess.add(labelTess, BorderLayout.NORTH);

      progressIndex.setStringPainted(true);
      progressTess.setStringPainted(true);

      p1Index.add(progressIndex, BorderLayout.SOUTH);
      p2Tess.add(progressTess, BorderLayout.SOUTH);

      p.add(p1Index);
      p.add(p2Tess);
      return p;
   }

   public void clearForms() {
      progressTess.setValue(0);
      progressIndex.setValue(0);
      labelIndex.setText(string1);
      select(true,Constante.INDEX);
      select(true,Constante.TESS);
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
         case Constante.INDEX : label = labelIndex; bord = border1; p = p1Index; break;
         case Constante.TESS : label = labelTess; bord = border2;  p = p2Tess; break;
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

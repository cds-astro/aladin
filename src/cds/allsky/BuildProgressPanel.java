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

import cds.aladin.prop.PropPanel;
import cds.fits.CacheFits;
import cds.tools.Util;

public class BuildProgressPanel extends JPanel {
   private JPanel p1Index = new JPanel();
   private JPanel p2Tess = new JPanel();

   private String string1 = "Healpix Indexation";
   private String string2 = "Sky tesselation";

   private JLabel labelIndex = new JLabel(string1);
   private JLabel labelTess = new JLabel(string2);

   private JProgressBar progressBarIndex = new JProgressBar(0,100);
   private JProgressBar progressBarTile = new JProgressBar(0,100);


   public BuildProgressPanel() {
      setLayout(new BorderLayout());
      add( createProgressPanel(),BorderLayout.NORTH );
      add( createStatPanel(),BorderLayout.CENTER );
   }

   protected void setSrcStat(int nbFile,int nbZipFile, long totalSize,long maxSize,int maxWidth,int maxHeight,int maxDepth,int maxNbyte) {
      String s;
      if( nbFile==-1 ) s = "--";
      else {
         s= nbFile+" file"+(nbFile>1?"s":"")
         + (nbZipFile==nbFile ? " (all gzipped)" : nbZipFile>0 ? " ("+nbZipFile+" gzipped)":"")
         + " using "+Util.getUnitDisk(totalSize)
         + (nbFile>1 && maxSize<0 ? "" : " => biggest: ["+maxWidth+"x"+maxHeight+(maxDepth>1?"x"+maxDepth:"")+" x"+maxNbyte+"]");
      }
      srcFileStat.setText(s);
   }

   protected void srcFileStat(String s) {
      srcFileStat.setText(s);
   }
   
   protected void setMemStat(int nbRunningThread,int nbThread,CacheFits cacheFits) {
      long maxMem = Runtime.getRuntime().maxMemory();
      long totalMem = Runtime.getRuntime().totalMemory();
      long freeMem = Runtime.getRuntime().freeMemory();
      long usedMem = totalMem-freeMem;

      String s= (nbThread>1?"thread: "+(nbRunningThread==-1?"":nbRunningThread+" / "+nbThread)+" - ":"")
      + "RAM: "+Util.getUnitDisk(usedMem)+"/"+Util.getUnitDisk(maxMem)
      + " (FITS cache: "+Util.getUnitDisk(cacheFits.getMem())+")";
      setMemStat(s);
   }
   
   protected void setMemStat(String s) {
      memStat.setText(s);
   }
   
   protected void setLowTileStat(int nbTile,int nbEmptyTile,long nbCells,long sizeTile,long minTime, long maxTime, long avgTime) {
      String s;
      if( nbTile==-1 ) s="";
      else 
       s= nbTile+"+"+nbEmptyTile+"/"+nbCells+" tile"+(nbTile>1?"s":"")
          + " for "+Util.getUnitDisk(sizeTile*nbTile)
          + " - avg.proc.time: "+Util.getTemps(avgTime)+" ["+Util.getTemps(minTime)+" .. "+Util.getTemps(maxTime)+"]";
      setLowTileStat(s);
   }
   
   protected void setLowTileStat(String s) {
      lowTileStat.setText(s);
   }
   
   protected void setNodeTileStat(int nbTile,long sizeTile, long avgTime) {
      String s;
      if( nbTile==-1 ) s="";
      else 
       s= nbTile+" tile"+(nbTile>1?"s":"")
          + " for "+Util.getUnitDisk(sizeTile*nbTile)
          + " - avg.proc.time: "+Util.getTemps(avgTime);
      setNodeTileStat(s);
   }
   
   protected void setNodeTileStat(String s) {
      nodeTileStat.setText(s);
   }

   protected void setTimeStat(long time,long nbTilesPerMin,long tempsTotalEstime) {
      StringBuilder s = new StringBuilder();
      if( time!=-1 )  s.append((tempsTotalEstime>0?"running ":"")+Util.getTemps(time,true));
      if(  time>5000 && tempsTotalEstime>0 ) s.append(" - "+nbTilesPerMin+" tiles/mn");
      if( time>20000 && tempsTotalEstime>0 ) s.append(" - ends in "+Util.getTemps(tempsTotalEstime,true));
      setTimeStat(s+"");
   }
   
   protected void setTimeStat(String s) {
      timeStat.setText(s);
   }

   private JLabel srcFileStat,memStat,lowTileStat,nodeTileStat,timeStat;

   private JPanel createStatPanel() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,10,2,2);
      JPanel p = new JPanel(g);

      srcFileStat = new JLabel("--");
      PropPanel.addCouple(p, ".Original images: ", srcFileStat, g, c);           

      memStat = new JLabel("--");
      PropPanel.addCouple(p, ".Processing info: ", memStat, g, c);           

      lowTileStat = new JLabel("--");
      PropPanel.addCouple(p, ".Low level tiles: ", lowTileStat, g, c);           

      nodeTileStat = new JLabel("--");
      PropPanel.addCouple(p, ".Tree tiles: ", nodeTileStat, g, c);           

      timeStat = new JLabel("--");
      PropPanel.addCouple(p, ".Time: ", timeStat, g, c);           

      return p;
   }


   private JPanel createProgressPanel() {
      JPanel p = new JPanel(new GridLayout(1,2,5,5));
      p1Index.setLayout(new BorderLayout());
      p2Tess.setLayout(new BorderLayout());

      select(true,Constante.PANEL_INDEX);
      select(true,Constante.PANEL_TESSELATION);

      p1Index.add(labelIndex, BorderLayout.NORTH);
      p2Tess.add(labelTess, BorderLayout.NORTH);

      progressBarIndex.setStringPainted(true);
      progressBarTile.setStringPainted(true);

      p1Index.add(progressBarIndex, BorderLayout.SOUTH);
      p2Tess.add(progressBarTile, BorderLayout.SOUTH);

      p.add(p1Index);
      p.add(p2Tess);
      return p;
   }
   
   protected void resetProgressBar() {
      for( JProgressBar bar : new JProgressBar[]{ progressBarIndex, progressBarTile} ) {
         bar.setIndeterminate(false);
         bar.setValue(0);
         bar.setMaximum(100);
         bar.setString(null);
      }
   }

   public void clearForms() {
      progressBarTile.setValue(0);
      progressBarIndex.setValue(0);
      labelIndex.setText(string1);
      select(true,Constante.PANEL_INDEX);
      select(true,Constante.PANEL_TESSELATION);
      resetProgressBar();
   }

   public void select(boolean enabled, int index) {
      JLabel label = null;
      switch (index) {
         case Constante.PANEL_INDEX : label = labelIndex; break;
         case Constante.PANEL_TESSELATION : label = labelTess; break;
      }
      if (!enabled) {
         label.setFont(getFont().deriveFont(Font.ITALIC));
         label.setForeground(Color.LIGHT_GRAY);
      }
      else {
         label.setFont(getFont().deriveFont(Font.PLAIN));
         label.setForeground(Color.BLACK);
      }
      repaint();
   }


   public JProgressBar getProgressBarTile() { return progressBarTile; }
   public JProgressBar getProgressBarIndex() { return progressBarIndex; }

   public void setProgressIndexDir(String txt) {
      labelIndex.setText(string1+" "+txt);
      labelIndex.setPreferredSize(labelIndex.getSize());
   }


}

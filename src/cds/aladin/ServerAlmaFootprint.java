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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ServerAlmaFootprint extends Server {
    private List<ALMASetup> setups = new ArrayList<ALMASetup>();

    private JComboBox setupList;
    private JTextField widthField;
    private JTextField heightField;


    protected ServerAlmaFootprint(Aladin aladin) {
        this.aladin = aladin;
        this.init();
        this.createChaine();

        this.buildGUI();
    }

    /** Initialisation des variables propres au footprint ALMA */
    protected void init() {
       type    = APPLI;
       aladinMenu = "FoV...";
       aladinLabel   = "ALMA footprint";
       aladinLogo    = "ALMALogo.gif";
       grab = null;

       // ajout des différents setups
       setups.add(new ALMASetup(3, 44.7f, new float[] {2.6f, 3.6f}, new float[] { 84, 116}));
       setups.add(new ALMASetup(4, 30.9f, new float[] {1.8f, 2.4f}, new float[] {125, 169}));
       setups.add(new ALMASetup(5, 24.1f, new float[] {1.4f, 1.8f}, new float[] {163, 211}));
       setups.add(new ALMASetup(6, 18.9f, new float[] {1.1f, 1.4f}, new float[] {211, 275}));
       setups.add(new ALMASetup(7, 13.8f, new float[] {0.8f, 1.1f}, new float[] {275, 373}));
       setups.add(new ALMASetup(8, 10.3f, new float[] {0.6f, 0.8f}, new float[] {385, 500}));
       setups.add(new ALMASetup(9,  6.9f, new float[] {0.4f, 0.5f}, new float[] {602, 720}));
    }

    protected void createChaine() {
        // TODO : localisation
        title = aladin.chaine.getString("ALTITLE");
        description = aladin.chaine.getString("ALINFO");
        super.createChaine();
     }

     private void buildGUI() {
         setBackground(Aladin.BLUE);
         setLayout(null);
         setFont(Aladin.PLAIN);

         int y =  50;
         int x = 150;

         // Le titre
         JPanel tp = new JPanel();
         Dimension d = makeTitle(tp,title);
         tp.setBackground(Aladin.BLUE);
         tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
         add(tp);

         // Un texte d'aide pour remplit le formulaire
         JLabel l = new JLabel(description);
         l.setBounds(90,y,400, 20); y+=20;
         add(l);

         // JPanel pour la memorisation du target (+bouton DRAG)
         JPanel tPanel = new JPanel();
         tPanel.setBackground(Aladin.BLUE);
         int h = makeTargetPanel(tPanel, NORADIUS);
         tPanel.setBounds(0,y,XWIDTH,h); y+=h;
         add(tPanel);

         modeCoo=COO|SIMBAD;

         // JComboBox avec listes des setups
         JLabel setuptTitle = new JLabel("Receiver band");
         setuptTitle.setBounds(XTAB1,y,XTAB2-10,HAUT);
         add(setuptTitle);
         this.setupList = new JComboBox();
         for (ALMASetup almaSetup: this.setups) {
             this.setupList.addItem(almaSetup);
         }
         add(this.setupList);
         this.setupList.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT);
         y+=HAUT+MARGE;

         // Footprint width
         JLabel widthTitle = new JLabel("Width (arcmin)");
         widthTitle.setBounds(XTAB1,y,XTAB2-10,HAUT);
         add(widthTitle);
         this.widthField = new JTextField("5");
         add(this.widthField);
         this.widthField.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT);
         y+=HAUT+MARGE;

         // Footprint height
         JLabel heightTitle = new JLabel("Height (arcmin)");
         heightTitle.setBounds(XTAB1,y,XTAB2-10,HAUT);
         add(heightTitle);
         this.heightField = new JTextField("2");
         add(this.heightField);
         this.heightField.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT);

     }

     public void submit() {
         String t = getTarget();
         ALMASetup selectedSetup = (ALMASetup) this.setupList.getItemAt(this.setupList.getSelectedIndex());

         double width, height;
         try {
             width  = Double.parseDouble(this.widthField.getText())/60.;
             height = Double.parseDouble(this.heightField.getText())/60.;
         }
         catch(NumberFormatException nfe) {
             Aladin.warning("Incorrect value for width/height !");
             return;
         }


         FootprintBean fpBean = new FootprintBean();
         float beam = selectedSetup.primaryBeam/3600f;
         Set<Point2D> centers = new TreeSet<Point2D>();
         double x, y;
         double ymax = 0;
         for (int i=0; (x = i * beam * 0.5 * 0.5)<=width/2; i++) {

             for (int j=0; (y = j * beam * Math.sqrt(3) * 0.5)<=height/2+beam/2; j++) {
                 if (i%2==1) {
                     y += Math.sqrt(3)/4*beam;
                 }

                 int signX, signY;
                 for (int k=0; k<4; k++) {
                     if (k%2==0) {
                         signX = 1;
                     }
                     else {
                         signX = -1;
                     }
                     if (k<2) {
                         signY = 1;
                     }
                     else {
                         signY = -1;
                     }

                     centers.add(new Point2D(signX * x, signY * y));
                     if (ymax<y) {
                         ymax = y;
                     }


                 }
             }
         }

         ymax *= 1.3;
         // ajout d'une poignée de rotation (une droite + un pickle)
         fpBean.addSubFootprintBean(new SubFootprintBean(new double[] {0, 0}, new double[] {0, ymax}, "handle"));
         fpBean.addSubFootprintBean(new SubFootprintBean(0, ymax, 270, 180, 0, 1e-3, "handle"));


         // ajout des cercles correspondant aux beams
         for (Point2D center: centers) {
             fpBean.addSubFootprintBean(new SubFootprintBean(center.x, center.y, beam/2., null));
         }

         synchronized(aladin.calque) {
             int idx = aladin.calque.newPlanField(fpBean, t, "ALMA", 0);
             ((PlanField)aladin.calque.plan[idx]).setIsAlmaFP(true);
             ((PlanField)aladin.calque.plan[idx]).setOpacityLevel(0.05f); // set low opacity level for ALMA footprints
         }
     }

     static public class Point2D implements Comparable<Point2D> {
         private double x;
         private double y;

         public Point2D(double x, double y) {
             this.x = x;
             this.y = y;
         }

         public boolean equals(Object obj) {
             if (! (obj instanceof Point2D)) {
                 return false;
             }
             Point2D p = (Point2D)obj;
             return p.x==this.x && p.y==this.y;
         }

         public int compareTo(Point2D p) {
             double eps = 0.00001;
             if (Math.abs(p.x-this.x)<eps && Math.abs(p.y-this.y)<eps) {
                 return 0;
             }
             return 1;
         }
     }

     static public class ALMASetup {
         int bandId;
         float primaryBeam;
         float[] wavelengthRange;
         float[] frequencyRange;

         /**
          *
          * @param primaryBeam primary beam in arcsec
          * @param wavelengthRange wavelength range in mm {min; max}
          */
         public ALMASetup(int bandId, float primaryBeam, float[] wavelengthRange, float[] frequencyRange) {
             this.bandId = bandId;
             this.primaryBeam = primaryBeam;
             this.wavelengthRange = wavelengthRange;
             this.frequencyRange = frequencyRange;
         }

         public String toString() {
             return   "Band " + this.bandId + " | "
                    + this.wavelengthRange[0] + "-" + this.wavelengthRange[1] + " mm" + " | "
                    + this.frequencyRange[0]  + "-" + this.frequencyRange[1]  + " GHz";
         }
     }

}

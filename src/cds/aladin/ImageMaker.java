// Copyright 2015 - UDS/CNRS
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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;


/**
* Generateur d'image statique
*
* @author Pierre Fernique [CDS]
* @version 1.0 : oct 2015
*/
public class ImageMaker {


   public ImageMaker(Aladin aladin,OutputStream output, String label, int width, int height,
                     String fmt, Coord c, double radius) throws Exception {
       this(aladin, output, label, width, height, fmt, c, radius, null);
   }


   public ImageMaker(Aladin aladin,OutputStream output, String label, int width, int height,
                     String fmt, Coord c, double radius, String url) throws Exception {

      if (url==null) {
          url = "http://alasky.u-strasbg.fr/DSS/DSSColor";
          /* url = "C:\\Users\\Pierre\\.aladin\\Cache\\Background\\CDS_P_DSS2_color" */

      }
      // Generation d'un PlanBG de travail
      PlanBG p = new PlanBGStatic(aladin, label, url);

//      ViewSimpleStatic vs = (ViewSimpleStatic) aladin.view.getCurrentView();
      ViewSimpleStatic vs = new ViewSimpleStatic(aladin);
      ((ViewStatic)aladin.view).setViewSimple(vs);
      vs.setViewParam(p, width,height, c,radius);

      // Il me faut un BufferImage de travail
      BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = (Graphics2D) img.getGraphics();

      // L'arrière fond
      p.drawBackground(g, vs);

      // Génération d'une vue de travail
      // Dessin de l'image de fond
      p.drawLosangesNow(g, vs);

      // L'avant fond
      p.drawForeground(g, vs);

      // Quelques overlays
//      vs.drawScale(g, vs, 0, 0);
//      vs.drawGrid(g, null, 0, 0);
//      vs.drawLabel(g,0, 0);
//      vs.drawSize(g,0, 0);
//      vs.drawNE(g,p.projd,0, 0);

      // Je génère le flux de l'image finale
      ImageIO.write(img, fmt, output);
   }

   static public Aladin createAladin() throws Exception {
      Aladin aladin = new Aladin();
      aladin.creatFonts();
      aladin.levelTrace=3;
      Aladin.aladin=aladin;
      aladin.configuration = new Configuration(aladin);
      aladin.configuration.load();
      aladin.localisation = new LocalisationStatic(aladin);
      aladin.view = new ViewStatic(aladin);
      aladin.calque = new CalqueStatic(aladin,(ViewStatic)aladin.view);

      return aladin;
   }

   static public void main(String [] argv) {
       int n=1;
       long t0=0;
       try {

          Aladin aladin = createAladin();
          t0 = System.currentTimeMillis();

          for( int i=0; i<n; i++ ) {
             FileOutputStream output = new FileOutputStream( new File("/Test"+i+".jpg"));
             new ImageMaker(aladin,output, "M101 test", 15000,15000, "jpeg", new Coord(210.80242,+54.34875/*056.61431,+24.13817*/), 2);
          }

          long t1 =System.currentTimeMillis();
          System.out.println("Image generated in "+(t1-t0)/n+"ms");

       }catch( Exception e ) {
          e.printStackTrace();
       }
       System.exit(0);
    }
}

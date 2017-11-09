// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;


/**
* Generateur d'images statiques, soit des previews (JPEG o PNG), soit des FITS utilisant Aladin
* en mode batch ou en frontal sur un serveur HiPS. Les classes relatives finissent par le suffixe "Static"
* (AladinStatic, PlanBGStatic, CalqueStatic, LocalisationStatic...)
* 
* Note: un main() ci-dessous montre quelques exemples d'utilisations
* 
* @author Pierre Fernique [CDS]
* @version 2.0 : nov 2017 - refonte complète
* @version 1.0 : oct 2015 - prototypage
*/
public class ImageMaker {
   
   private Aladin aladin=null;
   
   /** Fournit une "fabrique" d'images, soit preview, soit fits. A utiliser en batch
    * ou en frontal sur un serveur, de préférence localisé directement sur le serveur HiPS
    * dont on veut générer des images.
    * @param trace niveau de verbosité (0-rien, 3-beaucoup, 6-debug
    * @throws Exception
    */
   public ImageMaker() throws Exception { this(0); }
   public ImageMaker(int trace) throws Exception {
      // Création d'une instance unique d'Aladin
      if( aladin==null ) {
         synchronized( this ) {
            if( aladin==null ) aladin = new AladinStatic(trace);
         }
      }
   }
   
   /**
    * Génère une image "preview" en JPEG ou en PNG de la région indiquée, pour le HiPS passé en
    * paramètre. L'image contiendra une calibration astrométrique (segment commentaire), mais
    * il ne s'agira que d'une approximation (preview) dans le sens où il n'y aura pas de 
    * rééchantillonnage pixel à pixel. La méthode est rapide (à condition que les losanges
    * HiPS arrivent soient disponibles).
    * 
    * @param hipsUrlOrPath URL ou path du HiPS dont on veut extraire une image
    * @param raICRS Ascension droite (ICRS en degrés) du centre de l'image
    * @param deICRS Déclinaison (ICRS en degrées) du centre de l'image
    * @param radDeg Rayon (en degrées) de l'image, càd demi-diagonale
    * @param filename le nom du fichier de sortie
    * @param width La largeur (en pixels) de l'image à générer
    * @param height la hauteur (en pixels) de l'image à générer
    * @param fmt le format (soit jpeg, soit png)
    * @param overlays liste des overlays souhaités (grid,scale,NE,label,size)
    * @throws Exception
    */
   public void preview(String hipsUrlOrPath, double raICRS, double deICRS, double radDeg, 
         String filename, int width, int height, String fmt,String overlays) throws Exception {
      
      FileOutputStream output=null;
      try {
        output = new FileOutputStream( new File(filename) );
        preview( hipsUrlOrPath,raICRS,deICRS,radDeg, output,width,height, fmt,overlays);
      } finally {
         if( output!=null ) output.close();
      }
   }
   
   /**
    * Génère une image "preview" en JPEG ou en PNG de la région indiquée, pour le HiPS passé en
    * paramètre. L'image contiendra une calibration astrométrique (segment commentaire), mais
    * il ne s'agira que d'une approximation (preview) dans le sens où il n'y aura pas de 
    * rééchantillonnage pixel à pixel. La méthode est rapide (à condition que les losanges
    * HiPS arrivent soient disponibles).
    * 
    * Note: le flux de sortie n'est pas fermé
    * 
    * @param hipsUrlOrPath URL ou path du HiPS dont on veut extraire une image
    * @param raICRS Ascension droite (ICRS en degrés) du centre de l'image
    * @param deICRS Déclinaison (ICRS en degrées) du centre de l'image
    * @param radDeg Rayon (en degrées) de l'image, càd demi-diagonale
    * @param output Le flux de sortie pour enregistrer l'image
    * @param width La largeur (en pixels) de l'image à générer
    * @param height la hauteur (en pixels) de l'image à générer
    * @param fmt le format (soit jpeg, soit png)
    * @param overlays liste des overlays souhaités (grid,scale,NE,label,size)
    * @throws Exception
    */
   public void preview(String hipsUrlOrPath, double raICRS, double deICRS, double radDeg, 
         OutputStream output, int width, int height, String fmt, String overlays) throws Exception {
      
      // Generation d'un PlanBG de travail
      PlanBG p = new PlanBGStatic( aladin, hipsUrlOrPath, false );
      
      // Génération de la vue de travail
      ViewSimpleStatic vs = new ViewSimpleStatic( aladin );
      ((ViewStatic)aladin.view).setViewSimple( vs );
      vs.setViewParam(p, width,height, new Coord(raICRS,deICRS) , radDeg);
      
      // Il me faut un BufferImage de sortie
      BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = (Graphics2D) img.getGraphics();

      // Dessin de l'arrière fond
      p.drawBackground(g, vs);

      // Dessin de l'image de fond
      p.drawLosangesNow(g, vs);
      
      // Les éventuels overlays
      if( overlays!=null ) {
         g.setColor( Color.yellow );
         g.setFont( aladin.BOLD );
         Tok tok = new Tok( overlays," ,;" );
         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
                 if( s.equals("grid") )    vs.drawGrid(g, null, 0, 0);
            else if( s.equals("scale") )   vs.drawScale(g, vs, 0, 0);
            else if( s.equals("size") )    vs.drawSize(g, 0, 0);
            else if( s.equals("NE") )      vs.drawNE(g,p.projd,0, 0);
            else if( s.equals("reticle") ) {
               aladin.view.repere=new Repere(p,new Coord(raICRS,deICRS));
               aladin.view.repere.setType(Repere.TARGET);
               vs.drawRepere(g, 0, 0);
            }
            else if( s.startsWith("label") ) {
               int i=s.indexOf('=');
               String label;
               if( i>0 ) label=s.substring(i+1);
               else label=(new Coord(raICRS,deICRS)).getSexa();
               p.label=label;
               vs.drawLabel(g, 0, 0);
            }
         }
      }
      vs.drawCredit(g, 0, 0);
      
      // Dessin de l'avant fond
      p.drawForeground(g, vs);
      
      // Je publie le flux de l'image finale
      ImageIO.write(img, fmt, output);
   }
   
   /**
    * Genère une image FITS de la région indiquée pour le HiPS passé en
    * paramètre. L'image obtenue sera un réchantillonnage pixel à pixel du HiPS d'origine
    * dans une grille FITS dont la résolution sera équivalente à la résolution maximale du HiPS.
    * Attention, suivant la taille de la région demandée, cette méthode peut s'avérer très lourde
    * car l'image finale aura la résolution du relevé original.
    * 
    * Note: le flux de sortie n'est pas fermé
    * 
    * @param hipsUrlOrPath URL ou path du HiPS dont on veut extraire une image
    * @param raICRS Ascension droite (ICRS en degrés) du centre de l'image
    * @param deICRS Déclinaison (ICRS en degrées) du centre de l'image
    * @param radDeg Rayon (en degrées) de l'image, càd demi-diagonale
    * @param filename le nom du fichier de sortie
    * @param width La largeur (en pixels) de l'image à générer
    * @param height la hauteur (en pixels) de l'image à générer
    * 
    * @throws Exception
    */
   public void fits(String hipsUrlOrPath, double raICRS, double deICRS, double radDeg, 
         String filename, int width, int height) throws Exception {
      
      FileOutputStream output=null;
      try {
        output = new FileOutputStream( new File(filename) );
        fits( hipsUrlOrPath,raICRS,deICRS,radDeg, output,width,height );
      } finally {
         if( output!=null ) output.close();
      }
   }
   
   /**
    * Genère une image FITS de la région indiquée pour le HiPS passé en
    * paramètre. L'image obtenue sera un réchantillonnage pixel à pixel du HiPS d'origine
    * dans une grille FITS dont la résolution sera équivalente à la résolution maximale du HiPS.
    * Attention, suivant la taille de la région demandée, cette méthode peut s'avérer très lourde
    * car l'image finale aura la résolution du relevé original.
    * 
    * Note: le flux de sortie n'est pas fermé
    * 
    * @param hipsUrlOrPath URL ou path du HiPS dont on veut extraire une image
    * @param raICRS Ascension droite (ICRS en degrés) du centre de l'image
    * @param deICRS Déclinaison (ICRS en degrées) du centre de l'image
    * @param radDeg Rayon (en degrées) de l'image, càd demi-diagonale
    * @param output Le flux de sortie pour enregistrer l'image
    * @param width La largeur (en pixels) de l'image à générer
    * @param height la hauteur (en pixels) de l'image à générer
    * @throws Exception
    */
   public void fits(String hipsUrlOrPath, double raICRS, double deICRS, double radDeg, 
         OutputStream output, int width, int height) throws Exception {
      
      // Generation d'un PlanBG de travail
      PlanBG p = new PlanBGStatic(aladin, hipsUrlOrPath, true );
      
      // Génération de la vue de travail
      ViewSimpleStatic vs = new ViewSimpleStatic(aladin);
      ((ViewStatic)aladin.view).setViewSimple(vs);
      
      // Positionnement de la zone de travail
      vs.setViewParam(p, width,height, new Coord(raICRS,deICRS),radDeg);
      
      // Extraction/rééchantillonnage des pixels de la vue
      PlanImage pi = aladin.calque.createCropImage(vs,true);
      
      // Sauvegarde dans le flux de sortie au format FITS
      Save save = new SaveStatic(aladin);
      save.saveImageFITS(output, pi);
   }
   
   /************************************************* Pour exemple et debug *****************************************/

   /** Juste pour démo */
   static public void main(String [] argv) {
      try {

         ImageMaker im  = new ImageMaker(6);         
//         System.out.println("Génération d'un preview de M101 pour le HiPS DSS couleur distant...");
//         im.preview( "http://alasky.u-strasbg.fr/DSS/DSSColor", 210.80242,+54.34875,130., 
//               "D:/Test.png", 512,512,"png", "grid label=M101 size NE reticle");
//
//         System.out.println("Génération d'un fits de Orion pour le HiPS Halpha local...");
//         im.fits( "D:/HalphaNorthHips", 084.93891,-01.96410,10, "D:/Test.fits", 1024,512);
         
         long t = System.currentTimeMillis();
         im.fits( "http://alasky.u-strasbg.fr/MAMA/CDS_P_MAMA_srcj", 247.55114, -25.12124, 0.5, "D:/Test.fits", 1800,1800);
//         im.fitsFull( "D:/SkymapperHips", 057.57646,-07.93148, 0.2, "D:/Test.fits", 512,1024);
         long t1 = System.currentTimeMillis();
         System.out.println("C'est terminé en "+(t1-t)/1000.+"s");
         
         
      }catch( Exception e ) {
         e.printStackTrace();
      }
   }
}

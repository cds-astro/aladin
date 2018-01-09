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

package cds.image;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.Date;

/**
 * Contexte graphics pour générer du EPS. C'est du sur-mesure Aladin ce qui signifie
 * que les méthodes non utilisées par Aladin n'ont pas été (encore) implantées.
 * @author Pierre Fernique
 *
 */
public class EPSGraphics extends Graphics {

   private PrintStream out;         // Le flux de sortie
   private Image preview;           // L'image pour un préview EPS
   private String title;            // Le titre de l'entête EPS
   private int xmin,ymin,xmax,ymax; // La "bounding box"
   private int height;              // Hauteur de la bounding box
   private int mode=STROKE;         // mode de tracage
   private int size;                // Nombre d'octets (approximatif) dans le flux

   static final int STROKE=0;
   static final int FILL=1;
   static final int CLIP=2;

   // La font courante
   private Font font = new Font("Helvetica",Font.PLAIN,10);

   // La couleur courante
   private Color color=Color.black;

   /**
    * Ouvre un flux EPS et génère son entête
    * @param out le flux de sortie
    * @param title le titre
    * @param preview une éventuelle image en preview, null si aucune
    * @param xmin,ymin,xmax,ymax la bounding box
    */
   public EPSGraphics(PrintStream out,String title,Image preview,
         int xmin, int ymin, int xmax, int ymax) {
      this.out=out;
      this.title=title;
      this.xmin=xmin; this.ymin=ymin;
      this.xmax=xmax; this.ymax=ymax;
      height = ymax-ymin;
      this.preview=preview;
      head();
   }

   /**
    * Clotûre le flux EPS. Doit obligatoirement être appelé avant de fermer
    * le flux de sortie.
    */
   public void end() {
      tail();
      out.flush();
   }

   private void flushIfRequired(int n) {
      size+=n;
      if( size>10000 ) {
         out.flush();
         size=0;
      }
   }

   private void print(String s) {
      out.print(s);
      flushIfRequired(s.length());
   }

   private void print(char c) {
      out.print(c);
      flushIfRequired(1);
   }

   /** Génère l'entête EPS */
   private void head() {
      String d = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(
            new Date(System.currentTimeMillis()));

      print(
            "%!PS-Adobe-3.0 EPSF-3.0\n" +
                  "%%BoundingBox: "+xmin+" "+ymin+" "+xmax+" "+ymax+"\n" +
                  "%%Creator: Aladin [CDS]\n" +
                  "%%Title: "+title+"\n" +
                  "%%CreationDate: "+d+"\n" +
                  "%%LanguageLevel: 2\n" +
                  //            "%%DocumentData: Clean7Bit\n" +
                  //            "%%Origin: 0 0\n" +
                  //            "%%Pages: 1\n" +
                  //            "%%Page: 1 1\n" +
                  "%%EndComments\n"
            );
      if( preview!=null ) preview(preview);
      print(
            "0.5 setlinewidth\n" +
                  "0 setlinejoin\n" +
                  "/Helvetica findfont 10 scalefont setfont\n" +
                  "/l { newpath moveto lineto stroke } def\n" +
                  "/t { newpath moveto show } def\n"
            );
      clipRect(xmin,ymin,xmax-xmin+1,ymax-ymin+1);
   }

   /** Insère un preview au format EPSI (hexadecimal indépendant de la plate-forme) */
   private void preview(Image img) {
      int width = img.getWidth(null);
      int height = img.getHeight(null);
      int depth = 8;
      int lines = height*( (width/36)+(width%36!=0?1:0) );
      print("%%BeginPreview: "+width+" "+height+" "+depth+" "+lines+"\n");

      try {
         PixelGrabber pg = new PixelGrabber(img,0,0,width,height,true);
         pg.grabPixels();
         int []pixel = (int[])pg.getPixels();
         // Lecture dans l'ordre inverse
         for( int i=height-1; i>=0; i--) writePixelsPreviewHexStr(pixel,i*width,width);


      } catch( Exception e ) { e.printStackTrace(); }

      print("%%EndPreview\n");
   }

   /** Insère la fin du format EPS (cadre autour de l'image EPS) */
   private void tail() {
      print(
            "0 0 0 setrgbcolor\n" +
                  "newpath\n" +
                  xmin+" "+ymin+" moveto\n" +
                  xmin+" "+ymax+" lineto\n" +
                  xmax+" "+ymax+" lineto\n" +
                  xmax+" "+ymin+" lineto\n" +
                  "closepath\n" +
                  "1 setlinewidth\n" +
                  "stroke\n" +
                  "showpage\n" +
                  "%%EOF\n"
            );
   }

   public void clearRect(int x, int y, int width, int height) {
      setColor(Color.white);
      fillRect(x,y,width,height);
      setColor(color);
   }

   public void clipRect(int x, int y, int width, int height) {
      mode=CLIP; drawRect(x,y,width,height); mode=STROKE;
   }

   public void copyArea(int x, int y, int width, int height, int dx, int dy) {
      // TODO Auto-generated method stub

   }

   public Graphics create() {
      // TODO Auto-generated method stub
      return null;
   }

   public void dispose() {
      // TODO Auto-generated method stub

   }

   /** Dessin d'un Arc "à la Java" */
   public void drawArc(int x, int y, int width, int height,
         int startAngle,int arcAngle) {
      drawArc1(x+width/2.,y+height/2.,width,height,startAngle,arcAngle,0);
   }

   /** Dessin d'une ellipse
    * @param xc,yc centre
    * @param semiMA grand axe
    * @param semiMI petit axe
    * @param rot angle de rotation (deg, sens positif)
    */
   public void drawEllipse(double xc, double yc, double semiMA, double semiMI, double rot) {
      drawArc1(xc,yc,semiMA*2,semiMI*2,0,360,rot);
   }

   /** Dessin d'un Arc d'ellipse
    * @param xc,yc Centre de l'ellipse
    * @param width largeur de la boite englobante (avant rotation)
    * @param height hauteur de la boite englobante (avant rotation)
    * @param startAngle angle de départ
    * @param arcAngle longueur de l'arc
    * @param rot rotation de l'ellipse
    */
   private void drawArc1(double xc, double yc, double width, double height,
         double startAngle,double arcAngle,double rot) {
      double r = width/2.;
      double prop = height/width;
      startAngle+=180;
      rot=180-rot;
      yc=this.height-yc;
      print("gsave\n" + xc+" "+yc+" translate\n");
      if( rot!=0 ) print(rot+" rotate\n");
      if( !Double.isNaN(prop) && prop!=1 ) print("1 "+prop+" scale\n");
      print("newpath\n" +
            "0 0 "+r+" "+startAngle+" "+(arcAngle+startAngle)+" arc " +
            (mode==FILL?"fill":"stroke")+"\n" +
            "grestore\n");
   }

   /** Dessine l'image indiquée à l'échelle de la bounding box.
    * L'image peut être éventuellement plus large que la bounding box, elle sera croppée
    * à la taille
    * Les lignes seront affichées en sens inverse suivant le format postscript
    * L'image est codée en EPS RGB
    */
   public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
      try {
         int width = img.getWidth(observer);
         int height = img.getHeight(observer);
         int w = (xmax-xmin+1);
         int h = (ymax-ymin+1);
         if( width>w ) width=w;
         if( height>h ) height=h;

         PixelGrabber pg = new PixelGrabber(img,0,0,width,height,true);
         pg.grabPixels();
         int []pixel = (int[])pg.getPixels();
         print(
               "gsave\n" +
                     width+" "+height+" scale\n" +
                     x+" "+y+" translate\n" +
                     width+" "+height+" 8 ["+width+" 0 0 "+height+" 0 0]\n" +
                     //                     width+" "+height+" 8 ["+width+" 0 0 "+(-height)+" 0 "+height+"]\n" +
                     "{ currentfile "+(3*width)+" string readhexstring pop } bind\n" +
                     "false 3 colorimage\n"
               );

         // Lecture dans l'ordre inverse
         for( int i=height-1; i>=0; i--) writePixelsHexStr(pixel,i*width,width);

         print("grestore\n");
      } catch( Exception e ) {
         e.printStackTrace();
         return false;
      }
      return true;
   }

   public boolean drawImage(Image img, int x, int y, Color bgcolor,
         ImageObserver observer) {
      // TODO Auto-generated method stub
      return false;
   }

   public boolean drawImage(Image img, int x, int y, int width, int height,
         ImageObserver observer) {
      // TODO Auto-generated method stub
      return false;
   }

   public boolean drawImage(Image img, int x, int y, int width, int height,
         Color bgcolor, ImageObserver observer) {
      // TODO Auto-generated method stub
      return false;
   }

   public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
         int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
      // TODO Auto-generated method stub
      return false;
   }

   public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
         int sx1, int sy1, int sx2, int sy2, Color bgcolor,
         ImageObserver observer) {
      // TODO Auto-generated method stub
      return false;
   }

   private String HEX = "0123456789abcdef";

   /** Génération de la ligne Hexadécimale correspondant à la ligne des pixels
    * se situant à la position offset de l'image pix.
    * La ligne héxadécimale est "repliée" tous les 72 caractères et un retour
    * est inséré à la fin si nécessaire.
    * @param pix le tableau des pixels RGB issues de l'image java
    * @param offset la position de la ligne à générée
    * @param width le nombre de pixels dans une ligne
    */
   private void writePixelsHexStr(int []pix, int offset, int width) {
      for( int i=0; i<width; i++ ) {
         if( i>0 && i%24==0) out.println();
         int r = (pix[offset+i])>>16 & 0xFF;
         int g = (pix[offset+i])>>8 & 0xFF;
         int b = (pix[offset+i]) & 0xFF;
         print( HEX.charAt(r/16)); print( HEX.charAt(r%16));
         print( HEX.charAt(g/16)); print( HEX.charAt(g%16));
         print( HEX.charAt(b/16)); print( HEX.charAt(b%16));
      }
      if( width%72!=0 ) out.println();
   }

   /** Génération de la ligne Hexadécimale correspondant à la ligne des pixels
    * se situant à la position offset de l'image pix POUR UN PREVIEW AU FORMAT EPSI
    * EN NIVEAU DE GRIS
    * La ligne héxadécimale est "repliée" tous les 72 caractères et un retour
    * est inséré à la fin si nécessaire.
    * Chaque début de ligne est précédé du caractère '%'
    * @param pix le tableau des pixels RGB issues de l'image java
    * @param offset la position de la ligne à générée
    * @param width le nombre de pixels dans une ligne
    */
   private void writePixelsPreviewHexStr(int []pix, int offset, int width) {
      print('%');
      for( int i=0; i<width; i++ ) {
         if( i>0 && i%36==0 && i!=(width-1)) print("\n%");
         int r = (pix[offset+i])>>16 & 0xFF;
         int g = (pix[offset+i])>>8 & 0xFF;
         int b = (pix[offset+i]) & 0xFF;
         r=(r+g+b)/3;
         print( HEX.charAt(r/16)); print( HEX.charAt(r%16));
      }
      out.println();
   }


   public void drawLine(int x1, int y1, int x2, int y2) {
      print(x1+" "+(height-y1-1)+" "+x2+" "+(height-y2-1)+" l\n");
   }

   public void drawOval(int x, int y, int width, int height) {
      drawArc(x,y,width,height,0,360);
   }

   public void drawPolygon(Polygon p) { drawPolygon(p.xpoints,p.ypoints,p.npoints); }
   public void fillPolygon(Polygon p) { fillPolygon(p.xpoints,p.ypoints,p.npoints); }

   public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) { drawPolygon(xPoints,yPoints,nPoints,false); }
   public void drawPolygon( int[] xPoints, int[] yPoints, int nPoints) { drawPolygon(xPoints,yPoints,nPoints,true); }
   private void drawPolygon(int[] xPoints, int[] yPoints, int nPoints,boolean close) {
      print("newpath\n");
      for( int i=0; i<nPoints; i++ ) {
         if( i==0 ) print(xPoints[i]+" "+(height-yPoints[i]-1)+" moveto\n");
         else print(xPoints[i]+" "+(height-yPoints[i]-1)+" lineto\n");
      }
      if( nPoints>0 && close ) print(xPoints[0]+" "+(height-yPoints[0]-1)+" lineto\n");
      print("closepath\n" +
            (mode==STROKE?"stroke":mode==CLIP?"clip":"fill")+"\n");
   }

   public void drawRoundRect(int x, int y, int width, int height, int arcWidth,
         int arcHeight) {
      // TODO Auto-generated method stub
      // POUR LE MOMENT
      drawRect(x,y,width,height);

   }

   public void drawString(String str, int x, int y) {
      // Substitution de ( ) en \( et \) si nécessaire
      if( str.indexOf('(')>=0 || str.indexOf(')')>=0 ) {
         char a[] = str.toCharArray();
         StringBuffer res = new StringBuffer();
         for( int i=0; i<a.length; i++ ) {
            char c = a[i];
            if( c=='(' || c==')' ) res.append('\\');
            res.append(c);
         }
         str = res.toString();
      }
      print("("+str+") "+x+" "+(height-y-1)+"  t\n");

   }

   public void drawString(AttributedCharacterIterator iterator, int x, int y) {
      // TODO Auto-generated method stub
   }

   public void fillArc(int x, int y, int width, int height, int startAngle,
         int arcAngle) {
      mode=FILL; drawArc(x,y,width,height,startAngle,arcAngle); mode=STROKE;

   }

   public void fillOval(int x, int y, int width, int height) {
      mode=FILL; drawOval(x,y,width,height); mode=STROKE;
   }

   public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
      mode=FILL; drawPolygon(xPoints, yPoints, nPoints); mode=STROKE;
   }

   public void drawRect(int x, int y, int width, int height) {
      y = this.height-y-1;
      int xmax= x+width-1;
      int ymax = y-height+1;
      print(
            "newpath\n" +
                  x+" "+y+" moveto\n" +
                  x+" "+ymax+" lineto\n" +
                  xmax+" "+ymax+" lineto\n" +
                  xmax+" "+y+" lineto\n" +
                  "closepath\n" +
                  (mode==STROKE?"stroke":mode==CLIP?"clip":"fill")+"\n"
            );

   }

   public void fillRect(int x, int y, int width, int height) {
      mode=FILL; drawRect(x,y,width,height); mode=STROKE;
   }

   public void fillRoundRect(int x, int y, int width, int height, int arcWidth,
         int arcHeight) {
      // TODO Auto-generated method stub

   }

   public Shape getClip() {
      // TODO Auto-generated method stub
      return null;
   }

   public Rectangle getClipBounds() {
      // TODO Auto-generated method stub
      return null;
   }

   public Color getColor() {
      return color;
   }

   public Font getFont() {
      return font;
   }

   public FontMetrics getFontMetrics(Font f) {
      return Toolkit.getDefaultToolkit().getFontMetrics(f);
   }

   public void setClip(Shape clip) {
      // TODO Auto-generated method stub

   }

   public void setClip(int x, int y, int width, int height) {
      // TODO Auto-generated method stub

   }

   public void setColor(Color c) {
      if( c==null || c==color ) return;
      double r = c.getRed()/256.;
      double g = c.getGreen()/256.;
      double b = c.getBlue()/256.;
      print(r+" "+g+" "+b+" setrgbcolor\n");
      color=c;
   }

   public void setFont(Font font) {
      this.font=font;
      String italic = "Italic";  // Mot clé pour l'italique (peut aussi s'appeler Oblique)

      String f = font.getName();
      if( f.equals("Serif") ) f="Times-Roman";
      else if( f.equals("Monospaced") ) f="Courrier";
      else if( f.equals("SansSerif") ) f="Helvetica";

      if( f.indexOf("Courier")>=0 || f.indexOf("Helvetica")>=0 ) italic="Oblique";

      int style = font.getStyle();
      if( (style&Font.BOLD) == Font.BOLD ) f=f+"-"+"Bold";
      if( (style&Font.ITALIC) == Font.ITALIC ) f=f+"-"+italic;

      print("/"+f+" findfont "+font.getSize()+" scalefont setfont\n");

   }

   public void setPaintMode() {
      // TODO Auto-generated method stub

   }

   public void setXORMode(Color c1) {
      // TODO Auto-generated method stub

   }

   public void translate(int x, int y) {
      print(x+" "+y+" translate\n");
   }

}

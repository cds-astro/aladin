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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import javax.swing.JComponent;

import cds.tools.Util;

/**
 * Outil d'affichage d'une colormap et des valeurs qu'elle représente
 * @author Pierre Fernique [CDS]
 * @version 1 (création Avril 2011)
 */
public class Rainbow  extends JComponent implements Widget  {
   // Poignées pour étirer le rectangle
   static final int IN=0,HG=1,HD=2,BD=3,BG=4,H=5,D=6,B=7,G=8,START=9;

   // Curseur en fonction de la position de la souris
   static final int [] CURSOR = { Cursor.HAND_CURSOR, Cursor.NW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR,
      Cursor.SE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR,
      Cursor.E_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR,Cursor.SE_RESIZE_CURSOR };

   // Taille de la poignée d'étirement
   static final int W=16;

   static final int CLOSE=0;

   protected Aladin aladin;
   protected RectangleD r;            // La taille du rectangle dans les coordonnées de l'image
   private boolean visible=false;     // true si l'outil de cadrage est visible

   protected double lastPos;          // Dernière position de la souris (entre [0..1]- 0 pour min, 1 pour max)
   protected boolean isIn=false;      // true si on a la souris dessus
   protected boolean isSelected=false;// true si on a sélectionné l'objet
   protected int lastButton=-1;        // Close sous la souris (simple survol par la souris)
   private int lastPoignee=-1;      // Poignée active suite à un simple survol par la souris
   private int dragPoignee=-1;      // Poignée active du clic & drag en cours
   private double dragX=-1,dragY=-1;   // Coordonnées de référence d'un clic & drag

   private RectangleD closeButton = null;

   protected ColorModel cm=null;
   protected double min=0,max=255;
   protected String title=null;

   public Rainbow(Aladin aladin) {
      this.aladin=aladin;
      visible=true;
   }

   public Rainbow(Aladin aladin,ColorModel cm,double min, double max) {
      this(aladin);
      r = new RectangleD(10,40,30,300);
      setCM(cm);
      setMinMax(min,max);
   }

   public void setTitle(String title) {
      this.title=title;
   }

   public void setCM(ColorModel cm) {
      this.cm=cm;
   }

   public void setMinMax(double min,double max) {
      this.min=min;
      this.max=max;
   }

   private double getZoom() {
      double n = aladin.view.getModeView();
      return n==ViewControl.MVIEW16 ? 4 : n==ViewControl.MVIEW9 ? 3 :n==ViewControl.MVIEW4 ? 2 : 1;
   }

   /** Réutilisation "en l'état" */
   public void reset() { setVisible(true); }

   /** Affiche ou cache */
   public void setVisible(boolean flag) { visible=flag; }

   /** True si l'outil de recadrage est visible */
   public boolean isVisible() { return visible; }

   /** True si on est en train de faire un clic & drag */
   public boolean isDragging() { return visible && dragPoignee!=-1; }

   public boolean isUsed() { return isIn || isSelected; }

   /** Reçoit un évènement de la vue suite à un survol de la souris */
   public boolean mouseMove(double xview, double yview) {
      if( !visible ) return false;
      double z = getZoom(); xview *= z; yview *= z;
      isIn = r.x-4<=xview && xview<=r.x+r.width+4 && r.y-4<=yview && yview<=r.y+r.height+4;
      lastButton = getButton(xview,yview);
      lastPoignee = getPoignee(xview,yview);
      lastPos = r.height>r.width ? (r.y+r.height - yview)/r.height : (xview - r.x)/r.width;
      return isIn || isSelected;
   }

   /** Evènement débutant un clic & drag */
   public boolean startDrag(double xview,double yview) {
      if( !visible ) return false;
      double z = getZoom(); xview *= z; yview *= z;
      PointD p = new PointD(xview,yview);
      dragPoignee = getPoignee(p.x,p.y);
      if( dragPoignee==-1 ) { dragX=dragY=-1; return false; }
      dragX=p.x;
      dragY=p.y;
      return true;
   }

   public boolean isInside(double xview,double yview) {
      return mouseMove(xview,yview);
   }

   public boolean isSelected() { return isSelected; }

   /** Evènement lors d'un clic & drag => extension du rectangle */
   public boolean mouseDrag(ViewSimple vs, double xview, double yview,boolean shift) {
      if( !visible || !isIn ) return false;
      double z = getZoom(); xview *= z; yview *= z;
      PointD p = new PointD(xview,yview);
      double x = p.x;
      double y = p.y;
      double dx = x-dragX;
      double dy = y-dragY;
      if( shift && (dragPoignee==HG || dragPoignee==HD || dragPoignee==BG || dragPoignee==BD) ) {
         if( r.width<r.height ) r.width=r.height;
         else r.height=r.width;
         if( Math.abs(dx)<Math.abs(dy) ) dx=dy;
         else dy=dx;
      }
      switch(dragPoignee) {
         case IN: r.x+=dx; r.y+=dy; break;
         case HG: r.x+=dx; r.y+=dy; r.width-=dx; r.height-=dy; break;
         case HD: r.y+=dy; r.width+=dx; r.height-=dy; break;
         case BD: case START: r.width+=dx; r.height+=dy; break;
         case BG: r.x+=dx; r.width-=dx; r.height+=dy; break;
         case H:  r.y+=dy; r.height-=dy; break;
         case D:  r.width+=dx; break;
         case B:  r.height+=dy; break;
         case G:  r.x+=dx; r.width-=dx; break;
         default: return false;
      }

      // On ne peut donner une taille inf à 1
      if( r.width*vs.zoom<1 ) r.width=1/vs.zoom;
      if( r.height*vs.zoom<1 ) r.height=1/vs.zoom;

      dragX=x;
      dragY=y;
      return true;
   }

   /** Traitement d'un clic sur un bouton. Retourne true i on a cliqué dans un boutons
    * sinon false */
   public boolean submit(ViewSimple v) {

      if( lastButton==CLOSE ) {
         if( this==v.rainbow ) v.rainbow=null;
         if( this==v.rainbowF ) v.rainbowF=null;   // BEURK
         return true;
      }

      boolean oIsSelected=isSelected;
      isSelected=isIn;
      if( oIsSelected!=isSelected ) return !isSelected;

      return false;
   }

   /** Fin d'un clic & drag */
   public boolean  endDrag() {
      if( dragPoignee==-1 ) return false;
      dragPoignee=-1;
      dragX=dragY=-1;
      return true;
   }

   /** Retourne le numéro du label sous la souris (coordonnées de la vue) */
   protected int getButton(double xview,double yview) {
      double z = getZoom(); xview /= z; yview /= z;
      if( closeButton!=null && closeButton.contains(xview,yview) ) return CLOSE;
      return -1;
   }

   // Retourne la poignée sous la souris (coordonnées images)
   private int getPoignee(double x, double y) {
      if( !visible ) return -1;
      if( r.width==1 || r.height==1 ) return BD;
      for( int i=1; i<=8; i++ ) {
         RectangleD rc = getRectPoignee(i);
         if( rc.contains(x,y) ) return i;
      }
      return r.contains(x,y) ? IN : -1;
   }

   // Retourne le rectangle correspondant à la poignée
   private RectangleD getRectPoignee(int poignee) {
      if( !visible ) return null;
      double w = W;
      double w2 = w/2;
      switch(poignee) {
         case HG:  return new RectangleD(r.x-w2,r.y-w2, w,w);
         case HD:  return new RectangleD(r.x+r.width-w2,r.y-w2, w,w);
         case BD:  return new RectangleD(r.x+r.width-w2,r.y+r.height-w2, w,w);
         case BG:  return new RectangleD(r.x-w2,r.y+r.height-w2, w,w);

         case H:   return new RectangleD(r.x+w,r.y-w2, r.width-2*w,w);
         case D:   return new RectangleD(r.x+r.width-w2,r.y+w, w,r.height-2*w);
         case G:   return new RectangleD(r.x-w2,r.y+w, w,r.height-2*w);
         case B:   return new RectangleD(r.x+w,r.y+r.height-w2, r.width-2*w,w);
      }
      return null;
   }


   private int DS=5;

   private void drawSelect(Graphics g) {
      double z=getZoom();
      for( int i=0; i<8; i++ ) {
         int x = (int)((i==0 || i>=6 ? r.x : i==1 || i ==5 ? r.x+r.width/2 : r.x+r.width)/z)-DS/2;
         int y = (int)( (i<=2 ? r.y : i==3 || i==7 ? r.y+r.height/2 : r.y+r.height)/z)-DS/2;
         drawPoignee(g,x,y);
      }
   }

   protected void drawPoignee(Graphics g, int xc,int yc) {
      g.setColor( Color.green );
      g.fillRect( xc+1,yc+1 , DS,DS );
      g.setColor( Color.black );
      g.drawRect( xc,yc , DS,DS );
   }

   private void drawClose(Graphics g) {
      double z = getZoom();
      int x=(int)((r.x+r.width-2)/z);
      int y=(int)((r.y+2)/z);
      int w=5;
      g.setColor(Aladin.COLOR_BUTTON_BACKGROUND);
      g.fillRect(x-w-4,y+1,w+4,w+4);
      g.setColor(Color.red);
      g.drawLine(x-w-3,y+2,x-3,y+w+2);
      g.drawLine(x-w-3,y+3,x-3,y+w+3);
      g.drawLine(x-w-3,y+w+2,x-3,y+2);
      g.drawLine(x-w-3,y+w+3,x-3,y+3);

      closeButton = new RectangleD(x-w-4,y+1,w+4,w+4);
   }

   private void drawAxe(Graphics g,double min, double max, boolean lineSens,boolean haut,boolean gauche) {

      double z = getZoom();
      double z1;
      if( z>=3 ) {
         g.setFont(Aladin.SBOLD);
         z1=1.8;
      } else {
         g.setFont(Aladin.BOLD);
         z1=1;
      }


      boolean vertical = !lineSens;
      double sizeLabel = (vertical ? 40:80)/z1;
      double width = vertical ? r.height/z : r.width/z;
      FontMetrics fm = g.getFontMetrics();
      int nbLabel = (int)Math.max(width/sizeLabel,1);

      double pos = 0;
      int h = fm.getAscent();
      int h2 = fm.getHeight();
      int w = z>2?2:5;
      double lastValue = getLastValue(min,max);
      float trans=0.5f;

      // Détermination de la première valeur de l'échelle et de son incrément
      double range = max-min;
      double incr = Math.pow(10,(int)(0.5+Math.log10(range)));
      //      double x1=incr;
      double val1 = (Math.floor(min/incr)*incr);
      incr = range/(incr/10)<=nbLabel*1.5 ? incr/10 : range/(incr/4)<nbLabel*1.5 ? incr/4 : incr;
      incr = range/incr>=nbLabel/2 ? incr : incr/2;
      //      System.out.println("nbLabel="+nbLabel+" val1="+val1+" incr=("+x1+"=>"+incr+") range="+(max-min));

      boolean encore=true;
      boolean last=false;

      // On fera un tour de plus pour afficher la valeur sous la souris (lorsqu'on a atteind max)
      for( int i=0; encore ; i++, val1+=incr ) {
         double val = i>30 ? max : val1;

         if( last ) {
            encore=false;
            if( Double.isNaN(lastValue) || lastValue<min || lastValue>max ) break;
            val=lastValue;
            g.setColor(Color.blue);
            trans=0.8f;
         }
         else if( val<min ) val=min;
         else if( val>=max ) { val=max; last=true; }

         double valpos = (val-min)/(max-min);
         pos = width * valpos;

         // S'il y a une légende trop proche de la valeur min ou max, ou ne l'affiche pas
         if( val!=lastValue ) {
            if( val!=min &&  (valpos*width)<h2 ) continue;
            if( val!=max &&  (1-valpos)*width<h2 ) continue;
         }

         // Les tirets
         int x = (int)( (!vertical ? r.x : gauche ? r.x+r.width : r.x-w)/z );
         int y = (int)( (vertical ? r.y+r.height : haut ? r.y+r.height : r.y-w)/z );
         if( vertical ) y=(int)(y-pos);
         else x=(int)(x+pos);
         if( vertical ) g.drawLine(x,y,x+w,y);
         else g.drawLine(x,y,x,y+w);

         // Les labels
         String label = Util.myRound(val);
         int size = fm.stringWidth(label);
         x = ( !vertical ? x - size/2 : gauche ? x+w+2 : x-(size+2));
         y = ( vertical ? y+h/2 : haut ? y+h+w+2 : y-2 );
         Util.drawCartouche(g, x, y-h+2, size, h,trans, null, Color.white);
         g.drawString(label,x,y);
      }
   }

   private double getLastValue(double min,double max) {
      if( lastPos==-1 || !isIn || isSelected) return Double.NaN;
      double val = min+ lastPos*(max-min);
      return val;
   }

   protected void drawTitle(Graphics g) {
      if( title==null ) return;
      g.setFont(g.getFont().deriveFont(Font.ITALIC));
      double z = getZoom();
      FontMetrics fm = g.getFontMetrics();
      int size =fm.stringWidth(title);
      int d = fm.getDescent();
      int h = fm.getHeight();
      int x = (int)( r.x/z );
      int y = (int)( r.y/z-d*2 );
      Util.drawCartouche(g, x, y-h+2, size, h, 0.5f, null, Color.white);
      g.drawString(title,x,y);
   }

   protected void drawOverlays(Graphics g) {
      if( isSelected ) {
         drawSelect(g);
         drawClose(g);
      } else {
         closeButton=null;
      }
   }

   private BufferedImage img = null;   // Double buffer pour la table des couleurs
   private Graphics g = null;          // Contexte graphique de img

   public void draw(Graphics gr,ViewSimple v,int dx, int dy) {
      if( !visible ) return;

      gr.translate(dx,dy);

      try {
         // Conversion dans les coordonnées de la vue
         double z = getZoom();
         boolean gauche = (r.x/z)>v.getWidth()/2;
         boolean haut =   (r.y/z)>v.getHeight()/2;
         boolean vertical = r.width > r.height;
         if(  vertical && ((r.y/z)<30 || (v.getHeight()-(r.y+r.height)/z)<30) ) haut = !haut;
         if( !vertical && ((r.x/z)<60 || (v.getWidth() -(r.x+r.width )/z)<60) ) gauche=!gauche;

         ColorModel cm = this.cm!=null ? this.cm : ((PlanImage)v.pref).getCM();

         // (Re)génération du buffer image si nécessaire
         int w = (int)Math.ceil(r.width/z)+1;
         int h = (int)Math.ceil(r.height/z)+1;
         //         if( img==null || img.getWidth()!=w || img.getHeight()!=h ) {
         //            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
         //            g = img.getGraphics();
         //         }

         // Tracé de la table des couleurs dans le buffer image
         int y = 0;
         int x = 0;
         double currentPos = 0;
         double gap = (vertical ? r.width/z : r.height/z)/256.;
         double larg = vertical ? r.height/z : r.width/z;

         // Le fond
         int j = vertical ? 255 : 0;
         gr.setColor( new Color(cm.getRed(j), cm.getGreen(j), cm.getBlue(j)) );
         //         g.fillRect(0,0,(int)(r.width/z),(int)(r.height/z));
         gr.fillRect((int)(r.x/z),(int)(r.y/z),(int)(r.width/z),(int)(r.height/z));

         // La colormap
         double incr=currentPos;
         for( int i=0; i<256; i++, currentPos+=gap ) {
            j = vertical ? i : 255-i;
            gr.setColor( new Color(cm.getRed(j), cm.getGreen(j), cm.getBlue(j)) );
            if( vertical ) {
               gr.fillRect((int)(currentPos+r.x/z), (int)(y+r.y/z), (int)gap+2, (int)larg);
               gr.drawRect((int)(currentPos+r.x/z), (int)(y+r.y/z), (int)gap+2, (int)larg);
            } else {
               gr.fillRect((int)(x+r.x/z), (int)(currentPos+r.y/z), (int)larg, (int)gap);
               gr.drawRect((int)(x+r.x/z), (int)(currentPos+r.y/z), (int)larg, (int)gap);
            }

            //            for( ; incr<currentPos; incr++ ) {
            //               //               if( vertical ) g.drawLine(incr,y,incr,y+larg);
            //               //               else g.drawLine(x,incr,x+larg,incr);
            //               if( vertical ) gr.drawLine((int)(incr+r.x/z),(int)(y+r.y/z),(int)(incr+r.x/z),(int)(y+larg+r.y/z));
            //               else gr.drawLine((int)(x+r.x/z),(int)(incr+r.y/z),(int)(x+larg+r.x/z),(int)(incr+r.y/z));
            //
            //            }
         }

         // Le bord
         gr.setColor(Color.black);
         //         gr.drawRect(0,0,(int)(r.width/z),(int)(r.height/z));
         gr.drawRect((int)(r.x/z),(int)(r.y/z),(int)(r.width/z),(int)(r.height/z));

         // Tracé effective de la table des couleurs
         //         gr.drawImage(img,(int)(r.x/z),(int)(r.y/z),aladin);

         // L'axe
         gr.setColor(Color.black);
         double min = this.cm!=null ? this.min : ((PlanImage)v.pref).getPixelMin();
         double max = this.cm!=null ? this.max : ((PlanImage)v.pref).getPixelMax();
         drawAxe(gr,min,max,vertical,haut,gauche);

         drawTitle(gr);

         // Les boutons et sélecteurs
         drawOverlays(gr);

         // Mise en forme du curseur
         if( isSelected ) {
            int cursor = Cursor.DEFAULT_CURSOR;
            if( lastButton!=-1 ) cursor=Cursor.HAND_CURSOR;
            else {
               int fleche = dragPoignee!=-1 ? dragPoignee : lastPoignee;
               if( fleche!=-1 ) cursor = CURSOR[fleche];
            }
            if( cursor!=oCursor ) { oCursor=cursor; v.setCursor(new Cursor(cursor)); }
         }
      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

      gr.translate(-dx, -dy);
   }

   private int oCursor=-1;


   private WidgetControl voc=null;

   @Override
   public WidgetControl getWidgetControl() { return voc; }

   @Override
   public void createWidgetControl(int x, int y, int width, int height, float opacity,JComponent parent) {
      voc = new WidgetControl(this,x,y,width,height,opacity,parent);
      voc.setResizable(true);
   }

   @Override
   public void paintCollapsed(Graphics g) {}


}

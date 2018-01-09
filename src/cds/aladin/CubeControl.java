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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import cds.tools.Util;

public class CubeControl {

   static protected int NOTHING = 0;
   static protected int PAUSE   = 1;
   static protected int PLAY    = 2;
   static protected int REWIND  = 3;
   static protected int FORWARD = 4;
   static protected int PLUS    = 5;
   static protected int MOINS   = 6;
   static protected int SLIDE   = 7;
   static protected int EDIT    = 8;
   static protected int SHOULD_REPAINT = 9;
   static protected int IN      = 10;
   static protected int CURSOR  = 11;

   static final int MAX_TRANSPARENCY = 11;

   static protected String HELP[] ={
      "Nothing",
      "Pause",
      "Play",
      "Previous image",
      "Next image",
      "Increase the speed",
      "Decrease the speed",
      "Change the current frame",
      "Edit the frame number",
      "",
      "",
      "",
   };

   private StringBuilder sedit=null;

   protected int mode=PLAY;
   protected int delay;         // Délai en ms entre deux Frames
   protected int nbFrame;       // Nombre de frames
   protected int lastFrame=-1;   // Dernière Frame affichée
   protected double transparency=-1; // Niveau de transparence [0..1], -1 si non appliqué

   protected long startTime;      // Date de démarrage afin de calculer la bonne frame
   private int mouseMove=NOTHING;


   // Dernière position et taille où l'on a dessiné le blinkControl
   private int X=-1;
   private int Y=-1;
   protected int SIZE=-1;

   private int rewX[] = new int[3];
   private int rewY[] = new int[3];

   private int playX1[] = new int[4];
   private int playY1[] = new int[4];
   private int playX2[] = new int[3];
   private int playY2[] = new int[3];

   private int pauseX1[] = new int[4];
   private int pauseY1[] = new int[4];
   private int pauseX2[] = new int[4];
   private int pauseY2[] = new int[4];

   private int fowX[] = new int[3];
   private int fowY[] = new int[3];

   private int plusX1[] = new int[4];
   private int plusY1[] = new int[4];
   private int plusX2[] = new int[4];
   private int plusY2[] = new int[4];
   private int slashX[] = new int[4];
   private int slashY[] = new int[4];
   private int moinsX[] = new int[4];
   private int moinsY[] = new int[4];

   private int edit[] = new int[2];
   private int posX[] = new int[5];
   private int posY[] = new int[5];
   private int sliderX[] = new int[4];
   private int sliderY[] = new int[4];
   private int labelX,labelY;
   private int labelPX,labelPY;

   private int shapeX[][]   = { pauseX1,pauseX2,playX1,playX2,rewX,fowX,plusX1,plusX2,slashX,moinsX,sliderX,posX };
   private int shapeY[][]   = { pauseY1,pauseY2,playY1,playY2,rewY,fowY,plusY1,plusY2,slashY,moinsY,sliderY,posY };

   private ViewSimple v;
   private Aladin aladin;
   private Plan p;

   protected CubeControl(ViewSimple v,Plan p,int d,boolean pause) {
      this.v=v;
      this.aladin=v.aladin;
      this.p=p;
      delay = d;
      startTime=System.currentTimeMillis();
      if( delay==0 || pause ) setMode(PAUSE);
      if( delay < 20 ) delay=FrameBlink.getDefaultDelay();
   }

   /** Copie du BlinkControl */
   protected CubeControl copy() {
      CubeControl b = new CubeControl(v,p,delay,mode==PAUSE);
      b.startTime = startTime;
      b.nbFrame = nbFrame;
      b.lastFrame = lastFrame;
      b.mode = mode;
      return b;
   }

   /**
    * Création du controleur de séquence
    * @param size taille d'un élément (ex: le triangle de PLAY)
    */
   protected void init(int size) {
      int dx=0;
      SIZE=size;

      pauseX1[0]=3+dx; pauseX1[1]=3+dx; pauseX1[2]=4+dx; pauseX1[3]=4+dx;
      pauseY1[0]=0; pauseY1[1]=size; pauseY1[2]=size; pauseY1[3]=0;
      pauseX2[0]=6+dx; pauseX2[1]=6+dx; pauseX2[2]=7+dx; pauseX2[3]=7+dx;
      pauseY2[0]=0; pauseY2[1]=size; pauseY2[2]=size; pauseY2[3]=0;

      dx = size+size/2;
      playX1[0]=dx; playX1[1]=dx; playX1[2]=1+dx; playX1[3]=1+dx;
      playY1[0]=0; playY1[1]=size; playY1[2]=size; playY1[3]=0;
      playX2[0]=3+dx; playX2[1]=3+dx; playX2[2]=size+dx;
      playY2[0]=0; playY2[1]=size; playY2[2]=size/2;

      dx += 2*size;
      rewX[0]=2+dx; rewX[1]=size+dx; rewX[2]=size+dx;
      rewY[0]=size/2; rewY[1]=0; rewY[2]=size;

      dx += size+size/2;
      fowX[0]=dx; fowX[1]=dx; fowX[2]=size+dx-2;
      fowY[0]=0; fowY[1]=size; fowY[2]=size/2;

      dx += 2*size;
      plusX1[0]=dx+size/2-1; plusX1[1]=dx+size/2; plusX1[2]=dx+size/2; plusX1[3]=dx+size/2-1;
      plusY1[0]=1; plusY1[1]=1; plusY1[2]=size; plusY1[3]=size;
      plusX2[0]=dx; plusX2[1]=dx+size-1; plusX2[2]=dx+size-1; plusX2[3]=dx;
      plusY2[0]=size/2; plusY2[1]=size/2; plusY2[2]=size/2+1; plusY2[3]=size/2+1;

      dx += size;
      slashX[0]=dx; slashX[1]=dx+1; slashX[2]=dx+4; slashX[3]=dx+3;
      slashY[0]=size; slashY[1]=size; slashY[2]=0; slashY[3]=0;

      dx += size/2;
      moinsX[0]=dx+1; moinsX[1]=dx+size-1; moinsX[2]=dx+size-1; moinsX[3]=dx+1;
      moinsY[0]=size/2; moinsY[1]=size/2; moinsY[2]=size/2+1; moinsY[3]=size/2+1;

      dx += size+size/2+2;
      labelX=dx; labelY=size+ size/2 -2;

      dx+= 3*SIZE;
      labelPX=dx; labelPY=size+ size/2 -2;

      int dy = 2*size+2;
      posX[0]=posX[4]=-size/2; posX[1]=posX[2]=size/2; posX[3]=0;
      posY[0]=posY[1]=dy-4; posY[2]=posY[4]=dy+size-3; posY[3]=dy+size-1;
      sliderX[0]=sliderX[3]=0; sliderX[1]=sliderX[2]=getWidth();
      sliderY[0]=sliderY[1]=dy; sliderY[2]=sliderY[3]=dy;

   }

   protected int getWidth() { return SIZE*12+SIZE/2+ 4*SIZE; }
   protected int getHeight() { return SIZE*3; }

   /**
    * Retourne le code du logo sous la souris
    * Rq : si y==-1 retourne toujours SLIDE (voir ViewSimple.mouseDrag)
    * @param x,y position souris dans la View.
    */
   private int getLogo(int x,int y) {
      int m;

      if( y==-1 || y>Y+SIZE && y<Y+getHeight() && x>=X-2 && x<X-2+getWidth()+5 ) m=SLIDE;
      else if( y>=0 && (y<Y || y>Y+SIZE || x<X || x>X+getWidth()+5) ) m=NOTHING;
      else if( x<X+SIZE ) m=PAUSE;
      else if( x>X+SIZE+SIZE/2 && x<X+2*SIZE+SIZE/2 ) m=PLAY;
      else if( x>X+3*SIZE+SIZE/2 && x<X+4*SIZE+SIZE/2 ) m=REWIND;
      else if( x>X+4*SIZE+SIZE/2 && x<X+5*SIZE+SIZE/2 ) m=FORWARD;
      else if( x>X+7*SIZE && x<X+8*SIZE ) m=PLUS;
      else if( x>X+8*SIZE+SIZE/2 && x<X+9*SIZE+SIZE/2 ) m=MOINS;
      else if( x>X+9*SIZE+SIZE/2 && x<X+getWidth()+5 ) m = EDIT;
      else m=IN;
      return m;
   }

   protected boolean isEditing() { return sedit!=null; }
   protected void stopEditing() { sedit=null; }

   /** Traitement d'un évènement caractère */
   public boolean keyPress( KeyEvent e) {
      if( !isEditing() ) return false;

      int key = e.getKeyCode();
      char k = e.getKeyChar();

      if( key==KeyEvent.VK_ESCAPE ) {
         stopEditing();
         return true;
      }

      if( key==KeyEvent.VK_ENTER ) {
         try {
            int frame = Integer.parseInt(sedit.toString())-1;
            setFrameLevel(frame);
         } catch( NumberFormatException e1 ) { }
         stopEditing();
         return true;
      }

      // On efface le dernier caractere
      if( key==KeyEvent.VK_BACK_SPACE || key==KeyEvent.VK_DELETE ) {
         int n = sedit.length();
         if( n==0 ) return false;
         sedit.deleteCharAt(n-1);
         return true;
      }

      // On insere un nouveau caractere
      if( k>=31 && k<=255 ) {
         sedit.append(k);
         return true;
      }

      return false;
   }



   protected int mouseMoved(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      int m=getLogo(x,y);
      if( mouseMove!=NOTHING && m==NOTHING ) m =SHOULD_REPAINT;
      mouseMove=m;
      if( m!=NOTHING && m!=SHOULD_REPAINT ) {
         String s = "Blink control: "+HELP[mouseMove]+(aladin.view.isMultiView()?" (with SHIFT for synchronizing)":"");
         aladin.status.setText(s);
      }
      if( m!=NOTHING )
         Aladin.makeCursor(aladin,m==CubeControl.SHOULD_REPAINT?
               Aladin.DEFAULTCURSOR:Aladin.HANDCURSOR);

      Util.toolTip(v,m==IN || m==SHOULD_REPAINT  || m==NOTHING ? "" : HELP[mouseMove]);
      return m;
   }

   protected int mousePressed(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      boolean flagEdit=false;
      int m=getLogo(x,y);
      if( m==PLUS    ) { mode=PLAY; decreaseDelay(); }
      else if( m==MOINS   ) { mode=PLAY; increaseDelay(); }
      else if( m==PAUSE || m==PLAY  ) { setMode(m); }
      else if( m==REWIND  ) { mode=PAUSE; transparency=-1; askStep(-1); }
      else if( m==FORWARD ) { mode=PAUSE; transparency=-1; askStep(1); }
      else if( m==EDIT ) {
         sedit = new StringBuilder( Util.align3(lastFrame+1) );
         setMode(PAUSE);
         flagEdit=true;
      } else if( m==SLIDE ) {
         aladin.view.setCubeFrame(v, getFrameLevel(x),e.isShiftDown());
         aladin.view.repaintAll();
      }

      if( !flagEdit ) stopEditing();
      return m;
   }

   protected void mouseDragged(MouseEvent e) {
      int x = e.getX();
      aladin.view.setCubeFrame(v, getFrameLevel(x), e.isShiftDown());
      aladin.view.repaintAll();
   }

   protected boolean mouseWheelMoved(MouseWheelEvent e) {
      int x = e.getX();
      int y = e.getY();
      int m=getLogo(x,y);
      if( m!=SLIDE ) return false;
      mode=PAUSE;
      transparency=-1;
      askStep( -e.getWheelRotation() );
      aladin.calque.repaintAll();

      return true;
   }

   /** Détermine le frame et le niveau de transparence pointée par la souris
    * en position x (dans la fenêtre de la vue) en prenant en compte la taille
    * et la position du slider */
   protected double getFrameLevel(int x) {
      double dx = x - X;
      if( dx<0 ) return 0;
      if( dx>getWidth() ) return nbFrame-1;

      double frameLevel = (nbFrame-1)*(dx/getWidth());
      if( nbFrame>=MAX_TRANSPARENCY ) frameLevel=(int)frameLevel;
      if( frameLevel>=nbFrame ) frameLevel = nbFrame-1;
      return frameLevel;
   }

   /** Retourne l'indice de la frame courante */
   protected int getCurrentFrameIndex() {

      if( delay==0 || nbFrame<=0 ) return lastFrame;
      int n,m=-1;
      int step = getNextFrameInfo();
      if( step==2 ) {
         long now = System.currentTimeMillis();
         if( p instanceof PlanImageBlink )  n= (int) (((now - startTime) / delay) % nbFrame);
         else {
            // Delai non écoulé
            if( now - startTime < delay ) n=lastFrame;
            else {
               m = ((PlanBGCube)p).getCurrentFrameReady();

               // frame non prête => on attend
               if( m!=-1 && lastFrame+1>m ) n=lastFrame;
               else n=lastFrame+1;
            }
         }

      } else n=lastFrame+step;

      if( n>=nbFrame ) n=0;
      else if( n<0 ) n = nbFrame-1;

      lastFrame=n;

      return n;
   }

   /** Retourne le niveau de transparence de la frame courante avec la suivante
    * (ou la première si on est sur la dernière). Il s'agit d'un nombre entre 0 et 1,
    * 0 l'image n'est pas transparente, 1 l'image est totalement transparente
    * @return
    */
   protected double getTransparency() {
      return transparency;
   }

   /** Synchronize le blinkControl en fonction d'un autre */
   protected void syncBlink(CubeControl b) {
      startTime = b.startTime;
      lastFrame = b.lastFrame;
      delay = b.delay;
      mode = b.mode;
      p.changeImgID();
   }


   /** Positionne le frame courante et le niveau de transparence
    *  et met la pause */
   protected void setFrameLevel(double frameLevel) { setFrameLevel(frameLevel,true); }
   protected void setFrameLevel(double frameLevel,boolean pause) {
      if( frameLevel>=nbFrame || frameLevel<0 ) return;
      int frame = (int)frameLevel;
      transparency = frame==frameLevel || v.pref instanceof PlanBG ? -1
            : frameLevel - frame;

      p.changeImgID();
      long timeRef=System.currentTimeMillis();

      if( p instanceof PlanImageBlink ) {
         startTime = timeRef-frame*delay;
         lastFrame = (int) ((timeRef - startTime) / delay);
         if( nbFrame!=0 ) lastFrame = (int) (((timeRef - startTime) / delay) % nbFrame);
      } else {
         startTime = timeRef;
         lastFrame = (int)frameLevel;
      }
      p.setCubeFrame(frameLevel);
      if( pause ) setMode(PAUSE); // mode=PAUSE;
   }

   /** Positionne la transparence courante [0..1], -1 si inactive */
   protected void setTransparency(double t) {
      transparency = t;
   }

   /** Recale la date de début de la séquence pour qu'elle reprenne bien
    * à l'image courante */
   protected void resume() {
      long now = System.currentTimeMillis();
      if( p instanceof PlanImageBlink ) {
         startTime = now-lastFrame*delay;
      } else {
         startTime=now;
      }
      transparency=-1;  // On annule la transparence si on n'est pas en pause
   }

   /** Double le délai entre 2 frame sans dépasser la limite max */
   protected void increaseDelay() {
      int max = FrameBlink.getMaxDelay();
      if( delay>=max ) return;
      delay*=2;
      if( delay>max ) delay=max;
      aladin.status.setText("New blink delay: "+delay+"ms");
   }

   /** Divise par 2 le délai entre 2 frame sans dépasser la limite min */
   protected void decreaseDelay() {
      int min = FrameBlink.getMinDelay();
      if( delay<=min ) return;
      delay/=2;
      if( delay<min ) delay=min;
      aladin.status.setText("New blink delay: "+delay+"ms");
   }


   /** Force le passage en PAUSE ou en PLAY */
   protected void setMode(int m) {
      if( !p.isCube() ) return;
      if( m==PLAY ) { p.setPause(false,v); mode=PLAY; resume(); askStep(2); aladin.view.startTimer(); }
      else if( m==PAUSE ) { p.setPause(true,v); mode=PAUSE; askStep(0); }

   }
   
   /** Increment ou décrément du frame courant */
   public void incFrame(int s) { askStep(s); }

   private int step=0;
   synchronized private void askStep(int s)  { step=s; }

   // Retourne l'indicateur de prochaine Frame
   //  0 - on ne bouge pas
   //  1 - on demande la prochaine Frame
   // -1 - on demande la Frame précédente
   //  2 - on roule
   protected int getNextFrameInfo() {
      int n=2;      // On suppose qu'on est en PLAY
      if( mode==PAUSE ) { n=step; askStep(0); }
      return n;
   }

   protected void draw(Graphics g, int x,int y,int size,int frame,int nbFrame) {
      // Mémorisation de la position où l'on trace de blinkControl
      X=x; Y=y;

      // Création ou adaptations des logos si nécessaires
      if( size!=SIZE ) init(size);
      this.nbFrame=nbFrame;  // mémorisation pour éventuel setFrame()

      double frameLevel = frame;
      if( transparency!=-1 && transparency!=0 ) frameLevel += transparency;

      int pos = nbFrame>1 ? (int) (getWidth()*(frameLevel)/(nbFrame-1) ) : 0;
      pos-=2;
      posX[0]=posX[4]=pos; posX[1]=posX[2]=pos+size/2; posX[3]=pos+size/4;

      // Tracage du blinkControl en fonction du mode courant PLAY/PAUSE
      Polygon p;

      g.setColor(mouseMove==EDIT ? Aladin.COLOR_GREEN : Color.red);
      g.setFont(Aladin.SPLAIN);
      g.drawString(Util.align3(frame+1),edit[0]=x+labelX,edit[1]=y+labelY);

      g.setColor(Color.red);
      if( transparency!=-1 ) {
         g.drawString(Util.align2((int)((1-transparency)*100))+"%",x+labelPX,y+labelPY);
      } else {
         g.drawString("/  "+Util.align3(nbFrame),x+labelPX,y+labelPY);
      }

      // Tracé des petits tirets correspondant à chaque image
      if( nbFrame<MAX_TRANSPARENCY ) {
         for( int i=0; i<nbFrame; i++ ) {
            int shift = (int)(0.5+ i*getWidth()/(nbFrame-1.));
            g.drawLine(sliderX[0]+x+shift,sliderY[0]+y-2,
                  sliderX[0]+x+shift,sliderY[2]+y+3);
         }
      }

      // Traçage de chaque logo (Rewind, Play ou Pause, Forward
      for( int i=0; i<shapeX.length; i++ ) {
         //         if( nbFrame<=2 && (shapeX[i]==posX || shapeX[i]==sliderX) ) continue;

         p=Tool.setPolygon(shapeX[i],shapeY[i],x,y);

         // Détermination de la couleur de chaque logo en fonction
         // de la dernière position de la souris
         if( (mouseMove==PAUSE   && (i==0 || i==1) && mode==PLAY  )
               || (mouseMove==PLAY    && (i==2 || i==3) && mode==PAUSE )
               || (mouseMove==REWIND  &&  i==4)
               || (mouseMove==FORWARD &&  i==5)
               || (mouseMove==PLUS    && (i==6 || i==7))
               || (mouseMove==MOINS   &&  i==9)
               || (mouseMove==SLIDE   &&  i==11) ) g.setColor(Aladin.COLOR_GREEN);
         else if( ((i==0 || i==1) && mode==PAUSE  )
               || ((i==2 || i==3) && mode==PLAY ) ) g.setColor(Color.blue);
         else g.setColor(Color.red);

         if( i!=CURSOR || transparency==-1 ) g.fillPolygon(p);
         g.drawPolygon(p);
      }

      if( isEditing() ) drawEdit(g);
   }

   private void drawEdit(Graphics g) {
      String s = sedit.toString();
      Font ft = g.getFont();
      g.setFont(ft.deriveFont(Font.BOLD));
      int w=g.getFontMetrics().stringWidth(s);
      int h=g.getFontMetrics().getHeight();
      int x = edit[0];
      int y = edit[1]-h+2;
      Util.drawCartouche(g, x, y, w, h, 1f,  Color.black, Color.white);
      g.setColor(Color.black);
      g.drawString(s,x,y+h/2+5);
      g.setFont(ft);
      x = x+w-1;
      boolean blink=(System.currentTimeMillis()/500)%2==0;
      if( blink ) {
         g.setColor(Color.black);
         g.drawLine(x, y+2, x, y+h-2);
         g.drawLine(x+1, y+2, x+1, y+h-2);
      }
   }
}

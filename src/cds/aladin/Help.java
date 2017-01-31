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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.swing.JComponent;

import cds.tools.Util;

/**
 * Gestion du HELP
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (6 juin 2003) Prise en compte des imagettes
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Help extends JComponent implements
            MouseMotionListener, MouseListener
            {

   // Les composantes de l'objet
   String text;		                // Le texte courant
   Font font=FI;
   FontMetrics fm=null;	                // La font courante
   int ws=520,hs=520;	                // Taille du Help par defaut
   boolean flagFold=true;

   // Les references
   Aladin aladin;

   // Les variables de travail
   static Font FI = Aladin.LPLAIN;  // Font par defaut
   static Font FG = Aladin.LBOLD;	// Font grasse
   static Font FTITRE = FG;
   static Font TEST;
   static int dy=-1;			// Espace entre deux lignes
   
   static private Color BGD;
   String DEFAUT,VIEW;

  /** Creation de l'objet de manipulation du Help
   * @param aladin reference
   */
   protected Help(Aladin aladin) {
      this.aladin = aladin;
      
      FI = font = new Font("Trebuchet MS"/*"Segoe UI"*/,Font.PLAIN,Aladin.LSIZE);
      FG=FTITRE = new Font("Trebuchet MS",Font.BOLD,Aladin.LSIZE+2);
      
      BGD = Aladin.COLOR_BACKGROUND;
      
      setBackground( BGD );
      addMouseMotionListener(this);
      addMouseListener(this);
    }
   
//   private Dimension MINDIM = new Dimension(ws,hs);
//   public Dimension getPreferredSize() { return MINDIM; }

  /** Le texte du Help pour expliquer le HELP.
   * @return le help du help
   */
   protected String H() {
      if( VIEW==null ) VIEW = aladin.chaine.getString("View.HELP");
      return(VIEW);
   }

  /** Modifie le texte du Help.
   * Demande egalement le repaint()
   * @param text le nouveau texte
   */
   protected void setText(String text) {
      flagFold=true;
      if( text!=null && text.length()>1 ) {
         if( text.charAt(0)=='-' ) { font= Aladin.PLAIN; text=text.substring(1); }
         else if( text.charAt(0)=='|' ) { flagFold=false; font=Aladin.COURIER; text=text.substring(1); }
       } else font=FI;
      fm=null;
      this.text = text;
      repaint();
    }

   /** Modifie le texte du Help.
    * Demande egalement le repaint()
    * @param text le nouveau texte
    */
    protected void setHyperText(String link, String text) {
       resetLink();
       if( link!=null ) stack(link);
       setText(text);
     }
    
    private void goBack() {
       stack.pop();
       aladin.command.execHelpCmd((String)stack.pop());
    }

    Stack stack = new Stack();
    private void stack(String link) { stack.push(link); }

    protected void resetStack() {
       while( !stack.empty() ) stack.pop();   // stack.clear() si > JVM 1.1
    }
    private int hasStack() { return stack.size()-1; }

    protected void setDefault() {
       if( DEFAUT==null ) DEFAUT = aladin.chaine.getString("Help.HELP");
       setText( DEFAUT );
    }
    
    private boolean center=false;
    protected void setCenter(boolean center) { this.center=center; }

    // Juste pour manger l'evenement
    public void mouseMoved(MouseEvent e) {
       if( getLink(e.getX(),e.getY())!=null ) Aladin.makeCursor(this,Aladin.HANDCURSOR);
       else Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
    }

    public void mouseEntered(MouseEvent e) {
//       aladin.localisation.setMode(MyBox.AFFICHAGE);
//       aladin.pixel.setMode(MyBox.AFFICHAGE);
       aladin.status.setText("");
       if( aladin.inHelp ) {
          setText(H());
          repaint();
       }
    }

    public void mouseReleased(MouseEvent e) {
       if( aladin.msgOn ) aladin.endMsg();
       String wordLink = getLink(e.getX(),e.getY());
       if( wordLink!=null ) {
          if( wordLink.equals("Home") ) { resetStack(); wordLink="";}         
          if( wordLink.equals("Back") ) goBack();
          else aladin.command.execHelpCmd(wordLink);
       }
       else aladin.helpOff();
    }

  /** Affichage d'une ligne de texte dans la fenetre du help.
   * La fonction se charge des retours a la ligne en verifiant qu'elle
   * coupe correctement entre deux mots.<BR>
   * Si la chaine commence par '*', la ligne va etre affichee en gras
   * et centree. Si elle commence par '!' il s'agit d'un titre. Si
   * elle commence par '%', il s'agit d'un image (nom de l'image doit suivre),
   * si elle commence par '*%' il s'agit d'une image qui doit etre centree.
   * si un mot commence par @ il s'agit d'un hyperlien
   *
   * @param g le contexte graphique
   * @param s la ligne a afficher
   * @param x,y la position du debut
   * @return l'ordonnee de la prochaine ligne
   */
    int getTextHeight(Graphics g,String s,int x,int y) { return drawString1(g,s,x,y,false); }
    int drawString(Graphics g,String s,int x, int y) { return drawString1(g,s,x,y,true); }
    private int drawString1(Graphics g,String s,int x, int y,boolean draw) {
      StringTokenizer st;
      boolean flag_center=false;
      ImageObserver imo = aladin.isFullScreen() 
            ? (ImageObserver)aladin.fullScreen.viewSimple : 
              (ImageObserver)aladin;

      // Pas encore de contexte
      if( fm==null ) return y;
      else dy = fm.getHeight()+2;
//      else dy = Aladin.GETHEIGHT+1;	// Cochonnerie de JAVA
      
      boolean ligneVide=s.trim().length()==0;
      
      if( s.charAt(0)=='%' || (flag_center=(s.indexOf("*%")==0)) ) {
         String imgFile = s.substring(flag_center?2:1);
         Image i = aladin.getImagette(imgFile);
         if( i==null ) return y;
         if( flag_center ) x=ws/2-i.getWidth(imo)/2;
         if( draw ) g.drawImage(i,x,y,imo);
         return y+=i.getHeight(imo)+dy;
      }

      // Pour un titre
      if( s.charAt(0)=='!' ) {
          g.setFont(FTITRE);
          x=ws/2-fm.stringWidth(s)/2;
          y+=dy/2;
          if( draw ) g.drawString(s.substring(1),x,y);
          y+=1.5*dy;
          g.setFont(font);
          return y;
      }
      
      // Pour une chaine centrée
      if( s.charAt(0)=='*' || center) {
         if( s.charAt(0)=='*' ) s=s.substring(1);
         x=ws/2-fm.stringWidth(s)/2;
         if( draw ) g.drawString(s,x,y);
         y+=dy;
         return y;
      }

      boolean flagLink=false;
//      if( s.charAt(0)=='*' ) { flag_center=true; s=s.substring(1); }

      st=new StringTokenizer(s," \t,|(.",true);

      // Recherche du dernier blanc avant le retour a la ligne
      while( st.hasMoreTokens() ) {
         String mot=st.nextToken();
         int style=Font.PLAIN;
         int n=mot.length();
                  
         // S'agit-il d'un hyper-lien
          if( n>1 && mot.charAt(0)=='@') {
            flagLink=true;
            mot=mot.substring(1);
            n--;
         } else flagLink=false;
          
          // Un mot en italique
          if( n>2 && mot.charAt(0)=='#' && mot.charAt(n-1)=='#' ) {
             style = Font.ITALIC;
             mot=mot.substring(1,n-1);
             n-=2;
          }
          
          // Un mot en gras
          if( n>2 && mot.charAt(0)=='_' && mot.charAt(n-1)=='_' ) {
             style |= Font.BOLD;
             mot=mot.substring(1,n-1);
             n-=2;
          }
        
         // Le mot commence par @ et il y a inhibition par \ qui précéde
         if( mot.length()>=2 && mot.startsWith("\\@") ) mot = mot.substring(1);
          
         int w = fm.stringWidth(mot);
         if( x+w>ws ) {
            x=10;
            y+=dy;
            if( mot.equals(" ") ) continue;
         }
         if( draw ) x=drawWord(g,mot,x,y,flagLink,style);
      }
      return y+ (ligneVide ? dy-8 : dy);
   }
   
   static final private int MAXLINK = 200;
   private String link[] = new String[MAXLINK];
   private Rectangle xyLink[] = new Rectangle[MAXLINK];
   private int nbLink = 0;
   
   private void resetLink() { nbLink=0; }
   private boolean addLink(String word,int x,int y,int w,int h) {
      if( nbLink==MAXLINK ) return false ;
      link[nbLink]=word;
      xyLink[nbLink++]=new Rectangle(x,y,w,h);
      return true;
   }
   private String getLink(int x,int y) {
      for( int i=0; i<nbLink; i++ ) {
         if( xyLink[i].contains(x,y) ) return link[i];
      }
      return null;
   }
   
   
   private int ostyle=Font.PLAIN;
   
   /** Affichage d'un mot et mémorisation s'il s'agit d'un lien */
   private int drawWord(Graphics g,String word,int x,int y,boolean flagLink,int style) {
      Color c=null;
      if( style!=ostyle ) {
         ostyle=style;
         g.setFont(g.getFont().deriveFont(style));
         fm=g.getFontMetrics();
      } 
      int w = fm.stringWidth(word);
      if( flagLink ) flagLink=addLink(word,x,y+2-dy,w,dy);
      if( flagLink ) {
         c = g.getColor();
         g.setColor( Aladin.COLOR_FOREGROUND_ANCHOR );
//         g.drawLine(x,y+2,x+w,y+2);
      }
      g.drawString(word,x,y);
      if( c!=null ) g.setColor(c);
      return x+w;
   }

//   static final int NRANDOM = 300;
//   Random r = new Random();
//   boolean flagR = false;
//   byte [] xR  = new byte[NRANDOM];
//   byte [] yR  = new byte[NRANDOM];
//   byte [] zR  = new byte[NRANDOM];
//   Color BLUE = new Color(225,225,255);
   
   private int owidth=-1;
   private int oheight=-1;

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setFont(font);
      
      // AntiAliasing
      aladin.setAliasing(g,1);

      if( fm==null ) fm=g.getFontMetrics();
      
      if( text==null ) return;

      // Ajustement de taille ?
      ws = getSize().width;
      hs = getSize().height;
      
      // pour que les ancres retombent aux bons endroits
      if( owidth!=ws || oheight!=hs ) {
         resetLink();
         owidth=ws;
         oheight=hs;
      }
      
      // On efface tout
      g.setColor( BGD );
      g.fillRect(2,2,ws-3,hs-3);
      Util.drawEdge(g,ws,hs);
      
      
      // tracé du Banner d'accueil
      boolean flagBanner = center; //aladin.OUTREACH && center;
      if( flagBanner ) {
         try {
            Image img = aladin.getImagette("Background.jpg");
            aladin.waitImage(img);
            int wi = img.getWidth(this);
            int hi = img.getHeight(this);
            boolean vertical = Math.abs(1-(double)hi/hs) < Math.abs(1-(double)wi/ws);
            double sx2,sy2;
            if( vertical ) { sy2 = hi; sx2 = ws * ((double)hi/hs); }
            else { sx2 = wi; sy2 = hs * ((double)wi/ws); }
            g.drawImage(img,1,1,ws-2,hs-2, 0,0, (int)sx2,(int)sy2, this);
         } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      }

      // On ecrit les lignes du texte courant
      StringTokenizer st = new StringTokenizer(text,"\n");
      int x=10;
      int y=0;
      
      boolean beta = Aladin.BETA;
      boolean proto = Aladin.PROTO;
      
      beta=proto=false;
      
      if( center ) {
         while( st.hasMoreElements() ) {
            String s = (String) st.nextElement();

            if( s.startsWith(Aladin.BETAPREFIX) ) {
               if( !beta ) continue;
               s=s.substring(Aladin.BETAPREFIX.length());
            }

            if( s.startsWith(Aladin.PROTOPREFIX) ) {
               if( !proto ) continue;
               s=s.substring(Aladin.PROTOPREFIX.length());
            }

            y=getTextHeight(g,s,x-2,y-2);
         }
         y=getHeight()/2-y/2;
         if( y<15 ) y=15;
         
      } else y=15;
      
      st = new StringTokenizer(text,"\n");
      x=10;
      g.setColor( new Color(200,200,200) ); //Aladin.GREEN);
      while( st.hasMoreElements() ) {
         String s = (String) st.nextElement();

         if( s.startsWith(Aladin.BETAPREFIX) ) {
            if( !beta ) continue;
            s=s.substring(Aladin.BETAPREFIX.length());
         }

         if( s.startsWith(Aladin.PROTOPREFIX) ) {
            if( !proto ) continue;
            s=s.substring(Aladin.PROTOPREFIX.length());
         }

         y=drawString(g,s,x-2,y-2);
      }
      
      // Dans le cas d'un hyper-text, possibilité de revenir en
      // arrière.
      if( hasStack()>0 ) drawWord(g,"Back",ws-40,15,true,Font.PLAIN);
      if( hasStack()>1 ) drawWord(g,"Home",ws-80,15,true,Font.PLAIN);

   }
   
//   /** Calcul l'indentation d'une chaine */
//   private int indent(String s) {
//      if( !flagFold ) return 0;
//      int x=0;
//      int i;
//      char a[] = s.toCharArray();
//      for( i=0; i<a.length && a[i]==' '; i++) x+=6;
//      return x;
//   }
   
   public void mouseDragged(MouseEvent e) { }
   public void mouseClicked(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }
}

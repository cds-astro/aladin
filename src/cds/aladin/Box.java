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

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.astro.Proj3;
import cds.tools.Util;

/**
* Objet graphique representant une boite
* On utilise Forme avec 2 objets :
* - o[0] : le centre
* - o[1] : le coin HD
* - o[2] : le coin BD
* - o[3] : le coin BG
* - o[4] : le coin HG
* 
* @author Pierre Fernique [CDS]
* @version 1.0 : (déc 11) Création (pour support régions DS9)
*/
public class Box extends Forme {
   
   protected double angle;
      
   /**
    * Création d'une boite en coordonnées cartésiennes
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param v la vue utilisée pour le système de coord cartésienne, (ou null si aucun)
    * @param xv l'absisse du centre
    * @param yv l'ordonnée du centre
    * @param w la largeur
    * @param h la hauteur
    * @param angle l'angle de la boite (dans le sens trigo)
    */
   protected Box(Plan plan, ViewSimple v, double xv, double yv,double w, double h,double angle,String label) {
      super(plan,new Position[5]);
      this.angle=angle;
      double x,y,d;

      o[0] = new Position(plan,v,xv,yv,0,0,XY|RADE_COMPUTE,label);
      Coord c=new Coord(o[0].raj,o[0].dej);
      o[0] = new Tag(plan,null,0,0,label);
      o[0].raj=c.al; o[0].dej=c.del;
 
      double demiDia = Math.sqrt(w*w/4+h*h/4);
      double a = Math.atan2(h/2,w/2);
      double b = Math.toRadians(90+angle);
      d= b+a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[1] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      d= b-a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[2] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      d= b+Math.PI-a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[4] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      d= b+Math.PI+a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[3] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      setObjet(o);
   }
   
   /**
    * Création d'une boite avec des coordonnées sphériques
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param c Coordonnées du centre
    * @param w largeur (en degrés)
    * @param h hauteur (en degrés)
    * @param angle angle (dans le sens trigo)
    */
   protected Box(Plan plan,Coord c,double w, double h,double angle,String label) {
      super(plan,new Position[5]);
      this.angle=angle;
      double b=90;
//      o[0] = new Position(plan,null,0,0,c.al,c.del,RADE,label);
      o[0] = new Tag(plan,null,0,0,label);
      o[0].raj=c.al; o[0].dej=c.del;
      ((Tag)o[0]).setDist(40);
      double demiDia = Math.sqrt(w*w/4+h*h/4);
      double a = Math.toDegrees( Math.atan2(h,w) );
      Coord c1 = applySphereRot(c,demiDia,b+a+angle);
      o[1] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c1 = applySphereRot(c,demiDia,b-a+angle);
      o[2] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c1 = applySphereRot(c,demiDia,b+180+a+angle);
      o[3] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c1 = applySphereRot(c,demiDia,b+180-a+angle);
      o[4] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      setObjet(o);

   }
   
   /** Pour faire plaisir aux objets dérivées (Pickle par exemple) */
   protected Box(Plan plan,Position o[]) { super(plan,o); }   
   
   /** Retourne le type d'objet */
   public String getObjType() { return "box"; }
   
   /** Retourne l'angle mémorisé pour éviter de retourner un éventuel complément */
   protected double getAngle() { return angle; }
     
   /** Test d'appartenance. */
   protected boolean inside(ViewSimple v,double x, double y) { return in(v,x,y); }
   
   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve sur le périmètre
    * @param v la vue concernée
    * @param x,y le point a tester dans le plan de projection de la vue
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean in(ViewSimple v,double x, double y) {  
      if( !isVisible() ) return false;
      for( int i=1; i<o.length; i++ ) {
         if( o[i].in(v,x,y) ) return true;
         if( inLigne(i,v,x,y) ) return true;
      }
      return false;
   }
   
   // Retourne true si on se trouve sur la ligne i (de 1 à 4)
   private boolean inLigne(int i,ViewSimple v,double x,double y) {
      return Ligne.inLigne(o[i-1].xv[v.n],o[i-1].yv[v.n],o[i].xv[v.n],o[i].yv[v.n],x,y,mouseDist(v));
   }
   
   /** Test d'appartenance sur un des coins
    * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
    protected boolean inBout(ViewSimple v, double x, double y) {
       if( !isVisible() ) return false;
       for( int i=1; i<o.length; i++ ) if( o[i].in(v,x,y) ) return true;
       return false;
    }
          
    /** Il suffit qu'un des coins soit dans le rectangle pour retourner vrai */
    protected boolean inRectangle(ViewSimple v,RectangleD r) {
       for( int i=1; i<o.length; i++ ) if( o[i].inRectangle(v,r) ) return true;
       return false;
    }
    
    public Vector getProp() {
       Vector propList = super.getProp();

       final JTextArea textId = new JTextArea( 3,25 );
       JScrollPane paneId = new JScrollPane(textId);
       final PropAction updateId = new PropAction() {
          public int action() { textId.setText( o[0].id ); return PropAction.SUCCESS; }
       };
       final PropAction changeId = new PropAction() {
          public int action() { 
             String s = textId.getText();
             if( s.equals(o[0].id) ) return PropAction.NOTHING;
             o[0].id=textId.getText();
             return PropAction.SUCCESS;
          }
       };
       propList.add(Prop.propFactory("id","Label","Box label",paneId,updateId,changeId));

       return propList;
   }

    
    public String getInfo() { return o[0].id; }
    
    /** Set the information associated to the object (for instance tag label...) */
    public void setInfo(String info) { o[0].setInfo(info); }
    
    /** Dessine l'objet dans le contexte graphique en fonction:
     * @param g : le contexte graphique
     * @param v : la vue concernée
     * @param dx : un éventuel offset en absisse (impression sur papier par exemple)
     * @param dy : un éventuel offset en ordonnée (idem)
     */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      
      Color c =  getColor() ;
      Point op=null;
      for( int i=0; i<o.length; i++ ) {
         int j = i==0 ? 4:i;
         if( o[j].xv==null ) return false;
         Point p = v.getViewCoord(o[j].xv[v.n],o[j].yv[v.n]);
         if( p==null ) return false;
         p.x+=dx; p.y+=dy;
         if( op!=null ) {
            g.setColor(c);
            g.drawLine(op.x, op.y, p.x, p.y);
            if( isSelected() ) drawSelect(g,v,j);
         }
         op=p;
      }
      
      // Label
      if( o[0].id!=null && o[0].id.length()>0 ) o[0].draw(g,v,dx,dy);
      return true;
   }
}

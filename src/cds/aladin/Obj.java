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
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTextField;

import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.aladin.prop.Propable;

/**
 * Interface pour la manipulation d'un objet graphique affichable dans la vue
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : (nov 06) Compatibilité pour Plugins
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public abstract class Obj implements Propable{
   
   // Les différentes formes (pour les Sources uniquement)
   public static final int OVAL     = 0;
   public static final int SQUARE   = 1;
   public static final int CIRCLE   = 2;
   public static final int RHOMB    = 3;
   public static final int CROSS    = 4;
   public static final int TRIANGLE = 5;
   public static final int PLUS     = 6;
   public static final int CIRCLES  = 7;
   public static final int POINT    = 8;
   public static final int DOT      = 9;
   public static final int SOLIDOVAL     = 10;
   public static final int SOLIDSQUARE   = 11;
   public static final int SOLIDCIRCLE   = 12;
   public static final int SOLIDRHOMB    = 13;
   public static final int SOLIDTRIANGLE = 14;

   // Les constantes associees a "methode" lors de la creation
   protected static final int XY = 1;
   protected static final int RADE = 2;
   protected static final int RADE_COMPUTE = 4;
   protected static final int XY_COMPUTE = 8;

   // Les différents flags
   static protected final byte SELECT     = 1;
   static protected final byte TAG        = 1<<1;
   static protected final byte VISIBLE    = 1<<2;
   static protected final byte WITHLABEL  = 1<<3;
   static protected final byte HIGHLIGHT  = 1<<4;
   static protected final byte WITHSTAT   = 1<<5;

   protected Plan   plan;       // Plan d'appartenance de l'objet
   
   /** J2000 RA coordinate */
   public double raj=Double.NaN;

   /** J2000 DEC coordinate */
   public double dej;
   
   public String id;         // Object id
   protected byte flags = VISIBLE;  // Le tableau de flags
   
   public boolean hasProp() { return plan!=null && plan.type!=Plan.APERTURE; }
   public Vector<Prop> getProp() {
      Vector<Prop> propList = new Vector<Prop>();
      JLabel l = new JLabel("\""+getObjType()+"\" object in plane: \""+plan.getLabel()+"\"");
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      l.setFont(l.getFont().deriveFont(14f));
      propList.add( Prop.propFactory("object","",null,l,null,null) );
     
      final Obj myself = this;
      final JTextField pos = new JTextField(20);
      PropAction updatePos = new PropAction() {
         public int action() { pos.setText( plan.aladin.localisation.getLocalisation(myself) ); return PropAction.SUCCESS;}
      };
      PropAction changePos = new PropAction() {
         public int action() { 
            pos.setForeground(Color.black);
            String opos = plan.aladin.localisation.getLocalisation(myself);
            try {
               String npos = pos.getText();
               if( npos.equals(opos) ) return PropAction.NOTHING;
               Coord c1 = new Coord(pos.getText());
               if( (""+c1).indexOf("--")>=0 ) throw new Exception();
               c1 = plan.aladin.localisation.frameToICRS(c1);
               setRaDec(c1.al, c1.del);
               return PropAction.SUCCESS;
           } catch( Exception e1 ) { 
               pos.setForeground(Color.red);
               pos.setText(opos); 
           }
           return PropAction.FAILED;
         }
      };
      propList.add( Prop.propFactory("coord","Coord","Object position",pos,updatePos,changePos) );
      if( id!=null && id.length()>0 ) {
         final JLabel idL = new JLabel(id);
         PropAction updateId = new PropAction() {
            public int action() { 
               idL.setText(id);
               return PropAction.SUCCESS;
            }
         };
         propList.add( Prop.propFactory("id","Info","associated information",idL,updateId,null) );
      }
      return propList;
   }

   /** Positionne le flag select */
   protected void setSelect(boolean select) {
      if( select ) flags |= SELECT;
      else flags &= ~SELECT;
   }
   
   /** Return the Obj color, by default the Plane color */
   public Color getColor() { return plan.c; }
   
   /** Set the Obj color (if possible => depending of the nature of Obj) */
   public void setColor() throws Exception { throw new Exception("Not specifical color property"); }

   /** Retourne true si l'objet contient des informations de photométrie  */
   public boolean hasPhot() { return false; }
   public boolean hasPhot(ViewSimple v) { return false; }

   /** Retourne true si la source a le flag sélect positionné  */
   final public boolean isSelected() { return (flags & SELECT) !=0; }
   
   /** Provide RA J2000 position */
   public double getRa() { return raj; }
   
   /** Provide DEC J2000 position */
   public double getDec() { return dej; }
   
   /** Provide attache info (generally its name) */
   public String getInfo() { return id; }
   
   /** Iterator for multi-component objects such as Line, Polygon, Box ... */
   public Iterator<Obj> iterator() { return null; }
   
   /** Provide script command associated to this object */
   public String getCommand() { return null; }
   
   /** Return true if this object can be used for getting photometrical statistics (first segment of a polygon, or circle) */
   public boolean hasSurface() { return false; }
   
   /** Provide photometric statistics for the area described by the object (only for circle and polygon)
    * @param ad AladinData describing an image with valid pixels
    * @return { cnt,sum,sigma,surface_in_square_deg }
    * @throws Exception
    */
   public double[] getStatistics(AladinData ad) throws Exception {
      return getStatistics(ad.plan);
   }
   
   /** Provide photometric statistics for the area described by the object (only for circle and polygon)
    * @param p Plan describing an image with valid pixels
    * @return { cnt,sum,sigma,surface_in_square_deg }
    * @throws Exception
    */
   protected double[] getStatistics(Plan p) throws Exception { throw new Exception("no statistics available"); }
   
   /** Provide radius (in degrees) for photometry measure tags */
   public double getRadius() { return 0.; }

   /** Tool : Return the distance in degrees between this object and another */
   public double getDistance(Obj obj) {
      Coord ca=new Coord(), cb=new Coord();
      ca.al=raj; ca.del=dej;
      cb.al=obj.raj; cb.del=obj.dej;
      return Coord.getDist(ca,cb);
   }

   /** Return XML meta information associated to this object (GROUP meta definitions)
    * @return XML string, or null
    */
   public String getXMLMetaData() { return null; }

   /** Return the values associated to the object (Source object)
    * @return String array containing each field value
    */
   public String [] getValues() { return null; }

   /** Return the names associated to the columns (Source object)
    * @return String array containing each names
    */
   public String [] getNames() { return null; }

   /** Return the units associated to the columns (Source object)
    * @return String array containing each unit
    */
   public String [] getUnits() { return null; }

   /** Return the datatypes associated to the columns (Source object)
    * @return String array containing each datatype
    */
   public String [] getDataTypes() { return null; }

   /** Return the UCDs associated to the columns (Source object)
    * @return String array containing each unit
    */
   public String [] getUCDs() { return null; }

   /** Return the ArraySizes associated to the columns (Source object)
    * @return String array containing each value
    */
   public String [] getArraysizes() { return null; }
   
   /** Return the Widths associated to the columns (Source object)
    * @return String array containing each value
    */
   public String [] getWidths() { return null; }

   /** Return the Precisions associated to the columns (Source object)
    * @return String array containing each value
    */
   public String [] getPrecisions() { return null; }

   /** Return the number of columns (Source object) */
   public int getSize() { return 0; }

   /** Return the index of a column (Source object). Proceed in 2 steps,
    * Look into the column name, if there is no match, look into the ucd.
    * If nothing match, return -1.
    * The string key can use wilcards (* and ?).
    * @param key name or ucd to find
    * @return index of first column matching the key
    */
   public int indexOf(String key) {return -1; }

   /**
    * Set metadata for a specifical column (name, unit, ucd, display width).
    * null or <0 values are not modified.
    * If the index is greater than the number of columns, the additionnal columns
    * are automatically created
    * @param index number of column (0 is the first one)
    * @param name new name or null for no modification
    * @param unit new unit or null
    * @param ucd new ucd or null
    * @param width new width or -1. 0 to use the default display width.
    */
   public void setColumn(int index, String name,String unit,String ucd,int width) { }

   /** Set a new value for a specifical column index
    * For a new column, creates it before via the selColumn() method
    * @param index column index (0 is the first column)
    * @param value new value for this column
    * @return true ok, false otherwise
    */
   public boolean setValue(int index,String value) { return false; }
   
   /** Set the drawing shape (dedicated for catalog sources)
    * @param sourceType Obj.OVAL, SQUARE, CIRCLE, RHOMB, PLUS, CROSS, TRIANGLE, CIRCLES, POINT, DOT
    */
   public void setShape(int shape) {}

   /** Set specifical color (dedicated for catalog sources) */
   public void setColor(Color c) {}
   
   /** Set the information associated to the object (for instance tag label...) */
   public void setInfo(String info) { id=info; }

   /** Return the object type */
   public String getObjType() { return ""; }

   /** Return the position in J2000 sexagesimal coord */
   public String getSexa() { return Coord.getSexa(raj,dej); }
   
   /** VOTable just for this source (dedicated for catalog sources) */
   public InputStream getVOTable() throws Exception { throw new Exception("Not a source"); }

   /** Select or unselect the object
    * @param flag
    */
   public void setSelected(boolean flag) { plan.aladin.view.setSelected(this,flag); }

   /** Select or unselect the object (dedicated for catalog sources)
    * @param flag
    */
   public void setHighlighted(boolean flag) { }

   /** Change object celestial coordinates */
   public void setRaDec(double ra, double de) { raj=ra; dej=de; }

   /** Change object XY coordinates in function of the current background image */
   public void setXY(double x, double y) { setPosition(plan.aladin.view.getCurrentView(),x,y); }
   
   protected abstract void setPosition(ViewSimple v,double x, double y);
   protected abstract void deltaPosition(ViewSimple v,double x, double y);
   protected abstract void deltaRaDec(double dra, double dde);
   protected abstract void setText(String id);
   protected abstract Point getViewCoord(ViewSimple v,int dw, int dh);
   protected abstract boolean inside(ViewSimple v,double x, double y);
   protected abstract boolean in(ViewSimple v,double x, double y);
   protected abstract boolean inBout(ViewSimple v,double x,double y);
   protected Rectangle getClip(ViewSimple v) { return extendClip(v,null); }
   protected abstract Rectangle extendClip(ViewSimple v,Rectangle clip);
   protected boolean inClip(ViewSimple v,Rectangle clip) {
      if( clip==null ) return true;
      Rectangle r = getClip(v);
      if( r==null ) return true;
      return intersectRect(clip, r.x, r.y, r.width, r.height);
   }
   protected void remove() {};
   protected void projection(ViewSimple v) { };
   protected abstract boolean draw(Graphics g,ViewSimple v,int dx,int dy);
   protected abstract void writeLink(OutputStream o,ViewSimple v) throws Exception;
   protected abstract void drawSelect(Graphics g,ViewSimple v);
   protected abstract void info(Aladin aladin);
   protected abstract void status(Aladin aladin);
   protected String getSpecificAJInfo() { return id; }
   protected void setSpecificAJInfo(String s) { id = s==null || s.length()==0 ? null : s; }
   protected abstract void setVisibleGenerique(boolean flag);
   protected abstract void switchSelect();
   protected abstract Plan getPlan();
   protected abstract boolean cutOn();

   // Carré de la distance entre souris et objet
   protected final double mouseDist(ViewSimple v) {
//      double z = v.getZoom();
//      return z>0 ? 2/z : 1+6/z;
      return 10;
   }
   
   // Pixel Healpix order max (29)
//   public long hpxPos=-1;
//   static final long MAXNSIDE = CDSHealpix.pow2(CDSHealpix.MAXORDER);
//
//   public long getHpxPos() {
//      if( hpxPos==-1 ) {
//         double polar[] = CDSHealpix.radecToPolar(new double[]{raj,dej});
//         try { hpxPos = CDSHealpix.ang2pix_nest(MAXNSIDE, polar[0], polar[1]); } 
//         catch( Exception e ) { }
//      }
//      return hpxPos;
//   }

   // Extension d'un rectangle, création si nécessaire
   static final protected Rectangle unionRect(Rectangle r,Rectangle r1) {
      if( r1==null ) return r;
      return unionRect(r,r1.x,r1.y,r1.width,r1.height);
   }
   static final protected Rectangle unionRect(Rectangle r,int x,int y,int width,int height) {
      if( r==null )  return new Rectangle(x,y,width,height);
      int x1 = Math.max(r.x+r.width ,x+width);
      int y1 = Math.max(r.y+r.height,y+height);
      r.x = Math.min(r.x,x);
      r.y = Math.min(r.y,y);
      r.width  = x1-r.x;
      r.height = y1-r.y;
      return r;
   }

   // Teste l'intersection des deux rectangles
   static final protected boolean intersectRect(Rectangle r,int x,int y, int width, int height) {
      if( r==null ) return false;
      int x1 = x+width-1;
      int y1 = y+height-1;
      int rx1= r.x+r.width-1;
      int ry1 = r.y+r.height-1;
      return !(x<r.x && x1<r.x || x>rx1 && x1>rx1
            || y<r.y && y1<r.y || y>ry1 && y1>ry1);
   }
   
   // Pour du debuging
   public String toString() { return "("+raj+","+dej+") -> "+id; }
   


}

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
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;

import cds.xml.Field;

/**
 * <P>Aladin stack plane access for plugins. </P>
 * This class allows Aladin compatible plugins to access the stack plane data.
 * An Aladin compatible plugin must extend the AladinPlugin class.
 *
 * In a plugin, the current usage is to get an AladinData object from one of
 * these Aladin methods.
 * <OL>
 *    <LI>AladinData ad = aladin.getAladinData(String planeLabel)<BR>
 *      => return the AladinData object corresponding to the plane called planeLabel
 *
 *    <LI>AladinData ad = aladin.getAladinData()<BR>
 *      => return the AladinData object corresponding to the first selected plane in the stack
 *
 *    <LI>AladinData ad = aladin.getAladinImage()<BR>
 *      => return the AladinData object corresponding to current image stack plane (the background image)
 *
 *    <LI>AladinData ad = aladin.createAladinData(String planeLabel)<BR>
 *      => create a new (image) stack plane and return its corresponding AladinData object.
 * </OL>
 * @author Pierre Fernique [CDS]
 * @version 1.2 : january 2009 - isOn(), isReady()
 * @version 1.1 : november 2006 - calibration manipulation adds
 * @version 1.0 : october 2006  - creation
 */
public class AladinData {
   
   /** return the plane internal hashCode (never modified during the Aladin session)
    * @return the unique plane hashCode
    */
   public int getPlaneHashCode() { return plan.hashCode(); }
   
   /** Return the plane label
    * @return the plane label
    */
   public String getLabel() { return plan.label; }

   /** Return the color plane
    */
   public Color getColor() { return plan.c; }

   /** Return the origin of the plane
    * @param the origin of the plane
    */
   public String getOrigin() { return plan.from; }

   /**
    * Return the url from where the plane has been built
    * @return the url of the plane
    */
   public URL getUrl() { return plan.u; }

   /**
    * Return the last error message associated to the plane
    * @return the error message, or null
    */
   public String getError() {
      if( plan.type==Plan.NO ) return "Unknown plane";
      return plan.error;
   }

   /** Return the plane type. The possibilities are the following:
    * Image, Image/RGB, Image/Mosaic, Image/Blink, Image/Cube, Image/Resampled
    * Overlay/Catalog, Overlay/Tool", Overlay/Aperture,
    * Overlay/Filter, Overlay/Image FoV,
    * Folder
    * @return
    */
   public String getPlaneType() {
      if( plan.type==Plan.FOLDER || plan.type==Plan.IMAGE || plan.type==Plan.IMAGEHUGE) return Plan.Tp[plan.type];
      if( plan.type==Plan.NO ) return "";
      return (plan instanceof PlanImage ? "Image/" :  "Overlay/")
             + Plan.Tp[plan.type];
   }

   /** Return true if the plane is selected
    * @return true if the plane is selected
    */
   public boolean isSelected() { return plan.selected; }

   /** Return true if the plane is activated
    * @return true if the plane is activated
    */
   public boolean isOn() { return plan.active; }

   /** Return true if the plane is ready
    * @return true if the plane is ready
    */
   public boolean isReady() { return plan.isReady(); }

   /** Return a copy of the image pixels in doubles
    * This method is dedicated to the image planes
    * @return image pixels ([0][0] at the bottom left, [width-1][height-1] at the top right)
    * @throws AladinException
    */
   public double [][] getPixels() throws AladinException {
      testImage();
      testHuge();
      return ((PlanImage)plan).getPixels();
   }

   /** Return the full pixel value convert in double for a dedicated position.
    * This method is dedicated to the image planes
    * @param x,y Image coordinates (line 0 at the bottom "a la Fits")
    * @return the full pixel value (in double)
    * @throws AladinException
    */
   public double getPixel(int x,int y) throws AladinException {
      testImage();
      testHuge();
      if( !((PlanImage)plan).hasOriginalPixels()
            || !((PlanImage)plan).pixelsOriginFromCache() ) throw new AladinException(ERR004);
      if( x<0 || x>=((PlanImage)plan).naxis1
       || y<0 || y>=((PlanImage)plan).naxis2 ) return Double.NaN;
      return ((PlanImage)plan).getPixelOriginInDouble(x, y);
   }

   /** Return the full pixel value from a cube convert in double for a dedicated position.
    * This method is dedicated to the cube planes
    * @param x,y,z Cube coordinates (line 0 at the bottom "a la Fits")
    * @return the full pixel value (in double)
    * @throws AladinException
    */
   public double getPixel(int x,int y,int z) throws AladinException {
      testImage();
      if( !(plan instanceof PlanImageBlink )) throw new AladinException(ERR011);
      try {
         return ((PlanImageBlink)plan).getPixel(x,y,z);
      } catch( Exception e ) { e.printStackTrace(); throw new AladinException(ERR012+" "); }
   }

   /** Extract a sub-cube. The pixels are expressed in doubles
    * This method is dedicated to the cube or blink planes
    * @return cube pixels ([0][0][0] at the bottom left front,
    *                       [width-1][height-1][depth-1] at the top right rear)
    * @throws AladinException
    */
   public double [][][] getCube(int x,int y,int z,int width, int height, int depth) throws AladinException {
      return getCube(null,x,y,z,width,height,depth);
   }
   public double [][][] getCube(double [][][] cube,int x,int y,int z,int width, int height, int depth) throws AladinException {
      testImage();
      if( !(plan instanceof PlanImageBlink )) throw new AladinException(ERR011);
      try {
         return ((PlanImageBlink)plan).getCube(cube,x,y,z,width,height,depth);
      } catch( Exception e ) { e.printStackTrace(); throw new AladinException(ERR012+" "); }
   }

   /** Return the cube pixels. The pixels are expressed in doubles
    * This method is dedicated to the cube or blink planes
    * @return cube pixels
    * @throws AladinException
    */
   public double [][][] getCube() throws AladinException {
      return getCube(0,0,0,((PlanImage)plan).width,
            ((PlanImage)plan).height,((PlanImageBlink)plan).depth);
   }

   /** Replace the original image pixel values.
    * This method is dedicated to the image planes
    * Aladin will convert each double pixel value in the original image coding (see getFitsBitPix() )
    * @param pixels image pixels (([0][0] at the bottom left, [width-1][height-1] at the top right)
    * @throws AladinException
    */
   public void setPixels(double pixels[][]) throws AladinException {
      testImage();
      ((PlanImage)plan).setPixels(pixels);
      codedPixelsModified();
   }

   /**
    * Replace the original image and possibly modify its internal coding (byte,integer,float..)
    * @param pixels image pixels  (([0][0] at the bottom left, [width-1][height-1] at the top right)
    * @param bitpix coding mode "a la Fits"
    *               (8, 16 or 32 for unsigned integers, -32 or -64 for reals)
    * @throws AladinException
    */
   public void setPixels(double pixels[][],int bitpix) throws AladinException {
      testImage();
      testHuge();
      ((PlanImage)plan).setPixels(pixels,bitpix);
      codedPixelsModified();
   }

   /** Return the image width
    * This method is dedicated to the image planes
    * @return image width
    * @throws AladinException
    */
   public int getWidth() throws AladinException {
      testImage();
      return ((PlanImage)plan).width;
   }

   /** Return the image height
    * This method is dedicated to the image planes
    * @return image heith
    * @throws AladinException
    */
   public int getHeight() throws AladinException {
      testImage();
      return ((PlanImage)plan).height;
   }

   /** Return the cube depth
    * This method is dedicated to the blink or cube planes
    * @return image depth
    * @throws AladinException
    */
   public int getDepth() throws AladinException {
      testImage();
      if( !(plan instanceof PlanImageBlink )) throw new AladinException(ERR011);
      return ((PlanImageBlink)plan).depth;
   }
   
   /** Return the pixel angular size in degrees
    * This method is dedicated to the image planes
    * @return pixel alpha width, pixel delta height
    * @throws AladinException
    */
   public double [] getPixelSize() throws AladinException {
      testImage();
      double [] x = ((PlanImage)plan).getPixelSize();
      if( x==null ) throw new AladinException(ERR006);
      return x;
   }

   /** Return the image coding in FITS standard. Required to manipulate
    * directly the original pixels (see seeCodedPixels())
    * (8, 16 or 32 for unsigned integers, -32 or -64 for reals)
    * This method is dedicated to the image planes
    * @return image coding (a la FITS)
    * @throws AladinException
    */
   public int getFitsBitPix() throws AladinException {
      testImage();
      return ((PlanImage)plan).bitpix;
   }

   /** Return the BZERO value of the original pixels. Required to manipulate
    * directly the original pixels (see seeCodedPixels())
    * PhysicalPixelValue = codedPixelValue*BSCALE + BZERO
    * This method is dedicated to the image planes
    * @return BZERO (a la FITS)
    * @throws AladinException
    */
   public double getFitsBzero() throws AladinException {
      testImage();
      return ((PlanImage)plan).bZero;
   }

   /** Return the BSCALE value of the original pixels. Required to manipulate
    * directly the original pixels (see seeCodedPixels())
    * PhysicalPixelValue = codedPixelValue*BSCALE + BZERO
    * This method is dedicated to the image planes
    * @return BSCALE (a la FITS)
    * @throws AladinException
    */
   public double getFitsBscale() throws AladinException {
      testImage();
      return ((PlanImage)plan).bScale;
   }

   /** Return the Fits header as a String.
    * This method is dedicated to the image planes
    * @return the Fits header or "" if there is not.
    * @throws AladinException
    */
   public String getFitsHeader() throws AladinException {
      testImage();
      try {
         return plan.aladin.save.generateFitsHeaderString((PlanImage)plan);
//         return ((PlanImage)plan).headerFits.getHeader();
      } catch( Exception e ) { throw new AladinException(ERR005); }
   }

   /** Replace original Fits header, and modify the astrometrical projection
    * according to the WCS keys in this new Fits header.
    * The format is a simple string:  FITSKEY = Value\n.....
    * This method is dedicated to the image planes
    * @param header the fits header
    * @throws AladinException
    */
   public void setFitsHeader(String header) throws AladinException {
      testImage();
      try {
         ((PlanImage)plan).headerFits = new FrameHeaderFits(plan,header);
         ((PlanImage)plan).headerFits = new FrameHeaderFits(plan,plan.aladin.save.generateFitsHeaderString((PlanImage)plan));
      } catch( Exception e ) { throw new AladinException(ERR010+" => "+e.getMessage()); }
      setCalib();
   }

   // **************** ADVANCED IMAGE METHODS ******************************************** //

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Provide the BYTE pixel buffer internal reference of the image plane.
    * This method is dedicated for very fast image access in unsigned byte representation.
    * See bytePixelsModified() method for notifying Aladin.
    * WARNING : THE LINE 0 IS THE FIRST ONE (a la java) and THE BYTES ARE UNSIGNED
    * USe getPixels() for slower but simpler pixel access.
    * @return the byte pixel buffer reference
    * @throws AladinException
    */
   public byte [] seeBytePixels() throws AladinException {
      testImage();
      return ((PlanImage)plan).getBufPixels8();
   }

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Provide the BYTE pixel buffer internal reference for a specified cube frame
    * This method is dedicated for very fast cube access in unsigned byte representation.
    * See bytePixelsModified() method for notifying Aladin.
    * WARNING : THE LINE 0 IS THE FIRST ONE (a la java) and THE BYTES ARE UNSIGNED
    * USe getPixels() for slower but simpler pixel access.
    * @return the byte pixel buffer reference
    * @throws AladinException
    */
   public byte [] seeBytePixels(int z) throws AladinException {
      testImage();
      if( !(plan instanceof PlanImageBlink )) throw new AladinException(ERR011);
      return ((PlanImageBlink)plan).vFrames.elementAt(z).pixels;
   }

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Provide the CODED pixel buffer internal reference of the image plane.
    * This method is dedicated for very fast image access original representation
    * See getFitsBitPix() and CodedPixelsToDouble() for reading these pixels.
    * See codedPixelsModified() method for notifying Aladin.
    * WARNING : THE LINE 0 IS THE LAST ONE (a la FITS)
    * Use getPixels() for slower but simpler pixel access.
    * @return the coded pixel buffer reference
    * @throws AladinException
    */
   public byte [] seeCodedPixels() throws AladinException {
      testImage();
      testHuge();
      if( !((PlanImage)plan).hasOriginalPixels()
            || !((PlanImage)plan).pixelsOriginFromCache() ) throw new AladinException(ERR004);
      return ((PlanImage)plan).pixelsOrigin;
   }

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Provide the Aladin Hashtable internal reference of the FITS keys associating
    * to the image plane
    * This method is dedicated to the image planes
    * see fitsKeysModified() method for notifying Aladin.
    * @return FITS header keys
    * @throws AladinException
    */
   public Hashtable seeFitsKeys() throws AladinException {
      testImage();
      fitsKeysModified();
      return ((PlanImage)plan).headerFits.getHeaderFits().getHashHeader();
   }

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Interpret a pixel in a coded pixel array at the position "pos" and coded in "bitpix".
    * WARNING : remember that de PhysicalPixelValue = return * BSCALE + BZERO
    * (see getFitsBzero() and getFitsBscale() methods)
    * @param codedPixels Coded pixel array (see seeCodedPixels())
    * @param bitpix pixel coding (see getFitsBitPix)
    * @param pos pixel position in the array (takes into account the pixel size coding)
    * @return the double pixel representation
    */
   static public double CodedPixelsToDouble(byte [] codedPixels, int bitpix, int pos) {
      return PlanImage.getPixVal1(codedPixels,bitpix,pos);
   }

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Code a pixel double representation in a coded pixel array at the position "pos" and coded
    * in "bitpix".
    * @param val double pixel value
    * @param codedPixels Coded pixel array (see seeCodedPixels())
    * @param bitpix pixel coding (see getFitsBitPix)
    * @param pos pixel position in the array (takes into account the pixel size coding)
    * @return the double pixel representation
    */
  static public void DoubleToCodedPixels(double val,byte [] codedPixels, int bitpix, int pos) {
      PlanImage.setPixVal(codedPixels,bitpix,pos,val);
   }

  /**
   * FOR ADVANCED DEVELOPERS ONLY !
   * Notify to Aladin that the byte pixel array has been modified
   */
  public void bytePixelsModified() throws AladinException {
     testImage();
     testHuge();
     ((PlanImage)plan).noOriginalPixels();
     repaint();
  }

  /**
   * FOR ADVANCED DEVELOPERS ONLY !
   * Notify to Aladin that the coded pixel array has been modified
   */
  public void codedPixelsModified() throws AladinException {
     testImage();
     testHuge();
     ((PlanImage)plan).reUseOriginalPixels();
     repaint();
  }

  /**
   * FOR ADVANCED DEVELOPERS ONLY !
   * Notify to Aladin that the fits key hashtable has been modified
   */
  public void fitsKeysModified() throws AladinException {
     testImage();
     setCalib();
     setFitsHeader(plan.aladin.save.generateFitsHeaderString((PlanImage)plan));
  }

   // *******************  COORDINATES MANIPULATION ******************************** //

   /** Return the J2000 coordinates for a x,y position according
    * to the image astrometrical calibration
    * This method is dedicated to the image planes
    * @param x,y image coordinates (the center of the left-bottom pixel is (1,1) "a la FITS")
    * @return the RA and DEC J2000 coordinates
    * @throws AladinException
    */
   public double[] getCoord(double x, double y) throws AladinException {
      testImage();
      if( !Projection.isOk(plan.projd) ) throw new AladinException(ERR006);
      coo.x = x-0.5; coo.y = y-0.5;
      coo.y = ((PlanImage)plan).naxis2 - coo.y ;
      plan.projd.getCoord(coo);
      return new double[]{coo.al,coo.del};
   }

   /** Return the x,y pixel coordinates for a ra,dec J2000 postion according
    * to the image astrometrical calibration.
    * This method is dedicated to the image planes
    * @param ra,dec RA and DEC J2000 coordinates
    * @return x,y image coordinates (the center of the left-bottom pixel is (1,1) "a la FITS")
    * @throws AladinException
    */
   public double [] getXY(double ra, double dec) throws AladinException {
      if( !Projection.isOk(plan.projd) ) throw new AladinException(ERR006);
      coo.al = ra; coo.del = dec;
      plan.projd.getXY(coo);
      coo.y = ((PlanImage)plan).naxis2 - coo.y;
      return new double[]{coo.x,coo.y};
   }

   // ********************** OVERLAY PLANE METHODS ****************************** //

   /**
    * FOR ADVANCED DEVELOPERS ONLY !
     * This method is dedicated to the overlay planes
     * Provide an iterator on the list of the overlay objects.
     * The "Obj" class allows you to see the content of each overlay object...
     *    String getObjType()    return the type of object (Source, Line, Tag, Circle...)
     *    String [] getFields()  return values (in case of Source)
     *    String [] getColumnNames() return the column name (in case of Source)
     *    double ra,dec          the object position in the sky (J2000)
     * @throws AladinException
     */
    public Iterator<Obj> iteratorObj() throws AladinException {
       testOverlay();
       return plan.pcat.iterator();
    }
    
   /**
    * FOR ADVANCED DEVELOPERS ONLY !
     * This method is dedicated to the overlay planes
     * Provide a direct access to the list of the overlay objects.
     * The "Obj" class allows you to see the content of each overlay object...
     *    String getObjType()    return the type of object (Source, Line, Tag, Circle...)
     *    String [] getFields()  return values (in case of Source)
     *    String [] getColumnNames() return the column name (in case of Source)
     *    double ra,dec          the object position in the sky (J2000)
     * The number of objects is known by getNbObj().
     * @return reference to the object list
     * @throws AladinException
     * @{@link Deprecated}
     */
    public Obj [] seeObj() throws AladinException {
       testOverlay();
       return plan.pcat.getObj();
    }
    
   /** FOR ADVANCED DEVELOPERS ONLY !
    * Return the number of objects (see seeObj()).
    * This method is dedicated to the overlay planes
    * @return the number of objects
    * @throws AladinException
    */
   public int getNbObj() throws AladinException {
      testOverlay();
      return plan.pcat.getCount();
   }
   
   /** FOR ADVANCED DEVELOPERS ONLY !
    * Remove a dedicated Object from the overlay planes
    * @param obj the object to remove
    * @throws AladinException
    */
   public void rmObj(Obj obj) throws AladinException {
      testOverlay();
      if( obj.isSelected() ) obj.setSelected(false);
      plan.pcat.delObjet(obj,true);
   }
   
   /** FOR ADVANCED DEVELOPERS ONLY !
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column name list for a source plane.
    * @see addObj(Obj obj) method for adding new sources
    * @param name column name list
    */ 
   public void setName(String [] name) { leg = Legende.adjustDefaultLegende(leg,Legende.NAME,name); }
   
   /** FOR ADVANCED DEVELOPERS ONLY !
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column data type list for a source plane
    * => A la VOTABLE : int, long, char, float, double , boolean, short, bit, unsignedByte, floatComplex, doubleComplex
    * => A la FORTRAN: J,K,A,E,D,L,I,X,B,C,M
    * @see addObj(Obj obj) method for adding new sources
    * @param name column datatype list
    */ 
   public void setDatatype(String [] datatype) { leg = Legende.adjustDefaultLegende(leg,Legende.DATATYPE,datatype); }
   
   /** FOR ADVANCED DEVELOPERS ONLY !
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column unit list for a source plane.
    * (==> see VOTable standard)
    * @see addObj(Obj obj) method for adding new sources
    * @param name column unit list
    */ 
   public void setUnit(String [] unit) { leg = Legende.adjustDefaultLegende(leg,Legende.UNIT,unit); }
   
   /** FOR ADVANCED DEVELOPERS ONLY !
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column UCD list of column names for a source plane.
    * (==> see UCD standard)
    * @see addObj(Obj obj) method for adding new sources
    * @param name column ucd list
    */ 
   public void setUCD(String [] ucd) { leg = Legende.adjustDefaultLegende(leg,Legende.UCD,ucd); }
   
   /**
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column editing width list for a source plane (requires integers written as Strings).
    * @see addObj(Obj obj) method for adding new sources
    * @param name column width list
    */ 
   public void setWidth(String [] width) { leg = Legende.adjustDefaultLegende(leg,Legende.WIDTH,width); }
   
   /**
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column editing width list for a source plane (requires integers written as Strings).
    * @see addObj(Obj obj) method for adding new sources
    * @param name column arraysize list
    */ 
   public void setArraysize(String [] arraysize) { leg = Legende.adjustDefaultLegende(leg,Legende.ARRAYSIZE,arraysize); }
   
   /**
    * This method is dedicated to the overlay planes and specifically source planes
    * For providing a fast and direct way for creating overlay source plane.
    * Set the column editing width list for a source plane (requires integers written as Strings).
    * @see addObj(Obj obj) method for adding new sources
    * @param name column precision list
    */ 
   public void setPrecision(String [] precision) { leg = Legende.adjustDefaultLegende(leg,Legende.PRECISION,precision); }
   
   /** FOR ADVANCED DEVELOPERS ONLY !
    * This method is dedicated to the overlay planes and specifically source planes
    * Add an object to the source plane.
    * @param id source ID
    * @param ra RA J2000 in degrees
    * @param dec DEC J2000 in degrees
    * @param value value list (according to the current measurement table
    *        description defined with setName(), setDatatype(), setUnit(), setUCD(), setWidth() methods)
    * @return created obj
    * @throws AladinException
    */
   public Obj addSource(String id, double ra, double dec, String [] value) throws AladinException {
      testOverlay();
      StringBuffer s = new StringBuffer("<&_A>");
      for( int i=0; i<value.length; i++ ) {
         if( value[i].startsWith("http://") || value[i].startsWith("https://") ) s.append("\t<&Http "+value[i]+">");
         else s.append("\t"+value[i]);
      }
      Source o = new Source(plan,ra,dec,id,s.toString());
      o.leg = leg;
      plan.pcat.setObjetFast(o);
      return o;
   }
   
   /**
    * FOR ADVANCED DEVELOPERS ONLY !
    * Notify to Aladin that the overlay plane has been modified
    */
   public void objModified() throws AladinException {
      testOverlay();
      if( !Projection.isOk(plan.projd) ) plan.pcat.createDefaultProj();
      plan.planReady(true);
      plan.aladin.view.newView(1);
      plan.aladin.mesure.memoWordLineClear();
      plan.aladin.mesure.display();
      plan.aladin.calque.repaintAll();
   }



   // ********************** PRIVATE SECTION ****************************** //

   // Error messages
   static final protected String ERR000 = "000 No default data plane";
   static final protected String ERR001 = "001 Unknown data plane";
   static final protected String ERR002 = "002 Plane not ready";
   static final protected String ERR003 = "003 Not an image plane";
   static final protected String ERR004 = "004 Full pixels not available";
   static final protected String ERR005 = "005 Fits header not available";
   static final protected String ERR006 = "006 No astrometrical solution";
   static final protected String ERR007 = "007 No object";
   static final protected String ERR008 = "008 No info on this overlay object";
   static final protected String ERR009 = "009 Plane creation error";
   static final protected String ERR010 = "010 Calibration error";
   static final protected String ERR011 = "011 Not an cube or blink plane";
   static final protected String ERR012 = "012 Cube extraction error";
   static final protected String ERR013 = "013 Plugin already running";
   static final protected String ERR014 = "014 Not available for a huge image";
   static final protected String ERR015 = "015 Catalogue creation error";


   // Plane reference
   protected Plan plan;

   // pre-allocation
   private final Coord coo = new Coord();

   // current source plane "legende" (see setName())
   private Legende leg=null;

   /**
    * Aladin plane data for the first selected plane in the stack (from the top)
    * @param aladin Aladin object reference
    * @throws AladinException
    */
//   protected AladinData(Aladin aladin) throws AladinException { this(aladin,null); }
   public AladinData(Aladin aladin) throws AladinException { this(aladin,0,null); }

   /**
    * Aladin plane data for the specified plane
    * @param aladin Aladin object reference
    * @param name plane label as displayed in the Aladin stack
    *                or plane number (The bottom one is 1)
    * @throws AladinException
    */
   public AladinData(Aladin aladin,String name) throws AladinException { this(aladin,0,name); }
//   protected AladinData(Aladin aladin,String planeID) throws AladinException {
//      if( planeID==null ) {
//         try { planeID = aladin.calque.getFirstSelectedPlan().label; }
//         catch( Exception e ) { throw new AladinException(ERR000); }
//      }
//      plan = aladin.calque.getPlan(planeID);
//      if( plan==null ) throw new AladinException(ERR001);
//      
//   }
   
   /**
    * Aladin plane data for the specified plane
    * @param aladin Aladin object reference
    * @param planeHashCode plane hashCode (see getPlaneHashCode())
    * @throws AladinException
    */
   public AladinData(Aladin aladin,int planeHashCode) throws AladinException {
      plan = aladin.calque.getPlanByHashCode(planeHashCode);
      if( plan==null ) throw new AladinException(ERR001);
   }
   
   /**
    * Aladin plane data creation and/or simple access.
    * @param aladin aladin object reference
    * @param mode Plan mode:  0-get a plane, 1-create a new image, 2-create a new catalog
    * @param name plane name for mode 0 (as displayed in the Aladin stack)
    * @throws AladinException
    */
   public AladinData(Aladin aladin,int mode, String name) throws AladinException {
      String planeID;

      // Récupération d'un plan existant
      if( mode==0 ) {

         // Pas d'indication de plan => premier plan sélectionné
         if( name==null ) {
            try {
               planeID = aladin.calque.getFirstSelectedPlan().label;
               plan = aladin.calque.getPlan(planeID);
            }
            catch( Exception e ) { throw new AladinException(ERR000); }
         }

         // Plan pré-existant désigné par son label ?
         else {
            plan = aladin.calque.getPlan(name);         
            if( plan==null ) throw new AladinException(ERR001);
         }
         
         // Récupération de la légende de la dernière source insérée (en cas d'ajout)
         if( plan!=null ) leg = plan.getFirstLegende();

         // Plan à créer
      } else {

         try {
            // Image
            if( mode==1 ) {
               planeID = aladin.calque.newPlanPlugImg(name);
               plan = aladin.calque.getPlan(planeID);
               double pix[][] = new double[500][500];
               setPixels(pix);
               plan.error = PlanImage.NOREDUCTION;
               plan = aladin.calque.getPlan(planeID,1);
               plan.planReady(true);
               
               // catalog
            } else {
               planeID = aladin.calque.newPlanPlugCat(name);
               plan = aladin.calque.getPlan(planeID,1);
            }
         } catch( Exception e ) { throw new AladinException(AladinData.ERR009); }
      }
   }

   protected AladinData(Aladin aladin,String planeId,String origin) throws AladinException {
      int i = aladin.calque.newPlanImage(null,PlanImage.UNKNOWN,planeId,"","",origin,PlanImage.UNDEF,PlanImage.UNDEF,null,null);
      plan = aladin.calque.plan[i];
   }

   // Generate an Exception if the plane is not ready or if it is not an image
   private void testImage() throws AladinException {
      if( !plan.isReady() ) throw new AladinException(ERR002);
      if( !(plan instanceof PlanImage )) throw new AladinException(ERR003);
   }

   // Generate an Exception if the plane is a huge image
   private void testHuge() throws AladinException {
      if( plan.type==Plan.IMAGEHUGE ) throw new AladinException(ERR014);
   }

   // Generate an Exception if the plane is not ready or if it is not an overlay
   private void testOverlay() throws AladinException {
      if( !plan.isReady() ) throw new AladinException(ERR002);
      if( plan.pcat==null ) throw new AladinException(ERR007);
   }

   // Create a new projection associated to the plane
   private void setCalib() {
      try {
         Projection proj = new Projection(Projection.WCS,new Calib(((PlanImage)plan).headerFits.getHeaderFits()));
         plan.setNewProjD(proj);
         plan.setHasSpecificCalib();
         repaint();
      } catch( Exception e ) { }
   }


   // Force Aladin to recompute and redisplay the plane
   public void repaint() {
      if( plan instanceof PlanImage ) ((PlanImage)plan).changeImgID();
      else plan.aladin.view.newView(1);
      plan.aladin.calque.repaintAll();
   }

   // For debugging purpose
   public String toString() {
      try {
         String s = getPlaneType()+" "+getLabel();
         if( !(plan instanceof PlanImage) ) return s;
         return s+" "+getWidth()+"x"+getHeight();
      } catch( Exception e ) {}
      return null;
   }


}


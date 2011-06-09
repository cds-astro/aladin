package cds.tools;
import java.io.*;

/**
 * Interface between two Virtual Observatory applications, such as
 * Aladin (CDS), VOPlot( VO-India), TOPcat, APT (STScI) to listen current
 * J2000 position and pixel value from another VO application.
 * see also VOApp interface and notably addObserver() method.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 14 october 2005
 */
public abstract interface VOObserver {
   
     
   /** The observer method to call on a position VO event
    * in order to transmit the current coordinates J2000 position.
    * Generally called by a java mouse event, it is strongly recommended
    * to implemented this method as short as possible.
    * @param raJ2000 Right ascension in degrees J2000 
    * @param deJ2000 Declination in degrees J2000
    */
   public abstract void position(double raJ2000,double deJ2000);
   
   /** The observer method to call on a pixel VO event
    * in order to transmit the current pixel value
    * Generally called by a java mouse event, it is strongly recommended
    * to implemented this method as short as possible.
    * @param pixValue pixel value under the mouse
    */
   public abstract void pixel(double pixValue);
}
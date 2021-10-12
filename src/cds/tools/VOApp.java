// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.tools;
import java.io.InputStream;

/**
 * Interface between two Virtual Observatory applications, such as
 * Aladin (CDS), VOPlot( VO-India), TOPcat, APT (STScI) to exchange data and
 * have an object selection mechanism.
 * This interface is totally symmetrical, it has to be implemented in both applications
 * 
 * NOTA: This interface is based on a previous version called ExtApp but as some methods
 * have been modified, VOApp do not extend ExtApp but reimplements its methods. ExtApp is deprecated.
 *  
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 17 september 2005
 */
public abstract interface VOApp {
   
   /** To load a VOTable to another application
    * @param in VOTable stream
    * @param dataset label, or null
    */
   public abstract String putVOTable(InputStream in,String label);

    /**
    * Allow an "external" application to send new data via an InputStream
    * in VOTable format. The reference to this "external" application has to
    * passed in order to eventually calls back the "external" application,
    * or to be called again by the "external" application concerning the
    * VOTable objects that it had sent before (see showVOTableObject() and
    * selectVOTableObject() methods below)
    * For this calls or callbacks, the "external" application has to
    * create a new VOTable column giving an unique identifier for each object
    * that it has sent. This column has to be described by the following
    * VOTable FIELD tag : <FIELD name="_OID" type="hidden">. It is strongly
    * recommended to add an unambigus prefix to avoid conflicts with the
    * assignations done by the "external" application and its own assignations.
    * The unicity has to be maintained during all the session. It means that
    * successive VOTables must have difference unique identifiers.
    * @param app "external" application compliante with ExtApp java interface
    * @param in VOTable stream
    * @param dataset label, or null
    * @return an unique ID for this dataset (application dependent - for instance,
    *         the plane name in Aladin)
    */
   public abstract String putVOTable(VOApp app, InputStream in,String label);

   /** To get a dataset in VOTable format (typically for catalogs).
    * @param dataID the dataset identifier (application dependent
    * for instance, the plane name in Aladin)
    * @return a stream containing the VOTable
    */
   public abstract InputStream getVOTable(String dataID);   

   /** To load an image to another application
    * @param in FITS stream
    * @param dataset label, or null
    */
   public abstract String putFITS(InputStream in,String label);   
   
   /** To get a dataset in FITS format (typically for images)
    * @param dataID the dataset identifier (application dependent
    * for instance, the plane name in Aladin)
    * @return a stream containing the FITS
    */
   public abstract InputStream getFITS(String dataID);
   

   /**
    * Call or Callback asking the other application to SHOW objects found
    * in a VOTable previous transmission via loadVOTable() method.
    * The action "SHOW" is a choice of the other application (for example a blink)
    * @param oid list of identifiers found in VOTables (see comment of the
    *            loadVOTable() method.
    */
   public abstract void showVOTableObject(String oid[]);

   /**
    * Call or Callback asking the other  application to SELECT objects found
    * in a VOTable previous transmission via loadVOTable() method.
    * The action "SELECT" is a choice of the other application (for example select
    * objects by showing the corresponding measurements, it can be the same thing
    * that the "SHOW" action - see showVOTableObject() method.)
    * @param oid list of identifiers found in VOTables (see comment of the
    *            loadVOTable() method.
    */
   public abstract void selectVOTableObject(String oid[]);
   
   /**
    * Allow an "external" application to show or hide this application
    */
   public abstract void setVisible(boolean flag);

   /**
    * Allow an "external" application to control by script this application
    * @param cmd script command depending to this application
    * @return error or messages, can be null
    */
   public abstract String execCommand(String cmd);
   
   /** To register an observer of VO events implementing VOObserver interface.
    * see VOObserver.position() and VOObserver.pixel() associated callback methods
    * ex: addObserver(this,VOApp.POSITION|VOApp.PIXEL)
    * @param app the application to register
    * @param eventMasq a bit field (use POSITION or PIXEL),
    *                  (0 to remove the observer)
    */
   public abstract void addObserver(VOObserver app,int eventMasq);

   /** Position event, see addObserver() */
   public final int POSITION = 1;
   
   /** Pixel event, see addObserver() */
   public final int PIXEL = 2;
   
   /** Measure event, see addObserver() */
   public final int MEASURE = 4;
   
   /** Stack event, see addObserver() */
   public final int STACKEVENT = 8;
   
   /** Mouse event, see addObserver() */
   public final int MOUSEEVENT = 16;
   
   
}

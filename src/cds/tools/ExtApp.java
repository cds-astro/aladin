// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

package cds.tools;
import java.io.*;

/**
 * Interface between two Virtual Observatory applications, such as
 * Aladin (CDS) or VOPlot (PSPL/IUCAA)
 *
 * @author Pierre Fernique [CDS] on a collaboration between
 *         CDS team and PSPL/IUCAA team
 * @version 1.0 : 14 march 2003 creation
 */
public abstract interface ExtApp {
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
    */
   public abstract void loadVOTable(ExtApp app, InputStream in);

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
}

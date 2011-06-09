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

import cds.vizier.*;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Le formulaire d'interrogation des Surveys
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (2 mai 2002) Creation
 */
public final class ServerVizieRSurvey extends ServerVizieRMission  {

   protected void init() {
      aladinLogo = "VizieRSLogo.gif";
      docUser="http://vizier.u-strasbg.fr";
   }
   
   protected void createChaine() {
      super.createChaine();
      aladinLabel       = aladin.chaine.getString("SRNAME");
      nomTextfield= aladin.chaine.getString("SRCAT");
      title     = aladin.chaine.getString("SRTITLE");
      description      = aladin.chaine.getString("SRINFO");
      default_methode = aladin.chaine.getString("SRINFO1");
      help_list = aladin.chaine.getString("SRINFO2");
      verboseDescr      = aladin.chaine.getString("SRDESC");      
   }

   protected ServerVizieRSurvey(Aladin aladin,Vector v) {
      super(aladin,v);
   }
}

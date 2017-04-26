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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Vector;

import cds.tools.MultiPartPostOutputStream;

/**
 * Le formulaire pour interroger par X-match
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 26 avril 2017
 */
public class ServerXmatch extends ServerMocQuery  {


   protected void init() {
      type        = CATALOG;
      title       = "X-match query";
      aladinLabel = "X-match";
      title1      = "Choose a local table";
      title2      = "Catalog to x-match";
      baseUrl     = "http://cdsxmatch.u-strasbg.fr/xmatch/api/v1/sync";
   }

   protected void createChaine() {
      super.createChaine();
      description = "X-match query";
   }

   protected ServerXmatch(Aladin aladin) { super(aladin); }
   
   protected Vector<Plan> getPlans() {
      return aladin.calque.getPlans(PlanCatalog.class);
   }

   protected String getPlanName() {
      String s = planName==null ? getCatName() : planName;
      return s+" via Xmatch";
   }

   
   protected String getCatName() {
      String s = super.getCatName();
      if( s.endsWith("Simbad") ) return "simbad";
      if( s.startsWith("CDS/") ) return "vizier:"+s.substring(4);
      return s;
   }
   
   protected void addUpload( MultiPartPostOutputStream out, Plan plan ) throws Exception {
      Legende leg = plan.getFirstLegende();
      boolean addCoo=false;
      
      String raName, deName;
      int raIndex = leg.getRa();
      int deIndex = leg.getDe();
      String dataType = leg.getDataType(raIndex);
      String unit = leg.getUnit(raIndex);
      if( dataType!=null && (dataType.equals("double") || dataType.equals("float")) 
            && unit!=null && unit.startsWith("deg")) {
         raName = leg.getName( raIndex );
         deName = leg.getName( deIndex );
         addCoo=false;
      } else {
         raName = "_RAJ2000";
         deName = "_DEJ2000";
         addCoo=true;
      }
      
      out.writeField("colRA2", raName );
      out.writeField("colDec2", deName );
      aladin.trace(4,"with local plan="+plan.getLabel()+" "+plan.getCounts()+"src RA="+raName+" DE="+deName+"...");
     
//      File file = File.createTempFile("tmp", "xml");
//      file.deleteOnExit();
      File file = new File("D:\\Temp.xml");
      DataOutputStream dos = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(file)) );
      aladin.writePlaneInVOTable(plan, dos, addCoo,false);
      dos.close();
      out.writeFile("cat2", null, file, false);
   }
   
   protected void addParameter( MultiPartPostOutputStream out ) throws Exception {
      String catName = getCatName();
      aladin.trace(4,"Xmatch ["+catName+"]...");
      
      out.writeField("REQUEST", "xmatch");
      out.writeField("cat1", catName);
      out.writeField("distMaxArcsec", "5");
      out.writeField("selection", "best");
      out.writeField("cols2", "");
      out.writeField("RESPONSEFORMAT", "votable");
      
//      String limit = getLimit();
//      if ( ! limit.equals("unlimited")) {
//          limit = limit.replaceAll(",", "");
//          out.writeField("MAXREC", limit);
//      }
   }


}

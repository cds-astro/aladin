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

import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import cds.tools.Util;

/** Gère un losange Healpix contenant la liste des progéniteurs 
 * (images originales qui ont permis de créer le survey Healpix)
 * => Cette arborescence Healpix se trouve toujours dans le répertoire "HpxFinder"
 */
public class HealpixKeyFinder extends HealpixKey {
   
   int mem=0;
   ArrayList<String> list;
   
   protected HealpixKeyFinder(PlanBG planBG) { super(planBG); }
   
   protected HealpixKeyFinder(PlanBG planBG,int order, long npix) {
      super(planBG,order,npix,ASYNC);
      System.out.println("==> HealpixKeyFinder sur "+fileNet);
   }
   
   protected String getFileNet() {
      return getFilePath(null,order,npix);
   }
   
   protected String getFileCache() {
      return getFilePath(planBG.survey+planBG.version,order,npix);
   }

   /** Retourne le nom de fichier complet en fonction du survey, de l'ordre et du numéro healpix */
   static protected String getFilePath(String survey,int order, long npix) {
      return
      (survey!=null ? survey + "/" :"") +
      "HpxFinder/" +
      "Norder" + order + "/" +
      "Dir" + ((npix / 10000)*10000) + "/" +
      "Npix" + npix;
   }
   
   protected long loadCache(String filename) throws Exception {
      loadFile(filename);
      stream=null;     // Inutile de conserver le stream puisqu'on le prend du cache
      return 0;
   }
   
   protected long loadNet(String filename) throws Exception {
      loadFile(filename);
      return 0;
   }
   
   private void loadFile(String filename) throws Exception {
      list = new ArrayList<String>(10);
      stream = loadStream(filename);
      mem = stream.length;
      
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(stream));
      String s;
      while( (s=in.readLine())!=null ) list.add(s);
      in.close();
   }
   
   protected int writeCache() throws Exception {
      int n=writeStream();
      stream=null;        // Inutile de conserver le stream plus longtemps
      return n;
   }
   
   protected int free() { return free(true); }
   
   /** Libère le losange
    * @param force si false, ne libère que si aucune source n'est sélectionnée
    * @return 1 si libéré, 0 sinon
    */
   protected int free(boolean force) {
      
      // Abort ? Cache ?
      int status = getStatus();
      if( status==LOADINGFROMCACHE || status==LOADINGFROMNET ) abort();  // Arrêt de lecture
      else if( status==READY && planBG.useCache) write();                // Sauvegarde en cache

      list=null;
      mem=0;
      setStatus(UNKNOWN);
      return 1;
   }

   @Override
   protected void clearBuf() { }
   
   @Override
   protected int getMem() { return mem;}
   
   protected int draw(Graphics g, ViewSimple v,TreeMap<String,TreeNodeProgen> set) { 
      for( String json : list ) {
         String key = Util.extractJSON("name",json);
         if( key==null) key= Util.extractJSON("path",json);
         
         String url = ((PlanBGFinder)planBG).resolveImageSourcePath(json);
//         TreeNodeProgen node = new TreeNodeProgen(planBG.aladin, key, key, "Progen",
//               "http://cadc.hia.nrc.gc.ca/getData/anon/HSTCA/"+key+"_drz[1]",s);
         TreeNodeProgen node = new TreeNodeProgen(planBG.aladin, key, key, "Progen",
               url,json);
         set.put(key,node);
      }
      resetTimer();
      resetTimeAskRepaint();
      return list.size(); 
   }
   
   protected int getCounts() { return list==null ? 0 : list.size(); }
   
   /** Pour du debuging */
   @Override
   public String toString() {
      int status = getStatus();
      String code = status==HealpixKey.LOADINGFROMNET || status==HealpixKey.LOADINGFROMCACHE ? "**" :
         status==HealpixKey.TOBELOADFROMNET || status==HealpixKey.TOBELOADFROMCACHE ? " x" : " .";

      long t = (int)(getAskRepaintTime()/1000L);
      return code+"["+Util.align(priority+"",5)+"] "+
             Util.align(getStringNumber(),8)+
             Util.align(getCounts()+"d",8)+
             Util.align(getLongFullMem(),8)+
             Util.align(getStatusString(),16)+
             ( timer==-1 ? -1 : getLiveTime()/1000 ) +
             "/"+t + "s => "+VIE[-getLive()]+
             (getStatus()==READY?(fromNet?" Net":" Cache")+":"+timeStream+"ms" : "");
   }


 
}

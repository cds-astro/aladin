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

import java.io.ByteArrayInputStream;

import cds.tools.Util;

/** Gère un losange Healpix contenant la liste des progéniteurs 
 * (images originales qui ont permis de créer le survey Healpix)
 * => Cette arborescence Healpix se trouve toujours dans le répertoire "HpxFinder"
 */
public class HealpixKeyIndex extends HealpixKey {
   
   int mem=0;
   HealpixIndex index;
   
   protected HealpixKeyIndex(PlanBG planBG) { super(planBG); }
   
   protected HealpixKeyIndex(PlanBG planBG,int order, long npix) {
      super(planBG,order,npix,ASYNC);
//      System.out.println("==> HealpixKeyIndex sur "+fileNet);
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
      stream = loadStream(filename);
      mem = stream.length;
      index = new HealpixIndex();
      index.loadStream(new ByteArrayInputStream(stream));
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

      index=null;
      mem=0;
      setStatus(UNKNOWN);
      return 1;
   }

   @Override
   protected void clearBuf() { }
   
   @Override
   protected int getMem() { return mem;}
   
   protected int addHealpixIndexItem(HealpixIndex hi,ViewSimple v) {
      for( String id : index ) {
         HealpixIndexItem hii = index.get(id);
         if( hii.isOutView(v) ) continue;
         hi.put(id, hii);
      }
      resetTimer();
      return hi.size(); 
   }
   
   protected int getCounts() { return index==null ? 0 : index.size(); }
   
   /** Pour du debuging */
   @Override
   public String toString() {
      int status = getStatus();
      String code = status==HealpixKey.LOADINGFROMNET || status==HealpixKey.LOADINGFROMCACHE ? "**" :
         status==HealpixKey.TOBELOADFROMNET || status==HealpixKey.TOBELOADFROMCACHE ? " x" : " .";

//      long t = (int)(getAskRepaintTime()/1000L);
      return code+"["+Util.align(priority+"",5)+"] "+
             Util.align(getStringNumber(),8)+
             Util.align(getCounts()+"d",8)+
             Util.align(getLongFullMem(),8)+
             Util.align(getStatusString(),16)+
             ( timer==-1 ? -1 : getCurrentLiveTime()/1000 ) +
//             "/"+t + "s => "+VIE[-getLive()]+
             "s => "+VIE[-getLive()]+
             (getStatus()==READY?(fromNet?" Net":" Cache")+":"+timeStream+"ms" : "");
   }


 
}

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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

import cds.tools.Util;

public class HealpixKeyCat extends HealpixKey {

   Pcat pcat=null;
   int mem=0;
   boolean last;

   protected HealpixKeyCat(PlanBG planBG) { super(planBG); }

   protected HealpixKeyCat(PlanBG planBG,int order, long npix) {
      super(planBG,order,npix,ASYNC);
      last=false;
   }

   protected void updateCacheIfRequired(int time) throws Exception {
      String pathName = planBG.getCacheDir();
      pathName = pathName+Util.FS+fileCache;
      long ifmodifiedsince = new File(pathName).lastModified();
      String fileName = planBG.url+"/"+fileNet;
      URLConnection conn = (new URL(fileName)).openConnection();
      conn.setIfModifiedSince(ifmodifiedsince);
      try {
         conn.setReadTimeout(time);
         MyInputStream dis = (new MyInputStream(conn.getInputStream())).startRead();
         stream = readFully(dis,true);
         Aladin.trace(4,getStringNumber()+" => cache update");
         writeCache();
         dis.close();
      } catch( Exception e ) {
         //            System.out.println("Le cache est conservé");
      }
   }

   protected long loadCache(String filename) throws Exception {
      loadTSV(filename);
      stream=null;     // Inutile de conserver le stream puisqu'on le prend du cache
      return 0;
   }

   protected long loadNet(String filename) throws Exception {
      loadTSV(filename);
      return 0;
   }

   private void loadTSV(String filename) throws Exception {
      pcat = new Pcat(planBG);
      stream = loadStream(filename);
      mem = stream.length;

      testLast(stream);

      int trace=planBG.aladin.levelTrace;
      planBG.aladin.levelTrace=0;
      Legende leg = planBG.getFirstLegende();
      if( leg!=null ) pcat.setGenericLegende(leg);   // Indique a priori la légende à utiliser
      pcat.tableParsing(new MyInputStream(new ByteArrayInputStream(stream)),null);
      planBG.aladin.levelTrace=trace;

      if( !planBG.useCache ) stream=null;

      // Positionnement de la légende du premier Allsky chargé
      if( leg==null  ) ((PlanBGCat)planBG).setLegende( ((Source)pcat.iterator().next()).leg );
   }

   static final private char [] LAST = { '#','l','a','s','t',' ','l','e','v','e','l' };

   private void testLast(byte [] stream) {
      if( stream.length<LAST.length ) return;
      for( int i=0; i<LAST.length; i++ ) if( LAST[i]!=stream[i] ) return;
      last=true;
   }

   /** Retourne true s'il n'y a pas de descendant */
   protected boolean isLast() { return last; }

   /** Retourne true si on sait qu'il n'y a plus de descendance à charger */
   protected boolean isReallyLast(ViewSimple v) {
      if( last ) return true;
      for( int i=0; i<4; i++ ) {
         long filsPixid = npix*4+i;
         if( (new HealpixKey(planBG, order+1, filsPixid, NOLOAD)).isOutView(v) ) continue;
         HealpixKeyCat fils = (HealpixKeyCat) planBG.getHealpix(order+1, filsPixid, false);
         if( fils==null || fils.getStatus()==ERROR || !fils.isReallyLast(v) ) return false;
      }
      return true;
   }

   protected int writeCache() throws Exception {
      int n=writeStream();
      stream=null;        // Inutile de conserver le stream plus longtemps
      return n;
   }

   protected int free() { return free(true); }

   /** Libère le losange
    * @param force si false, ne libère que si aucune source n'est sélectionnée, ni tagguée
    * @return 1 si libéré, 0 sinon
    */
   protected int free(boolean force) {
      if( allSky ) return 0;

      // Abort ? Cache ?
      int status = getStatus();
      if( status==LOADINGFROMCACHE || status==LOADINGFROMNET ) abort();  // Arrêt de lecture
      else if( status==READY && planBG.useCache) write();                // Sauvegarde en cache

      if( pcat!=null ) {
         if( !force && pcat.hasSelectedOrTaggedObj() ) {
//            System.out.println(this+" impossible à supprimer => source sélectionnée ");
            return 0;   // Suppression impossible tant qu'un objet est sélectionné
         }
         pcat.free();
         mem=0;
      }
      planBG.nbFree++;
      setStatus(UNKNOWN);
      return 1;
   }

   @Override
   protected void clearBuf() { }

   @Override
   protected int getMem() { return mem;}

   @Override
   protected int draw(Graphics g, ViewSimple v) {
      if( pcat==null || !pcat.hasObj() ) return 0;

      // DE FAIT, DEJA TESTE DANS PlanBGCat.draw()
//      PointD[] b = getProjViewCorners(v);
//
//      boolean out=false;
//      nDraw++;
//      if( b==null  || (out=isOutView(v,b)) ) {
//         if( out ) nOut++;
//         return 0;
//      }
      pcat.draw(g, null, v, true, 0, 0);
      resetTimer();
      resetTimeAskRepaint();

      return pcat.getCount();
   }

   // Retourne le nombre de sources
   private int getCounts() {
      if( getStatus()==READY && pcat!=null ) return pcat.getCount();
      return 0;
   }

   /** Pour du debuging */
   @Override
   public String toString() {
      int status = getStatus();
      String code = status==HealpixKey.LOADINGFROMNET || status==HealpixKey.LOADINGFROMCACHE ? "**" :
         status==HealpixKey.TOBELOADFROMNET || status==HealpixKey.TOBELOADFROMCACHE ? " x" : " .";

      long t = (int)(getAskRepaintTime()/1000L);
      return code+"["+Util.align(priority+"",5)+"] "+
             Util.align(getStringNumber(),8)+
             Util.align(getCounts()+"s",8)+
             Util.align(getLongFullMem(),8)+
             Util.align(getStatusString(),16)+
             ( timer==-1 ? -1 : getLiveTime()/1000 ) +
             "/"+t + "s => "+VIE[-getLive()]+
             (getStatus()==READY?(fromNet?" Net":" Cache")+":"+timeStream+"ms" : "")+
             (isLast()?" last":"");
   }



}

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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Vector;

import cds.tools.Util;

public class HealpixKeyCat extends HealpixKey {

   Pcat pcat=null;
   int mem=0;
   boolean last;
   int nLoaded;
   int nTotal;

   protected HealpixKeyCat(PlanBG planBG) { super(planBG); }

   protected HealpixKeyCat(PlanBG planBG,int order, long npix) {
      super(planBG,order,npix,ASYNC);
      last=false;
      nTotal=0;
   }

   protected void updateCacheIfRequired(int time) throws Exception {
//      String pathName = planBG.getCacheDir();
//      pathName = pathName+Util.FS+fileCache;
//      long ifmodifiedsince = new File(pathName).lastModified();
//      String fileName = planBG.url+"/"+fileNet;
//      URLConnection conn = (new URL(fileName)).openConnection();
//      conn.setIfModifiedSince(ifmodifiedsince);
//      try {
//         conn.setReadTimeout(time);
//         MyInputStream dis = (new MyInputStream(conn.getInputStream())).startRead();
//         stream = readFully(dis,true);
//         Aladin.trace(4,getStringNumber()+" => cache update");
//         writeCache();
//         dis.close();
//      } catch( Exception e ) {
//         //            System.out.println("Le cache est conserv�");
//      }
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
      MyInputStream in = null;

      try {
         try {
            testLast(stream);      // A virer lorsqu'on n'utilisera plus les anciens HiPS
            testNLoaded(stream);   // A virer lorsqu'on n'utilisera plus les anciens HiPS
            testCompleteness(stream);
         } catch( Exception e ) {  if( Aladin.levelTrace>=3 ) e.printStackTrace(); }

         int trace=planBG.aladin.levelTrace;
         planBG.aladin.levelTrace=0;
         Legende leg = planBG.getFirstLegende();
         if( leg!=null ) pcat.setGenericLegende(leg);   // Indique a priori la l�gende � utiliser
         pcat.tableParsing(in=new MyInputStream( getInputStreamFromStream() ),null);
         planBG.aladin.levelTrace=trace;

         if( !planBG.useCache ) stream=null;

         // Positionnement de la l�gende du premier Allsky charg�
         if( leg==null  ) ((PlanBGCat)planBG).setLegende( ((Source)pcat.iterator().next()).leg );

         // Dans le cas o� l'�poque aurait-�t� modifi�
         recomputePosition(leg,pcat);
      } finally { if( in!=null ) in.close(); }
   }
   
   /** Fournit un Inputstream � partir du bloc de byte lu */
   protected InputStream getInputStreamFromStream() throws Exception  {
      return new ByteArrayInputStream(stream);
   }
   
   /** Recalcule toutes les positions internes dans le cas o� l'�poque
    * aurait �t� modifi�e au pr�alable */ 
   public void recomputePosition(Legende leg,Pcat pcat) {
      if( planBG.epoch==null || planBG.getEpoch().toString("J").equals("J2000") ) return;
//      System.out.println("Adaptation � l'�poque "+planBG.getEpoch());
      int npmra = leg.getPmRa();
      int npmde = leg.getPmDe();
      if( npmra<=0 || npmde<=0 ) return;  // Inutile, pas de PM
      int nra   = leg.getRa();
      int nde   = leg.getDe();
      planBG.recomputePosition(pcat.iterator(),leg,nra,nde,npmra,npmde);
   }

   static final private char [] LAST = { '#','l','a','s','t',' ','l','e','v','e','l' };

   private void testLast(byte [] stream) {
      if( stream.length<LAST.length ) return;
      for( int i=0; i<LAST.length; i++ ) if( LAST[i]!=stream[i] ) return;
      last=true;
   }

   static final private char [] NLOADED = { '#',' ','n','L','o','a','d','e','d',':',' ' };
   
   // # nLoaded: 48/48
   private void testNLoaded(byte [] stream) {
      if( stream.length<NLOADED.length ) return;
      for( int i=0; i<NLOADED.length; i++ ) if( NLOADED[i]!=stream[i] ) return;
      int deb=NLOADED.length;
      int fin;
      int slash=0;
      for( fin=NLOADED.length; fin<stream.length 
         && stream[fin]!='\n' && stream[fin]!='\r' && stream[fin]!=' '; fin++ ) {
         if( stream[fin]=='/' ) slash=fin;
      }
      if( slash==0 ) return;
      if( fin==stream.length ) return;
      try {
         nLoaded = Integer.parseInt(new String(stream,deb,slash-deb));
         nTotal = Integer.parseInt(new String(stream,slash+1,fin-(slash+1)));
         last = nLoaded==nTotal;
//         System.out.println("Trouve ["+new String(stream,0,fin)+"] pour "+this);
      } catch( Exception e ) { nLoaded = 1; nTotal = 2; last=false; }
   }
   
   static final private char [] COMPLETENESS = { '#',' ','C','o','m','p','l','e','t','e','n','e','s','s',' ','=' };
   
   // # Completeness = 903 / 90811
   private void testCompleteness(byte [] stream) {
      if( stream.length<COMPLETENESS.length ) return;
      for( int i=0; i<COMPLETENESS.length; i++ ) if( COMPLETENESS[i]!=stream[i] ) return;
      int deb=COMPLETENESS.length;
      int fin;
      int slash=0;
      for( fin=COMPLETENESS.length; fin<stream.length 
         && stream[fin]!='\n' && stream[fin]!='\r'; fin++ ) {
         if( stream[fin]=='/' ) slash=fin;
      }
      if( slash==0 ) return;
      if( fin==stream.length ) return;
      try {
         nLoaded = Integer.parseInt((new String(stream,deb,slash-deb)).trim());
         nTotal = Integer.parseInt((new String(stream,slash+1,fin-(slash+1))).trim());
         last = nLoaded==nTotal;
//         System.out.println("Trouve ["+new String(stream,0,fin)+"] pour "+this);
      } catch( Exception e ) { nLoaded = 1; nTotal = 2; last=false; }
   }

  /** Retourne true s'il n'y a pas de descendant */
   protected boolean isLast() { return last; }
   
   /** Retourne true si on sait qu'il n'y a plus de descendance � charger */
   protected boolean isReallyLast(ViewSimple v) {
      return last;
//      if( last ) return true;
//      for( int i=0; i<4; i++ ) {
//         long filsPixid = npix*4+i;
//         if( (new HealpixKey(planBG, order+1, filsPixid, NOLOAD)).isOutView(v) ) continue;
//         HealpixKeyCat fils = (HealpixKeyCat) planBG.getHealpix(order+1, filsPixid, false);
//         if( fils==null || fils.getStatus()==ERROR || !fils.isReallyLast(v) ) return false;
//      }
//      return true;
   }

   protected int writeCache() throws Exception {
      int n=writeStream();
      stream=null;        // Inutile de conserver le stream plus longtemps
      return n;
   }

   protected int free() { return free(true); }

   /** Lib�re le losange
    * @param force si false, ne lib�re que si aucune source n'est s�lectionn�e, ni taggu�e
    * @return 1 si lib�r�, 0 sinon
    */
   protected int free(boolean force) {
      if( allSky ) return 0;

      // Abort ? Cache ?
      int status = getStatus();
      if( status==LOADINGFROMCACHE || status==LOADINGFROMNET ) abort();  // Arr�t de lecture
      else if( status==READY && planBG.useCache) write();                // Sauvegarde en cache

      if( pcat!=null ) {
         if( !force && pcat.hasSelectedOrTaggedObj() ) {
//            System.out.println(this+" impossible � supprimer => source s�lectionn�e ");
            return 0;   // Suppression impossible tant qu'un objet est s�lectionn�
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
   protected int draw(Graphics g, ViewSimple v) { return draw1(g,v,false); }
   protected int drawOnlySelected(Graphics g, ViewSimple v) { return draw1(g,v,true); }
   private int draw1(Graphics g, ViewSimple v,boolean onlySelected) {
      if( pcat==null || !pcat.hasObj() ) return 0;
      int nb = pcat.draw(g, null, v, true, onlySelected, 0, 0);
      resetTimer();
      return nb;
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

//      long t = (int)(getAskRepaintTime()/1000L);
      return code+"["+Util.align(priority+"",5)+"] "+
             Util.align(getStringNumber(),8)+
             Util.align(getCounts()+"s",8)+
             Util.align(getLongFullMem(),8)+
             Util.align(getStatusString(),16)+
             ( timer==-1 ? -1 : getLiveTime()/1000 ) +
//             "/"+t + "s => "+VIE[-getLive()]+
             "s => "+VIE[-getLive()]+
             (getStatus()==READY?(fromNet?" Net":" Cache")+":"+timeStream+"ms" : "")+
             (isLast()?" last":"")+
             (nTotal!=0?" "+nLoaded+"/"+nTotal:"");
   }



}

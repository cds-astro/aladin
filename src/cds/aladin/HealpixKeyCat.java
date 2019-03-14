// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Graphics;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

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

   // POUR LE MOMENT JE L'INVALIDE A CAUSE DES CATALOGUES HiPS DE FX QUI CHANGENT DE DATE CONSTAMMENT
   protected void updateCacheIfRequired(int time) throws Exception {
      if( !planBG.live ) return;
//      String pathName = planBG.getCacheDir();
//      pathName = pathName+Util.FS+fileCache;
//      long ifmodifiedsince = new File(pathName).lastModified();
//      String fileName = planBG.url+"/"+fileNet;
//      URLConnection conn = (new URL(fileName)).openConnection();
//      conn.setIfModifiedSince(ifmodifiedsince);
//      MyInputStream dis=null;
//      try {
//         conn.setReadTimeout(time);
//         dis = (new MyInputStream(conn.getInputStream())).startRead();
//         stream = readFully(dis,true);
//         Aladin.trace(4,getStringNumber()+" => cache update");
//         writeCache();
//         dis.close();
//      } catch( Exception e ) {
//         //            System.out.println("Le cache est conservé");
//      } finally { if( dis!=null ) dis.close(); }
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
         try { testLast(stream); } catch( Exception e ) {  if( Aladin.levelTrace>=3 ) e.printStackTrace(); }

         int trace=planBG.aladin.levelTrace;
         planBG.aladin.levelTrace=0;
//         Legende leg = planBG.getFirstLegende();
//         if( leg!=null ) pcat.setGenericLegende(leg);   // Indique a priori la légende à utiliser
         
         in=new MyInputStream( getInputStreamFromStream() );
         in.setFileName(filename);
         pcat.tableParsing(in,null);
         planBG.aladin.levelTrace=trace;

         if( !planBG.useCache ) stream=null;

         // Positionnement de la légende du premier Allsky chargé
//         if( leg==null  ) ((PlanBGCat)planBG).setLegende( ((Source)pcat.iterator().next()).leg );
         if(  !((PlanBGCat)planBG).hasGenericPcat() )  ((PlanBGCat)planBG).setGenericPcat(pcat);

         // Dans le cas où l'époque aurait-été modifié
         recomputePosition(((PlanBGCat)planBG).getFirstLegende(),pcat);
      } finally { if( in!=null ) in.close(); }
   }
   
   /** Fournit un Inputstream à partir du bloc de byte lu */
   protected InputStream getInputStreamFromStream() throws Exception  {
      return new ByteArrayInputStream(stream);
   }
   
   /** Recalcule toutes les positions internes dans le cas où l'époque
    * aurait été modifiée au préalable */ 
   public void recomputePosition(Legende leg,Pcat pcat) {
      if( planBG.epoch==null || planBG.getEpoch().toString("J").equals("J2000") ) return;
//      System.out.println("Adaptation à l'époque "+planBG.getEpoch());
      int npmra = leg.getPmRa();
      int npmde = leg.getPmDe();
      if( npmra<=0 || npmde<=0 ) return;  // Inutile, pas de PM
      int nra   = leg.getRa();
      int nde   = leg.getDe();
      planBG.recomputePosition(pcat.iterator(),leg,nra,nde,npmra,npmde);
   }

   static final private char [] NLOADED = { '#',' ','n','L','o','a','d','e','d',':',' ' };
   static final private char [] COMPLETENESS = { '#',' ','C','o','m','p','l','e','t','e','n','e','s','s',' ','=' };
  
   // Postionne le flag last selon méthode "Completeness", retourne true si on a trouvé ce commentaire
   // # Completeness = 903 / 90811
   // # nLoaded: 48/48   (alternative pour compatibilité)
   private boolean testLast(byte [] stream) {
      boolean rep;
      
      // En début de fichier
      rep = testLast(stream,0,COMPLETENESS);
      if( !rep ) testLast(stream,0,NLOADED);    // A virer lorsque FX aura fait la modif sur son serveur
      
      // Parmi des commentaires ?
      if( !rep ) {
         for( int i=1; !rep && i<stream.length-1; i++) {
            if( stream[i]=='\n' || stream[i]=='\r' ) {  
               if( stream[i+1]=='#' ) {
                  rep=testLast(stream,i+1,COMPLETENESS);    
                  if( !rep ) rep=testLast(stream,i+1,NLOADED);    // A virer lorsque FX aura fait la modif sur son serveur
               }
               else if( stream[i+1]!='\n' && stream[i+1]!='\r' ) break;  // fin des commentaires ?
            }
         }
      }
      return rep;
   }
   
   // Scanne à partir de l'offset
   private boolean testLast(byte [] stream,int offset, char [] signature) {
      if( stream.length<signature.length ) return false;
      for( int i=offset; i<signature.length; i++ ) if( signature[i]!=stream[i] ) return false;
      int deb=offset+signature.length;
      int fin;
      int slash=0;
      for( fin=offset+signature.length; fin<stream.length 
         && stream[fin]!='\n' && stream[fin]!='\r' && stream[fin]!=' '; fin++ ) {
         if( stream[fin]=='/' ) slash=fin;
      }
      if( slash==0 ) return false;
      if( fin==stream.length ) return false;
      try {
         nLoaded = Integer.parseInt(new String(stream,deb,slash-deb));
         nTotal = Integer.parseInt(new String(stream,slash+1,fin-(slash+1)));
         last = nLoaded==nTotal;
//         System.out.println("Trouve ["+new String(stream,0,fin)+"] pour "+this);
      } catch( Exception e ) { nLoaded = 1; nTotal = 2; last=false; }
      return true;
   }
   
   
//   // Postionne le flag last selon méthode "Completeness", retourne true si on a trouvé ce commentaire
//   // # Completeness = 903 / 90811
//   private boolean testCompleteness(byte [] stream) {
//      if( stream.length<COMPLETENESS.length ) return false;
//      for( int i=0; i<COMPLETENESS.length; i++ ) if( COMPLETENESS[i]!=stream[i] ) return false;
//      int deb=COMPLETENESS.length;
//      int fin;
//      int slash=0;
//      for( fin=COMPLETENESS.length; fin<stream.length 
//         && stream[fin]!='\n' && stream[fin]!='\r'; fin++ ) {
//         if( stream[fin]=='/' ) slash=fin;
//      }
//      if( slash==0 ) return false;
//      if( fin==stream.length ) return false;
//      try {
//         nLoaded = Integer.parseInt((new String(stream,deb,slash-deb)).trim());
//         nTotal = Integer.parseInt((new String(stream,slash+1,fin-(slash+1))).trim());
//         last = nLoaded==nTotal;
////         System.out.println("Trouve ["+new String(stream,0,fin)+"] pour "+this);
//      } catch( Exception e ) { nLoaded = 1; nTotal = 2; last=false; }
//      return true;
//   }

  /** Retourne true s'il n'y a pas de descendant */
   protected boolean isLast() { return last; }
   
   /** Retourne true si on sait qu'il n'y a plus de descendance à charger */
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
   protected int draw(Graphics g, ViewSimple v) { return draw1(g,v,false); }
   protected int drawOnlySelected(Graphics g, ViewSimple v) { return draw1(g,v,true); }
   private int draw1(Graphics g, ViewSimple v,boolean onlySelected) {
      if( pcat==null || !pcat.hasObj() ) return 0;
      int nb = pcat.draw(g, null, v, true, onlySelected, 0, 0);
      resetTimer();
      return nb;
   }
   
   protected void resetDrawnInView(ViewSimple v) {
      if( pcat==null || !pcat.hasObj() ) return;
      pcat.resetDrawnInView(v);
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
             ( timer==-1 ? -1 : getCurrentLiveTime()/1000 ) +
//             "/"+t + "s => "+VIE[-getLive()]+
             "s => "+VIE[-getLive()]+
             (getStatus()==READY?(fromNet?" Net":" Cache")+":"+timeStream+"ms" : "")+
             (isLast()?" last":"")+
             (nTotal!=0?" "+nLoaded+"/"+nTotal:"");
   }



}

// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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

package cds.aladin;

import java.util.Date;

import cds.fits.HeaderFits;
import cds.fits.UtilFits;
import cds.tools.Util;

/**
 * Plan dedie � une image FITS compress�
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : jan 2020 - restructuration compl�te avec utilisation de UtilFits.uncompress
 * @version 1.1 : sept 2011 - Prise en compte ZSCALE, et ZZERO
 * @version 1.0 : mars 2008 - creation
 */
public class PlanImageFitsCmp extends PlanImage {

   protected PlanImageFitsCmp(Aladin aladin, String file,MyInputStream inImg,String label,String from,
         Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
      super(aladin,file,inImg,label,from,o,imgNode,skip,doClose,forPourcent);
   }
   
   protected boolean cacheImageFits(MyInputStream dis) throws Exception {
      
      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderFits(this,dis);

      setPourcent(0);
      
      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      // Lecture/decompression de l'image
      HeaderFits outHeader = new HeaderFits();
      try {
         pixelsOrigin = UtilFits.uncompress(outHeader, headerFits.getHeaderFits(), dis, flagSkip);
//         loadFitsHeaderParam(new FrameHeaderFits(outHeader));
      } catch( Exception e ) {
         String m = e.getMessage();
         if( m==null ) m="FITS compressed image read error";
         error=aladin.error=m;
         Aladin.error(error,1);
         return false;
      }
      if( flagSkip ) return true;
      
      // R�cup�ration des propri�t�s de l'image
      bitpix = outHeader.getIntFromHeader("BITPIX");
      width  = naxis1 = outHeader.getIntFromHeader("NAXIS1");
      height = naxis2 = outHeader.getIntFromHeader("NAXIS2");
      npix = Math.abs(bitpix)/8;  

      // Cr�ation du buffer 8bits
      boolean cut = aladin.configuration.getCMCut();
      setBufPixels8(new byte[width*height]);
      findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0,0,0,0);
      to8bits(getBufPixels8(),0,pixelsOrigin,width*height,bitpix, pixelMin,pixelMax,true);
      invImageLine(width,height,getBufPixels8());

      // N�cessaire pour que la cr�ation de la Calib ne se plante pas
      headerFits.setKeyword("BITPIX",bitpix+"");
      headerFits.setKeyword("NAXIS1",width+"");
      headerFits.setKeyword("NAXIS2",height+"");
      headerFits.setKeyword("NAXIS","2");

      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Reading, uncompressing "+(cut?"and autocutting ":"")+"in "+Util.round(temps/1000.,3)+" s => "+Util.round(((double)offsetLoad/temps)/(1024*1.024),2)+" Mbyte/s");

      creatDefaultCM();
      setPourcent(99);
      return true;
   }

   
   
//   protected boolean cacheImageFits1(MyInputStream dis) throws Exception {
//
//      int taille;       // nombre d'octets a lire
//      int n;            // nombre d'octets pour un pixel
//
//      // Lecture de l'entete Fits si ce n'est deja fait
//      if( headerFits==null ) headerFits = new FrameHeaderFits(this,dis);
//
//      bitpix = headerFits.getIntFromHeader("ZBITPIX");
//      width  = naxis1 = headerFits.getIntFromHeader("ZNAXIS1");
//      height = naxis2 = headerFits.getIntFromHeader("ZNAXIS2");
//      npix = n = Math.abs(bitpix)/8;    // Nombre d'octets par valeur
//      taille=width*height*n;    // Nombre d'octets
//      setPourcent(0);
//      Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" BITPIX="+bitpix+" => size="+taille);
//
//      // Les param�tres FITS facultatifs
//      loadFitsHeaderParam(headerFits);
//
//      // Pour des stats
//      Date d = new Date();
//      Date d1;
//      int temps;
//
//      int nnaxis1 = headerFits.getIntFromHeader("NAXIS1");
//      int nnaxis2 = headerFits.getIntFromHeader("NAXIS2");
//      int theap=nnaxis1*nnaxis2;
//      try  { theap = headerFits.getIntFromHeader("THEAP"); } catch( Exception e ) {}
//      int pcount=headerFits.getIntFromHeader("PCOUNT");    // nombres d'octets a lire en tout
//      
//      // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
//      if( flagSkip ) {
//         dis.skip( theap+pcount );
//         return true;
//      }
//
//      // Nombre de pixels d'une tuile (par d�faut une ligne)
//      int tile1 = width;
//      try { tile1 = headerFits.getIntFromHeader("ZTILE1"); } catch( Exception e ) {}
//      int tile2 = 1;
//      try { tile2 = headerFits.getIntFromHeader("ZTILE2"); } catch( Exception e ) {}
//      
//      // Eventuels param�tres propre � un mode de compression particulier
//      int val1=32;   // nblock pour RICE
//      try { val1 = headerFits.getIntFromHeader("ZVAL1"); } catch( Exception e ) {}
//      int val2=4;    // bsize pour RICE
//      try { val2 = headerFits.getIntFromHeader("ZVAL2"); } catch( Exception e ) {}
//
//
//      // Param�tre de quantification ?
//      try {
//         String quantiz  = headerFits.getStringFromHeader("ZQUANTIZ");
////         String zdither0 = headerFits.getStringFromHeader("ZDITHER0");
//         if( quantiz!=null && !quantiz.equals("NO_DITHER") ) {
//            System.err.println(getMode()+" FITS image extension problem (unsupported ZQUANTIZ ["+quantiz+"] => assuming NO_DITHER)");
//         }
//      } catch( Exception e ) {}
//
//      boolean cut = aladin.configuration.getCMCut();
//      setBufPixels8(new byte[width*height]);
//
//      int posCompress=0;            // Position du champ du segment compress�
//      int posgzipCompress=-1;       // Position du champ du segment s'il est directement GZIP�
//      int posUncompress=-1;         // Position du champ du segment s'il est non compress� (non standard)
//      int posZscale=-1;             // Position du champ du gacteur d'�chelle
//      int posZzero=-1;              // Position du champ d'origine pour l'application d'un facteur d'�chelle
//
//      // Parcours des champs de la table binaire
//      char sForm = ' ';
//      int tfields = headerFits.getIntFromHeader("TFIELDS");
//      for( int i=1,pos=0; i<=tfields; i++ ) {
//         String type = headerFits.getStringFromHeader("TTYPE"+i);
//              if( type.equals("COMPRESSED_DATA") )        { posCompress = pos;     sForm='B'; }
//         else if( type.equals("GZIP_COMPRESSED_DATA") )   { posgzipCompress = pos; sForm='B'; }
//         else if( type.equals("UNCOMPRESSED_DATA") )      { posUncompress = pos;   sForm='B'; }
//         else if( type.equals("ZSCALE") )                 { posZscale = pos;       sForm='D'; }
//         else if( type.equals("ZZERO") )                  { posZzero = pos;        sForm='D'; }
//              
//         String form = headerFits.getStringFromHeader("TFORM"+i);
//         pos+=Util.binSizeOf(form);
//         
//         // V�rification du type de donn�es de chaque champ
//         if( form.indexOf(sForm)<0 ) throw new Exception(getMode()+" FITS image extension problem ("
//                                               +type+" fields with data type ["+form+"] not supported yet)");
//      }
//      
//      Aladin.trace(2,"Loading "+getMode()+" FITS image extension (tileSize:"+tile1+"x"+tile2+"pix)");
//      
//      if( posUncompress>=0 ) System.err.println(getMode()+" FITS image extension warning (deprecated UNCOMPRESSED_DATA field)");
//
//      pixelsOrigin = new byte[taille];
//      byte [] table = new byte[nnaxis1*nnaxis2];
//      byte [] heap = new byte[pcount];
//
//      try {
//         dis.readFully(table);
//         dis.skip(theap - nnaxis1*nnaxis2);
//         dis.readFully(heap);
//         
//         int nbTileInWidth = naxis1/tile1;
//         
//         // Y a-t-il un nombre entier de tuiles en largeur ?
//         if( naxis1%tile1!=0 ) {
//            nbTileInWidth++;
//         }
//
//         int pixPos=0;   // indice du pixel courant dans la matrice de pixel finale
//         
//         for( int numTile=0; numTile<nnaxis2; numTile++ ) {
//            int offsetRec = numTile*nnaxis1;
//            int size = getInt(table,offsetRec+posCompress);    // Taille du champ variable (suppos� en Bytes)
//            int pos =  getInt(table,offsetRec+posCompress+4);  // position du champ variable
//            
//            double bzero  = posZzero<0  ? 0 : getDouble(table,offsetRec+posZzero);  // Param�tre origine fact d'�chelle (suppos� en double)
//            double bscale = posZscale<0 ? 1 : getDouble(table,offsetRec+posZscale); // Param�tre fact d'�chelle (suppos� en double)
//
//            // Calcul de la position de la tuile dans la matrice finale de pixels
//            // On suppose que les tuiles sont rang�es de gauche � droite et de haut en bas
//            // (pas d'�crit dans le standards FITS mais semblerait logique)
//            int nbRowOfTiles       = numTile/nbTileInWidth;  // nombre de rang�s de tuiles d�j� trait�es
//            int nbTileInCurrentRow = numTile%nbTileInWidth;  // nombre de tuiles d�j� trait�es dans la ligne 
//            pixPos = nbRowOfTiles * tile2 * width + nbTileInCurrentRow*tile1;
//
//            // Directement compress GZIP
//            if( size==0 && posgzipCompress>=0 ) {
//               size = getInt(table,offsetRec+posgzipCompress);
//               pos  = getInt(table,offsetRec+posgzipCompress+4);
//               byte [] tile = gunzip( heap, pos, size);
//               copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
//            
//            // Non compress� (m�thode d�sormais obsol�te mais que je laisse au cas o�)
//            } else if( size==0 && posUncompress>=0 ) {
//               size = getInt(table,offsetRec+posUncompress);
//               pos  = getInt(table,offsetRec+posUncompress+4);
//               
//               byte [] tile = new byte[ size ];
//               System.arraycopy(heap, pos, tile, 0, size);
//               copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
////               direct(heap,pos,pixelsOrigin,offset,tile1,bitpix,bzero,bscale);
//               
//            // Compress� suivant m�thode indiqu�e
//            } else {
//               
//               // Sp�cifique � RICE
//               if( this instanceof PlanImageFitsRice )  {
//               
//                  // Ligne par ligne ? => on peut �crire directement dans la matrice de pixels
//                  if( tile1==width && tile2==1 ) {
//                     decomp(heap,pos,pixelsOrigin,pixPos,tile1*tile2,val1,val2,bitpix,bzero,bscale);
//                     
//                  // Sinon il faut passer par un tableau interm�diaire puis, dans un deuxi�me
//                  // temps, placer la tuile au bon endroit
//                  } else {
//                     byte [] tile = new byte [ tile1*tile2*npix ];
//                     decomp( heap,pos,tile,0,tile1*tile2,val1,val2,bitpix,0,1);
//                     copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
//                  }
//                  
//               // Autres m�thodes plus basiques
//               } else {
//                  byte [] tile = decomp( heap, pos, size);
//                  copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
//                 
//               }
//            }
//         }
//      } catch (Exception e ) { e.printStackTrace(); }
//
//      findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0,0,0,0);
//      to8bits(getBufPixels8(),0,pixelsOrigin,width*height,bitpix, pixelMin,pixelMax,true);
//
//
//      // N�cessaire pour que la cr�ation de la Calib ne se plante pas
//      headerFits.setKeyword("BITPIX",bitpix+"");
//      headerFits.setKeyword("NAXIS1",width+"");
//      headerFits.setKeyword("NAXIS2",height+"");
//      headerFits.setKeyword("NAXIS","2");
//
//      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
//      Aladin.trace(3," => Reading, uncompressing "+(cut?"and autocutting ":"")+"in "+Util.round(temps/1000.,3)+" s => "+Util.round(((double)offsetLoad/temps)/(1024*1.024),2)+" Mbyte/s");
//
//      // Retournement de l'image (les lignes ne sont pas rangees dans le meme ordre
//      // en FITS et en JAVA
//      invImageLine(width,height,getBufPixels8());
//
//      creatDefaultCM();
//      setPourcent(99);
//      return true;
//   }
//   
//   /** Copie des pixels de la tuile au bon endroit de la matrice des pixels */
//   public void copyTile(byte [] tile, int tileWidth, int tileHeight, 
//                        byte [] pix,  int pixPos,  int width,    int height,
//                        int bitpix, double bzero, double bscale ) throws Exception {
//
//      // Si pas de facteur d'�chelle, on va recopier d'ou coup les bytes
//      // constitutifs de chaque pixel
//      boolean flagScale = bscale!=1 || bzero!=0;
//      if( !flagScale ) {
//         
//         int npix = Math.abs(bitpix)/8;
//         
//         // si la tuile � la m�me largeur que l'image ou va faire un simple dump
//         if( tileWidth==width ) {
//            System.arraycopy( tile, 0, pix, pixPos*npix, tileWidth*tileHeight*npix );
//            return;
//         }
//            
//         // sinon il va falloir travailler ligne � ligne
//         for( int y=0; y<tileHeight; y++ ) {
//            int src = y*tileWidth;
//            int trg = (pixPos + y*width) * npix;
//            System.arraycopy(tile, src, pix, trg, tileWidth*npix);
//         }
//         return;
//      }
//      
//      // M�thode pixel par pixel pour pouvoir appliquer le facteur d'�chelle
//      for( int y=0; y<tileHeight; y++ ) {
//         for( int x=0; x<tileWidth; x++ ) {
//            double val = getPixVal1(tile, bitpix, y*tileWidth +x );
//            val = val*bscale+bzero;
//            setPixVal( pix, bitpix, pixPos + y*width +x, val);
//         }
//      }
//   }
//   
//   /** Decompression Gzip d'un buffer de bytes */
//   protected byte [] gunzip( byte buf[], int pos,int size ) throws Exception {
//      ByteArrayInputStream bytein = new ByteArrayInputStream(buf,pos,size);
//      GZIPInputStream gzin = new GZIPInputStream(bytein);
//      ByteArrayOutputStream byteout = new ByteArrayOutputStream();
//
//      int res = 0;
//      byte tmp[] = new byte[1024];
//      while (res >= 0) {
//          res = gzin.read(tmp, 0, tmp.length);
//          if (res > 0) byteout.write(tmp, 0, res);
//      }
//      return byteout.toByteArray();
//   }
//
//   public static void direct(byte buf[],int pos,byte array[], int offset,int tileSize,
//         int bitpix,double bzero,double bscale) throws Exception {
//      int pixSize = Math.abs(bitpix)/8;
//      for( int i=0; i<tileSize; i+=pixSize ) {
//         double val = getPixVal1(buf, bitpix, pos+i);
//         setPixVal(array, bitpix, offset+i, val*bscale+bzero);
//      }
//   }
//   
//   abstract byte [] decomp(byte buf[],int pos, int size) throws Exception;
//   
//   abstract public void decomp(byte buf[],int pos,byte array[], int offset,int tileSize,int val1,int val2,
//         int bitpix,double bzero,double bscale)  throws Exception ;
}
   


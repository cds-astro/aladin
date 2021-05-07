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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import cds.aladin.stc.STCObj;
import cds.moc.Healpix;
import cds.moc.SMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** Generation d'un plan MOC � partir d'une liste de plans (Image, Catalogue ou map HEALPix) 
 * @author P.Fernique [CDS]
 * @version 1.2 - mar 2019 - correction HipstoMoc + correction
 * @version 1.1 - mar 2016 - ajout probability sky map
 * @version 1.0 - nov 2012
 */
public class PlanMocGen extends PlanMoc {
   
   private Plan [] p;       // Liste des plans � ajouter dans le MOC
   private double radius;   // Pour un plan catalogue, rayon autour de chaque source (en degres), sinon 0
   private boolean fov;     // Plan un plan catalogue, true si on prend les FOVs associ�s
   private double pixMin;   // Pour un plan Image ou map Healpix, valeur minimale des pixels retenus (sinon NaN)
   private double pixMax;   // Pour un plan Image ou map Healpix, valeur maximale des pixels retenus (sinon NaN)
   private double threshold;// Pour un plan Image Healpix, seuil max de l'int�gration (sinon NaN)
   private int order;         // R�solution (ordre) demand�e
   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est termin�)
   
   protected PlanMocGen(Aladin aladin,String label,Plan[] p,int order,double radius,
         double pixMin,double pixMax,double threshold,boolean fov) {
      super(aladin,null,null,label,p[0].co,30);
      this.p = p;
      this.order=order;
      this.radius=radius;
      this.pixMin=pixMin;
      this.pixMax=pixMax;
      this.threshold=threshold;
      this.fov=fov;
      
      pourcent=0;
      gapPourcent = 100/p.length;
      
      suiteSpecific();
      threading();
      log();
   }
   
   protected void launchLoading() {}
   
   protected boolean Free() {
      stop=true;
      return super.Free();
   }
   
   private boolean stop;
   
   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double radius,int order,boolean fov) throws Exception {
      
      
      long t0,t1;
      t0=System.currentTimeMillis();
      stop=false;
      ((SMoc)moc).setMocOrder(order);
      SMoc m2 = new SMoc(order);
      m2.bufferOn(10000);
      Iterator<Obj> it = p1.iterator();
      int m= 0;
      Healpix hpx = new Healpix();
      double incrPourcent = gapPourcent/p1.getCounts();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         pourcent+=incrPourcent;
         m++;

         if( m%10000==0 ) {
            if( stop ) throw new Exception("Abort");
            try { moc = moc.union( m2 ); } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
            m2.clear();
         }

         try {

            // Ajout simple d'une cellule
            if( !fov && radius==0 ) {
               long npix = hpx.ang2pix(order, ((Position)o).raj,((Position)o).dej);
               m2.add(order,npix);
               continue;
            }

            // Ajouts par formes
            SMoc m1=null;
            // Par FoV ?
            if( fov ) {
               Source s = (Source)o;
               SourceFootprint sf = s.getFootprint();
               if( sf==null ) continue;
               List<STCObj> listStcs = sf.getStcObjects();
               if( listStcs==null ) continue;
               m1 = aladin.createMocRegion(listStcs,order,true);

               // Par Cones ?
            } else {
               m1 = aladin.createMocRegionCircle(((Position)o).raj, ((Position)o).dej, radius, order, true);
            }
            m2.setRangeList( m2.seeRangeList().union( m1.seeRangeList() ) );
            
         } catch( Exception e ) {
            if( aladin.levelTrace>=3) e.printStackTrace();
         }
      }
      try {  moc = moc.union( m2 ); } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      t1 =System.currentTimeMillis();
      aladin.trace(4,"SMOC created in "+Util.getTemps((t1-t0)*1000L)+" ("+m+" sources)");
   }

   // Ajout d'un plan Image au MOC en cours de construction
   private void addMocFromImage(Plan p1,double pixMin,double pixMax) {
      boolean flagRange = !Double.isNaN(pixMin) || !Double.isNaN(pixMax);
      PlanImage pimg = (PlanImage)p1;
      Healpix hpx = new Healpix();
      int o1 = order;
      Coord coo = new Coord();
      double gap=1;
      double gapA=0,gapD=0;
      try { 
         gapA = Math.min(p1.projd.getPixResAlpha(),p1.projd.getPixResDelta());
         for( o1=order; CDSHealpix.pixRes( o1 )/3600. <= gapA*2; o1--);
      } catch( Exception e1 ) {
      }
//      if( gap<1 || Double.isNaN(gap) ) gap=1;
//      
      gapD = CDSHealpix.pixRes( o1 )/3600.;
//      System.out.println("res="+res+" order="+order+" gapA ="+Coord.getUnit(gapA)+" gapD ="+Coord.getUnit(gapD)+" gap="+gap);
      
      // Pour garder en m�moire les pixels 
      pimg.setLockCacheFree(true);
      pimg.pixelsOriginFromCache();

      ((SMoc)moc).bufferOn();

      double incrPourcent = gapPourcent/pimg.naxis2;
      long oNpix=-1;  
      for( double y=0; y<pimg.naxis2; y+=gap ) {
         pourcent += incrPourcent;
         for( double x=0; x<pimg.naxis1; x+=gap ) {
            try {
               coo.x = x;
               coo.y = (pimg.naxis2-y-1);

               // dans du vide - on test d'abord le buffers 8bits, et on v�rifie si on tombe sur 0
               if( pimg.getPixel8Byte((int)x,(int)coo.y)==0 && Double.isNaN(pimg.getPixel((int)x,(int)y)) ) continue;

               // Hors de la plage de pixels
               if( flagRange ) {
                  double pix = pimg.getPixel((int)x,(int)y);
                  if( !Double.isNaN(pixMin) && pix<pixMin ) continue;
                  if( !Double.isNaN(pixMax) && pix>pixMax ) continue;
               }

               pimg.projd.getCoord(coo);
               long npix=0;
               npix = hpx.ang2pix(o1, coo.al, coo.del);

               // Juste pour �viter d'ins�rer 2x de suite le m�me npix
               if( npix==oNpix ) continue;
               ((SMoc)moc).add(o1,npix);

               oNpix=npix;
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
      }
      ((SMoc)moc).bufferOff();

      // Les pixels peuvent d�sormais �tre lib�r�s
      pimg.setLockCacheFree(false);
   }
   
   // Ajout d'un plan map Healpix au MOC en cours de construction
   private void addMocFromPlanBG(Plan p1,int order, double pixMin,double pixMax) throws Exception { addMocFromPlanBG(p1,order,-1,pixMin,pixMax); }
   
   private void addMocFromPlanBG(Plan p1,int order, int infoTileWidth, double pixMin,double pixMax) throws Exception {
      boolean flagRange = !Double.isNaN(pixMin) || !Double.isNaN(pixMax);
      PlanBG p = (PlanBG)p1;
      
      // Passage en mode FITS
      if( !p.hasOriginalPixels() ) p.switchFormat();
      
      // Recherche de la taille d'une tuile si non explicitement mentionn�e
      int tileOrder = infoTileWidth==-1 ? p.getTileOrder() : (int) CDSHealpix.log2(infoTileWidth);
      int tileWidth = (int)CDSHealpix.pow2(tileOrder);
      int z = (int)p.getZ();
      
      if( order> tileOrder+ p.maxOrder ) {
         order = tileOrder+p.maxOrder;
         aladin.warning("MOC order greater than HiPS resolution. Assuming MOC order "+order);
      }

      
      // Quel est l'ordre des tuiles requis
      int requiredHipsOrder = order-tileOrder;
      
      // Par d�faut on suppose que cet order Hips est disponible
      int hipsOrder = requiredHipsOrder;
      
      // Par d�faut on consid�re qu'un pixel de tuile correspond � un pixel du MOC
      int cellOrder = 0;
      
      // Cet ordre HiPS est-il disponible ? et sinon quel est le niveau sup le plus proche
      // Cela impactera le nombre de pixels � moyenner dans la tuile de d�part pour un pixel du MOC : 2^cellOrder = taille de ce bloc
      int minHipsOrder = p.getMinOrder();
      if( minHipsOrder>requiredHipsOrder ) {
         cellOrder = minHipsOrder - requiredHipsOrder;
         hipsOrder=minHipsOrder;
      }
      
      // On g�n�re d'abord un MOC dans le syst�me de r�f�rence de la map HEALPix
      // on fera la conversion en ICRS � la fin du processus
      ((SMoc)moc).setMocOrder(order);
      ((SMoc)moc).setSys( p.frameOrigin==Localisation.GAL ? "G" : 
                               p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
      ((SMoc)moc).bufferOn();
      
      frameOrigin = p.frameOrigin;
      // Nombre de losanges � traiter
      int n = (int)CDSHealpix.pow2(hipsOrder); 
      n=12*n*n;
      
//      System.out.println("Nombre de losanges � traiter : "+n);
      
      try { createHealpixOrder(tileOrder-cellOrder); }
      catch( Exception e1 ) { }
      
      double incrPourcent = gapPourcent/n;
      boolean first=true;
      
      int cellWidth = (int)CDSHealpix.pow2(cellOrder);   // taille d'un bloc (cot�)
      int nbCell = tileWidth/cellWidth;                  // Nombre de blocs � consid�rer dans une ligne de la tuile
      int virtualTileWidth = tileWidth/cellWidth;
      
      for( int npixFile=0; npixFile<n; npixFile++ ) {
//         System.out.println("Traitement de "+npixFile);
         pourcent += incrPourcent;
         if( p.moc!=null && p.isOutMoc(hipsOrder, npixFile) ) continue;
         
         HealpixKey h = p.getHealpixLowLevel(hipsOrder,npixFile,z,HealpixKey.SYNC);
         if( h==null ) continue;
         
         // Dans le cas o� la taille de la tuile annonc�e ou suppos�e est fausse
         // on recommence en indiquant explicitement cette taille
         if( first && infoTileWidth==-1 && h.width!=CDSHealpix.pow2(tileOrder) ) {
            if( aladin.levelTrace>=3) System.err.println("Warning, the HiPS \""+p.label+"\" has missing or erroneous tileWidth propertie value => assuming "+h.width);
            addMocFromPlanBG(p1,order,h.width,pixMin,pixMax);
            return;
         }
         first=false;
         
         long min = virtualTileWidth *virtualTileWidth * npixFile;
         try {
            
            // Parcours des blocs
            for( int yCell=0; yCell<nbCell; yCell++ ) {
               for( int xCell=0; xCell<nbCell; xCell++ ) {
                  
                  // Parcours des pixels du bloc courant et d�termination de la moyenne de ses pixels
                  double average = 0;
                  int nbPixels=0;
                  for( int y=0; y<cellWidth; y++ ) {
                     for( int x=0; x<cellWidth; x++ ) {
                        int idx = ((yCell*cellWidth)+y) * h.width + (xCell*cellWidth)+x;
                        double pixel = h.getPixel(idx,HealpixKey.NOW);
                        
                        // Pixel vide
                        if( Double.isNaN( pixel ) || isBlank && pixel==blank ) continue;
                        
                        pixel = pixel  * p.bScale+ p.bZero;
                        
                        average += pixel;
                        nbPixels++;
                     }
                  }
                  if( nbPixels==0 ) continue;
                  average /= nbPixels;
                  
                  // En dehors de la plage
                  if( flagRange ) {
                     if( !Double.isNaN(pixMin) && average<pixMin ) continue;
                     if( !Double.isNaN(pixMax) && average>pixMax ) continue;
                  }
                  
                  long npixMoc = min + xy2hpx(yCell*virtualTileWidth + xCell);
                  ((SMoc)moc).add(order,npixMoc);
              }
            }
            
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }
      
      ((SMoc)moc).bufferOff();
      
      // Conversion en ICRS si n�cessaire
      if( frameOrigin!=Localisation.ICRS ) {
         try {
            aladin.info("This HiPS uses a Galactic reference. The resulting MOC has been converted in Equatorial System");
            moc=toReferenceFrame("C");
            frameOrigin=Localisation.ICRS;
         } catch( Exception e ) { e.printStackTrace(); }
      }
   }

   
   /** Creation d'un plan Moc � partir d'un HiPS en prenant toutes les pixels qui repr�sentent
    * threshold (sommation) de la totalit� des pixels. On commence par les valeurs les plus grandes.
    * Permet par exemple de cr�er un MOC � 10% (threshold=0.1) pour des maps de probabilit�
    * issues de l'�tude des ondes gravitationnelles.
    */
   // Ajout d'un plan map Healpix au MOC en cours de construction
   private void addMocFromPlanBG(Plan p1, int order, double threshold) throws Exception { addMocFromPlanBG(p1,order, -1,threshold); }
   
   private void addMocFromPlanBG(Plan p1, int order, int infoTileWidth, double threshold) throws Exception {
      PlanBG p = (PlanBG)p1;
      if( order==-1 ) {
         order= infoTileWidth==-1 ? p.getMaxHealpixOrder() : p.getMaxFileOrder() + (int)CDSHealpix.log2(infoTileWidth);
         if( order>12 ) order=12;
      }
      
      // Passage en mode FITS
      if( !p.hasOriginalPixels() ) p.switchFormat();
      
      // Recherche de la taille d'une tuile si non explicitement mentionn�e
      int tileOrder = infoTileWidth==-1 ? p.getTileOrder() : (int) CDSHealpix.log2(infoTileWidth);
      int tileWidth = (int)CDSHealpix.pow2(tileOrder);
      int z = (int)p.getZ();
      
      if( order> tileOrder+ p.maxOrder ) {
         order = tileOrder+p.maxOrder;
         aladin.warning("MOC order greater than HiPS resolution. Assuming MOC order "+order);
      }
      
      // Quel est l'ordre des tuiles requis
      int requiredHipsOrder = order-tileOrder;
      
      // Par d�faut on suppose que cet order Hips est disponible
      int hipsOrder = requiredHipsOrder;
      
      // Par d�faut on consid�re qu'un pixel de tuile correspond � un pixel du MOC
      int cellOrder = 0;
      
      // Cet ordre HiPS est-il disponible ? et sinon quel est le niveau sup le plus proche
      // Cela impactera le nombre de pixels � moyenner dans la tuile de d�part pour un pixel du MOC : 2^cellOrder = taille de ce bloc
      int minHipsOrder = p.getMinOrder();
      if( minHipsOrder>requiredHipsOrder ) {
         cellOrder = minHipsOrder - requiredHipsOrder;
         hipsOrder=minHipsOrder;
      }
      
      // On g�n�re d'abord un MOC dans le syst�me de r�f�rence de la map HEALPix
      // on fera la conversion en ICRS � la fin du processus
      ((SMoc)moc).setSys( p.frameOrigin==Localisation.GAL ? "G" : 
                               p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
      frameOrigin = p.frameOrigin;
      
      // Nombre de losanges � traiter
      int n = (int)CDSHealpix.pow2(hipsOrder); 
      n=12*n*n;
      
      System.out.println("Nombre de losanges � traiter (order="+hipsOrder+") : "+n);
      
      try { createHealpixOrder(tileOrder-cellOrder); }
      catch( Exception e1 ) { }
      
      double incrPourcent = gapPourcent/n;
      boolean first=true;
      
      int cellWidth = (int)CDSHealpix.pow2(cellOrder);   // taille d'un bloc (cot�)
      int nbCell = tileWidth/cellWidth;                  // Nombre de blocs � consid�rer dans une ligne de la tuile
      int virtualTileWidth = tileWidth/cellWidth;
      
      // Liste des num�ros de tuiles � traiter. Dans la premi�re �tape on se contentera
      // de calculer la moyenne de l'ensemble des pixels concern�es dans chaque tuile.
      // Puis avant la deuxi�me �tape, on triera ces num�ros de tuiles en commen�ant par celles
      // qui ont la moyenne la plus �lev�e (le but est de commencer par les valeurs
      // les plus grandes pour �viter de saturer "queue" (voir ci-dessous)
      ArrayList<PixCum> tileList = new ArrayList<>(n);
      
      // La queue va servir dans la deuxi�me �tape.
      // On ne va garder que les pixels qui ont les plus grandes valeurs, et � chaque fois qu'on devra en ins�rer un nouveau, on virera
      // autant de pixels de plus petites valeurs dont la somme est �gale � la nouvelle valeur ins�r��
      Queue<PixCum> queue = new Queue<>( threshold );

      // Somme totale d�termin�e � la premi�re �tape, et qui permettra de normaliser les valeurs
      // entre 0 et 1 � la deuxi�me �tape.
      double somme=0;

      for( int step=0; step<2; step++ ) {
         
         // 1�re �tape, Initilisation de la liste des losanges =>  suit l'ordre naturel
         if( step==0 ) {
            for( int i=0; i<n; i++ ) tileList.add( new PixCum(i,0) );
            
         // 2�me �tape: Tri de la liste des losanges => suit l'odre d�croissant des moyennes
         } else  Collections.sort(tileList);
         
         // Parcours des tuiles
         for( PixCum tile : tileList ) {
            if( tile.npix==-1 ) continue;   // c'est une tuile �cart�e � l'�tape 1
            int tileNpix = (int) tile.npix;
            tile.npix = -1;   // par d�faut, la tuile sera suppos�e rejet�e;
            
            aladin.trace(4,"Processing tile "+tileNpix+" (step "+(step+1)+
                  (step==1 ? " mean="+tile.val+" max="+tile.max+" queue.size="+queue.size() : "")+")");
            
            double tileTotal = 0;  // Cumul des pixels de la tuile courante
            double tileMax = 0;    // Valeur max sur les pixels de la tuile courante
            int tileNbPixel = 0;   // Nomre de pixels de la tuile courante
         
            pourcent += incrPourcent/2;
            if( p.moc!=null && p.isOutMoc(hipsOrder, tileNpix) ) continue;

            HealpixKey h = p.getHealpixLowLevel(hipsOrder,tileNpix,z,HealpixKey.SYNC);
            if( h==null ) continue;

            // Dans le cas o� la taille de la tuile annonc�e ou suppos�e est fausse
            // on recommence en indiquant explicitement cette taille
            if( first && infoTileWidth==-1 && h.width!=CDSHealpix.pow2(tileOrder) ) {
               if( aladin.levelTrace>=3) System.err.println("Warning, the HiPS \""+p.label+"\" has missing or erroneous tileWidth propertie value => assuming "+h.width);
               addMocFromPlanBG(p1,order, h.width, threshold );
               return;
            }
            first=false;
            
            // Dans la deuxi�me �tape, si le max de la tuile est inf�rieur � la plus petite
            // valeur de queue, on peut tout de suite �carter cette tuile
            if( step==1 && queue.size()>0 ) {
               PixCum last = queue.last();
//               if( last.val>(tile.max/somme) ) {
               if( last.val*somme>tile.max ) {
//                  System.out.println(" tilemax="+(tile.max/somme)+" less that the min of queue="+last.val+" => rejected");
                  continue;
               }
            }

            long min = virtualTileWidth *virtualTileWidth * tileNpix;
            try {
               
               // Parcours des blocs
               for( int yCell=0; yCell<nbCell; yCell++ ) {
                  for( int xCell=0; xCell<nbCell; xCell++ ) {

                     // Parcours des pixels du bloc courant et d�termination de la moyenne de ses pixels
                     double pixel = 0;
                     int nbPixels=0;
                     for( int y=0; y<cellWidth; y++ ) {
                        for( int x=0; x<cellWidth; x++ ) {
                           int idx = ((yCell*cellWidth)+y) * h.width + (xCell*cellWidth)+x;
                           double localPixel = h.getPixel(idx,HealpixKey.NOW);

                           // Pixel vide
                           if( Double.isNaN( localPixel ) || isBlank && localPixel==blank ) continue;

                           localPixel = localPixel  * p.bScale+ p.bZero;

                           pixel += localPixel;
                           nbPixels++;
                        }
                     }
                     if( nbPixels==0 ) continue; // aucune valeur dans ce bloc
//                     pixel /= nbPixels;        // moyenne de la valeur des pixels du bloc courant
                     
                     // Etape 1: mesures statistiques par tuile et globales
                     if( step==0 ) {
                        somme += pixel;
                        tileTotal += pixel;
                        if( tileNbPixel==0 || pixel>tileMax ) tileMax = pixel;
                        tileNbPixel ++;
                        
                     // Etape 2: valeur ramen�e entre 0 et 1, et m�morisation �ventuelle
                     // dans la file des candidats
                     } else  {
                        pixel /= somme;
                        long npixMoc = min + xy2hpx(yCell*virtualTileWidth + xCell);
                        queue.add( new PixCum(npixMoc,pixel) );
                     }
                  }
               }

            } catch( Exception e ) {
               e.printStackTrace();
            }
            
            // Etape 1: la tuile sera-t-elle trait�e � l'�tape 2 ? si oui, m�morisation de la moyenne
            // de ses valeurs
            if( step==0 && tileNbPixel>0 ) {
               tile.npix = tileNpix;
//               tile.val  = tileTotal / tileNbPixel;
               tile.val  = tileTotal;
               tile.max  = tileMax;
            }
         }
      }

      ((SMoc)moc).bufferOn();
      somme=0;
      try {
         // Remplissage du Moc
         Iterator<PixCum> it = queue.iterator();
         while( it.hasNext() ) {
            PixCum pc = it.next();
            long npix = pc.npix;
            somme += pc.val;
            if( somme>threshold ) break;
            ((SMoc)moc).add(order,npix);
         }
         ((SMoc)moc).bufferOff();

         // Conversion en ICRS si n�cessaire
         moc=toReferenceFrame("C");
         frameOrigin=Localisation.ICRS;
         
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   public class Queue<E> extends TreeSet<PixCum> {

      private double threshold;
      private double somme;

      public Queue(double threshold) {
          super();
          this.threshold = threshold;
      }

      public boolean add(PixCum e) {
         if( somme<=0 || somme+e.val<threshold ) {
            somme += e.val;
            super.add(e);
            return true;

         } else {
            double tot=0L;
            while( true ) {
               PixCum last = last();
               if( last==null ) break;
               tot += last.val;
               somme -= last.val;
               if( tot<e.val ) pollLast();
               else break;
            }
            if( tot!=0 ) {
               somme += e.val;
               super.add(e);
               return true;
            }
         }
         return false;
      }
      
      public String toString() {
         PixCum e = first(), f = last();
         return size()+"/"+threshold+"/"+somme+"["+e+".."+f+"]";
      }
   }

   // Pour g�rer un pixel Healpix
   class PixCum implements Comparable {
      long npix;   // indice dans la map
      double val;  // valeur du pixel
      double max;
      
      PixCum(long npix,double val) {
         this.npix=npix;
         this.val=val;
      }

      @Override
      public int compareTo(Object o) {
         if( val == ((PixCum)o).val ) {
            return npix == ((PixCum)o).npix ? 0 : npix < ((PixCum)o).npix ? 1 : -1;
         }
         return val < ((PixCum)o).val ? 1 : -1;
      }
      
      public String toString() { return npix+"/"+val; }
   }
   

   // ANCIENNE METHODE -> A JETER
//   private void addMocFromPlanBG(Plan p1, double threshold) throws Exception {
//      PlanBG p = (PlanBG)p1;
//      
//      order=p.getMaxHealpixOrder();
//      
//      // D�termination de l'ordre pixel (o1) et tuiles (fileOrder)
//      int o1 = p.getTileOrder();
//      int z = (int)p.getZ();
//      
//      int divOrder=0;
//      int fileOrder = order - o1;
//      
//      // L'ordre des tuiles ne peut �tre inf�rieur � 3
//      if( fileOrder<3 ) {
//         divOrder=(3-fileOrder)*2;
//         fileOrder=3;
//      }
//      
//      /// L'ordre des tuiles ne peut entrainer le d�passement de la r�solution
//      // de la map HEALPix
//      if( fileOrder>p.getMaxFileOrder() ) {
//         fileOrder=p.getMaxFileOrder();
//         order = fileOrder+o1;
//      }
//      
//      moc.setMocOrder(order);
//      
//      // On g�n�re d'abord un MOC dans le syst�me de r�f�rence de la map HEALPix
//      // on fera la conversion en ICRS � la fin du processus
//      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
//                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
//      frameOrigin = p.frameOrigin;
//      
//      // Nombre de losanges � traiter
//      int n = (int)CDSHealpix.pow2(fileOrder); 
//      n=12*n*n;
//      
////      System.out.println("Nombre de losanges � traiter : "+n);
//      
//      // Sans doute inutile car d�j� fait
//      try { p.createHealpixOrder(o1); } catch( Exception e1 ) { e1.printStackTrace(); }
//      long nsize = CDSHealpix.pow2(o1);
//      
//      double incrPourcent = gapPourcent/n;
//      
//      // Principe de l'algo: on parcours la map, pixel apr�s pixel qu'on ajoute
//      // � la liste cumul en la triant imm�diatement en ordre d�croissant, 
//      // tout en calculant la somme.
//      //D�s qu'on d�passe le threshold, l'insertion sera conditionn�e au fait qu'il faut
//      // que le nouveau pixel soit plus petit que la derni�re valeur de la liste 
//      // Et si oui, on va l'ins�rer, mais virer autant des pixels les plus petits que n�cessaire
//      ArrayList<PixCum> list;
//      try {
//         list = new ArrayList<>((int)(n*nsize*nsize));
//      } catch( Exception e1 ) {
//         throw new Exception("Sorry! too large probability sky map !");
//      }
//      double somme=0;
//      
//      for( int npixFile=0; npixFile<n; npixFile++ ) {
//         pourcent += incrPourcent;
//         HealpixKey h = p.getHealpixLowLevel(fileOrder,npixFile,z,HealpixKey.SYNC);
//         if( h==null ) continue;
//         
//         long min = nsize * nsize * npixFile;
//         try {
//            for( int y=0; y<h.height; y++ ) {
//               for( int x=0; x<h.width; x++ ) {
//                  try {
//                     int idx = y * h.width + x;
//                     double pixel = h.getPixel(idx,HealpixKey.NOW);
//                     
//                     // Pixel vide
//                     if( Double.isNaN( pixel ) || pixel==blank ) continue;
//                     
//                     long npix = min + p.xy2hpx(idx);
//                     list.add( new PixCum(npix,pixel) );
//                     somme += pixel;
//                     
//                  } catch( Exception e ) {
//                     e.printStackTrace();
//                  }
//               }
//            }
//         } catch( Exception e ) {
//            e.printStackTrace();
//         }
//      }
//      
//      Collections.sort(list);
//      
//      // Normalisation �ventuelle
//      if( Math.abs(1-somme)>1e-8 ) {
//         for( PixCum pc : list ) pc.val/=somme;
//      }
//      
//      somme=0;
//      try {
//         // Remplissage du Moc
//         moc.setCheckConsistencyFlag(false);
//         int nb=0;
//         for( PixCum pc : list ) {
//            long npix = pc.npix;
//            somme += pc.val;
//            if( somme>threshold ) break;
//            moc.add(order,npix>>>divOrder);
//            nb++;
//            if( nb>100000 ) { moc.checkAndFix(); nb=0; }
//         }
//
//         // Conversion en ICRS si n�cessaire
//         moc.setCheckConsistencyFlag(true);
//         moc=toReferenceFrame("C");
//         frameOrigin=Localisation.ICRS;
//         
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }
   
//   /** Creation d'un plan Moc � partir d'un HiPS en prenant tous les pixels dont les
//    * valeurs sont comprises dans un interval (bornes incluses)
//    */
//   private void addMocFromPlanBG(Plan p1, double min, double max) throws Exception {
//      PlanBG p = (PlanBG)p1;
//      
//      order=p.getMaxHealpixOrder();
//      
//      // D�termination de l'ordre pixel (o1) et tuiles (fileOrder)
//      int o1 = p.getTileOrder();
//      int z = (int)p.getZ();
//      
//      int divOrder=0;
//      int fileOrder = order - o1;
//      
//      // L'ordre des tuiles ne peut �tre inf�rieur � 3
//      if( fileOrder<3 ) {
//         divOrder=(3-fileOrder)*2;
//         fileOrder=3;
//      }
//      
//      /// L'ordre des tuiles ne peut entrainer le d�passement de la r�solution
//      // de la map HEALPix
//      if( fileOrder>p.getMaxFileOrder() ) {
//         fileOrder=p.getMaxFileOrder();
//         order = fileOrder+o1;
//      }
//      
//      moc.setMaxLimitOrder(order);
//      
//      // On g�n�re d'abord un MOC dans le syst�me de r�f�rence de la map HEALPix
//      // on fera la conversion en ICRS � la fin du processus
//      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
//                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
//      frameOrigin = p.frameOrigin;
//      
//      // Nombre de losanges � traiter
//      int n = (int)CDSHealpix.pow2(fileOrder); 
//      n=12*n*n;
//      
////      System.out.println("Nombre de losanges � traiter : "+n);
//      
//      // Sans doute inutile car d�j� fait
//      try { p.createHealpixOrder(o1); } catch( Exception e1 ) { e1.printStackTrace(); }
//      long nsize = CDSHealpix.pow2(o1);
//      
//      double incrPourcent = gapPourcent/n;
//      
//      // Principe de l'algo: on parcours la map, pixel apr�s pixel qu'on ajoute
//      // et on les ajoute au fur et � mesure au MOC en construction
//      for( int npixFile=0; npixFile<n; npixFile++ ) {
//         pourcent += incrPourcent;
//         HealpixKey h = p.getHealpixLowLevel(fileOrder,npixFile,z,HealpixKey.SYNC);
//         if( h==null ) continue;
//         
//         long minTile = nsize * nsize * npixFile;
//         int nb=0;
//         moc.setCheckConsistencyFlag(false);
//         try {
//            for( int y=0; y<h.height; y++ ) {
//               for( int x=0; x<h.width; x++ ) {
//                  try {
//                     int idx = y * h.width + x;
//                     double pixel = h.getPixel(idx,HealpixKey.NOW);
//                     
//                     // Pixel vide
//                     if( Double.isNaN( pixel ) || pixel==blank ) continue;
//                     
//                     // Pixel hors interval
//                     if( !Double.isNaN(min) && pixel<min ) continue;
//                     if( !Double.isNaN(max) && pixel>max ) continue;
//                     
//                     long npix = minTile + p.xy2hpx(idx);
//                     moc.add(order,npix>>>divOrder);
//                     nb++;
//                     if( nb>10000 ) { moc.checkAndFix(); nb=0; }
//                     
//                  } catch( Exception e ) {
//                     e.printStackTrace();
//                  }
//               }
//            }
//         } catch( Exception e ) {
//            e.printStackTrace();
//         }
//      }
//      
//      // Conversion en ICRS si n�cessaire
//      if( frameOrigin!=Localisation.ICRS ) {
//         moc.setCheckConsistencyFlag(true);
//         moc=toReferenceFrame("C");
//         frameOrigin=Localisation.ICRS;
//      }
//   }
   
   protected boolean waitForPlan() {

      // Pour des benchs
//      for( int order=6; order<=20; order+=2 ) {
//         long t0 = System.currentTimeMillis();

      try {
         moc = new SMoc();
         ((SMoc)moc).setMinOrder(3);
         if( order!=-1) ((SMoc)moc).setMocOrder(order);
         frameOrigin=Localisation.ICRS;
         for( Plan p1 : p ) {
            if( p1.isCatalog() )    addMocFromCatalog(p1,radius,order,fov);
            else if( p1.isImage() ) addMocFromImage(p1,pixMin,pixMax);
            else if( p1 instanceof PlanBG && !Double.isNaN(threshold) ) addMocFromPlanBG(p1,order,threshold);
            else if( p1 instanceof PlanBG ) addMocFromPlanBG(p1,order,pixMin,pixMax);
         }
      } catch( Exception e ) {
         error=e.getMessage();
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         flagProcessing=false;
         return false;
      }


//         long t1 = System.currentTimeMillis();
//         try {
//            File f = File.createTempFile("toto", "titi");
//            FileOutputStream fo = new FileOutputStream(f);
//            moc.writeFITS(fo);
//            fo.close();
//            long sizeFits = f.length();
//            Aladin.trace(3,"SMOC 'order="+((SMoc)moc).getMocOrder()
//                  +" built in "+(t1-t0)+"ms"
//                  +" nbRanges="+moc.getNbRanges()
//                  +" RAM="+Util.getUnitDisk( moc.getMem())
//                  +" FITS="+Util.getUnitDisk(sizeFits )
//                  );
//         } catch( Exception e ) {}
//
//      }

     flagProcessing=false;
      if( moc.isEmpty() ) error="Empty MOC";
      flagOk=true;
      return true;
   }



}


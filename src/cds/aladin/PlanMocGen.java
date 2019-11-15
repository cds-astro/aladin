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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import cds.aladin.stc.STCObj;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.SpaceMoc;
import cds.tools.pixtools.CDSHealpix;

/** Generation d'un plan MOC à partir d'une liste de plans (Image, Catalogue ou map HEALPix) 
 * @author P.Fernique [CDS]
 * @version 1.2 - mar 2019 - correction HipstoMoc + correction
 * @version 1.1 - mar 2016 - ajout probability sky map
 * @version 1.0 - nov 2012
 */
public class PlanMocGen extends PlanMoc {
   
   private Plan [] p;       // Liste des plans à ajouter dans le MOC
   private double radius;   // Pour un plan catalogue, rayon autour de chaque source (en degres), sinon 0
   private boolean fov;     // Plan un plan catalogue, true si on prend les FOVs associés
   private double pixMin;   // Pour un plan Image ou map Healpix, valeur minimale des pixels retenus (sinon NaN)
   private double pixMax;   // Pour un plan Image ou map Healpix, valeur maximale des pixels retenus (sinon NaN)
   private double threshold;// Pour un plan Image Healpix, seuil max de l'intégration (sinon NaN)
   private int order;         // Résolution (ordre) demandée
   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est terminé)
   
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
   
   protected void suite1() {}
   
   private void addMocFromCatFov(Plan p1,int order) {
      Iterator<Obj> it = p1.iterator();
      int m= p1.getCounts();
      int n=0;
      double incrPourcent = gapPourcent/m;
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         if( m<100 || n%100==0 ) pourcent+=incrPourcent;
         Source s = (Source)o;
         SourceFootprint sf = s.getFootprint();
         if( sf==null ) continue;
         List<STCObj> listStcs = sf.getStcObjects();
         if( listStcs==null || listStcs.size()==0 ) continue;
         try {
            HealpixMoc m1 = aladin.createMocRegion(listStcs,order);
            if( m1!=null ) moc.add(m1);
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
   }
      
   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double radius,int order) {
      Iterator<Obj> it = p1.iterator();
      Healpix hpx = new Healpix();
      int o1 = order;
      Coord coo = new Coord();
      int n=0;
      int m= p1.getCounts();
      double incrPourcent = gapPourcent/m;
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         if( m<100 || n%100==0 ) pourcent+=incrPourcent;
         try {
            coo.al = ((Position)o).raj;
            coo.del = ((Position)o).dej;
            long [] npix ;
            if( radius==0 ) npix = new long[] { hpx.ang2pix(o1, coo.al, coo.del) };
            else npix = hpx.queryDisc(o1, coo.al, coo.del, radius);
            
            for( long pix : npix ) moc.add(o1,pix);
            n+=npix.length;
            if( n>10000 ) { ((SpaceMoc)moc).checkAndFix(); n=0; }
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
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
         e1.printStackTrace();
      }
//      if( gap<1 || Double.isNaN(gap) ) gap=1;
//      
      gapD = CDSHealpix.pixRes( o1 )/3600.;
//      System.out.println("res="+res+" order="+order+" gapA ="+Coord.getUnit(gapA)+" gapD ="+Coord.getUnit(gapD)+" gap="+gap);
      
      // Pour garder en mémoire les pixels 
      pimg.setLockCacheFree(true);
      pimg.pixelsOriginFromCache();

      double incrPourcent = gapPourcent/pimg.naxis2;
      long oNpix=-1;  
      for( double y=0; y<pimg.naxis2; y+=gap ) {
         pourcent += incrPourcent;
         for( double x=0; x<pimg.naxis1; x+=gap ) {
            try {
               coo.x = x;
               coo.y = (pimg.naxis2-y-1);
               
               // dans du vide - on test d'abord le buffers 8bits, et on vérifie si on tombe sur 0
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

               // Juste pour éviter d'insérer 2x de suite le même npix
               if( npix==oNpix ) continue;
               
               moc.add(o1,npix);
               oNpix=npix;
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
      }
      
      // Les pixels peuvent désormais être libérés
      pimg.setLockCacheFree(false);
   }
   
   // Ajout d'un plan map Healpix au MOC en cours de construction
   private void addMocFromPlanBG(Plan p1,int order, double pixMin,double pixMax) throws Exception { addMocFromPlanBG(p1,order,-1,pixMin,pixMax); }
   
   private void addMocFromPlanBG(Plan p1,int order, int infoTileWidth, double pixMin,double pixMax) throws Exception {
      boolean flagRange = !Double.isNaN(pixMin) || !Double.isNaN(pixMax);
      PlanBG p = (PlanBG)p1;
      
      // Passage en mode FITS
      if( !p.hasOriginalPixels() ) p.switchFormat();
      
      // Recherche de la taille d'une tuile si non explicitement mentionnée
      int tileOrder = infoTileWidth==-1 ? p.getTileOrder() : (int) CDSHealpix.log2(infoTileWidth);
      int tileWidth = (int)CDSHealpix.pow2(tileOrder);
      int z = (int)p.getZ();
      
      if( order> tileOrder+ p.maxOrder ) {
         order = tileOrder+p.maxOrder;
         aladin.warning("MOC order greater than HiPS resolution. Assuming MOC order "+order);
      }

      
      // Quel est l'ordre des tuiles requis
      int requiredHipsOrder = order-tileOrder;
      
      // Par défaut on suppose que cet order Hips est disponible
      int hipsOrder = requiredHipsOrder;
      
      // Par défaut on considère qu'un pixel de tuile correspond à un pixel du MOC
      int cellOrder = 0;
      
      // Cet ordre HiPS est-il disponible ? et sinon quel est le niveau sup le plus proche
      // Cela impactera le nombre de pixels à moyenner dans la tuile de départ pour un pixel du MOC : 2^cellOrder = taille de ce bloc
      int minHipsOrder = p.getMinOrder();
      if( minHipsOrder>requiredHipsOrder ) {
         cellOrder = minHipsOrder - requiredHipsOrder;
         hipsOrder=minHipsOrder;
      }
      
      // On génère d'abord un MOC dans le système de référence de la map HEALPix
      // on fera la conversion en ICRS à la fin du processus
      moc.setMocOrder(order);
      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
      frameOrigin = p.frameOrigin;
      try { moc.setCheckConsistencyFlag(false); } catch(Exception e) {}
      
      // Nombre de losanges à traiter
      int n = (int)CDSHealpix.pow2(hipsOrder); 
      n=12*n*n;
      
//      System.out.println("Nombre de losanges à traiter : "+n);
      
      try { createHealpixOrder(tileOrder-cellOrder); }
      catch( Exception e1 ) { }
      
      double incrPourcent = gapPourcent/n;
      boolean first=true;
      
      int cellWidth = (int)CDSHealpix.pow2(cellOrder);   // taille d'un bloc (coté)
      int nbCell = tileWidth/cellWidth;                  // Nombre de blocs à considérer dans une ligne de la tuile
      int virtualTileWidth = tileWidth/cellWidth;
      
      for( int npixFile=0; npixFile<n; npixFile++ ) {
//         System.out.println("Traitement de "+npixFile);
         pourcent += incrPourcent;
         if( p.moc!=null && p.isOutMoc(hipsOrder, npixFile) ) continue;
         
         HealpixKey h = p.getHealpixLowLevel(hipsOrder,npixFile,z,HealpixKey.SYNC);
         if( h==null ) continue;
         
         // Dans le cas où la taille de la tuile annoncée ou supposée est fausse
         // on recommence en indiquant explicitement cette taille
         if( first && infoTileWidth==-1 && h.width!=CDSHealpix.pow2(tileOrder) ) {
            if( aladin.levelTrace>=3) System.err.println("Warning, the HiPS \""+p.label+"\" has missing or erroneous tileWidth propertie value => assuming "+h.width);
            addMocFromPlanBG(p1,order,h.width,pixMin,pixMax);
            return;
         }
         first=false;
         
         long min = virtualTileWidth *virtualTileWidth * npixFile;
         try {
            int nb=0;
            
            // Parcours des blocs
            for( int yCell=0; yCell<nbCell; yCell++ ) {
               for( int xCell=0; xCell<nbCell; xCell++ ) {
                  
                  // Parcours des pixels du bloc courant et détermination de la moyenne de ses pixels
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
                  moc.add(order,npixMoc);
                  nb++;
              }
            }
            
            // On remet immédiatement au propre le MOC uniquement si on a inséré
            // au-moins 100000 cellules (histoire de ne pas exploser la mémoire)
            if( nb>100000 ) ((SpaceMoc)moc).checkAndFix();
            
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }
      
      // Conversion en ICRS si nécessaire
      if( frameOrigin!=Localisation.ICRS ) {
         try {
            moc.setCheckConsistencyFlag(true);
            moc=toReferenceFrame("C");
            frameOrigin=Localisation.ICRS;
         } catch( Exception e ) { e.printStackTrace(); }
      }
   }

   
   /** Creation d'un plan Moc à partir d'un HiPS en prenant toutes les pixels qui représentent
    * threshold (sommation) de la totalité des pixels. On commence par les valeurs les plus grandes.
    * Permet par exemple de créer un MOC à 10% (threshold=0.1) pour des maps de probabilité
    * issues de l'étude des ondes gravitationnelles.
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
      
      // Recherche de la taille d'une tuile si non explicitement mentionnée
      int tileOrder = infoTileWidth==-1 ? p.getTileOrder() : (int) CDSHealpix.log2(infoTileWidth);
      int tileWidth = (int)CDSHealpix.pow2(tileOrder);
      int z = (int)p.getZ();
      
      if( order> tileOrder+ p.maxOrder ) {
         order = tileOrder+p.maxOrder;
         aladin.warning("MOC order greater than HiPS resolution. Assuming MOC order "+order);
      }
      
      // Quel est l'ordre des tuiles requis
      int requiredHipsOrder = order-tileOrder;
      
      // Par défaut on suppose que cet order Hips est disponible
      int hipsOrder = requiredHipsOrder;
      
      // Par défaut on considère qu'un pixel de tuile correspond à un pixel du MOC
      int cellOrder = 0;
      
      // Cet ordre HiPS est-il disponible ? et sinon quel est le niveau sup le plus proche
      // Cela impactera le nombre de pixels à moyenner dans la tuile de départ pour un pixel du MOC : 2^cellOrder = taille de ce bloc
      int minHipsOrder = p.getMinOrder();
      if( minHipsOrder>requiredHipsOrder ) {
         cellOrder = minHipsOrder - requiredHipsOrder;
         hipsOrder=minHipsOrder;
      }
      
      // On génère d'abord un MOC dans le système de référence de la map HEALPix
      // on fera la conversion en ICRS à la fin du processus
      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
      frameOrigin = p.frameOrigin;
      try { moc.setCheckConsistencyFlag(false); } catch(Exception e) {}
      
      // Nombre de losanges à traiter
      int n = (int)CDSHealpix.pow2(hipsOrder); 
      n=12*n*n;
      
      System.out.println("Nombre de losanges à traiter (order="+hipsOrder+") : "+n);
      
      try { createHealpixOrder(tileOrder-cellOrder); }
      catch( Exception e1 ) { }
      
      double incrPourcent = gapPourcent/n;
      boolean first=true;
      
      int cellWidth = (int)CDSHealpix.pow2(cellOrder);   // taille d'un bloc (coté)
      int nbCell = tileWidth/cellWidth;                  // Nombre de blocs à considérer dans une ligne de la tuile
      int virtualTileWidth = tileWidth/cellWidth;
      
      // Liste des numéros de tuiles à traiter. Dans la première étape on se contentera
      // de calculer la moyenne de l'ensemble des pixels concernées dans chaque tuile.
      // Puis avant la deuxième étape, on triera ces numéros de tuiles en commençant par celles
      // qui ont la moyenne la plus élevée (le but est de commencer par les valeurs
      // les plus grandes pour éviter de saturer "queue" (voir ci-dessous)
      ArrayList<PixCum> tileList = new ArrayList<>(n);
      
      // La queue va servir dans la deuxième étape.
      // On ne va garder que les pixels qui ont les plus grandes valeurs, et à chaque fois qu'on devra en insérer un nouveau, on virera
      // autant de pixels de plus petites valeurs dont la somme est égale à la nouvelle valeur inséréé
      Queue<PixCum> queue = new Queue<>( threshold );

      // Somme totale déterminée à la première étape, et qui permettra de normaliser les valeurs
      // entre 0 et 1 à la deuxième étape.
      double somme=0;

      for( int step=0; step<2; step++ ) {
         
         // 1ère étape, Initilisation de la liste des losanges =>  suit l'ordre naturel
         if( step==0 ) {
            for( int i=0; i<n; i++ ) tileList.add( new PixCum(i,0) );
            
         // 2ème étape: Tri de la liste des losanges => suit l'odre décroissant des moyennes
         } else  Collections.sort(tileList);
         
         // Parcours des tuiles
         for( PixCum tile : tileList ) {
            if( tile.npix==-1 ) continue;   // c'est une tuile écartée à l'étape 1
            int tileNpix = (int) tile.npix;
            tile.npix = -1;   // par défaut, la tuile sera supposée rejetée;
            
            aladin.trace(4,"Processing tile "+tileNpix+" (step "+(step+1)+
                  (step==1 ? " mean="+tile.val+" max="+tile.max+" queue.size="+queue.size() : "")+")");
            
            double tileTotal = 0;  // Cumul des pixels de la tuile courante
            double tileMax = 0;    // Valeur max sur les pixels de la tuile courante
            int tileNbPixel = 0;   // Nomre de pixels de la tuile courante
         
            pourcent += incrPourcent/2;
            if( p.moc!=null && p.isOutMoc(hipsOrder, tileNpix) ) continue;

            HealpixKey h = p.getHealpixLowLevel(hipsOrder,tileNpix,z,HealpixKey.SYNC);
            if( h==null ) continue;

            // Dans le cas où la taille de la tuile annoncée ou supposée est fausse
            // on recommence en indiquant explicitement cette taille
            if( first && infoTileWidth==-1 && h.width!=CDSHealpix.pow2(tileOrder) ) {
               if( aladin.levelTrace>=3) System.err.println("Warning, the HiPS \""+p.label+"\" has missing or erroneous tileWidth propertie value => assuming "+h.width);
               addMocFromPlanBG(p1,order, h.width, threshold );
               return;
            }
            first=false;
            
            // Dans la deuxième étape, si le max de la tuile est inférieur à la plus petite
            // valeur de queue, on peut tout de suite écarter cette tuile
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

                     // Parcours des pixels du bloc courant et détermination de la moyenne de ses pixels
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
                        
                     // Etape 2: valeur ramenée entre 0 et 1, et mémorisation éventuelle
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
            
            // Etape 1: la tuile sera-t-elle traitée à l'étape 2 ? si oui, mémorisation de la moyenne
            // de ses valeurs
            if( step==0 && tileNbPixel>0 ) {
               tile.npix = tileNpix;
//               tile.val  = tileTotal / tileNbPixel;
               tile.val  = tileTotal;
               tile.max  = tileMax;
            }
         }
      }

      somme=0;
      try {
         // Remplissage du Moc
         moc.setCheckConsistencyFlag(false);
         int nb=0;
         Iterator<PixCum> it = queue.iterator();
         while( it.hasNext() ) {
            PixCum pc = it.next();
            long npix = pc.npix;
            somme += pc.val;
            if( somme>threshold ) break;
            moc.add(order,npix);
            nb++;
            if( nb>100000 ) { ((SpaceMoc)moc).checkAndFix(); nb=0; }
         }

         // Conversion en ICRS si nécessaire
         moc.setCheckConsistencyFlag(true);
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

   // Pour gérer un pixel Healpix
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
//      // Détermination de l'ordre pixel (o1) et tuiles (fileOrder)
//      int o1 = p.getTileOrder();
//      int z = (int)p.getZ();
//      
//      int divOrder=0;
//      int fileOrder = order - o1;
//      
//      // L'ordre des tuiles ne peut être inférieur à 3
//      if( fileOrder<3 ) {
//         divOrder=(3-fileOrder)*2;
//         fileOrder=3;
//      }
//      
//      /// L'ordre des tuiles ne peut entrainer le dépassement de la résolution
//      // de la map HEALPix
//      if( fileOrder>p.getMaxFileOrder() ) {
//         fileOrder=p.getMaxFileOrder();
//         order = fileOrder+o1;
//      }
//      
//      moc.setMocOrder(order);
//      
//      // On génère d'abord un MOC dans le système de référence de la map HEALPix
//      // on fera la conversion en ICRS à la fin du processus
//      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
//                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
//      frameOrigin = p.frameOrigin;
//      
//      // Nombre de losanges à traiter
//      int n = (int)CDSHealpix.pow2(fileOrder); 
//      n=12*n*n;
//      
////      System.out.println("Nombre de losanges à traiter : "+n);
//      
//      // Sans doute inutile car déjà fait
//      try { p.createHealpixOrder(o1); } catch( Exception e1 ) { e1.printStackTrace(); }
//      long nsize = CDSHealpix.pow2(o1);
//      
//      double incrPourcent = gapPourcent/n;
//      
//      // Principe de l'algo: on parcours la map, pixel après pixel qu'on ajoute
//      // à la liste cumul en la triant immédiatement en ordre décroissant, 
//      // tout en calculant la somme.
//      //Dès qu'on dépasse le threshold, l'insertion sera conditionnée au fait qu'il faut
//      // que le nouveau pixel soit plus petit que la dernière valeur de la liste 
//      // Et si oui, on va l'insérer, mais virer autant des pixels les plus petits que nécessaire
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
//      // Normalisation éventuelle
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
//         // Conversion en ICRS si nécessaire
//         moc.setCheckConsistencyFlag(true);
//         moc=toReferenceFrame("C");
//         frameOrigin=Localisation.ICRS;
//         
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }
   
//   /** Creation d'un plan Moc à partir d'un HiPS en prenant tous les pixels dont les
//    * valeurs sont comprises dans un interval (bornes incluses)
//    */
//   private void addMocFromPlanBG(Plan p1, double min, double max) throws Exception {
//      PlanBG p = (PlanBG)p1;
//      
//      order=p.getMaxHealpixOrder();
//      
//      // Détermination de l'ordre pixel (o1) et tuiles (fileOrder)
//      int o1 = p.getTileOrder();
//      int z = (int)p.getZ();
//      
//      int divOrder=0;
//      int fileOrder = order - o1;
//      
//      // L'ordre des tuiles ne peut être inférieur à 3
//      if( fileOrder<3 ) {
//         divOrder=(3-fileOrder)*2;
//         fileOrder=3;
//      }
//      
//      /// L'ordre des tuiles ne peut entrainer le dépassement de la résolution
//      // de la map HEALPix
//      if( fileOrder>p.getMaxFileOrder() ) {
//         fileOrder=p.getMaxFileOrder();
//         order = fileOrder+o1;
//      }
//      
//      moc.setMaxLimitOrder(order);
//      
//      // On génère d'abord un MOC dans le système de référence de la map HEALPix
//      // on fera la conversion en ICRS à la fin du processus
//      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
//                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
//      frameOrigin = p.frameOrigin;
//      
//      // Nombre de losanges à traiter
//      int n = (int)CDSHealpix.pow2(fileOrder); 
//      n=12*n*n;
//      
////      System.out.println("Nombre de losanges à traiter : "+n);
//      
//      // Sans doute inutile car déjà fait
//      try { p.createHealpixOrder(o1); } catch( Exception e1 ) { e1.printStackTrace(); }
//      long nsize = CDSHealpix.pow2(o1);
//      
//      double incrPourcent = gapPourcent/n;
//      
//      // Principe de l'algo: on parcours la map, pixel après pixel qu'on ajoute
//      // et on les ajoute au fur et à mesure au MOC en construction
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
//      // Conversion en ICRS si nécessaire
//      if( frameOrigin!=Localisation.ICRS ) {
//         moc.setCheckConsistencyFlag(true);
//         moc=toReferenceFrame("C");
//         frameOrigin=Localisation.ICRS;
//      }
//   }
   
   protected boolean waitForPlan() {
      try {
         moc = new HealpixMoc();
         ((SpaceMoc)moc).setMinLimitOrder(3);
         if( order!=-1) ((SpaceMoc)moc).setMaxLimitOrder(order);
         moc.setCoordSys("C");
         frameOrigin=Localisation.ICRS;
         moc.setCheckConsistencyFlag(false);
         for( Plan p1 : p ) {
            if( p1.isCatalog() ) {
               if( fov ) addMocFromCatFov(p1,order);
               else addMocFromCatalog(p1,radius,order);
            }
            else if( p1.isImage() ) addMocFromImage(p1,pixMin,pixMax);
            else if( p1 instanceof PlanBG && !Double.isNaN(threshold) ) addMocFromPlanBG(p1,order,threshold);
            else if( p1 instanceof PlanBG ) addMocFromPlanBG(p1,order,pixMin,pixMax);
         }
         moc.setCheckConsistencyFlag(true);
      } catch( Exception e ) {
         error=e.getMessage();
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         flagProcessing=false;
         return false;
      }
      flagProcessing=false;
      if( moc.getSize()==0 ) error="Empty MOC";
      flagOk=true;
      return true;
   }
   

      
}


// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import cds.tools.Util;

/**
 * Gestion d'un plan image Mosaic
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : octobre 2005 creation
 */
public class PlanImageMosaic extends PlanImage {
   
   protected PlanImage Ref;
   private PlanImage tmpP[];    // Subtilité de programmation pour traiter la création
                                // au même titre qu'un ajout
   private boolean flagCreat;   // True si on est entrain de créer le plan, false   private PlanImage tmpP[];
   private boolean firstMosaic; // true si this n'est pas encore une mosaic (première création)
   private ViewSimple v;
   
   protected PlanImageMosaic(Aladin aladin,PlanImage p) { 
      super(aladin,p);
      type=IMAGEMOSAIC;
   }
   
   // Pour pouvoir recharger du AJ
   protected PlanImageMosaic(Aladin aladin) { 
      super(aladin);
      type=IMAGEMOSAIC;
   }
   

   /**
    * Création d'un plan mosaique à partir d'une liste d'images
    * @param aladin
    * @param p la liste des plan images, le premier plan est la référence astrométrique
    * @param label
    * @param v la vue de destination, ou null si nouvelle vue à créer
    */
   protected PlanImageMosaic(Aladin aladin,PlanImage p[],String label,ViewSimple v) {
      super(aladin);
      type=IMAGEMOSAIC;
      Ref=p[0];
      firstMosaic=true;
      this.v = v;
      
      Aladin.trace(3,"Mosaic ref plane: " + Ref.label);

      // Initialisation des parametres communs
      init(label,Ref);

      // Mémorisation des plans à traiter (sauf le premier qui est
      // celui de référence
      tmpP=new PlanImage[p.length - 1];
      System.arraycopy(p,1,tmpP,0,p.length - 1);

      StringBuffer s = new StringBuffer();
      for( int i=0; i<p.length; i++ ) s.append((i>0?"/":"")+p[i]);
      sendLog("Mosaic","["+s+"]");

      flagCreat=true;
      synchronized( this ) {
         runme=new Thread(this,"AladinBuildMosaic");
         Util.decreasePriority(Thread.currentThread(), runme);
//         runme.setPriority(Thread.NORM_PRIORITY - 1);
         runme.start();
      }
   }
   
   /** Copie les parametres
    * @param p le plan de reference pour le reechantillonage
    */
    protected void init(String label, PlanImage p){

       p.copy(this);
       flagOk=false;
       isOldPlan=false;
       askActive=true;
       type=IMAGEMOSAIC;
       headerFits=null;
       fmt=IMAGEMOSAIC;
       res=UNDEF;
       orig=COMPUTED;
       status="Mosaic in progress...";
       progress="computing...";
       if( label==null ) label="Msc img";
       setLabel(label);
       copyright="Mosaic";
       param="";
       bitpix=8;
       npix=1;
       
       try {
          RectangleD box = getPixelBox(new PlanImage[]{ p });
          Aladin.trace(3,"Mosaic bounding box pos="+box.x+","+box.y+" size="+box.width+"x"+box.height);       
          projd = shiftProjection(box);
       } catch( Exception e ) { e.printStackTrace(); }

    }

    /** Attente pendant la construction du plan.
     * @return <I>true</I> si ok, <I>false</I> sinon.
     */
    protected boolean waitForPlan() {
       try {
          addFrame(tmpP);
          calculPixelsZoom();
       } catch( Exception e ) {
          e.printStackTrace();
          return false;
       }
       return true;
    }

    /** Lance le chargement du plan */
    public void run() {
       if( flagCreat ) {
          flagCreat=false;
          Aladin.trace(1,"Creating the " + Tp[type] + " plane " + label);
          if( v==null )  planReady(waitForPlan());
          else {
             flagProcessing=true;
             v.pref=this;
             aladin.view.setSelect(v);
             waitForPlan();
             v.repaint();
          }
       } else {
          Aladin.trace(1,"Adding planes to " + label);
          try { addFrame(tmpP); }
          catch( Exception e ) {
             e.printStackTrace();
             return;
          }
          flagOk=true;
       }
       
    }

    synchronized protected void addPlan(PlanImage p) {
       flagOk=false;
       flagProcessing=true;
       tmpP=new PlanImage[1];
       tmpP[0]=p;

       aladin.calque.select.repaint();

       runme=new Thread(this,"AladinMosaicAdd");
       Util.decreasePriority(Thread.currentThread(), runme);
//       runme.setPriority(Thread.NORM_PRIORITY - 1);
       runme.start();
    }

     /**
      * Détermination des indices minimales pour la construction de l'image
      * mosaique. Je calcule l'indice x,y de tous les points 0,0
      * des images à incorporer dans la projection de l'image de référence
      * et je retourne le x et le y les plus petits trouvés.
      */
     private RectangleD getPixelBox(PlanImage p[]) throws Exception {
        RectangleD box= new RectangleD(0,0,width-1,height-1);
//System.out.println("Box ["+p[0]+"] 0,0 "+(width-1)+"x"+(height-1));
        Coord c = new Coord();
        for( int i=0; i<p.length; i++ ) {
           
           // On teste les 4 coins
           for( int j=0; j<4; j++ ) {
              switch(j) {
                 case 0: c.x=0;            c.y=0;             break;
                 case 1: c.x=0;            c.y=p[i].height-1; break;
                 case 2: c.x=p[i].width-1; c.y=p[i].height-1; break;
                 case 3: c.x=p[i].width-1; c.y=0;             break;
              }
              p[i].projd.getCoord(c);
              if( Double.isNaN(c.al) ) continue;
              projd.getXY(c);
              if( Double.isNaN(c.x) ) continue;
              if( c.x<box.x ) box.x=c.x;
              if( c.x>box.width ) box.width=c.x;
              if( c.y<box.y ) box.y=c.y;
              if( c.y>box.height ) box.height=c.y;
           }
        }
        
        // Je transforme box.W et box.H en vrai largeur et hauteur
        box.width -= box.x;
        box.height -= box.y;
        return box;
     }
     
     private Projection shiftProjection(RectangleD box) {
// TOTALEMENT FAUX SI SYMETRIE RA ACTIVE
//        Coord c;
//        try { c = projd.c.getProjCenter(); }
//        catch( Exception e) { return null; }
//        
//        double r = box.width;
//        double rm = projd.rm * (box.width/projd.r);
//        double r1 = box.height;
//        double rm1= projd.rm1 * (box.height/projd.r1);
//
//        Projection p = new Projection(projd.label,Projection.SIMPLE,
//              c.al,c.del,
//              rm,rm1,
//              c.x-box.x,/* c.y-box.Y */ box.height - (height-c.y-box.y),
//              r,r1,
//              projd.rot,
//              projd.sym,projd.t== -1 ? 1: projd.t);
        
        Projection p = projd.copy();
        p.crop((int)(box.x), (int)(box.y),(int)box.width,(int)box.height);
        return p;
     }
     
     /** Positionnement de la liste des plans d'originie dans param */
     private void setFrom(PlanImage p[]) {
        String s="";
        for( int i=0; i<p.length; i++ ) s = s+" ["+p[i]+"]";
        if( firstMosaic ) copyright = "Mosaic from ["+Ref+"]"+s;
        else copyright += s;        
     }

     synchronized protected void addFrame(PlanImage p[]) throws Exception {

        // Actuellement on ne travaille pas sur les pixels originaux
        // mais uniquement sur les 8 bits
        noOriginalPixels();
        
        Coord coo=new Coord();
        int x,y;
        int w,h;
        int i;

        setFrom(p);
        Aladin.trace(3,"Mosaicing "+param+"...");      
        
        RectangleD box = getPixelBox(p);
Aladin.trace(3,"Mosaic bounding box pos="+box.x+","+box.y+" size="+box.width+"x"+box.height);       
        
        w = (int)(box.width+1);
        h = (int)(box.height+1);
        byte newPixel[] = new byte[w*h];
//System.out.println("Ancienne proj="+projd);        
        Projection newProj = shiftProjection(box);
//System.out.println("Nouvelle proj="+newProj);        
        
        setHasSpecificCalib();
        
        for( i=0; i <newPixel.length; i++ ) {
           coo.x=i % w;
           coo.y=i / w;
           newProj.getCoord(coo);
           if( Double.isNaN(coo.al) ) continue;
           int c=0;
           int j=0;

           for( int n=-1; n < p.length; n++ ) {
              PlanImage p2 = n==-1 ? this : p[n];
              p2.projd.getXY(coo);
              if( !Double.isNaN(coo.x) ) {
                 x=(int) Math.round(coo.x);
                 y=(int) Math.round(coo.y);
                 if( x >= 0 && x < p2.width && y >= 0 && y < p2.height ) {
                    
                    // Si c'est la création d'une mosaic et que la valeur à ajouter
                    // est nulle, je passe
                    if( !firstMosaic && p2==this && p2.getBufPixels8()[y* p2.width + x]==0 ) continue;
                                        
                    c+=0xFF & p2.getBufPixels8()[y* p2.width + x];
                    j++;
                 }
              }
              
              // Pour laisser la main aux autres threads
              // et pouvoir afficher le changement de pourcentage
              if( (i * p.length) % 10000 == 0 ) {
                 setPourcent(i * 100 / newPixel.length);
              }
           }
           if( j!=0 ) newPixel[i]=(byte)(c/j & 0xFF);  
        }
        
        setBufPixels8(newPixel);
        naxis1=width = w;
        naxis2=height = h;
        projd = newProj;
        
        setPourcent(-1);
        flagProcessing=false;
        flagOk=true;
        if( firstMosaic ) this.cm=Ref.cm;
        firstMosaic = false;
        
        Aladin.trace(3,"Mosaic achieved...");
        
        // Personnalisation des parametres
        calculPixelsZoom();
        changeImgID();
        aladin.view.repaintAll();
        aladin.calque.zoom.zoomView.repaint();
    }
}

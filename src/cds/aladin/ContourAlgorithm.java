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


/**
 * Classe abstraite pour les differents algos de contour
 * Ces algos devront heriter de cette classe
 *
 * @author  Thomas Boch [CDS]
 * Juillet 2002 - Correction d'un bug de cleanContours lorsque le parametre est un tableau de 0 element
 */
 
public abstract class ContourAlgorithm {
  
  protected PlanContour pc; 	    // reference au PlanContour appelant 
  protected int width,height;	    // dimensions de l'image
  protected double level;   	    // valeur du niveau des contours a calculer
  protected short[] data;   	    // tableau des pixels de l'image
  

  /** Constructeur de ContourAlgorithm
   *  @param pixels  - tableau des pixels de l'image
   *  @param width  - largeur de l'image
   *  @param height  - hauteur de l'image
   *  @param level  - niveau choisi pour l'isocontour
   */
  ContourAlgorithm(short[] pixels, int width, int height, double level) {
    this();
    this.level = level;
    this.width = width;
    this.height = height;
    this.data = pixels;
  }
  
  ContourAlgorithm() {
    
  }
  
  // retourne un tableau de PointD, les differents contours pour le niveau choisi etant separes par un objet "null"
  abstract protected PointD[] getContours();
  
  /** setLevel
   *  @param level  - niveau du contour a calculer
   */
  protected void setLevel(double level) {
    this.level = level;
  }
  
  /** setData
   *  @param pix  - tableau des pixels de l'image sur laquelle les contours seront calcules
   */
  protected void setData(short[] pix) {
    this.data = pix;
  }
  
  /** setDimensions
   *  @param width  - largeur de l'image
   *  @param height  - hauteur de l'image
   */
  protected void setDimension(int width, int height) {
    this.width = width;
    this.height = height;
  }
  

 
  /** cleanContours  - nettoie les contours, cad supprime les points de controle inutiles
   *  @param cont  - tableau contenant les points du contour a nettoyer
   */
  static protected PointD[] cleanContours(PointD[] tab) {
      	if(tab.length==0) return tab;  
        PointD[] retTab = new PointD[tab.length];
        
        int indexOrg=0;
        int indexRet=0;
        int i1=0;
        int i2=1;
        int i3=2;
        PointD p1;
        boolean fin = false;
          
        while(!fin) {
            
          if (i3>=tab.length) fin=true;
          p1=tab[i1];
            
          retTab[indexRet]=p1;
          indexRet++;
          indexOrg=testPoints(i1,tab);
          retTab[indexRet] = tab[indexOrg];
          indexRet++;
            
          i1=indexOrg+1;
          i2=i1+1;
          i3=i2+1;
            
          if (i3>=tab.length) fin=true;
           
          }
        
        PointD[] tmp = new PointD[indexRet+1];
        System.arraycopy(retTab,0,tmp,0,indexRet+1);
		return tmp;
  } 
    
    /** methode privee appelee par cleanContours */
    private static int testPoints(int i1, PointD[] tab) {
       int i2=i1+1;
       int i3=i2+1;
       PointD p1,p2,p3;
       boolean fin=false;
       int type=-1;
       
       while(!fin) {
          if (i3>=tab.length) {
             fin=true;
             continue;
          }
          
          p1=tab[i1];
          p2=tab[i2];
          p3=tab[i3];
          if(p1==null||p2==null||p3==null) {
             fin=true;
             continue;
          }
          
          if (p1.x == p2.x && p2.x == p3.x && (type==0||type==-1)) {
              type=0;
              i2++;
              i3++;
              continue;
          }
          
          if (p1.y == p2.y && p2.y == p3.y && (type==1||type==-1)) {
              type=1;
              i2++;
              i3++;
              continue;
          }
          
          if( ( (p2.x-p1.x)==(p3.x-p2.x) ) && ( (p2.y-p1.y)==(p3.y-p2.y) ) && (type==2||type==-1))   {
              type=2;
              i2++;
              i3++;
              continue;
          }
          
          fin=true;
       }
       return i2;
    }
  
  
}

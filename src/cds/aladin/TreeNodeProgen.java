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

import java.awt.Color;
import java.awt.Graphics;
import java.io.ObjectInputStream.GetField;
import java.util.List;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCStringParser;

/** Gère les noeuds de l'arbre des catégories (formulaire ServerCategory) */
public class TreeNodeProgen extends TreeNode {
   static private int MAXLIVE=10000;   // Durée de vie en ms lorsque le TreeNodeProgen n'est plus dans la vue (<=0 =>peut être supprimé)
   
   private HealpixIndexItem hii;
   private PlanBG planBG;
   private long lastPaint;

   TreeNodeProgen(PlanBG planBG, HealpixIndexItem hii) {
      super(planBG.aladin,hii.getID(),null,hii.getID(),hii.getID());
      this.hii = hii;
      this.planBG = planBG;
      touch();
   }
   
   /** Redonne de la vie */
   protected void touch() { lastPaint=System.currentTimeMillis(); }
   
   protected int getLive() {
      int live = MAXLIVE - (int)(System.currentTimeMillis() - lastPaint); 
      return live<0 ? 0 : live;
   }
   
   /** Retourne false si le noeud peut être supprimé */
   protected boolean inLive() { return getLive()>0; }
   
   /** Retourne une couleur en fonction du niveau de vie */
   protected void updateColor() {
      int live = getLive();
      Aladin.trace(4,"TreeNodeProgen.updateColor() "+hii.getID()+" live="+live);
      int max = MAXLIVE-3000;
      Color c;
      if( live>max ) c = Color.black;
      else {
         int x =(int)( ( max-live)*(200./max) );
         Aladin.trace(4,"TreeNodeProgen.updateColor() x="+x);
         c = new Color(x,x,x);
      }
      setForeground(c);
   }
   
   @Override
   protected void submit() {
      String url = hii.resolveImageSourcePath(planBG.imageSourcePath);
      aladin.execCommand("get File(\""+url+"\")");
   }
   
   protected void draw(Graphics g,ViewSimple v) {
      String stc = hii.getSTC();
      if( stc==null ) return;
      List<STCObj> stcObjects = new STCStringParser().parse(stc); 
      Fov fov = new Fov(stcObjects);
      fov.draw(v.getProj(), v, g, 0, 0, Color.red);
   }
   
}

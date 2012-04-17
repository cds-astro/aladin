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
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCStringParser;
import cds.tools.Util;


/** Gère les noeuds de l'arbre des catégories (formulaire ServerCategory) */
public class TreeNodeProgen extends TreeNode {
   String url;
   String stc;
   String origin;

   TreeNodeProgen(Aladin aladin,String actionName,String description,String path,
         String url,String json) {
      super(aladin,actionName,null,description,path);
      this.url= url==null && json!=null ? Util.extractJSON("path",json) : url;
      if( json!=null ) stc = Util.extractJSON("stc",json);
   }
   
   @Override
   protected void submit() {
      aladin.execCommand("get File("+url+")");
   }
   
   protected void draw(Graphics g,ViewSimple v) {
      if( stc==null ) return;
      List<STCObj> stcObjects = new STCStringParser().parse(stc); 
      Fov fov = new Fov(stcObjects);
      fov.draw(v.getProj(), v, g, 0, 0, Color.red);
   }
   
}

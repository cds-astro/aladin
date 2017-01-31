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


package cds.aladin.bookmark;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import cds.aladin.Aladin;
import cds.aladin.Function;
import cds.tools.Util;

/**
 * Affichage sous forme d'un JButton d'un Bookmark (function script d'Aladin)
 * @author Pierre Fernique [CDS]
 * @version 1.1 - Mars 2011 - nettoyage/commentaire du code
 */
public class ButtonBookmark extends JButton {
   private Aladin aladin;
   
   /** La fonction script associée au bookmark */
   private Function fct;
   
   /**
    * Création d'un bookmark, et de sa fonction script associée
    * @param aladin
    * @param name le nom du bookmark
    * @param descr la description du bookmark
    * @param code le code script du bookmark
    */
   public ButtonBookmark(Aladin aladin,String name,String descr,String code) {
      super();
      this.aladin = aladin;
      fct = new Function(name,null,code,descr);
      suite();
   }
   
   /**
    * Création d'un bookmark à partir d'une fonction script
    * @param aladin
    * @param fonction La fonction script à associer au bookmark
    */
   public ButtonBookmark(Aladin aladin, Function fonction) {
      super();
      this.aladin = aladin;
      this.fct = fonction;
      suite();
   }
      
   private void suite() {
      setFont( Aladin.SBOLD );
      setForeground(Aladin.COLOR_LABEL);
      setBackground( aladin.getBackground() );
      setBorder(BorderFactory.createEmptyBorder(1,10,1,5));
      setToolTipText("<html>"+
            "<b>"+Util.fold(fct.getDescription(),20)+"</b>"+
            "<br><i>"+fct.getCode().replaceAll(";","<br>").replaceAll("\n","<br>")+"</i>"+
            "</html>");
      addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { exec(); }
      });
   }

   /** Fournit la fonction script associée au bookmark */
   public  Function getFunction() { return fct; }
   
   /** Fournit le texte du bouton, càd le nom de la fonction */
   public String getText() { return fct==null ? "X"  : fct.getName(); }
   
//   public boolean isEnabled() {
//      if( fonction==null ) return false;
//      String param = fonction.getParam();
//      if( param.indexOf("$TARGET")>=0 ) return fonction.getTarget(aladin).length()>0;
//      if( param.indexOf("$RADIUS")>=0 ) return  fonction.getRadius(aladin).length()>0;
//      return true;
//   }
   
   // Lance l'exécution du script
   private void exec() { 
      try { fct.exec(aladin,"",true); }
      catch( Exception e ) { e.printStackTrace(); }
   }
   
   static public final Color Orange = new Color(255,175,0);
   
   /** Affichage du bouton "bookmark", avec une petite étoile en préfixe */
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor( fct.isLocalDefinition() ? Color.blue : Orange);
      Util.drawStar(g, 6,8);
   }

}

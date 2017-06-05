// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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

import javax.swing.JComponent;

/**
 * Interface permettant de g�rer sous la forme d'un "Widget" un JComponent
 * en superposition d'un � JPanel classique. Par exemple, permet l'utilisation
 * de la boite de boutons d'Aladin (ToolBox) en superposition d'une vue (ViewSimple).
 *
 * Cette interface doit �tre implant�e par le JComponent
 *
 * @see aladin.view.ViewSimple
 * @see aladin.view.ToolBox
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : jan 2015 - cr�ation
 */
public interface Widget {

   /** Retourne l'objet g�rant l'affichage sous forme de widget associ� au JComponent */
   public WidgetControl getWidgetControl();

   /** Cr�e l'objet de gestion de l'affichage sous forme de widget
    * @param x Abscisse du widget (dans le rep�re du parent - origine HG)
    * @param y Ordonn�e du widget (dans le rep�re du parent - origine HG)
    * @param width Largeur du widget, ou -1 si on garde la largeur courante du JComponent
    * @param height Hauteur du widget, ou -1 si on garde la hauteur courante du JComponent
    * @param opacity Niveau d'opacit� [0 .. 1 ], ou -1 si totalement opaque
    * @param parent JComponent (typiquement un JPanel) parent (qui doit utiliser un ViewOverlayController)
    */
   public void createWidgetControl(int x,int y,int width,int height,float opacity,JComponent parent);

   /** Doit dessiner le contenu du bouton lorsque le widget est collaps� */
   public void paintCollapsed(Graphics g);
}

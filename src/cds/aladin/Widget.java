// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * Interface permettant de gérer sous la forme d'un "Widget" un JComponent
 * en superposition d'un à JPanel classique. Par exemple, permet l'utilisation
 * de la boite de boutons d'Aladin (ToolBox) en superposition d'une vue (ViewSimple).
 *
 * Cette interface doit être implantée par le JComponent
 *
 * @see aladin.view.ViewSimple
 * @see aladin.view.ToolBox
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : jan 2015 - création
 */
public interface Widget {

   /** Retourne l'objet gérant l'affichage sous forme de widget associé au JComponent */
   public WidgetControl getWidgetControl();

   /** Crée l'objet de gestion de l'affichage sous forme de widget
    * @param x Abscisse du widget (dans le repère du parent - origine HG)
    * @param y Ordonnée du widget (dans le repère du parent - origine HG)
    * @param width Largeur du widget, ou -1 si on garde la largeur courante du JComponent
    * @param height Hauteur du widget, ou -1 si on garde la hauteur courante du JComponent
    * @param opacity Niveau d'opacité [0 .. 1 ], ou -1 si totalement opaque
    * @param parent JComponent (typiquement un JPanel) parent (qui doit utiliser un ViewOverlayController)
    */
   public void createWidgetControl(int x,int y,int width,int height,float opacity,JComponent parent);

   /** Doit dessiner le contenu du bouton lorsque le widget est collapsé */
   public void paintCollapsed(Graphics g);
}

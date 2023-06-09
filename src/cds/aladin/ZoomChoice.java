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

import java.awt.*;
import javax.swing.*;

/**
 * Classe destin�e � trapper l'�venement mouseEnter lorsque l'utilisateur passe
 * sur le s�lecteur du Zoom afin de demander automatiquement le focus.
 * Ceci permet ainsi � l'utilisateur d'utiliser directement la molette
 * de la souris sans cliquer sur le s�lecteur du Zoom. (Sous r�serve
 * que la JVM utilise associe la molette au changement de choix dans
 * un Choice).
 * @author Pierre Fernique [CDS]
 * @version 1.0 - oct 2004 - Cr�ation
 */
public class ZoomChoice extends JPanel {
   private JComboBox cZoom;
   private Aladin aladin;

   ZoomChoice(Aladin aladin,JComboBox cZoom) {
      super();
      this.aladin = aladin;
      this.cZoom=cZoom;
      setLayout(new BorderLayout(0,0));
      setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 30));
      add(Aladin.createLabel("Zoom"),"West");
      add(cZoom,"Center");
   }

   public boolean mouseEnter(Event e, int x, int y) {
      cZoom.requestFocusInWindow();
      return true;
   }


}

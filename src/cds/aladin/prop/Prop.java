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

package cds.aladin.prop;

import java.awt.Font;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;

/** Gère une propriété. Il s'agit d'un objet qui contient tout ce qu'il faut pour
 * afficher et éventuellement mettre à jour une propriété
 * @date déc 2011 - création
 * @author Pierre Fernique [CDS]
 */
public class Prop {
   private String id;           // Identificateur
   private JComponent label;    // Titre (courte description)
   private JComponent widget;   // Le composant qui permet la visualisation et la mise à jour éventuelle
   private String help;         // Un help (petit paragraphe)
   private PropAction action;   // l'action à effectuer après une modification de la propriété par l'utilisateur (null sinon)
   private PropAction resume;   // l'action à effectuer pour réinitialiser la valeur de la propriété (null sinon)
   
   public String getId() { return id; }
   public JComponent getLabel() { return label; }
   public JComponent getWidget() { return widget; }
   public String getHelp() { return help; }
   
   /** Exécute l'action lors de la mise à jour de la priopriété
    * @return ProAction.NOTHING: aucun changement,
    *         PropAction.FAILED: nouvelle valeur impossible,
    *         PropAction.SUCCESS: nouvelle valeur prise en compte
    */
   public int apply() { 
      if( action==null ) return PropAction.NOTHING;
      return action.action();
   }
   
   /** Exécute la réinitialisation du widget de la propriété */
   public void resume() {
      if( resume==null ) return;
      if( widget.hasFocus() ) return; // Pour éviter une maj pdt une édition
      resume.action();
   }
   
   /** Crée une propriété
    * @param id Identificateur de la propriété (unique)
    * @param label  label de la propriété (peut être soit un String, soit directement un JComponent)
    * @param help  le help, ou null si aucun
    * @param widget  le widget associé à la proriété (peut être un String pour du simple texte)
    * @param resumeAction  L'action à exécuter lors de la réinitialisation de la propriété (ou null si aucune)
    * @param applyAction  L'action à exécuter lors après que l'utilisateur ait mise à jour la propriété (ou null si aucune)
    * @return La propriété
    */
   static public Prop propFactory(String id,Object label,String help,Object widget,PropAction resumeAction,PropAction applyAction) {
      Prop p = new Prop();
      p.id = id;
      p.resume = resumeAction;
      JLabel lab;
      if( label instanceof String ) {
         lab = new JLabel((String)label);
         lab.setFont(lab.getFont().deriveFont(Font.BOLD));
         p.label = lab;
      } else p.label = (JComponent)label;
      p.widget = widget instanceof String ? new JLabel((String)widget) : (JComponent)widget;
      p.help = help;
      p.action = applyAction;
      return p;
   }
   
   /** Supprime une propriété d'un vecteur
    * @param propList   La liste des propriétés
    * @param id         l'identificateur de la propriété à supprimer
    */
   static public void remove(Vector<Prop> propList,String id) {
      for( Prop p : propList ) {
         if( id.equals(p.getId())) { propList.remove(p); return; }
      }
   }
   
   
}



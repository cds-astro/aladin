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

package cds.aladin.prop;

import java.awt.Font;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;

/** G�re une propri�t�. Il s'agit d'un objet qui contient tout ce qu'il faut pour
 * afficher et �ventuellement mettre � jour une propri�t�
 * @date d�c 2011 - cr�ation
 * @author Pierre Fernique [CDS]
 */
public class Prop {
   private String id;           // Identificateur
   private JComponent label;    // Titre (courte description)
   private JComponent widget;   // Le composant qui permet la visualisation et la mise � jour �ventuelle
   private String help;         // Un help (petit paragraphe)
   private PropAction action;   // l'action � effectuer apr�s une modification de la propri�t� par l'utilisateur (null sinon)
   private PropAction resume;   // l'action � effectuer pour r�initialiser la valeur de la propri�t� (null sinon)
   
   public String getId() { return id; }
   public JComponent getLabel() { return label; }
   public JComponent getWidget() { return widget; }
   public String getHelp() { return help; }
   
   /** Ex�cute l'action lors de la mise � jour de la priopri�t�
    * @return ProAction.NOTHING: aucun changement,
    *         PropAction.FAILED: nouvelle valeur impossible,
    *         PropAction.SUCCESS: nouvelle valeur prise en compte
    */
   public int apply() { 
      if( action==null ) return PropAction.NOTHING;
      return action.action();
   }
   
   /** Ex�cute la r�initialisation du widget de la propri�t� */
   public void resume() {
      if( resume==null ) return;
      if( widget.hasFocus() ) return; // Pour �viter une maj pdt une �dition
      resume.action();
   }
   
   /** Cr�e une propri�t�
    * @param id Identificateur de la propri�t� (unique)
    * @param label  label de la propri�t� (peut �tre soit un String, soit directement un JComponent)
    * @param help  le help, ou null si aucun
    * @param widget  le widget associ� � la prori�t� (peut �tre un String pour du simple texte)
    * @param resumeAction  L'action � ex�cuter lors de la r�initialisation de la propri�t� (ou null si aucune)
    * @param applyAction  L'action � ex�cuter lors apr�s que l'utilisateur ait mise � jour la propri�t� (ou null si aucune)
    * @return La propri�t�
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
   
   /** Supprime une propri�t� d'un vecteur
    * @param propList   La liste des propri�t�s
    * @param id         l'identificateur de la propri�t� � supprimer
    */
   static public void remove(Vector<Prop> propList,String id) {
      for( Prop p : propList ) {
         if( id.equals(p.getId())) { propList.remove(p); return; }
      }
   }
   
   
}



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

/**
 * Le formulaire d'interrogation de NED
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class ServerNED extends ServerSimbad  {

  /** Initialisation des variables propres a NED */
   protected void init() {
      aladinLabel = "NED database";
      type = CATALOG;
      tagGlu = "NedXML";
      aladinLogo = "NedLogo.gif";
      docUser = "http://nedwww.ipac.caltech.edu/help/intro.html";
      maxRadius=3*60;
      filters = new String[] {
            "#All objects\nfilter All{\n" +
            "${Type}=\"G\" { draw oval }\n" +
            "${Type}=\"RadioS\" || ${Type}=\"Maser\" { draw triangle }\n" +
            "${Type}=\"IRS\" || ${Type}=\"IrS\" {draw rhomb}\n" +
            "${Type}=\"\\*\" {draw circle(3)}\n" +
            "${Type}=\"Vis*\" || ${Type}=\"SNR\" || ${Type}=\"Neb\" || ${Type}=\"HI*\" {draw square}\n" +
            "${Type}=\"UV*\" {draw cross;draw plus}\n" +
            "${Type}=\"X*\" {draw cross}\n" +
            "{ draw ${Type} }\n" +
            "}",
//            "#NED filter (colorized)\nfilter NED.filterC {\n" +
//            "${Type}=\"G\" { draw blue oval }\n" +
//            "${Type}=\"RadioS\" || ${Type}=\"Maser\" { draw green triangle }\n" +
//            "${Type}=\"IRS\" || ${Type}=\"IrS\" {draw red rhomb}\n" +
//            "${Type}=\"\\*\" {draw red circle(3)}\n" +
//            "${Type}=\"Vis*\" || ${Type}=\"SNR\" || ${Type}=\"Neb\" || ${Type}=\"HI*\" {draw red square}\n" +
//            "${Type}=\"UV*\" {draw magenta cross;draw magenta plus}\n" +
//            "${Type}=\"X*\" {draw black cross}\n" +
//            "{ draw #9900CC ${Type} }\n" +
//            "}",
            "#Star\nfilter Star {\n" +
            "${Type}=\"\\*\" {draw circle(3)}\n" +
            "}\n",
            "#Galaxy\nfilter Galaxy {\n" +
            "${Type}=\"G\" { draw oval }\n" +
            "}\n",
            "#Radio\nfilter Radio {\n" +
            "${Type}=\"RadioS\" || ${Type}=\"Maser\" { draw triangle }\n" +
            "}\n",
            "#UV\nfilter UV {\n" +
            "${Type}=\"UV*\" {draw cross;draw plus}\n" +
            "}\n",
            "#IR\nfilter IR {\n" +
            "${Type}=\"IRS\" || ${Type}=\"IrS\" {draw rhomb}\n" +
            "}\n",
            "#Neb\nfilter Neb {\n" +
            "${Type}=\"Vis*\" || ${Type}=\"SNR\" || ${Type}=\"Neb\" || ${Type}=\"HI*\" {draw square}\n" +
            "}\n",
            "#X\nfilter X {\n" +
            "${Type}=\"X*\" {draw cross}\n" +
            "}\n",
     };
   }

   protected void createChaine() {
      super.createChaine();
      description  = aladin.chaine.getString("NEDINFO");
      institute  = aladin.chaine.getString("NEDFROM");
      title = aladin.chaine.getString("NEDTITLE");
      verboseDescr  = aladin.chaine.getString("NEDDESC");
    }
 /** Creation du formulaire d'interrogation de NED.
   * @param aladin reference
   * @param status le label qui affichera l'etat courant
   */
   ServerNED(Aladin aladin) {
      super(aladin);
   }
   
   protected int createPlane(String target,String radius,String criteria,String label, String origin) {
      try {
         target=sesameIfRequired(target," ");
      } catch( Exception e ) { }
      return super.createPlane(target,radius,criteria,label,origin);
   }
}
